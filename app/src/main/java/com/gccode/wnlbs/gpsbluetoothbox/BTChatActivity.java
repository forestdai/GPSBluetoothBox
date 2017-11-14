package com.gccode.wnlbs.gpsbluetoothbox;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.gccode.wnlbs.gpsbluetoothbox.utils.Utils;

import java.util.ArrayList;
import java.util.List;


public class BTChatActivity extends Activity {

    private ListView mListView;
//    private Button sendButton;
//    private Button disconnectButton;
    private Button mSelectDevices;
    private Button mSave;
//    private EditText editMsgView;
    private MessageAdapter mAdapter;
    private List<String> msgList = new ArrayList<String>();

    private BTClient client;
    private BTServer server;

    private String mMsg = "";

    private LocationManager mLocationManager;

    private boolean write = false;

    private OnNmeaMessageListener messageListener = null;
    GpsStatus.NmeaListener nmeaListener = new GpsStatus.NmeaListener() {
        public void onNmeaReceived(long timestamp, String nmea) {
            //check nmea's checksum
            Log.d("GPS-NMEA", "dai = > nmea:"+nmea);
            if(write){
                Utils.write(nmea, Utils.PHONE_GPS);
            }

        }
    };

    LocationListener locationListener=new LocationListener(){

        @Override
        public void onLocationChanged(Location loc) {
                //TODO Auto-generated method stub
                //定位資料更新時會回呼

        }

        @Override
        public void onProviderDisabled(String provider) {
                //TODO Auto-generated method stub
                //定位提供者如果關閉時會回呼，並將關閉的提供者傳至provider字串中
        }

        @Override
        public void onProviderEnabled(String provider) {
                //TODO Auto-generated method stub
                //定位提供者如果開啟時會回呼，並將開啟的提供者傳至provider字串中
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras){
            //TODO Auto-generated method stub
            Log.d("GPS-NMEA",provider + "");
            //GPS狀態提供，這只有提供者為gps時才會動作
            switch(status) {
                case LocationProvider.OUT_OF_SERVICE:
                Log.d("GPS-NMEA","OUT_OF_SERVICE");
                break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.d("GPS-NMEA","TEMPORARILY_UNAVAILABLE");
                break;
                case LocationProvider.AVAILABLE:
                Log.d("GPS-NMEA",""+ provider + "");

                break;
            }

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bt_chat);
        initView();

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if(Build.VERSION.SDK_INT >= 24){
            messageListener = new OnNmeaMessageListener() {
                @Override
                public void onNmeaMessage(String message, long timestamp) {
                    Log.d("GPS-message", message);
                }
            };
            mLocationManager.addNmeaListener(messageListener);
        }else{
            mLocationManager.addNmeaListener(nmeaListener);
        }

    }

