package com.jason9075.importdlibdemo;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.jason9075.importdlibdemo.R;
import com.jason9075.importdlibdemo.detector.DLibLandmarks68Detector;
import com.jason9075.importdlibdemo.detector.GoogleVisionAndDlibLandmarkDetector;
import com.jason9075.importdlibdemo.detector.IDLibFaceDetector;
import com.jason9075.importdlibdemo.utils.Utils;
import com.jason9075.importdlibdemo.view.FaceLandmarksOverlayView;
import com.my.jni.dlib.data.DLibFace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PhotoDetectActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int TARGET_WIDTH = 300;
    private final int ACTIVITY_SELECT_IMAGE = 1234;

    private Detector<DLibFace> mDetector;

    static {
        System.loadLibrary("native-lib");
    }

    private ImageView imageView;
    private FaceLandmarksOverlayView mOverlayView;
    private IDLibFaceDetector mLandmarksDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detect);

        String path = Utils.copyAssets(this);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        imageView = findViewById(R.id.sample_image);
        imageView.setImageResource(R.drawable.photo);
        mOverlayView = findViewById(R.id.overlay);
        Button detectButton = findViewById(R.id.detect_button);
        FloatingActionButton album = findViewById(R.id.album_button);
        detectButton.setOnClickListener(this);
        album.setOnClickListener(this);
        tv.setText(stringFromJNI());

        // Init the face detector.
        mLandmarksDetector = new DLibLandmarks68Detector();

        /* Too Slow ! */
//        if (!landmarksDetector.isFaceDetectorReady()) {
//            landmarksDetector.prepareFaceDetector();
//        }
        if (!mLandmarksDetector.isFaceLandmarksDetectorReady()) {
            mLandmarksDetector.prepareFaceLandmarksDetector(
                    path);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Detector<Face> faceDetector = new FaceDetector.Builder(this)
                .setClassificationType(FaceDetector.FAST_MODE)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
                .build();

        mDetector = new GoogleVisionAndDlibLandmarkDetector(faceDetector,
                mLandmarksDetector,
                mOverlayView,
                false);
    }

    private void startDetect() {
        System.out.println(">>>>start detect face");

        Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        Matrix matrix = new Matrix();
        float scale = TARGET_WIDTH / (float)bitmap.getWidth();
        matrix.postScale(scale, scale);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        Frame frame = new Frame.Builder().setBitmap(resizedBitmap).build();

        mOverlayView.setCameraPreviewSize(resizedBitmap.getWidth(), resizedBitmap.getHeight());
        mOverlayView.setVisibility(View.VISIBLE);
        mDetector.receiveFrame(frame);

        System.out.println(">>>>>>> finish detect face result:");
    }


    public native String stringFromJNI();

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.detect_button:
                startDetect();
                break;
            case R.id.album_button:
                Intent i = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(i, ACTIVITY_SELECT_IMAGE);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case ACTIVITY_SELECT_IMAGE:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = data.getData();
                    if(selectedImage==null)
                        return;
                    try {
                        Bitmap currentImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                        imageView.setImageBitmap(currentImage);
                        mOverlayView.setVisibility(View.INVISIBLE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
        }
    }
}
