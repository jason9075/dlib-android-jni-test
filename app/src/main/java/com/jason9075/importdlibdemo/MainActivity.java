package com.jason9075.importdlibdemo;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.jason9075.importdlibdemo.detector.GoogleVisionAndDlibLandmarkDetector;
import com.jason9075.importdlibdemo.view.FaceLandmarksOverlayView;
import com.my.jni.dlib.data.DLibFace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String MODEL_DATA = "shape_predictor_68_face_landmarks.dat";
    public static final int TARGET_WIDTH = 300;

    private Detector<DLibFace> mDetector;

    static {
        System.loadLibrary("native-lib");
    }

    private ImageView imageView;
    private FaceLandmarksOverlayView mOverlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String path = copyAssets();

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        imageView = findViewById(R.id.sample_image);
        imageView.setImageResource(R.drawable.photo);
        mOverlayView = findViewById(R.id.overlay);
        Button detectButton = findViewById(R.id.detect_button);
        detectButton.setOnClickListener(this);
        tv.setText(stringFromJNI());

        System.out.println(">>>>init detector");
        // Init the face detector.
        IDLibFaceDetector landmarksDetector = new DLibLandmarks68Detector();

        /* Too Slow ! */
//        if (!landmarksDetector.isFaceDetectorReady()) {
//            landmarksDetector.prepareFaceDetector();
//        }
        if (!landmarksDetector.isFaceLandmarksDetectorReady()) {
            landmarksDetector.prepareFaceLandmarksDetector(
                    path);
        }

        Detector<Face> faceDetector = new FaceDetector.Builder(this)
                .setClassificationType(FaceDetector.FAST_MODE)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
                .build();

        mDetector = new GoogleVisionAndDlibLandmarkDetector(faceDetector,
                                                            landmarksDetector,
                mOverlayView);
        System.out.println(">>>>finish init detector");

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void startDetect() {
        System.out.println(">>>>start detect face");

        Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        Matrix matrix = new Matrix();
        float scale = TARGET_WIDTH / (float)bitmap.getWidth();
        matrix.postScale(scale, scale);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        Frame frame = new Frame.Builder().setBitmap(resizedBitmap).build();

        mOverlayView.setCameraPreviewSize(resizedBitmap.getWidth(), resizedBitmap.getHeight());
        mDetector.receiveFrame(frame);

        System.out.println(">>>>>>> finish detect face result:");
    }

    private String copyAssets() {
        File detectorData = new File(getExternalFilesDir(null), MODEL_DATA);
        if(detectorData.exists())
            return detectorData.getAbsolutePath();

        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        if (files != null) for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(filename);
                File outFile = new File(getExternalFilesDir(null), filename);
                out = new FileOutputStream(outFile);
                copyFile(in, out);
            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
            }
        }
        detectorData = new File(getExternalFilesDir(null), MODEL_DATA);
        return detectorData.getAbsolutePath();
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public native String stringFromJNI();

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.detect_button:
                startDetect();
        }
    }
}
