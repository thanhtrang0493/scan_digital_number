package com.styl.scan_digital_number.nfc.listener;

public interface NFCListener {
    void onReadTagSuccess(String response);

    void onReadTagFail(String errorMessage);
}
