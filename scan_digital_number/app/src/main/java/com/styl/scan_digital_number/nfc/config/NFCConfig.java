package com.styl.scan_digital_number.nfc.config;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Parcelable;

import com.styl.scan_digital_number.nfc.utils.NFCUtils;
import com.styl.scan_digital_number.nfc.record.NdefMessageParser;
import com.styl.scan_digital_number.nfc.listener.NFCListener;
import com.styl.scan_digital_number.nfc.listener.ParsedNdefRecord;

import java.util.List;

public class NFCConfig {

    private NFCListener nfcListener;
    private PendingIntent pendingIntent;

    public void setNfcListener(NFCListener listener) {
        this.nfcListener = listener;
    }

    public void enableNFC(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            context.startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
        } else {
            context.startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
        }
    }

    public void startNFC(Context context) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, context.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        nfcAdapter.enableForegroundDispatch((Activity) context, pendingIntent, null, null);
    }

    public boolean checkTurnNFC(Context context) {
        boolean isEnable = false;

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);

        String errorNFC = "";
        if (adapter == null) {
            errorNFC = "No Support NFC.";
        } else {
            if (!adapter.isEnabled()) {
                errorNFC = "NFC is disable.";
            } else
                isEnable = true;
        }

        if (nfcListener != null && !isEnable)
            nfcListener.onReadTagFail(errorNFC);

        return isEnable;
    }

    public void handleIntent(Intent intent, Context context) {
        ((Activity) context).setIntent(intent);
        String data = "";
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;

            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];

                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
                data = displayMsgs(msgs);
            } else {
//                byte[] empty = new byte[0];
                byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
//                Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//                byte[] payload = dumpTagData(tag).getBytes();
//                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload);
//                NdefMessage msg = new NdefMessage(new NdefRecord[] {record});
//                msgs = new NdefMessage[] {msg};

                data = String.valueOf(NFCUtils.toDec(id));
            }
        }

        if (nfcListener != null)
            nfcListener.onReadTagSuccess(data);
    }

    private String displayMsgs(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0)
            return "";

        StringBuilder builder = new StringBuilder();
        List<ParsedNdefRecord> records = NdefMessageParser.parse(msgs[0]);
        final int size = records.size();

        for (int i = 0; i < size; i++) {
            ParsedNdefRecord record = records.get(i);
            String str = record.str();
            builder.append(str).append("\n");
        }

        return builder.toString();
    }
}
