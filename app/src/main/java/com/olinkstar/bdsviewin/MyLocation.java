package com.olinkstar.bdsviewin;

import android.location.Location;
import com.olinkstar.util.LOG;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MyLocation
{
    private double latitude = -1.0D;
    private double longitude = -1.0D;
    private double altitude = -10911.0D;
    private double speed = -1.0D;
    private double bearing = -1.0D;
    private double accuracy = -1.0D;
    private double horizon_accuracy = -1.0D;
    private double vertical_accuracy = -1.0D;
    private int mode = 0;
    private int[] gps_in_rtk = new int[32];

    private int[] bds_in_rtk = new int[32];

    private double pdop = -1.0D;
    private double vdop = -1.0D;
    private double hdop = -1.0D;

    private int satInView = 0;

    private int baseDelay = 0;
    private int baseLine = 0;

    private int baseGPSN = 0;
    private int baseBDSN = 0;
    private int baseElvGpsSnr = 0;
    private int baseElvBdsSnr = 0;

    private int roverGPSN = 0;
    private int roverBDSN = 0;
    private int roverElvGpsSnr = 0;
    private int roverElvBdsSnr = 0;

    private int baseDiffRTCM = 0;

    private int roverUsedGPSN = 0;
    private int roverUsedBDSN = 0;
    public static final String TAG = "LocationDataManager";

    public MyLocation()
    {
    }

    public MyLocation(Location location)
    {
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.altitude = location.getAltitude();
        this.speed = location.getSpeed();
        this.bearing = location.getBearing();
        this.accuracy = location.getAccuracy();
    }

    public void setLocation(Location location)
    {
        if (location == null)
            return;
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.altitude = location.getAltitude();
        this.speed = location.getSpeed();
        this.bearing = location.getBearing();
        this.accuracy = location.getAccuracy();
    }

    public MyLocation clone()
    {
        MyLocation locClone = new MyLocation();

        locClone.latitude = this.latitude;
        locClone.longitude = this.longitude;
        locClone.altitude = this.altitude;
        locClone.bearing = this.bearing;
        locClone.speed = this.speed;
        locClone.accuracy = this.accuracy;
        locClone.horizon_accuracy = this.horizon_accuracy;
        locClone.vertical_accuracy = this.vertical_accuracy;
        locClone.mode = this.mode;
        locClone.satInView = this.satInView;
        locClone.pdop = this.pdop;
        locClone.vdop = this.vdop;
        locClone.hdop = this.hdop;

        locClone.baseDelay = this.baseDelay;
        locClone.baseLine = this.baseLine;

        locClone.baseGPSN = this.baseGPSN;
        locClone.baseBDSN = this.baseBDSN;
        locClone.baseElvGpsSnr = this.baseElvGpsSnr;
        locClone.baseElvBdsSnr = this.baseElvBdsSnr;

        locClone.roverGPSN = this.roverGPSN;
        locClone.roverBDSN = this.roverBDSN;
        locClone.roverElvGpsSnr = this.roverElvGpsSnr;
        locClone.roverElvBdsSnr = this.roverElvBdsSnr;

        locClone.baseDiffRTCM = this.baseDiffRTCM;

        locClone.roverUsedGPSN = this.roverUsedGPSN;
        locClone.roverUsedBDSN = this.roverUsedBDSN;

        return locClone;
    }

    public double getLatitude()
    {
        return this.latitude;
    }

    public double getLatitude(int precis)
    {
        NumberFormat nf = DecimalFormat.getInstance();
        nf.setMaximumFractionDigits(precis);
        nf.setMinimumFractionDigits(precis);
        return Double.parseDouble(nf.format(this.latitude));
    }

    public void setLatitude(double lat)
    {
        this.latitude = lat;
    }

    public double getLongitude()
    {
        return this.longitude;
    }

    public double getLongitude(int precis)
    {
        NumberFormat nf = DecimalFormat.getInstance();
        nf.setMaximumFractionDigits(precis);
        nf.setMinimumFractionDigits(precis);

        LOG.V("LocationDataManager", "----Mylocation----getLongitude-----longitude:" + this.longitude);

        return Double.parseDouble(nf.format(this.longitude));
    }

    public void setLongitude(double lon)
    {
        this.longitude = lon;
    }

    public double getAltitude()
    {
        return this.altitude;
    }

    public double getAltitude(int precis)
    {
        return ((int)(this.altitude * Math.pow(10.0D, precis)) / Math.pow(10.0D, precis));
    }

    public void setAltitude(double alt)
    {
        this.altitude = alt;
    }

    public double getBearing()
    {
        return this.bearing;
    }

    public double getBearing(int precis)
    {
        return ((int)(this.bearing * Math.pow(10.0D, precis)) / Math.pow(10.0D, precis));
    }

    public void setBearing(double ber)
    {
        this.bearing = ber;
    }

    public double getSpeed()
    {
        return (this.speed * 1852.0D * 0.0D);
    }

    public double getORGSpeed()
    {
        return this.speed;
    }

    public double getSpeed(int precis)
    {
        return ((int)(this.speed * 1852.0D * 0.0D * Math.pow(10.0D, precis)) / Math.pow(10.0D, precis));
    }

    public double getSpeedWithKmh()
    {
        return (this.speed * 1.852D);
    }

    public double getSpeedWithKmh(int precis)
    {
        return ((int)(this.speed * 1.852D * Math.pow(10.0D, precis)) / Math.pow(10.0D, precis));
    }

    public void setSpeed(double spd)
    {
        this.speed = spd;
    }

    public double getAccuracy()
    {
        return this.accuracy;
    }

    public double getAccuracy(int precis)
    {
        return ((int)(this.accuracy * Math.pow(10.0D, precis)) / Math.pow(10.0D, precis));
    }

    public double getHorizonAccuracy()
    {
        return this.horizon_accuracy;
    }

    public double getHorizonAccuracy(int precis)
    {
        return ((int)(this.horizon_accuracy * Math.pow(10.0D, precis)) / Math.pow(10.0D, precis));
    }

    public double getVerticalAccuracy()
    {
        return this.vertical_accuracy;
    }

    public double getVerticalAccuracy(int precis)
    {
        return ((int)(this.vertical_accuracy * Math.pow(10.0D, precis)) / Math.pow(10.0D, precis));
    }

    public void setAccuracy(double epe)
    {
        this.accuracy = epe;
    }

    public void setHorizonAccuracy(double hepe)
    {
        this.horizon_accuracy = hepe;
    }

    public void setVerticalAccuracy(double vepe) {
        this.vertical_accuracy = vepe;
    }

    public int getDiffRTCM()
    {
        return this.baseDiffRTCM;
    }

    public int getBaseDelay()
    {
        return this.baseDelay;
    }

    public int getBaseLine()
    {
        return this.baseLine;
    }

    public int getBaseGPSN()
    {
        return this.baseGPSN;
    }

    public int getBaseBDSN()
    {
        return this.baseBDSN;
    }

    public int getBaseElvGpsSnr()
    {
        return this.baseElvGpsSnr;
    }

    public int getBaseElvBdsSnr()
    {
        return this.baseElvBdsSnr;
    }

    public int getRoverGPSN()
    {
        return this.roverGPSN;
    }

    public int getRoverBDSN()
    {
        return this.roverBDSN;
    }

    public int getRoverUsedGPSN()
    {
        return this.roverUsedGPSN;
    }

    public int getRoverUsedBDSN()
    {
        return this.roverUsedBDSN;
    }

    public int getRoverElvGpsSnr()
    {
        return this.roverElvGpsSnr;
    }

    public int getRoverElvBdsSnr()
    {
        return this.roverElvBdsSnr;
    }

    public int getMode()
    {
        return this.mode;
    }

    public int getSatInView()
    {
        return this.satInView;
    }

    public void setMode(int m)
    {
        this.mode = m;
    }

    public void setBaseDelay(int b)
    {
        this.baseDelay = b;
    }

    public void setBaseLine(int b)
    {
        this.baseLine = b;
    }

    public void setBaseGPSN(int b)
    {
        this.baseGPSN = b;
    }

    public void setBaseBDSN(int b)
    {
        this.baseBDSN = b;
    }

    public void setBaseElvGpsSnr(int b)
    {
        this.baseElvGpsSnr = b;
    }

    public void setBaseElvBdsSnr(int b)
    {
        this.baseElvBdsSnr = b;
    }

    public void setRoverGPSN(int b)
    {
        this.roverGPSN = b;
    }

    public void setRoverBDSN(int b)
    {
        this.roverBDSN = b;
    }

    public void setRoverUsedGPSN(int b)
    {
        this.roverUsedGPSN = b;
    }

    public void setRoverUsedBDSN(int b)
    {
        this.roverUsedBDSN = b;
    }

    public void setRoverElvGpsSnr(int b)
    {
        this.roverElvGpsSnr = b;
    }

    public void setRoverElvBdsSnr(int b)
    {
        this.roverElvBdsSnr = b;
    }

    public void setBaseDiffRTCM(int b)
    {
        this.baseDiffRTCM = b;
    }

    public void setSatInView(int v)
    {
        this.satInView = v;
    }

    public double getPdop()
    {
        return this.pdop;
    }

    public void setPdop(double p)
    {
        this.pdop = p;
    }

    public double getVdop()
    {
        return this.vdop;
    }

    public void setVdop(double v)
    {
        this.vdop = v;
    }

    public double getHdop()
    {
        return this.hdop;
    }

    public void setHdop(double h)
    {
        this.hdop = h;
    }
}