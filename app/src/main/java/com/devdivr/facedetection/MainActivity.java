package com.devdivr.facedetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.devdivr.facedetection.libnative.Stasm;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV not init");
        } else {
            Log.i(TAG, "OpenCV init");
            try {
                // stasm 라이브러리 로딩
                System.loadLibrary("stasm");
            } catch (UnsatisfiedLinkError ex) {
                ex.printStackTrace();
            }
        }
    }

    private ImageView imageView;
    private File faceFile, leftEye, rightEye;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.imageview);

        new ImageProcessTask().execute();
    }

    private class ImageProcessTask extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // haar cascade file 로딩
            loadResources();
        }

        @Override
        protected Bitmap doInBackground(Void... params) {

            // 얼굴인식 할 이미지 로딩
            Bitmap src = BitmapFactory.decodeResource(getResources(), R.raw.main_model);
            String path = saveImage(src);
            Log.d(TAG, String.format("Original Image Size: %d x %d", src.getWidth(), src.getHeight()));

            // 디바이스 스크린 사이즈에 맞춰 이미지 스케일링
            float scale = calculateScale(MainActivity.this, src);
            Matrix m = new Matrix();
            m.setScale(scale, scale);
            Bitmap scaledSrc = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, false);
            Log.d(TAG, String.format("Scaled Image Size: %d x %d", scaledSrc.getWidth(), scaledSrc.getHeight()));

            int[] srcPoints = null;
            try {
                // Stasm을 이용해 얼굴의 landmark 포인트를 가져옴
                srcPoints = Stasm.FindFaceLandmarks(scale, scale, path);
            } catch (UnsatisfiedLinkError e) {
                e.printStackTrace();
            }

            // handle possible error
            if (srcPoints == null || srcPoints.length == 0) {
                return null;
            }
            if ((srcPoints[0] == -1) && (srcPoints[1] == -1)) {
                Log.e(TAG, "Cannot load image file");
                return null;
            } else if ((srcPoints[0] == -2) && (srcPoints[1] == -2)) {
                Log.e(TAG, "Error in stasm_search_single!");
                return null;
            } else if ((srcPoints[0] == -3) && (srcPoints[1] == -3)) {
                Log.e(TAG, "No face found");
                return null;
            }

            // 이미지 위에 가져온 landmark 포인트를 표시
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.CYAN);
            paint.setStrokeWidth(5);

            Canvas canvas = new Canvas(scaledSrc);
            for (int i = 0, j = srcPoints.length; i < j / 2; i++) {
                float x = srcPoints[2 * i];
                float y = srcPoints[2 * i + 1];
                canvas.drawPoint(x, y, paint);
            }
            return scaledSrc;
        }

        @Override
        protected void onPostExecute(Bitmap src) {
            super.onPostExecute(src);

            imageView.setImageBitmap(src);
        }
    }

    // haar cascade file 로딩
    private void loadResources() {
        if (!isDataFileInLocalDir()) {
            putDataFileInLocalDir(R.raw.haarcascade_frontalface_alt2, faceFile);
            putDataFileInLocalDir(R.raw.haarcascade_mcs_lefteye, leftEye);
            putDataFileInLocalDir(R.raw.haarcascade_mcs_righteye, rightEye);
        }
    }

    // jni에서 haar cascade file을 쓰기 위해 스토리지에 저장
    private void putDataFileInLocalDir(int id, File f) {
        try {
            InputStream is = getResources().openRawResource(id);
            FileOutputStream os = new FileOutputStream(f, false);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // haar cascade 파일이 있는지 체크
    private boolean isDataFileInLocalDir() {
        boolean exists = false;
        try {
            File dataDir = getDir("data", Context.MODE_PRIVATE);
            faceFile = new File(dataDir, "haarcascade_frontalface_alt2.xml");
            leftEye = new File(dataDir, "haarcascade_mcs_lefteye.xml");
            rightEye = new File(dataDir, "haarcascade_mcs_righteye.xml");
            exists = faceFile.exists() && leftEye.exists() && rightEye.exists();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return exists;
    }

    // 디바이스 해상도에 맞는 스케일 ratio를 계산함
    private float calculateScale(Context context, Bitmap src) {

        float ratio;
        int width = src.getWidth();
        int height = src.getHeight();

        if (height > width) {
            ratio = (float) context.getResources().getDisplayMetrics().heightPixels / height;
        } else {
            ratio = (float) context.getResources().getDisplayMetrics().widthPixels / width;
        }
        return ratio;
    }

    // 스케일링한 이미지를 jni에서 불러올 수 있게 저장함
    private String saveImage(Bitmap bitmap) {
        File direct = new File(getCacheDir(), "temp");
        if (!direct.exists()) {
            File directory = new File(direct.getAbsolutePath());
            directory.mkdirs();
        }
        File file = new File(new File(direct.getAbsolutePath()), "sample.jpg");
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file, false);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }


}
