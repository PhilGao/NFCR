package com.example.nfcr;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CancellationException;

public class AdamaMessage {
    private String AccountId ;
    private String DeviceId ;
    private String NFCId;
    private String BottleToken;
    private String EntryDatetime;

    public static String byte2HexString(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        if (bytes != null) {
            for (Byte b : bytes) {
                hex.append(String.format("%02X-", b.intValue()));
            }
        }
        return hex.substring(0,hex.length()-1);
    }

    public AdamaMessage(String nfcId) {
        Date d  = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat ssdf = new SimpleDateFormat("yyMMddHHmmssms");
        EntryDatetime = sdf.format(d);
        AccountId ="0x71be63f3384f5fb98995898a86b02fb2426c5788";
        DeviceId = "device03";
        BottleToken = byte2HexString(ssdf.format(d).getBytes(StandardCharsets.UTF_8));
        NFCId = nfcId;
    }
}
