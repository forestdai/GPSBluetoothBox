package com.olinkstar.util;

public class PositionUtil
{
    public static double pi = 3.141592653589793D;
    public static double a = 6378245.0D;
    public static double ee = 0.006693421622965943D;

    public static double[] gps84_To_Gcj02(double lat, double lon)
    {
        if (outOfChina(lat, lon)) {
            return null;
        }
        double dLat = transformLat(lon - 105.0D, lat - 35.0D);
        double dLon = transformLon(lon - 105.0D, lat - 35.0D);
        double radLat = lat / 180.0D * pi;
        double magic = Math.sin(radLat);
        magic = 1.0D - (ee * magic * magic);
        double sqrtMagic = Math.sqrt(magic);
        dLat = dLat * 180.0D / a * (1.0D - ee) / magic * sqrtMagic * pi;
        dLon = dLon * 180.0D / a / sqrtMagic * Math.cos(radLat) * pi;
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        double[] result = { mgLat, mgLon };
        return result;
    }

    public static double[] gcj_To_Gps84(double lat, double lon)
    {
        double[] gps = transform(lat, lon);
        double longitude = lon * 2.0D - gps[1];
        double latitude = lat * 2.0D - gps[0];
        double[] result = { latitude, longitude };
        return result;
    }

    public static double[] gcj02_To_Bd09(double gg_lat, double gg_lon)
    {
        double x = gg_lon; double y = gg_lat;
        double z = Math.sqrt(x * x + y * y) + 2.E-005D * Math.sin(y * pi);
        double theta = Math.atan2(y, x) + 3.E-006D * Math.cos(x * pi);
        double bd_lon = z * Math.cos(theta) + 0.0065D;
        double bd_lat = z * Math.sin(theta) + 0.006D;
        double[] result = { bd_lat, bd_lon };
        return result;
    }

    public static double[] bd09_To_Gcj02(double bd_lat, double bd_lon)
    {
        double x = bd_lon - 0.0065D; double y = bd_lat - 0.006D;
        double z = Math.sqrt(x * x + y * y) - (2.E-005D * Math.sin(y * pi));
        double theta = Math.atan2(y, x) - (3.E-006D * Math.cos(x * pi));
        double gg_lon = z * Math.cos(theta);
        double gg_lat = z * Math.sin(theta);
        double[] result = { gg_lat, gg_lon };
        return result;
    }

    public static double[] bd09_To_Gps84(double bd_lat, double bd_lon)
    {
        double[] gcj02 = bd09_To_Gcj02(bd_lat, bd_lon);
        double[] map84 = gcj_To_Gps84(gcj02[0], gcj02[1]);
        return map84;
    }

    public static boolean outOfChina(double lat, double lon)
    {
        if ((lon < 72.004000000000005D) || (lon > 137.8347D)) {
            return true;
        }
        return ((lat < 0.8293D) || (lat > 55.827100000000002D));
    }

    private static double[] transform(double lat, double lon)
    {
        double[] result = { lat, lon };
        if (outOfChina(lat, lon)) {
            return result;
        }
        double dLat = transformLat(lon - 105.0D, lat - 35.0D);
        double dLon = transformLon(lon - 105.0D, lat - 35.0D);
        double radLat = lat / 180.0D * pi;
        double magic = Math.sin(radLat);
        magic = 1.0D - (ee * magic * magic);
        double sqrtMagic = Math.sqrt(magic);
        dLat = dLat * 180.0D / a * (1.0D - ee) / magic * sqrtMagic * pi;
        dLon = dLon * 180.0D / a / sqrtMagic * Math.cos(radLat) * pi;
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        result[0] = mgLat;
        result[1] = mgLon;
        return result;
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0D + 2.0D * x + 3.0D * y + 0.2D * y * y + 0.1D * x * y +
                0.2D * Math.sqrt(Math.abs(x));
        ret += (20.0D * Math.sin(6.0D * x * pi) + 20.0D * Math.sin(2.0D * x * pi)) * 2.0D / 3.0D;
        ret += (20.0D * Math.sin(y * pi) + 40.0D * Math.sin(y / 3.0D * pi)) * 2.0D / 3.0D;
        ret += (160.0D * Math.sin(y / 12.0D * pi) + 320.0D * Math.sin(y * pi / 30.0D)) * 2.0D / 3.0D;
        return ret;
    }

    private static double transformLon(double x, double y) {
        double ret = 300.0D + x + 2.0D * y + 0.1D * x * x + 0.1D * x * y + 0.1D *
                Math.sqrt(Math.abs(x));
        ret += (20.0D * Math.sin(6.0D * x * pi) + 20.0D * Math.sin(2.0D * x * pi)) * 2.0D / 3.0D;
        ret += (20.0D * Math.sin(x * pi) + 40.0D * Math.sin(x / 3.0D * pi)) * 2.0D / 3.0D;

        ret = ret + (150.0D * Math.sin(x / 12.0D * pi) + 300.0D * Math.sin(x / 30.0D *
                pi)) *
                2.0D /
                3.0D;
        return ret;
    }
}