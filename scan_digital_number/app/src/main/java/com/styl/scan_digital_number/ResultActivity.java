package com.styl.scan_digital_number;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.styl.scan_digital_number.utils.Constants;
import com.styl.scan_digital_number.utils.Utils;

public class ResultActivity extends AppCompatActivity {

    public static String RESULT_SCAN = "resultScanDigital";
    public static String BITMAP_SCAN = "bitmapScanDigital";

    private EditText edtResult;
    private ImageView imgResult;
    private Button btnOK;
    private TextView txtId;

    private String result;
    private Bitmap bitmap;

    private void getBundle() {
        if (getIntent().getExtras() != null) {
            result = getIntent().getExtras().getString(RESULT_SCAN);
            byte[] bytes = getIntent().getExtras().getByteArray(BITMAP_SCAN);
            bitmap = Utils.getInstance().convertByteArrayToBitmap(bytes);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        getBundle();
        init();
        bindData();
    }

    private void bindData() {
        txtId.setText("ID: " + Constants.ID_USER);

        if (result != null)
            edtResult.setText(result);

        if (bitmap != null)
            imgResult.setImageBitmap(bitmap);
    }

    private void init() {
        edtResult = findViewById(R.id.edtResult);
        imgResult = findViewById(R.id.imgResult);
        btnOK = findViewById(R.id.btnOK);
        txtId = findViewById(R.id.txtId);

        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }
}
