#include <android/log.h>
#include <jni.h>
#include <unistd.h>

#include <algorithm>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>

#include "llama.h"

namespace {
constexpr const char * LOG_TAG = "QuietTypeLlama";
constexpr int DEFAULT_HEADROOM_THREADS = 2;
constexpr int MAX_THREADS = 4;

std::mutex g_mutex;
bool g_backend_initialized = false;
llama_model * g_model = nullptr;
std::string g_model_path;

void log_callback(enum ggml_log_level level, const char * text, void * /*user_data*/) {
    int priority = ANDROID_LOG_INFO;
    switch (level) {
        case GGML_LOG_LEVEL_ERROR: priority = ANDROID_LOG_ERROR; break;
        case GGML_LOG_LEVEL_WARN: priority = ANDROID_LOG_WARN; break;
        case GGML_LOG_LEVEL_INFO: priority = ANDROID_LOG_INFO; break;
        default: priority = ANDROID_LOG_DEBUG; break;
    }
    __android_log_print(priority, LOG_TAG, "%s", text ? text : "");
}

void free_model_locked() {
    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
        g_model_path.clear();
    }
}

int ideal_threads() {
    const long cpus = sysconf(_SC_NPROCESSORS_ONLN);
    if (cpus <= 0) return 2;
    const int trimmed = static_cast<int>(cpus) - DEFAULT_HEADROOM_THREADS;
    return std::max(1, std::min(MAX_THREADS, trimmed));
}

std::string token_to_piece(const llama_vocab * vocab, llama_token token) {
    char buf[256];
    const int n = llama_token_to_piece(vocab, token, buf, sizeof(buf), 0, true);
    if (n < 0) {
        return std::string();
    }
    return std::string(buf, n);
}

std::vector<llama_token> tokenize_or_throw(const llama_vocab * vocab, const std::string & prompt) {
    const int needed = -llama_tokenize(vocab, prompt.c_str(), prompt.size(), nullptr, 0, true, true);
    if (needed <= 0) {
        throw std::runtime_error("Failed to tokenize prompt");
    }
    std::vector<llama_token> tokens(needed);
    const int actual = llama_tokenize(vocab, prompt.c_str(), prompt.size(), tokens.data(), tokens.size(), true, true);
    if (actual < 0) {
        throw std::runtime_error("Failed to tokenize prompt");
    }
    return tokens;
}
}

extern "C"
JNIEXPORT void JNICALL
Java_dk_schulz_quiettype_llama_LlamaAndroidBridge_nativeInit(JNIEnv * env, jobject, jstring nativeLibDir) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_backend_initialized) return;

    llama_log_set(log_callback, nullptr);
    const char * lib_dir = env->GetStringUTFChars(nativeLibDir, nullptr);
    ggml_backend_load_all_from_path(lib_dir);
    env->ReleaseStringUTFChars(nativeLibDir, lib_dir);
    llama_backend_init();
    g_backend_initialized = true;
}

