package com.olinkstar.bdsviewin;

import android.os.Environment;
import android.os.StatFs;
import com.olinkstar.util.LOG;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public enum LocationDataManager
{
    INSTANSE;

    public boolean bdsExist = false;
    public boolean gpsExist = false;
    public boolean glsExist = false;
    public static final int MAX_LOCATIONS_SIZE = 7200;
    public static final int MAX_RSSI_SIZE = 7200;
    private MyLocation currentLocation = new MyLocation();
    private int currentRssi = 0;

    private CopyOnWriteArrayList<MySatellite> satellites = new CopyOnWriteArrayList();

    private String strGGA = "";

    private CopyOnWriteArrayList<MyLocation> locationCache = new CopyOnWriteArrayList();
    private ArrayList rssisCache = new ArrayList();

    private boolean located = false;
    private boolean satelliteFound = false;
    private double maxLat = 0.0D; private double minLat = 0.0D;
    private double maxLon = 0.0D; private double minLon = 0.0D;
    private double maxAlt = 0.0D; private double minAlt = 0.0D;

    private double maxBaseDelay = 0.0D; private double minBaseDelay = 0.0D;
    private double maxBaseLine = 0.0D; private double minBaseLine = 0.0D;
    protected static final String TAG = "TAGLocationDataManager";

    public double getAvgSNR()
    {
        int a = 0;
        double b = getAvgSnr();
        a = (int)Math.round(b);
        return a;
    }

    public double getAvgELV()
    {
        int a = 0;
        double b = getAvgElv();
        a = (int)Math.round(b);
        return a;
    }

    public MyLocation getLocation()
    {
        return this.currentLocation.clone();
    }

    public int getCurrentRSSI() {
        return this.currentRssi;
    }

    public CopyOnWriteArrayList<MyLocation> getLocations()
    {
        synchronized (this.locationCache)
        {
            return ((CopyOnWriteArrayList)this.locationCache.clone());
        }
    }

    public ArrayList getRSSI()
    {
        return ((ArrayList)this.rssisCache.clone());
    }

    public boolean isLocated()
    {
        return this.located;
    }

    public void updateRSSI(int rssi)
    {
        this.rssisCache.add(Integer.valueOf(rssi));
        this.currentRssi = rssi;

        if (this.rssisCache.size() >= 7200)
            this.rssisCache.remove(0);
    }

    public void updateLocation(MyLocation loc)
    {
        if (loc == null)
        {
            LOG.V("TAGLocationDataManager", "--updateLocation--------------meiyou-chuanru--");

            this.located = false;
            return;
        }

        this.located = true;
        this.currentLocation = loc.clone();
        this.locationCache.add(loc.clone());
        if (this.locationCache.size() >= 7200) {
            this.locationCache.remove(0);
        }

        if (loc.getLatitude() > this.maxLat)
            this.maxLat = loc.getLatitude();
        if (loc.getLatitude() < this.minLat)
            this.minLat = loc.getLatitude();
        if (loc.getLongitude() > this.maxLon)
            this.maxLon = loc.getLongitude();
        if (loc.getLongitude() < this.minLon)
            this.minLon = loc.getLongitude();
        if (loc.getAltitude() > this.maxAlt)
            this.maxAlt = loc.getAltitude();
        if (loc.getAltitude() < this.minAlt) {
            this.minAlt = loc.getAltitude();
        }
        if (loc.getBaseDelay() > this.maxBaseDelay)
            this.maxBaseDelay = loc.getBaseDelay();
        if (loc.getBaseDelay() < this.minBaseDelay) {
            this.minBaseDelay = loc.getBaseDelay();
        }
        if (loc.getBaseLine() > this.maxBaseLine)
            this.maxBaseLine = loc.getBaseLine();
        if (loc.getBaseLine() < this.minBaseLine)
            this.minBaseLine = loc.getBaseLine();
    }

    public void clearLocations()
    {
        this.locationCache.clear();
        this.rssisCache.clear();
        this.maxLat = 0.0D;
        this.minLat = 0.0D;
        this.maxLon = 0.0D;
        this.minLon = 0.0D;
        this.maxAlt = 0.0D;
        this.minAlt = 0.0D;
    }

    public double getMaxBaseDelay()
    {
        return this.maxBaseDelay;
    }

    public double getMinBaseDelay() {
        return this.minBaseDelay;
    }

    public double getMaxBaseLine()
    {
        return this.maxBaseLine;
    }

    public double getMinBaseLine()
    {
        return this.minBaseLine;
    }

    public double getMaxLatitude()
    {
        return this.maxLat;
    }

    public double getMinLatitude()
    {
        return this.minLat;
    }

    public double getMaxLongitude()
    {
        return this.maxLon;
    }

    public double getMinLongitude()
    {
        return this.minLon;
    }

    public double getMaxAltitude()
    {
        return this.maxAlt;
    }

    public double getMinAltitude()
    {
        return this.minAlt;
    }

    public CopyOnWriteArrayList<MySatellite> getSatellites()
    {
        return getSatellites(2, 1, 3);
    }

    public CopyOnWriteArrayList<MySatellite> getSatellites(int sys1, int sys2, int sys3)
    {
        return order(this.satellites, sys1, sys2, sys3);
    }

    public boolean isSatelliteFound()
    {
        return this.satelliteFound;
    }

    public void updateSatellites(CopyOnWriteArrayList<MySatellite> sats)
    {
        if (sats == null) {
            this.satelliteFound = false;
            LOG.V("TAGLocationDataManager", "-updateSatellites-----------meiyou-chuanru--");

            return;
        }

        this.satelliteFound = true;

        this.satellites = ((CopyOnWriteArrayList)sats.clone());

        sort();

        this.bdsExist = false;
        this.gpsExist = false;
        this.glsExist = false;
        for (MySatellite sat : sats)
            switch (sat.getSystem())
            {
                case 2:
                    this.bdsExist = true;
                    break;
                case 1:
                    this.gpsExist = true;
                    break;
                case 3:
                    this.glsExist = true;
            }
    }

    public String getGGA()
    {
        return this.strGGA;
    }

    protected void updateGGA(String gga)
    {
        if (gga == null)
            return;
        this.strGGA = gga;
    }

    private CopyOnWriteArrayList<MySatellite> order(CopyOnWriteArrayList<MySatellite> sat, int sys1, int sys2, int sys3)
    {
        CopyOnWriteArrayList result = new CopyOnWriteArrayList();

        int isys1 = 0; int isys2 = 0; int isys3 = 0;
        for (MySatellite s : sat) {
            if (s.getSystem() == sys1) {
                result.add(isys1++, s);
                ++isys2;
                ++isys3;
            } else if (s.getSystem() == sys2) {
                result.add(isys2++, s);
                ++isys3;
            } else if (s.getSystem() == sys3) {
                result.add(isys3++, s);
            } else {
                result.add(s);
            }
        }
        return result;
    }

    private void sort()
    {
        Comparator comparator = new Comparator()
        {
            public int compare(MySatellite lhs, MySatellite rhs)
            {
                return (lhs.getPrn() - rhs.getPrn());
            }

        };
        String satePrn = "";

        for (MySatellite s : this.satellites)
        {
            satePrn = satePrn + "," + s.getPrn();
        }

        List list = this.satellites;

        Object temp = new ArrayList(list);

        Collections.sort((List)temp, comparator);

        this.satellites.clear();
        for (Object stemp : (List)temp)
        {
            this.satellites.add((MySatellite)stemp);
        }

        String satePrn2 = "";

        for (MySatellite s2 : this.satellites)
            satePrn2 = satePrn2 + "," + s2.getPrn();
    }

    private double getAvgElv()
    {
        if (this.satellites.size() < 6)
        {
            LOG.V("TAGLocationDataManager", "--------satellites.size() < 6---------------");

            return 0.0D;
        }

        double[] temp = new double[60];
        double[] tempSnr = new double[60];

        int countSat = 0;
        for (MySatellite s : this.satellites) {
            temp[countSat] = s.getElevation();
            tempSnr[countSat] = s.getSnr();
            ++countSat;
        }

        for (int i = 0; i < countSat; ++i) {
            for (int j = i; j < countSat; ++j) {
                if (temp[i] < temp[j]) {
                    double t = temp[i];
                    temp[i] = temp[j];
                    temp[j] = t;

                    double tSnr = tempSnr[i];
                    tempSnr[i] = tempSnr[j];
                    tempSnr[j] = tSnr;
                }

            }

        }

        double avg = 0.0D;
        for (int i = 0; i < 6; ++i)
        {
            avg += tempSnr[i];
        }

        avg /= 6.0D;
        LOG.V("TAGLocationDataManager", " ----avg-avg-----ELV------------" + avg);

        return avg;
    }

    private double getAvgSnr()
    {
        if (this.satellites.size() < 6)
        {
            return 0.0D;
        }

        double[] temp = new double[60];
        int countSat = 0;
        for (MySatellite s : this.satellites) {
            temp[countSat] = s.getSnr();
            ++countSat;
        }

        for (int i = 0; i < countSat; ++i) {
            for (int j = i; j < countSat; ++j) {
                if (temp[i] < temp[j]) {
                    double t = temp[i];
                    temp[i] = temp[j];
                    temp[j] = t;
                }

            }

        }

        double avg = 0.0D;
        for (int i = 0; i < 6; ++i)
        {
            avg += temp[i];
        }

        avg /= 6.0D;
        LOG.V("TAGLocationDataManager", " ----avg-avg------SNR-----------" + avg);

        return avg;
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