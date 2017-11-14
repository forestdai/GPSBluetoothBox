package com.olinkstar.bdsviewin;

import android.util.Log;
import com.olinkstar.util.Buffer;
import com.olinkstar.util.LOG;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

public class NmeaAnalyzer
{
    public static final String TAG = "NmeaAnalyzer";
    public static final String TAG99 = "netBleTest";
    public static final String TAG5 = "NmeaAnalyzer5";
    public int srcId = 0;
    private double latitude = -1.0D;
    private double longitude = -1.0D;
    private double altitude = -10911.0D;

    private double speed = -1.0D;
    private double bearing = -1.0D;

    private double bearingBGI = -1.0D;
    private String strUtcTime;
    private int mode = 0;
    private double dop;
    private int u_stars = 0;

    private double hEPE = -1.0D;
    private double wEPE = -1.0D;

    private double pdop = -1.0D;
    private double vdop = -1.0D;
    private double hdop = -1.0D;

    private String latitudeNS = "";
    private String longitudeEW = "";

    private CopyOnWriteArrayList<MySatellite> satellites = new CopyOnWriteArrayList();
    private CopyOnWriteArrayList<MySatellite> t_satellites = new CopyOnWriteArrayList();

    private String strGGA = "";
    private int[] usedInFixTable = new int[500];

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

    private int roverUsedGPSN = 0;
    private int roverUsedBDSN = 0;
    private double sdkAccuracy;

    public NmeaAnalyzer(int id)
    {
        this.srcId = id;
    }

    boolean CheckSum(byte[] buff)
    {
        if ((buff == null) || (buff.length < 5)) {
            return false;
        }

        byte result = 0;

        result = buff[1]; for (int i = 2; (i < buff.length) && (buff[i] != 42); ++i) {
        result = (byte)(result ^ buff[i]);
    }

        String sCheck = Integer.toHexString(result).toUpperCase();

        byte[] hh = new byte[2];

        if ((result & 0xF0) != 0) {
            hh[0] = sCheck.getBytes()[0];
            hh[1] = sCheck.getBytes()[1];
        } else {
            hh[0] = 48;
            hh[1] = sCheck.getBytes()[0];
        }

        if (i + 4 != buff.length - 1) {
            return false;
        }

        if ((buff[i] != 42) || (buff[(i + 3)] != 13) || (buff[(i + 4)] != 10)) {
            return false;
        }

        return ((hh[0] == buff[(i + 1)]) && (hh[1] == buff[(i + 2)]));
    }

