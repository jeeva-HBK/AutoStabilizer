package com.rax.autostabilizer.Utilities;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;

public class UtilMethods {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String CRCCalc(String Crcmsg) {

        byte[] bytes = Crcmsg.getBytes(StandardCharsets.US_ASCII);
        String revCRC = String.format("%02X", crcCalculate(bytes));
        if (revCRC.length() > 2) {
            revCRC = revCRC.substring(revCRC.length() - 2);
        }
        return revCRC;

    }

    public static boolean checkCRC(String data, String actualCRC) {
        return CRCCalc(data).equals(actualCRC);
    }

    public static short crcCalculate(byte[] crcData) {

        byte carry, crc = 0;

        for (long temp : crcData) {
            crc = (byte) (crc ^ temp);
            for (byte j = 8; j != 0; j--) {
                carry = (byte) (crc & 0x80);
                crc <<= 1;
                if (carry != 0) {
                    crc ^= 0x07; /* 0x07 for Polynom X8 + X2 + X + 1 ; 0x01 for Polynom X^8 + 1*/
                }
            }
        }

        Log.d("Final CRC", String.valueOf(crc));
        return crc; // & 0x00FF);
    }


}

