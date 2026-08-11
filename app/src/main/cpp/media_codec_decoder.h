#pragma once

#include <android/native_window.h>

#include <Processing.NDI.Advanced.h>

enum class DecodeResult {
    Submitted,
    WaitingForKeyframe,
    DecoderFailure
};

class MediaCodecDecoder {
public:
    explicit MediaCodecDecoder(ANativeWindow* window);
    ~MediaCodecDecoder();

    MediaCodecDecoder(const MediaCodecDecoder&) = delete;
    MediaCodecDecoder& operator=(const MediaCodecDecoder&) = delete;

    DecodeResult submit(const NDIlib_video_frame_v2_t& frame);
    int takeRenderedFrameCount();
    void reset();

private:
    bool configure(
        NDIlib_compressed_FourCC_type_e codec_type,
        int width,
        int height,
        int frame_rate_n,
        int frame_rate_d,
        const uint8_t* codec_data,
        size_t codec_data_size
    );
    bool drainOutput();

    ANativeWindow* window_;
    int rendered_frame_count_ = 0;
    struct AMediaCodec* codec_ = nullptr;
    NDIlib_compressed_FourCC_type_e codec_type_ = NDIlib_compressed_FourCC_type_max;
    int width_ = 0;
    int height_ = 0;
};
