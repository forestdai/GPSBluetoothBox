package com.gccode.wnlbs.gpsbluetoothbox.utils;

import android.bluetooth.BluetoothDevice;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {
    public static final String PHONE_GPS = "phone_GPS";
    public static final String BOX_GPS = "box_GPS";
    private static String mDate = "";
    public static boolean createBond(Class btClass, BluetoothDevice btDevice)
            throws Exception {
        Method createBondMethod = btClass.getMethod("createBond");
        Log.i("life", "createBondMethod = " + createBondMethod.getName());
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    public static boolean setPin(Class btClass, BluetoothDevice btDevice,
                                 String str) throws Exception {
        Boolean returnValue = null;
        try {
            Method removeBondMethod = btClass.getDeclaredMethod("setPin",
                    new Class[] { byte[].class });
            returnValue = (Boolean) removeBondMethod.invoke(btDevice,
                    new Object[] { str.getBytes() });
            Log.i("life", "returnValue = " + returnValue);
        } catch (SecurityException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return returnValue;
    }

    // 取消用户输入
    public static boolean cancelPairingUserInput(Class btClass,
                                                 BluetoothDevice device) throws Exception {
        Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
        // cancelBondProcess()
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        Log.i("life", "cancelPairingUserInputreturnValue = " + returnValue);
        return returnValue.booleanValue();
    }

    public static void upDateFileName(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA);
        mDate = simpleDateFormat.format(new Date());
    }


    public static String getFile(String name){
        File sdDir = null;

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED))
            sdDir = Environment.getExternalStorageDirectory();

        File cacheDir = new File(sdDir + File.separator + "NMEA LOG");
        if (!cacheDir.exists())
            cacheDir.mkdir();
        if(mDate.equals("")){
            upDateFileName();
        }
        File filePath = new File(cacheDir + File.separator + name + mDate + ".txt");

        return filePath.toString();
    }

    public static String getFile(){
        File sdDir = null;
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)){
            sdDir = Environment.getExternalStorageDirectory();
            return sdDir + File.separator + "NMEA LOG";

        }
        return "";
    }

    /**
     * 保存到日志文件
     * @param content
     */
    public static synchronized void write(String content, String name){
        try
        {
            FileWriter writer = new FileWriter(getFile(name), true);
            writer.write(content);
            writer.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