extern "C"
JNIEXPORT jint JNICALL
Java_dk_schulz_quiettype_llama_LlamaAndroidBridge_nativeLoadModel(JNIEnv * env, jobject, jstring modelPath) {
    std::lock_guard<std::mutex> lock(g_mutex);
    const char * model_path = env->GetStringUTFChars(modelPath, nullptr);
    std::string requested_path(model_path ? model_path : "");
    env->ReleaseStringUTFChars(modelPath, model_path);

    if (g_model != nullptr && g_model_path == requested_path) {
        return 0;
    }

    free_model_locked();

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    g_model = llama_model_load_from_file(requested_path.c_str(), model_params);
    if (g_model == nullptr) {
        g_model_path.clear();
        return 1;
    }
    g_model_path = requested_path;
    return 0;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_dk_schulz_quiettype_llama_LlamaAndroidBridge_nativeComplete(
    JNIEnv * env,
    jobject,
    jstring prompt,
    jint maxTokens,
    jint contextSize,
    jfloat temperature,
    jint topK,
    jfloat topP
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_model == nullptr) {
        return env->NewStringUTF("");
    }

    try {
        const char * prompt_chars = env->GetStringUTFChars(prompt, nullptr);
        std::string prompt_text(prompt_chars ? prompt_chars : "");
        env->ReleaseStringUTFChars(prompt, prompt_chars);

        const llama_vocab * vocab = llama_model_get_vocab(g_model);
        std::vector<llama_token> prompt_tokens = tokenize_or_throw(vocab, prompt_text);
        const int requested_context = std::max<int>(contextSize, static_cast<int>(prompt_tokens.size()) + maxTokens + 8);

        llama_context_params ctx_params = llama_context_default_params();
        ctx_params.n_ctx = requested_context;
        ctx_params.n_batch = std::max<int>(1, std::min<int>(requested_context, static_cast<int>(prompt_tokens.size())));
        ctx_params.n_threads = ideal_threads();
        ctx_params.n_threads_batch = ctx_params.n_threads;
        ctx_params.no_perf = true;

        llama_context * ctx = llama_init_from_model(g_model, ctx_params);
        if (ctx == nullptr) {
            throw std::runtime_error("Failed to create llama context");
        }

        auto sampler_params = llama_sampler_chain_default_params();
        sampler_params.no_perf = true;
        llama_sampler * sampler = llama_sampler_chain_init(sampler_params);
        if (temperature <= 0.01f) {
            llama_sampler_chain_add(sampler, llama_sampler_init_greedy());
        } else {
            llama_sampler_chain_add(sampler, llama_sampler_init_top_k(topK));
            llama_sampler_chain_add(sampler, llama_sampler_init_top_p(topP, 1));
            llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
            llama_sampler_chain_add(sampler, llama_sampler_init_dist(1337));
        }

        llama_batch batch = llama_batch_get_one(prompt_tokens.data(), prompt_tokens.size());
        if (llama_model_has_encoder(g_model)) {
            if (llama_encode(ctx, batch) != 0) {
                llama_sampler_free(sampler);
                llama_free(ctx);
                throw std::runtime_error("Failed to encode prompt");
            }
            llama_token decoder_start_token_id = llama_model_decoder_start_token(g_model);
            if (decoder_start_token_id == LLAMA_TOKEN_NULL) {
                decoder_start_token_id = llama_vocab_bos(vocab);
            }
            batch = llama_batch_get_one(&decoder_start_token_id, 1);
        }

        std::string output;
        const int total_limit = static_cast<int>(prompt_tokens.size()) + maxTokens;
        for (int n_pos = 0; n_pos + batch.n_tokens < total_limit;) {
            if (llama_decode(ctx, batch) != 0) {
                llama_sampler_free(sampler);
                llama_free(ctx);
                throw std::runtime_error("llama_decode failed");
            }
            n_pos += batch.n_tokens;
            llama_token token = llama_sampler_sample(sampler, ctx, -1);
            if (llama_vocab_is_eog(vocab, token)) {
                break;
            }
            std::string piece = token_to_piece(vocab, token);
            if (piece.empty()) {
                break;
            }
            const auto newline = piece.find_first_of("\r\n");
            if (newline != std::string::npos) {
                output += piece.substr(0, newline);
                break;
            }
            output += piece;
            batch = llama_batch_get_one(&token, 1);
        }

        llama_sampler_free(sampler);
        llama_free(ctx);
        return env->NewStringUTF(output.c_str());
    } catch (const std::exception & e) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "nativeComplete failed: %s", e.what());
        return env->NewStringUTF("");
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_dk_schulz_quiettype_llama_LlamaAndroidBridge_nativeUnloadModel(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    free_model_locked();
}

extern "C"
JNIEXPORT void JNICALL
Java_dk_schulz_quiettype_llama_LlamaAndroidBridge_nativeShutdown(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    free_model_locked();
    if (g_backend_initialized) {
        llama_backend_free();
        g_backend_initialized = false;
    }
}
