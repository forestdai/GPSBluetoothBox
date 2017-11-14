package com.olinkstar.bdsview;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.olinkstar.bdsviewin.BLeService;
import com.olinkstar.bdsviewin.MScanCallback;
import com.olinkstar.bdsviewin.NmeaAnalyzer;
import com.olinkstar.bdsviewin.NordicBleManager;
import java.io.UnsupportedEncodingException;

public abstract class BleManager
{
    Activity m_Activity;
    protected static BLeService bleService = null;
    BluetoothDevice m_device = null;
    long m_timeout = 4000L;
    public static int m_RunStatus = -1;
    BLELinkStatusReceiver m_BLELinkStatusReceiver = null;
    boolean m_bReconnectBle = false;

    BLEReconRunnalbe m_BLEReconRunnalbe = new BLEReconRunnalbe();
    Thread BLEReconRunnalbeThread = new Thread();
    boolean bBLEReconRunnalbe = true;

    public abstract void OnSdkRunStatusChange(int paramInt1, int paramInt2);

    public boolean InitSdk(Activity atv)
    {
        if (atv == null)
            return false;
        this.m_Activity = atv;

        this.m_BLELinkStatusReceiver = new BLELinkStatusReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.ols.broadcast.blelink");
        this.m_Activity.registerReceiver(this.m_BLELinkStatusReceiver, filter);

        return true;
    }

    public boolean BleInit()
    {
        if (this.m_Activity == null)
            return false;
        if (bleService == null)
        {
            bleService = new BLeService(this.m_Activity);
            bleService.getBleManager(this.m_Activity);
        }

        int nStatus = bleService.init();
        OnSdkRunStatusChange(1, nStatus);
        m_RunStatus = nStatus;
        return true;
    }

