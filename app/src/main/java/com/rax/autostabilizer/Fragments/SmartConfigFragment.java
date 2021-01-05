package com.rax.autostabilizer.Fragments;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.location.LocationManagerCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.util.ByteUtil;
import com.espressif.iot.esptouch.util.TouchNetUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rax.autostabilizer.Database.Repository;
import com.rax.autostabilizer.Models.Stabilizer;
import com.rax.autostabilizer.R;
import com.rax.autostabilizer.Utilities.S_Communication;
import com.rax.autostabilizer.databinding.FragmentSmartConfigBinding;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.content.Context.WIFI_SERVICE;
import static com.rax.autostabilizer.Utilities.S_Communication.ACTION_MyIntentService;


public class SmartConfigFragment extends Fragment {
    private static final int REQUEST_PERMISSION = 0x01;
    private static final String TAG = "SmartConfigFragment";
    private final int RETRY_COUNT = 3, RETRY_DELAY = 2000;
    public String configURL, mIPAddress, mDeviceMac;
    public FragmentSmartConfigBinding binding;
    private boolean mReceiverRegistered = false;
    private Context mContext;
    public DialogFragment parentDialog;
    private EspSmartConfig mTask;
    private boolean mDestroyed = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(WIFI_SERVICE);
            Log.d(TAG, "onReceive: " + wifiManager.is5GHzBandSupported());
            assert wifiManager != null;

