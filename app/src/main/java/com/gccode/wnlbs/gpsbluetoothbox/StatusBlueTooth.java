package com.gccode.wnlbs.gpsbluetoothbox;

public interface StatusBlueTooth {

    final static int SEARCH_START=110;
    final static int SEARCH_END=112;

    final static int serverCreateSuccess=211;
    final static int serverCreateFail=212;
    final static int clientCreateSuccess=221;
    final static int clientCreateFail=222;
    final static int connectLose=231;

    void BTDeviceSearchStatus(int resultCode);
    void BTSearchFindItem(BTItem item);
    void BTConnectStatus(int result);

}
