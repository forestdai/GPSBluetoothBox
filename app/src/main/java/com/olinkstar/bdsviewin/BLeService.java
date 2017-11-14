package com.olinkstar.bdsviewin;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.util.Log;

import com.olinkstar.util.LOG;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class BLeService extends Service {
    public double sdkAccuracy = -1.0D;

    public LocationDataManager locDataManager = LocationDataManager.INSTANSE;
    protected static String currentTime = "";
    private BLeService bleServie;
    protected static boolean flagGPGGA = false;

    protected static boolean flagXXRMC = false;
    protected static boolean[] hasGSV = new boolean[1];

    public NmeaAnalyzer nmeaAnalyzer = new NmeaAnalyzer(1);
    protected static String nmeaGPGGA = "";
    protected static boolean flagXXGSV = false;
    boolean bRtcmInitAckRec = false;
    private boolean isHead = false;
    private boolean isReturnGot = false;
    private final String PACK_HEAD = "head";
    private final String PACK_FLAG = "pack";
    public final int BLE_RCV_STATE_IDLE = 0;
    public final int BLE_RCV_STATE_BUSY = 1;
    int ble_rcv_state = 0;
    public static NordicBleManager bleManager;
    CPktStatistic m_nmeaCPktStatistic = new CPktStatistic();
    CPktStatistic m_RtcmCPktStatistic = new CPktStatistic();
    private final String BLETX_BUFFER = "BLETXBUFFER";
    private final String BLETX_SPEED = "BLETX_SPEED";
    private final String RTCM_BUFFER = "RTCMBUFFER";
    private final String BLETX_BUFFER_RT = "BLETXBUFFERnew";
    private final String BLETX_BUFFER_ACK = "BLETX_BUFFER_ACK";
    private final int BUUFER_LEN_MAX = 4096;
    private ArrayList<byte[]> m_ListBLETxBuffer = null;
    int gl_ListBLETxBufferRead = 0;
    int gl_ListBLETxBufferWrite = 0;

    BLETxDataRunnalbe m_BLETxDataRunnalbe = new BLETxDataRunnalbe();
    Thread BLETxDataRunnalbeThread = new Thread(this.m_BLETxDataRunnalbe);
    boolean bBLETxDataRunnalbe = true;

    private ArrayList<byte[]> m_ListBLETxBufferAck = null;
    int gl_ListBLETxBufferAckRead = 0;
    int gl_ListBLETxBufferAckWrite = 0;
    RTCMRunnable m_RTCMRunnable = new RTCMRunnable();
    Thread RTCMThread = new Thread(this.m_RTCMRunnable);
    boolean bRtcmRunnable = true;
    public int m_nBleRssi = 0;
    SimpleDateFormat sDateFormatTemp;
    String dateTemp;
    String strTemp;
    public static final String TAG99 = "netBleTest";
    public long fressSize = 0L;
    int rtcm_lost_count = 0;
    public int count_while_all_times = 0;
    private int packReturn = -1;
    private byte[] repairIndex;
    boolean bNmeaTxRunnable = true;
    ACKTxRunnable m_ACKTxRunnable = new ACKTxRunnable();
    Thread ACKTxThread = new Thread(this.m_ACKTxRunnable);
    NmeaTxRunnable m_NmeaTxRunnable = new NmeaTxRunnable();
    Thread NmeaTxRunnableThread = new Thread(this.m_NmeaTxRunnable);
    boolean bACKTxRunnable = true;
    //    final boolean GET_AckTxRequest = 1;
//    final boolean SET_AckTxRequest = 0;
    private final String ACK_BUFFER = "ACKBUFFER";
    private final String ACK_DataBUFFER = "ACK_DataBUFFER";
    boolean gl_bAckTxRequest = false;
    boolean gl_bAckDataTxRequest = false;
    boolean gl_bAckAckRec = false;
    int gl_nPktTmpCnt = 0;
    boolean gl_InitPktFirst = true;
    private int gl_nPktCntBack = 0;
    boolean gl_bDataRec = true;
    boolean bNmeaRunnable = true;
    boolean gl_bOnreadpkt = false;
    int nRepert = 0;
    private boolean bisLastPkt = false;
    long gl_RecPrePktTime = 0L;
    private int gl_nPktReadCnt = 0;
    private int gl_nPktCnt = 0;
    int gl_nDeInitPktCnt = 0;
    public long gl_RevDatatimes = 0L;
    public final long count_Rev_Data_time_max = 3000L;
    int nReTxCnt = 0;
    byte[] reTxN = null;
    private Timer timer;
    private TimerTask task;
    private Timer timerVERSION;
    private TimerTask taskVERSION;
    public static final String TAG5 = "NmeaAnalyzer5";
    public static final String TAG6 = "Command5";
    public static final String TAG77 = "PCMDTTEST";
    public static final String TAG88 = "PCMDTALL";
    public String address;
    public boolean firstConnect = true;
    public boolean isDisBle = false;
    private boolean flagGNVER = false;
    private CopyOnWriteArrayList<byte[]> ListNmeaBack = null;
    NmeaRunnable m_NmeaRunnable = new NmeaRunnable();
    Thread NmeaThread = new Thread(this.m_NmeaRunnable);
    private boolean isManualDis = false;
    public long BleConnTime = -1L;
    public long BleDisConnTime = -1L;
    public int countBLErestart = 0;
    private StringBuilder para = null;
    private boolean flag_para_start = false;
    Timer timerPCMDT;
    TimerTask taskPCMDT;
    private boolean flagPCMDT = false;
    public int devicePower = 0;
    private final String NMEA_BUFFER = "NMEABUFFER";
    private CopyOnWriteArrayList<CopyOnWriteArrayList<byte[]>> ListNmeaBackList = null;
    CopyOnWriteArrayList<CopyOnWriteArrayList<byte[]>> ListBackList = new CopyOnWriteArrayList();

    CopyOnWriteArrayList<byte[]> sRtcmPktBuffer = null;

    public static boolean diagFilesConfigControl = true;
    public static boolean nmeaFileConfigControl = true;
    public static boolean rtcmorgFileConfigControl = true;
    public static boolean selfdefineFileConfigControl = true;

    public String bluetoothName = "";
    public String BoardVersion = "";
    public String BootloaderVersion = "";
    public String BGIVersion = "";
    public String BGIDateTime = "";

    Activity m_Activity = null;

    private GattCallback gattCallback = new GattCallback() {
        public void onDisconnected() {
            com.olinkstar.bdsview.BleManager.m_RunStatus = 32;
            Bundle bundle = new Bundle();
            bundle.putString("BLELink", "LinkDown");
            Intent intent = new Intent("com.ols.broadcast.blelink");
            intent.putExtras(bundle);
            if (m_Activity != null) {
                m_Activity.sendBroadcast(intent);
            }

            Date dt = new Date();
            BleDisConnTime = dt.getTime();

            countBLErestart += 1;

            if (!(isManualDis)) {
                isDisBle = true;
            } else {
                isDisBle = false;
            }
            count_while_all_times = 0;
        }

        public void onConnectionError() {
            LOG.V("bleservice", "--error 杩炴帴閿欒--");
            com.olinkstar.bdsview.BleManager.m_RunStatus = 35;
        }

        public void onTimeOut() {
            LOG.V("NmeaAnalyzer5", "------gattCallback-----onTimeOut------");
            LOG.V("bleservice", "--杩炴帴瓒呮椂--");
            com.olinkstar.bdsview.BleManager.m_RunStatus = 66;
        }

        public void onConnected(int status) {
            String strTemp;
            com.olinkstar.bdsview.BleManager.m_RunStatus = 34;
            Bundle bundle = new Bundle();
            bundle.putString("BLELink", "LinkUp");
            Intent intent = new Intent("com.ols.broadcast.blelink");
            intent.putExtras(bundle);
            if (m_Activity != null) {
                m_Activity.sendBroadcast(intent);
            }

            Date dt = new Date();
            BleConnTime = dt.getTime();

            isManualDis = false;
            timerPCMDT = new Timer();
            taskPCMDT = new TimerTask() {
                public void run() {
                    try {
                        String strPCMDT = getPCMDT();
                        byte[] bufferPCMDT = strPCMDT.getBytes("UTF-8");

                        TxDifDataV2(bufferPCMDT, bufferPCMDT.length);

                        String strTemp = "TAG999,TimePCMDT,1,PCMDT," + strPCMDT;

                        if (flagPCMDT) {
                            timerPCMDT.cancel();
                        }

                        strTemp = "TAG999,TimePCMDT,2,PCMDT," + strPCMDT;
                    } catch (Exception e) {
                        timerPCMDT.cancel();

                        e.printStackTrace();
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw, true));
                        String str = ",catch," + sw.toString();

                        String str1 = "TAG999,TimePCMDT,3,PCMDT,error,";
                    }

                }

            };
            timerPCMDT.schedule(taskPCMDT, 1000L, 1000L);

            isDisBle = false;

            if (status == 49) {
                strTemp = "TAG999,TimeRestartBleService,13,status," + status;

                LOG.V("NmeaAnalyzer5", "------\tdiffThread.start-----------------");
            } else {
                if (status != 48) {
                    return;
                }

                strTemp = "TAG999,TimeRestartBleService,15,status," + status;
            }
        }

        public void onRead(byte[] buffer) {
            if (buffer[0] == 126) {
                if (buffer[2] == 7) {
                    devicePower = new Byte(buffer[3]).intValue();
                } else if (buffer[2] == 1) {
                    if ((ListNmeaBack != null) &&
                            (ListNmeaBack.size() > 0)) {
                        if (gl_nPktReadCnt < gl_nPktCnt) {
                            if (gl_nDeInitPktCnt < 3) {
                                gl_nDeInitPktCnt += 1;
                                return;
                            }
                            CopyOnWriteArrayList List = ListNmeaBack;
                            DealNmeaListData(List, true);
                            AckTxDataRequest(false, true);
                        } else {
                            gl_nDeInitPktCnt = 0;
                        }

                    }

                    gl_bAckAckRec = true;
                    synchronized ("BLETX_SPEED") {
                        ble_rcv_state = 1;
                    }
                    long timePkt = System.currentTimeMillis();
                    String stimePkt = Long.toString(timePkt);

                    String sRet = stimePkt + ",Success nmea InitPkt Rec, ";
                    String ns0 = "";
                    for (int u = 0; u < buffer.length; ++u) {
                        ns0 = ns0 + Integer.toHexString(new Byte(buffer[u]).intValue());
                    }
                    sRet = sRet + ns0 + "\r\n";

                    gl_nPktTmpCnt += 1;

                    if (!(gl_bDataRec)) {
                        gl_nPktTmpCnt -= 1;

                        String u = "\r\nError Nmea data Rec Not \r\n ";
                    }

                    if (!(gl_InitPktFirst))
                        if (gl_bDataRec) {
                            m_nmeaCPktStatistic.SetTime(gl_RecPrePktTime);

                            m_nmeaCPktStatistic.CNTINIPKT();
                            m_nmeaCPktStatistic.CNTWillPKT(gl_nPktCntBack);
                            LOG.V("CNTINIPKT", "CNTINIPKT m_INIPKT 000  " + gl_nPktTmpCnt);
                            m_nmeaCPktStatistic.SaveStatistic(0);
                        } else {
                            m_nmeaCPktStatistic.ClearStatistic();
                        }
                    LOG.V("bleservice", "BlereTx onRead gl_nPktTmpCnt=  " + gl_nPktTmpCnt);
                    String sReData3 = "\r\nWill PktTx=" + gl_nPktTmpCnt + "\r\n ";

                    gl_bDataRec = false;
                    int nReTxCnt2 = 0;

                    if (nReTxCnt2 >= gl_nPktCnt) {
                        nReTxCnt2 = 0;
                    }
                    gl_nPktCnt = (buffer[3] & 0xFF);

                    gl_nPktCntBack = gl_nPktCnt;
                    byte[] sAck = {126, 4, 6, 122};
                    String ns = "";
                    for (int u = 0; u < sAck.length; ++u) {
                        ns = ns + Integer.toHexString(new Byte(sAck[u]).intValue());
                    }
                    String ns2 = "";
                    for (int u = 0; u < buffer.length; ++u) {
                        ns2 = ns2 + Integer.toHexString(new Byte(buffer[u]).intValue());
                    }

                    gl_nPktReadCnt = 0;
                    bisLastPkt = false;
                    gl_RecPrePktTime = System.currentTimeMillis();
                    gl_bOnreadpkt = false;
                    nRepert = 0;
                    if (ListNmeaBack == null)
                        ListNmeaBack = new CopyOnWriteArrayList();
                    if (ListNmeaBack.size() > 0) {
                        ListNmeaBack.clear();
                    }
                    BLETxAck(sAck);
                } else if (buffer[2] == 6) {
                    isHead = true;
                    m_RtcmCPktStatistic.CNTINIPKTACK();
                    isReturnGot = true;

                    bRtcmInitAckRec = false;

                    synchronized ("head") {
                        "head".notifyAll();
                    }
                } else if (buffer[2] == 8) {
                    m_RtcmCPktStatistic.CNTDATAPKTSACK();
                    packReturn = 0;
                    isReturnGot = true;
                    byte[] sAck = {126, 4, 9, 122};
                    BLETxAck(sAck);
                    m_RtcmCPktStatistic.CNTDATAACKACK();
                    synchronized ("pack") {
                        "pack".notifyAll();
                    }

                    LOG.D("netBleTest", "--------*-receive--ack-*---");
                } else if (buffer[2] == 9) {
                    m_nmeaCPktStatistic.CNTDATAACKACK();
                    gl_bAckAckRec = true;

                    LOG.E("bleservice", "onRead  ACK ACK rec");
                } else {
                    if ((buffer[2] == 4) ||
                            (buffer[2] != 2))
                        return;
                    if (buffer[3] == 255) {
                        packReturn = 2;
                    } else {
                        packReturn = 1;
                        int repairCount = buffer[3] & 0xFF;
                        m_RtcmCPktStatistic.SetRETXPKT(repairCount);
                        m_RtcmCPktStatistic.CNTRETXCNT();
                        repairIndex = new byte[repairCount];
                        System.arraycopy(buffer, 4, repairIndex, 0, repairCount);
                        isReturnGot = true;
                    }

                    synchronized ("pack") {
                        "pack".notifyAll();
                    }

                }

            } else {
                if (firstConnect) {
                    try {
                        byte[] writeBuffer = "$PCMDN,1,0\r\n".getBytes("UTF-8");

                        BLETxAck(writeBuffer);
                        String str = "TAG999,TimePCMDTNNN,run,1,PCMDN,$PCMDN,1,0,firstConnect," +
                                firstConnect;
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        StringWriter sw = new StringWriter();
                        e1.printStackTrace(new PrintWriter(sw, true));
                        String str = sw.toString();
                        LOG.V("NmeaAnalyzer5", "---123---->>>>str:" + str);
                    }

                    firstConnect = false;
                }
                gl_bDataRec = true;
                byte[] ss = new byte[buffer.length - 1];
                System.arraycopy(buffer, 1, ss, 0, buffer.length - 1);

                String sBufferLen = "\r\n" + Integer.toString(buffer.length - 1) + "\r\n";

                if ((gl_nPktCnt != 0) && (gl_nPktReadCnt < gl_nPktCnt)) {
                    long timePkt;
                    String stimePkt;
                    String stimePkt2;
                    gl_InitPktFirst = false;
                    long timef = System.currentTimeMillis();
                    gl_RevDatatimes = System.currentTimeMillis();
                    int nIndex = buffer[0] & 0xFF;
                    if (GetIndexBuffer(ListNmeaBack, nIndex) != null) {
                        LOG.V("bleservice", "Repert pkt nIndex= " + nIndex);
                        String sRet = "\r\n****************************************************************\r\n";

                        stimePkt = Integer.toString(nIndex);

                        String sRet2 = "\r\n****************************************************************\r\n";

                        return;
                    }
                    m_nmeaCPktStatistic.CNTSetPKT();
                    ListNmeaBack.add(buffer);

                    if (gl_bOnreadpkt) {
                        if (reTxN[(nReTxCnt - 1)] == nIndex) {
                            bisLastPkt = true;
                            timePkt = System.currentTimeMillis();
                            stimePkt = Long.toString(timePkt);

                            stimePkt2 = "\r\n" + stimePkt + ",retxbuffer  bisLastPkt = true " + gl_nPktReadCnt +
                                    " --" + gl_nPktCnt + "--" + nIndex + "\r\n";

                            gl_bOnreadpkt = false;
                            nRepert = 0;
                        } else {
                            bisLastPkt = false;
                            timePkt = System.currentTimeMillis();
                            stimePkt = Long.toString(timePkt);

                            stimePkt2 = "\r\n" + stimePkt + ",retxbuffer  bisLastPkt = false\r\n";
                        }

                    } else if (nIndex == gl_nPktCnt - 1) {
                        bisLastPkt = true;
                        timePkt = System.currentTimeMillis();
                        stimePkt = Long.toString(timePkt);

                        stimePkt2 = "\r\n" + stimePkt + ",retxbuffer ReadCnt == PktCnt " + gl_nPktReadCnt +
                                "--" + nIndex + "\r\n";
                    } else {
                        bisLastPkt = false;
                        timePkt = System.currentTimeMillis();
                        stimePkt = Long.toString(timePkt);

                        stimePkt2 = "\r\n" + stimePkt + ",retxbuffer ReadCnt != PktCnt " + gl_nPktReadCnt +
                                "--" + nIndex + "\r\n";
                    }

                    gl_RecPrePktTime = System.currentTimeMillis();
                    gl_nPktReadCnt += 1;
                }
                if ((gl_nPktCnt != 0) && (gl_nPktReadCnt == gl_nPktCnt)) {
                    m_nmeaCPktStatistic.CNTPerPKTS();
                    bisLastPkt = false;
                    gl_bOnreadpkt = false;
                    nRepert = 0;
                    CopyOnWriteArrayList List = ListNmeaBack;
                    DealNmeaListData(List, true);
                    AckTxDataRequest(false, true);
                    LOG.E("bleservice", "onRead  ACK ACK data time= " + System.currentTimeMillis());
                    long timePkt = System.currentTimeMillis();
                    String stimePkt = Long.toString(timePkt);

                    String sRet = "\r\n" + stimePkt + ",Success,nmea Data Rec" +
                            "\r\n==================================================All\r\n";

                    synchronized ("BLETX_SPEED") {
                        ble_rcv_state = 0;
                    }
                    long time2 = System.currentTimeMillis();
                    gl_nPktReadCnt = 0;
                    gl_nPktCnt = 0;
                }
            }
        }

        public void onWrite() {
        }

        public void onWrite(boolean bSuccess) {
        }

        public void onRSSI(int rssi, int status) {
            m_nBleRssi = rssi;

            LOG.E("BLETx", "BLETx nStatus onRSSI rssi= " + m_nBleRssi);
            m_nmeaCPktStatistic.SetBleRssi(m_nBleRssi);
            LocationDataManager.INSTANSE.updateRSSI(rssi);
            String stimePkt = "\r\nTime:" + System.currentTimeMillis() + ",Rssi:" + m_nBleRssi;

            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String date = sDateFormat.format(new Date());

            String st = "\r\nTimeRSSI," + date + ",rssi," + m_nBleRssi;
        }
    };

    public double getSdkAccuracy() {
        return this.sdkAccuracy;
    }

    public void onCreate() {
        this.bleServie = this;
    }

    public int GetBleRssi() {
        return this.m_nBleRssi;
    }

    public int DevicePower() {
        return this.devicePower;
    }

    public NordicBleManager getBleManager(Activity atv) {
        bleManager = new NordicBleManager(atv);
        return bleManager;
    }

    public BLeService(Activity atv) {
        this.m_Activity = atv;
        this.m_nmeaCPktStatistic.SetFileNmae("PktStatistic_LOG.txt");
        this.m_RtcmCPktStatistic.SetFileNmae("PktStatistic_LOG.txt");
        this.m_nmeaCPktStatistic.ClearStatistic();
        this.m_RtcmCPktStatistic.ClearStatistic();

        this.timer = new Timer();
        this.task = new TimerTask() {
            public void run() {
                if (BLeService.bleManager != null)
                    BLeService.bleManager.ReadRssi();
            }
        };
        this.timer.schedule(this.task, 0L, 1000L);
        this.timerVERSION = new Timer();
        this.taskVERSION = new TimerTask() {
            public void run() {
                try {
                    if (!(flagGNVER)) {
                        try {
                            byte[] bufferCC50VER = "$PCMDV\r\n".getBytes("UTF-8");
                            LOG.V("Command5", "------firstConnect-----4---------");
                            BLeService.bleManager.write(bufferCC50VER);
                            LOG.V("Command5", "------firstConnect-----5---------");
                            String str = new String(bufferCC50VER, "UTF-8");
                            LOG.V("PCMDTALL", "------$PCMDV------:" + str);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            LOG.V("Command5", "------firstConnect-----5----error-----");
                        }
                        return;
                    }
                    if (flagGNVER)
                        timerVERSION.cancel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        this.timerVERSION.schedule(this.taskVERSION, 0L, 10000L);
        this.NmeaThread.start();
        this.ACKTxThread.start();
        this.BLETxDataRunnalbeThread.start();
        this.NmeaTxRunnableThread.start();
    }

    public void CtrlRtcmThread(boolean bStart) {
        if (bStart) {
            this.RTCMThread.start();
        } else
            this.RTCMThread.stop();
    }

    protected void finalize() {
        LOG.V("NmeaAnalyzer5", "----bleservice------onDestroy---------------");
        this.bNmeaRunnable = false;
        this.bACKTxRunnable = false;
        this.bBLETxDataRunnalbe = false;
        this.bRtcmRunnable = false;
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        synchronized ("head") {
            "head".notifyAll();
        }
        synchronized ("pack") {
            "pack".notifyAll();
        }
        bleManager.exit();
        this.timer.cancel();
    }

    void BLEDealData(CopyOnWriteArrayList<byte[]> List) {
        if (List.size() == 0)
            return;
        LOG.V("bleservice", "Tx ACK To BL List.size() = " + List.size());
        for (int nPktCnt = 0; nPktCnt < List.size(); ++nPktCnt) {
            int nIndex = ((byte[]) List.get(nPktCnt))[0] & 0xFF;
            LOG.V("bleservice", "List nIndex= " + nIndex);
            StringBuilder sb = new StringBuilder();
            byte[] tmps = new byte[((byte[]) List.get(nPktCnt)).length - 1];
            System.arraycopy(List.get(nPktCnt), 1, tmps, 0, ((byte[]) List.get(nPktCnt)).length - 1);
            try {
                sb.append(new String(tmps, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            int k = 0;

            if (this.para == null) {
                this.para = new StringBuilder();
            }
            for (int i = 0; i < sb.length(); ++i) {
                if (sb.charAt(i) == '$') {
                    if (this.flag_para_start) {
                        this.flag_para_start = false;
                        decodeNMEA(this.para.toString(), 0);
                        this.para.delete(0, this.para.length());
                    }
                    this.flag_para_start = true;
                }

                if (this.flag_para_start) {
                    this.para.append(sb.charAt(i));
                }

                if (sb.charAt(i) == '\n') {
                    this.flag_para_start = false;
                    decodeNMEA(this.para.toString(), 0);
                    this.para.delete(0, this.para.length());
                }
            }
        }
    }

    boolean CheckSum(byte[] buff) {
        if (buff.length > 0) {
            byte result = 0;

            result = buff[1];
            int i = 0;
            for (i = 2; (i < buff.length) && (buff[i] != 42); ++i) {
                result = (byte) (result ^ buff[i]);
            }

            String sCheck = Integer.toHexString(result).toUpperCase();

            if (i + 4 != buff.length - 1) {
                return false;
            }

            if ((buff[i] != 42) || (buff[(i + 3)] != 13) || (buff[(i + 4)] != 10)) {
                String ns0 = "";
                int u = 0;
                while (true) {
                    ns0 = ns0 + Integer.toHexString(new Byte(buff[u]).intValue());

                    ++u;
                    if (u >= buff.length) {
                        return false;
                    }
                }
            }

            if (sCheck.length() == 2) {
                if ((sCheck.getBytes()[0] == buff[(i + 1)]) && (sCheck.getBytes()[1] == buff[(i + 2)])) {
                    return true;
                }

            } else if (sCheck.getBytes()[0] == buff[(i + 2)]) {
                return true;
            }

            String ns0 = "";
            int u = 0;
            while (true) {
                ns0 = ns0 + Integer.toHexString(new Byte(buff[u]).intValue());

                ++u;
                if (u >= buff.length) {
                    return false;
                }

            }

        }

        return false;
    }

    private void analyzeGNVER(String nmea) {
        if (!(CheckSum(nmea.getBytes())))
            return;
        int index = 0;
        StringBuffer str = new StringBuffer();

        for (int i = 0; i < nmea.length(); ++i)
            if ((((nmea.charAt(i) == ',') ? 1 : 0) | ((nmea.charAt(i) == '*') ? 1 : 0) | ((i == nmea.length() - 1) ? 1 : 0)) != 0) {
                if (!(str.toString().equals(""))) {
                    switch (index) {
                        case 1:
                            try {
                                this.BoardVersion = str.toString();
                            } catch (NumberFormatException e) {
                                LOG.E("analyzeGNVER", "Illegal BoardVersion: " + str);
                            }

                            break;
                        case 2:
                            try {
                                this.BootloaderVersion = str.toString();
                            } catch (NumberFormatException e) {
                                LOG.E("analyzeGNVER", "Illegal BootloaderVersion: " + str);
                            }

                            break;
                        case 3:
                            try {
                                this.BGIVersion = str.toString();
                            } catch (NumberFormatException e) {
                                LOG.E("analyzeGNVER", "Illegal BGIVersion: " + str);
                            }

                            break;
                        case 4:
                            try {
                                this.BGIDateTime = str.toString();
                            } catch (NumberFormatException e) {
                                LOG.E("analyzeGNVER", "Illegal BGIDateTime: " + str);
                            }

                    }

                }

                ++index;
                str.delete(0, str.length());
            } else {
                str.append(nmea.charAt(i));
            }
    }

    public boolean decodeNMEA(String buffer, int sourceID) {
        boolean isASCIIEncoded = false;

        if ((!(this.flagGNVER)) &&
                (buffer.startsWith("$GNVER"))) {
            analyzeGNVER(buffer);
            isASCIIEncoded = true;

            if (!(this.BoardVersion.equals(""))) {
                this.flagGNVER = true;
            }

            LOG.V("Command5", "------firstConnect-----7----app.bluetoothName:" + this.bluetoothName);
            LOG.V("Command5", "------firstConnect-----7----app.BoardVersion:" + this.BoardVersion);
            LOG.V("Command5", "------firstConnect-----7----app.BootloaderVersion:" + this.BootloaderVersion);
            LOG.V("Command5", "------firstConnect-----7----app.BGIVersion:" + this.BGIVersion);
            LOG.V("Command5", "------firstConnect-----7----app.BGIDateTime:" + this.BGIDateTime);
        }

        if ((!(this.flagPCMDT)) &&
                (buffer.startsWith("$PCMDT,OK"))) {
            this.sDateFormatTemp = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            this.dateTemp = this.sDateFormatTemp.format(new Date());
            this.strTemp = "\r\nTAG999,TimePCMDT--------4," + this.dateTemp + ",PCMDT-back," + buffer;

            this.flagPCMDT = true;
            isASCIIEncoded = true;

            LOG.V("PCMDTTEST", "------TTT-receive----OKOKOKOK-----:" + buffer);
        }

        if (buffer.startsWith("$PCMDN,OK")) {
            isASCIIEncoded = true;
            LOG.V("PCMDTTEST", "------NNN-receive----OKOKOKOK-----:" + buffer);
        }

        if (buffer.startsWith("$PCMD")) {
            isASCIIEncoded = true;
        }
        if (buffer.startsWith("$POWER")) {
            isASCIIEncoded = true;
            this.devicePower = Integer.parseInt(buffer.substring(7, buffer.length() - 2));
        } else {
            isASCIIEncoded = this.nmeaAnalyzer.analyze(buffer, new NmeaAnalyzer.NmeaCallback() {
                public void onNmeaOneBroadcast(String nmea) {
                    if (!(nmea.equals(""))) {
                        Bundle bundle = new Bundle();
                        bundle.putString("nmea", nmea);
                        Intent intent = new Intent("com.ols.broadcast.nmea");
                        intent.putExtras(bundle);
                        if (m_Activity != null)
                            m_Activity.sendBroadcast(intent);
                    }
                }

                public void onNmeaUpdate(boolean[] hasGSV) {
                    MyLocation location = new MyLocation();
                    LOG.V("TAG", "---SSSSS---nmeaAnalyzer-1---:");
                    location.setLatitude(nmeaAnalyzer.getLatitude());
                    LOG.V("TAG", "---SSSSS---nmeaAnalyzer-2---getLatitude:" + nmeaAnalyzer.getLatitude());
                    location.setLongitude(nmeaAnalyzer.getLongitude());
                    LOG.V("TAG", "---SSSSS---nmeaAnalyzer-3---getLongitude:" + nmeaAnalyzer.getLongitude());

                    location.setAltitude(nmeaAnalyzer.getAltitude());
                    LOG.V("TAG", "---SSSSS---nmeaAnalyzer-4---getAltitude:" + nmeaAnalyzer.getAltitude());

                    location.setSpeed(nmeaAnalyzer.getSpeed());
                    LOG.V("TAG", "---SSSSS---nmeaAnalyzer-5---getSpeed:" + nmeaAnalyzer.getSpeed());

                    location.setBearing(nmeaAnalyzer.getBearing());
                    LOG.V("TAG", "---SSSSS---nmeaAnalyzer-6---getBearing:" + nmeaAnalyzer.getSpeed());

                    location.setMode(nmeaAnalyzer.getMode());

                    LOG.V("TAG", "---SSSSS---nmeaAnalyzer--7-getMode---:" + nmeaAnalyzer.getMode());

                    location.setPdop(nmeaAnalyzer.getPdop());

                    LOG.V("TAG", "---SSSSS---nmeaAnalyzer--8-PDOP---:" + nmeaAnalyzer.getPdop());
                    location.setHdop(nmeaAnalyzer.getHdop());
                    LOG.V("TAG", "---SSSSS---nmeaAnalyzer--9-HDOP---:" + nmeaAnalyzer.getHdop());
                    location.setVdop(nmeaAnalyzer.getVdop());
                    LOG.V("TAG", "---SSSSS---nmeaAnalyzer--10-VDOP---:" + nmeaAnalyzer.getVdop());

                    location.setSatInView(nmeaAnalyzer.getSatellites().size());
                    LOG.V("TAGGG", "---SSSSS---nmeaAnalyzer--10-VDOP---:" + nmeaAnalyzer.getSatellites().size());

                    location.setBaseDelay(nmeaAnalyzer.getBaseDelay());

                    location.setBaseLine(nmeaAnalyzer.getBaseLine());

                    location.setBaseGPSN(nmeaAnalyzer.getBaseGPSN());

                    location.setBaseBDSN(nmeaAnalyzer.getBaseBDSN());

                    location.setBaseElvGpsSnr(nmeaAnalyzer.getBaseElvGpsSnr());

                    location.setBaseElvBdsSnr(nmeaAnalyzer.getBaseElvBdsSnr());

                    location.setRoverGPSN(nmeaAnalyzer.getRoverGPSN());

                    location.setRoverBDSN(nmeaAnalyzer.getRoverBDSN());

                    location.setRoverElvGpsSnr(nmeaAnalyzer.getRoverElvGpsSnr());

                    location.setRoverElvBdsSnr(nmeaAnalyzer.getRoverElvBdsSnr());

                    location.setRoverUsedGPSN(nmeaAnalyzer.getRoverUsedGPSN());

                    location.setRoverUsedBDSN(nmeaAnalyzer.getRoverUsedBDSN());

                    double hepe = nmeaAnalyzer.getHepe();
                    double wepe = nmeaAnalyzer.getWepe();

                    if ((hepe == -1.0D) && (wepe == -1.0D)) {
                        location.setAccuracy(-1.0D);
                        sdkAccuracy = -1.0D;
                    } else {
                        double ava = hepe * hepe + wepe * wepe;
                        ava = Math.sqrt(ava);
                        sdkAccuracy = ((int) (ava * Math.pow(10.0D, 3.0D)) / Math.pow(10.0D, 3.0D));
                        Log.e("setAccuracy", "service ava:" + ava);
                        location.setAccuracy(ava);
                        location.setHorizonAccuracy(hepe);
                        location.setVerticalAccuracy(wepe);
                    }

                    nmeaAnalyzer.resetHWepe();

                    SrcManager.INSTANCE.sendLocation(location, nmeaAnalyzer.srcId);
                }

            });
        }

        return isASCIIEncoded;
    }

    CopyOnWriteArrayList<CopyOnWriteArrayList<byte[]>> DealNmeaListData(CopyOnWriteArrayList<byte[]> List, boolean bAdd) {
        int i = 0;

        synchronized ("NMEABUFFER") {
            if (this.ListNmeaBackList == null) {
                this.ListNmeaBackList = new CopyOnWriteArrayList();
            }
            if (bAdd) {
                LOG.V("bleservice", "DealNmeaListData aaa ");
                if (List == null)
                    return null;
                CopyOnWriteArrayList ListAdd = new CopyOnWriteArrayList();
                ListAdd.clear();
                LOG.V("bleservice", "DealNmeaListData bbb ");
                for (int nCnt = 0; nCnt < List.size(); ++nCnt) {
                    byte[] buffer = GetIndexBuffer(List, nCnt);
                    if (buffer != null)
                        ListAdd.add(buffer);
                }
                LOG.V("bleservice", "DealNmeaListData ccc " + ListAdd.size());
                this.ListNmeaBackList.add(ListAdd);
            } else {
                int nListListSize = this.ListNmeaBackList.size();
                LOG.V("bleservice", "DealNmeaListData " + nListListSize);
                if (nListListSize == 0)
                    return null;
                this.ListBackList.clear();
                this.ListBackList.addAll(this.ListNmeaBackList);
                LOG.V("bleservice", "DealNmeaListData 111" + nListListSize);
                this.ListNmeaBackList.clear();
                return this.ListBackList;
            }
        }

        return null;
    }

    boolean AckTxRequest(boolean bGet, boolean bAckTxRequest) {
        synchronized ("ACKBUFFER") {
            if (bGet) {
                return this.gl_bAckTxRequest;
            }
            this.gl_bAckTxRequest = bAckTxRequest;
            return true;
        }
    }

    boolean AckTxDataRequest(boolean bGet, boolean bAckDataTxRequest) {
        synchronized ("ACK_DataBUFFER") {
            if (bGet) {
                return this.gl_bAckDataTxRequest;
            }
            this.gl_bAckDataTxRequest = bAckDataTxRequest;
            return true;
        }
    }

    int BLETx2(byte[] buffer) {
        if (bleManager == null) {
            return 0;
        }
        int nstatus = 1;

        int speed_delay = this.ble_rcv_state;

        if (speed_delay == 0) {
            speed_delay = 10;
            Log.v("BLETx", "BLETx speed_delay  10");
        } else {
            speed_delay = 80;
            Log.v("BLETx", "BLETx speed_delay  80");
        }
        speed_delay = 10;
        if (!(bleManager.IsWriteReady())) {
            Log.v("BLETx", "BLETx nStatus onCharacteristicWrite TxCnt= speed_delay=" + speed_delay);
            bleManager.SetWriteReady();
            try {
                Thread.sleep(speed_delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        if (bleManager.IsWriteReady()) {
            this.m_RtcmCPktStatistic.CNTTxDataCnt();
            this.m_nmeaCPktStatistic.CNTTxDataCnt();

            nstatus = bleManager.write(buffer);

            bleManager.getClass();
            if (nstatus == 0) {
                Log.v("BLETx", "BLETx BLETx nstatus WRITE_OPERRATION_SUCCESS ");

                Log.v("BLETx", "BLETx nStatus onCharacteristicWrite SUCCESS " + this.m_nBleRssi);
            } else {
                Log.v("BLETx", "BLETx BLETx nstatus WRITE_OPERRATION_SUCCESS not");
                if (bleManager != null) {
                    Log.v("BLETx", "BLETx nStatus onCharacteristicWrite Fail " + this.m_nBleRssi);
                }

                this.m_RtcmCPktStatistic.CNTTxDataFailCnt();
                this.m_nmeaCPktStatistic.CNTTxDataFailCnt();
                String e = "\r\nBLE SendData operation failed\r\n";
            }
        } else {
            bleManager.getClass();
            nstatus = 3;
            Log.v("BLETx", "BLETx BLETx nstatus Request BLE SendData failed");
            bleManager.SetWriteReady();
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String stimePkt4 = "\r\nRequest BLE SendData failed ";
            if (speed_delay == 80)
                stimePkt4 = stimePkt4 + " speed_delay== 80\r\n";
            else {
                stimePkt4 = stimePkt4 + " speed_delay== 10\r\n";
            }

        }

        return nstatus;
    }

    byte[] GetBleTxData() {
        synchronized ("BLETXBUFFERnew") {
            if (this.m_ListBLETxBuffer == null) {
                return null;
            }
            int nWrite = this.gl_ListBLETxBufferWrite;
            int nRead = this.gl_ListBLETxBufferRead;
            if (nWrite == nRead) {
                return null;
            }
            if (nRead < nWrite) {
                this.gl_ListBLETxBufferRead += 1;
                return ((byte[]) ((byte[]) this.m_ListBLETxBuffer.get(nRead % 4096)).clone());
            }

            return null;
        }
    }

    byte[] GetBleTxDataAck() {
        synchronized ("BLETX_BUFFER_ACK") {
            if (this.m_ListBLETxBufferAck == null) {
                return null;
            }
            int nWrite = this.gl_ListBLETxBufferAckWrite;
            int nRead = this.gl_ListBLETxBufferAckRead;
            if (nWrite == nRead) {
                return null;
            }
            if (nRead < nWrite) {
                this.gl_ListBLETxBufferAckRead += 1;
                return ((byte[]) ((byte[]) this.m_ListBLETxBufferAck.get(nRead % 4096)).clone());
            }

            return null;
        }
    }

    void BLETxAck(byte[] buffer) {
        synchronized ("BLETX_BUFFER_ACK") {
            if (this.m_ListBLETxBufferAck == null) {
                this.m_ListBLETxBufferAck = new ArrayList();
                for (int i = 0; i < 4096; ++i) {
                    byte[] buf = new byte[1];
                    this.m_ListBLETxBufferAck.add(buf);
                }
            }
            this.m_ListBLETxBufferAck.set(this.gl_ListBLETxBufferAckWrite % 4096, buffer);
            this.gl_ListBLETxBufferAckWrite += 1;
        }
    }

    int BLETx(byte[] buffer) {
        synchronized ("BLETXBUFFERnew") {
            if (this.m_ListBLETxBuffer == null) {
                this.m_ListBLETxBuffer = new ArrayList();
                for (int i = 0; i < 4096; ++i) {
                    byte[] buf = new byte[1];
                    this.m_ListBLETxBuffer.add(buf);
                }
            }
            this.m_ListBLETxBuffer.set(this.gl_ListBLETxBufferWrite % 4096, buffer);
            this.gl_ListBLETxBufferWrite += 1;
            return 1;
        }
    }

    boolean InitPkt(int nCnt) {
        this.isHead = false;

        int whileTimes = 0;

        while (!(this.isHead)) {
            long timePkt;
            String stimePkt;
            String sRet;
            byte[] head = {126, 6, 1, (byte) nCnt, 0, 122};
            BLETx(head);
            this.m_RtcmCPktStatistic.SetTime(System.currentTimeMillis());
            this.m_RtcmCPktStatistic.CNTINIPKT();
            this.m_RtcmCPktStatistic.SaveStatistic(1);
            if (!(this.bRtcmInitAckRec)) {
                timePkt = System.currentTimeMillis();
                stimePkt = Long.toString(timePkt);

                sRet = stimePkt + ",Success RTCM init Pkt Sended, ";
                String ns0 = "";
                for (int u = 0; u < head.length; ++u) {
                    ns0 = ns0 + Integer.toHexString(new Byte(head[u]).intValue());
                }
                sRet = sRet + ns0 + "\r\n";

                this.bRtcmInitAckRec = true;
            } else {
                timePkt = System.currentTimeMillis();
                stimePkt = Long.toString(timePkt);

                sRet = stimePkt + ",Error,RTCM initPkt ACK Rec not\r\n";
            }

            synchronized ("head") {
                try {
                    "head".wait(50L);
                    ++whileTimes;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            LOG.V("netBleTest", "---------while------OOOOOOOOOOO--->>>>status:" + bleManager.getConnectStatus());
            this.sDateFormatTemp = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            this.dateTemp = this.sDateFormatTemp.format(new Date());
            this.strTemp = "\r\nTAG999,TimeRestartBleService--------99," + this.dateTemp + "-in--initPK---status:" +
                    bleManager.getConnectStatus();

            LOG.V("netBleTest", this.strTemp);
            if (whileTimes > 9) {
                this.count_while_all_times += 1;
                return false;
            }
        }
        this.isHead = false;
        this.count_while_all_times = 0;
        return true;
    }

    public String getPCMDT() {
        String UTCTimeBuffer = new String();
        try {
            Calendar cal = Calendar.getInstance();

            int zoneOffset = cal.get(Calendar.ZONE_OFFSET);

            int dstOffset = cal.get(Calendar.DST_OFFSET);

            cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            int day = cal.get(Calendar.DATE);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);
            int second = cal.get(Calendar.SECOND);

            UTCTimeBuffer = "$PCMDT," + String.valueOf(year) + "," + String.valueOf(month) + "," + String.valueOf(day) +
                    "," + String.valueOf(hour) + "," + String.valueOf(minute) + "," + String.valueOf(second) + "\r\n";

            LOG.V("netBleTest", "------reConnect-----getUTC----2222222-----utcTimeBuffer锛�" + UTCTimeBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return UTCTimeBuffer;
    }

    void DealData2(byte[] buffer, int frameCnt, int nLen) {
        long timePkt1;
        String stimePkt1;
        String sRet1;
        int mark = 0;
        CopyOnWriteArrayList packCache = new CopyOnWriteArrayList();
        int index = 1;
        int frameNum = frameCnt;
        this.rtcm_lost_count += 1;
        byte[] pack = new byte[nLen + 1];
        this.m_RtcmCPktStatistic.CNTPerPKTS();
        this.m_RtcmCPktStatistic.SetPKT(frameCnt);
        for (int i = 0; i < frameNum; ++i) {
            System.arraycopy(buffer, mark, pack, 1, nLen);
            mark += nLen;
            pack[0] = (byte) i;
            packCache.add(i, (byte[]) pack.clone());

            BLETx((byte[]) packCache.get(i));
        }
        long timePkt = System.currentTimeMillis();
        String stimePkt = Long.toString(timePkt);

        String sRet = ",Success RTCM DataPkt Sended\r\n";

        LOG.E("44444", "System.currentTimeMillis() bbb  " + timePkt);
        this.isReturnGot = false;
        synchronized ("pack") {
            try {
                "pack".wait(500L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOG.E("44444", "System.currentTimeMillis() aaa timegap=  " + (System.currentTimeMillis() - timePkt));
        byte[] request = {126, 4, 5, 122};
        if (!(this.isReturnGot)) {
            timePkt1 = System.currentTimeMillis();
            stimePkt1 = Long.toString(timePkt1);

            sRet1 = stimePkt1 + ",Error RTCM Data ACK and retxpkt Rec not\r\n";

            this.m_RtcmCPktStatistic.CNTTIMEOUT();
        }
        if (this.packReturn == 0) {
            timePkt1 = System.currentTimeMillis();
            stimePkt1 = Long.toString(timePkt1);

            sRet1 = stimePkt1 + ",Success RTCM Data ACK Rec\r\n";
        }

        while (this.packReturn == 1) {
            long timePkt2;
            String stimePkt2;
            String sRet2;
            long timePkt6 = System.currentTimeMillis();
            String stimePkt6 = Long.toString(timePkt6);

            String sRet6 = stimePkt6 + ",Success RTCM Data retxpkt Rec\r\n";

            for (byte b : this.repairIndex) {
                int i = b;
                this.m_RtcmCPktStatistic.CNTREVRETXPKT();
                if (i < packCache.size()) {
                    BLETx((byte[]) packCache.get(i));
                }
            }
            timePkt1 = System.currentTimeMillis();
            stimePkt1 = Long.toString(timePkt1);
            LOG.E("44444", "System.currentTimeMillis() bbb retx= " + timePkt1);
            sRet1 = stimePkt1 + ",Success RTCM Data Retxpkt Sended\r\n";

            this.isReturnGot = false;
            synchronized ("pack") {
                try {
                    "pack".wait(300L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            LOG.E("44444", "System.currentTimeMillis() aaa timegap retx=  " + (System.currentTimeMillis() - timePkt1));

            if (!(this.isReturnGot)) {
                timePkt2 = System.currentTimeMillis();
                stimePkt2 = Long.toString(timePkt2);

                sRet2 = stimePkt2 + ",Error RTCM Data Retxpkt  Rec not\r\n";

                this.packReturn = 0;
                this.m_RtcmCPktStatistic.CNTTIMEOUT2();
            } else {
                timePkt2 = System.currentTimeMillis();
                stimePkt2 = Long.toString(timePkt2);

                sRet2 = stimePkt2 + ",Success RTCM Data Retxpkt Rec\r\n";
            }

            this.packReturn = 0;
        }

        mark = 0;
        packCache.clear();
        this.isHead = false;
        this.packReturn = -1;
    }

    void TxDifDataV2(byte[] buffer, int nLen) {
        int nBigpkt;
        int nRemainpkt;
        int i;
        int nCnt;
        int k;
        byte[] pack;
        int PktCnt = nLen / 19;

        int nRemain = nLen % 19;

        int mark = 0;
        int nfMaxCnt = 15;
        if (PktCnt >= 20) {
            nBigpkt = PktCnt / 20;
            nRemainpkt = PktCnt % 20;
            for (i = 0; i < nBigpkt; ++i) {
                nCnt = 20;
                for (k = 0; k < 1; ++k) {
                    boolean status = InitPkt(nCnt);
                    if (!(status)) {
                        this.sDateFormatTemp = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                        this.dateTemp = this.sDateFormatTemp.format(new Date());
                        this.strTemp = "\r\nTAG999,TimeRestartBleService--------666," + this.dateTemp +
                                "-in--TxDifDataV2---false:" + bleManager.getConnectStatus();

                        LOG.V("netBleTest", this.strTemp);
                        return;
                    }

                }

                pack = new byte[nCnt * 19];
                System.arraycopy(buffer, mark, pack, 0, nCnt * 19);
                mark += nCnt * 19;
                DealData2(pack, nCnt, 19);
            }

            nCnt = nRemainpkt;
            if (nCnt > 0) {
                for (k = 0; k < 1; ++k) {
                    InitPkt(nCnt);
                }
                pack = new byte[nCnt * 19];
                System.arraycopy(buffer, mark, pack, 0, nCnt * 19);
                mark += nCnt * 19;
                DealData2(pack, nCnt, 19);
            }
        } else if (PktCnt >= nfMaxCnt) {
            nBigpkt = PktCnt / nfMaxCnt;
            nRemainpkt = PktCnt % nfMaxCnt;
            for (nCnt = 0; nCnt < nBigpkt; ++nCnt) {
                pack = nfMaxCnt;
                InitPkt(pack);
                pack = new byte[pack * 19];
                System.arraycopy(buffer, mark, pack, 0, pack * 19);
                mark += pack * 19;
                DealData2(pack, pack, 19);
            }

            nCnt = nRemainpkt;
            if (nCnt > 0) {
                for (pack = 0; pack < 1; ++pack) {
                    InitPkt(nCnt);
                }
                pack = new byte[nCnt * 19];
                System.arraycopy(buffer, mark, pack, 0, nCnt * 19);
                mark += nCnt * 19;
                DealData2(pack, nCnt, 19);
            }
        } else if (PktCnt >= 10) {
            nBigpkt = PktCnt / 10;
            nRemainpkt = PktCnt % 10;
            for (nCnt = 0; nCnt < nBigpkt; ++nCnt) {
                pack = 10;
                for (pack = 0; pack < 1; ++pack) {
                    InitPkt(pack);
                }
                pack = new byte[pack * 19];
                System.arraycopy(buffer, mark, pack, 0, pack * 19);
                mark += pack * 19;
                DealData2(pack, pack, 19);
            }

            nCnt = nRemainpkt;
            if (nCnt > 0) {
                for (pack = 0; pack < 1; ++pack) {
                    InitPkt(nCnt);
                }
                pack = new byte[nCnt * 19];
                System.arraycopy(buffer, mark, pack, 0, nCnt * 19);
                mark += nCnt * 19;
                DealData2(pack, nCnt, 19);
            }
        } else {
            int nCnt = PktCnt;
            if (nCnt > 0) {
                for (int k = 0; k < 1; ++k) {
                    InitPkt(nCnt);
                }
                byte[] pack = new byte[nCnt * 19];
                System.arraycopy(buffer, mark, pack, 0, nCnt * 19);
                mark += nCnt * 19;
                DealData2(pack, nCnt, 19);
            }

        }

        int nRemainBytes = nRemain;
        if (nRemainBytes > 0) {
            int nPkt = 1;
            for (int k = 0; k < 1; ++k) {
                InitPkt(nPkt);
            }
            byte[] pack2 = new byte[nRemainBytes];
            System.arraycopy(buffer, mark, pack2, 0, nRemainBytes);
            DealData2(pack2, 1, nRemainBytes);
        }
    }

    public long getSDFreeSize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs sf = new StatFs(path.getPath());

        long blockSize = sf.getBlockSize();

        long freeBlocks = sf.getAvailableBlocks();

        return (freeBlocks * blockSize / 1024L / 1024L);
    }

    void SaveBuffer(byte[] buffer, String name) {
        int i = 0;
        if (i == 0)
            return;
        if ((!(selfdefineFileConfigControl)) && ((
                (name.startsWith("BDSView_task")) || (name.startsWith("BDSView_TASK"))))) {
            return;
        }
        if ((!(diagFilesConfigControl)) && (((name.startsWith("BDSView_diag")) || (name.startsWith("BDSView_DIAG"))))) {
            return;
        }
        if ((!(nmeaFileConfigControl)) && (((name.startsWith("BDSView_nmea")) || (name.startsWith("BDSView_NMEA"))))) {
            return;
        }
        if ((!(rtcmorgFileConfigControl)) && (((name.startsWith("BDSView_rtcm")) || (name.startsWith("BDSView_RTCM"))))) {
            return;
        }

        File sdCardDir = Environment.getExternalStorageDirectory();

        File saveFile = new File(sdCardDir, name);

        this.fressSize = getSDFreeSize();
        if (this.fressSize <= 0L) {
            return;
        }

        try {
            OutputStream outStream = new FileOutputStream(saveFile, true);
            outStream.write(buffer);
            outStream.flush();
            outStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    byte[] GetIndexBuffer(CopyOnWriteArrayList<byte[]> list, int nIdex) {
        for (int nCnt = 0; nCnt < list.size(); ++nCnt) {
            if ((((byte[]) list.get(nCnt))[0] & 0xFF) == nIdex) {
                return ((byte[]) list.get(nCnt));
            }
        }

        return null;
    }

    public int init() {
        if (bleManager == null) {
            LOG.V("NmeaAnalyzer5", " ++++ 钃濈墮鏈垵濮嬪寲 +++++++++");
            return 0;
        }
        return bleManager.bleInit();
    }

    public byte[] SdkData(byte[] buffer, boolean bAdd) {
        synchronized ("RTCMBUFFER") {
            if (this.sRtcmPktBuffer == null) {
                this.sRtcmPktBuffer = new CopyOnWriteArrayList();
            }
            if (bAdd) {
                if (buffer == null) {
                    return null;
                }
                this.sRtcmPktBuffer.add(buffer);
                return null;
            }

            if (this.sRtcmPktBuffer.size() == 0) {
                return null;
            }
            int nLen = 0;
            for (int i = 0; i < this.sRtcmPktBuffer.size(); ++i) {
                nLen += ((byte[]) this.sRtcmPktBuffer.get(i)).length;
            }

            byte[] sRtcmPkt = new byte[nLen];
            int nMark = 0;

            for (int j = 0; j < this.sRtcmPktBuffer.size(); ++j) {
                int nL = ((byte[]) this.sRtcmPktBuffer.get(j)).length;
                System.arraycopy(this.sRtcmPktBuffer.get(j), 0, sRtcmPkt, nMark, nL);
                nMark += nL;
            }

            String ns = "";
            for (int u = 0; u < sRtcmPkt.length; ++u) {
                ns = ns + Integer.toHexString(new Byte(sRtcmPkt[u]).intValue());
            }

            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String date = sDateFormat.format(new Date());
            String str = "\r\nTAG999,TimeFRAMLIST-----------4--------1," + date +
                    ",---3-----------ble------------------*-----T0:";
            SaveBuffer(str.getBytes(), "BDSView_TASK.txt");
            LOG.V("", "sRtcmPkt sRtcmPkt.length= " + sRtcmPkt.length);
            TxDifDataV2(sRtcmPkt, sRtcmPkt.length);

            sDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            date = sDateFormat.format(new Date());
            str = "\r\nTAG999,TimeFRAMLIST-----------4--------2," + date +
                    ",---3-----------ble------------------*-----T0:";
            SaveBuffer(str.getBytes(), "BDSView_TASK.txt");
            this.sRtcmPktBuffer.clear();
            return null;
        }
    }

    public void connectDevice(BluetoothDevice device, long timeout) {
        StringWriter sw;
        String str;
        String strTemp = "\r\nTAG999,TimeRestartBleService,8," + this.dateTemp + ",device.getAddress():" +
                device.getAddress();
        this.bluetoothName = device.getName();
        this.address = device.getAddress();
        try {
            bleManager.connectDevice(device, timeout, false, this.gattCallback);
        } catch (NordicBleManager.NullDeviceException e) {
            e.printStackTrace();
            sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            str = sw.toString();

            LOG.V("NmeaAnalyzer5", "-------connectDevice--error----------" + str);
        } catch (NordicBleManager.AlreadyConnectException e) {
            e.printStackTrace();
            sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            str = sw.toString();

            LOG.V("NmeaAnalyzer5", "-------connectDevice--error----------" + str);
        }
    }

    public void disconnectDevice() {
        this.isManualDis = true;
        bleManager.disconnect();
        LOG.V("NmeaAnalyzer5", "-------disconnectDevice--shoudong ?----------");
    }

    public IBinder onBind(Intent arg0) {
        return null;
    }

    private class ACKTxRunnable
            implements Runnable {
        private String TAG = "ACKTxRunnable";

        public void run() {
            while (bACKTxRunnable) {
                long timePkt4;
                String stimePkt4;
                String sRet4;
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
                long nCurTime = System.currentTimeMillis();

                if (AckTxRequest(true, true)) {
                    timePkt4 = System.currentTimeMillis();
                    stimePkt4 = Long.toString(timePkt4);

                    sRet4 = "\r\n" + stimePkt4 + ",Will send nmea data Initpkt ACK in Event\r\n";

                    long time1 = System.currentTimeMillis();

                    byte[] sAck = {126, 4, 6, 122};
                    AckTxRequest(false, false);
                    long time2 = System.currentTimeMillis();
                    LOG.E("bleservice", "send nmea data Initpkt ACK bbbb=  " + (time2 - time1));
                    m_nmeaCPktStatistic.CNTINIPKTACK();
                    BLETxAck(sAck);
                    long time3 = System.currentTimeMillis();
                    LOG.E("bleservice", "send nmea data Initpkt ACK aaa=  " + (time3 - time2));
                    long timePkt = System.currentTimeMillis();
                    String stimePkt = Long.toString(timePkt);

                    String sRet = "\r\n" + stimePkt + ",nmea data Initpkt ACK Sended, ";
                    String ns0 = "";
                    for (int u = 0; u < sAck.length; ++u) {
                        ns0 = ns0 + Integer.toHexString(new Byte(sAck[u]).intValue());
                    }
                    long time4 = System.currentTimeMillis();

                    LOG.E("bleservice",
                            "send nmea data Initpkt ACK Sended=  " + (time4 - time3) + "   " + (time4 - time1));

                    sRet = sRet + ns0;
                    sRet = sRet + "\r\n";
                }

                if (AckTxDataRequest(true, true)) {
                    timePkt4 = System.currentTimeMillis();
                    stimePkt4 = Long.toString(timePkt4);

                    sRet4 = stimePkt4 + ",Will send nmea data ACK in Event\r\n ";

                    byte[] sAck = {126, 4, 8, 122};
                    AckTxDataRequest(false, false);
                    gl_bAckAckRec = false;
                    int nRepertCnt = 1;
                    do {
                        BLETxAck(sAck);
                        m_nmeaCPktStatistic.CNTRETXACK();
                        LOG.E("bleservice", "onRead  ACK ACK Tx time= " + System.currentTimeMillis());
                        try {
                            Thread.sleep(50L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        --nRepertCnt;
                        if (gl_bAckAckRec)
                            break;
                    }
                    while (
                            nRepertCnt >= 0);

                    long timePkt = System.currentTimeMillis();
                    String stimePkt = Long.toString(timePkt);

                    String sRet = stimePkt + ",nmea data data ACK Sended, ";
                    String ns0 = "";
                    for (int u = 0; u < sAck.length; ++u) {
                        ns0 = ns0 + Integer.toHexString(new Byte(sAck[u]).intValue());
                    }
                    sRet = sRet + ns0 + "\r\n";
                }
            }
        }
    }

    private class BLETxDataRunnalbe
            implements Runnable {
        public void run() {
            while (bBLETxDataRunnalbe) {
                boolean bData = false;
                int nWrite = gl_ListBLETxBufferWrite;
                int nRead = gl_ListBLETxBufferRead;
                int nRet = nWrite - nRead;
                byte[] sAck = GetBleTxDataAck();
                if (sAck != null) {
                    bData = true;
                    BLETx2(sAck);
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String ns = "";
                    for (int u = 0; u < sAck.length; ++u) {
                        ns = ns + Integer.toHexString(new Byte(sAck[u]).intValue());
                    }
                    long timePkt = System.currentTimeMillis();
                    String stimePkt = Long.toString(timePkt);

                    String stimePkt2 = "\r\n" + stimePkt + ",BLETxDataRunnalbe TxAck " + ns + "," +
                            gl_ListBLETxBufferAckWrite + "--" + gl_ListBLETxBufferAckRead + "\r\n";

                    Log.v("BLETxDataRunnalbe", "sAck=" + ns + "--" + "gl_ListBLETxBufferAckWrite= " +
                            gl_ListBLETxBufferAckWrite + " gl_ListBLETxBufferAckRead= " + gl_ListBLETxBufferAckRead);
                }

                while (gl_ListBLETxBufferWrite > gl_ListBLETxBufferRead) {
                    byte[] sBuffer = GetBleTxData();
                    if (sBuffer != null) {
                        BLETx2(sBuffer);
                        try {
                            Thread.sleep(10L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        bData = true;
                        Log.v("BLETxDataRunnalbe", "gl_ListBLETxBufferWrite= " + gl_ListBLETxBufferWrite +
                                " gl_ListBLETxBufferRead= " + gl_ListBLETxBufferRead);
                    } else {
                        Log.v("BLETxDataRunnalbe", "BLETxDataRunnalbe sleep");
                    }

                    byte[] sAck1 = GetBleTxDataAck();
                    if (sAck1 != null) {
                        bData = true;
                        BLETx2(sAck1);
                        try {
                            Thread.sleep(10L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        String ns = "";
                        for (int u = 0; u < sAck1.length; ++u) {
                            ns = ns + Integer.toHexString(new Byte(sAck1[u]).intValue());
                        }
                        long timePkt = System.currentTimeMillis();
                        String stimePkt = Long.toString(timePkt);

                        String stimePkt2 = "\r\n" + stimePkt + ",BLETxDataRunnalbe TxAck " + ns + "," +
                                gl_ListBLETxBufferAckWrite + "--" + gl_ListBLETxBufferAckRead + "\r\n";

                        Log.v("BLETxDataRunnalbe",
                                "sAck=" + ns + "--" + "gl_ListBLETxBufferAckWrite= " + gl_ListBLETxBufferAckWrite +
                                        " gl_ListBLETxBufferAckRead= " + gl_ListBLETxBufferAckRead);
                    }
                }
                if (bData) continue;
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class NmeaRunnable
            implements Runnable {
        public void run() {
            while (bNmeaRunnable) {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
                if (nRepert > 3) {
                    byte[] sAck = {126, 4, 8, 122};
                    nRepert = 0;

                    BLETxAck(sAck);
                    gl_bOnreadpkt = false;
                    String sRet = "\r\n reTxPkt Cnt > 3 ,";

                    String ns = "";
                    for (int u = 0; u < sAck.length; ++u) {
                        ns = ns + Integer.toHexString(new Byte(sAck[u]).intValue());
                    }
                    sRet = sRet + ns;
                    sRet = sRet + "\r\n";
                }

                long losstimegap = System.currentTimeMillis() - gl_RecPrePktTime;
                boolean bContinue = false;
                if (!(bisLastPkt)) {
                    bContinue = true;

                    if ((gl_RecPrePktTime == 0L) || (gl_nPktReadCnt == 0) ||
                            (System.currentTimeMillis() - gl_RecPrePktTime < 100L)) {
                        bContinue = true;
                    } else {
                        m_nmeaCPktStatistic.CNTTIMEOUT();
                        bContinue = false;
                    }

                } else {
                    bContinue = false;
                }

                if (bContinue)
                    continue;
                int tmpgl_nPktReadCnt = gl_nPktReadCnt;
                int tmpgl_nPktCnt = gl_nPktCnt;
                if ((tmpgl_nPktReadCnt >= tmpgl_nPktCnt) ||
                        (tmpgl_nPktReadCnt == 0)) continue;
                int[] bListFlag = new int[tmpgl_nPktCnt];
                for (int n = 0; n < tmpgl_nPktCnt; ++n)
                    bListFlag[n] = 0;
                if (ListNmeaBack.size() == 0)
                    continue;
                for (int k = 0; k < ListNmeaBack.size(); ++k) {
                    if ((((byte[]) ListNmeaBack.get(k))[0] & 0xFF) > tmpgl_nPktCnt - 1) {
                        for (int n = 0; n < tmpgl_nPktCnt; ++n) {
                            bListFlag[n] = 0;
                        }
                        break;
                    }
                    bListFlag[(((byte[]) ListNmeaBack.get(k))[0] & 0xFF)] = 1;
                }

                if ((tmpgl_nPktReadCnt == 0) || (tmpgl_nPktReadCnt >= tmpgl_nPktCnt) ||
                        (tmpgl_nPktCnt == 0) || (bListFlag == null))
                    continue;
                nReTxCnt = 0;
                reTxN = new byte[tmpgl_nPktCnt];
                LOG.V("bleservice", "BlereTx 0000 onWrite nReTxCnt=  " + nReTxCnt);
                for (int n = 0; n < bListFlag.length; ++n) {
                    if (bListFlag[n] != 0)
                        continue;
                    reTxN[nReTxCnt] = (byte) n;
                    LOG.V("bleservice", "BlereTx Loss No=  " + reTxN[nReTxCnt]);
                    nReTxCnt += 1;
                }

                if (nReTxCnt > 15) {
                    nReTxCnt = 15;
                }
                LOG.V("bleservice", "BlereTx 222 onWrite nReTxCnt=  " + nReTxCnt);
                if (nReTxCnt > 0) {
                    m_nmeaCPktStatistic.SetRETXPKT(nReTxCnt);
                    m_nmeaCPktStatistic.CNTRETXCNT();

                    byte[] reTxPktBuffer = new byte[nReTxCnt + 5];
                    reTxPktBuffer[0] = 126;
                    reTxPktBuffer[1] = (byte) (nReTxCnt + 5 & 0xFF);
                    reTxPktBuffer[2] = 2;
                    reTxPktBuffer[3] = (byte) (nReTxCnt & 0xFF);
                    System.arraycopy(reTxN, 0, reTxPktBuffer, 4, nReTxCnt);
                    reTxPktBuffer[(nReTxCnt + 4)] = 122;
                    nRepert += 1;
                    bisLastPkt = false;

                    gl_bOnreadpkt = true;
                    String ns = "";
                    for (int u = 0; u < reTxPktBuffer.length; ++u) {
                        ns = ns + Integer.toHexString(new Byte(reTxPktBuffer[u]).intValue());
                    }

                    BLETxAck(reTxPktBuffer);

                    long timePkt = System.currentTimeMillis();
                    String stimePkt = Long.toString(timePkt);

                    String sRet = "\r\n" + stimePkt + ",+++++ reTxPktBuffer ++++, ";

                    String ns1 = "";
                    for (int u = 0; u < reTxPktBuffer.length; ++u) {
                        ns1 = ns1 + Integer.toHexString(new Byte(reTxPktBuffer[u]).intValue());
                    }
                    sRet = sRet + ns1;
                    sRet = sRet + "\r\n";
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    gl_RecPrePktTime = System.currentTimeMillis();
                }
            }
        }
    }

    private class NmeaTxRunnable
            implements Runnable {
        private String TAG = "NmeaTxRunnable";

        public void run() {
            while (bNmeaTxRunnable) {
                try {
                    Thread.sleep(300L);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
                if (gl_RevDatatimes != 0L) {
                    LOG.V("", "NmeaTxRunnable --------------111");
                    long timeGap = System.currentTimeMillis() - gl_RevDatatimes;
                    if (timeGap >= 3000L) {
                        LOG.V("",
                                "NmeaTxRunnable --------------222==" + (System.currentTimeMillis() - gl_RevDatatimes));
                        BLeService.bleManager.disconnect();

                        sDateFormatTemp = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                        dateTemp = sDateFormatTemp.format(new Date());
                        strTemp = "\r\nTAG999,TimeRestartBleService----onread," + dateTemp +
                                "-in--NmeaTxRunnable---disconnect:" + BLeService.bleManager.getConnectStatus();

                        sDateFormatTemp = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                        dateTemp = sDateFormatTemp.format(new Date());
                        strTemp = "\r\nGPInitCntNmeaErr:" + timeGap;

                        LOG.V("bledisonnected", "bledisconnected," + strTemp);

                        gl_RevDatatimes = 0L;
                    }
                }

                CopyOnWriteArrayList ListList = DealNmeaListData(null, false);
                if (ListList != null) {
                    int nListListSize = ListList.size();
                    LOG.V("bleservice", "NmeaTxRunnable ListList.size() = " + nListListSize);
                    if (nListListSize == 0)
                        continue;
                    for (int index = 0; index < nListListSize; ++index)
                        BLEDealData((CopyOnWriteArrayList) ListList.get(index));
                }
            }
        }
    }

    private class RTCMRunnable
            implements Runnable {
        public void run() {
            while (bRtcmRunnable) {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }

                SdkData(null, false);
            }
        }
    }
}