    public boolean analyze(String nmea, NmeaCallback callback)
    {
        boolean bSend;
        boolean isASCIIEncoded = false;

        if ((nmea.startsWith("$GP")) || (nmea.startsWith("$GN")) || (nmea.startsWith("$BD")) || (nmea.startsWith("$GB"))) {
            isASCIIEncoded = true;

            if (!(CheckSum(nmea.getBytes())))
                return isASCIIEncoded;
        } else {
            return isASCIIEncoded;
        }

        if (nmea.length() < 6) {
            return isASCIIEncoded;
        }
        if ((nmea.startsWith("$GPGGA")) || (nmea.startsWith("$GNGGA")))
        {
            BLeService.flagGPGGA = true;
            BLeService.nmeaGPGGA = nmea;
            bSend = analyzeGGA(nmea);
            if (bSend)
                callback.onNmeaOneBroadcast(nmea);
        }
        else if (nmea.substring(3, 6).equals("GLL"))
        {
            bSend = analyzeGLL(nmea);
            if (bSend)
                callback.onNmeaOneBroadcast(nmea);
        }
        else if (nmea.substring(3, 6).equals("GSA"))
        {
            bSend = analyzeGSA(nmea);
            if (bSend)
                callback.onNmeaOneBroadcast(nmea);
        }
        else if (nmea.substring(3, 6).equals("GSV"))
        {
            BLeService.flagXXGSV = true;
            bSend = analyzeGSV(nmea);
            if (bSend)
                callback.onNmeaOneBroadcast(nmea);
        }
        else if (nmea.substring(3, 6).equals("RMC"))
        {
            BLeService.flagXXRMC = true;
            bSend = analyzeRMC(nmea);
            if (bSend)
                callback.onNmeaOneBroadcast(nmea);
        }
        else if (nmea.startsWith("$GNBGI"))
        {
            Log.d("GNBGI", "解析方位角");
            bSend = analyzeBGI(nmea);
            if (bSend)
                callback.onNmeaOneBroadcast(nmea);
        }
        else if (nmea.startsWith("$GNEPE"))
        {
            bSend = analyzeEPE(nmea);
            if (bSend) {
                Log.d("SDKGNEPE", "SDKGNEPE: =" + nmea.toString() + "\n:hepe " + this.hEPE + " wepe = " + this.wEPE);
                callback.onNmeaOneBroadcast(nmea);
            }
        } else if (nmea.startsWith("$GNRDK"))
        {
            bSend = analyzeRDK(nmea);
            if (bSend)
                callback.onNmeaOneBroadcast(nmea);
        }
        else if (nmea.startsWith("$GNPPS"))
        {
            bSend = analyzePPS(nmea);
            if (bSend) {
                callback.onNmeaOneBroadcast(nmea);
            }
        }

        if ((BLeService.flagGPGGA) && (BLeService.flagXXRMC))
        {
            this.strGGA = BLeService.nmeaGPGGA;

            this.satellites = ((CopyOnWriteArrayList)this.t_satellites.clone());

            this.usedInFixTable = new int[500];
            this.t_satellites.clear();

            if (BLeService.flagXXGSV) {
                BLeService.hasGSV[0] = true;
                callback.onNmeaUpdate(BLeService.hasGSV);
            }
            else {
                BLeService.hasGSV[0] = false;
                callback.onNmeaUpdate(BLeService.hasGSV);
            }

            BLeService.flagGPGGA = false;
            BLeService.flagXXGSV = false;
            BLeService.flagXXRMC = false;

            BLeService.nmeaGPGGA = "";
        }

        return isASCIIEncoded;
    }

    public String getUtcDateTime() {
        return this.strUtcTime;
    }

    public int getBaseDelay()
    {
        return this.baseDelay;
    }

    public double getSdkAccuracy()
    {
        return this.sdkAccuracy;
    }

    public int getBaseLine()
    {
        return this.baseLine;
    }

    public int getBaseGPSN() {
        return this.baseGPSN;
    }

    public int getBaseBDSN() {
        return this.baseBDSN;
    }

    public int getBaseElvGpsSnr() {
        return this.baseElvGpsSnr;
    }

    public int getBaseElvBdsSnr() {
        return this.baseElvBdsSnr;
    }

    public int getRoverGPSN() {
        return this.roverGPSN;
    }

    public int getRoverBDSN() {
        return this.roverBDSN;
    }

    public int getRoverElvGpsSnr() {
        return this.roverElvGpsSnr;
    }

    public int getRoverElvBdsSnr() {
        return this.roverElvBdsSnr;
    }

    public int getRoverUsedGPSN() {
        return this.roverUsedGPSN;
    }

    public int getRoverUsedBDSN() {
        return this.roverUsedBDSN;
    }

    public double getLatitude()
    {
        if (this.latitudeNS.equals("N")) {
            return this.latitude;
        }

        return (this.latitude * -1.0D);
    }

    public String getLatitudeNS()
    {
        return this.latitudeNS;
    }

    public String getLongitudeEW()
    {
        return this.longitudeEW;
    }

    public int getUstars()
    {
        return this.u_stars;
    }

    public double getLongitude()
    {
        if (this.longitudeEW.equals("E")) {
            return this.longitude;
        }
        return (this.longitude * -1.0D);
    }

    public double getAltitude()
    {
        return this.altitude;
    }

    public double getSpeed()
    {
        return this.speed;
    }

    public double getBearing()
    {
        return this.bearing;
    }

    public int getMode()
    {
        return this.mode;
    }

    public double getDop()
    {
        return this.dop;
    }

    public double getPdop()
    {
        return this.pdop;
    }

    public double getVdop()
    {
        return this.vdop;
    }

    public double getHdop()
    {
        return this.hdop;
    }

    public double getHepe() {
        return this.hEPE;
    }

    public double getWepe() {
        return this.wEPE;
    }

    public void resetHWepe() {
        this.hEPE = -1.0D;
        this.wEPE = -1.0D;
    }

    public CopyOnWriteArrayList<MySatellite> getSatellites()
    {
        return ((CopyOnWriteArrayList)this.satellites.clone());
    }

