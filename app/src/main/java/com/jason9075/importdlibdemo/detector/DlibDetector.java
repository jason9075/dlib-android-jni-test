package com.jason9075.importdlibdemo.detector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jason9075.importdlibdemo.detector.single.IDLibFaceDetector;
import com.jason9075.importdlibdemo.detector.single.LegoToyDetector;
import com.jason9075.importdlibdemo.view.FaceLandmarksOverlayView;
import com.my.jni.dlib.data.DLibFace;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DlibDetector extends Detector<DLibFace> {

    // State.
    private final SparseArray<DLibFace> mDetFaces = new SparseArray<>();

    private final LegoToyDetector mLegoToyDetector;
    private boolean mIsFrontCamera;

    public DlibDetector(final LegoToyDetector legoToyDetector,
                        final FaceLandmarksOverlayView overlay,
                        final boolean isFront) {
        mLegoToyDetector = legoToyDetector;
        mIsFrontCamera = isFront;

        setProcessor(new DlibDetector.PostProcessor(overlay));
    }

    @Override
    public SparseArray<DLibFace> detect(Frame frame) {
        if (mLegoToyDetector == null) {
            throw new IllegalStateException(
                    "Invalid detector.");
        }

        mDetFaces.clear();

        final Matrix transform = getCameraToViewTransform(frame);

        // Get bitmap from YUV frame.
        final Bitmap bitmap = getBitmapFromFrame(frame, transform);

        try {
            final List<DLibFace> detFaces = mLegoToyDetector.findFaces(bitmap);
            System.out.println(">>>size"+ detFaces.size());
            for (int i = 0; i < detFaces.size(); ++i) {
                mDetFaces.put(i, detFaces.get(i));
            }

            return mDetFaces;

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return null;
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    // TODO: This method could be an util method.
    // TODO: Rotation and facing are important parameters.
    private Matrix getCameraToViewTransform(final Frame frame) {
        final Matrix transform = new Matrix();
        switch (frame.getMetadata().getRotation()) {
            case Frame.ROTATION_90:
                transform.postRotate(90);
                break;
            case Frame.ROTATION_180:
                transform.postRotate(180);
                break;
            case Frame.ROTATION_270:
                transform.postRotate(270);
                break;
        }

        if (mIsFrontCamera) {
            transform.postScale(-1, 1);
        }

        return transform;
    }

    private Bitmap getBitmapFromFrame(final Frame frame,
                                      final Matrix transform) {
        if (frame.getBitmap() != null) {
            return frame.getBitmap();
        } else {
            final int width = frame.getMetadata().getWidth();
            final int height = frame.getMetadata().getHeight();
            final YuvImage yuvImage = new YuvImage(
                    frame.getGrayscaleImageData().array(),
                    ImageFormat.NV21, width, height, null);

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, outputStream);

            final byte[] jpegArray = outputStream.toByteArray();
            final Bitmap rawBitmap = BitmapFactory.decodeByteArray(
                    jpegArray, 0, jpegArray.length);

            final int bw = rawBitmap.getWidth();
            final int bh = rawBitmap.getHeight();

            return Bitmap.createBitmap(rawBitmap,
                    0, 0, bw, bh,
                    transform, false);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    private static class PostProcessor implements Detector.Processor<DLibFace> {

        final FaceLandmarksOverlayView mOverlay;

        // Data.
        final List<DLibFace> mFaces = new ArrayList<>();

        PostProcessor(FaceLandmarksOverlayView overlay) {
            mOverlay = overlay;
        }

        @Override
        public void release() {
            // DO NOTHING.
        }

        @Override
        public void receiveDetections(Detections<DLibFace> detections) {
            mFaces.clear();
            if (detections == null) {
                mOverlay.setFaces(mFaces);
                return;
            }

            final SparseArray<DLibFace> faces = detections.getDetectedItems();
            if (faces == null) return;

            for (int i = 0; i < faces.size(); ++i) {
                mFaces.add(faces.get(faces.keyAt(i)));
            }

            Log.d("xyz", String.format("Ready to render %d faces", faces.size()));
            mOverlay.setFaces(mFaces);
        }
    }
}
