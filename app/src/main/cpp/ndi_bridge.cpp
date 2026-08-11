#include <jni.h>

#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#include <Processing.NDI.Advanced.h>

#include "media_codec_decoder.h"
#include "ndi_audio_capture.h"

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cmath>
#include <cstring>
#include <memory>
#include <mutex>
#include <shared_mutex>
#include <string>
#include <thread>
#include <vector>

namespace {

constexpr char kLogTag[] = "NdiBridge";
constexpr char kReceiverConfig[] =
    R"({"ndi":{"codec":{"shq":{"passthrough":false}}}})";

std::shared_mutex g_lifecycle_mutex;
std::mutex g_finder_mutex;
std::mutex g_sender_mutex;
bool g_initialized = false;
NDIlib_find_instance_t g_finder = nullptr;
NDIlib_recv_instance_t g_receiver = nullptr;
std::unique_ptr<NdiAudioCapture> g_audio_capture;
NDIlib_send_instance_t g_sender = nullptr;
ANativeWindow* g_window = nullptr;
jobject g_playback_listener = nullptr;
jmethodID g_aspect_ratio_method = nullptr;
jmethodID g_playback_state_method = nullptr;
jmethodID g_ptz_support_method = nullptr;
jmethodID g_video_diagnostics_method = nullptr;
JavaVM* g_java_vm = nullptr;
std::thread g_receive_thread;
std::atomic_bool g_receiving = false;

void log_error(const char* message) {
    __android_log_write(ANDROID_LOG_ERROR, kLogTag, message);
}

int stream_format_code(const NDIlib_video_frame_v2_t& frame) {
    const auto four_cc = static_cast<NDIlib_FourCC_video_type_ex_e>(frame.FourCC);
    if (four_cc == NDIlib_FourCC_video_type_ex_H264_highest_bandwidth ||
        four_cc == NDIlib_FourCC_video_type_ex_H264_lowest_bandwidth) {
        return 1;
    }
    if (four_cc == NDIlib_FourCC_video_type_ex_HEVC_highest_bandwidth ||
        four_cc == NDIlib_FourCC_video_type_ex_HEVC_lowest_bandwidth) {
        return 2;
    }
    return 0;
}

bool is_hx_video(const NDIlib_video_frame_v2_t& frame) {
    return stream_format_code(frame) != 0;
}

uint8_t clamp_color(int value) {
    return static_cast<uint8_t>(std::clamp(value, 0, 255));
}

void write_uyvy_pixel(uint8_t* destination, uint8_t y, uint8_t u, uint8_t v) {
    const int luminance = std::max(static_cast<int>(y) - 16, 0);
    const int blue_difference = static_cast<int>(u) - 128;
    const int red_difference = static_cast<int>(v) - 128;
    destination[0] = clamp_color((298 * luminance + 409 * red_difference + 128) >> 8);
    destination[1] = clamp_color(
        (298 * luminance - 100 * blue_difference - 208 * red_difference + 128) >> 8
    );
    destination[2] = clamp_color((298 * luminance + 516 * blue_difference + 128) >> 8);
    destination[3] = 255;
}

enum class RenderResult {
    Rendered,
    UnsupportedFormat,
    Failure
};

RenderResult render_frame(ANativeWindow* window, const NDIlib_video_frame_v2_t& frame) {
    if (window == nullptr || frame.p_data == nullptr || frame.xres <= 0 || frame.yres <= 0) {
        return RenderResult::Failure;
    }
    const bool rgba = frame.FourCC == NDIlib_FourCC_type_RGBA ||
        frame.FourCC == NDIlib_FourCC_type_RGBX;
    const bool uyvy = frame.FourCC == NDIlib_FourCC_type_UYVY;
    if (!rgba && !uyvy) {
        log_error("NDI returned an unexpected pixel format");
        return RenderResult::UnsupportedFormat;
    }

    if (ANativeWindow_setBuffersGeometry(window, frame.xres, frame.yres, WINDOW_FORMAT_RGBA_8888) != 0) {
        log_error("Could not set the native window geometry");
        return RenderResult::Failure;
    }

    ANativeWindow_Buffer buffer{};
    if (ANativeWindow_lock(window, &buffer, nullptr) != 0) return RenderResult::Failure;

    const int destination_stride = buffer.stride * 4;
    const int rows = std::min(frame.yres, buffer.height);
    const int columns = std::min(frame.xres, buffer.width);

    auto* destination = static_cast<uint8_t*>(buffer.bits);
    if (rgba) {
        const int source_stride = frame.line_stride_in_bytes > 0
            ? frame.line_stride_in_bytes
            : frame.xres * 4;
        const int row_bytes = std::min(columns * 4, destination_stride);
        for (int row = 0; row < rows; ++row) {
            std::memcpy(
                destination + row * destination_stride,
                frame.p_data + row * source_stride,
                static_cast<size_t>(row_bytes)
            );
        }
    } else {
        const int source_stride = frame.line_stride_in_bytes > 0
            ? frame.line_stride_in_bytes
            : frame.xres * 2;
        for (int row = 0; row < rows; ++row) {
            const uint8_t* source_row = frame.p_data + row * source_stride;
            uint8_t* destination_row = destination + row * destination_stride;
            for (int column = 0; column + 1 < columns; column += 2) {
                const uint8_t u = source_row[column * 2];
                const uint8_t y0 = source_row[column * 2 + 1];
                const uint8_t v = source_row[column * 2 + 2];
                const uint8_t y1 = source_row[column * 2 + 3];
                write_uyvy_pixel(destination_row + column * 4, y0, u, v);
                write_uyvy_pixel(destination_row + (column + 1) * 4, y1, u, v);
            }
        }
    }

    ANativeWindow_unlockAndPost(window);
    return RenderResult::Rendered;
}

void notify_playback_state(
    JNIEnv* env,
    jobject listener,
    jmethodID method,
    int state,
    int& last_state
) {
    if (state == last_state) return;
    env->CallVoidMethod(listener, method, state);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        log_error("The playback-state callback failed");
        return;
    }
    last_state = state;
}

