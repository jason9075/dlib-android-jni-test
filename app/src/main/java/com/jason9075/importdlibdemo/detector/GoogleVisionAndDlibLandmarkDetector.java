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
import com.jason9075.importdlibdemo.view.FaceLandmarksOverlayView;
import com.my.jni.dlib.data.DLibFace;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class GoogleVisionAndDlibLandmarkDetector extends Detector<DLibFace> {

    // State.
    private final SparseArray<DLibFace> mDetFaces = new SparseArray<>();

    private final Detector<Face> mFaceDetector;
    private final IDLibFaceDetector mLandmarksDetector;
    private boolean mIsFrontCamera;

    public GoogleVisionAndDlibLandmarkDetector(final Detector<Face> faceDetector,
                                              final IDLibFaceDetector landmarksDetector,
                                              final FaceLandmarksOverlayView overlay,
                                              final boolean isFront) {
        mFaceDetector = faceDetector;
        mLandmarksDetector = landmarksDetector;
        mIsFrontCamera = isFront;

        setProcessor(new PostProcessor(overlay));
    }

    @Override
    public SparseArray<DLibFace> detect(Frame frame) {
        if (mFaceDetector == null ||
                mLandmarksDetector == null) {
            throw new IllegalStateException(
                    "Invalid detector.");
        }

        mDetFaces.clear();

        // Use Google Vision face detector to get face bounds.
        final SparseArray<Face> faces = mFaceDetector.detect(frame);

        if (faces.size() == 0) return mDetFaces;

        final Matrix transform = getCameraToViewTransform(frame);

        // Get bitmap from YUV frame.
        final Bitmap bitmap = getBitmapFromFrame(frame, transform);

        // Translate the face bounds into something that DLib detector knows.
        final List<Rect> faceBounds = new ArrayList<>();
        for (int i = 0; i < faces.size(); ++i) {
            final Face face = faces.get(faces.keyAt(i));

            // The facing-front preview is horizontally mirrored and it's
            // no harm for the algorithm to find the face bound, but it's
            // critical for the algorithm to align the landmarks. I need
            // to mirror it again.
            // 前鏡頭時 x座標系統是從右到左 Ex：自拍時 照片的文字順序會相反
            // For example:
            //
            // <-------------+ (1) The mirrored coordinate.
            // +-------------> (2) The not-mirrored coordinate.
            // |       |-----| This is x in the (1) system.
            // |   |---|       This is w in both (1) and (2) systems.
            // |   ?           This is what I want in the (2) system.
            // |   .---.
            // |   | F |
            // |   '---'
            // |
            // v

            final float x;
            if(mIsFrontCamera) {
                x = frame.getMetadata().getHeight() - face.getPosition().x - face.getWidth();
            } else {
                x = face.getPosition().x;
            }
            final float y = face.getPosition().y;
            final float w = face.getWidth();
            final float h = face.getHeight();
            final Rect bound = new Rect((int) (x),
                    (int) (y),
                    (int) (x + w),
                    (int) (y + h));

            // The face bound that DLib landmarks algorithm needs is slightly
            // different from the one given by Google Vision API, so I change
            // it a bit from the experience of try-and-error.
            bound.inset(bound.width() / 10,
                    bound.height() / 6);
            bound.offset(0, bound.height() / 4);

            faceBounds.add(bound);
        }


        // Detect landmarks.
        try {
            List<DLibFace> detFaces = mLandmarksDetector.findLandmarksFromFaces(
                    bitmap,
                    faceBounds);
            mDetFaces.clear();
            for (int i = 0; i < detFaces.size(); ++i) {
                mDetFaces.put(i, detFaces.get(i));
            }


            return mDetFaces;
        } catch (InvalidProtocolBufferException err) {
            err.printStackTrace();
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