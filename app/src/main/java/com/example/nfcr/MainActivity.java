package com.example.nfcr;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
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
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    public static final String[] ADAMA_URL = {
            "https://cn.bing.com",
            "https://itodemosga01.z13.web.core.windows.net/apollo.html",
            "https://itodemosga01.z13.web.core.windows.net/apollo.html"
    };
    NfcAdapter nfcAdapter;
    TextView nfcTextView;
    EditText nfcEditText;
    Button buttonWriteIn;
    String[][] techListsArray;
    IntentFilter[] intentFiltersArray;
    PendingIntent pendingIntent;
    Tag tag = null;
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
            if (msg.what == 2) {
                String text = (String) msg.obj;
                nfcTextView.append(text);
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
                if (tag != null & Arrays.asList(tag.getTechList()).contains("android.nfc.tech.Ndef")) {
                    /**写入格式例如 aaaaa:1 ,如果没有指定:1,则使用测试URL*/
                    int urlIndex = 0;
                    String text = nfcEditText.getText().toString();
                    String[] splitText = text.split(";");
                    if (splitText.length == 2) {
                        urlIndex = Integer.parseInt(splitText[1]);
                        text = splitText[0];
                    }
                    /*写入NFC*/
                    if (urlIndex > ADAMA_URL.length - 1 || urlIndex < 0)
                        Toast.makeText(MainActivity.this,
                                "url index is invalid , the url index must between 0 and " + String.valueOf(ADAMA_URL.length - 1)
                                , Toast.LENGTH_SHORT).show();
                    else {
                        writeNedf(tag, text, urlIndex);
                    }
                }

            }
        });

    }


    public Tag parseIntent(Intent intent) {
        String nfcAction = intent.getAction();
        Log.d("h_bl", nfcAction);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(nfcAction)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG); // 获取Tag标签，既可以处理相关信息
            Log.d("h_bl", "id = " + tag.getId());
            nfcTextView.setText(null);
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
                            (byte) 0x04,
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
                    Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                }

            }
        }).start();
    }


    public void readNedf(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef != null)
            Log.d("h_bl", ndef.toString());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ndef.connect();
                    NdefMessage ndefMessage = ndef.getNdefMessage();
                    NdefRecord[] recordsecord = ndefMessage.getRecords();
                    for (NdefRecord fRecord : recordsecord) {
                        Log.d("h_kk", String.valueOf(fRecord.getTnf()));
                        byte[] payload = fRecord.getPayload();
                        if (payload != null) {
                            Log.d("h_bl", new String(payload));
                            Log.d("h_bl", byte2HexString(payload));
                            Bundle bundle = new Bundle();
                            bundle.putString("hex", byte2HexString(payload));
                            bundle.putString("ASCII", new String(payload));
                            Message message = Message.obtain();
                            message.setData(bundle);
                            message.what = 1;
                            handler.sendMessage(message);
                        }
                    }
                    ndef.close();
                } catch (IOException | FormatException e) {
                    Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

            }
        }).start();
    }


    public void writeNfcA(Tag tag, String nfcCode) {
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
                    byte[] messageBytes = nfcCode.getBytes(StandardCharsets.US_ASCII);
                    byte[] WRITE = new byte[6];

                    /**Empty the page from 6 to 30**/
                    for (int page = 6; page <= 30; page++) {
                        byte[] defaultBytes = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                        byte[] writeCmd = {(byte) 0xA2, (byte) page};
                        System.arraycopy(writeCmd, 0, WRITE, 0, writeCmd.length);
                        System.arraycopy(defaultBytes, 0, WRITE, writeCmd.length, defaultBytes.length);
                        nfca.transceive(WRITE);
                    }
                    int totalStep = (int) Math.ceil((double) messageBytes.length / 4.0);
                    //写命令后面只能跟4个字节
                    for (int step = 1, page = 6; step <= totalStep; step++, page++) {
                        Log.d("step", String.valueOf(step));
                        byte[] writeCmd = {(byte) 0xA2, (byte) page};
                        byte[] defaultBytes = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                        int len = step == totalStep && (messageBytes.length % 4 != 0) ? messageBytes.length % 4 : 4;
                        System.arraycopy(writeCmd, 0, WRITE, 0, writeCmd.length);
                        System.arraycopy(messageBytes, 4 * (step - 1), defaultBytes, 0, len);
                        System.arraycopy(defaultBytes, 0, WRITE, writeCmd.length, defaultBytes.length);
                        Log.d("h_w", byte2HexString(WRITE));
                        nfca.transceive(WRITE);
                    }
                    Message message = Message.obtain();
                    message.obj = nfcCode + " Forged!";
                    message.what = 2;
                    handler.sendMessage(message);
                    nfca.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }

    public void writeNedf(Tag tag, String nfcCode, int urlIndex) {
        String url = ADAMA_URL[urlIndex];
        Ndef ndef = Ndef.get(tag);
        if (ndef != null)
            Log.e("e_bl", ndef.toString());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ndef.connect();
                    NdefRecord rtdUriRecord = NdefRecord.createUri(url);
                    String domain = "com.nfcr"; //usually your app's package name
                    String type = "AdamaID";
                    NdefRecord extRecord = NdefRecord.createExternal(domain, type, nfcCode.getBytes(Charset.forName("US-ASCII")));
                    NdefMessage ndefMessage = new NdefMessage(rtdUriRecord, extRecord);

                    ndef.writeNdefMessage(ndefMessage);
                    Message message = Message.obtain();
                    message.obj = nfcCode + ";" + url + " Forged!";
                    message.what = 2;
                    handler.sendMessage(message);
                    ndef.close();
                } catch (IOException | FormatException e) {
                    Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
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
            tag = parseIntent(intent);
            if (tag != null) {
//                writeNfcA(tag, "helloworld");
                if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.Ndef")) {
                    readNedf(tag);
                    return;
                } else if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.NfcA")) {
                    readNfcA(tag);
                    return;
                } else return;
            }
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