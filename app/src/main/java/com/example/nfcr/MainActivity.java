package com.example.nfcr;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    NfcAdapter nfcAdapter;
    TextView nfcTView;
    String readResult;
    String[][] techListsArray;
    IntentFilter[] intentFiltersArray;
    PendingIntent pendingIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcTView = (TextView) findViewById(R.id.textViewNFC);
        techListsArray = new String[][]{{NfcA.class.getName()}, {NfcB.class.getName()}, {IsoDep.class.getName()}, {NfcV.class.getName()}, {NfcF.class.getName()},};

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        try {
            ndef.addDataType("*/*");    /* Handles all MIME based dispatches.
                                       You should specify only the ones that you need. */
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        intentFiltersArray = new IntentFilter[]{ndef,};

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Log.d("h_bl", "Not Support NFC");
            finish();
            return;
        }
        if (nfcAdapter != null && !nfcAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Please open nfc", Toast.LENGTH_SHORT);
            Log.d("h_bl", "Not Open NFC");
            return;
        }
        Intent intent = this.getIntent();
        parseIntent(intent);
    }


    public void parseIntent(Intent intent) {
        String nfcAction = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(nfcAction)) {
            Log.d("h_bl", "ACTION_TECH_DISCOVERED");
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG); // 获取Tag标签，既可以处理相关信息
            Log.d("h_bl", "id = " + tag.getId());
            for (String tech : tag.getTechList()) {
                Log.d("h_bl", "tech=" + tech);
            }
            //readNfcA(tag);
            readNfcIsoDep(tag);
        }
//        if (NfcAdapter.EXTRA_NDEF_MESSAGES.equals(nfcAction)) {
//            Log.d("h_bl", "EXTRA_NDEF_MESSAGES");
//            Parcelable[] rawArray = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
//            NdefMessage mNdefMsg = (NdefMessage) rawArray[0];
//            NdefRecord mNdefRecord = mNdefMsg.getRecords()[0];
//            try {
//                if (mNdefRecord != null) {
//                    readResult = new String(mNdefRecord.getPayload(), "UTF-8");
//                }
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//            ;
//        }
    }


    public void readNfcA(Tag tag) {
        NfcA nfca = NfcA.get(tag);
        try {
            nfca.connect();
            Log.d("h_bl", String.valueOf(nfca.getMaxTransceiveLength()));
            Log.d("h_bl", String.valueOf(nfca.getSak()));
            byte[] SELECT = {
                    (byte) 0x30,
            };
            byte[] response = nfca.transceive(SELECT);
            nfca.close();
            if (response != null) {
                Log.d("h_bl", new String(response, Charset.forName("utf-8")));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readNfcIsoDep(Tag tag) {
        IsoDep isoDep = IsoDep.get(tag);
        try {
            isoDep.connect();
            Log.d("h_bl", String.valueOf(isoDep.getMaxTransceiveLength()));
            byte[] SELECT = {
                    (byte) 0x30,
            };
            byte[] response = isoDep.transceive(SELECT);
            isoDep.close();
            if (response != null) {
                Log.d("h_bl", new String(response, Charset.forName("utf-8")));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    public void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
    }

    public void onNewIntent(Intent intent) {

        super.onNewIntent(intent);
        Log.d("h_bl", intent.getAction());
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Log.d("h_bl", "onNewIntent");
            parseIntent(intent);
        }
    }

    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editTextTextPersonName);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
}