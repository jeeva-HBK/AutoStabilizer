package com.rax.autostabilizer.Activities;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

import static com.rax.autostabilizer.Utilities.S_Communication.mIPAddress;
import static com.rax.autostabilizer.Utilities.S_Communication.mPortNumber;

public class StabilizerListActivity extends AppCompatActivity implements StabilizerListAdapter.ClickListener {

    ActivityStabilizerListBinding mBinding;
    StabilizerListAdapter mAdapter;
    Repository mRepo;
    private Context mContext;
    private ApplicationClass mAppClass;

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
        mAdapter.setData(mRepo.getStabilizerList(this));

        mBinding.rvStabilizerFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment d = new DialogFragment(new SmartConfigFragment(), "Add Stabilizer");
                d.show(getSupportFragmentManager(), null);
            }
        });


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

    @Override
    public void OnStabilizerClicked(Stabilizer stabilizer) {
        mIPAddress = stabilizer.getIPAddress();
        mPortNumber = stabilizer.getPort();
        mAppClass.sendPacket(new ApplicationClass.TCPDataListener() {
            @Override
            public void OnDataReceive(String data) {

            }
        }, "");
    }
}