package com.rax.autostabilizer.Activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rax.autostabilizer.Adapters.StabilizerListAdapter;
import com.rax.autostabilizer.ApplicationClass;
import com.rax.autostabilizer.Database.Repository;
import com.rax.autostabilizer.Fragments.DialogFragment;
import com.rax.autostabilizer.Fragments.SmartConfigFragment;
import com.rax.autostabilizer.Models.Stabilizer;
import com.rax.autostabilizer.R;
import com.rax.autostabilizer.databinding.ActivityStabilizerListBinding;

import java.util.ArrayList;
import java.util.List;

import static com.rax.autostabilizer.Utilities.S_Communication.CONNECTED;
import static com.rax.autostabilizer.Utilities.S_Communication.mIPAddress;
import static com.rax.autostabilizer.Utilities.S_Communication.mPortNumber;

public class StabilizerListActivity extends AppCompatActivity implements StabilizerListAdapter.ClickListener {

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
        List<Stabilizer> list = mRepo.getStabilizerList(this);
        if (list.size() == 0) {
            mBinding.emptyState.setVisibility(View.VISIBLE);
        } else {
            mAdapter.setData(list);
            mBinding.emptyState.setVisibility(View.GONE);
        }

        //mAdapter.setData(getStabListDemo());

        mBinding.fabAddNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //gva do here
            }
        });

        mBinding.fabAddNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment d = new DialogFragment(new SmartConfigFragment(), "Add Stabilizer");
                d.show(getSupportFragmentManager(), null);
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

    private void openFab() {
        isFabOpen = true;
        mBinding.rlWhiteOverlay.setVisibility(View.VISIBLE);
        mBinding.txtAddExisiting.setVisibility(View.VISIBLE);
        mBinding.txtAddnew.setVisibility(View.VISIBLE);
        rotateFab(mBinding.fabAddStabilizer, isFabOpen);
        mBinding.fabAddExisting.show();
        mBinding.fabAddNew.show();
    }

    private void closeFab() {
        isFabOpen = false;
        mBinding.rlWhiteOverlay.setVisibility(View.GONE);
        mBinding.txtAddExisiting.setVisibility(View.GONE);
        mBinding.txtAddnew.setVisibility(View.GONE);
        rotateFab(mBinding.fabAddStabilizer, isFabOpen);
        mBinding.fabAddExisting.hide();
        mBinding.fabAddNew.hide();
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
                        .setMessage("Are you sure you want to delete " + stabilizer.getName() + "?")
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (!mRepo.deleteStabilizer(StabilizerListActivity.this, stabilizer.getMacAddress())) {
                                    Toast.makeText(mContext, "Delete failed", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                mAdapter.mStabilizerList.remove(pos);
                                mAdapter.notifyItemRemoved(pos);
                            }
                        })
                        .setNegativeButton("Cancel", null)
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

        // startActivity(new Intent(StabilizerListActivity.this, StabilizerStatusActivity.class));

        mIPAddress = stabilizer.getIPAddress();
        mPortNumber = stabilizer.getPort();
        showProgress();
        mAppClass.sendPacket(data -> {
            dismissProgress();
            if (data.equals(CONNECTED)) {
                startActivity(new Intent(StabilizerListActivity.this, StabilizerStatusActivity.class));
                return;
            }
            Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
        }, "");
    }
}