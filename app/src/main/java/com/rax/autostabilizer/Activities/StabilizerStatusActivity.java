package com.rax.autostabilizer.Activities;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
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

    ApplicationClass mAppClass;
    Context mContext = this;
    ActivityStabilizerStatusBinding mBinding;
    int nackCount = 0;
    private final boolean delay = true;
    private final long delayMillis = 500;
    CountDownTimer keepAliveHandler;
    String mPacketToSend = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_stabilizer_status);
        mAppClass = (ApplicationClass) getApplication();
        mBinding.swtPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBinding.swtPower.isChecked()) {
                    reopenTelnetAndSendPacket("*A#");
                } else {
                    reopenTelnetAndSendPacket("*B#");
                }
            }
        });
        keepAliveHandler = new CountDownTimer(Long.MAX_VALUE, 3000) {
            @Override
            public void onTick(long l) {
                mAppClass.sendPacket(StabilizerStatusActivity.this, "");
            }

            @Override
            public void onFinish() {

            }
        };
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mAppClass.sendPacket(StabilizerStatusActivity.this, "*S#");
            }
        }, 500);
    }

    private void reopenTelnetAndSendPacket(String data) {
        S_Communication communication = new S_Communication();
        communication.stop();
        mPacketToSend = data;

    }

    private void restartKeepAlive() {
        keepAliveHandler.cancel();
        keepAliveHandler.start();
        mPacketToSend = "*S#";
        mAppClass.sendPacket(StabilizerStatusActivity.this, "");
    }

    @Override
    public void OnDataReceive(String data) {
        if (data.contains(CONNECTED)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mAppClass.sendPacket(StabilizerStatusActivity.this, mPacketToSend);
                }
            }, 500);
        }
        if (data.contains("NACK")) {
            if (nackCount <= 3) {
                nackCount++;
                restartKeepAlive();
            } else {
                Toast.makeText(mContext, "Please restart and try again", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            nackCount = 0;
            String[] splitData = data.split(";");
            if (splitData[1].equals("5")) {
                switch (splitData[2]) {
                    case "01":
                        mBinding.txtFaultAlert.setText("Low");
                        break;
                    case "02":
                        mBinding.txtFaultAlert.setText("High");
                        break;
                    case "03":
                        mBinding.txtFaultAlert.setText("Time delay");
                        mBinding.swtTimeDelay.setChecked(true);
                        break;
                    case "04":
                        mBinding.txtFaultAlert.setText("Normal");
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
            }
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

    }
}