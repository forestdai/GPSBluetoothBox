package com.olinkstar.bdsviewin;

import android.os.Environment;
import android.os.StatFs;
import com.olinkstar.util.LOG;
import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

public enum SrcManager
{
    INSTANCE;

    public static final String TAG = "SrcManager";
    public static final int SRC_SYSTEM = 0;
    public static final int SRC_BLUETOOTH = 1;
    public static final int SRC_NETWORK = 2;
    private int srcId = 0;
    public static CopyOnWriteArrayList<MySatellite> data_last;

    public int getSrcId()
    {
        return this.srcId;
    }

    public boolean changeTo(int id)
    {
        switch (id)
        {
            case 0:
                this.srcId = 0;
                return true;
            case 1:
                this.srcId = 1;
                return true;
            case 2:
                this.srcId = 2;
                return true;
        }
        return false;
    }

    public boolean sendLocation(MyLocation data, int id)
    {
        if (this.srcId != id) {
            LOG.V("sendLocation", "srcid " + this.srcId + " sendid " + id);
            return false;
        }
        LocationDataManager.INSTANSE.updateLocation(data);
        return true;
    }

    public boolean sendSatellites(CopyOnWriteArrayList<MySatellite> data, int id, boolean[] hasGSV)
    {
        if (this.srcId != id) {
            LOG.V("sendSatellites", "srcid " + this.srcId + " sendid " + id);
            return false;
        }

        if (hasGSV[0] != 0) {
            LocationDataManager.INSTANSE.updateSatellites(data);

            data_last = data;
        }
        else {
            LocationDataManager.INSTANSE.updateSatellites(data_last);
        }

        return true;
    }

    public boolean sendGGA(String gga, int id)
    {
        if (this.srcId != id)
            return false;
        LocationDataManager.INSTANSE.updateGGA(gga);
        return true;
    }

    public long getSDFreeSize()
    {
        File path = Environment.getExternalStorageDirectory();
        StatFs sf = new StatFs(path.getPath());

        long blockSize = sf.getBlockSize();

        long freeBlocks = sf.getAvailableBlocks();

        return (freeBlocks * blockSize / 1024L / 1024L);
    }
}