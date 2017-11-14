package com.olinkstar.bdsviewin;

import android.bluetooth.BluetoothDevice;

public abstract interface MScanCallback
{
    public abstract void onStartScan();

    public abstract void onStopScan();

    public abstract void onLeScan(BluetoothDevice paramBluetoothDevice, int paramInt, byte[] paramArrayOfByte);
}