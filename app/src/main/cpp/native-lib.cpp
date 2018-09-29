#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/bitmap.h>
#include <dlib/image_processing/frontal_face_detector.h>
#include <dlib/image_processing.h>
#include <dlib/image_io.h>
#include "include/my/dlib/data/messages.pb.h"


#define LOGI(...) \
((void)__android_log_print(ANDROID_LOG_INFO, "dlib-jni:", __VA_ARGS__))

#define JNI_METHOD(NAME) \
Java_com_jason9075_importdlibdemo_detector_single_DLibLandmarks68Detector_##NAME

#define JNI_TOY_METHOD(NAME) \
        Java_com_jason9075_importdlibdemo_detector_single_LegoToyDetector_##NAME

using namespace ::com::my::jni::dlib::data;

void throwException(JNIEnv* env,
                    const char* message) {
    jclass Exception = env->FindClass("java/lang/RuntimeException");
    env->ThrowNew(Exception, message);
}

typedef dlib::scan_fhog_pyramid<dlib::pyramid_down<6> > image_scanner_type;

dlib::shape_predictor sFaceLandmarksDetector;
dlib::frontal_face_detector sFaceDetector;
dlib::object_detector<image_scanner_type> sToyDetector;

extern "C" JNIEXPORT jstring JNICALL
Java_com_jason9075_importdlibdemo_PhotoDetectActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

// FIXME: Create a class inheriting from dlib::array2d<dlib::rgb_pixel>.
void convertBitmapToArray2d(JNIEnv* env,
                            jobject bitmap,
                            dlib::array2d<dlib::rgb_pixel>& out) {
    AndroidBitmapInfo bitmapInfo;
    void* pixels;
    int state;

    if (0 > (state = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo))) {
        LOGI("L%d: AndroidBitmap_getInfo() failed! error=%d", __LINE__, state);
        throwException(env, "AndroidBitmap_getInfo() failed!");
        return;
    } else if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGI("L%d: Bitmap format is not RGB_565!", __LINE__);
        throwException(env, "Bitmap format is not RGB_565!");
    }

    // Lock the bitmap for copying the pixels safely.
    if (0 > (state = AndroidBitmap_lockPixels(env, bitmap, &pixels))) {
        LOGI("L%d: AndroidBitmap_lockPixels() failed! error=%d", __LINE__, state);
        throwException(env, "AndroidBitmap_lockPixels() failed!");
        return;
    }

    LOGI("L%d: info.width=%d, info.height=%d", __LINE__, bitmapInfo.width, bitmapInfo.height);
    out.set_size((long) bitmapInfo.height, (long) bitmapInfo.width);

    char* line = (char*) pixels;
    for (int h = 0; h < bitmapInfo.height; ++h) {
        for (int w = 0; w < bitmapInfo.width; ++w) {
            uint32_t* color = (uint32_t*) (line + 4 * w);

            out[h][w].red = (unsigned char) (0xFF & ((*color) >> 24));
            out[h][w].green = (unsigned char) (0xFF & ((*color) >> 16));
            out[h][w].blue = (unsigned char) (0xFF & ((*color) >> 8));
        }

        line = line + bitmapInfo.stride;
    }

    // Unlock the bitmap.
    AndroidBitmap_unlockPixels(env, bitmap);
}


