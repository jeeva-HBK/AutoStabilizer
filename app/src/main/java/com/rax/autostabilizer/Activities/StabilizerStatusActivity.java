package com.rax.autostabilizer.Activities;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.rax.autostabilizer.ApplicationClass;
import com.rax.autostabilizer.R;
import com.rax.autostabilizer.databinding.ActivityStabilizerStatusBinding;

public class StabilizerStatusActivity extends AppCompatActivity implements ApplicationClass.TCPDataListener {

    ApplicationClass mAppClass;
    Context mContext = this;
    ActivityStabilizerStatusBinding mBinding;


    CountDownTimer keepAliveHandler = new CountDownTimer(Long.MAX_VALUE, 3000) {
        @Override
        public void onTick(long l) {
            mAppClass.sendPacket(StabilizerStatusActivity.this, "*S#");
        }

        @Override
        public void onFinish() {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_stabilizer_status);
        mAppClass = (ApplicationClass) getApplication();
        keepAliveHandler.start();
        mAppClass.sendPacket(StabilizerStatusActivity.this, "*S#");
        mBinding.swtPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBinding.swtPower.isChecked()) {
                    mAppClass.sendPacket(StabilizerStatusActivity.this, "*A#");
                } else {
                    mAppClass.sendPacket(StabilizerStatusActivity.this, "*B#");
                }
            }
        });

    }

    @Override
    public void OnDataReceive(String data) {
        // *01;5;02;20,1;21,000;22,000;23,000;CRC#
        String[] splitData = data.split(";");
        if (splitData[1].equals("05")) {
            switch (splitData[2]) {
                case "01":
                    mBinding.txtFaultAlert.setText("Low");
                    break;
                case "02":
                    mBinding.txtFaultAlert.setText("High");
                    break;
                case "03":
                    mBinding.txtFaultAlert.setText("Time delay");
                    break;
                case "04":
                    mBinding.txtFaultAlert.setText("Normal");
                    break;
            }
            mBinding.txtInputVoltage.setText(splitData[4] + "v");
            mBinding.txtOutputVoltage.setText(splitData[6] + "v");
            if (splitData[3].equals("05")) {

            }
        } else if (splitData[1].equals("06")) {

        }

    }
}