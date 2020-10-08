package com.rax.autostabilizer.Utilities;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;


/*
 * Do not use this class for other projects, this class is modified for autoStabilizer,
 * */
@SuppressWarnings("ALL")
public class S_Communication extends IntentService {

    public static final String ACTION_MyIntentService = "com.example.w.RESPONSE", CONNECTED = "Connected", RECEIVED_DATA = "ReceivedData";
    private static final String TAG = "S_Communication";
    public static Socket socketDevice = null;
    public static BufferedReader _inputSteam;
    public static String Packet, mIPAddress;
    public static int mPortNumber;


    public S_Communication() {
        super("Test the service");
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        String stringPassedToThisService = intent.getStringExtra("test");

        if (stringPassedToThisService != null) {

            new Thread(new SendToDevice(stringPassedToThisService)).start();
        }
    }

    public void stop() {
        SendToDevice STD = new SendToDevice(null);
        STD.close();
    }

    private void intentMessage(String message) {
        Intent intentResponse = new Intent();
        intentResponse.setAction(ACTION_MyIntentService);
        intentResponse.addCategory(Intent.CATEGORY_DEFAULT);
        intentResponse.putExtra(RECEIVED_DATA, message);
        sendBroadcast(intentResponse);
    }


//    public void framepack(String pack) {
//        packetBuffer = packetBuffer.append(pack);
//        int indexOfEnd = 0;
//        if (packetBuffer.toString().contains("#")) {
//            indexOfEnd = packetBuffer.indexOf("#");
//            String fPack = packetBuffer.toString().substring(0, indexOfEnd + 1);
//            intentMessage(fPack);
//            Log.e("MessagePacket", "Receive  " + fPack + "\n\n");
//            packetBuffer.setLength(0);
//        }
//    }

    class SendToDevice implements Runnable {
        private String m_command;

        SendToDevice(String command) {
            Packet = command;
        }

        @Override
        public void run() {

            try {
                if (Connect()) {
                    if (!Packet.equals("")) {
                        if (send()) {
                            Receive();
                        }
                    }
                }
            } catch (Exception e1) {
                intentMessage("No Device");
                Log.d(TAG, "No Device");
            }

        }

        public boolean send() {
            PrintWriter out0;
            try {
                out0 = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(
                                socketDevice.getOutputStream())), true);

                out0.println(Packet);

                Log.d(TAG, "Send: " + Packet);

                return true;
            } catch (Exception e) {
                intentMessage("sendCatch");
                Log.d(TAG, e.getMessage());
            }
            intentMessage("pckError");
            Log.d(TAG, "Packet Error");
            return false;
        }

        public void Receive() {
            boolean dataReceived = false;
            String data = "";
            try {
                char[] buffer = new char[2048];
                int charsRead = 0;
                while ((charsRead = _inputSteam.read(buffer)) != -1) {
                    String message = new String(buffer).substring(0, charsRead);
                    if (!message.isEmpty()) {
                        dataReceived = true;
                        data = message;
                        Log.e("SR1COMM", "Receive  " + message);
                    } else {
                        Log.d("Receive Error Message", message);
                    }
                }
            } catch (java.io.InterruptedIOException e) {
                intentMessage("timeOut");
                Log.d(TAG, e.getMessage());
                Log.d(TAG, "timeOut");
            } catch (UnknownHostException e1) {
                intentMessage("UnknownHostException");
                Log.d(TAG, e1.getMessage());
                Log.d(TAG, "UnknownHostException");
            } catch (IOException e1) {
                intentMessage("restart");
                Log.d(TAG, e1.getMessage());
                Log.d(TAG, "restart");
            }
            close();
            Log.d(TAG, "Receive: " + data);
            intentMessage(data);
        }

        public boolean Connect() {
            Log.d(TAG, "Connect: " + (socketDevice == null));
            try {
                if (socketDevice == null) {
                    Log.d(TAG, "Connect: ");
                    socketDevice = new Socket();
                    socketDevice.connect(new InetSocketAddress(
                            mIPAddress, mPortNumber), 30000);
                    _inputSteam = new BufferedReader(new InputStreamReader(socketDevice.getInputStream()));
                    socketDevice.setKeepAlive(true);
                    socketDevice.setSoLinger(true, 1);
                    intentMessage("Connected");
                    Log.d(TAG, "Device Connected");
                }
                return true;
            } catch (UnknownHostException e1) {
                intentMessage("UnknownHostException");
                Log.d(TAG, e1.getMessage());
                socketDevice = null;
            } catch (IOException e1) {
//                intentMessage("No Device");
                intentMessage("FailedToConnect");
                Log.d(TAG, e1.getMessage());
                socketDevice = null;
            } catch (Exception e) {
                socketDevice = null;
            }
            return false;
        }

        public void close() {
            if (socketDevice != null) {
                if (socketDevice.isConnected()) {
                    try {
                        Log.d(TAG, "closing ");
                        socketDevice.close();
                        Log.d(TAG, "closed ");
                    } catch (IOException e) {
                        Log.d(TAG, "Excep: " + e.getMessage());
                        e.printStackTrace();
                    }
                    Log.d(TAG, "close: ");
                    socketDevice = null;
                }
            }
        }
    }
}

