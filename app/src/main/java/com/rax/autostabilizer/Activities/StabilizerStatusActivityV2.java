package com.rax.autostabilizer.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.rax.autostabilizer.ApplicationClass;
import com.rax.autostabilizer.ConnectionMode;
import com.rax.autostabilizer.R;
import com.rax.autostabilizer.Utilities.AWSIoT;
import com.rax.autostabilizer.Utilities.S_Communication;
import com.rax.autostabilizer.databinding.ActivityStabilizerStatusBinding;

import static com.rax.autostabilizer.ApplicationClass.macAddress;
import static com.rax.autostabilizer.ApplicationClass.pubTopic;
import static com.rax.autostabilizer.ApplicationClass.subTopic;
import static com.rax.autostabilizer.ApplicationClass.topic;
import static com.rax.autostabilizer.Utilities.AWSIoT.AWS_CONNECTED;
import static com.rax.autostabilizer.Utilities.AWSIoT.AWS_NOT_CONNECTED;

public class StabilizerStatusActivityV2 extends AppCompatActivity implements ApplicationClass.DataListener {

    private static final String TAG = "Stabilizer Status";
    ApplicationClass mAppClass;
    Context mContext = this;
    ActivityStabilizerStatusBinding mBinding;
    int keepAliveNackCount = 0, powerPacketNackCount = 0;
    String packetToSend;
    boolean isTimeDelay = false;
    private int mNackCount = 0;
    CountDownTimer keepAliveHandler = new CountDownTimer(Long.MAX_VALUE, 5000) {
        @Override
        public void onTick(long l) {
            sendKeepAlive();
        }

        @Override
        public void onFinish() {

        }
    };

    CountDownTimer packetSender = new CountDownTimer(15000, 5000) {
        @Override
        public void onTick(long millisUntilFinished) {
            switch (mAppClass.checkNetwork()) {
                case TCP:
                    mAppClass.sendPacket(StabilizerStatusActivityV2.this, packetToSend);
                    break;
                case AWSIoT:
                    if (awsIoT!=null){
                        awsIoT.publish(packetToSend, publishTopic, StabilizerStatusActivityV2.this);
                    }else {
                        mAppClass.showSnackBar("Network Changed, Please Restart !", mBinding.cod);
                    }
                    break;
                case NONE:
                    mAppClass.showSnackBar(getString(R.string.noConnection), mBinding.cod);
                    mAppClass.showConnectionPop(StabilizerStatusActivityV2.this);
                    break;
            }
        }

        @Override
        public void onFinish() {
            dismissProgress();
        }
    };

    AWSIoT awsIoT;

    //  AWS Topics
    private String publishTopic = topic + macAddress + pubTopic,
            subscribeTopic = topic + macAddress + subTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(
                this, R.layout.activity_stabilizer_status);
        mAppClass = (ApplicationClass) getApplication();

      /*  mBinding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_schedule) {
                startActivity(new Intent(StabilizerStatusActivityV2.this, ScheduleActivity.class));
            }
            return false;
        });*/

        mBinding.VwSchedule.setOnClickListener(view -> {
            startActivity(new Intent(StabilizerStatusActivityV2.this, ScheduleActivity.class));
        });

        mBinding.TgSleepMode.setOnClickListener(view -> {
            if (mBinding.TgSleepMode.isChecked()) {
                turnOnSleepMode();
            } else {
                turnOffSleepMode();
            }
        });

