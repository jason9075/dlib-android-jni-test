package com.jason9075.importdlibdemo.detector.single;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.my.jni.dlib.data.DLibFace;
import com.my.jni.dlib.data.DLibFace68;
import com.my.jni.dlib.data.Messages;

import java.util.ArrayList;
import java.util.List;

public class LegoToyDetector {


    public LegoToyDetector() {
        // TODO: Load library in worker thread?
        try {
            System.loadLibrary("c++_shared");
            Log.d("jni", "libc++_shared.so is loaded");
        } catch (UnsatisfiedLinkError error) {
            throw new RuntimeException(
                    "\"c++_shared\" not found; check that the correct native " +
                            "libraries are present in the APK.");
        }

        // TODO: Load library in worker thread?
        try {
            System.loadLibrary("protobuf-lite");
            Log.d("jni", "libprotobuf-lite.so is loaded");
        } catch (UnsatisfiedLinkError error) {
            throw new RuntimeException(
                    "\"protobuf-lite-3.2.0\" not found; check that the correct " +
                            "native libraries are present in the APK.");
        }

        // TODO: Load library in worker thread?
        try {
            System.loadLibrary("dlib");
            Log.d("jni", "libdlib.so is loaded");
        } catch (UnsatisfiedLinkError error) {
            throw new RuntimeException(
                    "\"dlib\" not found; check that the correct native libraries " +
                            "are present in the APK.");
        }

        // TODO: Load library in worker thread?
        try {
            System.loadLibrary("native-lib");
            Log.d("jni", "native-lib.so is loaded");
        } catch (UnsatisfiedLinkError error) {
            throw new RuntimeException(
                    "\"dlib_jni\" not found; check that the correct native " +
                            "libraries are present in the APK.");
        }
    }

    public List<DLibFace> findFaces(Bitmap bitmap)
            throws InvalidProtocolBufferException {
        // Do the face landmarks detection.
        final byte[] rawData = detectLegoToy(bitmap);
        final Messages.FaceList rawFaces = Messages.FaceList.parseFrom(rawData);

        // Convert raw data to my data structure.
        final List<DLibFace> faces = new ArrayList<>();
        for (int i = 0; i < rawFaces.getFacesCount(); ++i) {
            final Messages.Face rawFace = rawFaces.getFaces(i);
            final DLibFace face = new DLibFace68(rawFace);

            faces.add(face);
        }

        return faces;
    }


    public native void prepareLegoToyDetector(String path);

    public native byte[] detectLegoToy(Bitmap bitmap);


}
