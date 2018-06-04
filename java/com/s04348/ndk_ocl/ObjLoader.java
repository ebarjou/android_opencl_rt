package com.s04348.ndk_ocl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import android.content.Context;

public class ObjLoader {

	Context context;
	ArrayList<Vector3> VertexStore;
	ArrayList<Face> FaceStore;
	
	short[] ind;
	float[] verts;
	
	public ObjLoader(Context context) {
		this.context = context;
	}

	public void load(String file) {
		VertexStore = new ArrayList<Vector3>();
		FaceStore = new ArrayList<Face>();
		ind = null;
		verts = null;

		InputStream is;
		try {
			is = context.getAssets().open(file);
		}catch (Exception e){
			e.printStackTrace();
			return;
		}

		StringBuilder model = new StringBuilder("");
		String line;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			line = reader.readLine();
			while(line != null)
			{
				model.append(line);
				model.append("\n");
				line = reader.readLine();
			}
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		parseModel(model.toString());
	}
	
	public void parseModel(String model){
		String[] lines = model.split("\n");
		int len = lines.length;
		
		for (int i = 0; i < len; i++) {
			String l = lines[i];
			if(l.startsWith("#")){
				continue;
			}
			
			if(l.startsWith("v ")){
				String[] verts = l.split("[ ]");
				parseVertexStore(i, l, verts);
			}
			
			if(l.startsWith("f ")){
				String[] faces = l.split("[ ]+");
				parseFaces(i, l, faces);
			}
		}
		ind = new short[FaceStore.size()*3];
		int indx = 0;
		for(int i = 0; i < FaceStore.size(); i++)
		{
			ind[indx++] = (short) FaceStore.get(i).x;
			ind[indx++] = (short) FaceStore.get(i).y;
			ind[indx++] = (short) FaceStore.get(i).z;
		}

		indx = 0;
		verts = new float[VertexStore.size()*3];
		for(int i = 0; i < VertexStore.size(); i++)
		{
			verts[indx++] = VertexStore.get(i).x;
			verts[indx++] = VertexStore.get(i).y;
			verts[indx++] = VertexStore.get(i).z;
		}
	}
	
	private void parseVertexStore(int i, String l, String[] vert){
		String[] v = l.split("[ ]");
		VertexStore.add(new Vector3(Float.parseFloat(v[1]), Float.parseFloat(v[2]), Float.parseFloat(v[3])));
	}
	
	private void parseFaces(int i, String s, String[] index){
		String[] fi = s.split("[ ]");
		FaceStore.add(
				new Face((Integer.valueOf(fi[1])-1), (Integer.valueOf(fi[2])-1), (Integer.valueOf(fi[3])-1))
		);
	}
	
	class Face
	{
		int x, y, z;
		
		public Face(int ...fs)
		{
			x = fs[0];
			y = fs[1];
			z = fs[2];
		}
		
		@Override
		public String toString()
		{
			return "( x "+x+" y "+y+" z "+z+" )";
		}
	}
	
	class Vector3
	{
		float x, y, z;
		
		public Vector3()
		{
			x = y = z = 0;
		}
		
		public Vector3(float ...fs)
		{
			x = fs[0];
			y = fs[1];
			z = fs[2];
		}
		
		@Override
		public String toString()
		{
			return "( "+x+" "+y+" "+z+" )";
		}
	}
}