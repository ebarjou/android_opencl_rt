#define MAX_VIEW_DIST 1000
#define EPSILON 0.0001

typedef struct structSphere {
    float4 or;
    float4 color;
} Sphere;

typedef struct structRay {
    float3 o;
    float3 d;
} Ray;

typedef struct structLight {
    float3 o;
    float3 color;
} Light;

typedef struct structCamera {
    float3 o;
    float3 f;
    float3 x;
    float3 y;
} Camera;

const Camera cam = {(float3)(0,0,-1), (float3)(0,0,1), (float3)(1,0,0), (float3)(0,1,0)};

__kernel void rayKernel(__write_only image2d_t dst_image) {
    float w = get_global_size(0), h = get_global_size(1);
    float3 d = cam.f + 2.0*(get_global_id(0)/w - 0.5)*cam.x + 2.0*(get_global_id(1)/h - 0.5)*cam.y*(h/w);
    write_imagef( dst_image, (int2)(get_global_id(0),get_global_id(1)), (float4)(d, 1.0) );
}

float intersect_sphere(__constant Sphere scene[], int id, Ray* r){
    float a = dot(r->d, r->d);
    float3 s0_r0 = r->o - scene[id].or.xyz;
    float b = 2.0 * dot(r->d, s0_r0);
    float c = dot(s0_r0, s0_r0) - scene[id].or.w * scene[id].or.w;
    return (b*b - 4.0*a*c <= 0.0)?-1.0:(-b - sqrt((b*b) - 4.0*a*c))/(2.0*a);
}
/*

bool intersect(Ray* ray, __constant Sphere scene[], int scene_size, float3* hit, float3* normal, int* shape_id){
    float t = MAX_VIEW_DIST, tt = -1;
    for(int i = 0; i < scene_size; ++i){
        tt = intersect_sphere(scene, i, ray);
        if(tt > 0 && tt < t){
            t = tt;
            *shape_id = i;
        }
    }
    if(t < MAX_VIEW_DIST){
        *hit = ray->o+t*ray->d;
        *normal = normalize(*hit - scene[*shape_id].or.xyz);
        return true;
    }
    return false;
}
*/

bool intersect_triangle(Ray* ray, __constant float *vertices, __constant short *faces, int index, float* t){
    short ind = faces[index];
    float3 v0 = (float3)(vertices[ind], vertices[ind+1], vertices[ind+2]);
    ind = faces[index+1];
    float3 v1 = (float3)(vertices[ind], vertices[ind+1], vertices[ind+2]);
    ind = faces[index+2];
    float3 v2 = (float3)(vertices[ind], vertices[ind+1], vertices[ind+2]);

    float3 v0v1 = v1 - v0;
    float3 v0v2 = v2 - v0;
    float3 pvec = cross(ray->d, v0v2);
    float det = dot(v0v1, pvec);

    if (fabs(det) < EPSILON) return false;
    float invDet = 1 / det;

    float3 tvec = ray->o - v0;
    float u = dot(tvec, pvec) * invDet;
    if (u < 0 || u > 1) return false;

    float3 qvec = cross(tvec, v0v1);
    float v = dot(ray->d, qvec) * invDet;
    if (v < 0 || u + v > 1) return false;

    *t = dot(v0v2, qvec) * invDet;

    return true;
}

void intersect(Ray* ray, float4* hit, int* shape_id, short start, short size,
            __constant float *vertices, __constant short *faces){
    float t = -1;

    for(int i = 0; i < size; ++i){
        if(intersect_triangle(ray, vertices, faces, start+i*3, &t)){
            if(t > 0 && t < (*hit).w){
                (*hit).w = t;
                *shape_id = i;
            }
        }
    }
    /*if(t < MAX_VIEW_DIST){
        *hit = ray->o+t*ray->d;
        *hit = (float3)(0,1,1);
        *normal = (float3)(1,0,0);
        return true;
    }

    return false;*/
}

__kernel void traceKernel(__read_only image2d_t ray_image, __constant float *vertices, __constant short *faces,
                        __constant short *object, __constant short *object_size,
                        __constant short *object_to_draw, __private int nb_object,
                         __write_only image2d_t hit_image, __write_only image2d_t normal_image) {
    int scene_size = 3;

    Ray ray = {cam.o, read_imagef( ray_image, (int2)(get_global_id(0),get_global_id(1))).xyz};

    //float3 normal = (float3)(0,0,0);
    float4 hit = (float4)(0,0,0, MAX_VIEW_DIST);
    int res = -2;
    for(int i = 0; i < nb_object; ++i){
        intersect(&ray, &hit, &res, object[object_to_draw[i]], object_size[i],  vertices, faces);
    }
    if(hit.w < MAX_VIEW_DIST){//
        hit = (float4)(1,0,1,1);
    }


    //write_imagef( normal_image, (int2)(get_global_id(0),get_global_id(1)), (float4)(normal, (float)res) );
    write_imagef( hit_image, (int2)(get_global_id(0),get_global_id(1)), (float4)(hit.xyz, (float)res) );
}



float blinn_phong(float3 lightDir, float3 viewDir, float3 normal, float exponent){
    float3 halfDir = normalize(lightDir + viewDir);
    float specAngle = max(dot(halfDir, normal), 0.0);
    return pow(specAngle, exponent);
}

float3 brdf(float3 normal, float3 hit, Light* lights[], int nb_light, float4 dColor){
    float3 color = (float3)(0,0,0);
    float3 lightDir;
    float3 viewDir = normalize(cam.o - hit);
    for(int i = 0; i < nb_light; ++i){
        lightDir = normalize(lights[i]->o - hit);
        float3 p = dColor.xyz + lights[i]->color*blinn_phong(lightDir, viewDir, normal, dColor.w);
        color += p*max(dot(lightDir,normal),0.0)*(1.0/(1.0+length(hit-lights[i]->o)));
    }
    return color;
}

__kernel void shadeKernel(__read_only image2d_t hit_image, __read_only image2d_t normal_image, __write_only image2d_t dst_image) {
    /*Light light1 = {(float3)(0.8,0.8,-0.45), (float3)(1,1,1)};
    Light light2 = {(float3)(-0.9,-0.7,-0.9), (float3)(1,1,1)};
    Light* lights[] = {
          &light1,
          &light2
    };
    int light_size = 2;*/

    //float4 normal = read_imagef( normal_image, (int2)(get_global_id(0),get_global_id(1)));
    float4 hit = read_imagef( hit_image, (int2)(get_global_id(0),get_global_id(1)));

    write_imagef( dst_image, (int2)(get_global_id(0),get_global_id(1)), (float4)(hit.xyz,1) );

    /*float4 normal = read_imagef( normal_image, (int2)(get_global_id(0),get_global_id(1)));
    float4 hit = read_imagef( hit_image, (int2)(get_global_id(0),get_global_id(1)));
    int object_id = (int)normal.w;

    float4 color = object_id<0?(float4)(0,0,0,0):(float4)(brdf(normal.xyz, hit.xyz, lights, light_size, scene[object_id].color), 1);
    write_imagef( dst_image, (int2)(get_global_id(0),get_global_id(1)), (float4)(color) );*/
}