void notify_ptz_support(
    JNIEnv* env,
    jobject listener,
    jmethodID method,
    bool is_supported,
    int& last_support_state
) {
    const int support_state = is_supported ? 1 : 0;
    if (support_state == last_support_state) return;
    env->CallVoidMethod(listener, method, is_supported ? JNI_TRUE : JNI_FALSE);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        log_error("The PTZ-support callback failed");
        return;
    }
    last_support_state = support_state;
}

void notify_video_diagnostics(
    JNIEnv* env,
    jobject listener,
    jmethodID method,
    int64_t total_frames,
    int64_t dropped_frames,
    int width,
    int height,
    int queue_depth,
    float received_fps,
    float rendered_fps,
    float processing_time_ms
) {
    env->CallVoidMethod(
        listener,
        method,
        static_cast<jlong>(total_frames),
        static_cast<jlong>(dropped_frames),
        static_cast<jint>(width),
        static_cast<jint>(height),
        static_cast<jint>(queue_depth),
        static_cast<jfloat>(received_fps),
        static_cast<jfloat>(rendered_fps),
        static_cast<jfloat>(processing_time_ms)
    );
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        log_error("The video-diagnostics callback failed");
    }
}

void receive_loop(
    NDIlib_recv_instance_t receiver,
    ANativeWindow* window,
    JavaVM* java_vm,
    jobject aspect_ratio_listener,
    jmethodID aspect_ratio_method,
    jmethodID playback_state_method,
    jmethodID ptz_support_method,
    jmethodID video_diagnostics_method,
    bool automatic_bandwidth
) {
    JNIEnv* env = nullptr;
    bool attached_to_vm = false;
    if (java_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_EDETACHED) {
        if (java_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            log_error("Could not attach the NDI receive thread to the JVM");
            return;
        }
        attached_to_vm = true;
    }

    float last_aspect_ratio = 0.0f;
    int last_playback_state = -1;
    int last_ptz_support_state = -1;
    bool automatic_fallback_active = false;
    auto last_video_time = std::chrono::steady_clock::now();
    auto last_diagnostics_time = last_video_time;
    int last_video_width = 0;
    int last_video_height = 0;
    int64_t interval_received_frames = 0;
    int64_t interval_rendered_frames = 0;
    int64_t interval_processing_time_microseconds = 0;
    MediaCodecDecoder hx_decoder(window);
    notify_playback_state(
        env,
        aspect_ratio_listener,
        playback_state_method,
        0,
        last_playback_state
    );
    notify_ptz_support(
        env,
        aspect_ratio_listener,
        ptz_support_method,
        NDIlib_recv_ptz_is_supported(receiver),
        last_ptz_support_state
    );
    while (g_receiving.load()) {
        const auto publish_diagnostics = [&]() {
            const auto now = std::chrono::steady_clock::now();
            const auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(
                now - last_diagnostics_time
            ).count();
            if (elapsed < 1000) return;

            NDIlib_recv_performance_t total{};
            NDIlib_recv_performance_t dropped{};
            NDIlib_recv_queue_t queue{};
            NDIlib_recv_get_performance(receiver, &total, &dropped);
            NDIlib_recv_get_queue(receiver, &queue);
            const float elapsed_seconds = static_cast<float>(elapsed) / 1000.0f;
            const float received_fps = static_cast<float>(interval_received_frames) / elapsed_seconds;
            const float rendered_fps = static_cast<float>(interval_rendered_frames) / elapsed_seconds;
            const float processing_time_ms = interval_received_frames > 0
                ? static_cast<float>(interval_processing_time_microseconds) /
                    static_cast<float>(interval_received_frames) / 1000.0f
                : 0.0f;
            notify_video_diagnostics(
                env,
                aspect_ratio_listener,
                video_diagnostics_method,
                total.video_frames,
                dropped.video_frames,
                last_video_width,
                last_video_height,
                queue.video_frames,
                received_fps,
                rendered_fps,
                processing_time_ms
            );
            interval_received_frames = 0;
            interval_rendered_frames = 0;
            interval_processing_time_microseconds = 0;
            last_diagnostics_time = now;
        };
        publish_diagnostics();
        NDIlib_video_frame_v2_t video{};
        const NDIlib_frame_type_e type = NDIlib_recv_capture_v3(
            receiver,
            &video,
            nullptr,
            nullptr,
            100
        );

        if (type == NDIlib_frame_type_video) {
            if (video.xres <= 0 || video.yres <= 0) {
                NDIlib_recv_free_video_v2(receiver, &video);
                log_error("NDI returned a video frame with invalid dimensions");
                notify_playback_state(
                    env,
                    aspect_ratio_listener,
                    playback_state_method,
                    5,
                    last_playback_state
                );
                break;
            }

            bool terminal_playback_error = false;
            const auto processing_started = std::chrono::steady_clock::now();
            interval_received_frames++;
            last_video_width = video.xres;
            last_video_height = video.yres;
            last_video_time = processing_started;
            const float aspect_ratio = video.picture_aspect_ratio > 0.0f
                ? video.picture_aspect_ratio
                : static_cast<float>(video.xres) / static_cast<float>(video.yres);
            if (std::fabs(aspect_ratio - last_aspect_ratio) > 0.001f) {
                env->CallVoidMethod(aspect_ratio_listener, aspect_ratio_method, aspect_ratio);
                if (env->ExceptionCheck()) {
                    env->ExceptionClear();
                    log_error("The video aspect-ratio callback failed");
                }
                last_aspect_ratio = aspect_ratio;
            }
            if (is_hx_video(video)) {
                const DecodeResult decode_result = hx_decoder.submit(video);
                interval_rendered_frames += hx_decoder.takeRenderedFrameCount();
                if (decode_result == DecodeResult::WaitingForKeyframe) {
                    notify_playback_state(
                        env,
                        aspect_ratio_listener,
                        playback_state_method,
                        1,
                        last_playback_state
                    );
                } else if (decode_result == DecodeResult::DecoderFailure) {
                    notify_playback_state(
                        env,
                        aspect_ratio_listener,
                        playback_state_method,
                        5,
                        last_playback_state
                    );
                    terminal_playback_error = true;
                } else {
                    notify_playback_state(
                        env,
                        aspect_ratio_listener,
                        playback_state_method,
                        2,
                        last_playback_state
                    );
                }
            } else {
                hx_decoder.reset();
                const RenderResult render_result = render_frame(window, video);
                const int state = render_result == RenderResult::Rendered
                    ? 2
                    : render_result == RenderResult::UnsupportedFormat ? 4 : 5;
                notify_playback_state(
                    env,
                    aspect_ratio_listener,
                    playback_state_method,
                    state,
                    last_playback_state
                );
                if (render_result == RenderResult::Rendered) interval_rendered_frames++;
                terminal_playback_error = render_result != RenderResult::Rendered;
            }
            interval_processing_time_microseconds += std::chrono::duration_cast<std::chrono::microseconds>(
                std::chrono::steady_clock::now() - processing_started
            ).count();
            NDIlib_recv_free_video_v2(receiver, &video);
            if (terminal_playback_error) break;
        } else if (type == NDIlib_frame_type_status_change) {
            notify_ptz_support(
                env,
                aspect_ratio_listener,
                ptz_support_method,
                NDIlib_recv_ptz_is_supported(receiver),
                last_ptz_support_state
            );
        } else if (type == NDIlib_frame_type_error) {
            log_error("The NDI receiver lost its connection");
            notify_playback_state(
                env,
                aspect_ratio_listener,
                playback_state_method,
                3,
                last_playback_state
            );
            break;
        } else if (type == NDIlib_frame_type_none) {
            const auto silent_for = std::chrono::duration_cast<std::chrono::seconds>(
                std::chrono::steady_clock::now() - last_video_time
            );
            if (silent_for >= std::chrono::seconds(2) &&
                NDIlib_recv_get_no_connections(receiver) == 0) {
                notify_playback_state(
                    env,
                    aspect_ratio_listener,
                    playback_state_method,
                    3,
                    last_playback_state
                );
            } else if (silent_for >= std::chrono::seconds(4)) {
                if (automatic_bandwidth && !automatic_fallback_active &&
                    NDIlib_recv_set_bandwidth(receiver, NDIlib_recv_bandwidth_lowest)) {
                    automatic_fallback_active = true;
                    last_video_time = std::chrono::steady_clock::now();
                    notify_playback_state(
                        env,
                        aspect_ratio_listener,
                        playback_state_method,
                        0,
                        last_playback_state
                    );
                } else {
                    notify_playback_state(
                        env,
                        aspect_ratio_listener,
                        playback_state_method,
                        6,
                        last_playback_state
                    );
                }
            }
        }
    }

    if (attached_to_vm) java_vm->DetachCurrentThread();
}

