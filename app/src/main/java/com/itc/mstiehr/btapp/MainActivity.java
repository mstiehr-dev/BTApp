package com.itc.mstiehr.btapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/*
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
*/
public class MainActivity extends Activity
{

    private static final int REQUEST_ENABLE_BT = 65535;
    private static final String TAG = MainActivity.class.getSimpleName();

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> btDeviceList;
    private ActionFoundReceiver actionFoundReceiver;
    private BluetoothDevice k01;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
        {
            Toast.makeText(this, "BT nicht möglich", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes Bluetooth adapter.
        //            final BluetoothManager bluetoothManager =
        //                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //            bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Toast.makeText(this, "BT geladen", Toast.LENGTH_SHORT).show();
        turn_on();
        btDeviceList = new ArrayList<>();
        actionFoundReceiver = new ActionFoundReceiver(btDeviceList);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(actionFoundReceiver, filter);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == RESULT_OK)
            {
                Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
            }
            else if (resultCode == RESULT_CANCELED)
            {
                Toast.makeText(getApplicationContext(), "cancelled", Toast.LENGTH_LONG).show();
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy ()
    {
        super.onDestroy();
        turn_off();
        unregisterReceiver(actionFoundReceiver);
    }

    @InjectView(R.id.btn_turn_on)
    Button btn_turn_on;

    @OnClick(R.id.btn_turn_on)
    void turn_on ()
    {
        if (!bluetoothAdapter.isEnabled())
        {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, REQUEST_ENABLE_BT);
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
    }

    @InjectView(R.id.btn_turn_off)
    Button btn_turn_off;

    @OnClick(R.id.btn_turn_off)
    void turn_off ()
    {
        // require the BLUETOOTH_ADMIN permission
        if (bluetoothAdapter != null)
            bluetoothAdapter.disable();
        Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG).show();
    }

    @InjectView(R.id.btn_scan)
    Button btn_scan;

    @OnClick(R.id.btn_scan)
    void scan ()
    {
        bluetoothAdapter.startDiscovery();
    }

    @InjectView(R.id.btn_show)
    Button btn_show;

    @OnClick(R.id.btn_show)
    void show ()
    {
        if (bluetoothAdapter.getBondedDevices().size() == 0)
        {
            Log.d(TAG, "NO DEVICE BONDED");
            return;
        }
        for (BluetoothDevice bondedDevice : bluetoothAdapter.getBondedDevices())
        {
            Log.i(TAG, " -> bd: " + bondedDevice.getName());
            if (bondedDevice.getName().contains("K01"))
            {
                this.k01 = bondedDevice;
                Toast.makeText(this, "Device bonded", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private MediaRecorder mediaRecorder;
    private boolean isRecording;
    @InjectView(R.id.btn_record_start)
    Button btn_record_start;

    @OnClick(R.id.btn_record_start)
    void record_start ()
    {
        if (null == mediaRecorder)
        {
            mediaRecorder = new MediaRecorder();
        }
        if (isRecording)
            return;
        String filename =
                Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/record.mp3";
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(filename);
        try
        {
            mediaRecorder.prepare();
            isRecording = true;
            mediaRecorder.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @InjectView(R.id.btn_record_stop)
    Button btn_record_stop;

    @OnClick(R.id.btn_record_stop)
    void record_stop ()
    {
        if(isRecording)
        {
            mediaRecorder.stop();
        }
    }

    @InjectView(R.id.btn_execute)
    Button btn_exec;

    @OnClick(R.id.btn_execute)
    public void executeCommand ()
    {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled())
        {
                bluetoothAdapter.cancelDiscovery();
                new BtReadAsyncTask(this, bluetoothAdapter, k01).execute();
        }
    }

    @InjectView(R.id.btn_list_devices)
    Button btn_list_devices;
    @OnClick(R.id.btn_list_devices)
    void list()
    {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Bonded Devices:");
        String msg = "";
        for(BluetoothDevice d : btDeviceList) {
            msg += d.getName() + "\n";
        }
        alertDialog.setMessage(msg);
        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick (DialogInterface dialogInterface, int i)
            {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }
}
