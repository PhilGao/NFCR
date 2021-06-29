package com.example.nfcr;

import android.annotation.SuppressLint;
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
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodCallback;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    public static final String[] ADAMA_URL = {
            "",
            "https://iotdemosga01.z13.web.core.windows.net/kohinor.html",
            "https://iotdemosga01.z13.web.core.windows.net/badge.html"
    };

    private final String connString = BuildConfig.DeviceConnectionString;
    private DeviceClient client;
    IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

    NfcAdapter nfcAdapter;
    TextView nfcTextView;
    EditText nfcEditText;
    Button buttonWriteIn;
    String[][] techListsArray;
    IntentFilter[] intentFiltersArray;
    PendingIntent pendingIntent;
    Tag tag = null;
    private final Handler handler = new Handler(Looper.myLooper()) {
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
                nfcTextView.append(text + "\r\n");
            }
        }
    };

    public static String byte2HexString(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        if (bytes != null) {
            for (Byte b : bytes) {
                hex.append(String.format("%02X", b.intValue() & 0xFF));
            }
        }
        return hex.toString();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcTextView = (TextView) findViewById(R.id.textViewNFC);
        nfcEditText = (EditText) findViewById(R.id.editTextNFC);
        buttonWriteIn = (Button) findViewById(R.id.buttonWriteIn);
        techListsArray = new String[][]{{NfcA.class.getName()}, {NfcB.class.getName()}, {IsoDep.class.getName()}, {NfcV.class.getName()}, {NfcF.class.getName()},};

        try {
            initClient();
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }


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
        if (!nfcAdapter.isEnabled()) {
            nfcTextView.setText("Please open nfc");
            Log.d("h_bl", "Not Open NFC");
            return;
        }
        buttonWriteIn.setOnClickListener(v -> {
            assert tag != null;
            if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.NdefFormatable")) {
                writeNdefFormatable(tag);
            }
            if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.Ndef")) {
                /*写入格式例如 nfc_code;1 ,如果没有指定:1,则使用测试URL*/
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
                            "url index is invalid , the url index must between 0 and " + (ADAMA_URL.length - 1)
                            , Toast.LENGTH_SHORT).show();
                else {
                    writeNdef(tag, text, urlIndex);
//                    writeNfcA(tag,text);
//                    writeNfcAIntital(tag,text);
                }
            }

        });

    }

    private void initClient() throws URISyntaxException, IOException {
        client = new DeviceClient(connString, protocol);
        try {
            client.registerConnectionStatusChangeCallback(new IotHubConnectionStatusChangeCallbackLogger(), new Object());
            client.open();
            client.setMessageCallback((message, callbackContext) -> {
                Log.d("IOT", "Received message with content: " + new String(message.getBytes(), com.microsoft.azure.sdk.iot.device.Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));
                return IotHubMessageResult.COMPLETE;
            }, null);

            client.subscribeToDeviceMethod(
                    (DeviceMethodCallback) (methodName, methodData, context) -> null,
                    getApplicationContext(),
                    new EventCallback()
                    , null);
        } catch (Exception e) {
            Log.e("IOT", "Exception while opening IoTHub connection: " + e);
            client.closeNow();
            Log.e("IOT", "Shutting down...");
        }
    }

    class EventCallback implements IotHubEventCallback {
        public void execute(IotHubStatusCode status, Object context) {

            String result = "IoT Hub responded to message " + context.toString()
                    + " with status " + status.name();
            Log.d("IOT", result);
            Log.d("IOT", "EventCallBack from Event Hub");
            String finalResult = result + "\r\n";
            handler.post(() -> nfcTextView.append(finalResult));
        }
    }

    public Tag parseIntent(Intent intent) {
        String nfcAction = intent.getAction();
        Log.d("h_bl", nfcAction);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(nfcAction)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG); // 获取Tag标签，既可以处理相关信息
            Log.d("h_bl", "id = " + Arrays.toString(tag.getId()));
            nfcTextView.setText(null);
            return tag;
        }
        return null;
    }


    public void readNfcA(Tag tag) {
        NfcA nfca = NfcA.get(tag);
        if (nfca != null)
            Log.d("h_bl", nfca.toString());
        new Thread(() -> {
            try {
                assert nfca != null;
                nfca.connect();
                Log.d("h_bl", String.valueOf(nfca.getMaxTransceiveLength()));
                Log.d("h_bl", String.valueOf(nfca.getSak()));
                //读取06之后的4个字节
                byte[] SELECT = {
                        (byte) 0x3A,
                        (byte) 0x01,
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
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }).start();
    }


    public void readNdef(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef != null)
            Log.d("h_bl", ndef.toString());
        new Thread(() -> {
            try {
                assert ndef != null;
                ndef.connect();
                if (ndef.isWritable()) {
                    Log.d("h_28282", "Tag is writable");
                }
                NdefMessage ndefMessage = ndef.getNdefMessage();
                if (ndefMessage == null) {
                    ndef.close();
                    writeNfcAInitial(tag);
                    return;
                }
                NdefRecord[] recorders = ndefMessage.getRecords();
                for (NdefRecord fRecord : recorders) {
                    Log.d("h_readNdef", String.valueOf(fRecord.getTnf()));
                    Log.d("h_readNdef", new String(fRecord.getType()));
                    byte[] payload = fRecord.getPayload();
                    if (payload != null) {
                        if (new String(fRecord.getType()).equals("com.nfcr:adamaid")) {
                            //send nfc token & nfc id to iot
                            AdamaMessage adamaMessage = new AdamaMessage(new String(payload));
                            Gson gson = new Gson();
                            String message = gson.toJson(adamaMessage);
                            client.sendEventAsync(new com.microsoft.azure.sdk.iot.device.Message(message),
                                    new EventCallback(), "SendNfcAdamaid"
                            );
                        }
                        Log.d("h_readNdef", new String(payload));
                        Log.d("h_readNdef", byte2HexString(payload));
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
                e.printStackTrace();
            }

        }).start();
    }

    public void writeNfcAInitial(Tag tag) {

        NfcA nfca = NfcA.get(tag);
        byte[] WRITE = {(byte) 0xA2, (byte) 0x05, (byte) 0x34, (byte) 0x03, (byte) 0x10, (byte) 0xD1};
        if (nfca != null) {
            Log.e("e_bl", nfca.toString());
            try {
                nfca.connect();
                byte[] response = nfca.transceive(WRITE);
                Log.d("h_ii", byte2HexString(response));
                nfca.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Deprecated
    public void writeNfcA(Tag tag, String nfcCode) {
        NfcA nfca = NfcA.get(tag);
        if (nfca != null)
            Log.e("e_bl", nfca.toString());
        new Thread(() -> {
            try {
                assert nfca != null;
                nfca.connect();
                Log.d("h_bl", String.valueOf(nfca.getMaxTransceiveLength()));
                Log.d("h_bl", String.valueOf(nfca.getSak()));
                byte[] messageBytes = nfcCode.getBytes(StandardCharsets.US_ASCII);
                byte[] WRITE = new byte[6];

                nfca.transceive(WRITE);

                /*Empty the page from 6 to 30**/
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

        }).start();

    }

    public void writeNdefFormatable(Tag tag) {
        NdefFormatable nfcdf = NdefFormatable.get(tag);
        if (nfcdf != null)
            Log.e("e_bl", nfcdf.toString());
        new Thread(() -> {
            try {
                assert nfcdf != null;
                nfcdf.connect();
                nfcdf.format(null);
                Message message = Message.obtain();
                message.obj = "nfcdf Forged!";
                message.what = 2;
                handler.sendMessage(message);
                nfcdf.close();
            } catch (IOException | FormatException e) {
                e.printStackTrace();
            }

        }).start();
    }

    public void writeNdef(Tag tag, String nfcCode, int urlIndex) {
        String url = ADAMA_URL[urlIndex];
        Ndef ndef = Ndef.get(tag);
        if (ndef != null)
            Log.e("e_bl", ndef.toString());
        new Thread(() -> {
            try {
                assert ndef != null;
                ndef.connect();
                NdefRecord rtdUriRecord = NdefRecord.createUri(url);
                String domain = "com.nfcr"; //usually your app's package name
                String type = "AdamaID";
                NdefRecord extRecord = NdefRecord.createExternal(domain, type, nfcCode.getBytes(StandardCharsets.US_ASCII));
                NdefMessage ndefMessage = new NdefMessage(rtdUriRecord, extRecord);
                ndef.writeNdefMessage(ndefMessage);
                Message message = Message.obtain();
                message.obj = nfcCode + ";" + url + " Forged!";
                message.what = 2;
                handler.sendMessage(message);
                ndef.close();
            } catch (IOException | FormatException e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
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
            Log.d("h_bl", Arrays.toString(tag.getTechList()));
            if (tag != null) {
                if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.Ndef")) {
                    readNdef(tag);
                }
            }
        }
    }

}