void stop_receiver_locked(JNIEnv* env) {
    g_receiving.store(false);
    if (g_receive_thread.joinable()) g_receive_thread.join();

    if (g_audio_capture != nullptr) {
        g_audio_capture->stop();
        g_audio_capture.reset();
    }

    if (g_receiver != nullptr) {
        // Stop unconditionally: capability can disappear before surface destruction.
        NDIlib_recv_ptz_pan_tilt_speed(g_receiver, 0.0f, 0.0f);
        NDIlib_recv_ptz_zoom_speed(g_receiver, 0.0f);
        NDIlib_recv_ptz_focus_speed(g_receiver, 0.0f);
        NDIlib_recv_destroy(g_receiver);
        g_receiver = nullptr;
    }
    if (g_window != nullptr) {
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }
    if (g_playback_listener != nullptr) {
        env->DeleteGlobalRef(g_playback_listener);
        g_playback_listener = nullptr;
    }
    g_aspect_ratio_method = nullptr;
    g_playback_state_method = nullptr;
    g_ptz_support_method = nullptr;
    g_video_diagnostics_method = nullptr;
    g_java_vm = nullptr;
}

void stop_sender_locked() {
    if (g_sender != nullptr) {
        NDIlib_send_destroy(g_sender);
        g_sender = nullptr;
    }
}

