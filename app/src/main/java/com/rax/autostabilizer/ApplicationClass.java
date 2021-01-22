package com.rax.autostabilizer;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.rax.autostabilizer.Utilities.S_Communication;

import org.acra.ACRA;
import org.acra.annotation.AcraMailSender;
import org.acra.annotation.AcraToast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import static com.rax.autostabilizer.Utilities.S_Communication.ACTION_MyIntentService;
import static com.rax.autostabilizer.Utilities.S_Communication.RECEIVED_DATA;

@AcraMailSender(mailTo = "jeeva.raxtech@gmail.com")
@AcraToast(resText = R.string.CrashReportMessage)

// Created by Loki
// Edited by jeeva
public class ApplicationClass extends Application {
    Context mContext;
    DataListener mTCPTCPDataListener;
    CountDownTimer mPacketTimeout;
    // Static Strings
    private static final String TAG = "ApplicationClass";
    public static String macAddress,
    // Packet Strucure
    startPacket = "SST#",
            endPacket = "#ED",

    // MqttTopics
    topic = "topic/",
            pubTopic = "/app2irc",
            subTopic = "/irc2app";

    BroadcastReceiver mTCPDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                String data;
                data = intent.getStringExtra(RECEIVED_DATA);
                if (mPacketTimeout != null) {
                    mPacketTimeout.cancel();
                }
                if (mTCPTCPDataListener != null) {
                    // Without CRC
                    mTCPTCPDataListener.OnDataReceived(data);
                    // With CRC
                    /*if (data.contains("*") && data.contains("#")) {
                        data = data.split("\\*")[1].split("#")[0];
                        String[] splitted = data.split(";");
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < splitted.length; i++) {
                            if (i != splitted.length - 1) {
                                builder.append(splitted[i]);
                                builder.append(";");
                            }
                        }
                        mTCPTCPDataListener.OnDataReceive(data);
                        if (UtilMethods.checkCRC(builder.toString(), splitted[splitted.length - 1])) {
                            mTCPTCPDataListener.OnDataReceive(data);
                        } else {
                            mTCPTCPDataListener.OnDataReceive("Invalid CRC");
                        }
                    } else {
                        mTCPTCPDataListener.OnDataReceive(data);
                    }*/
                }
            } catch (Exception e) {
                //Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {

            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                try {
                    mContext = activity;
                    registerReceiver();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                unregisterReceiver();
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        });
        // TEMP
        if (isExternalStorageWritable()) {
            File appDirectory = new File(Environment.getExternalStorageDirectory() + "/AutoStabilizer");
            File logDirectory = new File(appDirectory + "/logs");
            File logFile = new File(logDirectory, "logcat_" + System.currentTimeMillis() + ".txt");

            // create app folder
            if (!appDirectory.exists()) {
                appDirectory.mkdir();
            }

            // create log folder
            if (!logDirectory.exists()) {
                logDirectory.mkdir();
            }

            // clear the previous logcat and then write the new one to the file
            try {
                Process process = Runtime.getRuntime().exec("logcat -c");
                process = Runtime.getRuntime().exec("logcat -f " + logFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (isExternalStorageReadable()) {
              //  only readable
             //  Toast.makeText(mContext, "only Read", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "only Read");
        } else {
            // not accessible
            Log.e(TAG, "not accesible");
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
        // TEMP
    }

    public void sendPacket(final DataListener listener, String packet) {
        this.mTCPTCPDataListener = listener;
        if (mPacketTimeout != null) {
            mPacketTimeout.cancel();
        }
        mPacketTimeout = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                Toast.makeText(mContext, R.string.requestTimeOut, Toast.LENGTH_SHORT).show();
                listener.OnDataReceived("timeOut");
            }
        };
        if (!packet.equals("")) {
            //  mPacketTimeout.start();
        }
        Intent mServiceIntent = new Intent(mContext,
                S_Communication.class);
        mServiceIntent.putExtra("test", packet);
        mContext.startService(mServiceIntent);
    }

    public void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter(ACTION_MyIntentService);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        mContext.registerReceiver(mTCPDataReceiver, intentFilter);
    }

    public void unregisterReceiver() {
        mContext.unregisterReceiver(mTCPDataReceiver);
    }

    public String framePack(String packet) {
        return startPacket + packet + endPacket;
    }

    public interface DataListener {
        void OnDataReceived(String data);
    }

    public ConnectionMode checkNetwork() {
        if (mobileDataCheck(getApplicationContext())) {
            return ConnectionMode.AWSIoT;
        } else if (wifiCheck()) {
            return ConnectionMode.TCP;
        } else {
            return ConnectionMode.NONE;
        }
    }

    public boolean mobileDataCheck(Context context) {
        boolean mobileDataEnabled = false; // Assume disabled
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);
            mobileDataEnabled = (Boolean) method.invoke(cm);
        } catch (Exception e) {
            mobileDataEnabled = false;
        }
        return mobileDataEnabled;
    }

    public boolean wifiCheck() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            return true;
        }
        return false;
    }

    public void showConnectionPop(Context context) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Network Error")
                .setMessage("Not Connected to network")
                .setPositiveButton("mobile Data", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent myIntent = new Intent(Settings.ACTION_SETTINGS);
                        mContext.startActivity(myIntent);
                        Toast.makeText(mContext, "mobileData_Intent | Navigated to Settings", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Wifi", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent myIntent = new Intent(Settings.ACTION_SETTINGS);
                mContext.startActivity(myIntent);
                Toast.makeText(mContext, "Wifi_Intent | Navigated to Settings", Toast.LENGTH_SHORT).show();
            }
        }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(mContext, "Cancel", Toast.LENGTH_SHORT).show();
                dialogInterface.dismiss();
            }
        }).show();

    }

    public void showSnackBar(String message, CoordinatorLayout layout) {
        Snackbar.make(layout, message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void attachBaseContext(Context baseContext) {
        super.attachBaseContext(baseContext);
        ACRA.init(this);
    }
}

/*Version: 1.1.2 | Phase II*/