    public String getUtcTime()
    {
        return this.strUtcTime;
    }

    public String getGGA()
    {
        return this.strGGA;
    }

    private int analyzeMode(String nmea)
    {
        int index = 0;

        int mode2 = 0;
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < nmea.length(); ++i) {
            if ((((nmea.charAt(i) == ',') ? 1 : 0) | ((nmea.charAt(i) == '*') ? 1 : 0) | ((i == nmea.length() - 1) ? 1 : 0)) != 0) {
                if (!(str.toString().equals(""))) {
                    if (6 == index)
                    {
                        try {
                            mode2 = Integer.parseInt(str.toString());
                        }
                        catch (NumberFormatException e) {
                            mode2 = 0;

                            LOG.E("NmeaAnalyzer", "Illegal mode: " + str);
                        }
                        return mode2;
                    }
                }
                else
                {
                    if (nmea.startsWith("$GPGGA"))
                    {
                        mode2 = 0;
                        LOG.D("NmeaAnalyzer", "-GPGGA-mode-kong--: " + mode2);
                    }
                    else if (nmea.startsWith("$GNGGA"))
                    {
                        mode2 = 0;
                        LOG.D("NmeaAnalyzer", "-GNGGA-mode-kong--: " + mode2);
                    }

                    mode2 = 0;

                    return mode2;
                }

                ++index;
                str.delete(0, str.length());
            }
            else {
                str.append(nmea.charAt(i));
            }

        }