extern "C" JNIEXPORT jbyteArray JNICALL
JNI_METHOD(detectFacesAndLandmarks)(JNIEnv *env,
                                    jobject thiz,
                                    jobject bitmap) {
    if (sFaceDetector.num_detectors() == 0) {
        LOGI("L%d: sFaceDetector is not initialized!", __LINE__);
        throwException(env, "sFaceDetector is not initialized!");
        return NULL;
    }
    if (sFaceLandmarksDetector.num_parts() == 0) {
        LOGI("L%d: sFaceLandmarksDetector is not initialized!", __LINE__);
        throwException(env, "sFaceLandmarksDetector is not initialized!");
        return NULL;
    }

    // Convert bitmap to dlib::array2d.
    dlib::array2d<dlib::rgb_pixel> img;
    convertBitmapToArray2d(env, bitmap, img);


    const float width = (float) img.nc();
    const float height = (float) img.nr();

    // Now tell the face detector to give us a list of bounding boxes
    // around all the faces in the image.
    std::vector<dlib::rectangle> dets = sFaceDetector(img);

    // Protobuf message.
    FaceList faces;
    // Now we will go ask the shape_predictor to tell us the pose of
    // each face we detected.
    for (unsigned long j = 0; j < dets.size(); ++j) {
        dlib::full_object_detection shape = sFaceLandmarksDetector(img, dets[j]);


        // To protobuf message.
        Face* face = faces.add_faces();
        // Transfer face boundary.
        RectF* bound = face->mutable_bound();
        bound->set_left((float) dets[j].left() / width);
        bound->set_top((float) dets[j].top() / height);
        bound->set_right((float) dets[j].right() / width);
        bound->set_bottom((float) dets[j].bottom() / height);
        // Transfer face landmarks.
        for (u_long i = 0 ; i < shape.num_parts(); ++i) {
            dlib::point& pt = shape.part(i);

            Landmark* landmark = face->add_landmarks();
            landmark->set_x((float) pt.x() / width);
            landmark->set_y((float) pt.y() / height);
        }
    }


    // Prepare the return message.
    int outSize = faces.ByteSize();
    jbyteArray out = env->NewByteArray(outSize);
    jbyte* buffer = new jbyte[outSize];

    faces.SerializeToArray(buffer, outSize);
    env->SetByteArrayRegion(out, 0, outSize, buffer);
    delete[] buffer;


    return out;
}


// JNI ////////////////////////////////////////////////////////////////////////

