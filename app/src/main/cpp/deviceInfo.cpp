#include <jni.h>
#include <string>
#include <cstring>
#include <format>
#include <android/log.h>
#include <sstream>
#include <cpu-features.h>
#include <deque>
#include <string>
#include <sys/system_properties.h>
#include <iostream>
#include <vector>
#include <sys/auxv.h>

#define LOG_TAG "MemTest"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
        Java_com_example_apptest_MainActivity_getCPUFamilyName(JNIEnv *env, jobject thiz){
    uint16_t cpuFamily = android_getCpuFamily();
    std::string  ret = "unknown";

    switch (cpuFamily) {

        case AndroidCpuFamily::ANDROID_CPU_FAMILY_UNKNOWN:
            ret = "Unknown";
            break;
        case AndroidCpuFamily::ANDROID_CPU_FAMILY_ARM:
            ret = "ARM";
            break;
        case AndroidCpuFamily::ANDROID_CPU_FAMILY_X86:
            ret = "X86";
            break;
        case AndroidCpuFamily::ANDROID_CPU_FAMILY_MIPS:
            ret = "MIPS";
            break;
        case AndroidCpuFamily::ANDROID_CPU_FAMILY_ARM64:
            ret = "ARM64";
            break;
        case AndroidCpuFamily::ANDROID_CPU_FAMILY_X86_64:
            ret = "X86-64";
            break;
        case AndroidCpuFamily::ANDROID_CPU_FAMILY_MIPS64:
            ret = "MIPS64";
            break;
        default:
             break;
    }

    jstring jret = env->NewStringUTF(ret.c_str());
    return jret;
}

std::deque<std::string> getArm32Features(uint64_t flags){
    std::deque<std::string> features;

    if(flags & ANDROID_CPU_ARM_FEATURE_ARMv7){
        features.emplace_back("ARMV7");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_VFPv3){
        features.emplace_back("VFPv3");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_NEON){
        features.emplace_back("NEON");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_LDREX_STREX){
        features.emplace_back("LDREX_STREX");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_VFPv2){
        features.emplace_back("VFPv2");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_VFP_D32){
        features.emplace_back("VFP_D32");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_VFP_FP16){
        features.emplace_back("FP16");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_VFP_FMA){
        features.emplace_back("VFP_FMA");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_NEON_FMA){
        features.emplace_back("NEON_FMA");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_IDIV_ARM){
        features.emplace_back("IDIV_ARM");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_NEON){
        features.emplace_back("NEON");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_IDIV_THUMB2){
        features.emplace_back("IDIV_THUMB2");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_IDIV_THUMB2){
        features.emplace_back("IDIV_THUMB2");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_iWMMXt){
        features.emplace_back("iWMMXt");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_AES){
        features.emplace_back("AES");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_PMULL){
        features.emplace_back("PMULL");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_SHA1){
        features.emplace_back("SHA1");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_SHA2){
        features.emplace_back("SHA2");
    }
    if(flags & ANDROID_CPU_ARM_FEATURE_CRC32){
        features.emplace_back("CRC32");
    }

    return features;
}

std::deque<std::string> getArm64Features(uint64_t flags){
    std::deque<std::string> ret;

    if(flags & ANDROID_CPU_ARM64_FEATURE_FP){
        ret.emplace_back("FP");
    }
    if(flags & ANDROID_CPU_ARM64_FEATURE_ASIMD){
        ret.emplace_back("ASIMD");
    }
    if(flags & ANDROID_CPU_ARM64_FEATURE_AES){
        ret.emplace_back("AES");
    }
    if(flags & ANDROID_CPU_ARM64_FEATURE_PMULL){
        ret.emplace_back("PMULL");
    }
    if(flags & ANDROID_CPU_ARM64_FEATURE_SHA1){
        ret.emplace_back("SHA1");
    }
    if(flags & ANDROID_CPU_ARM64_FEATURE_SHA2){
        ret.emplace_back("SHA2");
    }
    if(flags & ANDROID_CPU_ARM64_FEATURE_CRC32){
        ret.emplace_back("CRC32");
    }

    return ret;
}

extern "C" JNIEXPORT jobjectArray JNICALL
        Java_com_example_apptest_MainActivity_getArmFeatures(JNIEnv *env, jobject thiz){
    // Find the ArrayList class and constructor
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");

    // Create a new ArrayList object
    auto ret =  (jobjectArray ) env->NewObject(arrayListClass, arrayListConstructor);

    uint16_t cpuFamily = android_getCpuFamily();

    std::deque<std::string> armFeatures;

    if(cpuFamily == ANDROID_CPU_FAMILY_ARM){
        armFeatures = getArm32Features(android_getCpuFeatures());
    }
    else if(cpuFamily == ANDROID_CPU_FAMILY_ARM64)
    {
        armFeatures = getArm64Features(android_getCpuFeatures());
    }

    for (const auto& str : armFeatures) {
        // Convert std::string to jstring
        jstring jstr = env->NewStringUTF(str.c_str());

        // Add the jstring to the ArrayList
        env->CallBooleanMethod(ret, arrayListAdd, jstr);

        // Delete the local reference to jstring
        env->DeleteLocalRef(jstr);

    }

    return ret;
}


extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_apptest_MainActivity_isNeonSupported(JNIEnv *env, jobject thiz) {
    uint16_t cpuFamily = android_getCpuFamily();

    uint64_t features = android_getCpuFeatures();
    return (features & ANDROID_CPU_ARM_FEATURE_NEON) != 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_apptest_MainActivity_getModel(JNIEnv* env, jobject /* this */) {
    char model[PROP_VALUE_MAX];
    __system_property_get("ro.product.model", model);
    return env->NewStringUTF(model);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_apptest_MainActivity_getSoCInfo(JNIEnv* env, jobject /* this */) {
    char soc_name[PROP_VALUE_MAX];
    char board[PROP_VALUE_MAX];
    char manufacturer[PROP_VALUE_MAX];
    char model[PROP_VALUE_MAX];
    char cpuInfo[PROP_VALUE_MAX];

    __system_property_get("ro.board.platform", soc_name);
    __system_property_get("ro.product.board", board);
    __system_property_get("ro.product.manufacturer", manufacturer);
    __system_property_get("ro.product.model", model);
    unsigned long cpu_part = getauxval(AT_HWCAP);
    std::ostringstream strCpuPart;
    strCpuPart << std::hex << cpu_part;

    std::string info =  "Manufacturer: " + std::string(manufacturer) + "\n" +
                        "Model: " + std::string(model) + "\n" +
                        "SoC Name: " + std::string(soc_name) + "\n" +
                        "Board: " + std::string(board)  + "\n" +
                        "CPU Part: " + strCpuPart.str();

    return env->NewStringUTF(info.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_apptest_MainActivity_getSoCName(JNIEnv* env, jobject /* this */) {
    char soc_name[PROP_VALUE_MAX];
    int len = __system_property_get("ro.board.platform", soc_name);
    std::string info = "SoC Name (direct): ";
    if (len > 0) {
        info += soc_name;;
    } else {
        info+= "Unknown";
    }

    return env->NewStringUTF(info.c_str());
}