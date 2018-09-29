package com.jason9075.importdlibdemo.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class Utils {

    public static final String MODEL_DATA = "shape_predictor_68_face_landmarks.dat";
    public static final String TOY_MODEL_DATA = "detector.svm";


    public static void copyAssets(Context context) {
        File model = new File(context.getExternalFilesDir(null), MODEL_DATA);
        File toyModel = new File(context.getExternalFilesDir(null), TOY_MODEL_DATA);

        if(model.exists() && toyModel.exists())
            return;

        AssetManager assetManager = context.getAssets();
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
                File outFile = new File(context.getExternalFilesDir(null), filename);
                out = new FileOutputStream(outFile);
                copyFile(in, out);
            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            } finally {
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
    }

    public static String getCopyedAssets(Context context, String name) {
        File detectorData = new File(context.getExternalFilesDir(null), name);
        return detectorData.getAbsolutePath();
    }



    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
}
