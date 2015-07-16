package com.itc.mstiehr.btapp;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by MStiehr on 14.07.2015.
 */
public class ActionFoundReceiver extends BroadcastReceiver
{
    private final static String TAG = ActionFoundReceiver.class.getSimpleName();
    ArrayList<BluetoothDevice> btDeviceList;
    ProgressDialog pd;

    public ActionFoundReceiver(ArrayList<BluetoothDevice> btList)
    {
        btDeviceList = btList;
    }

    @Override
    public void onReceive (Context context, Intent intent)
    {
        if(null!=intent)
        {
            switch(intent.getAction())
            {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    btDeviceList.add(device);
                    Log.d(TAG, "action: ACTION_FOUND" + " " + device.getName());
                    break;
                case BluetoothDevice.ACTION_UUID:
                    Log.d(TAG, "action: ACTION_UUID");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.d(TAG, "action: ACTION_DISCOVERY_STARTED");
                    pd = new ProgressDialog(context);
                    pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    pd.setMessage("Scanning BT Network");
                    pd.show();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d(TAG, "action: ACTION_DISCOVERY_FINISHED");
                    if(null!=pd) {
                        pd.dismiss();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
