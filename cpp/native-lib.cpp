#include <jni.h>
#include <string>

#include "CL/cl.h"
#include "CL/cl_gl.h"
#include <EGL/egl.h>
#include <GLES3/gl31.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>

const cl_image_format format_object = {CL_RGBA, CL_FLOAT};
const cl_image_format format_ray = {CL_RGBA, CL_FLOAT};
cl_image_desc desc = {CL_MEM_OBJECT_IMAGE2D, 240, 240, 0, 0, 0, 0, 0, 0, NULL};

cl_context context;
cl_device_id device;
cl_int last_error;
cl_kernel kernel_ray;
cl_kernel kernel_trace;
cl_kernel kernel_shade;
size_t WorkSize[2] = { 240, 240 };
cl_command_queue commandQueue;
cl_mem texture;
cl_mem textureRay, textureNormal, textureHit;

int per_vertex = 3;
int per_material = 4;
int per_transform = 16;
cl_mem vertexBuffer, faceBuffer, objectBuffer, objectSizeBuffer, materialBuffer;
cl_mem objectToDrawBuffer, objectToDrawTransformBuffer, objectToDrawMaterialBuffer;
int objectToDrawSize = 0;

cl_context create_context(){
    cl_platform_id platform;
    clGetPlatformIDs (1, &platform, NULL);
    clGetDeviceIDs (platform, CL_DEVICE_TYPE_ALL, 1, &device, NULL);

    cl_context_properties contextProps[] = {
            CL_CONTEXT_PLATFORM, cl_context_properties(platform),
            CL_GL_CONTEXT_KHR, (cl_context_properties)eglGetCurrentContext(),
            CL_EGL_DISPLAY_KHR, (cl_context_properties)eglGetCurrentDisplay(),
            0
    };

    context = clCreateContext(contextProps, 1, &device, NULL, NULL, NULL);

    return context;
}

extern "C" JNIEXPORT void
JNICALL
Java_com_s04348_ndk_1ocl_Renderer2D_createScene(JNIEnv *env, jobject /* this */,  jfloatArray vertex,
                             jshortArray face, jfloatArray material, jshortArray object, jshortArray object_sizes){

    float* vertexArray = env->GetFloatArrayElements(vertex, NULL);
    int vertexSize = env->GetArrayLength(vertex);

    short* faceArray = env->GetShortArrayElements(face, NULL);
    int faceSize = env->GetArrayLength(face);

    float* materialArray = env->GetFloatArrayElements(material, NULL);
    int materialSize = env->GetArrayLength(material);

    short* objectArray = env->GetShortArrayElements(object, NULL);
    int objectSize = env->GetArrayLength(object);

    short* objectSizeArray = env->GetShortArrayElements(object_sizes, NULL);
    int objectSizeSize = env->GetArrayLength(object_sizes);

    vertexBuffer = clCreateBuffer (context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, sizeof(float) * vertexSize, vertexArray, &last_error);
    faceBuffer = clCreateBuffer (context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, sizeof(short) * faceSize, faceArray, &last_error);
    materialBuffer = clCreateBuffer (context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, sizeof(float) * materialSize, materialArray, &last_error);
    objectBuffer = clCreateBuffer (context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, sizeof(short) * objectSize, objectArray, &last_error);
    objectSizeBuffer = clCreateBuffer (context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, sizeof(short) * objectSizeSize, objectSizeArray, &last_error);

    free(vertexArray);
    free(faceArray);
    free(materialArray);
    free(objectArray);
    free(objectSizeArray);
}

