package com.s04348.ndk_ocl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLES31;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.stream.IntStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Renderer2D implements GLSurfaceView.Renderer {
    private TextureDrawer textureDrawer;
    private int outputTexture, width, height;
    private String shaderCode;
    private MeshContainer meshContainer;

    public Renderer2D(Context context){
        shaderCode = Utils.readFileFromAsset(context, "raytracing.cl");
        meshContainer = new MeshContainer(new ObjLoader(context), 3, 1,"mesh1.obj", "mesh2.obj", "mesh3.obj");
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        this.height = 240;
        this.width = 240;
        this.textureDrawer = new TextureDrawer();

        GLES31.glDisable(GLES31.GL_DEPTH_TEST);
        allocTexture(width,height);
        System.err.println("OpenCl init : " + initOpenCL(shaderCode));
        setupTexture(outputTexture, width, height);

        createScene(meshContainer.vertexBuffer, meshContainer.faceBuffer, meshContainer.materialBuffer,
                    meshContainer.objectBuffer, meshContainer.objectSizeBuffer);

        updateScene(meshContainer.objectToDrawBuffer, meshContainer.objectToDrawMaterialBuffer, meshContainer.objectToDrawTransformBuffer);
    }

    public void onDrawFrame(GL10 unused) {
        computeRT();
        textureDrawer.drawTexture(outputTexture);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        resizeTexture(width, height);
    }

    public void allocTexture(int width, int height){
        final int[] textureHandle = new int[1];
        GLES31.glGenTextures(1, textureHandle, 0);
        outputTexture = textureHandle[0];

        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, outputTexture);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_NEAREST);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_NEAREST);
        GLES31.glTexStorage2D(GLES31.GL_TEXTURE_2D,1,GLES31.GL_RGBA8, width, height);
    }

    public void resizeTexture(int width, int height){
        this.width = width;
        this.height = height;
        GLES31.glDeleteTextures(1, new int[]{outputTexture}, 0);
        System.out.println("Resize " + this.width + ":" + this.height);
        allocTexture(this.width, this.height);
        setupTexture(outputTexture, this.width, this.height);
    }

    public native String initOpenCL(String shaderCode);

    public native void setupTexture(int texture, int width, int height);

    public native void computeRT();

    public native void createScene(float vertex[], short faces[], float materials[], short object[], short objectSize[]);

    public native void updateScene(short objectToDraw[], short objectMaterial[], float objectTransform[]);
}
