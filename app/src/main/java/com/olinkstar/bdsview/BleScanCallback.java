package com.olinkstar.bdsview;

import android.bluetooth.BluetoothDevice;

public abstract interface BleScanCallback
{
    public abstract void onStartScan();

    public abstract void onStopScan();

    public abstract void onBLeScanResult(BluetoothDevice paramBluetoothDevice, int paramInt, byte[] paramArrayOfByte);
}