        mBinding.swtPower.setOnClickListener(view -> {
            showProgress();
            if (mBinding.swtPower.isChecked()) {
                turnOnOutput();
            } else {
                turnOffOutput();
            }
        });
    }

    private void closeTelnet() {
        S_Communication communication = new S_Communication();
        communication.stop();
    }

    public void initialize() {
        switch (mAppClass.checkNetwork()) {
            case TCP:
                break;
            case AWSIoT:
                awsIoT = AWSIoT.getInstance(mContext, this);
                if (awsIoT.isConnected()) {
                    awsIoT.subscribe(subscribeTopic);
                }
                break;
            case NONE:
                break;
        }
        readSleepData();
    }

    private void readSleepData() {
        sendData(mAppClass.framePack("4"));
    }

    private void turnOnSleepMode() {
        sendData(mAppClass.framePack("3#C"));
    }

    private void turnOffSleepMode() {
        sendData(mAppClass.framePack("3#D"));
    }

    private void turnOnOutput() {
        sendData(mAppClass.framePack("7#A"));
    }

    private void turnOffOutput() {
        sendData(mAppClass.framePack("8#B"));
    }

    private void startKeepAlive() {
        stopKeepAlive();
        keepAliveHandler.start();
    }

    private void stopKeepAlive() {
        keepAliveHandler.cancel();
    }

    private void sendData(String data) {
        stopKeepAlive();
        showProgress();
        packetToSend = data;
        packetSender.cancel();
        packetSender.start();
    }

    private void closeConnectivity() {
        if (awsIoT != null) {
            awsIoT.disconnect();
        }
        closeTelnet();
    }

    private void showProgress() {
        mBinding.progressCircular.setVisibility(View.VISIBLE);
    }

    private void dismissProgress() {
        mBinding.progressCircular.setVisibility(View.GONE);
    }

    private void sendKeepAlive() {
        switch (mAppClass.checkNetwork()) {
            case TCP:
                mAppClass.sendPacket(StabilizerStatusActivityV2.this, mAppClass.framePack("6#S"));
                break;
            case AWSIoT:
                if (awsIoT != null) {
                    awsIoT.publish(mAppClass.framePack("6#S"), publishTopic, StabilizerStatusActivityV2.this);
                }else{
                    mAppClass.showSnackBar("Network Changed, Please Restart !",mBinding.cod);
                    onBackPressed();
                }
                break;
            case NONE:
                mAppClass.showSnackBar(getString(R.string.noConnection), mBinding.cod);
                mAppClass.showConnectionPop(StabilizerStatusActivityV2.this);
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        keepAliveHandler.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        keepAliveNackCount = 0;
        powerPacketNackCount = 0;
        initialize();
    }

    @Override
    public void onBackPressed() {
        closeConnectivity();
        dismissProgress();
        finish();
        stopKeepAlive();
        Intent homeIntent = new Intent(StabilizerStatusActivityV2.this, StabilizerListActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }

    @Override
    public void OnDataReceived(String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAppClass.checkNetwork() == ConnectionMode.AWSIoT) {
                    if (data.equals(AWS_NOT_CONNECTED)) {
                        mAppClass.showSnackBar(getString(R.string.unableToReachServer), mBinding.cod);
                        onBackPressed();
                    } else if (data.equals(AWS_CONNECTED)) {
                        awsIoT.subscribe(subscribeTopic);
                    }
                }
                handleResponse(data);
            }
        });
    }

    private void handleResponse(String data) {
        if (data.contains("ST#")) {
            packetSender.cancel();
            handleData(data);
            startKeepAlive();
        } else if (data.equals("NACK")) {
            mNackCount++;
            if (mNackCount > 3) {
                mAppClass.showSnackBar(getString(R.string.operationFailed), mBinding.cod);
                onBackPressed();
            }
        } else {
            Log.e("V2Error ", "OnDataReceive: " + data);
        }
    }

    private void handleData(String data) {
         /* Status-read- data = "ST#6#01;5;XX;20,X;21,XXX;22,XXX;23,XX;24,X;FF#ED";
         OutPut ON - responce = "ST#7#01;6;01;ACK;D7#ED";
         OutPut OFF - responce =  "ST#8#01;6;02;ACK;AC#ED";
         SleepMode On - responce = "ST#3#RECEIVED,C#ED";
         SleepMode OFF - responce = "ST#3#RECEIVED,D#ED";
         SleepMode Read - responce = "ST#4#C/D#ED"; */

        String[] handleData = data.split("#");
        if (handleData[1].equals("6")) {
            keepAliveNackCount = 0;
            String[] splitData = data.split(";");
            // ST#6#01;5;04;20,1;21,330;22,227;23,14;24,0;FF#ED

            if (splitData[1].equals("5")) {
                switch (splitData[2]) {
                    case "01":
                        mBinding.txtFaultAlert.setText(R.string.low);
                        mBinding.swtPower.setEnabled(false);
                        mBinding.view5.setBackgroundResource(R.drawable.red_circle_bg);
                        mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_off);
                        break;
                    case "02":
                        mBinding.txtFaultAlert.setText(R.string.high);
                        mBinding.swtPower.setEnabled(false);
                        mBinding.view5.setBackgroundResource(R.drawable.red_circle_bg);
                        mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_off);
                        break;
                    case "03":
                        mBinding.txtFaultAlert.setText(R.string.timeDelay);
                        isTimeDelay = true;
                        mBinding.swtPower.setEnabled(false);
                        // mBinding.TgSleepMode.setChecked(true);
                        mBinding.view5.setBackgroundResource(R.drawable.yellow_circle_bg);
                        mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_time_delay);
                        break;
                    case "04":
                        mBinding.txtFaultAlert.setText(R.string.normal);
                        isTimeDelay = false;
                        mBinding.view5.setBackgroundResource(R.drawable.green_circle_bg);
                        mBinding.swtPower.setEnabled(true);
                        break;
                }
                String[] powerStatus = splitData[3].split(",");
                String[] inputVolt = splitData[4].split(",");
                String[] outputVolt = splitData[5].split(",");
                String[] ampVolt = splitData[6].split(",");

                if (powerStatus[1].equals("1")) {
                    mBinding.swtPower.setChecked(true);
                    mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_on);
                    mBinding.TgSleepMode.setEnabled(true);
                    mBinding.txtPower.setText(R.string.outputOn);
                } else if (powerStatus[1].equals("0")) {
                    mBinding.swtPower.setChecked(false);
                    mBinding.txtPower.setText(R.string.outputOff);
                    mBinding.TgSleepMode.setEnabled(true);
                    if (isTimeDelay) {
                        mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_time_delay);
                        mBinding.TgSleepMode.setEnabled(false);
                    } else if (!isTimeDelay) {
                        mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_off);
                    }
                }
                //Amp Decimal
                String[] ampValue = ampVolt[1].split("");
                mBinding.txtAmpere.setText(ampValue[1] + ampValue[2] + "." + ampValue[3] + "A");
                // mBinding.txtAmpere.setText(ampVolt[1] + "A");
                mBinding.txtInputVoltage.setText(inputVolt[1] + "v");
                mBinding.txtOutputVoltage.setText(outputVolt[1] + "v");
            }
            dismissProgress();

            // OutPut ON - responce = "ST#7#01;6;01;ACK;D7#ED";
        } else if (handleData[1].equals("7")) { // On
            powerPacketNackCount = 0;
            mBinding.swtPower.setChecked(true);
            mBinding.txtPower.setText(R.string.outputOn);
            mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_on);

            // OutPut OFF - responce =  "ST#8#01;6;02;ACK;AC#ED";
        } else if (handleData[1].equals("8")) { // OFF
            powerPacketNackCount = 0;
            mBinding.swtPower.setChecked(false);
            mBinding.txtPower.setText(R.string.outputOff);
            mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_time_delay);

            //  ST#4#C/D#ED -- sleepRead
        } else if (handleData[1].contains("4")) {
            String[] spiltData = data.split("#");
            if (spiltData[1].equals("4")) {
                if (spiltData[2].equals("C")) {
                    mBinding.TgSleepMode.setChecked(true);
                    //  mBinding.TgSleepMode.setBackground(getResources().getDrawable(R.drawable.simens_circle_bg,null));
                } else if (spiltData[2].equals("D")) {
                    mBinding.TgSleepMode.setChecked(false);
                    //    mBinding.TgSleepMode.setBackground(getResources().getDrawable(R.drawable.transparent_circle_bg,null));
                }
            }

            // ST#3#RECEIVED,C/D#ED -- sleepWrite
        } else if (handleData[1].equals("3")) {
            String[] sleepMode = handleData[2].split(",");
            if (sleepMode[0].equals("RECEIVED")) {
                if (sleepMode[1].equals("C")) {
                    mBinding.TgSleepMode.setChecked(true);
                    //   mBinding.TgSleepMode.setBackground(getResources().getDrawable(R.drawable.simens_circle_bg,null));
                } else if (sleepMode[1].equals("D")) {
                    mBinding.TgSleepMode.setChecked(false);
                    //  mBinding.TgSleepMode.setBackground(getResources().getDrawable(R.drawable.transparent_circle_bg,null));
                }
            }
        }
    }
}
/*Version: 1.1.0 | Phase II*/
