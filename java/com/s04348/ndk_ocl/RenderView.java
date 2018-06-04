package com.s04348.ndk_ocl;

import android.content.Context;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;
import android.view.View;

import static android.opengl.EGL14.eglGetCurrentContext;
import static android.opengl.EGL14.eglGetCurrentDisplay;

public class RenderView extends GLSurfaceView {
    private final Renderer2D mRenderer;

    public RenderView(Context context){
        super(context);
        setEGLContextClientVersion(3);
        this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mRenderer = new Renderer2D(context);
        setRenderer(mRenderer);
    }

    public Renderer2D getRenderer() {
        return mRenderer;
    }
}