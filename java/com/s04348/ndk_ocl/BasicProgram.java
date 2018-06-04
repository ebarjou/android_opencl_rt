package com.s04348.ndk_ocl;

public class BasicProgram {
    public static final String vertexShaderCode =
            "#version 300 es\n"+
                    "layout (location = 0) in vec4 vPosition;"+
                    "out vec2 fTexcoord;"+
                    "void main() {" +
                    "   gl_Position = vec4(vPosition.xy, 0, 1);" +
                    "   fTexcoord = vPosition.zw;"+
                    "}";
    public static final String fragmentShaderCode =
            "#version 300 es\n precision mediump float;" +
                    "in vec2 fTexcoord;"+
                    "uniform sampler2D uTexture;"+
                    "out vec4 outColor;"+
                    "void main() {" +
                    "   outColor = texture(uTexture, fTexcoord);"+
                    "}";
    public static final int COORDS_PER_VERTEX = 4;
    public static final float vertexArray[] = {
            -1.0f,  1.0f, 0.0f, 0.0f,
            -1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 1.0f, 1.0f,
            1.0f,  1.0f, 1.0f, 0.0f, };
    public static final short faceArray[] = { 0, 1, 2, 0, 2, 3 };
    public static final int mPositionHandle = 0;

}