extern "C" JNIEXPORT void
JNICALL
Java_com_s04348_ndk_1ocl_Renderer2D_updateScene(JNIEnv *env, jobject /* this */,
                                                jshortArray object, jshortArray material, jfloatArray transform){
    clReleaseMemObject (objectToDrawBuffer);
    clReleaseMemObject (objectToDrawMaterialBuffer);
    clReleaseMemObject (objectToDrawTransformBuffer);

    short* objectToDrawArray = env->GetShortArrayElements(object, NULL);
    objectToDrawSize = env->GetArrayLength(object);

    short* objectMaterialArray = env->GetShortArrayElements(material, NULL);
    int objectMaterialSize = env->GetArrayLength(material);

    float* objectTransformArray = env->GetFloatArrayElements(transform, NULL);
    int objectTransformSize = env->GetArrayLength(transform);

    objectToDrawBuffer = clCreateBuffer (context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, sizeof(short) * objectToDrawSize, objectToDrawArray, &last_error);
    objectToDrawMaterialBuffer = clCreateBuffer (context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, sizeof(short) * objectMaterialSize, objectMaterialArray, &last_error);
    objectToDrawTransformBuffer = clCreateBuffer (context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, sizeof(float) * objectTransformSize, objectTransformArray, &last_error);

    free(objectToDrawArray);
    free(objectMaterialArray);
    free(objectTransformArray);
}

extern "C" JNIEXPORT jstring
JNICALL
Java_com_s04348_ndk_1ocl_Renderer2D_initOpenCL(JNIEnv *env, jobject /* this */,  jstring shaderCode){
    create_context();
    commandQueue = clCreateCommandQueue(context, device,0,0);

    const char *kernelSource[] = { env->GetStringUTFChars(shaderCode, NULL) };
    cl_program kernelProgram = clCreateProgramWithSource (context, 1, kernelSource, 0, 0);

    clBuildProgram (kernelProgram, 0, NULL, NULL, NULL, NULL);
    kernel_ray = clCreateKernel (kernelProgram, "rayKernel", &last_error);
    kernel_trace = clCreateKernel (kernelProgram, "traceKernel", &last_error);
    kernel_shade = clCreateKernel (kernelProgram, "shadeKernel", &last_error);

    char log[4096];
    int res = clGetProgramBuildInfo(kernelProgram, device, CL_PROGRAM_BUILD_LOG, 4096, log, NULL);

    return env->NewStringUTF(log);
}

extern "C" JNIEXPORT void
JNICALL
Java_com_s04348_ndk_1ocl_Renderer2D_setupTexture(JNIEnv *env, jobject /* this */, jint glTexture, jint width, jint height){
    clReleaseMemObject (texture);
    clReleaseMemObject (textureRay);
    clReleaseMemObject (textureNormal);
    clReleaseMemObject (textureHit);

    desc.image_height = (uint)height;
    desc.image_width = (uint)width;
    textureRay = clCreateImage(context, CL_MEM_READ_WRITE, &format_ray, &desc, NULL, &last_error);
    textureNormal = clCreateImage(context, CL_MEM_READ_WRITE, &format_object, &desc, NULL, &last_error);
    textureHit = clCreateImage(context, CL_MEM_READ_WRITE, &format_object, &desc, NULL, &last_error);

    texture = clCreateFromGLTexture2D(context, CL_MEM_WRITE_ONLY, GL_TEXTURE_2D, 0, (uint)glTexture, &last_error);
    WorkSize[0] = (uint)width;
    WorkSize[1] = (uint)height;
}

