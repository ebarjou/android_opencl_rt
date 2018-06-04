package com.s04348.ndk_ocl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class Utils {
    public static <T>int findFirstNull(T[] array){
        for (int i = 0; i < array.length; ++i) {
            if(array[i] == null) return i;
        }
        return -1;
    }

    public static int findFirstNegative(int[] array){
        for (int i = 0; i < array.length; ++i) {
            if(array[i] < 0) return i;
        }
        return -1;
    }

    public static FloatBuffer allocFloatBuffer(int capacity){
        ByteBuffer bb = ByteBuffer.allocateDirect(capacity*Float.BYTES);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.position(0);
        return buffer;
    }

    public static ShortBuffer allocShortBuffer(int capacity){
        ByteBuffer bb = ByteBuffer.allocateDirect(capacity*Short.BYTES);
        bb.order(ByteOrder.nativeOrder());
        ShortBuffer buffer = bb.asShortBuffer();
        buffer.position(0);
        return buffer;
    }

    public static IntBuffer allocIntBuffer(int capacity){
        ByteBuffer bb = ByteBuffer.allocateDirect(capacity*Integer.BYTES);
        bb.order(ByteOrder.nativeOrder());
        IntBuffer buffer = bb.asIntBuffer();
        buffer.position(0);
        return buffer;
    }

    public static int loadTextureFromBitmap(Bitmap bitmap, int format)
    {
        final int[] textureHandle = new int[1];

        GLES30.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle[0]);

            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, format, bitmap, 0);

            //bitmap.recycle();
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    public static String readFileFromAsset(Context context, String path){
        try {
            InputStream inputStream = context.getAssets().open(path);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null)
                stringBuilder.append(line + "\n");
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
