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


@SuppressWarnings("ALL")
public class S_Communication extends IntentService {

    public static final String ACTION_MyIntentService = "com.example.w.RESPONSE", CONNECTED = "Connected", RECEIVED_DATA = "ReceivedData";
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
                    if (send()) {
                        Receive();
                    }
                }
            } catch (Exception e1) {
                intentMessage("No Device");
                Log.d("Send Message", "No Device");
            }

        }

        public boolean send() {
            PrintWriter out0;
            Log.d("Send Message", Packet);
            try {
                out0 = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(
                                socketDevice.getOutputStream())), true);

                out0.println(Packet);

                Log.e("SR1COMM", "Send  " + Packet);

                return true;
            } catch (Exception e) {
                intentMessage("sendCatch");
                Log.d("Send Message", e.getMessage());
            }
            intentMessage("pckError");
            Log.d("Send Message", "Packet Error");
            close();
            return false;
        }

        public void Receive() {
            try {
                char[] buffer = new char[2048];
                int charsRead = 0;
                while ((charsRead = _inputSteam.read(buffer)) != -1) {
                    String message = new String(buffer).substring(0, charsRead);
                    if (!message.isEmpty()) {
                        intentMessage(message);
                        Log.e("SR1COMM", "Receive  " + message);
                    } else {
                        Log.d("Receive Error Message", message);
                    }
                }
            } catch (java.io.InterruptedIOException e) {
                intentMessage("timeOut");
                Log.d("Receive Message", e.getMessage());
                Log.d("Receive Message", "timeOut");
            } catch (UnknownHostException e1) {
                intentMessage("UnknownHostException");
                Log.d("Receive Message", e1.getMessage());
                Log.d("Receive Message", "UnknownHostException");
            } catch (IOException e1) {
                //    intentMessage("restart");
                Log.d("Receive Message", e1.getMessage());
                Log.d("Receive Message", "restart");
            }
        }

        public boolean Connect() {

            try {
                if (socketDevice == null) {
                    socketDevice = new Socket();
                    socketDevice.connect(new InetSocketAddress(
                            mIPAddress, mPortNumber), 30000);
                    _inputSteam = new BufferedReader(new InputStreamReader(socketDevice.getInputStream()));
                    socketDevice.setKeepAlive(true);
                    socketDevice.setSoLinger(true, 1);
                    intentMessage("Connected");
                    Log.d("Communication", "Device Connected");

                }
                return true;
            } catch (UnknownHostException e1) {
                intentMessage("UnknownHostException");
                Log.d("Communication", e1.getMessage());
                socketDevice = null;
            } catch (IOException e1) {
//                intentMessage("No Device");
                intentMessage("FailedToConnect");
                Log.d("Communication", e1.getMessage());
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
                        socketDevice.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    socketDevice = null;
                }
            }
        }
    }
}

