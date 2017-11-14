package com.olinkstar.util;

import android.util.Log;

public class Buffer
{
    private static int mode = 3;

    public static byte[] shortToByteArray(short s)
    {
        byte[] targets = new byte[2];
        for (int i = 0; i < 2; ++i) {
            int offset = (targets.length - 1 - i) * 8;
            targets[i] = (byte)(s >>> offset & 0xFF);
        }
        return targets;
    }

    public static byte[] longToByteArray(long s)
    {
        byte[] targets = new byte[8];
        for (int i = 0; i < 8; ++i) {
            int offset = (targets.length - 1 - i) * 8;
            targets[i] = (byte)(int)(s >>> offset & 0xFF);
        }
        return targets;
    }

    public static byte[] intToByteArray(int s)
    {
        byte[] targets = new byte[4];
        for (int i = 0; i < 4; ++i) {
            int offset = (targets.length - 1 - i) * 8;
            targets[i] = (byte)(s >>> offset & 0xFF);
        }
        return targets;
    }

    public static void printHexString(byte[] b)
    {
        String hex = "";
        for (int i = 0; i < b.length; ++i) {
            hex = hex + "0x";
            if ((b[i] & 0xFF) < 16) hex = hex + "0";
            hex = hex + Integer.toHexString(b[i] & 0xFF);
            hex = hex + ",";
        }
        Log.v("hex", hex);
    }

    public static int crc16(byte[] buffer)
    {
        int crc = 65535;

        for (int j = 0; j < buffer.length; ++j) {
            crc = (crc >>> 8 | crc << 8) & 0xFFFF;
            crc ^= buffer[j] & 0xFF;
            crc ^= (crc & 0xFF) >> 4;
            crc ^= crc << 12 & 0xFFFF;
            crc ^= (crc & 0xFF) << 5 & 0xFFFF;
        }
        crc &= 65535;
        return crc;
    }

    public static byte xor(byte[] buffer)
    {
        byte cs = 0;
        for (int i = 0; i < buffer.length; ++i) {
            cs = (byte)(cs ^ buffer[i]);
        }
        return cs;
    }

    public static double degreeToDM(double value)
    {
        double partV = value - (int)value;
        double entierV = value - partV;
        double minuteV = partV * 60.0D;
        double result = entierV * 100.0D + minuteV;
        return result;
    }

    public static double DMToDegree(double value) {
        double value100 = value / 100.0D;
        double partV = value100 - (int)value100;
        double entierV = value100 - partV;
        double minuteV = partV * 100.0D;
        double degreeV = minuteV / 60.0D;
        double result = entierV + degreeV;
        return result;
    }

    public static double[] GaussToBLToGauss(double longitude, double latitude)
    {
        int ProjNo = 0;

        double[] output = new double[2];

        double iPI = 0.0174532925199433D;
        int ZoneWide = 6;
        double a = 6378137.0D; double f = 0.003352810664747481D;

        ProjNo = (int)(longitude / ZoneWide);
        double longitude0 = ProjNo * ZoneWide + ZoneWide / 2;
        longitude0 *= iPI;
        double longitude1 = longitude * iPI;
        double latitude1 = latitude * iPI;
        double e2 = 2.0D * f - (f * f);
        double ee = e2 / (1.0D - e2);
        double NN = a /
                Math.sqrt(1.0D - (e2 * Math.sin(latitude1) *
                        Math.sin(latitude1)));
        double T = Math.tan(latitude1) * Math.tan(latitude1);
        double C = ee * Math.cos(latitude1) * Math.cos(latitude1);
        double A = (longitude1 - longitude0) * Math.cos(latitude1);
        double M = a *
                ((1.0D - (e2 / 4.0D) - (3.0D * e2 * e2 / 64.0D) - (5.0D * e2 * e2 * e2 / 256.0D)) *
                        latitude1 - (
                        (3.0D * e2 / 8.0D + 3.0D * e2 * e2 / 32.0D + 45.0D * e2 * e2 * e2 /
                                1024.0D) *
                                Math.sin(2.0D * latitude1)) +
                        (15.0D * e2 * e2 / 256.0D + 45.0D * e2 * e2 * e2 / 1024.0D) *
                                Math.sin(4.0D * latitude1) - (
                        35.0D * e2 * e2 * e2 / 3072.0D *
                                Math.sin(6.0D * latitude1)));

        double xval = NN *
                (A + (1.0D - T + C) * A * A * A / 6.0D +
                        (5.0D - (18.0D * T) + T * T + 14.0D *
                                C - (58.0D * ee)) *
                                A * A * A * A * A / 120.0D);
        double yval = M +
                NN *
                        Math.tan(latitude1) *
                        (A * A / 2.0D + (5.0D - T + 9.0D * C + 4.0D * C * C) * A * A * A * A / 24.0D +
                                (61.0D - (
                                        58.0D * T) + T * T + 270.0D * C - (330.0D * ee)) *
                                        A * A * A * A * A * A / 720.0D);
        double X0 = 1000000L * (ProjNo + 1) + 500000L;
        double Y0 = 0.0D;
        xval += X0;
        yval += Y0;
        output[0] = xval;
        output[1] = yval;
        return output;
    }

    public static double[] lla_ecef(double lat, double lon, double alt)
    {
        double lat_rad = lat * 3.141592653589793D / 180.0D;
        double lon_rad = lon * 3.141592653589793D / 180.0D;
        double WGS_MAJOR = 6378137.0D;
        double WGS_E1_SQRT = 0.00669437999014D;
        double WGS_MINOR = 6356752.3141999999D;

        double e = Math.sqrt(272331606681.94531D) / 6378137.0D;
        double N = 6378137.0D / Math.sqrt(1.0D - (e * e * Math.sin(lat_rad) * Math.sin(lat_rad)));
        double x = (N + alt) * Math.cos(lat_rad) * Math.cos(lon_rad);
        double y = (N + alt) * Math.cos(lat_rad) * Math.sin(lon_rad);
        double z = (N * 0.99330562000986D + alt) * Math.sin(lat_rad);
        double[] result = new double[2];
        result[0] = x;
        result[1] = y;
        return result;
    }

    public static double approximate(double srcValue, int precis)
    {
        return ((int)(srcValue * Math.pow(10.0D, precis)) / Math.pow(10.0D, precis));
    }
}