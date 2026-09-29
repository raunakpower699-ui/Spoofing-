#include <jni.h>
#include <string>
#include <vector>
#include <thread>
#include <atomic>
#include <cmath>
#include <android/log.h>

#define TAG "MonsterNativeEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::atomic<bool> g_is_stress_active{false};
static std::vector<std::thread> g_worker_threads;
static std::atomic<uint64_t> g_total_iterations{0};

// Intensive floating point matrix & trigonometric math loop
void native_stress_worker(int core_id) {
    LOGI("Native worker thread spawned on logical core: %d", core_id);

    double accumulator = 1.00007 + (core_id * 0.0001);
    uint64_t local_iterations = 0;

    while (g_is_stress_active.load(std::memory_order_relaxed)) {
        // Unrolled floating point operations keeping ALU & FPU pipelines saturated
        for (int i = 0; i < 2000; ++i) {
            double angle = (local_iterations + i) * 0.00174532925; // radians
            accumulator = std::sqrt(std::abs(accumulator * accumulator + std::sin(angle) * std::cos(angle) + 0.00001));
            
            // Prevent infinity or denormal floating point slows
            if (accumulator > 100000.0 || std::isnan(accumulator)) {
                accumulator = 1.00007;
            }
        }
        local_iterations += 2000;
        g_total_iterations.fetch_add(2000, std::memory_order_relaxed);
    }

    LOGI("Native worker thread on core %d terminated gracefully.", core_id);
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_service_NativeStressService_startNativeStress(JNIEnv *env, jobject thiz) {
    if (g_is_stress_active.load()) {
        LOGW("Native stress engine is already active.");
        return JNI_TRUE;
    }

    g_is_stress_active.store(true);
    g_worker_threads.clear();

    unsigned int num_cores = std::thread::hardware_concurrency();
    if (num_cores == 0) {
        num_cores = 4; // Sensible fallback
    }

    LOGI("Starting low-level Monster Turbo stress on %u cores", num_cores);

    for (unsigned int i = 0; i < num_cores; ++i) {
        g_worker_threads.emplace_back(native_stress_worker, static_cast<int>(i));
    }

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_service_NativeStressService_stopNativeStress(JNIEnv *env, jobject thiz) {
    if (!g_is_stress_active.load()) {
        return JNI_FALSE;
    }

    LOGI("Stopping native stress threads...");
    g_is_stress_active.store(false);

    for (auto &t : g_worker_threads) {
        if (t.joinable()) {
            t.join();
        }
    }
    g_worker_threads.clear();
    LOGI("All native stress threads successfully joined and stopped.");

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_service_NativeStressService_isNativeStressRunning(JNIEnv *env, jobject thiz) {
    return static_cast<jboolean>(g_is_stress_active.load());
}

JNIEXPORT jint JNICALL
Java_com_example_service_NativeStressService_getHardwareCoreCount(JNIEnv *env, jobject thiz) {
    unsigned int cores = std::thread::hardware_concurrency();
    return static_cast<jint>(cores > 0 ? cores : 4);
}

// Fallback exports in case called from MainActivity directly
JNIEXPORT jboolean JNICALL
Java_com_example_MainActivity_startNativeStress(JNIEnv *env, jobject thiz) {
    return Java_com_example_service_NativeStressService_startNativeStress(env, thiz);
}

JNIEXPORT jboolean JNICALL
Java_com_example_MainActivity_stopNativeStress(JNIEnv *env, jobject thiz) {
    return Java_com_example_service_NativeStressService_stopNativeStress(env, thiz);
}

}
