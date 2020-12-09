package com.rax.autostabilizer.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentTransaction;

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
    boolean isViewVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_stabilizer_status);
        mAppClass = (ApplicationClass) getApplication();
        Log.d(TAG, "onCreate: 5/12");

        mBinding.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_schedule) {
                    startActivity(new Intent(StabilizerStatusActivity.this, ScheduleActivity.class));
                }
                return false;
            }
        });

        //        mBinding.swtPower.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showProgress();
//                //stopKeepAlive();
//                restartKeepAlive();
//                //closeTelnet();
//                if (mBinding.swtPower.isChecked()) {
//                    sendPacket("*A#");
//                } else {
//                    sendPacket("*B#");
//                }
//            }
//
//        });
//
//        keepAliveHandler = new CountDownTimer(5000, 5000) {
//            @Override
//            public void onTick(long l) {
//                //mBinding.swtPower.setClickable(true);
//            }
//
//            @Override
//            public void onFinish() {
//                Log.d(TAG, "RAXLOG: CountDownTimer ENDED");
//                //mBinding.swtPower.setClickable(false);
//                sendKeepAlive();
//            }
//        };
//        showProgress();
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                packetToSend = "*S#";
//                mAppClass.sendPacket(StabilizerStatusActivity.this, "*S#");
//            }
//        }, 500);
    }
    private void showProgress() {
        mBinding.progressCircular.setVisibility(View.VISIBLE);
    }

    private void dismissProgress() {
        mBinding.progressCircular.setVisibility(View.GONE);
    }

    private void sendPacket(String packet) {
        packetToSend = packet;
        if (!isViewVisible) {
            return;
        }
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
        if (!isViewVisible) {
            return;
        }
        mAppClass.sendPacket(StabilizerStatusActivity.this, "");
    }

    private void sendKeepAlive() {
        sendPacket("*S#");
    }


    @Override
    public void OnDataReceive(String data) {
        Log.d(TAG, "RAXLOGtelnetData: " + data);
        if (data.contains("\r") || data.contains("\n")) {
            data = data.replace("\r", "");
            data = data.replace("\n", "");
        }
        if (data.equals("timeOut") || data.equals("FailedToConnect")) {
            dismissProgress();
            Log.d(TAG, "OnDataReceive: log1");
            Toast.makeText(mAppClass, "Please restart and try again", Toast.LENGTH_SHORT).show();
            onBackPressed();
        }
        if (data.contains(CONNECTED)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isViewVisible) {
                        return;
                    }
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
                            Log.d(TAG, "run: " + keepAliveNackCount);
                            Toast.makeText(mContext, "Please restart and try again", Toast.LENGTH_SHORT).show();
                            onBackPressed();
                        }
                    });
                }
            } else if (packetToSend.contains("A") || packetToSend.contains("B")) {
                if (powerPacketNackCount < 3) {
                    powerPacketNackCount++;
                    retryLastSentPacket();
                } else {
                    powerPacketNackCount = 0;
                    Log.d(TAG, "OnDataReceive: log2");
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
            // dismissProgress();
            Log.d(TAG, "OnDataReceive: log3");
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
                        mBinding.swtPower.setEnabled(false);
                        mBinding.swtTimeDelay.setChecked(true);
                        mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_off);
                        break;
                    case "04":
                        mBinding.txtFaultAlert.setText("Normal");
                        mBinding.swtPower.setEnabled(true);
                        break;
                }
                String[] powerStatus = splitData[3].split(",");
                String[] inputVolt = splitData[4].split(",");
                String[] outputVolt = splitData[5].split(",");

                if (powerStatus[1].equals("1")) {
                    mBinding.swtPower.setChecked(true);
                    mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_on);
                    mBinding.text4.setText("Output ON");
                } else if (powerStatus[1].equals("0")) {
                    mBinding.swtPower.setChecked(false);
                    mBinding.text4.setText("Output OFF");
                    mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_off);
                }
                mBinding.txtInputVoltage.setText(inputVolt[1] + "v");
                mBinding.txtOutputVoltage.setText(outputVolt[1] + "v");
                restartKeepAlive();
                dismissProgress();
            } else if (splitData[1].equals("6")) {
                powerPacketNackCount = 0;
                if (splitData[2].equals("01")) {
                    if (splitData[3].equals("ACK")) {
                        mBinding.swtPower.setChecked(true);
                        mBinding.text4.setText("Output ON");
                        //mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_on);
                    }
                } else if (splitData[2].equals("02")) {
                    if (splitData[3].equals("ACK")) {
                        mBinding.swtPower.setChecked(false);
                        mBinding.text4.setText("Output OFF");
                        //mBinding.swtPower.setBackgroundResource(R.drawable.ic_power_off);
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
        isViewVisible = false;
        //keepAliveHandler.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        keepAliveNackCount = 0;
        powerPacketNackCount = 0;
        isViewVisible = true;
        sendPacket("*S#");
    }

    @Override
    public void onBackPressed() {
       /* closeTelnet();
        dismissProgress();
        startActivity(new Intent(StabilizerStatusActivity.this, StabilizerListActivity.class));
        finish();
        Intent homeIntent = new Intent(StabilizerStatusActivity.this, StabilizerListActivity.class);
        //  homeIntent.addCategory( Intent.CATEGORY_HOME );
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);*/

    }
}

/*Version: N/A | Phase 2*/