    private Handler detectedHandler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            mMsg += msg.obj.toString();
            if(mMsg.indexOf("\r\n") > 0){
                subMessage();
            }

        }
    };

    private void subMessage(){
        String newstr = mMsg.substring(0, mMsg.indexOf("\r\n"));
        if(mMsg.length() >= mMsg.indexOf("\r\n")+2){
            mMsg = mMsg.substring(mMsg.indexOf("\r\n")+2, mMsg.length());
        }

        if(write){
            Utils.write(newstr+"\r\n", Utils.BOX_GPS);
        }
        msgList.add(newstr);
        mAdapter.notifyDataSetChanged();
        mListView.setSelection(msgList.size() - 1);
        if(mMsg.indexOf("\r\n") > 0){
            subMessage();
        }
    }

    private void initView() {

        mAdapter=new MessageAdapter(this, msgList);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setFastScrollEnabled(true);
//        editMsgView= (EditText)findViewById(R.id.MessageText);
//        editMsgView.clearFocus();

/*        RadioGroup group = (RadioGroup)this.findViewById(R.id.radioGroup);
        group.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            //             @Override
//             public void onCheckedChanged(RadioGroup arg0, int arg1) {
//                 int radioId = arg0.getCheckedRadioButtonId();
//
//             }
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId){
                    case R.id.radioNone:
                        BluetoothMsg.serviceOrCilent = BluetoothMsg.ServerOrCilent.NONE;
                        if(null!=client){
                            client.closeBTClient();
                            client=null;
                        }
                        if(null!=server){
                            server.closeBTServer();
                            server=null;
                        }
                        break;
                    case R.id.radioClient:
                        BluetoothMsg.serviceOrCilent = BluetoothMsg.ServerOrCilent.CILENT;
                        Intent it=new Intent(getApplicationContext(),BTDeviceActivity.class);
                        startActivityForResult(it, 100);
                        break;
                    case R.id.radioServer:
                        BluetoothMsg.serviceOrCilent = BluetoothMsg.ServerOrCilent.SERVICE;
                        initConnecter();
                        break;
                }
            }
        });

        sendButton= (Button)findViewById(R.id.btn_msg_send);
        sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {

                String msgText =editMsgView.getText().toString();
                if (msgText.length()>0) {
                    if (BluetoothMsg.serviceOrCilent == BluetoothMsg.ServerOrCilent.CILENT){
                        if(null==client)
                            return;
                        if(client.sendmsg(msgText)){
                            Message msg = new Message();
                            msg.obj = "send: "+msgText;
                            msg.what = 1;
                            detectedHandler.sendMessage(msg);
                        }else{
                            Message msg = new Message();
                            msg.obj = "send fail!! ";
                            msg.what = 1;
                            detectedHandler.sendMessage(msg);
                        }
                    }
                    else if (BluetoothMsg.serviceOrCilent == BluetoothMsg.ServerOrCilent.SERVICE) {
                        if(null==server)
                            return;
                        if(server.sendmsg(msgText)){
                            Message msg = new Message();
                            msg.obj = "send: "+msgText;
                            msg.what = 1;
                            detectedHandler.sendMessage(msg);
                        }else{
                            Message msg = new Message();
                            msg.obj = "send fail!! ";
                            msg.what = 1;
                            detectedHandler.sendMessage(msg);
                        }
                    }
                    editMsgView.setText("");
//                     editMsgView.clearFocus();
//                     //close InputMethodManager
//                     InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//                     imm.hideSoftInputFromWindow(editMsgView.getWindowToken(), 0);
                }else{
                    Toast.makeText(getApplicationContext(), "发送内容不能为空！", Toast.LENGTH_SHORT).show();
                }
            }
        });

        disconnectButton= (Button)findViewById(R.id.btn_disconnect);
        disconnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (BluetoothMsg.serviceOrCilent == BluetoothMsg.ServerOrCilent.CILENT){
                    if(null==client)
                        return;
                    client.closeBTClient();
                }
                else if (BluetoothMsg.serviceOrCilent == BluetoothMsg.ServerOrCilent.SERVICE) {
                    if(null==server)
                        return;
                    server.closeBTServer();
                }
                BluetoothMsg.isOpen = false;
                BluetoothMsg.serviceOrCilent=BluetoothMsg.ServerOrCilent.NONE;
                Toast.makeText(getApplicationContext(), "已断开连接！", Toast.LENGTH_SHORT).show();
            }
        });*/

        mSelectDevices = (Button) findViewById(R.id.select_device);
        mSelectDevices.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {
                BluetoothMsg.serviceOrCilent = BluetoothMsg.ServerOrCilent.CILENT;
                Intent it=new Intent(getApplicationContext(),BTDeviceActivity.class);
                startActivityForResult(it, 100);
            }
        });

        mSave = (Button) findViewById(R.id.save);
        mSave.setText(write ? "停止保存" : "保存数据");
        mSave.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {
                write = !write;
                mSave.setText(write ? "停止保存" : "保存数据");
                if(write){
                    Utils.upDateFileName();
                    Toast.makeText(BTChatActivity.this, "保存至" + Utils.getFile(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (BluetoothMsg.isOpen) {
            Toast.makeText(getApplicationContext(), "连接已经打开，可以通信。如果要再建立连接，请先断开！",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==100){
            //从设备列表返回
            initConnecter();
        }
    }

    private void initConnecter(){
        if (BluetoothMsg.serviceOrCilent == BluetoothMsg.ServerOrCilent.CILENT) {
            String address = BluetoothMsg.BlueToothAddress;
            if (!TextUtils.isEmpty(address)) {
                if(null==client) {
                    client = new BTClient(BTManage.getInstance().getBtAdapter(), detectedHandler);
                }
                client.connectBTServer(address);
//                startServer(address);
                BluetoothMsg.isOpen = true;
            } else {
                Toast.makeText(getApplicationContext(), "address is empty please choose server address !",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (BluetoothMsg.serviceOrCilent == BluetoothMsg.ServerOrCilent.SERVICE) {
            if(null==server) {
                server = new BTServer(BTManage.getInstance().getBtAdapter(), detectedHandler);
            }
            server.startBTServer();
//            startServer();


            BluetoothMsg.isOpen = true;
        }
    }

    private void startServer(String address){
        Intent it = new Intent(BTChatActivity.this, BTUpdataServer.class);
        it.putExtra("address",address);
        BTChatActivity.this.startService(it);
    }

    class MessageAdapter extends BaseAdapter{
        private Context mContext;
        private LayoutInflater mInflater = null;
        private List<String> list;

        public MessageAdapter(Context context, List<String> lis) {
            this.mContext = context;
            this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.list = lis;
        }

        @Override
        public int getCount() {
            return list == null ? 0 : list.size();
        }

        @Override
        public Object getItem(int position) {
            if(list == null){
                return null;
            }
            return position > list.size() ? null : list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder=null;

            if(convertView==null){
                holder=new ViewHolder();
                convertView = mInflater.inflate(R.layout.messag_item, null);
                holder.tv=(TextView)convertView.findViewById(R.id.textView);
                convertView.setTag(holder);
            }else{
                holder=(ViewHolder)convertView.getTag();
            }
            holder.tv.setText(list.get(position));
            return convertView;
        }

        class ViewHolder{
            TextView tv;
        }
    }

}
