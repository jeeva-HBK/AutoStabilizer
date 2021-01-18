package com.rax.autostabilizer.Activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rax.autostabilizer.Adapters.StabilizerListAdapter;
import com.rax.autostabilizer.ApplicationClass;
import com.rax.autostabilizer.ConnectionMode;
import com.rax.autostabilizer.Database.Repository;
import com.rax.autostabilizer.Fragments.DialogFragment;
import com.rax.autostabilizer.Fragments.SmartConfigFragment;
import com.rax.autostabilizer.Models.Stabilizer;
import com.rax.autostabilizer.R;
import com.rax.autostabilizer.Utilities.AWSIoT;
import com.rax.autostabilizer.Utilities.S_Communication;
import com.rax.autostabilizer.databinding.ActivityStabilizerListBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.rax.autostabilizer.ApplicationClass.macAddress;
import static com.rax.autostabilizer.Utilities.AWSIoT.AWS_CONNECTED;
import static com.rax.autostabilizer.Utilities.AWSIoT.AWS_NOT_CONNECTED;
import static com.rax.autostabilizer.Utilities.S_Communication.CONNECTED;
import static com.rax.autostabilizer.Utilities.S_Communication.mIPAddress;
import static com.rax.autostabilizer.Utilities.S_Communication.mPortNumber;

public class StabilizerListActivity extends AppCompatActivity implements StabilizerListAdapter.ClickListener {

