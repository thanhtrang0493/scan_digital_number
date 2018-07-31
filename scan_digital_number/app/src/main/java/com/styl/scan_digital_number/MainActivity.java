package com.styl.scan_digital_number;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";
    private static int REQUSET_CODE_CAMERA = 1200;
    private int COUNT_RESULT = 4;

    private CameraBridgeViewBase mOpenCvCameraView;
    private TextView txtResult;
    private ImageView imgResult;

    private int x;
    private int y;
    private int width;
    private int height;

    private Bitmap bitmapResult;
    private Bitmap bitmapGray;
    private String result;
    private List<String> listResult = new ArrayList<>();

    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private File mCascadeFileEye;
    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mJavaDetectorEye;

    private boolean isToResultActivity = true;
    private String dot = ".";
    private boolean isNewScan = true;
    private HandleBitmapTask task;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // load cascade file from application resources
                        InputStream ise = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
                        File cascadeDirEye = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFileEye = new File(cascadeDirEye, "haarcascade_lefteye_2splits.xml");
                        FileOutputStream ose = new FileOutputStream(mCascadeFileEye);

                        while ((bytesRead = ise.read(buffer)) != -1) {
                            ose.write(buffer, 0, bytesRead);
                        }
                        ise.close();
                        ose.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mJavaDetectorEye = new CascadeClassifier(mCascadeFileEye.getAbsolutePath());
                        if (mJavaDetectorEye.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier for eye");
                            mJavaDetectorEye = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFileEye.getAbsolutePath());

                        cascadeDir.delete();
                        cascadeDirEye.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.setCameraIndex(1);
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        init();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUSET_CODE_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUSET_CODE_CAMERA) {
            isNewScan = true;
        }
    }

    private void init() {
        txtResult = (TextView) findViewById(R.id.txtResult);
        imgResult = (ImageView) findViewById(R.id.imgResult);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.jcvCamera);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        if (mGray != null)
            mGray.release();

        if (mRgba != null)
            mRgba.release();
    }

    private void drawRectangle() {
        Rect rect = new Rect();
        rect.width = mRgba.width();
        rect.height = mRgba.height();
        x = (int) (rect.tl().x + rect.br().x) / 7;
        y = (int) (rect.tl().y + rect.br().y) / 4;
        width = rect.height / 6;
        height = (int) (mRgba.height() / 3 * 1.5);
        rect = new Rect(x, y, width, height);
        Imgproc.rectangle(mRgba, rect.tl(), rect.br(), new Scalar(255, 0, 0), 2, 8, 0);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        System.gc();

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        drawRectangle();

        if (isNewScan) {
            isNewScan = false;
            task = new HandleBitmapTask();
            task.execute(mGray);
        }

        return mRgba;
    }

    private Boolean isGoToResultActivity() {
        String number = "";
        int positionNumber = 0;
        if (listResult.size() > COUNT_RESULT) {
            Set<String> uniqueSet = new HashSet<String>(listResult);
            for (String temp : uniqueSet) {
                int d = Collections.frequency(listResult, temp);
                if (d > positionNumber) {
                    positionNumber = d;
                    number = temp;
                }
            }
        }

        if (positionNumber >= COUNT_RESULT) {
            result = number;
            return true;
        } else
            return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isToResultActivity = true;
        listResult = new ArrayList<>();
        isNewScan = true;
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }


    private String readSevenSegment(Mat mat) {
        String data = "";
        if (mat != null && width > 0 && height > 0 && mat.width() > 0) {
            Rect roi = new Rect(x, y, width, height);
            Mat cropped = new Mat(mat, roi);
            Bitmap b = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped, b);
            bitmapResult = ImageUtils.getInstance().rotateBitmap(b, 90);

            if (bitmapResult != null) {
                Bitmap bitmap = ImageUtils.getInstance().convertBitmapToBinary(bitmapResult);
                bitmapGray = bitmap;
                data = handleResult(bitmap);
            }
        }
        return data;
    }

    private String handleResult(Bitmap bitmap) {
        String number = "";
        String result = ImageUtils.getInstance().readSevenSegment(bitmap);
        if (result != null) {
            String[] arrData = result.split("[,\n+-]");
            String textBefore = "";
            for (int i = 0; i < arrData.length; i++) {
                String text = arrData[i].trim();
                if (number.length() == 0) {
                    if (!text.isEmpty() && !text.equals(dot)) {
                        number += text;
                        textBefore = text;
                    }
                } else {
                    if (textBefore.equals(dot)) {
                        if (text.isEmpty() || text.equals(dot)) {
                            number = number.substring(0, number.length() - 1);
                            break;
                        } else {
                            number += text;
                            textBefore = text;
                        }
                    } else {
                        if (text.isEmpty()) {
                            break;
                        } else {
                            number += text;
                            textBefore = text;
                        }
                    }
                }
            }
        }

        try {
            double d = Double.valueOf(number);

            //remove dot first
            if (number.length() > 0) {
                String first = number.substring(0, 1).trim();
                if (first.equals(dot))
                    number = number.substring(0, 1);
            }

            //remove dot end
            if (number.length() > 0) {
                String end = number.substring(number.length() - 1, number.length()).trim();
                if (end.equals(dot))
                    number = number.substring(number.length() - 1, number.length());
            }
        } catch (Exception e) {
            number = "";
        }
        return number;
    }

    private void goToResultActivity() {
        if (mRgba != null && width > 0 && height > 0 && mRgba.width() > 0) {
            Rect roi = new Rect(x, y, width, height);
            Mat cropped = new Mat(mRgba, roi);
            Bitmap b = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped, b);
            b = ImageUtils.getInstance().rotateBitmap(b, 90);

            isToResultActivity = false;
            listResult.clear();

            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
            intent.putExtra(ResultActivity.RESULT_SCAN, result);
            intent.putExtra(ResultActivity.BITMAP_SCAN, com.styl.scan_digital_number.Utils.getInstance().convertBitmapToByteArray(b));

            startActivity(intent);
        }
    }

    private class HandleBitmapTask extends AsyncTask<Mat, Void, String> {

        @Override
        protected String doInBackground(Mat... mats) {
            return readSevenSegment(mats[0]);
        }

        @Override
        protected void onPostExecute(String number) {
            isNewScan = true;
            if (!number.isEmpty() && !number.equals(dot))
                listResult.add(number);
            txtResult.setText(number);
            imgResult.setImageBitmap(bitmapGray);

            if (isGoToResultActivity() && isToResultActivity) {
                goToResultActivity();
            }
            this.cancel(true);
        }

        @Override
        protected void onCancelled() {
            listResult.clear();
            isNewScan = true;
        }

    }
}
