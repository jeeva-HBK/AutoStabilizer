package com.rax.autostabilizer.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.rax.autostabilizer.ApplicationClass;
import com.rax.autostabilizer.R;
import com.rax.autostabilizer.Utilities.S_Communication;
import com.rax.autostabilizer.databinding.ActivityStabilizerStatusBinding;

import static com.rax.autostabilizer.Utilities.S_Communication.CONNECTED;

public class StabilizerStatusActivity extends AppCompatActivity implements ApplicationClass.TCPDataListener {

    private static final String TAG = "StabilizerStatusActivit";
    private final boolean delay = true;
    private final long delayMillis = 500;
    ApplicationClass mAppClass;
    Context mContext = this;
    ActivityStabilizerStatusBinding mBinding;
    int keepAliveNackCount = 0, powerPacketNackCount = 0;
    CountDownTimer keepAliveHandler;
    String packetToSend;

    //  Handler keepAliveHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_stabilizer_status);
        mAppClass = (ApplicationClass) getApplication();
        mBinding.swtPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgress();
                stopKeepAlive();
                //closeTelnet();
                if (mBinding.swtPower.isChecked()) {
                    sendPacket("*A#");
                } else {
                    sendPacket("*B#");
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
                packetToSend = "*S#";
                mAppClass.sendPacket(StabilizerStatusActivity.this, "*S#");
            }
        }, 500);
    }

    private void showProgress() {
        mBinding.progressCircular.setVisibility(View.VISIBLE);
    }

    private void dismissProgress() {
        mBinding.progressCircular.setVisibility(View.GONE);
    }

    private void sendPacket(String packet) {
        packetToSend = packet;
        mAppClass.sendPacket(StabilizerStatusActivity.this, "");
    }


    private void closeTelnet() {
        S_Communication communication = new S_Communication();
        communication.stop();
    }

    private void restartKeepAlive() {
        keepAliveHandler.start();
    }

    private void stopKeepAlive() {
        keepAliveHandler.cancel();
    }

    private void retryLastSentPacket() {
        Log.d(TAG, "retryLastSentPacket: ");
        mAppClass.sendPacket(StabilizerStatusActivity.this, "");
    }

    private void sendKeepAlive() {
        sendPacket("*S#");
    }


    @Override
    public void OnDataReceive(String data) {
        Log.d(TAG, "OnDataReceive: " + data);
        if (data.contains("\r") || data.contains("\n")) {
            data = data.replace("\r", "");
            data = data.replace("\n", "");
        }
        if (data.contains(CONNECTED)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mAppClass.sendPacket(StabilizerStatusActivity.this, packetToSend);
                }
            }, 500);
        } else if (data.equals("NACK")) {
            if (packetToSend.contains("S")) {
                if (keepAliveNackCount <= 3) {
                    keepAliveNackCount++;
                    restartKeepAlive();
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Please restart and try again", Toast.LENGTH_SHORT).show();
                        }
                    });
                    finish();
                }
            } else if (packetToSend.contains("A") || packetToSend.contains("B")) {
                if (powerPacketNackCount < 3) {
                    powerPacketNackCount++;
                    retryLastSentPacket();
                } else {
                    powerPacketNackCount = 0;
                    dismissProgress();
                    sendKeepAlive();
                    if (packetToSend.contains("A")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Power On Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                        mBinding.swtPower.setChecked(false);
                    } else if (packetToSend.contains("B")) {
                        mBinding.swtPower.setChecked(true);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Power Off Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        } else if (data.contains(";")) {
            dismissProgress();
            keepAliveNackCount = 0;
            String[] splitData = data.split(";");
            if (splitData[1].equals("5")) {
                switch (splitData[2]) {
                    case "01":
                        mBinding.txtFaultAlert.setText("Low");
                        mBinding.swtPower.setEnabled(false);
                        break;
                    case "02":
                        mBinding.txtFaultAlert.setText("High");
                        mBinding.swtPower.setEnabled(false);
                        break;
                    case "03":
                        mBinding.txtFaultAlert.setText("Time delay");
                        mBinding.swtPower.setEnabled(false);
                        mBinding.swtTimeDelay.setChecked(true);
                        break;
                    case "04":
                        mBinding.txtFaultAlert.setText("Normal");
                        mBinding.swtPower.setEnabled(true);
                        break;
                }
                String[] inputVolt = splitData[4].split(",");
                String[] outputVolt = splitData[5].split(",");

                String[] powerStatus = splitData[3].split(",");

                if (powerStatus[1].equals("1")) {
                    mBinding.swtPower.setChecked(true);
                } else if (powerStatus[1].equals("0")) {
                    mBinding.swtPower.setChecked(false);
                }
                mBinding.txtInputVoltage.setText(inputVolt[1] + "v");
                mBinding.txtOutputVoltage.setText(outputVolt[1] + "v");
                restartKeepAlive();
            } else if (splitData[1].equals("6")) {
                powerPacketNackCount = 0;
                if (splitData[2].equals("01")) {
                    if (splitData[3].equals("ACK")) {
                        mBinding.swtPower.setChecked(true);
                    }
                } else if (splitData[2].equals("02")) {
                    if (splitData[3].equals("ACK")) {
                        mBinding.swtPower.setChecked(false);
                    }
                }
                restartKeepAlive();
            }
        }
        /*else {
            restartKeepAlive();
        }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        //keepAliveHandler.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onBackPressed() {
        closeTelnet();
        dismissProgress();
        startActivity(new Intent(StabilizerStatusActivity.this,StabilizerListActivity.class));
        finish();
    }
}