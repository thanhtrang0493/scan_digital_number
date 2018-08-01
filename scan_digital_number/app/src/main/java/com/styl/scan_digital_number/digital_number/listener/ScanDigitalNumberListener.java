package com.styl.scan_digital_number.digital_number.listener;

import android.graphics.Bitmap;

import org.opencv.core.Mat;

public interface ScanDigitalNumberListener {
    void onCameraFrame(Mat mRgba, Mat mGray, String data);

    void onScanDigitalNumberResponse(Bitmap bitmap, String result);
}
