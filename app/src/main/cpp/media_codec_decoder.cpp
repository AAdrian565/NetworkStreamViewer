#include "media_codec_decoder.h"

#include <android/log.h>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaFormat.h>

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <limits>

namespace {

constexpr char kLogTag[] = "NdiHxDecoder";
constexpr int64_t kInputTimeoutMicroseconds = 5'000;

void log_error(const char* message) {
    __android_log_write(ANDROID_LOG_ERROR, kLogTag, message);
}

bool is_supported_codec(NDIlib_compressed_FourCC_type_e type) {
    return type == NDIlib_compressed_FourCC_type_H264 ||
        type == NDIlib_compressed_FourCC_type_HEVC;
}

const char* mime_type_for(NDIlib_compressed_FourCC_type_e type) {
    return type == NDIlib_compressed_FourCC_type_H264 ? "video/avc" : "video/hevc";
}

}  // namespace

MediaCodecDecoder::MediaCodecDecoder(ANativeWindow* window) : window_(window) {}

MediaCodecDecoder::~MediaCodecDecoder() {
    reset();
}

bool MediaCodecDecoder::submit(const NDIlib_video_frame_v2_t& frame) {
    if (frame.p_data == nullptr || frame.data_size_in_bytes < NDIlib_compressed_packet_version_0) {
        log_error("NDI HX frame has an invalid packet buffer");
        return false;
    }

    const auto* packet = reinterpret_cast<const NDIlib_compressed_packet_t*>(frame.p_data);
    const int packet_size = frame.data_size_in_bytes;
    if (packet->version < NDIlib_compressed_packet_version_0 || packet->version > packet_size ||
        !is_supported_codec(packet->fourCC)) {
        log_error("NDI HX frame has an unsupported packet header");
        return false;
    }

    const uint64_t payload_size = static_cast<uint64_t>(packet->data_size);
    const uint64_t extra_size = static_cast<uint64_t>(packet->extra_data_size);
    const uint64_t available_size = static_cast<uint64_t>(packet_size - packet->version);
    if (payload_size + extra_size > available_size ||
        payload_size > static_cast<uint64_t>(std::numeric_limits<size_t>::max())) {
        log_error("NDI HX frame packet sizes are invalid");
        return false;
    }

    const auto* payload = frame.p_data + packet->version;
    const auto* extra_data = payload + packet->data_size;
    const bool keyframe = (packet->flags & NDIlib_compressed_packet_flags_keyframe) != 0;
    const bool format_changed = codec_ != nullptr &&
        (codec_type_ != packet->fourCC || width_ != frame.xres || height_ != frame.yres);
    if (format_changed) reset();

    if (codec_ == nullptr) {
        if (!keyframe) return true;
        if (!configure(
                packet->fourCC,
                frame.xres,
                frame.yres,
                frame.frame_rate_N,
                frame.frame_rate_D,
                extra_data,
                packet->extra_data_size
            )) {
            return false;
        }
    }

    if (!drainOutput()) return false;

    const ssize_t input_index = AMediaCodec_dequeueInputBuffer(codec_, kInputTimeoutMicroseconds);
    if (input_index == AMEDIACODEC_INFO_TRY_AGAIN_LATER) return true;
    if (input_index < 0) {
        log_error("Could not dequeue an Android video decoder input buffer");
        reset();
        return false;
    }

    size_t input_capacity = 0;
    uint8_t* input = AMediaCodec_getInputBuffer(
        codec_,
        static_cast<size_t>(input_index),
        &input_capacity
    );
    if (input == nullptr || packet->data_size > input_capacity) {
        log_error("NDI HX frame is larger than the Android decoder input buffer");
        AMediaCodec_queueInputBuffer(codec_, static_cast<size_t>(input_index), 0, 0, 0, 0);
        return false;
    }

    std::memcpy(input, payload, packet->data_size);
    const int64_t presentation_time_microseconds = packet->pts / 10;
    if (AMediaCodec_queueInputBuffer(
            codec_,
            static_cast<size_t>(input_index),
            0,
            packet->data_size,
            static_cast<uint64_t>(std::max<int64_t>(presentation_time_microseconds, 0)),
            0
        ) != AMEDIA_OK) {
        log_error("Could not queue an NDI HX frame for Android video decoding");
        reset();
        return false;
    }

    return drainOutput();
}

void MediaCodecDecoder::reset() {
    if (codec_ != nullptr) {
        AMediaCodec_stop(codec_);
        AMediaCodec_delete(codec_);
        codec_ = nullptr;
    }
    codec_type_ = NDIlib_compressed_FourCC_type_max;
    width_ = 0;
    height_ = 0;
}

bool MediaCodecDecoder::configure(
    NDIlib_compressed_FourCC_type_e codec_type,
    int width,
    int height,
    int frame_rate_n,
    int frame_rate_d,
    const uint8_t* codec_data,
    size_t codec_data_size
) {
    if (window_ == nullptr || width <= 0 || height <= 0 || !is_supported_codec(codec_type)) {
        return false;
    }

    codec_ = AMediaCodec_createDecoderByType(mime_type_for(codec_type));
    if (codec_ == nullptr) {
        log_error("This Android device has no decoder for the NDI HX video codec");
        return false;
    }

    AMediaFormat* format = AMediaFormat_new();
    if (format == nullptr) {
        log_error("Could not create the Android video decoder format");
        reset();
        return false;
    }

    AMediaFormat_setString(format, AMEDIAFORMAT_KEY_MIME, mime_type_for(codec_type));
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_WIDTH, width);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_HEIGHT, height);
    if (frame_rate_n > 0 && frame_rate_d > 0) {
        AMediaFormat_setFloat(
            format,
            AMEDIAFORMAT_KEY_FRAME_RATE,
            static_cast<float>(frame_rate_n) / static_cast<float>(frame_rate_d)
        );
    }
    if (codec_data != nullptr && codec_data_size > 0) {
        AMediaFormat_setBuffer(format, "csd-0", codec_data, codec_data_size);
    }

    const media_status_t configure_status = AMediaCodec_configure(
        codec_,
        format,
        window_,
        nullptr,
        0
    );
    AMediaFormat_delete(format);
    if (configure_status != AMEDIA_OK || AMediaCodec_start(codec_) != AMEDIA_OK) {
        log_error("Android could not configure the NDI HX video decoder");
        reset();
        return false;
    }

    codec_type_ = codec_type;
    width_ = width;
    height_ = height;
    __android_log_print(
        ANDROID_LOG_INFO,
        kLogTag,
        "Started %s decoder for %dx%d NDI HX video",
        mime_type_for(codec_type),
        width,
        height
    );
    return true;
}

bool MediaCodecDecoder::drainOutput() {
    while (codec_ != nullptr) {
        AMediaCodecBufferInfo info{};
        const ssize_t output_index = AMediaCodec_dequeueOutputBuffer(codec_, &info, 0);
        if (output_index >= 0) {
            if (AMediaCodec_releaseOutputBuffer(
                    codec_,
                    static_cast<size_t>(output_index),
                    info.size > 0
                ) != AMEDIA_OK) {
                log_error("Could not render an Android video decoder output buffer");
                reset();
                return false;
            }
            continue;
        }
        if (output_index == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED ||
            output_index == AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED) {
            continue;
        }
        if (output_index == AMEDIACODEC_INFO_TRY_AGAIN_LATER) return true;

        log_error("Android video decoder reported an output error");
        reset();
        return false;
    }
    return false;
}
