#include "ndi_audio_capture.h"

#include <Processing.NDI.Advanced.h>
#include <Processing.NDI.FrameSync.h>
#include <Processing.NDI.utilities.h>

#include <algorithm>
#include <limits>

struct NdiAudioCapture::State {
    NDIlib_recv_instance_t receiver = nullptr;
    NDIlib_framesync_instance_t frame_sync = nullptr;
};

namespace {
constexpr int kMinSampleRate = 8'000;
constexpr int kMaxSampleRate = 192'000;
constexpr int kMaxChannels = 2;
constexpr int kMaxSamplesPerChannel = 4'096;
}

NdiAudioCapture::~NdiAudioCapture() {
    stop();
}

bool NdiAudioCapture::start(
    const std::string& source_name,
    const std::string& source_url,
    const char* receiver_config
) {
    stop();
    auto* state = new State();
    NDIlib_recv_create_v3_t settings{};
    settings.source_to_connect_to.p_ndi_name = source_name.empty() ? nullptr : source_name.c_str();
    settings.source_to_connect_to.p_url_address = source_url.empty() ? nullptr : source_url.c_str();
    settings.color_format = static_cast<NDIlib_recv_color_format_e>(
        NDIlib_recv_color_format_compressed_v4
    );
    settings.bandwidth = NDIlib_recv_bandwidth_audio_only;
    settings.allow_video_fields = false;
    settings.p_ndi_recv_name = "Network Stream Viewer Audio";
    state->receiver = NDIlib_recv_create_v4(&settings, receiver_config);
    if (state->receiver == nullptr) {
        delete state;
        return false;
    }
    state->frame_sync = NDIlib_framesync_create(state->receiver);
    if (state->frame_sync == nullptr) {
        NDIlib_recv_destroy(state->receiver);
        delete state;
        return false;
    }
    state_ = state;
    return true;
}

int64_t NdiAudioCapture::fill_interleaved_16s(
    int16_t* destination,
    size_t destination_samples,
    int sample_rate,
    int channels,
    int samples_per_channel
) {
    if (state_ == nullptr || state_->frame_sync == nullptr || destination == nullptr ||
        sample_rate < kMinSampleRate || sample_rate > kMaxSampleRate ||
        channels < 1 || channels > kMaxChannels || samples_per_channel < 1 ||
        samples_per_channel > kMaxSamplesPerChannel ||
        static_cast<size_t>(channels) > std::numeric_limits<size_t>::max() /
            static_cast<size_t>(samples_per_channel) ||
        destination_samples < static_cast<size_t>(channels) * static_cast<size_t>(samples_per_channel)) {
        return state_ == nullptr ? -1 : -2;
    }

    const size_t sample_count = static_cast<size_t>(channels) * static_cast<size_t>(samples_per_channel);
    std::fill(destination, destination + sample_count, static_cast<int16_t>(0));

    NDIlib_audio_frame_v2_t audio{};
    NDIlib_framesync_capture_audio(
        state_->frame_sync,
        &audio,
        sample_rate,
        channels,
        samples_per_channel
    );

    const auto finish = [this, &audio](int64_t result) {
        NDIlib_framesync_free_audio(state_->frame_sync, &audio);
        return result;
    };

    if (audio.p_data == nullptr || audio.no_samples == 0) {
        const auto stats = performance();
        return finish(stats.first);
    }
    if (audio.no_samples != samples_per_channel || audio.no_channels != channels ||
        audio.sample_rate != sample_rate || audio.no_samples < 0 || audio.no_channels < 1) {
        return finish(-3);
    }

    NDIlib_audio_frame_interleaved_16s_t converted{};
    converted.sample_rate = sample_rate;
    converted.no_channels = channels;
    converted.no_samples = samples_per_channel;
    converted.reference_level = 20;
    converted.p_data = destination;
    NDIlib_util_audio_to_interleaved_16s_v2(&audio, &converted);
    const auto stats = performance();
    return finish(stats.first);
}

std::pair<int64_t, int64_t> NdiAudioCapture::performance() const {
    if (state_ == nullptr || state_->receiver == nullptr) return {0, 0};
    NDIlib_recv_performance_t total{};
    NDIlib_recv_performance_t dropped{};
    NDIlib_recv_get_performance(state_->receiver, &total, &dropped);
    return {total.audio_frames, dropped.audio_frames};
}

void NdiAudioCapture::stop() {
    if (state_ == nullptr) return;
    if (state_->frame_sync != nullptr) {
        NDIlib_framesync_destroy(state_->frame_sync);
        state_->frame_sync = nullptr;
    }
    if (state_->receiver != nullptr) {
        NDIlib_recv_destroy(state_->receiver);
        state_->receiver = nullptr;
    }
    delete state_;
    state_ = nullptr;
}