        return 0;
    }

    private boolean analyzeGGA(String nmea)
    {
        boolean bSend = true;
        int index = 0;
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < nmea.length(); ++i) {
            if ((((nmea.charAt(i) == ',') ? 1 : 0) | ((nmea.charAt(i) == '*') ? 1 : 0) | ((i == nmea.length() - 1) ? 1 : 0)) != 0) {
                if (!(str.toString().equals(""))) {
                    switch (index) {
                        case 2:
                            try {
                                this.latitude = Double.parseDouble(str.toString());

                                if (this.latitude > 500.0D)
                                    this.latitude = Buffer.DMToDegree(this.latitude);
                            }
                            catch (NumberFormatException e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal latitude: " + str);
                            }
                            break;
                        case 3:
                            try {
                                this.latitudeNS = str.toString();
                            } catch (Exception e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal companyNS: " + str + "  GGA" + nmea);
                            }

                            break;
                        case 5:
                            try {
                                this.longitudeEW = str.toString();
                            } catch (Exception e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal companyEW: " + str + "  GGA" + nmea);
                            }
                            break;
                        case 4:
                            try {
                                this.longitude = Double.parseDouble(str.toString());
                                if (this.longitude > 500.0D)
                                    this.longitude = Buffer.DMToDegree(this.longitude);
                            }
                            catch (NumberFormatException e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal longitude: " + str + "  GGA" + nmea);
                            }
                            break;
                        case 6:
                            try {
                                if (str.toString().equals("")) {
                                    this.mode = 0;
 }
                                this.mode = Integer.parseInt(str.toString());

                                Log.e("MODE", "MODE         " + this.mode + "  GGA" + nmea);
                            }
                            catch (Exception e)
                            {
                                bSend = false;
                                this.mode = 0;
                                LOG.E("NmeaAnalyzer", "Illegal mode: " + str);
                            }
                            break;
                        case 7:
                            try
                            {
                                if (str.toString().equals("")) {
                                    this.u_stars = 0;
}
                                this.u_stars = Integer.parseInt(str.toString());

                                Log.e("MODEE", "MODE     卫星数：    " + this.u_stars);
                            }
                            catch (Exception e)
                            {
                                bSend = false;
                                this.u_stars = 0;
                                LOG.E("NmeaAnalyzer", "Illegal mode: " + str);
                            }
                            break;
                        case 8:
                            try {
                                this.dop = Double.parseDouble(str.toString());
                            } catch (Exception e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal mode: " + str);
                            }
                            break;
                        case 9:
                            try {
                                this.altitude = Double.parseDouble(str.toString());
                            } catch (Exception e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal mode: " + str);
                            }

                    }

                }

                label684: ++index;
                str.delete(0, str.length());
            } else {
                str.append(nmea.charAt(i));
            }
        }

        return bSend;
    }

    private boolean analyzeGLL(String nmea)
    {
        double latitudeGLL = 0.0D;
        double longitudeGLL = 0.0D;
        boolean bSend = true;
        int index = 0;
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < nmea.length(); ++i) {
            if ((((nmea.charAt(i) == ',') ? 1 : 0) | ((nmea.charAt(i) == '*') ? 1 : 0) | ((i == nmea.length() - 1) ? 1 : 0)) != 0) {
                if (!(str.toString().equals(""))) {
                    switch (index) {
                        case 1:
                            try {
                                latitudeGLL = Double.parseDouble(str.toString());
                                if (latitudeGLL > 500.0D)
                                    latitudeGLL = Buffer.DMToDegree(latitudeGLL);
                            }
                            catch (NumberFormatException e)
                            {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal latitude: " + str);
                            }
                            break;
                        case 3:
                            try {
                                longitudeGLL = Double.parseDouble(str.toString());
                                if (longitudeGLL > 500.0D)
                                    longitudeGLL = Buffer.DMToDegree(longitudeGLL);
                            }
                            catch (NumberFormatException e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal longitude: " + str);
                            }

                        case 2:
                    }

                }

                ++index;
                str.delete(0, str.length());
            } else {
                str.append(nmea.charAt(i));
            }
        }

        return bSend;
    }

    private boolean analyzeGSA(String nmea)
    {
        boolean bSend = true;
        int index = 0;
        int usedSV = 0;
        StringBuffer str = new StringBuffer();
        ArrayList strList = new ArrayList();
        for (int i = 0; i < nmea.length(); ++i) {
            if ((((nmea.charAt(i) == ',') ? 1 : 0) | ((nmea.charAt(i) == '*') ? 1 : 0) | ((i == nmea.length() - 1) ? 1 : 0)) != 0) {
                if ((!(str.toString().equals(""))) &&
                        (index >= 3) && (index <= 14)) {
                    try {
                        int prn = Integer.parseInt(str.toString());
                        if ((prn > 0) && (prn < 500))
                        {
                            if ((((prn >= 100) ? 1 : 0) & ((prn < 200) ? 1 : 0)) != 0)
                                prn += 100;
                            if (prn < 300) {
                                this.usedInFixTable[prn] = 1;
                                ++usedSV;
                            }
                        }
                    }
                    catch (NumberFormatException e) {
                        bSend = false;
                        LOG.E("NmeaAnalyzer", "Illegal prn at index: " + index);
                    }
                }

                strList.add(str.toString());
                ++index;
                str.delete(0, str.length());
            } else {
                str.append(nmea.charAt(i));
            }
        }

        if (nmea.startsWith("$GPGSA"))
            this.roverUsedGPSN = usedSV;
        else {
            this.roverUsedBDSN = usedSV;
        }

        return bSend;
    }

    private boolean analyzeRDK(String nmea)
    {
        boolean bSend = true;
        int index = 0;
        StringBuffer str = new StringBuffer();

        for (int i = 0; i < nmea.length(); ++i) {
            if ((((nmea.charAt(i) == ',') ? 1 : 0) | ((nmea.charAt(i) == '*') ? 1 : 0) | ((i == nmea.length() - 1) ? 1 : 0)) != 0) {
                if (!(str.toString().equals(""))) {
                    switch (index)
                    {
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            try
                            {
                                this.baseDelay = Integer.parseInt(str.toString());
                                LOG.V("netBleTest", "baseDelay: " + this.baseDelay);
                            }
                            catch (NumberFormatException e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer5", "Illegal baseDelay: " + str);
                            }

                            break;
                        case 4:
                            try
                            {
                                this.baseLine = Integer.parseInt(str.toString());
                                LOG.V("NmeaAnalyzer5", "baseLine: " + this.baseLine);
                            } catch (NumberFormatException e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer5", "Illegal baseLine: " + str);
                            }
                            break;
                        case 5:
                            try
                            {
                                this.baseGPSN = Integer.parseInt(str.toString());
                                LOG.V("NmeaAnalyzer5", "baseGPSN: " + this.baseGPSN);
                            } catch (NumberFormatException e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer5", "Illegal baseGPSN: " + str);
                            }

                            break;
                        case 6:
                            try
                            {
                                this.baseBDSN = Integer.parseInt(str.toString());
                                LOG.V("NmeaAnalyzer5", "baseBDSN: " + this.baseBDSN);
                            } catch (NumberFormatException e) {
                                LOG.E("NmeaAnalyzer5", "Illegal baseBDSN: " + str);
                            }
                            break;
                        case 7:
                            try
                            {
                                this.baseElvGpsSnr = Integer.parseInt(str.toString());
                                LOG.V("NmeaAnalyzer5", "baseElvGpsSnr: " + this.baseElvGpsSnr);
                            } catch (NumberFormatException e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer5", "Illegal baseElvGpsSnr: " + str);
                            }

                            break;
                        case 8:
                            try
                            {
                                this.baseElvBdsSnr = Integer.parseInt(str.toString());
                                LOG.V("NmeaAnalyzer5", "baseElvBdsSnr: " + this.baseElvBdsSnr);
                            } catch (NumberFormatException e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal baseElvBdsSnr: " + str);
                            }

                    }

                }

                ++index;
                str.delete(0, str.length());
            }
            else {
                str.append(nmea.charAt(i));
            }
        }

        return bSend;
    }

    private boolean analyzeGSV(String nmea)
    {
        boolean bSend = true;
        int index = 0;
        StringBuffer str = new StringBuffer();
        int prn = -1;
        double ele = -1.0D;
        double azm = -1.0D;
        double snr = -1.0D;
        for (int i = 0; i < nmea.length(); ++i) {
            if (nmea.charAt(i) != ',') if ((((nmea.charAt(i) == '*') ? 1 : 0) | ((i == nmea.length() - 1) ? 1 : 0)) == 0);
            if ((!(str.toString().equals(""))) &&
                    (index > 3)) {
                switch (index % 4) {
                    case 0:
                        try {
                            prn = Integer.parseInt(str.toString());

                            if (nmea.startsWith("$GBGSV")) {
                                prn += 100;
                            }

                            if ((prn >= 100) && (prn < 200))
                                prn += 100;
                        } catch (NumberFormatException e) {
                            bSend = false;
                            LOG.E("NmeaAnalyzer", "Illegal prn: " + str);
                        }
                        break;
                    case 1:
                        try {
                            ele = Double.parseDouble(str.toString());
                        } catch (NumberFormatException e) {
                            bSend = false;
                            LOG.E("NmeaAnalyzer", "Illegal elevator: " + str);
                        }
                        break;
                    case 2:
                        try {
                            azm = Double.parseDouble(str.toString());
                        } catch (NumberFormatException e) {
                            bSend = false;
                            LOG.E("NmeaAnalyzer", "Illegal azimuth: " + str);
                        }
                        break;
                    case 3:
                        try
                        {
                            snr = Integer.parseInt(str.toString());
                        } catch (NumberFormatException e) {
                            bSend = false;
                            LOG.E("NmeaAnalyzer", "Illegal azimuth: " + str);
                        }

                        if ((prn > 0) && (prn < 500) &&
                                (prn != -1)) {
                            MySatellite sat = new MySatellite();
                            sat.setPrn(prn);
                            sat.setAzimuth(azm);
                            sat.setElevation(ele);
                            sat.setSnr(snr);
                            sat.setUsedInFix(this.usedInFixTable[prn] == 1);
                            this.t_satellites.add(sat);

                            prn = -1;
                            ele = -1.0D;
                            azm = -1.0D;
                            snr = -1.0D;
                        }

                }

            }

            str.delete(0, str.length());
            ++index;
            if (nmea.charAt(i) == '*') {
                index = 0;
                str.append(nmea.charAt(i));
            }

        }

        analyzeRover(this.t_satellites);
        analyzeRoverGpsAvgElv(this.t_satellites);
        analyzeRoverBdsAvgElv(this.t_satellites);

        LOG.V("NmeaAnalyzerrrr", "--YYYYYYYYYYYYYYYYYYYY----,roverGPSN," + this.roverGPSN + ",roverBDSN," + this.roverBDSN +
                ",roverElvGpsSnr," + this.roverElvGpsSnr + ",roverElvBdsSnr," + this.roverElvBdsSnr);

        return bSend;
    }

    private boolean analyzeBGI(String nmea)
    {
        Log.d("bearing", "analyzeBGI：" + nmea);

        boolean bSend = true;
        int index = 0;
        StringBuffer str = new StringBuffer();

        for (int i = 0; i < nmea.length(); ++i) {
            if ((((nmea.charAt(i) == ',') ? 1 : 0) | ((nmea.charAt(i) == '*') ? 1 : 0) | ((i == nmea.length() - 1) ? 1 : 0)) != 0) {
                if (!(str.toString().equals(""))) {
                    switch (index)
                    {
                        case 5:
                            try
                            {
                                this.bearingBGI = Double.parseDouble(str.toString());
                                this.bearing = this.bearingBGI;
                            }
                            catch (NumberFormatException e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal bearingBGI: " + str);
                            }
                    }

                }

                ++index;
                str.delete(0, str.length());
            } else {
                str.append(nmea.charAt(i));
            }
        }

        return bSend;
    }

    private boolean analyzeRMC(String nmea)
    {
        Log.e("bearing", "analyzeRMC：" + nmea);

        boolean bSend = true;
        int index = 0;
        StringBuffer str = new StringBuffer();
        String utc = "";
        String utcTime = ""; String utcDate = "";
        for (int i = 0; i < nmea.length(); ++i) {
            if ((((nmea.charAt(i) == ',') ? 1 : 0) | ((nmea.charAt(i) == '*') ? 1 : 0) | ((i == nmea.length() - 1) ? 1 : 0)) != 0) {
                if (!(str.toString().equals("")))
                    switch (index) {
                        case 1:
                            utcTime = str.toString();
                            break;
                        case 7:
                            try {
                                this.speed = Double.parseDouble(str.toString());
                            } catch (Exception e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal speed: " + str);
                            }
                            break;
                        case 8:
                            try {
                                this.bearing = Double.parseDouble(str.toString());
                            } catch (Exception e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal bearing: " + str);
                            }
                            break;
                        case 9:
                            utcDate = str.toString();
                            utc = utcDate + utcTime;
                            SimpleDateFormat df1 = new SimpleDateFormat("ddMMyyHHmmss.SSS");
                            SimpleDateFormat df2 = new SimpleDateFormat("yyMMddHHmmss.SSS");
                            SimpleDateFormat df3 = new SimpleDateFormat("yyyyMMddHHmmss.SSS");
                            LOG.V("NmeaAnalyzer5", "--utc:" + utc);
                            try {
                                Date date = df1.parse(utc);
                                utc = df2.format(date);
                                utc = "20" + utc;
                                date = df3.parse(utc);

                                this.strUtcTime = df3.format(date);

                                LOG.V("NmeaAnalyzer5", "--strUtcTime:" + this.strUtcTime);
                                BLeService.currentTime = this.strUtcTime;

                                LOG.V("NmeaAnalyzer", utcTime);
                            } catch (Exception e) {
                                bSend = false;
                                e.printStackTrace(); } case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 6: } ++index;
                str.delete(0, str.length());
            } else {
                str.append(nmea.charAt(i));
            }
        }

        return bSend;
    }

    private boolean analyzeEPE(String nmea)
    {
        boolean bSend = true;
        int index = 0;
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < nmea.length(); ++i) {
            if ((((nmea.charAt(i) == ',') ? 1 : 0) | ((nmea.charAt(i) == '*') ? 1 : 0) | ((i == nmea.length() - 1) ? 1 : 0)) != 0) {
                if (!(str.toString().equals(""))) {
                    switch (index) {
                        case 1:
                            try {
                                this.hEPE = Double.parseDouble(str.toString());
                            } catch (NumberFormatException e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal hEPE: " + str);
                            }
                            break;
                        case 2:
                            try {
                                this.wEPE = Double.parseDouble(str.toString());
                            } catch (NumberFormatException e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal wEPE: " + str);
                            }
                    }
                }

                ++index;
                str.delete(0, str.length());
            } else {
                str.append(nmea.charAt(i));
            }
        }
        if ((this.hEPE == -1.0D) && (this.wEPE == -1.0D)) {
            this.sdkAccuracy = -1.0D;
        } else {
            double ava = this.hEPE * this.hEPE + this.wEPE * this.wEPE;
            ava = Math.sqrt(ava);
            this.sdkAccuracy = ((int)(ava * Math.pow(10.0D, 3.0D)) / Math.pow(10.0D, 3.0D));
        }
        return bSend;
    }

    private boolean analyzePPS(String nmea)
    {
        boolean bSend = true;
        int pps = 0;
        int index = 0;
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < nmea.length(); ++i) {
            if ((((nmea.charAt(i) == ',') ? 1 : 0) | ((nmea.charAt(i) == '*') ? 1 : 0) | ((i == nmea.length() - 1) ? 1 : 0)) != 0) {
                if (!(str.toString().equals(""))) {
                    switch (index) {
                        case 1:
                            try {
                                pps = Integer.parseInt(str.toString());
                            } catch (NumberFormatException e) {
                                bSend = false;
                                LOG.E("NmeaAnalyzer", "Illegal PPS: " + str);
                            }
                    }
                }

                ++index;
                str.delete(0, str.length());
            } else {
                str.append(nmea.charAt(i));
            }
        }

        return bSend;
    }

    private void analyzeRover(CopyOnWriteArrayList<MySatellite> sats)
    {
        int usedInFixGPS = 0;
        int usedInFixBDS = 0;
        for (MySatellite sat : sats) {
            if (1 == sat.getSystem()) {
                ++usedInFixGPS;
            }
            else if (2 == sat.getSystem()) {
                ++usedInFixBDS;
            }
        }
        this.roverGPSN = usedInFixGPS;
        this.roverBDSN = usedInFixBDS;
    }

    private void analyzeRoverGpsAvgElv(CopyOnWriteArrayList<MySatellite> sats)
    {
        if (sats.size() < 6) {
            LOG.V("NmeaAnalyzerrrr", "-001--YYYYYYYYYYYYYYYYYYYY----,roverGPSN," + this.roverGPSN + ",roverBDSN," + this.roverBDSN +
                    ",roverElvGpsSnr," + this.roverElvGpsSnr + ",roverElvBdsSnr," + this.roverElvBdsSnr);
            this.roverElvGpsSnr = 0;
            return;
        }

        double[] temp = new double[60];
        double[] tempSnr = new double[60];

        int countSat = 0;
        for (MySatellite s : sats) {
            if (1 == s.getSystem()) {
                temp[countSat] = s.getElevation();
                tempSnr[countSat] = s.getSnr();
                ++countSat;
            }
        }

        for (int i = 0; i < 20; ++i) {
            LOG.V("NmeaAnalyzerrrr", "----org----FIRST----10-------ELV:" + temp[i] + "------SNR-----:" + tempSnr[i]);
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

        for (int i = 0; i < 10; ++i) {
            LOG.V("NmeaAnalyzer", "----after----FIRST----10-------ELV:" + temp[i] + "------SNR-----:" + tempSnr[i]);
        }

        double avg = 0.0D;
        for (int i = 0; i < 6; ++i)
        {
            avg += tempSnr[i];
        }

        avg /= 6.0D;
        LOG.V("NmeaAnalyzer", " ----avg-avg-----ELV------------" + avg);

        this.roverElvGpsSnr = (int)Math.round(avg);
    }

    private void analyzeRoverBdsAvgElv(CopyOnWriteArrayList<MySatellite> sats)
    {
        if (sats.size() < 6) {
            this.roverElvGpsSnr = 0;
            LOG.V("NmeaAnalyzerrrr",
                    "--002-YYYYYYYYYYYYYYYYYYYY----,roverGPSN," + this.roverGPSN + ",size," + sats.size() + ",roverBDSN," +
                            this.roverBDSN + ",roverElvGpsSnr," + this.roverElvGpsSnr + ",roverElvBdsSnr," + this.roverElvBdsSnr);
            return;
        }

        double[] temp = new double[60];
        double[] tempSnr = new double[60];

        int countSat = 0;
        for (MySatellite s : sats) {
            if (2 == s.getSystem()) {
                temp[countSat] = s.getElevation();
                tempSnr[countSat] = s.getSnr();
                ++countSat;
            }

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
        LOG.V("NmeaAnalyzer", " ----avg-avg-----ELV------------" + avg);

        this.roverElvBdsSnr = (int)Math.round(avg);
    }

    public static abstract interface NmeaCallback
    {
        public abstract void onNmeaOneBroadcast(String paramString);

        public abstract void onNmeaUpdate(boolean[] paramArrayOfBoolean);
    }
}