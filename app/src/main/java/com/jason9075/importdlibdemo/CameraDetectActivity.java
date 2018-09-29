package com.jason9075.importdlibdemo;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.Manifest;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.jason9075.importdlibdemo.detector.single.DLibLandmarks68Detector;
import com.jason9075.importdlibdemo.detector.GoogleVisionAndDlibLandmarkDetector;
import com.jason9075.importdlibdemo.detector.single.IDLibFaceDetector;
import com.jason9075.importdlibdemo.utils.Utils;
import com.jason9075.importdlibdemo.view.CameraSourcePreview;
import com.jason9075.importdlibdemo.view.FaceLandmarksOverlayView;
import com.my.jni.dlib.data.DLibFace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraDetectActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int CAMERA_REQUEST_CODE = 111;
    private Detector<DLibFace> mDetector;

    private CameraSourcePreview mCameraView;
    private FaceLandmarksOverlayView mOverlayView;
    private IDLibFaceDetector mLandmarksDetector;

    private final int PREVIEW_WIDTH = 320;
    private final int PREVIEW_HEIGHT = 240;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_detect);

        Utils.copyAssets(this);
        String path = Utils.getCopyedAssets(this, Utils.MODEL_DATA);

        mCameraView = findViewById(R.id.camera);
        mOverlayView = findViewById(R.id.overlay);
        Button detectButton = findViewById(R.id.detect_button);
        detectButton.setOnClickListener(this);

        mLandmarksDetector = new DLibLandmarks68Detector();

        if (!mLandmarksDetector.isFaceLandmarksDetectorReady()) {
            mLandmarksDetector.prepareFaceLandmarksDetector(
                    path);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!isGrantedPermission())
            return;

        Detector<Face> faceDetector = new FaceDetector.Builder(this)
                .setClassificationType(FaceDetector.FAST_MODE)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
                .build();

        mDetector = new GoogleVisionAndDlibLandmarkDetector(faceDetector,
                mLandmarksDetector,
                mOverlayView,
                true);

        // Create camera source.
        final CameraSource source = new CameraSource.Builder(this, mDetector)
                .setRequestedPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setAutoFocusEnabled(true)
                .setRequestedFps(30f)
                .build();

        // Open the camera.
        try {
            mCameraView.start(source);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean isGrantedPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int hasCameraPermission = checkSelfPermission(Manifest.permission.CAMERA);
            List<String> permissions = new ArrayList<>();

            if (hasCameraPermission != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.CAMERA);
            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[0]), CAMERA_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        System.out.println("取得權限");
                    }
                }
            }
            break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        // Close camera.
        mCameraView.release();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.detect_button:
                // Set the preview config. PortraitMode 寬=240, 高=320
                mOverlayView.setCameraPreviewSize(PREVIEW_HEIGHT,
                        PREVIEW_WIDTH);
                v.setVisibility(View.INVISIBLE);
                break;
        }
    }
}
