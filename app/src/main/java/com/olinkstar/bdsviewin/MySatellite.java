package com.olinkstar.bdsviewin;

import android.location.GpsSatellite;

public class MySatellite
{
    private double azimuth = -1.0D;
    private double elevation = -1.0D;
    private int prn = -1;
    private double snr = -1.0D;
    private boolean used = false;

    private int system = 0;
    public static final int OTHER = 0;
    public static final int GPS = 1;
    public static final int BDS = 2;
    public static final int GLS = 3;

    public MySatellite()
    {
    }

    public MySatellite(GpsSatellite satellite)
    {
        this.azimuth = satellite.getAzimuth();
        this.elevation = satellite.getElevation();
        this.snr = satellite.getSnr();

        this.system =
                ((((satellite.getPrn() >= 1) && (satellite.getPrn() <= 32)) ? 1 : 0) +
                        ((satellite.getPrn() >= 200) ? 2 : 0) +
                        (((satellite.getPrn() >= 65) && (satellite.getPrn() <= 88)) ? 3 : 0));

        switch (this.system)
        {
            case 1:
                this.prn = satellite.getPrn();
                break;
            case 2:
                this.prn = (satellite.getPrn() - 200);
                break;
            case 3:
                this.prn = (satellite.getPrn() - 64);
        }

        this.used = satellite.usedInFix();
    }

    public void setSatellite(GpsSatellite satellite)
    {
        this.azimuth = satellite.getAzimuth();
        this.elevation = satellite.getElevation();
        this.snr = satellite.getSnr();

        this.system =
                ((((satellite.getPrn() >= 1) && (satellite.getPrn() <= 32)) ? 1 : 0) +
                        ((satellite.getPrn() >= 200) ? 2 : 0) +
                        (((satellite.getPrn() >= 65) && (satellite.getPrn() <= 88)) ? 3 : 0));

        switch (this.system)
        {
            case 2:
                this.prn = (satellite.getPrn() - 200);
                break;
            case 3:
                this.prn = (satellite.getPrn() - 64);
        }
    }

    public double getAzimuth()
    {
        return this.azimuth; }

    public void setAzimuth(double t) {
        this.azimuth = t;
    }

    public double getElevation()
    {
        return this.elevation; }

    public void setElevation(double t) {
        this.elevation = t;
    }

    public int getPrn()
    {
        return this.prn; }

    public void setPrn(int t) {
        this.system =
                ((((t >= 1) && (t <= 32)) ? 1 : 0) +
                        ((t >= 200) ? 2 : 0) +
                        (((t >= 65) && (t <= 88)) ? 3 : 0));

        this.prn = t;
    }

    public double getSnr()
    {
        return this.snr; }

    public void setSnr(double t) {
        this.snr = t;
    }

    public boolean usedInFix()
    {
        return this.used; }

    public void setUsedInFix(boolean t_used) {
        this.used = t_used;
    }

    public int getSystem()
    {
        return this.system;
    }
}