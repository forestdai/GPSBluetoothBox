package com.olinkstar.bdsview;

import com.olinkstar.bdsviewin.BLeService;

public class RtcmDataManager extends BleManager
{
    public boolean InitRtcmDataManager()
    {
        if (bleService != null)
            bleService.CtrlRtcmThread(true);
        return true;
    }

    public int TxRtcmData(byte[] buffer) {
        if (bleService != null) {
            bleService.SdkData(buffer, true);
        }

        return buffer.length;
    }

    public void OnSdkRunStatusChange(int nType, int nCode)
    {
    }
}