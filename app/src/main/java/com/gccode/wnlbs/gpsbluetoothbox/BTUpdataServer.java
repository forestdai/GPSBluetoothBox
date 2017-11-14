package com.gccode.wnlbs.gpsbluetoothbox;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;


import com.gccode.wnlbs.gpsbluetoothbox.utils.ThreadPool;
import com.gccode.wnlbs.gpsbluetoothbox.utils.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.UUID;

public class BTUpdataServer extends Service implements LocationListener {
    private BluetoothServerSocket btServerSocket = null;
    private BluetoothSocket btsocket = null;
    private BluetoothDevice btdevice = null;
    private BufferedInputStream bis = null;
    private BufferedOutputStream bos = null;
    private String address;

    private String mMockProviderName = LocationManager.GPS_PROVIDER;
    private LocationManager mLocationManager;

    private String mMsg = "";
    private String mSend = "";

    private String[] gga = new String[]{};
    private String[] rmc = new String[]{};

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        address = intent.getStringExtra("address");
        btdevice = BTManage.getInstance().getBtAdapter().getRemoteDevice(address);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.addTestProvider(mMockProviderName, false, true, false,
                false, true, true, true, 0, 5);
        mLocationManager.setTestProviderEnabled(mMockProviderName, true);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        mLocationManager.requestLocationUpdates(mMockProviderName, 0, 0, this);
        startBTServer();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void startBTServer() {
        ThreadPool.getInstance().excuteTask(new Runnable() {
            public void run() {
                try {
                    btsocket = btdevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

//                    Message msg = new Message();
//                    msg.obj = "请稍候，正在等待客户端的连接...";
//                    msg.what = 0;
//                    mBTSerializable.getHandler().sendMessage(msg);
//
                    btsocket.connect();
//                    Message msg2 = new Message();
//                    String info = "客户端已经连接上！可以发送信息。";
//                    msg2.obj = info;
//                    msg.what = 0;
//                    mBTSerializable.getHandler().sendMessage(msg2);

                    receiverMessageTask();
                } catch (EOFException e) {
//                    Message msg = new Message();
//                    msg.obj = "client has close!";
//                    msg.what = 1;
//                    mBTSerializable.getHandler().sendMessage(msg);
                    e.printStackTrace();
                } catch (IOException e) {
//                    e.printStackTrace();
//                    Message msg = new Message();
//                    msg.obj = "receiver message error! please make client try again connect!";
//                    msg.what = 1;
//                    mBTSerializable.getHandler().sendMessage(msg);
                    e.printStackTrace();
                }
            }
        });
    }

