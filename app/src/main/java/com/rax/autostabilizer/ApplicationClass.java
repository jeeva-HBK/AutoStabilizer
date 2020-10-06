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

import static com.rax.autostabilizer.Utilities.S_Communication.ACTION_MyIntentService;

public class ApplicationClass extends Application {
    Context mContext;
    TCPDataListener mTCPTCPDataListener;
    CountDownTimer mPacketTimeout;
    BroadcastReceiver mTCPDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String data;
                if (mPacketTimeout != null) {
                    mPacketTimeout.cancel();
                }
                if (mTCPTCPDataListener != null) {
                    data = intent.getStringExtra("received_data");
                    if (data.contains("restart")) {
                        return;
                    }
                    mTCPTCPDataListener.OnDataReceive(data);
                }
            } catch (Exception e) {
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

    public interface TCPDataListener {
        void OnDataReceive(String data);
    }

}
