package com.rax.autostabilizer.Activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rax.autostabilizer.ApplicationClass;
import com.rax.autostabilizer.ConnectionMode;
import com.rax.autostabilizer.DataReceiveCallback;
import com.rax.autostabilizer.R;
import com.rax.autostabilizer.databinding.ActivityStabilizerStatusBinding;

import static com.rax.autostabilizer.ApplicationClass.macAddress;
import static com.rax.autostabilizer.Utilities.S_Communication.CONNECTED;

public class StabilizerStatusActivity extends AppCompatActivity implements ApplicationClass.DataListener, DataReceiveCallback {

    private static final String TAG = "Gva";
    private final boolean delay = true;
    private final long delayMillis = 500;
    ApplicationClass mAppClass;
    Context mContext = this;
    ActivityStabilizerStatusBinding mBinding;
    int keepAliveNackCount = 0, powerPacketNackCount = 0;
    CountDownTimer keepAliveHandler;
    String packetToSend, awsPacketToSend;
    boolean isViewVisible = false, isTimeDelay = false;
    ConnectionMode mConnectionMode;

    //AWS
    private String publishTopic = "topic/" + macAddress + "/app2irc",
            subscribeTopic = "topic/" + macAddress + "/irc2app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_stabilizer_status);
        mAppClass = (ApplicationClass) getApplication();

        closeConnectivity();
        //mAppClass.initializeClient(mContext);

