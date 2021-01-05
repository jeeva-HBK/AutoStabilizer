package com.rax.autostabilizer.Database;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.rax.autostabilizer.Models.Stabilizer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class Repository {
    private static String PREF_KEY = "StabilizerDB";
    private static String STAB_NAME = "StabilizerName", STAB_IP = "StabilizerIP",
            STAB_PORT = "StabilizerPort", STAB_MAC = "StabilizerMAC";

    public boolean saveStabilizer(Activity mActivity, Stabilizer stabilizer) {
        try {
            JSONArray machineArray;
            JSONObject machineObject;
            SharedPreferences preferences = mActivity.getPreferences(Context.MODE_PRIVATE);
            String machineString = preferences.getString(PREF_KEY, "");
            if (!machineString.equals("")) {
                machineArray = new JSONArray(machineString);
            } else {
                machineArray = new JSONArray();
            }

            for (int i = 0; i < machineArray.length(); i++) {
                JSONObject object = machineArray.getJSONObject(i);
                if (object.get(STAB_MAC).equals(stabilizer.getMacAddress())) {
                    machineArray.remove(i);
                }
            }

            machineObject = new JSONObject();
            machineObject.put(STAB_NAME, stabilizer.getName());
            machineObject.put(STAB_IP, stabilizer.getIPAddress());
            machineObject.put(STAB_PORT, stabilizer.getPort());
            machineObject.put(STAB_MAC, stabilizer.getMacAddress());
            machineArray.put(machineObject);

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(PREF_KEY, machineArray.toString());
            editor.apply();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mActivity, "Error occurred", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public ArrayList<Stabilizer> getStabilizerList(Activity mActivity) {
        try {
            JSONArray machineArray;
            ArrayList<Stabilizer> machineList = new ArrayList<>();
            SharedPreferences preferences = mActivity.getPreferences(Context.MODE_PRIVATE);
            String machineString = preferences.getString(PREF_KEY, "");
            if (machineString.equals("")) {
                return new ArrayList<>();
            } else {
                machineArray = new JSONArray(machineString);
                for (int i = 0; i < machineArray.length(); i++) {
                    JSONObject object = machineArray.getJSONObject(i);
                    machineList.add(new Stabilizer(
                            object.getString(STAB_NAME),
                            object.getString(STAB_IP),
                            object.getInt(STAB_PORT),
                            object.getString(STAB_MAC)
                    ));
                }
                return machineList;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mActivity, "Error occurred", Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
    }

    public boolean deleteStabilizer(Activity mActivity, String macAddress) {
        try {
            SharedPreferences preferences = mActivity.getPreferences(Context.MODE_PRIVATE);
            String machineString = preferences.getString(PREF_KEY, "");
            if (machineString.equals("")) {
                return false;
            } else {
                JSONArray machineArray = new JSONArray(machineString);
                for (int i = 0; i < machineArray.length(); i++) {
                    JSONObject object = machineArray.getJSONObject(i);
                    if (object.get(STAB_MAC).equals(macAddress)) {
                        machineArray.remove(i);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(PREF_KEY, machineArray.toString());
                        editor.apply();
                        return true;
                    }
                }
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mActivity, "Error occurred", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

}
