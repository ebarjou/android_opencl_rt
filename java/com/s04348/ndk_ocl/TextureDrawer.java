package com.s04348.ndk_ocl;

import android.opengl.GLES31;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class TextureDrawer {
    public final int vertexVBO, faceVBO, glProgram, faceBufferSize, mTextureUniformHandle;

    public TextureDrawer(){
        GLES31.glClearColor(0.3f, 0.3f, 0.3f, 1.0f);

        glProgram = GLES31.glCreateProgram();

        int vertexShader = GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER);
        GLES31.glShaderSource(vertexShader, BasicProgram.vertexShaderCode);
        GLES31.glCompileShader(vertexShader);

        int fragmentShader = GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER);
        GLES31.glShaderSource(fragmentShader, BasicProgram.fragmentShaderCode);
        GLES31.glCompileShader(fragmentShader);

        GLES31.glAttachShader(glProgram, vertexShader);
        GLES31.glAttachShader(glProgram, fragmentShader);
        GLES31.glLinkProgram(glProgram);


        faceBufferSize = BasicProgram.faceArray.length;

        int[] vBufferId = new int[2];
        GLES31.glGenBuffers(2, vBufferId, 0);

        vertexVBO = vBufferId[0];
        FloatBuffer vertexBuffer = Utils.allocFloatBuffer( BasicProgram.vertexArray.length);
        vertexBuffer.put(BasicProgram.vertexArray);
        vertexBuffer.position(0);

        faceVBO = vBufferId[1];
        ShortBuffer faceBuffer = Utils.allocShortBuffer(BasicProgram.faceArray.length);
        faceBuffer.put(BasicProgram.faceArray);
        faceBuffer.position(0);

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, vertexVBO);
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, BasicProgram.vertexArray.length * Float.BYTES,
                vertexBuffer, GLES31.GL_STATIC_DRAW);
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, 0);

        GLES31.glBindBuffer(GLES31.GL_ELEMENT_ARRAY_BUFFER, faceVBO);
        GLES31.glBufferData(GLES31.GL_ELEMENT_ARRAY_BUFFER, BasicProgram.faceArray.length * Short.BYTES,
                faceBuffer, GLES31.GL_STATIC_DRAW);
        GLES31.glBindBuffer(GLES31.GL_ELEMENT_ARRAY_BUFFER, 0);

        mTextureUniformHandle =  GLES31.glGetUniformLocation(glProgram, "uTexture");
    }

    public void drawTexture(int texture){
        GLES31.glUseProgram(glProgram);
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, vertexVBO);

        GLES31.glEnableVertexAttribArray(BasicProgram.mPositionHandle);
        GLES31.glVertexAttribPointer(BasicProgram.mPositionHandle, BasicProgram.COORDS_PER_VERTEX,
                GLES31.GL_FLOAT,  false,
                BasicProgram.COORDS_PER_VERTEX * Float.BYTES, 0);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 );
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture);
        GLES31.glUniform1i(mTextureUniformHandle, 0);

        GLES31.glBindBuffer(GLES31.GL_ELEMENT_ARRAY_BUFFER, faceVBO);
        GLES31.glDrawElements(GLES31.GL_TRIANGLES, faceBufferSize, GLES31.GL_UNSIGNED_SHORT, 0);


        GLES31.glDisableVertexAttribArray(BasicProgram.mPositionHandle);
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, 0);
        GLES31.glBindBuffer(GLES31.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
}
