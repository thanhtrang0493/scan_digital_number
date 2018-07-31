package com.styl.scan_digital_number.nfc;

import java.util.HashMap;
import java.util.Map;

public enum ErrorNFC {
    NO_SUPPORT_NFC(0),
    DISABLE_NFC(1),
    ENABLE_NFC(2);

    private int errorCode = -1;

    private static final Map<Integer, ErrorNFC> lookup = new HashMap<Integer, ErrorNFC>();

    static {
        for (ErrorNFC d : ErrorNFC.values()) {
            lookup.put(d.getErrorCode(), d);
        }
    }

    public int getErrorCode() {
        return errorCode;
    }

    ErrorNFC(int error) {
        this.errorCode = error;
    }

    public static ErrorNFC get(int errorCode) {
        return lookup.get(errorCode);
    }

    public static String getErrorMessage(ErrorNFC errorNFC) {
        String error = "";
        switch (errorNFC) {
            case NO_SUPPORT_NFC:
                error = "No Support NFC.";
                break;
            case ENABLE_NFC:
                error = "NFC is enabled.";
                break;
            case DISABLE_NFC:
                error = "NFC is disabled.";
                break;
        }
        return error;
    }
}