std::string from_java_string(JNIEnv* env, jstring value) {
    if (value == nullptr) return {};
    const char* characters = env->GetStringUTFChars(value, nullptr);
    if (characters == nullptr) return {};
    std::string result(characters);
    env->ReleaseStringUTFChars(value, characters);
    return result;
}

}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_initialize(JNIEnv*, jobject) {
    std::scoped_lock lock(g_lifecycle_mutex);
    if (g_initialized) return JNI_TRUE;
    if (!NDIlib_initialize()) {
        log_error("NDIlib_initialize failed");
        return JNI_FALSE;
    }

    NDIlib_find_create_t settings{};
    settings.show_local_sources = true;
    g_finder = NDIlib_find_create_v2(&settings);
    if (g_finder == nullptr) {
        NDIlib_destroy();
        log_error("NDIlib_find_create_v2 failed");
        return JNI_FALSE;
    }

    g_initialized = true;
    return JNI_TRUE;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_discoverSources(
    JNIEnv* env,
    jobject,
    jint timeout_ms
) {
    std::scoped_lock lock(g_finder_mutex);
    const jclass string_class = env->FindClass("java/lang/String");
    if (!g_initialized || g_finder == nullptr) {
        return env->NewObjectArray(0, string_class, nullptr);
    }

    NDIlib_find_wait_for_sources(g_finder, static_cast<uint32_t>(std::max(timeout_ms, 0)));

    uint32_t source_count = 0;
    const NDIlib_source_t* sources = NDIlib_find_get_current_sources(g_finder, &source_count);
    const jsize value_count = static_cast<jsize>(source_count * 2);
    jobjectArray result = env->NewObjectArray(value_count, string_class, nullptr);

    for (uint32_t index = 0; index < source_count; ++index) {
        const char* name = sources[index].p_ndi_name != nullptr ? sources[index].p_ndi_name : "Unnamed source";
        const char* url = sources[index].p_url_address != nullptr ? sources[index].p_url_address : "";
        jstring java_name = env->NewStringUTF(name);
        jstring java_url = env->NewStringUTF(url);
        env->SetObjectArrayElement(result, static_cast<jsize>(index * 2), java_name);
        env->SetObjectArrayElement(result, static_cast<jsize>(index * 2 + 1), java_url);
        env->DeleteLocalRef(java_name);
        env->DeleteLocalRef(java_url);
    }
    return result;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_probeSource(
    JNIEnv* env,
    jobject,
    jstring source_name,
    jstring source_url,
    jint timeout_ms
) {
    std::shared_lock lifecycle_lock(g_lifecycle_mutex);
    if (!g_initialized) return nullptr;

    const std::string name = from_java_string(env, source_name);
    const std::string url = from_java_string(env, source_url);
    NDIlib_recv_create_v3_t settings{};
    settings.source_to_connect_to.p_ndi_name = name.empty() ? nullptr : name.c_str();
    settings.source_to_connect_to.p_url_address = url.empty() ? nullptr : url.c_str();
    settings.color_format = static_cast<NDIlib_recv_color_format_e>(
        NDIlib_recv_color_format_compressed_v4
    );
    settings.bandwidth = NDIlib_recv_bandwidth_highest;
    settings.allow_video_fields = false;
    settings.p_ndi_recv_name = "Network Stream Viewer Probe";

    NDIlib_recv_instance_t receiver = NDIlib_recv_create_v4(&settings, kReceiverConfig);
    if (receiver == nullptr) return nullptr;

    const auto deadline = std::chrono::steady_clock::now() +
        std::chrono::milliseconds(std::max(timeout_ms, 0));
    jint values[5]{};
    bool found_video = false;
    while (std::chrono::steady_clock::now() < deadline) {
        const auto remaining = std::chrono::duration_cast<std::chrono::milliseconds>(
            deadline - std::chrono::steady_clock::now()
        ).count();
        NDIlib_video_frame_v2_t video{};
        const NDIlib_frame_type_e type = NDIlib_recv_capture_v3(
            receiver,
            &video,
            nullptr,
            nullptr,
            static_cast<uint32_t>(std::max<int64_t>(remaining, 1))
        );
        if (type == NDIlib_frame_type_video) {
            values[0] = video.xres;
            values[1] = video.yres;
            values[2] = video.frame_rate_N;
            values[3] = video.frame_rate_D;
            values[4] = stream_format_code(video);
            found_video = video.xres > 0 && video.yres > 0 &&
                video.frame_rate_N > 0 && video.frame_rate_D > 0;
            NDIlib_recv_free_video_v2(receiver, &video);
            break;
        }
        if (type == NDIlib_frame_type_error) break;
    }
    NDIlib_recv_destroy(receiver);

    if (!found_video) return nullptr;
    jintArray result = env->NewIntArray(5);
    if (result != nullptr) env->SetIntArrayRegion(result, 0, 5, values);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_startReceiver(
    JNIEnv* env,
    jobject,
    jstring source_name,
    jstring source_url,
    jobject surface,
    jint bandwidth,
    jobject aspect_ratio_listener
) {
    std::scoped_lock lock(g_lifecycle_mutex);
    if (!g_initialized || surface == nullptr || aspect_ratio_listener == nullptr) return JNI_FALSE;
    stop_receiver_locked(env);

    const std::string name = from_java_string(env, source_name);
    const std::string url = from_java_string(env, source_url);
    NDIlib_recv_create_v3_t settings{};
    settings.source_to_connect_to.p_ndi_name = name.empty() ? nullptr : name.c_str();
    settings.source_to_connect_to.p_url_address = url.empty() ? nullptr : url.c_str();
    settings.color_format = static_cast<NDIlib_recv_color_format_e>(
        NDIlib_recv_color_format_compressed_v4
    );
    settings.bandwidth = bandwidth == 2
        ? NDIlib_recv_bandwidth_lowest
        : NDIlib_recv_bandwidth_highest;
    settings.allow_video_fields = false;
    settings.p_ndi_recv_name = "Network Stream Viewer";

    g_receiver = NDIlib_recv_create_v4(&settings, kReceiverConfig);
    if (g_receiver == nullptr) {
        log_error("NDIlib_recv_create_v4 failed");
        return JNI_FALSE;
    }

    g_window = ANativeWindow_fromSurface(env, surface);
    if (g_window == nullptr) {
        NDIlib_recv_destroy(g_receiver);
        g_receiver = nullptr;
        log_error("Could not acquire the Android surface");
        return JNI_FALSE;
    }

    const jclass listener_class = env->GetObjectClass(aspect_ratio_listener);
    g_aspect_ratio_method = env->GetMethodID(
        listener_class,
        "onVideoAspectRatioChanged",
        "(F)V"
    );
    g_playback_state_method = env->GetMethodID(
        listener_class,
        "onPlaybackStateChanged",
        "(I)V"
    );
    g_ptz_support_method = env->GetMethodID(
        listener_class,
        "onPtzSupportChanged",
        "(Z)V"
    );
    g_video_diagnostics_method = env->GetMethodID(
        listener_class,
        "onVideoDiagnosticsChanged",
        "(JJIIIFFF)V"
    );
    env->DeleteLocalRef(listener_class);
    if (g_aspect_ratio_method == nullptr || g_playback_state_method == nullptr ||
        g_ptz_support_method == nullptr || g_video_diagnostics_method == nullptr ||
        env->GetJavaVM(&g_java_vm) != JNI_OK) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        NDIlib_recv_destroy(g_receiver);
        g_receiver = nullptr;
        ANativeWindow_release(g_window);
        g_window = nullptr;
        g_aspect_ratio_method = nullptr;
        g_playback_state_method = nullptr;
        g_ptz_support_method = nullptr;
        g_video_diagnostics_method = nullptr;
        g_java_vm = nullptr;
        log_error("Could not prepare the playback callbacks");
        return JNI_FALSE;
    }
    g_playback_listener = env->NewGlobalRef(aspect_ratio_listener);
    if (g_playback_listener == nullptr) {
        NDIlib_recv_destroy(g_receiver);
        g_receiver = nullptr;
        ANativeWindow_release(g_window);
        g_window = nullptr;
        g_aspect_ratio_method = nullptr;
        g_playback_state_method = nullptr;
        g_ptz_support_method = nullptr;
        g_video_diagnostics_method = nullptr;
        g_java_vm = nullptr;
        return JNI_FALSE;
    }

    try {
        auto audio_capture = std::make_unique<NdiAudioCapture>();
        if (audio_capture->start(name, url, kReceiverConfig)) {
            g_audio_capture = std::move(audio_capture);
        } else {
            log_error("Could not initialize the NDI audio receiver; continuing with video");
        }
    } catch (...) {
        log_error("Could not allocate the NDI audio receiver; continuing with video");
    }

    g_receiving.store(true);
    g_receive_thread = std::thread(
        receive_loop,
        g_receiver,
        g_window,
        g_java_vm,
        g_playback_listener,
        g_aspect_ratio_method,
        g_playback_state_method,
        g_ptz_support_method,
        g_video_diagnostics_method,
        bandwidth == 0
    );
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_recallPtzPreset(
    JNIEnv*,
    jobject,
    jint preset_number,
    jfloat speed
) {
    if (preset_number < 0 || preset_number > 99 || !std::isfinite(speed) ||
        speed < 0.0f || speed > 1.0f) {
        return 3;
    }

    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;
    return NDIlib_recv_ptz_recall_preset(g_receiver, preset_number, speed) ? 0 : 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_storePtzPreset(
    JNIEnv*,
    jobject,
    jint preset_number
) {
    if (preset_number < 0 || preset_number > 99) return 3;

    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;
    return NDIlib_recv_ptz_store_preset(g_receiver, preset_number) ? 0 : 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_panTiltSpeed(
    JNIEnv*,
    jobject,
    jfloat pan_speed,
    jfloat tilt_speed
) {
    if (!std::isfinite(pan_speed) || !std::isfinite(tilt_speed) ||
        pan_speed < -1.0f || pan_speed > 1.0f || tilt_speed < -1.0f || tilt_speed > 1.0f) {
        return 3;
    }
    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;
    return NDIlib_recv_ptz_pan_tilt_speed(g_receiver, pan_speed, tilt_speed) ? 0 : 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_zoomSpeed(
    JNIEnv*,
    jobject,
    jfloat zoom_speed
) {
    if (!std::isfinite(zoom_speed) || zoom_speed < -0.5f || zoom_speed > 0.5f) return 3;
    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;
    return NDIlib_recv_ptz_zoom_speed(g_receiver, zoom_speed) ? 0 : 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_focusSpeed(
    JNIEnv*,
    jobject,
    jfloat focus_speed
) {
    if (!std::isfinite(focus_speed) || focus_speed < -0.5f || focus_speed > 0.5f) return 3;
    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;
    return NDIlib_recv_ptz_focus_speed(g_receiver, focus_speed) ? 0 : 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_focus(
    JNIEnv*,
    jobject,
    jfloat focus_value
) {
    if (!std::isfinite(focus_value) || focus_value < 0.0f || focus_value > 1.0f) return 3;
    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;
    return NDIlib_recv_ptz_focus(g_receiver, focus_value) ? 0 : 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_autoFocus(JNIEnv*, jobject) {
    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;
    return NDIlib_recv_ptz_auto_focus(g_receiver) ? 0 : 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_whiteBalanceAuto(JNIEnv*, jobject) {
    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;
    return NDIlib_recv_ptz_white_balance_auto(g_receiver) ? 0 : 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_whiteBalanceIndoor(JNIEnv*, jobject) {
    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;
    return NDIlib_recv_ptz_white_balance_indoor(g_receiver) ? 0 : 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_whiteBalanceOutdoor(JNIEnv*, jobject) {
    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;
    return NDIlib_recv_ptz_white_balance_outdoor(g_receiver) ? 0 : 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_whiteBalanceOneShot(JNIEnv*, jobject) {
    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;
    return NDIlib_recv_ptz_white_balance_oneshot(g_receiver) ? 0 : 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_whiteBalanceManual(
    JNIEnv*,
    jobject,
    jfloat red,
    jfloat blue
) {
    if (!std::isfinite(red) || !std::isfinite(blue) || red < 0.0f || red > 1.0f ||
        blue < 0.0f || blue > 1.0f) return 3;
    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;
    return NDIlib_recv_ptz_white_balance_manual(g_receiver, red, blue) ? 0 : 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_stopPtzMovement(JNIEnv*, jobject) {
    std::shared_lock lock(g_lifecycle_mutex);
    if (g_receiver == nullptr || !NDIlib_recv_ptz_is_supported(g_receiver)) return 1;

    const bool pan_tilt_stopped = NDIlib_recv_ptz_pan_tilt_speed(g_receiver, 0.0f, 0.0f);
    const bool zoom_stopped = NDIlib_recv_ptz_zoom_speed(g_receiver, 0.0f);
    const bool focus_stopped = NDIlib_recv_ptz_focus_speed(g_receiver, 0.0f);
    return pan_tilt_stopped && zoom_stopped && focus_stopped ? 0 : 2;
}

extern "C" JNIEXPORT void JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_stopReceiver(JNIEnv* env, jobject) {
    std::scoped_lock lock(g_lifecycle_mutex);
    stop_receiver_locked(env);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_fillAudioBuffer(
    JNIEnv* env,
    jobject,
    jobject buffer,
    jint sample_rate,
    jint channel_count,
    jint samples_per_channel
) {
    std::shared_lock lock(g_lifecycle_mutex);
    if (buffer == nullptr || sample_rate < 8000 || sample_rate > 192000 ||
        channel_count < 1 || channel_count > 2 || samples_per_channel < 1 ||
        samples_per_channel > 4096) {
        return -2;
    }
    void* address = env->GetDirectBufferAddress(buffer);
    const jlong capacity = env->GetDirectBufferCapacity(buffer);
    if (address == nullptr || capacity < 0) return -2;
    const jlong required_bytes = static_cast<jlong>(channel_count) *
        static_cast<jlong>(samples_per_channel) * 2;
    if (required_bytes <= 0 || capacity < required_bytes) return -2;
    if (g_audio_capture == nullptr) return -1;
    return g_audio_capture->fill_interleaved_16s(
        static_cast<int16_t*>(address),
        static_cast<size_t>(required_bytes / 2),
        sample_rate,
        channel_count,
        samples_per_channel
    );
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_getAudioPerformance(JNIEnv* env, jobject) {
    std::shared_lock lock(g_lifecycle_mutex);
    if (g_audio_capture == nullptr) return env->NewLongArray(0);
    const auto performance = g_audio_capture->performance();
    const jlong values[] = {performance.first, performance.second};
    jlongArray result = env->NewLongArray(2);
    if (result != nullptr) env->SetLongArrayRegion(result, 0, 2, values);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_startSender(
    JNIEnv* env,
    jobject,
    jstring sender_name
) {
    std::scoped_lock lifecycle_lock(g_lifecycle_mutex);
    if (!g_initialized) return JNI_FALSE;

    const std::string name = from_java_string(env, sender_name);
    if (name.empty()) return JNI_FALSE;

    std::scoped_lock sender_lock(g_sender_mutex);
    stop_sender_locked();

    NDIlib_send_create_t settings{};
    settings.p_ndi_name = name.c_str();
    settings.p_groups = nullptr;
    settings.clock_video = true;
    settings.clock_audio = false;
    g_sender = NDIlib_send_create(&settings);
    if (g_sender == nullptr) {
        log_error("NDIlib_send_create failed");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_sendVideoFrame(
    JNIEnv* env,
    jobject,
    jbyteArray nv12_data,
    jint width,
    jint height,
    jint frame_rate
) {
    if (nv12_data == nullptr || width <= 0 || height <= 0 ||
        width % 2 != 0 || height % 2 != 0 || frame_rate <= 0) {
        return JNI_FALSE;
    }

    const jlong expected_size =
        static_cast<jlong>(width) * static_cast<jlong>(height) * 3L / 2L;
    if (env->GetArrayLength(nv12_data) < expected_size) return JNI_FALSE;

    std::scoped_lock sender_lock(g_sender_mutex);
    if (g_sender == nullptr) return JNI_FALSE;

    jbyte* data = env->GetByteArrayElements(nv12_data, nullptr);
    if (data == nullptr) return JNI_FALSE;

    NDIlib_video_frame_v2_t frame{};
    frame.xres = width;
    frame.yres = height;
    frame.FourCC = NDIlib_FourCC_type_NV12;
    frame.frame_rate_N = frame_rate;
    frame.frame_rate_D = 1;
    frame.picture_aspect_ratio = static_cast<float>(width) / static_cast<float>(height);
    frame.frame_format_type = NDIlib_frame_format_type_progressive;
    frame.timecode = NDIlib_send_timecode_synthesize;
    frame.p_data = reinterpret_cast<uint8_t*>(data);
    frame.line_stride_in_bytes = width;
    frame.p_metadata = nullptr;
    frame.timestamp = 0;
    NDIlib_send_send_video_v2(g_sender, &frame);

    env->ReleaseByteArrayElements(nv12_data, data, JNI_ABORT);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_senderConnectionCount(JNIEnv*, jobject) {
    std::scoped_lock lock(g_sender_mutex);
    if (g_sender == nullptr) return 0;
    return NDIlib_send_get_no_connections(g_sender, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_stopSender(JNIEnv*, jobject) {
    std::scoped_lock lock(g_sender_mutex);
    stop_sender_locked();
}

extern "C" JNIEXPORT void JNICALL
Java_com_adriant_networkstreamviewer_data_ndi_NdiNative_shutdown(JNIEnv* env, jobject) {
    std::scoped_lock lifecycle_lock(g_lifecycle_mutex);
    stop_receiver_locked(env);

    std::scoped_lock sender_lock(g_sender_mutex);
    stop_sender_locked();

    std::scoped_lock finder_lock(g_finder_mutex);
    if (g_finder != nullptr) {
        NDIlib_find_destroy(g_finder);
        g_finder = nullptr;
    }
    if (g_initialized) {
        NDIlib_destroy();
        g_initialized = false;
    }
}