    private void receiverMessageTask() {
        ThreadPool.getInstance().excuteTask(new Runnable() {
            public void run() {
                byte[] buffer = new byte[2048];
                int totalRead;
                /*InputStream input = null;
                OutputStream output=null;*/
                try {
                    bis = new BufferedInputStream(btsocket.getInputStream());
                    bos = new BufferedOutputStream(btsocket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    //  ByteArrayOutputStream arrayOutput=null;
                    while ((totalRead = bis.read(buffer)) > 0) {
                        //       arrayOutput=new ByteArrayOutputStream();
                        String txt = new String(buffer, 0, totalRead, "UTF-8");
                        mMsg += txt;
                        if (mMsg.indexOf("\r\n") > 0) {
                            subMessage();
                        }
//                        if(mMsg.length() > 500){
//                            Log.e("","dai = > Msg:"+ mMsg);
//                            decodeNMEA(mMsg.getBytes(), mMsg.length());
//                            mMsg = "";
//                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void subMessage() {
        String newstr = mMsg.substring(0, mMsg.indexOf("\r\n"));
        if (mMsg.length() >= mMsg.indexOf("\r\n") + 2) {
            mMsg = mMsg.substring(mMsg.indexOf("\r\n") + 2, mMsg.length());
        }

        mSend += newstr;
        Log.e("", "dai = > mSend:" + mSend);
        decodeNMEA(mSend);
//        decodeNMEA(mSend.getBytes(), mSend.length());
        mSend = "";

        if (mMsg.indexOf("\r\n") > 0) {
            subMessage();
        }
    }

    private void decodeNMEA(String nmea) {
        /*
        $GPGGA
        字段0：$GPGGA，语句ID，表明该语句为Global Positioning System Fix Data（GGA）GPS定位信息
        字段1：UTC 时间，hhmmss.sss，时分秒格式
        字段2：纬度ddmm.mmmm，度分格式（前导位数不足则补0）
        字段3：纬度N（北纬）或S（南纬）
        字段4：经度dddmm.mmmm，度分格式（前导位数不足则补0）
        字段5：经度E（东经）或W（西经）
        字段6：GPS状态，0=未定位，1=非差分定位，2=差分定位，3=无效PPS，6=正在估算
        字段7：正在使用的卫星数量（00 - 12）（前导位数不足则补0）
        字段8：HDOP水平精度因子（0.5 - 99.9）
        字段9：海拔高度（-9999.9 - 99999.9）
        字段10：地球椭球面相对大地水准面的高度
        字段11：差分时间（从最近一次接收到差分信号开始的秒数，如果不是差分定位将为空）
        字段12：差分站ID号0000 - 1023（前导位数不足则补0，如果不是差分定位将为空）
        字段13：校验值
        */
        if (nmea.indexOf("$GNGGA", 0) == 0) {
            gga = nmea.split(",");
        }
        /* * $GPRMC
        字段0：$GPRMC，语句ID，表明该语句为Recommended Minimum Specific GPS/TRANSIT Data（RMC）推荐最小定位信息
        字段1：UTC时间，hhmmss.sss格式
        字段2：状态，A=定位，V=未定位
        字段3：纬度ddmm.mmmm，度分格式（前导位数不足则补0）
        字段4：纬度N（北纬）或S（南纬）
        字段5：经度dddmm.mmmm，度分格式（前导位数不足则补0）
        字段6：经度E（东经）或W（西经）
        字段7：速度，节，Knots
        字段8：方位角，度
        字段9：UTC日期，DDMMYY格式
        字段10：磁偏角，（000 - 180）度（前导位数不足则补0）
        字段11：磁偏角方向，E=东W=西
        字段16：校验值*/
        if (nmea.indexOf("$GNRMC", 0) == 0) {
            rmc = nmea.split(",");
        }

        if (gga.length > 1 && rmc.length > 1) {
            Location location = new Location(mMockProviderName);
            location.setLatitude(gga[2].equals("") ? 0.0 : Double.valueOf(gga[2]));//纬度
            location.setLongitude(gga[4].equals("") ? 0.0 : Double.valueOf(gga[4]));//经度
            location.setAltitude(gga[9].equals("") ? 0.0 : Double.valueOf(gga[9]));//高度
            location.setBearing(rmc[8].equals("") ? 0.0f : Float.valueOf(rmc[8]));//方向
            location.setSpeed(rmc[7].equals("") ? 0.0f : Float.valueOf(rmc[7]));//速度
            location.setAccuracy(10.0f);//精度
            location.setTime(System.currentTimeMillis());//时间
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

            mLocationManager.setTestProviderLocation(mMockProviderName,
                    location);
            gga = new String[]{};
            rmc = new String[]{};
        }
    }

    private void simulateLoca() {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("GPS-NMEA", "dai = > nmea:"+provider);

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

//    private Location getLoc(String provider)
//    {
//        Location location = new Location(provider);
//        location.setLatitude(lat);
//        location.setLongitude(lng);
//        location.setAltitude(altitude);
//        location.setBearing(bearing);
//        location.setSpeed(speed);
//        location.setAccuracy(accuracy);
//        location.setTime(System.currentTimeMillis());
//        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
//        return location;
//    }

//    private native static void decodeNMEA(byte[] data, int len);
////    private native static void initJNI();
//
//    static {
//        System.loadLibrary("native-lib");
//    }
}
