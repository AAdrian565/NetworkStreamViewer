#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <utility>

class NdiAudioCapture {
public:
    NdiAudioCapture() = default;
    ~NdiAudioCapture();
    NdiAudioCapture(const NdiAudioCapture&) = delete;
    NdiAudioCapture& operator=(const NdiAudioCapture&) = delete;

    bool start(const std::string& source_name, const std::string& source_url, const char* receiver_config);
    int64_t fill_interleaved_16s(
        int16_t* destination,
        size_t destination_samples,
        int sample_rate,
        int channels,
        int samples_per_channel
    );
    std::pair<int64_t, int64_t> performance() const;
    void stop();

private:
    struct State;
    State* state_ = nullptr;
};
