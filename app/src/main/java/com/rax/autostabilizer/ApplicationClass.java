package com.rax.autostabilizer;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rax.autostabilizer.Utilities.S_Communication;
import com.rax.autostabilizer.Utilities.UtilMethods;

import static com.rax.autostabilizer.Utilities.S_Communication.ACTION_MyIntentService;
import static com.rax.autostabilizer.Utilities.S_Communication.RECEIVED_DATA;

public class ApplicationClass extends Application {
    Context mContext;
    TCPDataListener mTCPTCPDataListener;
    CountDownTimer mPacketTimeout;
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
                    mTCPTCPDataListener.OnDataReceive(data);
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
                Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
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
    }

    public void sendPacket(final TCPDataListener listener, String packet) {
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
                Toast.makeText(mContext, "Request timeout", Toast.LENGTH_SHORT).show();
                listener.OnDataReceive("timeOut");
            }
        };
        if (!packet.equals("")) {
            mPacketTimeout.start();
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

    public String framePack(String data) {
        return "*" + data + UtilMethods.CRCCalc(data) + "#";
    }

    public interface TCPDataListener {
        void OnDataReceive(String data);
    }

}
