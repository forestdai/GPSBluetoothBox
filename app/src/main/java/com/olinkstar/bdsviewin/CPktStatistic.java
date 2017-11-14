package com.olinkstar.bdsviewin;

import android.os.Environment;
import android.os.StatFs;
import com.olinkstar.util.LOG;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CPktStatistic
{
    private String FILE_BUFFER = "FILEBUFFER";
    private String FILE_BUFFER2 = "FILEBUFFER2";
    private String utcTime = "1402993039";
    private String m_sFinetime = null;
    private long m_Time;
    private int m_INIPKT;
    private int m_PKTS;
    private int m_PKT;
    private int m_LOSSPKTS;
    private int m_LOSSPKT;
    private int m_RETXPKT;
    private int m_RETXCNT;
    private int m_REVRETXPKT;
    private int m_INIPKTACK;
    private int m_DATAPKTSACK;
    private int m_DATAACKACK;
    private int m_RETXACK;
    private int m_LOSSACK;
    private int m_WillPKT;
    private int m_nTimeoutCnt;
    private int m_nTimeoutCnt2;
    private int m_nBLERSSI;
    private int m_nTxDataFailCnt;
    private int m_nTxDataCnt;

    public static void makeRootDirectory(String filePath)
    {
        File file = null;
        try {
            file = new File(filePath);
            if (!(file.exists()))
                file.mkdir();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File makeFilePath(String filePath, String fileName)
    {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!(file.exists()))
                file.createNewFile();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    public void writeTxtToFile(String strcontent, String filePath, String fileName)
    {
        makeFilePath(filePath, fileName);
        String strFilePath = filePath + fileName;

        String strContent = strcontent + "\r\n";
        try {
            File file = new File(strFilePath);
            if (!(file.exists())) {
                LOG.V("TestFile", "Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            try
            {
                OutputStream outStream = new FileOutputStream(file, true);
                outStream.write(strContent.getBytes());
                outStream.flush();
                outStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (Exception e) {
            LOG.V("TestFile", "Error on write File:" + e);
        }
    }

    public void writeTxtToFile(byte[] strcontent, String filePath, String fileName)
    {
        makeFilePath(filePath, fileName);
        String strFilePath = filePath + fileName;

        String strContent = strcontent + "\r\n";
        try {
            File file = new File(strFilePath);
            if (!(file.exists())) {
                LOG.V("TestFile", "Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            try
            {
                OutputStream outStream = new FileOutputStream(file, true);
                outStream.write(strcontent);
                outStream.flush();
                outStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (Exception e) {
            LOG.V("TestFile", "Error on write File:" + e);
        }
    }

    public boolean SaveBuffer(String buffer)
    {
        int i = 0;
        if (i == 0) {
            return true;
        }
        if (this.m_sFinetime.equals(""))
            return false;
        if (buffer.length() <= 0) {
            return false;
        }
        File sdCardDir1 = Environment.getExternalStorageDirectory();
        String sDir = sdCardDir1.getPath();
        String filePath = sDir + "/APPDATA/PktStatistic/";

        long fressSize = getSDFreeSize();
        if (fressSize > 0L)
        {
            writeTxtToFile(buffer, filePath, this.m_sFinetime);
        }

        return true;
    }

    public void SetFileNmae(String sFinetime) {
        this.m_sFinetime = sFinetime;
    }

    public void SetTime(long time) {
        this.m_Time = time;
        Date date = new Date();
        SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.utcTime = sdformat.format(date);
    }

    public void SetINIPKT(int INIPKT) {
        LOG.V("CNTINIPKT", "SetINIPKT m_INIPKT " + INIPKT);
        this.m_INIPKT = INIPKT;
        LOG.V("CNTINIPKT", "SetINIPKT m_INIPKT 222 " + this.m_INIPKT);
    }

    public void SetPerPKTS(int PKTS) {
        this.m_PKTS += PKTS;
    }

    public void SetPKT(int PKT) {
        this.m_PKT += PKT;
    }

    public void SetLOSSPKTS(int LOSSPKTS) {
        this.m_LOSSPKTS += LOSSPKTS;
    }

    public void SetLOSSPKT(int LOSSPKT) {
        this.m_LOSSPKT += LOSSPKT;
    }

    public void SetRETXPKT(int RETXPKT) {
        this.m_RETXPKT += RETXPKT;
    }

    public void SetRETXCNT(int RETXCNT) {
        this.m_RETXCNT = RETXCNT;
    }

    public void SetINIPKTACK(int INIPKTACK) {
        this.m_INIPKTACK = INIPKTACK;
    }

    public void SetDATAPKTSACK(int DATAPKTSACK) {
        this.m_DATAPKTSACK = DATAPKTSACK;
    }

    public void SetDATAACKACK(int DATAACKACK) {
        this.m_DATAACKACK = DATAACKACK;
    }

    public void SetRETXACK(int RETXACK) {
        this.m_RETXACK = RETXACK;
    }

    public void SetLOSSACK(int LOSSACK) {
        this.m_LOSSACK = LOSSACK;
    }

    public void CNTINIPKT() {
        LOG.V("CNTINIPKT", "CNTINIPKT m_INIPKT " + this.m_INIPKT);
        this.m_INIPKT += 1;
    }

    public void CNTPerPKTS() {
        this.m_PKTS += 1;
    }

    public void CNTTxDataFailCnt() {
        this.m_nTxDataFailCnt += 1;
    }

    public void CNTTxDataCnt() {
        this.m_nTxDataCnt += 1;
    }

    public void CNTSetPKT() {
        this.m_PKT += 1;
    }

    public void CNTWillPKT(int nWillPkt) {
        this.m_WillPKT += nWillPkt;
    }

    public void CNRTTXPKT() {
        this.m_RETXPKT += 1;
    }

    public void CNTRETXCNT() {
        this.m_RETXCNT += 1;
    }

    public void CNTINIPKTACK() {
        this.m_INIPKTACK += 1;
    }

    public void CNTDATAPKTSACK() {
        this.m_DATAPKTSACK += 1;
    }

    public void CNTDATAACKACK() {
        this.m_DATAACKACK += 1;
    }

    public void CNTRETXACK() {
        this.m_RETXACK += 1;
    }

    public void CNTREVRETXPKT() {
        this.m_REVRETXPKT += 1;
    }

    public void CNTTIMEOUT() {
        this.m_nTimeoutCnt += 1;
    }

    public void CNTTIMEOUT2() {
        this.m_nTimeoutCnt2 += 1;
    }

    public void SetBleRssi(int nBLERSSI) {
        this.m_nBLERSSI = nBLERSSI;
    }

    void ClearStatistic() {
        this.m_Time = 0L;
        this.m_INIPKT = 0;
        this.m_PKTS = 0;
        this.m_PKT = 0;
        this.m_LOSSPKTS = 0;
        this.m_LOSSPKT = 0;
        this.m_RETXPKT = 0;
        this.m_RETXCNT = 0;
        this.m_REVRETXPKT = 0;
        this.m_INIPKTACK = 0;
        this.m_DATAPKTSACK = 0;
        this.m_DATAACKACK = 0;
        this.m_RETXACK = 0;
        this.m_LOSSACK = 0;
        this.m_WillPKT = 0;
        this.m_nTimeoutCnt = 0;
        this.m_nTimeoutCnt2 = 0;
        this.m_nBLERSSI = 0;
        this.m_nTxDataFailCnt = 0;
        this.m_nTxDataCnt = 0;
    }

    public void SaveStatistic(int nType) {
        synchronized (this.FILE_BUFFER2)
        {
            String sBuffer = "";
            if (nType == 0) {
                LOG.V("SaveStatistic", "SaveStatistic nmea ");
                sBuffer = sBuffer + "Nmea:|";
                sBuffer = sBuffer + "TIME:" + this.m_Time + "|" +
                        "data:" + this.utcTime + "|" +
                        "REVINIPKT:" + this.m_INIPKT + "|" +
                        "REVPKTS:" + this.m_PKTS + "|" +
                        "REVPKT:" + this.m_PKT + "|" +
                        "REVLOSSPKTS:" + (this.m_INIPKT - this.m_PKTS) + "|" +
                        "REVLOSSPKT:" + (this.m_WillPKT - this.m_PKT) + "|" +
                        "RETXPKT:" + this.m_RETXPKT + "|" +
                        "RETXCNT:" + this.m_RETXCNT + "|" +
                        "TXINIPKTACK:" + this.m_INIPKTACK + "|" +
                        "TXDATAPKTSACK:" + this.m_RETXACK + "|" +
                        "REVDATAACKACK:" + this.m_DATAACKACK + "|" +
                        "TXLOSSDataACK:" + (this.m_RETXACK - this.m_DATAACKACK) + "|" +
                        "TXDataCnt:" + this.m_nTxDataCnt + "|" +
                        "TXDataFailCnt:" + this.m_nTxDataFailCnt + "|" +
                        "CHECKRETXTIMEOUT:" + this.m_nTimeoutCnt + "|" +
                        "CHECKRETXTIMEOUT2:" + this.m_nTimeoutCnt2 + "|" +
                        "BLERSSI:" + this.m_nBLERSSI + "|" +
                        "\r\n";
            } else {
                LOG.V("SaveStatistic", "SaveStatistic RTCM ");
                sBuffer = sBuffer + "RTCM:|";
                sBuffer = sBuffer + "TIME:" + this.m_Time + "|" +
                        "data:" + this.utcTime + "|" +
                        "TXINIPKT:" + this.m_INIPKT + "|" +
                        "TXPKTS:" + this.m_PKTS + "|" +
                        "TXPKT:" + this.m_PKT + "|" +
                        "TXRETXPKT:" + this.m_REVRETXPKT + "|" +
                        "REVINIPKTACK:" + this.m_INIPKTACK + "|" +
                        "REVDATAACK:" + this.m_DATAPKTSACK + "|" +
                        "REVRETXPKT:" + this.m_RETXPKT + "|" +
                        "REVRETXCNT:" + this.m_RETXCNT + "|" +
                        "TXDATAACKACK:" + this.m_DATAACKACK + "|" +
                        "TXDataCnt:" + this.m_nTxDataCnt + "|" +
                        "TXDataFailCnt:" + this.m_nTxDataFailCnt + "|" +
                        "CHECKRETXTIMEOUT:" + this.m_nTimeoutCnt + "|" +
                        "CHECKRETXTIMEOUT2:" + this.m_nTimeoutCnt2 + "|" +
                        "\r\n";
            }
            SaveBuffer(sBuffer);
        }
    }

    public long getSDFreeSize()
    {
        File path = Environment.getExternalStorageDirectory();
        StatFs sf = new StatFs(path.getPath());

        long blockSize = sf.getBlockSize();

        long freeBlocks = sf.getAvailableBlocks();

        return (freeBlocks * blockSize / 1024L / 1024L);
    }

    public long getSDAllSize()
    {
        File path = Environment.getExternalStorageDirectory();
        StatFs sf = new StatFs(path.getPath());

        long blockSize = sf.getBlockSize();

        long allBlocks = sf.getBlockCount();

        return (allBlocks * blockSize / 1024L / 1024L);
    }
}