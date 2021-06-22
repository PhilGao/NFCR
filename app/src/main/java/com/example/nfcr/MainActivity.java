package com.example.nfcr;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    NfcAdapter nfcAdapter;
    TextView nfcTextView;
    EditText nfcEditText;
    Button buttonWriteIn;
    String[][] techListsArray;
    IntentFilter[] intentFiltersArray;
    PendingIntent pendingIntent;
    private Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Bundle bundle = msg.getData();
                Log.d("h_bl", bundle.toString());
                String hex = (String) bundle.getString("hex");
                String strAscii = (String) bundle.getString("ASCII");
                nfcTextView.append("hex:" + hex + "\r\n");
                nfcTextView.append("ASCII:" + strAscii + "\r\n");
            }
        }
    };

    public static String byte2HexString(byte[] bytes) {
        String hex = "";
        if (bytes != null) {
            for (Byte b : bytes) {
                hex += String.format("%02X", b.intValue() & 0xFF);
            }
        }
        return hex;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcTextView = (TextView) findViewById(R.id.textViewNFC);
        nfcEditText = (EditText) findViewById(R.id.editTextNFC);
        buttonWriteIn = (Button) findViewById(R.id.buttonWriteIn);
        techListsArray = new String[][]{{NfcA.class.getName()}, {NfcB.class.getName()}, {IsoDep.class.getName()}, {NfcV.class.getName()}, {NfcF.class.getName()},};

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
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
            nfcTextView.setText("Please open nfc");
            Log.d("h_bl", "Not Open NFC");
            return;
        }
        buttonWriteIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent(); //todo: add button handler for write data to nfc
            }
        });

//        Intent intent = this.getIntent();
//        Tag tag = parseIntent(intent);
//        if (tag != null )
//            readNfcA(tag);
    }


    public Tag parseIntent(Intent intent) {
        String nfcAction = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(nfcAction)) {
            Log.d("h_bl", "ACTION_TECH_DISCOVERED");
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG); // 获取Tag标签，既可以处理相关信息
            Log.d("h_bl", "id = " + tag.getId());
            nfcTextView.setText(null);
            for (String tech : tag.getTechList()) {
                Log.d("h_bl", "tech=" + tech);
            }
            return tag;
        }
        return null;
    }


    public void readNfcA(Tag tag) {
        NfcA nfca = NfcA.get(tag);
        if (nfca != null)
            Log.d("h_bl", nfca.toString());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    nfca.connect();
                    Log.d("h_bl", String.valueOf(nfca.getMaxTransceiveLength()));
                    Log.d("h_bl", String.valueOf(nfca.getSak()));
                    //读取06之后的4个字节
                    byte[] SELECT = {
                            (byte) 0x3A,
                            (byte) 0x06,
                            (byte) 0x27,
                    };
                    byte[] response = nfca.transceive(SELECT);
                    if (response != null) {
                        Log.d("h_bl", new String(response));
                        Log.d("h_bl", byte2HexString(response));
                        Bundle bundle = new Bundle();
                        bundle.putString("hex", byte2HexString(response));
                        bundle.putString("ASCII", new String(response));
                        Message message = Message.obtain();
                        message.setData(bundle);
                        message.what = 1;
                        handler.sendMessage(message);
                    }
                    nfca.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }


    public void writeNfcA(Tag tag, String message) {
        NfcA nfca = NfcA.get(tag);
        if (nfca != null)
            Log.e("e_bl", nfca.toString());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    nfca.connect();
                    Log.d("h_bl", String.valueOf(nfca.getMaxTransceiveLength()));
                    Log.d("h_bl", String.valueOf(nfca.getSak()));
                    byte[] messageBytes = message.getBytes(StandardCharsets.US_ASCII);
                    byte[] writeCmd = {(byte) 0xA2, (byte) 0x06}; //todo: add next page logic
                    byte[] WRITE = new byte[6];
                    //写命令后面只能跟4个字节
                    for (int i = 1; i <= Math.ceil(messageBytes.length / 4); i++) {
                        byte[] defaultBytes = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                        int len = i == Math.ceil(messageBytes.length / 4) ? messageBytes.length % 4 : 4;
                        System.arraycopy(writeCmd, 0, WRITE, 0, writeCmd.length);
                        System.arraycopy(messageBytes, 4 * (i - 1), defaultBytes, 0, len);
                        System.arraycopy(defaultBytes, 0, WRITE, writeCmd.length, defaultBytes.length);
                        Log.d("h_w", byte2HexString(WRITE));
                        nfca.transceive(WRITE);
                    }
                    nfca.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();

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
            Tag tag = parseIntent(intent);
            if (tag != null)
                writeNfcA(tag, "helloworld");
            //readNfcA(tag);
        }
    }

    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editTextNFC);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
}