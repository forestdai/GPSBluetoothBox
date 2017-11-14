package com.olinkstar.bdsviewin;

public abstract interface GattCallback
{
    public abstract void onDisconnected();

    public abstract void onConnectionError();

    public abstract void onTimeOut();

    public abstract void onConnected(int paramInt);

    public abstract void onRead(byte[] paramArrayOfByte);

    public abstract void onWrite();

    public abstract void onWrite(boolean paramBoolean);

    public abstract void onRSSI(int paramInt1, int paramInt2);
}