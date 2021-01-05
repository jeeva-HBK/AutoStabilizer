package com.rax.autostabilizer.Activities;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.rax.autostabilizer.ApplicationClass;
import com.rax.autostabilizer.R;
import com.rax.autostabilizer.Utilities.AWSIoT;
import com.rax.autostabilizer.databinding.ActivityScheduleBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.rax.autostabilizer.ApplicationClass.macAddress;

// Created on 16 Dec by Jeeva
public class ScheduleActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, ApplicationClass.DataListener {

    ActivityScheduleBinding mBinding;
    private static final String TAG = "ScheduleActivity ";

    TimePickerDialog timePickerDialog;
    SimpleDateFormat newFromat = new SimpleDateFormat("HH:mm");
    SimpleDateFormat twelve = new SimpleDateFormat("hh:mm a");
    ApplicationClass mAppClass;
    Context mContext = this;
    boolean isViewVisible = false;
    String packetToSend;
    private int mNackCount = 0;

    AWSIoT awsIoT;

    private String publishTopic = "topic/" + macAddress + "/app2irc",
            subscribeTopic = "topic/" + macAddress + "/irc2app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_schedule);
        mAppClass = (ApplicationClass) getApplication();

        getActionBar();
        init();
    }

    private void init() {
        showProgress();
        mBinding.schedule1Cb.setOnCheckedChangeListener(this);
        mBinding.schedule2Cb.setOnCheckedChangeListener(this);
        mBinding.schedule3Cb.setOnCheckedChangeListener(this);
        mBinding.schedule4Cb.setOnCheckedChangeListener(this);

        mBinding.pirCb.setOnCheckedChangeListener(this);

        mBinding.sch1st.setOnClickListener(this);
        mBinding.sch1et.setOnClickListener(this);
        mBinding.sch2st.setOnClickListener(this);
        mBinding.sch2et.setOnClickListener(this);
        mBinding.sch3st.setOnClickListener(this);
        mBinding.sch3et.setOnClickListener(this);
        mBinding.sch4st.setOnClickListener(this);
        mBinding.sch4et.setOnClickListener(this);

        mBinding.schSave.setOnClickListener(this);

       /* awsIoT = AWSIoT.getInstance(mContext, this);
        awsIoT.subscribe(subscribeTopic);
        sendData("2");*/
    }

    private void showProgress() {
        mBinding.progressCircular.setVisibility(View.VISIBLE);
    }

    private void dismissProgress() {
        mBinding.progressCircular.setVisibility(View.VISIBLE);
    }

    private void sendData(String packet) {
        packetToSend = mAppClass.framePack(packet);
        switch (mAppClass.checkNetwork()) {
            case TCP:
                mAppClass.sendPacket(ScheduleActivity.this, packetToSend);
                break;
            case AWSIoT:
                awsIoT.publish(packetToSend, publishTopic, ScheduleActivity.this);
                break;
            case NONE:
                Toast.makeText(mContext, "No Connection", Toast.LENGTH_SHORT).show();
                mAppClass.showConnectionPop(ScheduleActivity.this);
                break;
        }
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
        readScheduleData();
    }

    private void readScheduleData() {
        sendData("2");
    }

    @Override
    protected void onResume() {
        super.onResume();
        initialize();
    }

    @Override
    public void onClick(View view) {
        String starTime = " Start ", endTime = " End ";
        switch (view.getId()) {
            case R.id.sch1st:
                pickTimeDialog(starTime, mBinding.sch1st);
                break;
            case R.id.sch1et:
                pickTimeDialog(endTime, mBinding.sch1et);
                break;

            case R.id.sch2st:
                pickTimeDialog(starTime, mBinding.sch2st);
                break;
            case R.id.sch2et:
                pickTimeDialog(endTime, mBinding.sch2et);
                break;

            case R.id.sch3st:
                pickTimeDialog(starTime, mBinding.sch3st);
                break;
            case R.id.sch3et:
                pickTimeDialog(endTime, mBinding.sch3et);
                break;

            case R.id.sch4st:
                pickTimeDialog(starTime, mBinding.sch4st);
                break;
            case R.id.sch4et:
                pickTimeDialog(endTime, mBinding.sch4et);
                break;

            case R.id.schSave:
                validateAll();
                if (!checkBoxCheck()) {
                    Snackbar.make(mBinding.cod, "Select Any One Of The Schedule !", Snackbar.LENGTH_SHORT).show();
                    return;
                } else {
                    sendData(formSendPacket());
                }
                Log.d(TAG, "onClick: FormSendData " + formSendPacket());
                break;
        }
    }

    private String formSendPacket() {
        String packet = null;
        if (mBinding.pirCb.isChecked()) {
            packet = "1";
        } else {
            packet = "0";
        }
        if (!mBinding.edtPirTimeDelay.getText().toString().equals("")) {
            packet = packet + "#" + mBinding.edtPirTimeDelay.getText().toString(); //timeDelay
        } else {
            packet = packet + "#" + "00";
        }
        packet = packet + "#" + checkBoxCount(); //scheduleCount
        packet = "1#" + packet + getSentTime(); //scheduleTime
        return packet;
    }

    private String getSentTime() {
        String time = null;
        if (mBinding.schedule1Cb.isChecked()) {
            time = "#1" + mBinding.sch1st.getText().toString().replace(":", "") +
                    mBinding.sch1et.getText().toString().replace(":", "");
        } else {
            time = "#000000000";
        }

        if (mBinding.schedule2Cb.isChecked()) {
            time = time + "#1" + mBinding.sch2st.getText().toString().replace(":", "") +
                    mBinding.sch2et.getText().toString().replace(":", "");
        } else {
            time = time + "#000000000";
        }

        if (mBinding.schedule3Cb.isChecked()) {
            time = time + "#1" + mBinding.sch3st.getText().toString().replace(":", "") +
                    mBinding.sch3et.getText().toString().replace(":", "");
        } else {
            time = time + "#000000000";
        }

        if (mBinding.schedule4Cb.isChecked()) {
            time = time + "#1" + mBinding.sch4st.getText().toString().replace(":", "") +
                    mBinding.sch4et.getText().toString().replace(":", "");
        } else {
            time = time + "#000000000";
        }
        return time;
    }

    private int checkBoxCount() {
        int count = 0;
        if (mBinding.schedule1Cb.isChecked()) {
            count = 1;
        }
        if (mBinding.schedule2Cb.isChecked()) {
            count = count + 1;
        }
        if (mBinding.schedule3Cb.isChecked()) {
            count = count + 1;
        }
        if (mBinding.schedule4Cb.isChecked()) {
            count = count + 1;
        }
        return count;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

    /*
        switch (compoundButton.getId()) {
            case R.id.schedule1_cb:
                validate(mBinding.sch1st, mBinding.sch1et, mBinding.schedule1Cb);
                break;

            case R.id.schedule2_cb:
                validate(mBinding.sch2st, mBinding.sch2et, mBinding.schedule2Cb);
                break;

            case R.id.schedule3_cb:
                validate(mBinding.sch3st, mBinding.sch3et, mBinding.schedule3Cb);
                break;

            case R.id.schedule4_cb:
                validate(mBinding.sch4st, mBinding.sch4et, mBinding.schedule4Cb);
                break;

        }
*/
    /* if (mBinding.schedule1Cb.isChecked()) {
            validate(mBinding.sch1st, mBinding.sch1et, mBinding.schedule1Cb);
            return;
        }
        if (mBinding.schedule2Cb.isChecked()) {
            validate(mBinding.sch2st, mBinding.sch2et, mBinding.schedule2Cb);
            return;
        }
        if (mBinding.schedule3Cb.isChecked()) {
            validate(mBinding.sch3st, mBinding.sch3et, mBinding.schedule3Cb);
            return;
        }
        if (mBinding.schedule4Cb.isChecked()) {
            validate(mBinding.sch4st, mBinding.sch4et, mBinding.schedule4Cb);
            return;
        }*/
    }

    private boolean checkBoxCheck() {
        if (mBinding.schedule1Cb.isChecked()) {
            return true;
        }
        if (mBinding.schedule2Cb.isChecked()) {
            return true;
        }
        if (mBinding.schedule3Cb.isChecked()) {
            return true;
        }
        if (mBinding.schedule4Cb.isChecked()) {
            return true;
        }
        if (mBinding.pirCb.isChecked()) {
            return true;
        }
        return false;
    }

    private void validate(TextView start, TextView end, MaterialCheckBox scheduleCb) {
        Date timeStart = formatDate((String) start.getText()),
                timeEnd = formatDate((String) end.getText());
        /* if (start.getTag().equals("AM")) {
            timeStart =;
        } else if (start.getTag().equals("PM")) {
            timeStart = formatDate((String) start.getText());
        }

        if (end.getTag().equals("AM")) {
            timeEnd =;
        } else if (end.getTag().equals("PM")) {
            timeEnd = formatDate((String) end.getText());
        }*/
        String starTime = start.getText().toString(), endTime = end.getText().toString();
        Log.d(TAG, "validate: timeStart " + timeStart + "| timeEnd | " + timeEnd);

        if (starTime.equals("Start Time") || endTime.equals("End Time")) {
            showSnackBar("Choose Schedule Time !");
            scheduleCb.setChecked(false);
            return;
        }

        if (starTime.equals(endTime)) {
            showSnackBar("StartTime and EndTime should not be Same !");
            end.setText("End Time");
            return;
        }
        //        if (timeStart.getHours() == 00) {
//            int newTime = 0;
//            if (start.getTag().equals("AM")) {
//                newTime = 12;
//                Log.d(TAG, "Jeeva: if" + timeStart.getHours());
//            } else if (start.getTag().equals("PM")) {
//                newTime = 24;
//                Log.d(TAG, "Jeeva: else" + timeStart.getHours());
//            }
//        } else {
        if (timeEnd.before(timeStart)) {
            showSnackBar("Time validation failure !");
            scheduleCb.setChecked(false);
        }
//           }


    }

    private void validateAll() {

        Date start = formatDate((String) mBinding.sch1st.getText());
        Date end = formatDate((String) mBinding.sch1et.getText());

        Date start2 = formatDate((String) mBinding.sch2st.getText());
        Date end2 = formatDate((String) mBinding.sch2et.getText());

        Date start3 = formatDate((String) mBinding.sch3st.getText());
        Date end3 = formatDate((String) mBinding.sch3et.getText());

        Date start4 = formatDate((String) mBinding.sch4st.getText());
        Date end4 = formatDate((String) mBinding.sch4et.getText());

        if (mBinding.schedule1Cb.isChecked()) {
            if (end.before(start)) {
                showSnackBar("Time validation failure !");
                mBinding.schedule1Cb.setChecked(false);
            }
            if (start.equals(end)) {
                showSnackBar("Start Time & End Time Should not be Same ! ");
                mBinding.schedule1Cb.setChecked(false);
            }
        }

        if (mBinding.schedule2Cb.isChecked()) {
            if (start2.before(end)) {
                showSnackBar("schedule 2 start validation failure !");
                mBinding.schedule2Cb.setChecked(false);
            }

            if (end2.before(start2)) {
                showSnackBar("schedule 2 end validation failure !");
                mBinding.schedule2Cb.setChecked(false);
            }
            if (start2.equals(end2)) {
                showSnackBar("Start Time & End Time Should not be Same ! ");
                mBinding.schedule2Cb.setChecked(false);
            }
        }

        if (mBinding.schedule3Cb.isChecked()) {
            if (start3.before(end2)) {
                showSnackBar("schedule 3 start validation failure !");
                mBinding.schedule3Cb.setChecked(false);
            }

            if (end3.before(start3)) {
                showSnackBar("schedule 3 end validation failure !");
                mBinding.schedule3Cb.setChecked(false);
            }
            if (start3.equals(end3)) {
                showSnackBar("Start Time & End Time Should not be Same ! ");
                mBinding.schedule3Cb.setChecked(false);
            }
        }

        if (mBinding.schedule4Cb.isChecked()) {
            if (start4.before(end3)) {
                showSnackBar("schedule 4 start validation failure !");
                mBinding.schedule4Cb.setChecked(false);
            }

            if (end4.before(start4)) {
                showSnackBar("schedule 4 end validation failure !");
                mBinding.schedule4Cb.setChecked(false);
            }
            if (start4.equals(end4)) {
                showSnackBar("Start Time & End Time Should not be Same ! ");
                mBinding.schedule4Cb.setChecked(false);
            }
        }
    }

    private void showSnackBar(String message) {
        Snackbar.make(mBinding.cod, message, Snackbar.LENGTH_SHORT).show();
    }

    private String pickTimeDialog(String title, TextView txtView) {

        final String[] finalTime = new String[1];
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        timePickerDialog = new TimePickerDialog(ScheduleActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                try {
                    String sHour = "00";
                    String sMinute = "00";

                    if (selectedHour < 10)
                        sHour = "0" + selectedHour;
                    else
                        sHour = String.valueOf(selectedHour);

                    if (selectedMinute < 10)
                        sMinute = "0" + selectedMinute;
                    else
                        sMinute = String.valueOf(selectedMinute);

                    String AM_PM;

                    if (selectedHour < 12) {
                        AM_PM = "AM";
                    } else {
                        AM_PM = "PM";
                    }
                    Date newTime = formatDate(sHour + ":" + sMinute);
                    showSnackBar(twelve.format(newTime));
                    txtView.setTag(AM_PM);
                    txtView.setText(sHour + ":" + sMinute);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }, hour, minute, true);
        timePickerDialog.setTitle(title + "Time");
        timePickerDialog.setCanceledOnTouchOutside(false);
        timePickerDialog.show();
        return finalTime[0];

    }

    private Date formatDate(String Time) {
        Date time = null;
        try {
            time = newFromat.parse(Time);
        } catch (ParseException e) {
            //  e.printStackTrace();
        }
        return time;
    }

    private boolean nothingChanged() {
        if (mBinding.pirCb.isChecked()) {
            return false;
        }
        if (mBinding.edtPirTimeDelay.getText().toString().trim().length() != 0) {
            return false;
        }
       /* if (mBinding.schedule1Cb.isChecked()){
            return false;
        }
        if (mBinding.schedule2Cb.isChecked()){
            return false;
        }
        if (mBinding.schedule3Cb.isChecked()){
            return false;
        }
        if (mBinding.schedule4Cb.isChecked()){
            return false;
        }*/
        if (checkBoxCheck()) {
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeConnectivity();
        dismissProgress();
        finish();
      /*  if (nothingChanged()) {
            Intent StatusIntent = new Intent(ScheduleActivity.this, StabilizerStatusActivity.class);
            StatusIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(StatusIntent);
        } else {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Exit")
                    .setMessage("Changes will not be Saved, are you Sure You Want to Exit this Screen")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            closeConnectivity();
                            Intent StatusIntent = new Intent(ScheduleActivity.this, StabilizerStatusActivity.class);
                            StatusIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(StatusIntent);
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).show();
        }*/
    }

    private void closeConnectivity() {
        mAppClass.AWSDisConnect();
    }

    private void handleData(String data) {
        if (data.contains("ST#")) {

            String[] spiltData = data.split("#");
            // ST# 2# 1# 65# 4# 1 01100200# 1 02200245# 1 03120506# 1 06291015# ED-- read
            // ST# 2# 1# 00 4# 101100200# 102200245# 103120506# 106291015# EED
            if (spiltData[1].equals("2")) {
                //pir checkBox
                if (spiltData[2].equals("0")) {
                    mBinding.pirCb.setChecked(false);
                } else if (spiltData[2].equals("1")) {
                    mBinding.pirCb.setChecked(true);
                }
                //pir timeDelay
                if (!spiltData[3].equals("00")) {
                    mBinding.edtPirTimeDelay.setHint(spiltData[3]);
                }
                //Schedule Time
                String schedule1Data = spiltData[5], schedule2Data = spiltData[6],
                        schedule3Data = spiltData[7], schedule4Data = spiltData[8],

                        sch1St, sch1Et, sch2St, sch2Et, sch3St, sch3Et, sch4St, sch4Et;

                String[] sch1Time, sch2Time, sch3Time, sch4Time;

                sch1Time = schedule1Data.split("");
                sch1St = sch1Time[1] + sch1Time[2] + ":" + sch1Time[3] + sch1Time[4];
                sch1Et = sch1Time[5] + sch1Time[6] + ":" + sch1Time[7] + sch1Time[8];

                mBinding.sch1st.setText(sch1St);
                mBinding.sch1et.setText(sch1Et);

                if (sch1Time[0].equals("1")) {
                    mBinding.schedule1Cb.setChecked(true);
                } else if (sch1Time[0].equals("0")) {
                    mBinding.schedule1Cb.setChecked(false);
                }
                // ST#2#1#65#4#101100200#102200245#103120506#106291015#ED-- read

                sch2Time = schedule2Data.split("");
                sch2St = sch2Time[1] + sch2Time[2] + ":" + sch2Time[3] + sch2Time[4];
                sch2Et = sch2Time[5] + sch2Time[6] + ":" + sch2Time[7] + sch2Time[8];
                mBinding.sch2st.setText(sch2St);
                mBinding.sch2et.setText(sch2Et);

                if (sch2Time[0].equals("1")) {
                    mBinding.schedule2Cb.setChecked(true);
                } else if (sch2Time[0].equals("0")) {
                    mBinding.schedule2Cb.setChecked(false);
                }

                sch3Time = schedule3Data.split("");
                sch3St = sch3Time[1] + sch3Time[2] + ":" + sch3Time[3] + sch3Time[4];
                sch3Et = sch3Time[5] + sch3Time[6] + ":" + sch3Time[7] + sch3Time[8];
                mBinding.sch3st.setText(sch3St);
                mBinding.sch3et.setText(sch3Et);

                if (sch3Time[0].equals("1")) {
                    mBinding.schedule3Cb.setChecked(true);
                } else if (sch3Time[0].equals("0")) {
                    mBinding.schedule3Cb.setChecked(false);
                }

                sch4Time = schedule4Data.split("");
                sch4St = sch4Time[1] + sch4Time[2] + ":" + sch4Time[3] + sch4Time[4];
                sch4Et = sch4Time[5] + sch4Time[6] + ":" + sch4Time[7] + sch4Time[8];
                mBinding.sch4st.setText(sch4St);
                mBinding.sch4et.setText(sch4Et);

                if (sch4Time[0].equals("1")) {
                    mBinding.schedule4Cb.setChecked(true);
                } else if (sch4Time[0].equals("0")) {
                    mBinding.schedule4Cb.setChecked(false);
                }

            } else if (spiltData[1].equals("1")) {
                if (spiltData[2].equals("RECEIVED")){
                    Toast.makeText(mContext, "Schedule Updated Successfully !", Toast.LENGTH_SHORT).show();
                }
            }
            dismissProgress();
        }
    }

    @Override
    public void OnDataReceived(String data) {
        Log.e("ScheduleV2", "OnDataReceived: " + data);
        if (data.contains("ST#")) {
            handleData(data);
        } else if (data.equals("NACK")) {
            mNackCount++;
            if (mNackCount > 2) {
                Toast.makeText(mContext, "Operation failed \n Restart Stabilizer and try again", Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        } else {
            Log.e("V2Error ", "OnDataReceive: " + data);
            // Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
        }
    }

    /* 30/12/2020 */

    /*Log.d(TAG, "OnDataReceive: 26-12-2020 ->receive " + data);
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
                    mAppClass.sendPacket(ScheduleActivity.this, packetToSend);
                }
            }, 500);
        } else if (data.contains("ST#")) {
            handleData(data, 0);
        }*/

}