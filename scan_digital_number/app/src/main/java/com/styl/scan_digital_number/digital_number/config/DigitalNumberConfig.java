package com.styl.scan_digital_number.digital_number.config;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.styl.scan_digital_number.R;
import com.styl.scan_digital_number.digital_number.utils.ImageUtils;
import com.styl.scan_digital_number.digital_number.listener.ScanDigitalNumberListener;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DigitalNumberConfig implements CameraBridgeViewBase.CvCameraViewListener2 {

    private Context context;
    private static final String TAG = "DigitalNumberConfig";
    private static int REQUSET_CODE_CAMERA = 1200;
    private int COUNT_RESULT = 4;

    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private File mCascadeFileEye;
    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mJavaDetectorEye;

    private CameraBridgeViewBase mOpenCvCameraView;

    private int xRec;
    private int yRec;
    private int widthRec;
    private int heightRec;

    private String dot = ".";
    private boolean isNewScan = true;
    private List<String> listResult = new ArrayList<>();
    private boolean isScanSuccess = true;
    private String result;

    private ScanDigitalNumberListener listener;

    public DigitalNumberConfig(Context context, ScanDigitalNumberListener scanDigitalNumberListener) {
        this.listener= scanDigitalNumberListener;
        this.context = context;
    }

    public void initOpenCvCameraView(CameraBridgeViewBase camera) {
        mOpenCvCameraView = camera;
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    /**
     * Adjust probability count after scan
     *
     * @param countResult
     */
    public void setCountResult(int countResult) {
        COUNT_RESULT = countResult;
    }

    /**
     * The function is called in onResume
     */
    public void startCameraScan() {
        checkPermissionCamera();

        isScanSuccess = true;
        isNewScan = true;
        listResult = new ArrayList<>();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, context, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    /**
     * The function is called in onPause
     */
    public void pauseCameraScan(){
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(context) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    try {
                        // load cascade file from application resources
                        InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
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
                        InputStream ise = context.getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
                        File cascadeDirEye = context.getDir("cascade", Context.MODE_PRIVATE);
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

    /**
     * Adjust camera full screen
     */
    public void setCameraFullScreen() {
        ((Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Request permission turn on camera if users don't turn on
     */
    public void checkPermissionCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.CAMERA}, REQUSET_CODE_CAMERA);
        }
    }

    /**
     * Draw rectangle on camera
     */
    private void drawRectangle() {
        Rect rect = new Rect();
        rect.width = mRgba.width();
        rect.height = mRgba.height();
        xRec = (int) (rect.tl().x + rect.br().x) / 7;
        yRec = (int) (rect.tl().y + rect.br().y) / 4;
        widthRec = rect.height / 6;
        heightRec = (int) (mRgba.height() / 3 * 1.5);
        rect = new Rect(xRec, yRec, widthRec, heightRec);
        Imgproc.rectangle(mRgba, rect.tl(), rect.br(), new Scalar(255, 0, 0), 2, 8, 0);
    }

    private Bitmap cropRectangleBitmap(Mat mat) {
        Bitmap bitmap = null;
        if (mat != null && widthRec > 0 && heightRec > 0 && mat.width() > 0) {
            Rect roi = new Rect(xRec, yRec, widthRec, heightRec);
            Mat cropped = new Mat(mat, roi);

            //crop bitmap gray
            bitmap = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped, bitmap);
            bitmap = ImageUtils.getInstance().rotateBitmap(bitmap, 90);
        }

        return bitmap;
    }

    private String readSevenSegment(Mat mat) {
        String data = "";
        Bitmap bitmap = cropRectangleBitmap(mat);
        if (bitmap != null) {
            bitmap = ImageUtils.getInstance().convertBitmapToBinary(bitmap);
            data = handleResult(bitmap);
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

    private Boolean isScanSuccess() {
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

            if (isScanSuccess() && isScanSuccess) {
                isScanSuccess = false;

                if (listener != null) {
                    Bitmap b = cropRectangleBitmap(mRgba);
                    listener.onScanDigitalNumberResponse(b, result);
                }
            } else {
                if (listener != null) {
                    listener.onCameraFrame(mRgba, mGray, number);
                }
            }
            this.cancel(true);
        }

        @Override
        protected void onCancelled() {
            listResult.clear();
            isNewScan = true;
        }

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

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        System.gc();

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        drawRectangle();

        if (isNewScan) {
            isNewScan = false;
            new HandleBitmapTask().execute(mGray);
        }

        return mRgba;
    }
}