    public void BleScan(long period, final BleScanCallback callback)
    {
        if (bleService == null)
            return;
        if (BLeService.bleManager == null)
            return;
        try {
            BLeService.bleManager.startScanDevices(period, new MScanCallback()
            {
                public void onStartScan()
                {
                    callback.onStartScan();
                    BleManager.this.OnSdkRunStatusChange(1, 17);
                    BleManager.m_RunStatus = 17;
                }

                public void onStopScan()
                {
                    callback.onStopScan();
                    BleManager.this.OnSdkRunStatusChange(1, 16);
                    BleManager.m_RunStatus = 16;
                }

                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
                {
                    BleManager.this.OnSdkRunStatusChange(1, 64);
                    BleManager.m_RunStatus = 64;
                    callback.onBLeScanResult(device, rssi, scanRecord);
                } } );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void BleStopScan()
    {
        if (bleService == null)
            return;
        if (BLeService.bleManager == null)
            return;
        BLeService.bleManager.stopScanDevices();
        OnSdkRunStatusChange(1, 16);
        m_RunStatus = 16;
    }

    public void BleConnect(BluetoothDevice device, long timeout)
    {
        if (bleService == null)
            return;
        if (device != null) {
            this.m_device = device;
            this.m_timeout = timeout;
            bleService.connectDevice(device, timeout);
            Log.v("onBLeScanResult", "onBLeScanResult STATE_GATT_CONNECTING");
            OnSdkRunStatusChange(1, 33);
            m_RunStatus = 33;
        }
        else {
            OnSdkRunStatusChange(1, 65);
            m_RunStatus = 65;
        }
    }

    public void BleDisConnect()
    {
        if (bleService != null) {
            this.m_bReconnectBle = false;
            this.m_device = null;
            bleService.disconnectDevice();
            m_RunStatus = 32;
        }
    }

    private void BleReConnect()
    {
        if ((bleService == null) ||
                (this.m_device == null)) return;
        bleService.connectDevice(this.m_device, this.m_timeout);
        OnSdkRunStatusChange(1, 33);
        m_RunStatus = 33;
    }

    private void BleReInitConnect()
    {
        if (this.m_device == null)
            return;
        BleInit();
        try {
            bleService.init();
            BLeService.bleManager.startScanDevices(4100L, new MScanCallback()
            {
                public void onLeScan(BluetoothDevice arg0, int arg1, byte[] arg2)
                {
                    if (!(arg0.getAddress().equals(BleManager.this.m_device.getAddress())))
                        return;
                    BLeService.bleManager.stopScanDevices();
                    BleManager.bleService.connectDevice(BleManager.this.m_device, 4100L);
                    BleManager.m_RunStatus = 33;
                    BleManager.this.OnSdkRunStatusChange(1, 33);
                }

                public void onStartScan()
                {
                }

                public void onStopScan()
                {
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public int GetBleStatus()
    {
        return m_RunStatus;
    }

    public int GetBleRssi()
    {
        if (bleService != null) {
            return bleService.GetBleRssi();
        }
        return 0;
    }

    public int GetDevicePower()
    {
        if (bleService != null) {
            return bleService.DevicePower();
        }
        return -1;
    }

    public double GetLongitude()
    {
        if (bleService != null) {
            return bleService.nmeaAnalyzer.getLongitude();
        }
        return -999.0D;
    }

    public double GetLatitude()
    {
        if (bleService != null) {
            return bleService.nmeaAnalyzer.getLatitude();
        }
        return -999.0D;
    }

    private String GetLatitudeNS()
    {
        if (bleService != null) {
            return bleService.nmeaAnalyzer.getLatitudeNS();
        }
        return "";
    }

    private String GetLongitudeEW()
    {
        if (bleService != null) {
            return bleService.nmeaAnalyzer.getLongitudeEW();
        }
        return "";
    }

    public double GetAltitude()
    {
        if (bleService != null) {
            return bleService.nmeaAnalyzer.getAltitude();
        }
        return -1.0D;
    }

    public int getMode()
    {
        if (bleService != null) {
            return bleService.nmeaAnalyzer.getMode();
        }
        return -1;
    }

    public double getSpeed()
    {
        if (bleService != null) {
            return bleService.nmeaAnalyzer.getSpeed();
        }
        return -1.0D;
    }

    public String getUtcDateTime()
    {
        if (bleService != null) {
            return bleService.nmeaAnalyzer.getUtcDateTime();
        }
        return "";
    }

    public double getAzimuth()
    {
        if (bleService != null) {
            return bleService.nmeaAnalyzer.getBearing();
        }
        return -1.0D;
    }

    public int getUStates()
    {
        if (bleService != null)
        {
            return bleService.nmeaAnalyzer.getUstars();
        }
        return -1;
    }

    public double getAccuracy()
    {
        if (bleService != null)
        {
            return bleService.nmeaAnalyzer.getSdkAccuracy();
        }
        return -1.0D;
    }

    public double getDop()
    {
        if (bleService != null) {
            return bleService.nmeaAnalyzer.getDop();
        }
        return -1.0D;
    }

    private class BLELinkStatusReceiver extends BroadcastReceiver
    {
        public void onReceive(Context arg0, Intent arg1)
        {
            String sNmea = arg1.getStringExtra("BLELink");
            StringBuilder sb = new StringBuilder();
            try
            {
                sb.append(new String(sNmea.getBytes(), "utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (sb.toString().equals("LinkUp")) {
                BleManager.this.m_bReconnectBle = false;
                Log.d("ReceiveBroadCast", "BLEStatus LinkUp Indicate onconnected");
                BleManager.this.OnSdkRunStatusChange(1, 34);
            } else if (sb.toString().equals("LinkDown")) {
                BleManager.this.OnSdkRunStatusChange(1, 32);
                if (BleManager.this.m_device != null) {
                    if (!(BleManager.this.BLEReconRunnalbeThread.isAlive())) {
                        BleManager.this.BLEReconRunnalbeThread.start();
                    }

                    BleManager.this.m_bReconnectBle = true;
                    Log.d("ReceiveBroadCast", "BLEStatus LinkDown Indicate disonconnected");
                }
            } else {
                BleManager.this.OnSdkRunStatusChange(1, 35);
            }

            Log.d("ReceiveBroadCast", "BLELink= " + sb.toString());
        }
    }

    private class BLEReconRunnalbe
            implements Runnable
    {
        public void run()
        {
            while (BleManager.this.bBLEReconRunnalbe) {
                try {
                    Thread.sleep(10000L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d("ReceiveBroadCast", "BLEStatus BleReConnect Threadrunning +++++++++");
                if (BleManager.this.m_bReconnectBle) {
                    Log.d("ReceiveBroadCast", "BLEStatus BleReConnect +++++++++");
                    BleManager.this.BleReConnect();
                }
            }
        }
    }

    public class SDKStateType
    {
        public static final int STATE_BLUETOOTH = 1;
    }

    public class State
    {
        public static final int STATE_BLUETOOTH_NOT_FOUND = 1;
        public static final int STATE_BLE_UNSUPPORTED = 2;
        public static final int STATE_BLUETOOTH_NOT_OPEN = 3;
        public static final int STATE_BLE_INIT_FINISHED = 4;
        public static final int STATE_BLE_SCAN_STOP = 16;
        public static final int STATE_BLE_SCAN_RUNNING = 64;
        public static final int STATE_BLE_SCAN_START = 17;
        public static final int STATE_GATT_DISCONNECTED = 32;
        public static final int STATE_GATT_CONNECTING = 33;
        public static final int STATE_GATT_CONNECTED = 34;
        public static final int STATE_GATT_ERROR = 35;
        public static final int STATE_BLE_DEVICE_ERR = 65;
        public static final int STATE_BLE_TIMEOUT = 66;
    }
}