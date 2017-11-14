package com.olinkstar.bdsviewin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Handler;
import android.util.Log;
import com.olinkstar.util.Buffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

@SuppressLint({"NewApi"})
public class NordicBleManager
{
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothScanner;
    private Activity mActivity;
    private boolean isBleAvailable = false;
    private int scanDevicesStatus = 16;
    private BluetoothGatt mGatt = null;
    private int gattStatus = 32;
    private MScanCallback scanCallback;
    private GattCallback mCallback = null;
    private BluetoothGattCharacteristic mDFUPacketCharacteristic;
    private BluetoothGattCharacteristic mDFUControlPointCharacteristic;
    private int deviceMode = 0;
    private int updateStep = 0;
    private int sendPackStep = 0;
    private File updateFile;
    private byte[] updateFileBuffer;
    private int sendGap;
    private int updateState = 0;

    private final String ONREAD_BUFFER = "ONREADBUFFER";
    int gl_SuccessCnt = 0;
    int gl_FailCnt = 0;
    int gl_SuccessCntCall = 0;

    private final UUID DFU_SERVICE_UUID = UUID.fromString("00001530-1212-efde-1523-785feabcd123");

    private final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");

    private final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final UUID DFU_CONTROLPOINT_CHARACTERISTIC_UUID = UUID.fromString("00001531-1212-efde-1523-785feabcd123");

    private final UUID DFU_PACKET_CHARACTERISTIC_UUID = UUID.fromString("00001532-1212-efde-1523-785feabcd123");

    private final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    private final int STEP_START_DFU = 1;
    private final int STEP_WRITE_FILE_SIZE = 2;
    private final int STEP_ENABLED_PACKET_1st = 3;
    private final int STEP_SEND_CRC = 4;
    private final int STEP_ENABLED_PACKET_2nd = 5;
    private final int STEP_WRITE_FIRMWARE = 6;
    private final int STEP_ENABLED_PACKET_3rd = 7;
    private final int STEP_SEND_PACKET = 8;
    private final int STEP_SEND_COMPLETE = -1;
    private final int STEP_ENABLED_PACKET_4th = 9;
    private final int STEP_ENABLED_PACKET_5th = 10;

    private final byte[] CODE_NOTIFICATION_1 = { 16, 1, 1 };
    private final byte[] CODE_NOTIFICATION_2 = { 16, 2, 1 };
    private final byte[] CODE_NOTIFICATION_3 = { 16, 3, 1 };
    private final byte[] CODE_NOTIFICATION_4 = { 16, 4, 1 };

    private final byte[] CODE_START_DFU = { 1, 4 };
    private final byte[] CODE_ENABLED_PACKET_1st = { 2 };
    private final byte[] CODE_ENABLED_PACKET_2nd = { 2, 1 };
    private final byte[] CODE_ENABLED_PACKET_3rd = { 3 };
    private final byte[] CODE_ENABLED_PACKET_4th = { 4 };
    private final byte[] CODE_ENABLED_PACKET_5th = { 5 };

    private final boolean OPEN_ANDROID_L = 1;
    private static boolean bWrtieSuccess = true;

