package com.s04348.ndk_ocl;

import android.opengl.Matrix;

import java.util.ArrayList;
import java.util.List;

public class MeshContainer {
    float[] vertexBuffer; int per_vertex = 3;
    short[] faceBuffer;
    short[] objectBuffer, objectSizeBuffer;
    float[] materialBuffer; int per_material = 4;

    short[] objectToDrawBuffer;
    float[] objectToDrawTransformBuffer; int per_transform = 16;
    short[] objectToDrawMaterialBuffer;

    public MeshContainer(ObjLoader loader, int nb_object_to_draw, int nb_material, String... objs){
        List<short[]> faces = new ArrayList<>();
        List<float[]> vertices = new ArrayList<>();
        int nb_face = 0, nb_vertex = 0;
        for(String obj : objs) {
            loader.load(obj);
            faces.add(loader.ind);
            nb_face += loader.ind.length;
            vertices.add(loader.verts);
            nb_vertex += loader.verts.length;
        }
        vertexBuffer = new float[nb_vertex];
        faceBuffer = new short[nb_face];
        objectBuffer = new short[faces.size()];
        objectSizeBuffer = new short[faces.size()];
        int face_pos = 0, vertex_pos = 0;
        for(int i = 0; i < faces.size(); ++i){
            System.arraycopy(vertices.get(i), 0, vertexBuffer, vertex_pos, vertices.get(i).length);
            System.arraycopy(faces.get(i), 0, faceBuffer, face_pos, faces.get(i).length);
            for(int j = face_pos; j < face_pos+faces.get(i).length; ++j){
                faceBuffer[j] += vertex_pos/3;
                faceBuffer[j] *= 3;
            }
            objectBuffer[i] = (short)face_pos;
            objectSizeBuffer[i] = (short)(faces.get(i).length/3);
            face_pos += faces.get(i).length;
            vertex_pos += vertices.get(i).length;
        }

        initMaterials(1);
        initObjectToDraw(3);
    }

    private void initMaterials(int size){
        materialBuffer = new float[size*per_material];

        materialBuffer[0] = 0.0f;
        materialBuffer[1] = 0.0f;
        materialBuffer[2] = 1.0f;
        materialBuffer[3] = 50.0f;
    }

    private void initObjectToDraw(int size){
        objectToDrawBuffer = new short[size];
        objectToDrawMaterialBuffer = new short[size];
        objectToDrawTransformBuffer = new float[size*per_transform];

        objectToDrawBuffer[0] = 0;
        objectToDrawBuffer[1] = 1;
        objectToDrawBuffer[2] = 2;

        objectToDrawMaterialBuffer[0] = 0;
        objectToDrawMaterialBuffer[1] = 0;
        objectToDrawMaterialBuffer[2] = 0;

        for(int i = 0; i < size; ++i){
            Matrix.setIdentityM(objectToDrawTransformBuffer, i*per_transform);
        }
    }
}
