package com.styl.scan_digital_number;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.styl.scan_digital_number.nfc.config.NFCConfig;
import com.styl.scan_digital_number.nfc.listener.NFCListener;
import com.styl.scan_digital_number.utils.Constants;

public class NFCActivity extends AppCompatActivity implements NFCListener {

    private TextView textView;
    private NFCConfig nfcConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        textView = (TextView) findViewById(R.id.text);
        nfcConfig = new NFCConfig();
        nfcConfig.setNfcListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isTurn = nfcConfig.checkTurnNFC(this);
        if (!isTurn) {
            nfcConfig.enableNFC(this);
        } else
            nfcConfig.startNFC(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        nfcConfig.handleIntent(intent, this);
    }

    @Override
    public void onReadTagSuccess(String response) {
        textView.setText(response);
        startScanDigitalNumberActivity(response);
    }

    private void startScanDigitalNumberActivity(String idUser) {
        Constants.ID_USER = idUser;
        Intent intent = new Intent(NFCActivity.this, MainActivity.class);
        this.startActivity(intent);

        this.finish();
    }

    @Override
    public void onReadTagFail(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }
}