extern "C" JNIEXPORT void
JNICALL
Java_com_s04348_ndk_1ocl_Renderer2D_computeRT(JNIEnv *env, jobject /* this */){

    clSetKernelArg (kernel_ray, 0, sizeof(textureRay), &textureRay);
    clEnqueueNDRangeKernel(commandQueue, kernel_ray, 2, 0, WorkSize, 0, 0, 0, 0);
    int res;
    res = clSetKernelArg (kernel_trace, 0, sizeof(textureRay), &textureRay);

    res = clSetKernelArg (kernel_trace, 1, sizeof(vertexBuffer), &vertexBuffer);
    res = clSetKernelArg (kernel_trace, 2, sizeof(faceBuffer), &faceBuffer);
    res = clSetKernelArg (kernel_trace, 3, sizeof(objectBuffer), &objectBuffer);
    res = clSetKernelArg (kernel_trace, 4, sizeof(objectSizeBuffer), &objectSizeBuffer);

    res = clSetKernelArg (kernel_trace, 5, sizeof(objectToDrawBuffer), &objectToDrawBuffer);

    res = clSetKernelArg (kernel_trace, 6, sizeof(int), &objectToDrawSize);

    res = clSetKernelArg (kernel_trace, 7, sizeof(textureHit), &textureHit);
    res = clSetKernelArg (kernel_trace, 8, sizeof(textureNormal), &textureNormal);
    clEnqueueNDRangeKernel(commandQueue, kernel_trace, 2, 0, WorkSize, 0, 0, 0, 0);

    clEnqueueAcquireGLObjects(commandQueue, 1, &texture, 0, NULL, NULL);

    res = clSetKernelArg (kernel_shade, 0, sizeof(textureHit), &textureHit);
    res = clSetKernelArg (kernel_shade, 1, sizeof(textureNormal), &textureNormal);
    //clSetKernelArg (kernel_shade, 2, sizeof(sceneBuffer), &sceneBuffer);
    res = clSetKernelArg (kernel_shade, 2, sizeof(texture), &texture);
    clEnqueueNDRangeKernel(commandQueue, kernel_shade, 2, 0, WorkSize, 0, 0, 0, 0);

    clEnqueueReleaseGLObjects(commandQueue, 1, &texture, 0, NULL, NULL);
    clFinish(commandQueue);
}
/*
std::string maincl () {
    // creer un contexte
    cl_platform_id platform;
    clGetPlatformIDs (1, &platform, NULL);
    cl_device_id device;
    clGetDeviceIDs (platform, CL_DEVICE_TYPE_ALL, 1, &device, NULL);
    cl_context context = clCreateContext (0, 1, &device, NULL, NULL, NULL);

    //creer une file de commandes

    // allouer et initialiser la memoire du device
    float inputDataHost[DATA_SIZE];
    fillData(inputDataHost, DATA_SIZE);
    cl_mem inputBufferDevice = clCreateBuffer (context, CL_MEM_READ_ONLY |
                                                        CL_MEM_COPY_HOST_PTR, sizeof(float) * DATA_SIZE, inputDataHost, 0);
    cl_mem outputBufferDevice = clCreateBuffer (context, CL_MEM_WRITE_ONLY,
                                                sizeof (float) * DATA_SIZE, 0, 0);
    // charger et compiler le kernel
    cl_program kernelProgram = clCreateProgramWithSource (context, 4,
                                                          kernelSource, 0, 0);
    clBuildProgram (kernelProgram, 0, NULL, NULL, NULL, NULL);
    cl_kernel kernel = clCreateKernel (kernelProgram, "add42", NULL);
    clSetKernelArg (kernel, 0, sizeof (cl_mem), (void *) &inputBufferDevice);
    clSetKernelArg (kernel, 1, sizeof (cl_mem), (void *) &outputBufferDevice);
    // ajouter le kernel dans la file de commandes
    size_t WorkSize[1] = { DATA_SIZE };
    clEnqueueNDRangeKernel (commandQueue, kernel, 1, 0, WorkSize, 0, 0, 0, 0);
    // recuperer les donnees calculees dans la memoire du device
    float outputDataHost[DATA_SIZE];
    clEnqueueReadBuffer (commandQueue, outputBufferDevice, CL_TRUE, 0,
                         DATA_SIZE * sizeof (float), outputDataHost, 0, NULL, NULL);
    // liberer les ressources
    clReleaseKernel (kernel);
    clReleaseProgram (kernelProgram);
    clReleaseCommandQueue (commandQueue);
    clReleaseMemObject (inputBufferDevice);
    clReleaseMemObject (outputBufferDevice);
    clReleaseContext (context);
    // validation
    int i;
    for (i=0; i<DATA_SIZE; i++)
        if (fabs((inputDataHost[i] + 42.f) - outputDataHost[i]) > 1e-2)
            break;
    std::string text;
    if (i == DATA_SIZE)
        text = "passed\n";
    else
        text = "failed\n";

    return text;
}
*/