extern "C" JNIEXPORT jboolean JNICALL
JNI_METHOD(isFaceDetectorReady)(JNIEnv* env,
                                jobject thiz) {
    if (sFaceDetector.num_detectors() > 0) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
JNI_METHOD(isFaceLandmarksDetectorReady)(JNIEnv* env,
                                         jobject thiz) {
    if (sFaceLandmarksDetector.num_parts() > 0) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT void JNICALL
JNI_METHOD(prepareFaceDetector)(JNIEnv *env,
                                jobject thiz) {

    // Prepare the detector.
    sFaceDetector = dlib::get_frontal_face_detector();

    LOGI("L%d: sFaceDetector.num_detectors()=%lu", __LINE__, sFaceDetector.num_detectors());
}

extern "C" JNIEXPORT void JNICALL
JNI_METHOD(prepareFaceLandmarksDetector)(JNIEnv *env,
                                         jobject thiz,
                                         jstring detectorPath) {
    const char *path = env->GetStringUTFChars(detectorPath, JNI_FALSE);

    // We need a shape_predictor. This is the tool that will predict face
    // landmark positions given an image and face bounding box.  Here we are just
    // loading the model from the shape_predictor_68_face_landmarks.dat file you gave
    // as a command line argument.
    // Deserialize the shape detector.
    dlib::deserialize(path) >> sFaceLandmarksDetector;

    LOGI("L%d: sFaceLandmarksDetector.num_parts()=%lu", __LINE__, sFaceLandmarksDetector.num_parts());

    env->ReleaseStringUTFChars(detectorPath, path);

    if (sFaceLandmarksDetector.num_parts() != 68) {
        throwException(env, "It's not a 68 landmarks detector!");
    }
}

extern "C" JNIEXPORT jbyteArray JNICALL
JNI_METHOD(detectLandmarksFromFaces)(JNIEnv *env,
                                     jobject thiz,
                                     jobject bitmap,
                                     jbyteArray faceRects) {

    // Convert bitmap to dlib::array2d.
    dlib::array2d<dlib::rgb_pixel> img;
    convertBitmapToArray2d(env, bitmap, img);

    const long width = img.nc();
    const long height = img.nr();

    // Translate the input face-rects message into something we recognize here.
    jbyte* pFaceRects = env->GetByteArrayElements(faceRects, NULL);
    jsize pFaceRectsLen = env->GetArrayLength(faceRects);
    RectFList msgBounds;
    msgBounds.ParseFromArray(pFaceRects, pFaceRectsLen);
    env->ReleaseByteArrayElements(faceRects, pFaceRects, 0);
    std::vector<dlib::rectangle> bounds;
    for (int i = 0; i < msgBounds.rects().size(); ++i) {
        const RectF& msgBound = msgBounds.rects().Get(i);
        bounds.push_back(dlib::rectangle((long) msgBound.left(),
                                         (long) msgBound.top(),
                                         (long) msgBound.right(),
                                         (long) msgBound.bottom()));
    }

    // Detect landmarks and return protobuf message.
    FaceList faces;
    for (unsigned long j = 0; j < bounds.size(); ++j) {
        dlib::full_object_detection shape = sFaceLandmarksDetector(img, bounds[j]);

        // To protobuf message.
        Face* face = faces.add_faces();
        // Transfer face boundary.
        RectF* bound = face->mutable_bound();
        bound->set_left((float) bounds[j].left() / width);
        bound->set_top((float) bounds[j].top() / height);
        bound->set_right((float) bounds[j].right() / width);
        bound->set_bottom((float) bounds[j].bottom() / height);
        // Transfer face landmarks.
        for (u_long i = 0 ; i < shape.num_parts(); ++i) {
            dlib::point& pt = shape.part(i);

            Landmark* landmark = face->add_landmarks();
            landmark->set_x((float) pt.x() / width);
            landmark->set_y((float) pt.y() / height);
        }
    }

    // Prepare the return message.
    int outSize = faces.ByteSize();
    jbyteArray out = env->NewByteArray(outSize);
    jbyte* buffer = new jbyte[outSize];

    faces.SerializeToArray(buffer, outSize);
    env->SetByteArrayRegion(out, 0, outSize, buffer);
    delete[] buffer;

    return out;
}

extern "C" JNIEXPORT void JNICALL
JNI_TOY_METHOD(prepareLegoToyDetector)(JNIEnv *env,
                                         jobject thiz,
                                         jstring detectorPath) {
    const char *path = env->GetStringUTFChars(detectorPath, JNI_FALSE);

    dlib::deserialize(path) >> sToyDetector;

    LOGI("L%d: sToyDetector Ready", __LINE__);

    env->ReleaseStringUTFChars(detectorPath, path);

}

extern "C" JNIEXPORT jbyteArray JNICALL
JNI_TOY_METHOD(detectLegoToy)(JNIEnv *env,
                                     jobject thiz,
                                     jobject bitmap) {

    // Convert bitmap to dlib::array2d.
    dlib::array2d<dlib::rgb_pixel> img;
    convertBitmapToArray2d(env, bitmap, img);

    const long width = img.nc();
    const long height = img.nr();

    std::vector<dlib::rectangle> dets = sToyDetector(img);

    // Protobuf message.
    FaceList faces;
    // Now we will go ask the shape_predictor to tell us the pose of
    // each face we detected.
    for (unsigned long j = 0; j < dets.size(); ++j) {

        // To protobuf message.
        Face* face = faces.add_faces();
        // Transfer face boundary.
        RectF* bound = face->mutable_bound();
        bound->set_left((float) dets[j].left() / width);
        bound->set_top((float) dets[j].top() / height);
        bound->set_right((float) dets[j].right() / width);
        bound->set_bottom((float) dets[j].bottom() / height);
    }

    // Prepare the return message.
    int outSize = faces.ByteSize();
    jbyteArray out = env->NewByteArray(outSize);
    jbyte* buffer = new jbyte[outSize];

    faces.SerializeToArray(buffer, outSize);
    env->SetByteArrayRegion(out, 0, outSize, buffer);
    delete[] buffer;

    return out;
}


