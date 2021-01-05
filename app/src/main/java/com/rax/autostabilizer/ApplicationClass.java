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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttMessageDeliveryCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttSubscriptionStatusCallback;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPolicyRequest;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rax.autostabilizer.Activities.StabilizerListActivity;
import com.rax.autostabilizer.Activities.StabilizerStatusActivity;
import com.rax.autostabilizer.Utilities.S_Communication;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.rax.autostabilizer.Utilities.S_Communication.ACTION_MyIntentService;
import static com.rax.autostabilizer.Utilities.S_Communication.RECEIVED_DATA;

public class ApplicationClass extends Application implements DataReceiveCallback {
    Context mContext;
    DataListener mTCPTCPDataListener;
    CountDownTimer mPacketTimeout;

    //AWS
    public static final String clientId = UUID.randomUUID().toString();
    public static final String accountID = "484643486344";
    public static final Regions region = Regions.US_EAST_1;
    public static final String endPoint = "a1i25lg7rvcymv-ats.iot.us-east-1.amazonaws.com";
    public static final String policyName = "pawsiotexpo1";
    public static final String cognitoPoolID = "us-east-1:7a40cebf-4d98-4619-a512-7a8ef4b03449";  // IRC
    private static final String TAG = "ApplicationClass";
    public DataReceiveCallback callback;
    AWSIotMqttManager awsIotMqttManager;
    CognitoCachingCredentialsProvider credentialsProvider;
    final String publishFail = "sendFailed", connected = "Connected";
    public static String macAddress;
    public static String publishTopic = "topic/" + macAddress + "/app2irc",
            subscribeTopic = "topic/" + macAddress + "/irc2app";


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
        callback = this::OnAWSDataReceive;
    }

    public void sendData() {

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
                Toast.makeText(mContext, "Request timeout", Toast.LENGTH_SHORT).show();
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
        return "SST#" + packet + "#ED";
    }

    public interface DataListener {
        void OnDataReceived(String data);
    }

    // AWS
    public void initializeClient(Context context) {
        startInitialize(context);
    }

    public void initializeClient(CognitoCachingCredentialsProvider provider, String cognitoID) {
        if (awsIotMqttManager == null) {
            awsIotMqttManager = new AWSIotMqttManager(clientId, endPoint);
            AttachPolicyRequest request = new AttachPolicyRequest();
            request.setPolicyName(policyName);
            request.setTarget(cognitoID);
            AWSIotClient mIotAndroidClient = new AWSIotClient(provider);
            mIotAndroidClient.setRegion(Region.getRegion(region));
            new AttachPolicy(mIotAndroidClient, provider, awsIotMqttManager).execute(request);
        } else {
            Connect(awsIotMqttManager, provider);
        }
    }

    public void startInitialize(Context context) {
        new GetTarget(getCredentialProvider(context, accountID, cognitoPoolID, region)).execute();
    }

    public CognitoCachingCredentialsProvider getCredentialProvider(Context mContext, String accountID, String cognitoPoolID, Regions region) {
        if (credentialsProvider == null) {
            //Need only for authentication
            //DeveloperAuthenticationProvider developerProvider = new DeveloperAuthenticationProvider(mContext, accountID, cognitoPoolID, region);
            credentialsProvider = new CognitoCachingCredentialsProvider(mContext, cognitoPoolID, Regions.US_EAST_1);
            return credentialsProvider;
        } else {
            return credentialsProvider;
        }

    }

    public void reConnect() {
        if (awsIotMqttManager != null && credentialsProvider != null) {
            Connect(awsIotMqttManager, credentialsProvider);
        } else {
            Log.d(TAG, "reConnect: RECONNECTION FAILED");
        }
    }

    public void Connect(AWSIotMqttManager mqttManager, CognitoCachingCredentialsProvider provider) {
        if (mqttManager != null) {
            mqttManager.connect(provider, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
                    Log.e(TAG, String.valueOf(status));
                    callback.OnAWSDataReceive(String.valueOf(status));
                }
            });
        } else {
            Connect(mqttManager, provider);
        }
    }

    public void AWSDisConnect() {
        if (awsIotMqttManager != null) {
            awsIotMqttManager.disconnect();
            Log.d(TAG, "AWSDisConnect: DISCONNECT");
        }
    }

    public void publish(final String data, String topic, final DataReceiveCallback callback) {
        this.callback = callback;
        if (awsIotMqttManager != null) {
            awsIotMqttManager.publishString(data, topic, AWSIotMqttQos.QOS0, new AWSIotMqttMessageDeliveryCallback() {
                @Override
                public void statusChanged(MessageDeliveryStatus status, Object userData) {
                    if (status.equals(MessageDeliveryStatus.Fail)) {
                        Log.d(TAG, "newLog" + "publishFail");
                        callback.OnAWSDataReceive(publishFail);
                    } else {
                        Log.d(TAG, "OnAWSDataReceive: AWS send --> " + data);
                    }
                }
            }, new Object());
        }
    }

    public void subscribe(String topic) {
        if (awsIotMqttManager != null) {
            awsIotMqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0, new AWSIotMqttSubscriptionStatusCallback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailure(Throwable exception) {
                            Log.e(TAG, "Failed: ");
                        }
                    },
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(String topic, byte[] data) {
                            if (callback != null) {
                                callback.OnAWSDataReceive(new String(data, StandardCharsets.UTF_8));
                                Log.e(TAG, "Receive <- " + new String(data, StandardCharsets.UTF_8));
                            }
                        }
                    });
        }
    }

    public class GetTarget extends AsyncTask<Void, Void, Void> {
        CognitoCachingCredentialsProvider provider;
        String target;

        public GetTarget(CognitoCachingCredentialsProvider provider) {
            this.provider = provider;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            target = provider.getIdentityId();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (target != null)
                initializeClient(provider, target);
        }
    }

    public class AttachPolicy extends AsyncTask<AttachPolicyRequest, Void, Void> {
        AWSIotClient mIotAndroidClient;
        CognitoCachingCredentialsProvider provider;
        AWSIotMqttManager manager;

        AttachPolicy(AWSIotClient mIotAndroidClient, CognitoCachingCredentialsProvider provider, AWSIotMqttManager manager) {
            this.mIotAndroidClient = mIotAndroidClient;
            this.provider = provider;
            this.manager = manager;
        }

        @Override
        protected Void doInBackground(AttachPolicyRequest... attachPolicyRequests) {
            try {
                mIotAndroidClient.attachPolicy(attachPolicyRequests[0]);
            } catch (AmazonClientException e) {
                callback.OnAWSDataReceive("Unable to connect");
                this.cancel(true);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Connect(manager, provider);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    @Override
    public void OnAWSDataReceive(String data) {
        Log.d(TAG, "1) OnAWSDataReceive: AWS_RECEIVE " + data);
    }

    //Connection Preference
//    public String checkNetwork() {
//        if (mobileDataCheck(getApplicationContext())) {
//            return "mobileData";
//        } else if (wifiCheck()) {
//            return "wifi";
//        } else {
//            return "nothing";
//        }
//    }

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
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean) method.invoke(cm);
        } catch (Exception e) {
            mobileDataEnabled = false;
            //e.printStackTrace();
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

    public void showConnectionPop(Context context){
        new MaterialAlertDialogBuilder(context)
                .setTitle("Network Error")
                .setMessage("Not Connected to network")
                .setPositiveButton("mobile Data", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(mContext, "mobile Data", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                    }
                }).setNegativeButton("Wifi", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(mContext, "Wifi", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(mContext, "Cancel", Toast.LENGTH_SHORT).show();
                dialogInterface.dismiss();
            }
        }).show();

    }
}
