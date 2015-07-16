package com.itc.mstiehr.btapp;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BtReadAsyncTask extends AsyncTask<Void, Void, String>
{
    private final static String TAG = BtReadAsyncTask.class.getSimpleName();

    Context context;
    BluetoothSocket bluetoothSocket;
    BluetoothDevice device;
    BluetoothAdapter adapter;
    BufferedReader reader;
    ProgressDialog pd;

    public final static int SOH = 0x01; // START OF HEAD
    public final static int STX = 0x02; // START OF TEXT
    public final static int ETX = 0x03; // END OF TEXT
    public final static int NAK = 0x15; // NEGATIVE ACKNOWLEDGEMENT
    public final static int APO = 0x60; // APOSTROPHE

    char[] commandHello = {
            0x2f,// ------------------
            0x3f,
            0x21,// say hello :-)
            0x0D,
            0x0A
    };

    char[] commandAckAndProgMode = {
            0x06,
            0x30,
            0x30,// switch into programming mode
            0x31,
            0x0D,
            0x0A
    };

    char[] commandAskForMeterReading = {
            0x01,// ------------------
            0x52,
            0x35,
            0x02,
            0x31,
            0x2E,
            0x38,// ask for HT meter reading
            0x2E,
            0x30,
            0x28,
            0x29,
            0x03,
            0x5E
            //            0x0D,
            //            0x0A// ------------------
    };

    public BtReadAsyncTask (Context context, BluetoothAdapter adapter, BluetoothDevice device)
    {
        this.context = context;
        this.device = device;
        this.adapter = adapter;
        try
        {
            bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPreExecute ()
    {
        super.onPreExecute();
        pd = new ProgressDialog(context);
        pd.setMessage("transferring");
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.show();
    }

    @Override
    protected String doInBackground (Void[] objects)
    {
        try
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
            }
            if (!bluetoothSocket.isConnected())
            {
                bluetoothSocket.connect();
            }

            if (bluetoothSocket.isConnected())
            {
                reader = new BufferedReader(new InputStreamReader(bluetoothSocket.getInputStream()));
                // say hello
                String helloResult = executeCommand(bluetoothSocket, commandHello, false);
                if (helloResult != null && helloResult.contains("EMH5"))
                {
                    Log.i(TAG, "result for hello: " + helloResult);
                    String resp = executeCommand(bluetoothSocket, commandAckAndProgMode, true);
                    if (resp.contains("P0"))
                    {
                        Log.i(TAG, "ack request completed");
                        String tString = "";
                        while ("".equals(tString))
                        {
                            tString = executeCommand(bluetoothSocket, commandAskForMeterReading, true);
                        }
                        String meterReadingHT = tString.substring(tString.indexOf('(') + 1, tString.indexOf(')'));
                        //                        Log.i(TAG, "result for meterReadingHT: " + meterReadingHT);
                        String meterReadingHTValue = meterReadingHT.substring(0, meterReadingHT.indexOf('*'));
                        double val = Double.parseDouble(meterReadingHTValue);
                        String meterReadingHTUnit = meterReadingHT.substring(meterReadingHT.indexOf('*') + 1);
                        Log.d(TAG, "Value::Unit -> " + val + "::" + meterReadingHTUnit);
                        //Toast.makeText(context, val + " " + meterReadingHTUnit, Toast.LENGTH_SHORT).show();
                        return "" + val;
                    }
                    else
                    {
                        Log.d(TAG, "resp did not contain 'P0'");
                    }
                }
                else
                {
                    Log.i(TAG, "error while connecting the bluetooth device: " + helloResult);
                }
            }
            else
            {
                Log.d(TAG, "socket is not connected");
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "Exception: ", e);
        }
        finally
        {
            if (bluetoothSocket != null)
            {
                try
                {
                    bluetoothSocket.close();
                }
                catch (IOException e)
                {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute (String s)
    {
        super.onPostExecute(s);
        if (null != pd)
        {
            pd.dismiss();
        }
        Toast.makeText(context, s + " kWh", Toast.LENGTH_LONG).show();
    }

    private String executeCommand (BluetoothSocket bluetoothSocket, char[] command, boolean readBits) throws IOException
    {
        Log.d(TAG, "connected: " + bluetoothSocket.isConnected());
        Log.d(TAG, "sending: " + new String(command));
        for (char hex : command)
        {
            bluetoothSocket.getOutputStream().write(hex);
        }
        Log.d(TAG, "sending done");

        if (!readBits)
        {
            return reader.readLine();
        }
        if (readBits)
        {
            StringBuilder sb = new StringBuilder();
            int x;
            while ((x = reader.read()) != -1)
            {
                if (!(x == SOH || x == ETX || x == STX || x == NAK || x == APO))
                {
                    sb.append((char) x);
                    Log.d(TAG, x + " -> " + (char) x);
                }
                if (x == 0x03 || x == 0x15)
                {
                    break;
                }
            }
            return sb.toString();
        }
        return "";
    }
}