        mBinding.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_schedule) {
                    startActivity(new Intent(StabilizerStatusActivity.this, ScheduleActivity.class));
                }
                return false;
            }
        });

        mBinding.cbSleepMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBinding.cbSleepMode.isChecked()) {
                    currentNetwork(1, "3#C");
                } else {
                    currentNetwork(1, "3#D");
                }
            }
        });

        mBinding.swtPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgress();
                //stopKeepAlive();        ST#7#01;6;01;ACK;D7#ED
                // restartKeepAlive();
                //closeTelnet();
                if (mBinding.swtPower.isChecked()) {
                    currentNetwork(1, "7#A");
                } else {
                    currentNetwork(1, "8#B");
                }
            }
        });

        keepAliveHandler = new CountDownTimer(5000, 5000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                sendKeepAlive();
            }
        };

        showProgress();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                awsPacketToSend = mAppClass.framePack("4");
                packetToSend = mAppClass.framePack("4");
                // mAppClass.sendPacket(StabilizerStatusActivity.this, "SST#4#ED");
                currentNetwork(2, "4");
            }
        }, 500);
        packetToSend = mAppClass.framePack("6#S");
        mAppClass.sendPacket(StabilizerStatusActivity.this, packetToSend);

    }

    private void closeConnectivity() {
        closeTelnet();
        mAppClass.AWSDisConnect();
    }

    private void currentNetwork(int mode, String packet) {
        if (mAppClass.checkNetwork().equals("mobileData")) {
            methodMQTT(mode, packet);

        } else if (mAppClass.checkNetwork().equals("wifi")) {
            methodTCP(mode, packet);

        } else if (mAppClass.checkNetwork().equals("nothing")) {
            new MaterialAlertDialogBuilder(mContext)
                    .setTitle("Network Error")
                    .setMessage("Both MobileData_and_Wifi_turned_Off, Please_Turn_On_OneOfThem..! ")
                    .setPositiveButton("mobile Data", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(mContext, "mobile Data", Toast.LENGTH_SHORT).show();
                            startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
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
                    finish();
                    Intent homeIntent = new Intent(StabilizerStatusActivity.this, StabilizerListActivity.class);
                    //  homeIntent.addCategory( Intent.CATEGORY_HOME );
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(homeIntent);
                }
            }).show();
        }
    }

    private void methodMQTT(int mode, String packet) {
        if (mode == 2) {
            mAppClass.initializeClient(mContext);
            sendMqtt(packet);
        } else {
            sendMqtt(packet);
        }
    }

    private void sendMqtt(String packet) {
        awsPacketToSend = packet;
        publishPacket(mAppClass.framePack(packet));
    }

    private void publishPacket(String packet) {
        awsPacketToSend = packet;
        mAppClass.subscribe(subscribeTopic);
        Log.d(TAG, "TOPIC: " + subscribeTopic);
        mAppClass.publish(packet, publishTopic, this::OnAWSDataReceive);
    }

    private void methodTCP(int mode, String packet) {
        packetToSend = packet;
        Toast.makeText(mContext, "MethodTCP !", Toast.LENGTH_SHORT).show();
        if (mode == 2) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isViewVisible) {
                        return;
                    }
                    mAppClass.sendPacket(StabilizerStatusActivity.this, mAppClass.framePack(packet));
                }
            }, 500);
        } else {
            sendPacket(mAppClass.framePack(packet));
        }
    }

    private void sendPacket(String packet) {
        packetToSend = packet;
        if (!isViewVisible) {
            return;
        }
        mAppClass.sendPacket(StabilizerStatusActivity.this, "");
    }

    private void showProgress() {
        mBinding.progressCircular.setVisibility(View.VISIBLE);
    }

    private void dismissProgress() {
        mBinding.progressCircular.setVisibility(View.GONE);
    }

    private void closeTelnet() {
      /*  S_Communication communication = new S_Communication();
        communication.stop();*/
    }

    private void restartKeepAlive() {
        keepAliveHandler.start();
    }

    private void stopKeepAlive() {
        keepAliveHandler.cancel();
    }

    private void retryLastSentPacket(int mode) {
        if (!isViewVisible) {
            return;
        }
        if (mode == 0) {
            mAppClass.sendPacket(StabilizerStatusActivity.this, "");
        } else if (mode == 1) {
            publishPacket(awsPacketToSend);
        }
    }

    private void sendKeepAlive() {
        packetToSend = "6#S";
        awsPacketToSend = "6#S";
        // sendPacket("*S#");
        currentNetwork(1, "6#S");
    }

    @Override
    protected void onPause() {
        super.onPause();
        isViewVisible = false;
        //keepAliveHandler.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        keepAliveNackCount = 0;
        powerPacketNackCount = 0;
        isViewVisible = true;
        // sendPacket("*S#");
    }

    @Override
    public void onBackPressed() {
        closeConnectivity();

        dismissProgress();
        finish();
        stopKeepAlive();
        Intent homeIntent = new Intent(StabilizerStatusActivity.this, StabilizerListActivity.class);
        //  homeIntent.addCategory( Intent.CATEGORY_HOME );
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }
   /* @Override
    public void OnAWSDataReceive(String data) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
                if (data.equals("Connected")) {
                    //  Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
                } else if (data.equals("sendFailed")) {
                    //  Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
                    retryLastSentPacket(1);
                } else if (data.equals("Connecting")) {
                    Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
                } else {
                    handleData(data, 1);
                }
                 if (data.contains("ST#")) {
                    // ST#4,D#ED
                    String[] spiltData = data.split("#");

                    String[] sleepMode = spiltData[1].split(",");
                    if (sleepMode[0].equals("4")) {
                        if (sleepMode[1].equals("D")) {
                            mBinding.cbSleepMode.setChecked(false);
                        } else if (sleepMode[1].equals("C")) {
                            mBinding.cbSleepMode.setChecked(true);
                        }
                    }
                    // ST# 6# 01;5;XX;20,X;21,XXX;22,XXX;23,XX;24,X;FF #ED
                    if (spiltData[1].equals("6")) {
                        String[] rawData = spiltData[2].split(";");

                    }
                }



            }
        });

    }*/

    @Override
    public void OnDataReceived(String data) {
        if (data.contains("\r") || data.contains("\n")) {
            data = data.replace("\r", "");
            data = data.replace("\n", "");
        } else if (data.equals("timeOut") || data.equals("FailedToConnect")) {
            Toast.makeText(mAppClass, "Please restart and try again", Toast.LENGTH_SHORT).show();
            onBackPressed();
        } else if (data.contains(CONNECTED)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isViewVisible) {
                        return;
                    }
                    mAppClass.sendPacket(StabilizerStatusActivity.this, packetToSend);
                }
            }, 500);
        } else if (data.contains("ST#")) {
            handleData(data, 0);
        } else if (data.equals("")) {
            retryLastSentPacket(0);
        }
    }

    @Override
    public void OnAWSDataReceive(String data) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
                if (data.equals("Connected")) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!isViewVisible) {
                                return;
                            }
                            publishPacket(awsPacketToSend);
                        }
                    }, 500);
                } else if (data.equals("sendFailed")) {
                    //  Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
                    if (keepAliveNackCount <= 3) {
                        keepAliveNackCount++;
                        restartKeepAlive();
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Please restart and try again", Toast.LENGTH_SHORT).show();
                                onBackPressed();
                            }
                        });
                    }
                } else if (data.equals("Connecting")) {
                    Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
                } else if (data.contains("ST#")) {
                    // handleAWSData(data, 1);
                    handleData(data, 1);
                } else if (data.contains("pckError")) {
                    retryLastSentPacket(1);
                } else if (data.contains("sendCatch")) {
                    retryLastSentPacket(1);
                }
            }
        });
    }

    private void handleData(String data, int mode) {
        // Status-read- data = "ST#6#01;5;XX;20,X;21,XXX;22,XXX;23,XX;24,X;FF#ED";
        // OutPut ON - responce = "ST#7#01;6;01;ACK;D7#ED";
        // OutPut OFF - responce =  "ST#8#01;6;02;ACK;AC#ED";
        // SleepMode On - responce = "ST#3#RECEIVED,C#ED";
        // SleepMode OFF - responce = "ST#3#RECEIVED,D#ED";
        // SleepMode Reade- responce = "ST#4#C/D#ED";

        String[] handleData = data.split("#");
        if (data.equals("NACK")) {
            if (packetToSend.contains("S")) {
                if (keepAliveNackCount <= 3) {
                    keepAliveNackCount++;
                    restartKeepAlive();
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Please restart and try again", Toast.LENGTH_SHORT).show();
                            onBackPressed();
                        }
                    });
                }
            } else if (packetToSend.contains("C") || packetToSend.contains("D")) {
                if (powerPacketNackCount < 3) {
                    powerPacketNackCount++;
                    retryLastSentPacket(mode);
                } else {
                    powerPacketNackCount = 0;
                    dismissProgress();
                    sendKeepAlive();
                    if (packetToSend.contains("C")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Power On Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                        mBinding.swtPower.setChecked(false);
                    } else if (packetToSend.contains("D")) {
                        mBinding.swtPower.setChecked(true);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Power Off Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } /*ST#6#01;5;XX;20,X;21,XXX;22,XXX;23,XX;24,X;FF#ED*/
        } else if (handleData[1].equals("6")) {
            keepAliveNackCount = 0;
            String[] splitData = data.split(";");
            if (splitData[1].equals("5")) {
                switch (splitData[2]) {
                    case "01":
                        mBinding.txtFaultAlert.setText("Low");
                        mBinding.swtPower.setEnabled(false);
                        mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_off);
                        break;
                    case "02":
                        mBinding.txtFaultAlert.setText("High");
                        mBinding.swtPower.setEnabled(false);
                        mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_off);
                        break;
                    case "03":
                        mBinding.txtFaultAlert.setText("Time delay");
                        isTimeDelay = true;
                        mBinding.swtPower.setEnabled(false);
                        mBinding.swtTimeDelay.setChecked(true);
                        mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_time_delay);
                        break;
                    case "04":
                        mBinding.txtFaultAlert.setText("Normal");
                        isTimeDelay = false;
                        mBinding.swtPower.setEnabled(true);
                        break;

                }
                String[] powerStatus = splitData[3].split(",");
                String[] inputVolt = splitData[4].split(",");
                String[] outputVolt = splitData[5].split(",");
                // ST#6#01;5;03;20,0;21,266;22,185;23,08;24,0;FF#ED
                if (powerStatus[1].equals("1")) {
                    mBinding.swtPower.setChecked(true);
                    mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_on);
                    mBinding.cbSleepMode.setEnabled(true);
                    mBinding.text4.setText("Output ON");
                } else if (powerStatus[1].equals("0")) {
                    mBinding.swtPower.setChecked(false);
                    mBinding.text4.setText("Output OFF");
                    mBinding.cbSleepMode.setEnabled(true);
                    if (isTimeDelay) {
                        mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_time_delay);
                        mBinding.cbSleepMode.setEnabled(false);
                    } else if (!isTimeDelay) {
                        mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_off);
                    }
                }
                mBinding.txtInputVoltage.setText(inputVolt[1] + "v");
                mBinding.txtOutputVoltage.setText(outputVolt[1] + "v");
                restartKeepAlive();
            }
            dismissProgress(); // Phase II
            // OutPut ON - responce = "ST#7#01;6;01;ACK;D7#ED";
        } else if (handleData[1].equals("7")) { // On
            powerPacketNackCount = 0;
            mBinding.swtPower.setChecked(true);
            mBinding.text4.setText("Output ON");
            mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_on);
            restartKeepAlive();
            // OutPut OFF - responce =  "ST#8#01;6;02;ACK;AC#ED";

        } else if (handleData[1].equals("8")) { // OFF
            powerPacketNackCount = 0;
            mBinding.swtPower.setChecked(false);
            mBinding.text4.setText("Output OFF");
            mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_time_delay);
            restartKeepAlive();
            //  ST#4#C/D#ED -- sleepRead

        } else if (handleData[1].contains("4")) {
            // ST#4#D#ED
            String[] spiltData = data.split("#");
            if (spiltData[1].equals("4")) {
                if (spiltData[2].equals("C")) {
                    mBinding.cbSleepMode.setChecked(true);
                } else if (spiltData[2].equals("D")) {
                    mBinding.cbSleepMode.setChecked(false);
                }
            }
            awsPacketToSend = mAppClass.framePack("6#S");
            packetToSend = mAppClass.framePack("6#S");
            mAppClass.sendPacket(StabilizerStatusActivity.this, packetToSend);
            restartKeepAlive();


            // ST#3#RECEIVED,C/D#ED -- sleepWrite
        } else if (handleData[1].equals("3")) {
            String[] sleepMode = handleData[2].split(",");
            if (sleepMode[0].equals("RECEIVED")) {
                if (sleepMode[1].equals("C")) {
                    mBinding.cbSleepMode.setChecked(true);
                } else if (sleepMode[1].equals("D")) {
                    mBinding.cbSleepMode.setChecked(false);
                }
            }
            restartKeepAlive();
        }
    }
}
/*Version: N/A | Phase II*/

