package com.styl.scan_digital_number.nfc;

public interface NFCListener {
    void onReadTagSuccess(String response);

    void onReadTagFail(String errorMessage);
}
