#include <jni.h>
#include <string>
#include <cstring>
#include <format>
#include <deque>
#include <string>
#include <iostream>
#include <cmath>
#include <vector>
#include <linux/resource.h>
#include <sys/resource.h>

#define LOG_TAG "MemTest"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

struct MemTestData {
    int size;
    double deltaRunTime;
    double deltaUserCPU;
    double deltaSystemCPU;
    int sum1;
    int sum2;

    MemTestData(int _size, double _deltaRunTime, double _deltaUserCPU, double _deltaSystemCPU, int _sum1, int _sum2) :
            size(_size), deltaRunTime(_deltaRunTime), deltaUserCPU(_deltaUserCPU), deltaSystemCPU(_deltaSystemCPU) ,sum1(_sum1), sum2(_sum2) {}
    //MemTestData(const MemTestData& cpy) : size(cpy.size), delta(cpy.delta), sum1(cpy.sum1), sum2(cpy.sum2){}
};

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_apptest_MainActivity_runSweepMemTest(
        JNIEnv* env,
        jobject /* this */,
        jint loopIterations,
        jint initialBlockSize,
        jlong maxBlockSize) {

    //const int MAX_ITER = 10;
    //const int MAX_SIZE = 32 * 1024 * 1024;
    std::vector<MemTestData> results;


    //result << "buffer size,time,source sum, dest sum\n";

    int iters = 0;

    for (int size = initialBlockSize; size <= maxBlockSize; size *= 2) {
        char* src = new char[size];
        char* dst = new char[size];
        std::memset(src, 0, size);
        for(int i = 0; i <= size - 1024; i += 1024)
        {
            src[i] = 100;
        }

        int sourceSum = 0;

        for(int i = 0; i < size; i++){
            sourceSum += src[i];
        }
        struct rusage startUsage, endUsage;
        getrusage(RUSAGE_SELF, &startUsage);

        auto start = std::chrono::high_resolution_clock::now();
        for (int i = 0; i < loopIterations; ++i) {
            std::memcpy(dst, src, size);
        }
        auto end = std::chrono::high_resolution_clock::now();
        getrusage(RUSAGE_SELF, &endUsage);

        int destSum = 0;
        for(int i = 0; i < size; i++){
            destSum += dst[i];
        }

        std::chrono::duration<double> diffWrite = end - start;
        double deltaUserCPU = (endUsage.ru_utime.tv_sec - startUsage.ru_utime.tv_sec)+
                (endUsage.ru_utime.tv_usec - startUsage.ru_utime.tv_usec);

        double deltaSysCPU = (endUsage.ru_stime.tv_sec - startUsage.ru_stime.tv_sec) +
                (endUsage.ru_stime.tv_usec - startUsage.ru_stime.tv_usec);


        //LOGD("Memcpy size: %d, time: %f seconds", size, diff.count());
        results.emplace_back(size, diffWrite.count(), deltaUserCPU, deltaSysCPU, sourceSum, destSum);
        //result << size << "," << diff.count() << "," << sourceSum << "," << destSum << "\n";

        delete[] src;
        delete[] dst;
        ++iters;
    }

    // Find the MyStruct Java class
    jclass myStructClass = env->FindClass("com/example/apptest/MainActivity$JMemTestData");
    if (myStructClass == nullptr) {
        return nullptr; // Class not found
    }

    // Get the constructor of MyStruct(long, double, int, int)
    jmethodID constructor = env->GetMethodID(myStructClass, "<init>", "(IDDDII)V");
    if (constructor == nullptr) {
        return nullptr; // Constructor not found
    }

    // Create a new Java array of MyStruct
    jobjectArray structArray = env->NewObjectArray((int) results.size(), myStructClass, nullptr);
    if (structArray == nullptr) {
        return nullptr; // Out of memory error
    }

    // Populate the Java array with MyStruct objects
    for (int i = 0; i < results.size(); ++i) {
        jobject myStructObject = env->NewObject(myStructClass, constructor,
                                                static_cast<jint>(results[i].size),
                                                static_cast<jdouble>(results[i].deltaRunTime),
                                                static_cast<jdouble>(results[i].deltaUserCPU),
                                                static_cast<jdouble>(results[i].deltaSystemCPU),
                                                static_cast<jint>(results[i].sum1),
                                                static_cast<jint>(results[i].sum2));
        env->SetObjectArrayElement(structArray, i, myStructObject);
        env->DeleteLocalRef(myStructObject);
    }


    return structArray;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_example_apptest_MainActivity_runLoopMemTest(
    JNIEnv* env,
    jobject /* this */,
    jint loopIterations,
    jint blockSize) {


    volatile char* src = new char[blockSize];
    volatile char* dst = new char[blockSize];
    std::memset(const_cast<char*>(src), 0, blockSize);
    for(int i = 0; i <= blockSize - 1024; i += 1024)
    {
        src[i] = 100;
    }

    int sourceSum = 0;

    for(int i = 0; i < blockSize; i++){
        sourceSum += src[i];
    }

    auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < loopIterations; ++i) {
        std::memcpy(const_cast<char*>(dst), const_cast<char*>(src), blockSize);
    }
    auto end = std::chrono::high_resolution_clock::now();


    std::chrono::duration<double> diffWrite = end - start;

    int destSum = 0;
    for(int i = 0; i < blockSize; i++){
        destSum += dst[i];
    }

    if(sourceSum != destSum)
    {
        return 0.0;
    }

    delete[] src;
    delete[] dst;

    return static_cast<jdouble>(diffWrite.count());
}

int sumValue = 0;

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_apptest_MainActivity_runTimedMemTest(
        JNIEnv* env,
        jobject /* this */,
        jint runtimeMSecs,
        jint blockSize) {


    double runtimeSecs = runtimeMSecs;
    runtimeSecs /= 1000;

    volatile char* src = new char[blockSize];
    volatile char* dst = new char[blockSize];
    std::memset(const_cast<char*>(src), 0, blockSize);
    for(int i = 0; i < blockSize; i++)
    {
        src[i] = static_cast<int_fast8_t>(i % 0xff);
    }

    auto start = std::chrono::high_resolution_clock::now();
    int loopCount = 0;

    while(true) {
        if(loopCount %2 == 0)
        {
            for(int i = 0; i < blockSize; ++i)
            {
                dst[i] = src[i];
            }
        }
        else{
            for(int i = blockSize - 1; i >= 0; --i)
            {
                dst[i] = src[i];
            }
        }
        //std::memcpy(const_cast<char*>(dst), const_cast<char*>(src), blockSize);
        double elapsed_seconds = std::chrono::duration<double>(std::chrono::high_resolution_clock::now() - start).count();
        if(elapsed_seconds >= runtimeSecs) {
            break;
        }
        ++loopCount;
    }

    delete[] src;
    delete[] dst;

    int64_t totalBytesWritten = loopCount;
    totalBytesWritten *= static_cast<int64_t>(blockSize);

    return static_cast<jlong>(totalBytesWritten);
}




// Created by David Ronca on 7/14/24.
//
