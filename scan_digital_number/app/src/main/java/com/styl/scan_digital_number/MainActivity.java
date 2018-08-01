package com.styl.scan_digital_number;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.styl.scan_digital_number.digital_number.config.DigitalNumberConfig;
import com.styl.scan_digital_number.digital_number.listener.ScanDigitalNumberListener;
import com.styl.scan_digital_number.utils.Utils;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

public class MainActivity extends AppCompatActivity implements ScanDigitalNumberListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    private TextView txtResult;
    private ImageView imgResult;
    private DigitalNumberConfig digitalConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        digitalConfig = new DigitalNumberConfig(this, this);
        init();
    }

    private void init() {
        txtResult = (TextView) findViewById(R.id.txtResult);
        imgResult = (ImageView) findViewById(R.id.imgResult);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.jcvCamera);

        digitalConfig.initOpenCvCameraView(mOpenCvCameraView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        digitalConfig.pauseCameraScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        digitalConfig.startCameraScan();
    }

    @Override
    public void onCameraFrame(Mat mRgba, Mat mGray, String data) {
        txtResult.setText(data);
    }

    @Override
    public void onScanDigitalNumberResponse(Bitmap bitmap, String result) {
        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
        intent.putExtra(ResultActivity.RESULT_SCAN, result);
        intent.putExtra(ResultActivity.BITMAP_SCAN, Utils.getInstance().convertBitmapToByteArray(bitmap));

        startActivity(intent);
    }

}