            switch (action) {
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                case LocationManager.PROVIDERS_CHANGED_ACTION:
                    onWifiChanged(wifiManager.getConnectionInfo());
                    break;
            }
        }
    };

    public SmartConfigFragment(DialogFragment parentDialog) {
        this.parentDialog = parentDialog;
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        return super.onOptionsItemSelected(item);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.fragment_smart_config, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        mContext = getContext();

        if (isSDKAtLeastP()) {
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
                requestPermissions(permissions, REQUEST_PERMISSION);
            } else {
                registerBroadcastReceiver();
            }

        } else {
            registerBroadcastReceiver();
        }
        binding.edtNAME.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_NEXT){
                    binding.edtPass.requestFocus();
                }
                return false;
            }
        });

        binding.btnSearch.setOnClickListener(v -> {

            if (binding.edtSSID.getText().toString().equals("")) {
                Toast.makeText(mContext, "Connect to wifi network", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.edtBSSID.getText().toString().equals("")) {
                Toast.makeText(mContext, "Unknown BSSID", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.edtNAME.getText().toString().equals("")) {
                Toast.makeText(mContext, "Enter Name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.edtPass.getText().toString().equals("")) {
                Toast.makeText(mContext, "Enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] ssid = binding.edtSSID.getTag() == null ? ByteUtil.getBytesByString(binding.edtSSID.getText().toString())
                    : (byte[]) binding.edtSSID.getTag();
            byte[] password = ByteUtil.getBytesByString(binding.edtPass.getText().toString());
            byte[] bssid = TouchNetUtil.parseBssid2bytes(binding.edtBSSID.getText().toString());
            byte[] deviceCount = "1".getBytes();
//                byte[] broadcast = {(byte) (mPackageModeGroup.getCheckedRadioButtonId() == R.id.package_broadcast
//                        ? 1 : 0)};

            byte[] broadcast = {(byte) 1};

            if (mTask != null) {
                mTask.cancelEsptouch();
            }
            mTask = new EspSmartConfig(SmartConfigFragment.this);
            mTask.execute(ssid, bssid, password, deviceCount, broadcast);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!mDestroyed) {
                    registerBroadcastReceiver();
                }
            }
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDestroyed = true;
        if (mReceiverRegistered) {
            getActivity().unregisterReceiver(mReceiver);
        }
    }

    private void onWifiChanged(WifiInfo info) {
        boolean disconnected = info == null
                || info.getNetworkId() == -1
                || "<unknown ssid>".equals(info.getSSID());
        if (disconnected) {
            binding.edtSSID.setText("");
            binding.edtSSID.setTag(null);
            Toast.makeText(mContext, "No Wifi Connection", Toast.LENGTH_SHORT).show();
            binding.btnSearch.setEnabled(false);
            if (isSDKAtLeastP()) {
                checkLocation();
            }
            if (mTask != null) {
                mTask.cancelEsptouch();
                mTask = null;
                new AlertDialog.Builder(mContext)
                        .setMessage("Wifi disconnected or changed")
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        } else {
            String ssid = info.getSSID();
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            binding.edtSSID.setText(ssid);
            binding.edtSSID.setTag(ByteUtil.getBytesByString(ssid));
            byte[] ssidOriginalData = TouchNetUtil.getOriginalSsidBytes(info);
            binding.edtSSID.setTag(ssidOriginalData);
            String bssid = info.getBSSID();
            binding.edtBSSID.setText(bssid);
            binding.btnSearch.setEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int frequency = info.getFrequency();
                if (frequency > 4900 && frequency < 5900) {
                    // Connected 5G wifi. Device does not support 5G
//                    new MaterialAlertDialogBuilder(mContext)
//                            .setTitle("Smart Config")
//                            .setMessage("Smart device does'nt support 5G Wifi, please make sure the currently connected Wifi is 2.4G")
//                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialogInterface, int i) {
//                                    getActivity().finish();
//                                }
//                            })
//                            .setCancelable(true)
//                            .show();
                }
            }
        }
    }

    private void checkLocation() {
        boolean enable;
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        enable = locationManager != null && LocationManagerCompat.isLocationEnabled(locationManager);
        if (!enable) {
            Toast.makeText(mContext, "Location(GPS) is disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        if (isSDKAtLeastP()) {
            filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        }
        getActivity().registerReceiver(mReceiver, filter);
        mReceiverRegistered = true;
    }

    private boolean isSDKAtLeastP() {
        return Build.VERSION.SDK_INT >= 28;
    }


    private void showProgress() {
        binding.progressCircular.setVisibility(View.VISIBLE);
    }

    private void dismissProgress() {
        binding.progressCircular.setVisibility(View.GONE);
    }


    public void sendData(Context mContext, String message) {
        Intent mServiceIntent = new Intent(mContext,
                S_Communication.class);
        mServiceIntent.putExtra("test", message);
        mContext.startService(mServiceIntent);
    }

    public static class EspSmartConfig extends AsyncTask<byte[], IEsptouchResult, List<IEsptouchResult>> {
        private final Object mLock = new Object();
        boolean isReceiverRegistered = false;
        private WeakReference<SmartConfigFragment> mActivity;
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    String data = intent.getStringExtra("news");
                    Log.d(TAG, data);
                    if (data.contains("OK")) {
                        mActivity.get().dismissProgress();
                        // Save Device here
                        new MaterialAlertDialogBuilder(mActivity.get().mContext)
                                .setTitle("Stabilizer Configured")
                                .setMessage("IP Address: " + mActivity.get().mIPAddress + "\n" + "MAC: " + mActivity.get().mDeviceMac)
                                .setPositiveButton("OK", null)
                                .show();
                    } else if (data.contains("NOT OK")) {
                        mActivity.get().dismissProgress();
                        new MaterialAlertDialogBuilder(mActivity.get().mContext).setTitle("Commissioning failed").
                                setMessage("Stabilizer responded with error code").show();
                    }
                } catch (Exception e) {
                    mActivity.get().dismissProgress();
                    Toast.makeText(context, "Error occurred", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

            }
        };
        private ProgressDialog mProgressDialog;
        private AlertDialog mResultDialog;
        private IEsptouchTask mEsptouchTask;

        EspSmartConfig(SmartConfigFragment mActivity) {
            this.mActivity = new WeakReference<>(mActivity);
            //  registerCustomReceiver(mActivity.mContext, receiver);
        }

        void registerCustomReceiver(Context context, BroadcastReceiver receiver) {
            if (!isReceiverRegistered) {
                IntentFilter intentFilter = new IntentFilter(ACTION_MyIntentService);
                intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
                context.registerReceiver(receiver, intentFilter);
                isReceiverRegistered = true;
            }

        }

        void unregisterReceiver() {
            if (isReceiverRegistered) {
                mActivity.get().getActivity().unregisterReceiver(receiver);
                isReceiverRegistered = false;
            }
        }

        void cancelEsptouch() {
            cancel(true);
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            if (mResultDialog != null) {
                mResultDialog.dismiss();
            }
            if (mEsptouchTask != null) {
                mEsptouchTask.interrupt();
            }
        }

        @Override
        protected void onPreExecute() {
            Activity activity = mActivity.get().getActivity();
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage("Stabilizer is being configured…");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(dialog -> {
                synchronized (mLock) {
                    if (mEsptouchTask != null) {
                        mEsptouchTask.interrupt();
                    }
                }
            });
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getText(android.R.string.cancel),
                    (dialog, which) -> {
                        synchronized (mLock) {
                            if (mEsptouchTask != null) {
                                mEsptouchTask.interrupt();
                            }
                        }
                    });
            mProgressDialog.show();
        }

        @Override
        protected List<IEsptouchResult> doInBackground(byte[]... params) {
            SmartConfigFragment activity = mActivity.get();
            int taskResultCount;
            synchronized (mLock) {
                byte[] apSsid = params[0];
                byte[] apBssid = params[1];
                byte[] apPassword = params[2];
                byte[] deviceCountData = params[3];
                byte[] broadcastData = params[4];
                taskResultCount = deviceCountData.length == 0 ? -1 : Integer.parseInt(new String(deviceCountData));
                Context context = activity.getActivity().getApplicationContext();
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, context);
                mEsptouchTask.setPackageBroadcast(broadcastData[0] == 1);
                mEsptouchTask.setEsptouchListener(this::publishProgress);
            }
            return mEsptouchTask.executeForResults(taskResultCount);
        }

        @Override
        protected void onProgressUpdate(IEsptouchResult... values) {
            Context context = mActivity.get().getActivity();
            if (context != null) {
                IEsptouchResult result = values[0];
                Log.i(TAG, "EspTouchResult: " + result);
                String text = result.getBssid() + " is connected to the wifi";
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            SmartConfigFragment activity = mActivity.get();
            activity.mTask = null;
            mProgressDialog.dismiss();
            if (result == null) {
                mResultDialog = new AlertDialog.Builder(activity.getActivity())
                        .setMessage("Unable to start config process")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                mResultDialog.setCanceledOnTouchOutside(false);
                unregisterReceiver();
                return;
            }

            // check whether the task is cancelled and no results received
            IEsptouchResult firstResult = result.get(0);
            if (firstResult.isCancelled()) {
                unregisterReceiver();
                return;
            }
            // the task received some results including cancelled while
            // executing before receiving enough results

            if (!firstResult.isSuc()) {
                mResultDialog = new AlertDialog.Builder(activity.getActivity())
                        .setMessage("Config failed")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                mResultDialog.setCanceledOnTouchOutside(false);
                unregisterReceiver();
                return;
            }
            mActivity.get().mIPAddress = firstResult.getInetAddress().getHostAddress();
            mActivity.get().mDeviceMac = firstResult.getBssid();
            new MaterialAlertDialogBuilder(mActivity.get().mContext)
                    .setTitle("Stabilizer Configured")
                    .setMessage("IP Address: " + mActivity.get().mIPAddress + "\n" + "MAC: " + mActivity.get().mDeviceMac)
                    .setPositiveButton("OK", (dialogInterface, i) -> {
                        if (mActivity.get().parentDialog != null) {
                            mActivity.get().parentDialog.dismiss();
                        }
                    })
                    .show();
            Repository repository = new Repository();
            repository.saveStabilizer(mActivity.get().getActivity(),
                    new Stabilizer(mActivity.get().binding.edtNAME.getText().toString(), mActivity.get().mIPAddress, 6000, mActivity.get().mDeviceMac));

        }
    }
}
