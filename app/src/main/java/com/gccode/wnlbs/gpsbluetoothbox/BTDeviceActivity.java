package com.gccode.wnlbs.gpsbluetoothbox;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.gccode.wnlbs.gpsbluetoothbox.utils.Utils;


public class BTDeviceActivity extends Activity implements OnItemClickListener
        ,View.OnClickListener ,StatusBlueTooth, PairDeviceListener {

    //  private List<BTItem> mListDeviceBT=new ArrayList<BTItem>();
    private ListView deviceListview;
    private Button btserch;
    private BTDeviceAdapter adapter;
    private boolean hasregister=false;
    private BTItem mItem;

    @Override
    public void Bonded() {
        setResult(100);
        finish();
    }

    @Override
    public void Bonding() {
        mItem.setBluetoothType(BluetoothDevice.BOND_BONDING);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void Bondnone() {
        mItem.setBluetoothType(BluetoothDevice.BOND_NONE);
        deviceListview.setOnItemClickListener(BTDeviceActivity.this);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finddevice);
        setView();

        BTManage.getInstance().setBlueListner(this);

        checkLocationPermission();
    }

    private void setView(){
        deviceListview=(ListView)findViewById(R.id.devicelist);
        deviceListview.setOnItemClickListener(this);
        adapter=new BTDeviceAdapter(getApplicationContext());
        deviceListview.setAdapter(adapter);
        deviceListview.setOnItemClickListener(this);
        btserch=(Button)findViewById(R.id.start_seach);
        btserch.setOnClickListener(this);
    }

    protected void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(BTDeviceActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(BTDeviceActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    99);
        }else{
            startSearchOrstop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 99: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startSearchOrstop(); // --->
                } else {
                    //TODO re-request
                }
                break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //注册蓝牙接收广播    
        if(!hasregister){
            hasregister=true;
            BTManage.getInstance().registerBluetoothReceiver(getApplicationContext());
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub  
        super.onResume();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub  
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(hasregister){
            hasregister=false;
            BTManage.getInstance().unregisterBluetooth(getApplicationContext());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {

        final BTItem item=(BTItem)adapter.getItem(position);


        AlertDialog.Builder dialog = new AlertDialog.Builder(this);// 定义一个弹出框对象    
        dialog.setTitle("Confirmed connecting device");
        dialog.setMessage(item.getBuletoothName());
        dialog.setPositiveButton("connect",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //  btserch.setText("repeat search");
                        BTManage.getInstance().cancelScanDevice();
                        BluetoothMsg.BlueToothAddress=item.getBluetoothAddress();

                        if(BluetoothMsg.lastblueToothAddress!=BluetoothMsg.BlueToothAddress){
                            BluetoothMsg.lastblueToothAddress=BluetoothMsg.BlueToothAddress;
                        }
                        if(item.getBluetoothType() != BluetoothDevice.BOND_BONDED){
                            deviceListview.setOnItemClickListener(null);
                            mItem = item;
                            BTManage.getInstance().setPairDeviceListener(BTDeviceActivity.this);

                            BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                                    .getDefaultAdapter();

                            bluetoothAdapter.cancelDiscovery();

                            if (!bluetoothAdapter.isEnabled())
                            {
                                bluetoothAdapter.enable();
                            }

                            if (!BluetoothAdapter.checkBluetoothAddress(item.getBluetoothAddress()))
                            { // 检查蓝牙地址是否有效

                            }

                            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(item.getBluetoothAddress());
                            try {
                                Utils.createBond(device.getClass(),device);
                                Utils.setPin(device.getClass(),device, "8888");
                                Utils.createBond(device.getClass(),device);
                            }catch (Exception e){
                                mItem.setBluetoothType(BluetoothDevice.BOND_NONE);
                                deviceListview.setOnItemClickListener(BTDeviceActivity.this);
                            }
                        }else{
                            setResult(100);
                            finish();
                        }

                    }
                });
        dialog.setNegativeButton("cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BluetoothMsg.BlueToothAddress = null;
                    }
                });
        dialog.show();
    }

    @Override
    public void onClick(View v) {
//        startSearchOrstop();
        checkLocationPermission();
    }

    private void startSearchOrstop(){
        BTManage.getInstance().openBluetooth(this);

        if(BTManage.getInstance().isDiscovering()){
            BTManage.getInstance().cancelScanDevice();
            btserch.setText("start search");
        }else{
            BTManage.getInstance().scanDevice();
            btserch.setText("stop search");
        }
    }

    @Override
    public void BTDeviceSearchStatus(int resultCode) {
        switch(resultCode){
            case StatusBlueTooth.SEARCH_START:
                adapter.clearData();
                adapter.addDataModel(BTManage.getInstance().getPairBluetoothItem());
                break;
            case StatusBlueTooth.SEARCH_END:
                break;
        }
    }

    @Override
    public void BTSearchFindItem(BTItem item) {
        adapter.addDataModel(item);
    }

    @Override
    public void BTConnectStatus(int result) {

    }

}  
