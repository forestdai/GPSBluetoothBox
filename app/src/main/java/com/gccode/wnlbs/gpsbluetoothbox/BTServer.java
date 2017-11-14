package com.gccode.wnlbs.gpsbluetoothbox;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;


import com.gccode.wnlbs.gpsbluetoothbox.utils.ThreadPool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.UUID;

public class BTServer {

    /* 一些常量，代表服务器的名称 */
    public static final String PROTOCOL_SCHEME_L2CAP = "btl2cap";
    public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";
    public static final String PROTOCOL_SCHEME_BT_OBEX = "btgoep";
    public static final String PROTOCOL_SCHEME_TCP_OBEX = "tcpobex";

    private BluetoothServerSocket btServerSocket = null;
    private BluetoothSocket btsocket = null;
    private BluetoothAdapter mBtAdapter =null;
    private BufferedInputStream bis=null;
    private BufferedOutputStream bos=null;

    private Handler detectedHandler=null;

    public BTServer(BluetoothAdapter mBtAdapter,Handler detectedHandler){
        this.mBtAdapter=mBtAdapter;
        this.detectedHandler=detectedHandler;
    }

    public void startBTServer() {
        ThreadPool.getInstance().excuteTask(new Runnable() {
            public void run() {
                try {
                    btServerSocket = mBtAdapter.listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM,
                            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                    Message msg = new Message();
                    msg.obj = "请稍候，正在等待客户端的连接...";
                    msg.what = 0;
                    detectedHandler.sendMessage(msg);

                    btsocket=btServerSocket.accept();
                    Message msg2 = new Message();
                    String info = "客户端已经连接上！可以发送信息。";
                    msg2.obj = info;
                    msg.what = 0;
                    detectedHandler.sendMessage(msg2);

                    receiverMessageTask();
                } catch(EOFException e){
                    Message msg = new Message();
                    msg.obj = "client has close!";
                    msg.what = 1;
                    detectedHandler.sendMessage(msg);
                }catch (IOException e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.obj = "receiver message error! please make client try again connect!";
                    msg.what = 1;
                    detectedHandler.sendMessage(msg);
                }
            }
        });
    }

    private void receiverMessageTask(){
        ThreadPool.getInstance().excuteTask(new Runnable() {
            public void run() {
                byte[] buffer = new byte[2048];
                int totalRead;
                /*InputStream input = null;
                OutputStream output=null;*/
                try {
                    bis=new BufferedInputStream(btsocket.getInputStream());
                    bos=new BufferedOutputStream(btsocket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    //  ByteArrayOutputStream arrayOutput=null;
                    while((totalRead = bis.read(buffer)) > 0 ){
                        //       arrayOutput=new ByteArrayOutputStream();
                        String txt = new String(buffer, 0, totalRead, "UTF-8");
                        Message msg = new Message();
                        msg.obj = txt;
                        msg.what = 1;
                        detectedHandler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean sendmsg(String msg){
        boolean result=false;
        if(null==btsocket||bos==null)
            return false;
        try {
            bos.write(msg.getBytes());
            bos.flush();
            result=true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void closeBTServer(){
        try{
            if(bis!=null)
                bis.close();
            if(bos!=null)
                bos.close();
            if(btServerSocket!=null)
                btServerSocket.close();
            if(btsocket!=null)
                btsocket.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

}
