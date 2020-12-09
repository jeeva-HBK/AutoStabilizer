package com.rax.autostabilizer.Activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.rax.autostabilizer.R;
import com.rax.autostabilizer.databinding.ActivityScheduleBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ScheduleActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    ActivityScheduleBinding mBinding;
    private static final String TAG = "ScheduleActivity";

    TimePickerDialog timePickerDialog;
    SimpleDateFormat newFromat = new SimpleDateFormat("HH:mm");
    SimpleDateFormat twelve = new SimpleDateFormat("hh:mm a");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_schedule);
        getActionBar();
        init();
    }

    private void init() {
        mBinding.schedule1Cb.setOnCheckedChangeListener(this);
        mBinding.schedule2Cb.setOnCheckedChangeListener(this);
        mBinding.schedule3Cb.setOnCheckedChangeListener(this);
        mBinding.schedule4Cb.setOnCheckedChangeListener(this);

        mBinding.pirCb.setOnCheckedChangeListener(this);

        mBinding.schSave.setOnClickListener(this);
        mBinding.sch1st.setOnClickListener(this);
        mBinding.sch1et.setOnClickListener(this);
        mBinding.sch2st.setOnClickListener(this);
        mBinding.sch2et.setOnClickListener(this);
        mBinding.sch3st.setOnClickListener(this);
        mBinding.sch3et.setOnClickListener(this);
        mBinding.sch4st.setOnClickListener(this);
        mBinding.sch4et.setOnClickListener(this);
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
                if (!checkBoxCheck()) {
                    Snackbar.make(mBinding.cod, "Select Any One Of The Schedule !", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                validateAll();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

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

     /*   if (timeStart.getHours()==00) {
            if (start.getTag().equals("AM")) {
                timeStart.setHours(12);
                Log.d(TAG, "Jeeva: if" + timeStart.getHours());
            } else if (start.getTag().equals("PM")) {
                timeStart.setHours(24);
                Log.d(TAG, "Jeeva: else" + timeStart.getHours());
            }
        }*/

        if (timeEnd.before(timeStart)) {
            showSnackBar("Time validation failure !");
            scheduleCb.setChecked(false);
            return;
        }
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
        timePickerDialog.setTitle("Select" + title + "Time");
        timePickerDialog.setCanceledOnTouchOutside(false);
        timePickerDialog.show();
        return finalTime[0];

    }

    private Date formatDate(String Time) {
        Date time = null;
        try {
            time = newFromat.parse(Time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time;
    }
    /*09/12/2020*/
}