    private static final Pattern IP_ADDRESS
            = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))");
    private static final String TAG = "StabilizerListActivity";
    ActivityStabilizerListBinding mBinding;
    StabilizerListAdapter mAdapter;
    Repository mRepo;
    private Context mContext;
    private ApplicationClass mAppClass;
    private boolean isFabOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_stabilizer_list);
        mContext = this;
        mAppClass = (ApplicationClass) getApplication();
        mRepo = new Repository();
        mAdapter = new StabilizerListAdapter(this);
        mBinding.rvStabilizerList.setLayoutManager(new LinearLayoutManager(mContext));
        mBinding.rvStabilizerList.setAdapter(mAdapter);

        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 111);

        mBinding.fabAddExisting.setOnClickListener(view -> {
            closeFab();
            showAddDialog();
        });

        mBinding.fabAddNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeFab();
                DialogFragment dialog = new DialogFragment(new DialogFragment.OnDismissListener() {
                    @Override
                    public void OnDismiss() {
                        refreshData();
                    }
                });
                dialog.setFragment(new SmartConfigFragment(dialog), getString(R.string.addStabilizer));
                dialog.show(getSupportFragmentManager(), null);
            }
        });

        mBinding.fabAddStabilizer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFabOpen) {
                    closeFab();
                } else {
                    openFab();
                }
            }
        });
    }

    private void refreshData() {
        List<Stabilizer> list = mRepo.getStabilizerList(this);
        if (list.size() == 0) {
            mBinding.emptyState.setVisibility(View.VISIBLE);
        } else {
            mAdapter.setData(list);
            mBinding.emptyState.setVisibility(View.GONE);
        }
    }

    private void closeTelnet() {
        S_Communication communication = new S_Communication();
        communication.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
        //closeTelnet();
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.addStabilizer));
        View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_add_existing, null);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.save, null);
        builder.setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button P = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button N = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                P.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EditText name, ipAddress, macAddress;
                        name = dialogView.findViewById(R.id.dialogNameEt);
                        ipAddress = dialogView.findViewById(R.id.dialogIpEt);
                        macAddress = dialogView.findViewById(R.id.dialogMacEt);
                        Matcher matcher = IP_ADDRESS.matcher(ipAddress.getText().toString());
                        if (name.getText().toString().trim().equals("")) {
                            name.setError(getString(R.string.enterName));
                            return;
                        }
                        if (ipAddress.getText().toString().trim().equals("")) {
                            ipAddress.setError(getString(R.string.enterIp));
                            return;
                        } else if (!matcher.matches()) {
                            ipAddress.setError(getString(R.string.enterAValidIpAddress));
                            return;
                        }
                        if (macAddress.getText().toString().trim().equals("")) {
                            macAddress.setError(getString(R.string.enterMacAdress));
                            return;
                        }
                        Stabilizer stabilizer = new Stabilizer(name.getText().toString()
                                , ipAddress.getText().toString(), 5000
                                , macAddress.getText().toString());
                        Repository repo = new Repository();
                        repo.saveStabilizer(StabilizerListActivity.this, stabilizer);
                        mAppClass.showSnackBar(getString(R.string.complete), mBinding.cod);
                        closeFab();
                        dialogInterface.dismiss();
                        refreshData();
                    }
                });
                N.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialogInterface.dismiss();
                        closeFab();

                    }
                });
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void openFab() {
        isFabOpen = true;
        mBinding.rlWhiteOverlay.setVisibility(View.VISIBLE);
        mBinding.txtAddExisiting.setVisibility(View.VISIBLE);
        mBinding.txtAddnew.setVisibility(View.VISIBLE);
        rotateFab(mBinding.fabAddStabilizer, isFabOpen);
        mBinding.fabAddExisting.show();
        mBinding.fabAddNew.show();
        ColorStateList csl = AppCompatResources.getColorStateList(this, R.color.colorRed);
        Drawable drawable = DrawableCompat.wrap(mBinding.fabAddStabilizer.getDrawable());
        DrawableCompat.setTintList(drawable, csl);
        mBinding.fabAddStabilizer.setImageDrawable(drawable);
    }

    // MacAddress Validation
    /*   public boolean isValidMac(String mac) {
        Pattern p = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
        Matcher m = p.matcher(mac);
        return m.matches();
    }*/

    private void closeFab() {
        isFabOpen = false;
        mBinding.rlWhiteOverlay.setVisibility(View.GONE);
        mBinding.txtAddExisiting.setVisibility(View.GONE);
        mBinding.txtAddnew.setVisibility(View.GONE);
        rotateFab(mBinding.fabAddStabilizer, isFabOpen);
        mBinding.fabAddExisting.hide();
        mBinding.fabAddNew.hide();
        ColorStateList csl = AppCompatResources.getColorStateList(this, R.color.white);
        Drawable drawable = DrawableCompat.wrap(mBinding.fabAddStabilizer.getDrawable());
        DrawableCompat.setTintList(drawable, csl);
        mBinding.fabAddStabilizer.setImageDrawable(drawable);
    }

    private void rotateFab(final View v, boolean rotate) {
        ViewCompat.animate(v).rotation(rotate ? 135f : 0f).withLayer().setDuration(200).setInterpolator(new LinearInterpolator()).start();
    }

    private List<Stabilizer> getStabListDemo() {
        List<Stabilizer> stabilizers = new ArrayList<>();
        stabilizers.add(new Stabilizer("Test", "192.168.6.45", 5000, "454545"));
        stabilizers.add(new Stabilizer("Stabilizer 2", "1.1.1.2", 5050, "4545784578"));
        stabilizers.add(new Stabilizer("Stabilizer 3", "1.1.1.3", 5050, "4545784578"));
        stabilizers.add(new Stabilizer("Stabilizer 4", "1.1.1.4", 5050, "4545784578"));
        return stabilizers;
    }

    @Override
    public void OnStabilizerLongClick(int pos, Stabilizer stabilizer, View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        popup.getMenuInflater()
                .inflate(R.menu.favourite_remove, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(getResources().getString(R.string.app_name))
                        .setMessage(getString(R.string.DeleteConfirmation) + stabilizer.getName() + "?")
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (!mRepo.deleteStabilizer(StabilizerListActivity.this, stabilizer.getMacAddress())) {
                                    mAppClass.showSnackBar(getString(R.string.deleteFailed), mBinding.cod);
                                    return;
                                }
                                mAdapter.mStabilizerList.remove(pos);
                                mAdapter.notifyItemRemoved(pos);
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
                return true;
            }
        });
        popup.show();

    }

    private void showProgress() {
        mBinding.progressbar.setVisibility(View.VISIBLE);
        mBinding.fabAddStabilizer.hide();
    }

    private void dismissProgress() {
        mBinding.progressbar.setVisibility(View.GONE);
        mBinding.fabAddStabilizer.show();
    }

    @Override
    public void OnStabilizerClicked(Stabilizer stabilizer) {
        mIPAddress = stabilizer.getIPAddress();
        mPortNumber = stabilizer.getPort();
        macAddress = stabilizer.getMacAddress().toUpperCase();
        Log.e(TAG, "OnStabilizerClicked: " + mAppClass.checkNetwork().toString());
        switch (mAppClass.checkNetwork()) {
            case TCP:
                showProgress();
                mAppClass.sendPacket(data -> {
                    dismissProgress();
                    if (data.equals(CONNECTED)) {
                        closeTelnet();
                        startActivity(new Intent(StabilizerListActivity.this, StabilizerStatusActivityV2.class));
                        return;
                    }
                    mAppClass.showSnackBar(data, mBinding.cod);
                }, "");
                break;
            case AWSIoT:
                showProgress();
                AWSIoT.getInstance(mContext, data -> {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dismissProgress();
                            Log.d(TAG, "OnStabilizerClicked: " + macAddress);
                            if (mAppClass.checkNetwork() == ConnectionMode.AWSIoT) {
                                if (data.equals(AWS_NOT_CONNECTED)) {
                                    mAppClass.showSnackBar(getString(R.string.unableToReachServer), mBinding.cod);

                                } else if (data.equals(AWS_CONNECTED)) {
                                    startActivity(new Intent(StabilizerListActivity.this, StabilizerStatusActivityV2.class));
                                }
                            }
                        }
                    });
                });
                break;
            case NONE:
                mAppClass.showConnectionPop(StabilizerListActivity.this);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        dismissProgress();
        new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.exit)
                .setMessage(R.string.exitConfirmation)
                .setCancelable(false)
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .show();
    }

}