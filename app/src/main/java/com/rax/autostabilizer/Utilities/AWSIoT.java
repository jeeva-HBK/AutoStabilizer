package com.rax.autostabilizer.Utilities;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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
import com.rax.autostabilizer.ApplicationClass;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class AWSIoT {
    public static AWSIoT awsIot;
    private static final String TAG = "AWSIoT";
    public static final String clientId = UUID.randomUUID().toString();
    public static final String accountID = "484643486344";
    public static final Regions region = Regions.US_EAST_1;
    public static final String endPoint = "a1i25lg7rvcymv-ats.iot.us-east-1.amazonaws.com";
    public static final String policyName = "pawsiotexpo1";
    public static final String cognitoPoolID = "us-east-1:7a40cebf-4d98-4619-a512-7a8ef4b03449";
    final String publishFail = "sendFailed", connected = "Connected";
    AWSIotMqttManager awsIotMqttManager;
    CognitoCachingCredentialsProvider credentialsProvider;
    public static ApplicationClass.DataListener mCallback;
    private boolean isConnected = false;
    public static final String AWS_CONNECTED = "ConnectedToAWS", AWS_NOT_CONNECTED = "NotConnectedToAWS";

    public boolean isConnected() {
        return isConnected;
    }

    public static AWSIoT getInstance(Context context, ApplicationClass.DataListener callback) {
        mCallback = callback;
        if (awsIot == null) {
            awsIot = new AWSIoT(context);
        }
        return awsIot;
    }

    public AWSIoT(Context context) {
        initializeClient(context);
    }

    public void initializeClient(Context context) {
        startInitialize(context);
    }

    public void initializeClient(CognitoCachingCredentialsProvider provider, String cognitoID) {
        if (awsIotMqttManager == null) {
            awsIotMqttManager = new AWSIotMqttManager(clientId, endPoint);
            awsIotMqttManager.setAutoReconnect(false);
            awsIotMqttManager.setCleanSession(true);
            AttachPolicyRequest request = new AttachPolicyRequest();
            request.setPolicyName(policyName);
            request.setTarget(cognitoID);
            AWSIotClient mIotAndroidClient = new AWSIotClient(provider);
            mIotAndroidClient.setRegion(Region.getRegion(region));
            new AttachPolicy(mIotAndroidClient, provider, awsIotMqttManager).execute(request);
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

    public void Connect(AWSIotMqttManager mqttManager, CognitoCachingCredentialsProvider provider) {
        if (mqttManager != null) {
            mqttManager.connect(provider, (status, throwable) -> {
                Log.e(TAG + " Status", String.valueOf(status));
                switch (status) {
                    case Connected:
                        isConnected = true;
                        mCallback.OnDataReceived(AWS_CONNECTED);
                        break;
                    case Connecting:
                        isConnected = false;
                        break;
                    case ConnectionLost:
                        isConnected = false;
                        disconnect();
                        mCallback.OnDataReceived(AWS_NOT_CONNECTED);
                        break;
                    case Reconnecting:
                        isConnected = false;
                        mCallback.OnDataReceived(AWS_NOT_CONNECTED);
                        break;
                }
            });
        }
    }

    public void publish(final String data, String topic, final ApplicationClass.DataListener callback) {
        mCallback = callback;
        if (!isConnected) {
            mCallback.OnDataReceived(AWS_NOT_CONNECTED);
            return;
        }
        if (awsIotMqttManager != null) {
            awsIotMqttManager.publishString(data, topic, AWSIotMqttQos.QOS0, (status, userData) -> {
                if (status.equals(AWSIotMqttMessageDeliveryCallback.MessageDeliveryStatus.Fail)) {
                    callback.OnDataReceived(publishFail);
                } else {
                    Log.e("AWS ", "Sent -> " + data);
                }
            }, new Object());
        }
        Log.e("AWS", "Publish - Topic <->" + topic);
    }

    public void subscribe(String topic) {
        if (!isConnected) {
            mCallback.OnDataReceived(AWS_NOT_CONNECTED);
            return;
        }
        if (awsIotMqttManager != null) {
            awsIotMqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0, new AWSIotMqttSubscriptionStatusCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "onSuccess: ");
                        }

                        @Override
                        public void onFailure(Throwable exception) {
                            Log.e(TAG, "Failed: ");
                        }
                    },
                    (topic1, data) -> {
                        if (mCallback != null) {
                            Log.e("AWS", "Received <- " + new String(data, StandardCharsets.UTF_8));
                            mCallback.OnDataReceived(new String(data, StandardCharsets.UTF_8));
                        }
                    });
        }
        Log.e("AWS", "Subscribe - Topic <->" + topic);
    }

    public void disconnect() {
        if (awsIot == null) {
            return;
        }
        awsIot = null;
        awsIotMqttManager.disconnect();
        awsIotMqttManager = null;
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
                mCallback.OnDataReceived("Unable to connect");
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


}