    public final int WRITE_OPERRATION_SUCCESS = 0;
    public final int WRITE_OPERRATION_FAILED = 1;
    public final int WRITE_RESULT_SUCCESS = 2;
    public final int WRITE_RESULT_FAILED = 3;
    private final String BLEBLEW = "BLEW";

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            NordicBleManager.this.scanCallback.onLeScan(device, rssi, scanRecord);
        }
    };

    private Object newScanCallback = (Build.VERSION.SDK_INT > 20) ?
            new ScanCallback()
            {
                public void onScanResult(int callbackType, ScanResult result) {
                    byte[] scanRecord = result.getScanRecord().getBytes();
                    NordicBleManager.this.scanCallback.onLeScan(result.getDevice(), result.getRssi(), scanRecord);
                }
            }
            :
            null;

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            if (newState == 2) {
                NordicBleManager.this.gattStatus = 34;
                gatt.discoverServices();
            }
            else if (newState == 0) {
                NordicBleManager.this.gattStatus = 32;
                NordicBleManager.this.mCallback.onDisconnected();
            }
            else
            {
                NordicBleManager.this.gattStatus = 35;
                NordicBleManager.this.mCallback.onConnectionError();
            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            if (status == 0) {
                List services = gatt.getServices();
                boolean isDFUServiceFound = false;
                for (BluetoothGattService service : services) {
                    if (service.getUuid().equals(NordicBleManager.this.DFU_SERVICE_UUID)) {
                        NordicBleManager.this.mDFUControlPointCharacteristic = service.getCharacteristic(
                                NordicBleManager.this.DFU_CONTROLPOINT_CHARACTERISTIC_UUID);
                        NordicBleManager.this.mDFUPacketCharacteristic = service.getCharacteristic(
                                NordicBleManager.this.DFU_PACKET_CHARACTERISTIC_UUID);
                        isDFUServiceFound = true;
                    }
                }

                if (isDFUServiceFound)
                {
                    NordicBleManager.this.mGatt.setCharacteristicNotification(NordicBleManager.this.mDFUControlPointCharacteristic, true);
                    BluetoothGattDescriptor descriptor = NordicBleManager.this.mDFUControlPointCharacteristic.getDescriptor(
                            NordicBleManager.this.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    NordicBleManager.this.mGatt.writeDescriptor(descriptor);

                    NordicBleManager.this.deviceMode = 48;
                }
                else {
                    BluetoothGattService RxService = NordicBleManager.this.mGatt.getService(NordicBleManager.this.RX_SERVICE_UUID);
                    BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(NordicBleManager.this.TX_CHAR_UUID);
                    NordicBleManager.this.mGatt.setCharacteristicNotification(TxChar, true);

                    BluetoothGattDescriptor descriptor = TxChar.getDescriptor(
                            NordicBleManager.this.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    NordicBleManager.this.mGatt.writeDescriptor(descriptor);

                    NordicBleManager.this.deviceMode = 49;
                }

                NordicBleManager.this.mCallback.onConnected(NordicBleManager.this.deviceMode);
            }
        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            byte[] readBuffer = characteristic.getValue();

            if (NordicBleManager.this.deviceMode == 49)
            {
                synchronized ("ONREADBUFFER")
                {
                    NordicBleManager.this.mCallback.onRead(readBuffer);
                }

            }
            else if (NordicBleManager.this.deviceMode == 48)
                NordicBleManager.this.onDFURead(readBuffer);
        }

        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
        {
            if ((status != 0) ||
                    (NordicBleManager.this.mCallback == null)) return;
            NordicBleManager.this.mCallback.onRSSI(rssi, status);
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            if (status == 0) {
                NordicBleManager.bWrtieSuccess = true;
                if (NordicBleManager.this.deviceMode == 49) {
                    NordicBleManager.bWrtieSuccess = true;
                    NordicBleManager.this.mCallback.onWrite(NordicBleManager.bWrtieSuccess);
                    NordicBleManager.this.gl_SuccessCntCall += 1;
                    Log.v("BLETx", "BLETx nStatus onCharacteristicWrite Callback= " + status);
                }
                else if (NordicBleManager.this.deviceMode == 48) {
                    NordicBleManager.this.onDFUWrite();
                }

            }
            else if (NordicBleManager.this.deviceMode == 49) {
                NordicBleManager.this.mCallback.onWrite(false);
            }
        }
    };

    public boolean IsWriteReady()
    {
        return bWrtieSuccess; }

    public void SetWriteReady() {
        bWrtieSuccess = true;
    }

    public NordicBleManager(Activity atv) {
        this.bluetoothManager = ((BluetoothManager)atv.getSystemService("bluetooth"));
        this.mActivity = atv;
    }

    public boolean ReadRssi() {
        if (this.mGatt != null)
        {
            return this.mGatt.readRemoteRssi();
        }
        return false;
    }

    public int bleInit()
    {
        this.bluetoothAdapter = this.bluetoothManager.getAdapter();
        if (Build.VERSION.SDK_INT > 20) this.bluetoothScanner = this.bluetoothAdapter.getBluetoothLeScanner();

        if (this.bluetoothAdapter == null) {
            return 1;
        }

        if (!(this.mActivity.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le"))) {
            return 2;
        }

        if (!(this.bluetoothAdapter.isEnabled())) {
            return 3;
        }

        this.isBleAvailable = true;
        return 4;
    }

    public void startScanDevices(long period, MScanCallback callback)
            throws NordicBleManager.UninitializeException
    {
        if (!(this.isBleAvailable)) throw new NordicBleManager.UninitializeException(this, "default");
        this.scanDevicesStatus = 17;
        this.scanCallback = callback;
        Handler mHandler = new Handler();

        mHandler.postDelayed(new Runnable()
                             {
                                 public void run()
                                 {
                                     NordicBleManager.this.stopScanDevices();
                                 }

                             }

                , period);

        if (Build.VERSION.SDK_INT > 20)
            this.bluetoothScanner.startScan((ScanCallback)this.newScanCallback);
        else {
            this.bluetoothAdapter.startLeScan(this.leScanCallback);
        }

        this.scanCallback.onStartScan();
    }

    public void stopScanDevices()
    {
        if (this.scanDevicesStatus == 17) {
            this.scanDevicesStatus = 16;

            if (Build.VERSION.SDK_INT > 20)
                this.bluetoothScanner.stopScan((ScanCallback)this.newScanCallback);
            else {
                this.bluetoothAdapter.stopLeScan(this.leScanCallback);
            }

            this.scanCallback.onStopScan();
        }
    }

    public int getScanDevicesStatus()
    {
        return this.scanDevicesStatus;
    }

    public void connectDevice(BluetoothDevice device, long timeOut, boolean autoConnect, GattCallback callback)
            throws NordicBleManager.NullDeviceException, NordicBleManager.AlreadyConnectException
    {
        if ((device != null) && (this.gattStatus == 32)) {
            if (this.mGatt != null) this.mGatt.close();
            this.gattStatus = 33;
            this.mCallback = callback;
            this.mGatt = device.connectGatt(this.mActivity, false, this.mGattCallback);

            Timer timer = new Timer();
            timer.schedule(new TimerTask()
                           {
                               public void run() {
                                   if (NordicBleManager.this.gattStatus == 33) {
                                       NordicBleManager.this.mGatt.disconnect();
                                       NordicBleManager.this.mCallback.onTimeOut();
                                       NordicBleManager.this.gattStatus = 32;
                                   }
                               }
                           }
                    , timeOut);
        }
        else {
            if (device == null)
                throw new NordicBleManager.NullDeviceException(this, "default");
            if (this.gattStatus != 32)
                throw new NordicBleManager.AlreadyConnectException(this, "default");
        }
    }

    public void disconnect()
    {
        if (this.gattStatus == 34)
            this.mGatt.disconnect();
    }

    public int getConnectStatus()
    {
        return this.gattStatus;
    }

    public int write(byte[] buffer)
    {
        if ((buffer != null) && (this.mGatt != null)) {
            bWrtieSuccess = false;
            BluetoothGattService RxService = this.mGatt.getService(this.RX_SERVICE_UUID);
            if (RxService == null) {
                Log.v("BLETx", "BLETx nStatus onCharacteristicWrite AAAA");
                bWrtieSuccess = true;
                this.gl_FailCnt += 1;
                return 1;
            }
            BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(this.RX_CHAR_UUID);
            if (RxChar == null) {
                Log.v("BLETx", "BLETx nStatus onCharacteristicWrite BBB");
                bWrtieSuccess = true;
                this.gl_FailCnt += 1;
                return 1;
            }

            RxChar.setValue(buffer);
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            if (this.mGatt.writeCharacteristic(RxChar))
            {
                Log.v("BLETx", "BLETx nStatus onCharacteristicWrite TxCnt=" + this.gl_SuccessCnt + "--" + this.gl_SuccessCntCall + "--" + this.gl_FailCnt);
                this.gl_SuccessCnt += 1;
                return 0;
            }

            Log.v("BLETx", "BLETx nStatus onCharacteristicWrite 222");
            bWrtieSuccess = true;
            this.gl_FailCnt += 1;

            return 1;
        }

        Log.v("BLETx", "BLETx nStatus onCharacteristicWrite 333");
        bWrtieSuccess = true;
        this.gl_FailCnt += 1;
        return 1;
    }

    public void update(File dir, String name, int gap)
            throws FileNotFoundException
    {
        if (this.deviceMode == 48) {
            this.updateFile = new File(dir, name);
            if ((this.updateFile.exists()) && (this.updateFile.isFile())) {
                this.sendGap = gap;
                startDFU();
            }
            else {
                throw new FileNotFoundException("升级文件未找到");
            }
        }
    }

    public int getUpdateState()
    {
        return this.updateState;
    }

    public void exit()
    {
        if (this.mGatt != null) {
            this.mGatt.disconnect();
            this.mGatt.close();
        }
    }

    private void onDFURead(byte[] buffer)
    {
        switch (this.updateStep)
        {
            case 2:
                if (!(Arrays.equals(buffer, this.CODE_NOTIFICATION_1))) return;
                enablePacketNotification();

                break;
            case 5:
                if (!(Arrays.equals(buffer, this.CODE_NOTIFICATION_2))) return;
                writeFirmwareImage();

                break;
            case 8:
                if (!(Arrays.equals(buffer, this.CODE_NOTIFICATION_3))) return;
                enablePacketNotification4();

                break;
            case 9:
                if (!(Arrays.equals(buffer, this.CODE_NOTIFICATION_4))) return;
                enablePacketNotification5();
            case 3:
            case 4:
            case 6:
            case 7:
        }
    }

    private void onDFUWrite()
    {
        switch (this.updateStep)
        {
            case 1:
                writeFileSize();
                break;
            case 3:
                sendCRC();
                break;
            case 4:
                enablePacketNotification2();
                break;
            case 6:
                Timer tt = new Timer();
                tt.schedule(new TimerTask()
                            {
                                public void run() {
                                    NordicBleManager.this.enablePacketNotification3();
                                }
                            }
                        , 500L);
                break;
            case 7:
                sendPacket();
                break;
            case 8:
                if (this.sendPackStep != -1) {
                    sendPacket();
                    return;
                }
                Log.v("--tt--", "successfully write file");
                this.sendPackStep = 0;
            case 2:
            case 5:
        }
    }

    private void startDFU()
    {
        this.mDFUControlPointCharacteristic.setValue(this.CODE_START_DFU);
        this.mGatt.writeCharacteristic(this.mDFUControlPointCharacteristic);
        this.updateStep = 1;
        Log.v("DFUUpdate", "第1步：开启DFU升级模块完成");
        this.updateState = 65;
    }

    private void writeFileSize() {
        int fileSize = (int)this.updateFile.length();
        byte[] buf = new byte[12];
        byte[] fileSize_byte = Buffer.intToByteArray(fileSize);
        buf[8] = fileSize_byte[3];
        buf[9] = fileSize_byte[2];
        buf[10] = fileSize_byte[1];
        buf[11] = fileSize_byte[0];
        this.mDFUPacketCharacteristic.setWriteType(1);
        this.mDFUPacketCharacteristic.setValue(buf);
        this.mGatt.writeCharacteristic(this.mDFUPacketCharacteristic);
        this.updateStep = 2;
        Log.v("DFUUpdate", "第2步：发送升级文件长度完成");
        this.updateState = 66;
    }

    private void enablePacketNotification() {
        this.mDFUControlPointCharacteristic.setValue(this.CODE_ENABLED_PACKET_1st);
        this.mGatt.writeCharacteristic(this.mDFUControlPointCharacteristic);
        this.updateStep = 3;
        Log.v("DFUUpdate", "第3步：发送首个控制指令{0x02,0x00}完成");
        this.updateState = 67;
    }

    private void sendCRC()
    {
        byte[] buf = { 1, 0, 1,
                0, -1,
                0, 0, 0, 1, 0, -2, -1 };
        try
        {
            FileInputStream is = new FileInputStream(this.updateFile);
            this.updateFileBuffer = new byte[(int)this.updateFile.length()];
            this.updateStep = 4;
            while (is.read(this.updateFileBuffer) != -1)
            {
                int crc = Buffer.crc16(this.updateFileBuffer);

                byte[] crc_byte = Buffer.intToByteArray(crc);
                buf[12] = crc_byte[3];
                buf[13] = crc_byte[2];

                this.mDFUPacketCharacteristic.setValue(buf);
                this.mGatt.writeCharacteristic(this.mDFUPacketCharacteristic);
                Log.v("DFUUpdate", "第4步：发送校验和完成");
                this.updateState = 68;
            }

            is.close();
        } catch (Exception e) {
            Log.v("DFUUpdate", "第4步：发送校验和失败");
            e.printStackTrace();
        }
    }

    private void enablePacketNotification2() {
        this.mDFUControlPointCharacteristic.setValue(this.CODE_ENABLED_PACKET_2nd);
        this.mGatt.writeCharacteristic(this.mDFUControlPointCharacteristic);
        this.updateStep = 5;
        Log.v("DFUUpdate", "第5步：发送第二个控制指令{0x02,0x01}完成");
        this.updateState = 69;
    }

    private void writeFirmwareImage()
    {
        if (this.sendGap != 0) {
            byte[] buf = { 8, (byte)this.sendGap };
            this.mDFUControlPointCharacteristic.setValue(buf);
            this.mGatt.writeCharacteristic(this.mDFUControlPointCharacteristic);
        }
        this.updateStep = 6;
        Log.v("DFUUpdate", "第6步：发送文件前设置完成");
        this.updateState = 70;
    }

    private void enablePacketNotification3()
    {
        this.mDFUControlPointCharacteristic.setValue(this.CODE_ENABLED_PACKET_3rd);
        this.mGatt.writeCharacteristic(this.mDFUControlPointCharacteristic);
        this.updateStep = 7;
        Log.v("DFUUpdate", "第7步：发送第三个控制指令{0x03}完成");
        this.updateState = 71;
    }

    private void sendPacket()
    {
        this.updateStep = 8;

        if (this.updateFileBuffer.length - (20 * this.sendPackStep) <= 20) {
            byte[] lastBuf = new byte[this.updateFileBuffer.length - (20 * this.sendPackStep)];
            System.arraycopy(this.updateFileBuffer, 20 * this.sendPackStep, lastBuf, 0, lastBuf.length);
            this.sendPackStep = -1;
            this.mDFUPacketCharacteristic.setValue(lastBuf);
            this.mGatt.writeCharacteristic(this.mDFUPacketCharacteristic);
            Log.v("DFUUpdate", "正在发送最后一个包，  size: " + lastBuf.length);
            Buffer.printHexString(lastBuf);
            Log.v("DFUUpdate", "第8步：发送升级文件完成");
            this.updateState = 72;
        }
        else {
            byte[] buf = new byte[20];
            System.arraycopy(this.updateFileBuffer, 20 * this.sendPackStep, buf, 0, 20);
            this.sendPackStep += 1;
            this.mDFUPacketCharacteristic.setValue(buf);
            this.mGatt.writeCharacteristic(this.mDFUPacketCharacteristic);
            Log.v("DFUUpdate", "正在发送第" + this.sendPackStep + "个包， 大小: " + buf.length);
            Buffer.printHexString(buf);
        }
    }

    private void enablePacketNotification4()
    {
        this.mDFUControlPointCharacteristic.setValue(this.CODE_ENABLED_PACKET_4th);
        this.mGatt.writeCharacteristic(this.mDFUControlPointCharacteristic);
        this.updateStep = 9;
        Log.v("DFUUpdate", "第9步：发送第四个控制指令{0x04}完成");
        this.updateState = 73;
    }

    private void enablePacketNotification5()
    {
        this.mDFUControlPointCharacteristic.setValue(this.CODE_ENABLED_PACKET_5th);
        this.mGatt.writeCharacteristic(this.mDFUControlPointCharacteristic);
        this.updateStep = 10;
        Log.v("DFUUpdate", "最后一步：发送第五个控制指令{0x05}完成，即将重新启动蓝牙模块");
        this.updateState = 74;
    }

    public class AlreadyConnectException extends Exception
    {
        private static final long serialVersionUID = -4500603668819521086L;

        public AlreadyConnectException(String paramString)
        {
            super("BLE正在连接或已连接");
        }
    }

    public class NullDeviceException extends Exception
    {
        private static final long serialVersionUID = 4268633602990389229L;

        public NullDeviceException(String paramString)
        {
            super("指定的Device不存在");
        }
    }

    public class State
    {
        public static final int STATE_BLUETOOTH_NOT_FOUND = 1;
        public static final int STATE_BLE_UNSUPPORTED = 2;
        public static final int STATE_BLUETOOTH_NOT_OPEN = 3;
        public static final int STATE_BLE_INIT_FINISHED = 4;
        public static final int STATE_BLE_SCAN_STOP = 16;
        public static final int STATE_BLE_SCAN_START = 17;
        public static final int STATE_GATT_DISCONNECTED = 32;
        public static final int STATE_GATT_CONNECTING = 33;
        public static final int STATE_GATT_CONNECTED = 34;
        public static final int STATE_GATT_ERROR = 35;
        public static final int STATE_SERVICE_DFU = 48;
        public static final int STATE_SERVICE_UART = 49;
        public static final int STATE_UPDATE_1 = 65;
        public static final int STATE_UPDATE_2 = 66;
        public static final int STATE_UPDATE_3 = 67;
        public static final int STATE_UPDATE_4 = 68;
        public static final int STATE_UPDATE_5 = 69;
        public static final int STATE_UPDATE_6 = 70;
        public static final int STATE_UPDATE_7 = 71;
        public static final int STATE_UPDATE_8 = 72;
        public static final int STATE_UPDATE_9 = 73;
        public static final int STATE_UPDATE_10 = 74;
    }

    public class UninitializeException extends Exception
    {
        private static final long serialVersionUID = 7113761196274092208L;

        public UninitializeException(String paramString)
        {
            super("NordicBleManager未进行初始化");
        }
    }
}