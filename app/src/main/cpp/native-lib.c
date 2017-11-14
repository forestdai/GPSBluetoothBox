#include <jni.h>
#include "gps.h"
#include "android/log.h"
#include <math.h>
#include <stdlib.h>
#include <time.h>

static const char *TAG = "gps_hardware";
static GpsSvStatus  sGpsSvStatus;
//
//static void callback_location(GpsLocation *ll);
//static void callback_nmea(GpsUtcTime timestamp, const char* nmea, int length);
//static void sv_status_callback(GpsSvStatus* sv_status);
//static JavaVM *gJavaVM;
//static jobject gJavaobj;

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGX(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
/*****************************************************************/
/*****************************************************************/
/*****                                                       *****/
/*****       N M E A   P A R S E R                           *****/
/*****                                                       *****/
/*****************************************************************/
/*****************************************************************/

#define  NMEA_MAX_SIZE  100

typedef struct {
    int     pos;
    int     overflow;
    int     utc_year;
    int     utc_mon;
    int     utc_day;
    GpsLocation  fix;
    GpsSvStatus  sat_status;
    GpsCallbacks callbacks;
//  GpsStatus    status;
    char    in[ NMEA_MAX_SIZE+1 ];
    int     sat_count;
} NmeaReader;

static void
nmea_reader_init( NmeaReader*  r )
{
    // clear it
    memset( r, 0, sizeof(*r) );
    r->fix.size = sizeof(r->fix);
}

typedef struct {
    const char*  p;
    const char*  end;
} Token;

#define  MAX_NMEA_TOKENS  32

typedef struct {
    int     count;
    Token   tokens[ MAX_NMEA_TOKENS ];
} NmeaTokenizer;


static int
nmea_tokenizer_init( NmeaTokenizer*  t, const char*  p, const char*  end )
{
    int    count = 0;
    char*  q;

    // the initial '$' is optional
    if (p < end && p[0] == '$')
        p += 1;

    // remove trailing newline
    if (end > p && (end[-1] == '\n' || end[-1] == '\r')) {
        end -= 1;
        if (end > p && end[-1] == '\r')
            end -= 1;
    }

    // get rid of checksum at the end of the sentecne
    if (end >= p+3 && end[-3] == '*') {
        end -= 3;
    }

    while (p < end) {
        const char*  q = p;

        q = (const char *) memchr(p, ',', end - p);
        if (q == NULL)
            q = end;

        if (q >= p) {
            if (count < MAX_NMEA_TOKENS) {
                t->tokens[count].p   = p;
                t->tokens[count].end = q;
                count += 1;
            }
        }
        if (q < end)
            q += 1;

        p = q;
    }

    t->count = count;
    return count;
}

static Token
nmea_tokenizer_get( NmeaTokenizer*  t, int  index )
{
    Token  tok;
    static const char*  dummy = "";

    if (index < 0 || index >= t->count) {
        tok.p = tok.end = dummy;
    } else
        tok = t->tokens[index];

    return tok;
}

static double
str2float( const char*  p, const char*  end )
{
    int   result = 0;
    int   len    = end - p;
    char  temp[16];

    if (len >= (int)sizeof(temp))
        return 0.;

    memcpy( temp, p, len );
    temp[len] = 0;
    return strtod( temp, NULL );
}

static int
str2int( const char*  p, const char*  end )
{
    int   result = 0;
    int   len    = end - p;

    for ( ; len > 0; len--, p++ )
    {
        int  c;

        if (p >= end)
            goto Fail;

        c = *p - '0';
        if ((unsigned)c >= 10)
            goto Fail;

        result = result*10 + c;
    }
    return  result;

    Fail:
    return -1;
}

static int
str2num(Token a_token)
{
    Token tok = a_token;
    if (tok.p >= tok.end)
        return -1;
    return (int)str2float(tok.p, tok.end);
}

static int
nmea_reader_update_gps_status( NmeaReader*  r,
                               Token        tok_gps_status )
{
    Token   tok = tok_gps_status;

    if (tok.p >= tok.end)
        return -1;

    r->fix.gpsstatus = str2int(tok.p, tok.end);
    return 0;
}

static int
nmea_reader_update_time( NmeaReader*  r, Token  tok )
{
    int        hour, minute;
    double     seconds;
    struct tm  utc_tm = {0};
    struct tm  loc_tm = {0};
    time_t     utc_time;
    time_t     loc_time;
    time_t     cur_time;
    char       str[32];

    if (tok.p + 6 > tok.end)
        return -1;

    hour    = str2int  (tok.p,   tok.p+2);
    minute  = str2int  (tok.p+2, tok.p+4);
    seconds = str2float(tok.p+4, tok.end);

    utc_tm.tm_hour = hour;
    utc_tm.tm_min  = minute;
    utc_tm.tm_sec  = (int) seconds;
    utc_tm.tm_year = r->utc_year - 1900;
    utc_tm.tm_mon  = r->utc_mon - 1;
    utc_tm.tm_mday = r->utc_day;

    utc_time = timegm(&utc_tm);
    localtime_r(&utc_time, &loc_tm);
    loc_time = mktime(&loc_tm);
    cur_time = time(NULL);
    r->fix.timestamp = (long long)loc_time * 1000;

//    LOGD("utc_tm: %s\n", asctime(&utc_tm));
//    LOGD("loc_tm: %s\n", asctime(&loc_tm));

    //++ update time if gps fixed
    if ((r->fix.flags & GPS_LOCATION_HAS_LAT_LONG)) {
        if (labs(cur_time - utc_time) > 60) {
            struct timeval tv = { loc_time, 0 };
            settimeofday(&tv, NULL);
        }
    }
    //-- update time if gps fixed

    return 0;
}

static double
convert_from_hhmm( Token  tok )
{
    double  val     = str2float(tok.p, tok.end);
    int     degrees = (int)(floor(val) / 100);
    double  minutes = val - degrees*100.;
    double  dcoord  = degrees + minutes / 60.0;
    return  dcoord;
}

static int
nmea_reader_update_latlong( NmeaReader*  r,
                            Token        latitude,
                            char         latitudeHemi,
                            Token        longitude,
                            char         longitudeHemi )
{
    double   lat, lon;
    Token    tok;

    tok = latitude;
    if (tok.p + 6 > tok.end) {
//        LOGD("latitude is too short: '%.*s'", tok.end-tok.p, tok.p);
        return -1;
    }
    lat = convert_from_hhmm(tok);
    if (latitudeHemi == 'S')
        lat = -lat;

    tok = longitude;
    if (tok.p + 6 > tok.end) {
//        LOGD("longitude is too short: '%.*s'", tok.end-tok.p, tok.p);
        return -1;
    }
    lon = convert_from_hhmm(tok);
    if (longitudeHemi == 'W')
        lon = -lon;

    r->fix.flags    |= GPS_LOCATION_HAS_LAT_LONG;
    r->fix.latitude  = lat;
    r->fix.longitude = lon;
    return 0;
}

static int
nmea_reader_update_altitude( NmeaReader*  r,
                             Token        altitude,
                             Token        units )
{
    double  alt;
    Token   tok = altitude;

    if (tok.p >= tok.end)
        return -1;

    r->fix.flags   |= GPS_LOCATION_HAS_ALTITUDE;
    r->fix.altitude = str2float(tok.p, tok.end);
    return 0;
}

static int nmea_reader_update_accuracy(NmeaReader * r, Token accuracy)
{
    double acc;

    Token tok = accuracy;

    if (tok.p >= tok.end)
        return -1;

    r->fix.accuracy = (float) str2float(tok.p, tok.end);

    if (r->fix.accuracy == 99.99) {
        return 0;
    }

    r->fix.flags |= GPS_LOCATION_HAS_ACCURACY;
    return 0;
}

static int
nmea_reader_update_sat_num( NmeaReader*  r,
                            Token        tok_sat_num )
{
    double  alt;
    Token   tok = tok_sat_num;

    if (tok.p >= tok.end)
        return -1;

    r->sat_status.num_svs = str2int(tok.p, tok.end);
    return 0;
}

static int
nmea_reader_update_sat_pnr( NmeaReader*  r,
                            Token        tok_sat_pnr, int index )
{
    double  alt;
    Token   tok = tok_sat_pnr;

    if (tok.p >= tok.end)
        return -1;

//  r->fix.flags |= GPS_LOCATION_HAS_SPEED;
    r->sat_status.sv_list[index].prn = (int) str2float(tok.p, tok.end);
    return 0;
}

static int
nmea_reader_update_sat_elevation( NmeaReader*  r,
                                  Token        tok_sat_elevation , int index)
{
    double  alt;
    Token   tok = tok_sat_elevation;

    if (tok.p >= tok.end)
        return -1;

//  r->fix.flags |= GPS_LOCATION_HAS_SPEED;
    r->sat_status.sv_list[index].elevation = (float) str2float(tok.p, tok.end);
    return 0;
}

static int
nmea_reader_update_sat_azimuth( NmeaReader*  r,
                                Token        tok_sat_azimuth, int index )
{
    double  alt;
    Token   tok = tok_sat_azimuth;

    if (tok.p >= tok.end)
        return -1;

//  r->fix.flags |= GPS_LOCATION_HAS_SPEED;
    r->sat_status.sv_list[index].azimuth = (float) str2float(tok.p, tok.end);
    return 0;
}

static int
nmea_reader_update_sat_snr( NmeaReader*  r,
                            Token        tok_sat_snr, int index )
{
    double  alt;
    Token   tok = tok_sat_snr;

    if (tok.p >= tok.end)
        return -1;

//  r->fix.flags |= GPS_LOCATION_HAS_SPEED;
    r->sat_status.sv_list[index].snr = (float) str2float(tok.p, tok.end);
    return 0;
}

static int
nmea_reader_update_date( NmeaReader*  r, Token  date )
{
    Token  tok = date;
    int    day, mon, year;

    if (tok.p + 6 != tok.end) {
        LOGD("date not properly formatted: '%.*s'", tok.end-tok.p, tok.p);
        return -1;
    }
    day  = str2int(tok.p  , tok.p+2);
    mon  = str2int(tok.p+2, tok.p+4);
    year = str2int(tok.p+4, tok.p+6) + 2000;

    if ((day|mon|year) < 0) {
        LOGD("date not properly formatted: '%.*s'", tok.end-tok.p, tok.p);
        return -1;
    }

    r->utc_year = year;
    r->utc_mon  = mon;
    r->utc_day  = day;

//  LOGE("%d, %d, %d\n", year, mon, day);
    return 0;
}

static int
nmea_reader_update_bearing( NmeaReader*  r,
                            Token        bearing )
{
    double  alt;
    Token   tok = bearing;

    if (tok.p >= tok.end)
        return -1;

    r->fix.flags   |= GPS_LOCATION_HAS_BEARING;
    r->fix.bearing  = (float) str2float(tok.p, tok.end);
    return 0;
}

static int
nmea_reader_update_speed( NmeaReader*  r,
                          Token        speed )
{
    double  alt;
    Token   tok = speed;

    if (tok.p >= tok.end)
        return -1;

    r->fix.flags |= GPS_LOCATION_HAS_SPEED;
    r->fix.speed  = (float) str2float(tok.p, tok.end);
    r->fix.speed  = (float) ((r->fix.speed * 1.852) / 3.6); // ******** key value ****
//  LOGE(" RMC speed = %f m/s \n",r->fix.speed);
    return 0;
}

/**
 * $GPRMC
例：$GPRMC,085223.136,A,3957.6286,N,11619.2078,E,0.06,36.81,180908,,,A*57
字段0：$GPRMC，语句ID，表明该语句为Recommended Minimum Specific GPS/TRANSIT Data（RMC）推荐最小定位信息
字段1：UTC时间，hhmmss.sss格式
字段2：状态，A=定位，V=未定位
字段3：纬度ddmm.mmmm，度分格式（前导位数不足则补0）
字段4：纬度N（北纬）或S（南纬）
字段5：经度dddmm.mmmm，度分格式（前导位数不足则补0）
字段6：经度E（东经）或W（西经）
字段7：速度，节，Knots
字段8：方位角，度
字段9：UTC日期，DDMMYY格式
字段10：磁偏角，（000 - 180）度（前导位数不足则补0）
字段11：磁偏角方向，E=东W=西
字段16：校验值

$GPGGA
例：$GPGGA,085223.136,3957.6286,N,11619.2078,E,1,03,4.0,6.5,M,-6.5,M,0.0,0000*63
字段0：$GPGGA，语句ID，表明该语句为Global Positioning System Fix Data（GGA）GPS定位信息
字段1：UTC 时间，hhmmss.sss，时分秒格式
字段2：纬度ddmm.mmmm，度分格式（前导位数不足则补0）
字段3：纬度N（北纬）或S（南纬）
字段4：经度dddmm.mmmm，度分格式（前导位数不足则补0）
字段5：经度E（东经）或W（西经）
字段6：GPS状态，0=未定位，1=非差分定位，2=差分定位，3=无效PPS，6=正在估算
字段7：正在使用的卫星数量（00 - 12）（前导位数不足则补0）
字段8：HDOP水平精度因子（0.5 - 99.9）
字段9：海拔高度（-9999.9 - 99999.9）
字段10：地球椭球面相对大地水准面的高度
字段11：差分时间（从最近一次接收到差分信号开始的秒数，如果不是差分定位将为空）
字段12：差分站ID号0000 - 1023（前导位数不足则补0，如果不是差分定位将为空）
字段13：校验值

$GPGSA
例：$GPGSA,A,3,01,20,19,13,,,,,,,,,40.4,24.4,32.2*0A
字段0：$GPGSA，语句ID，表明该语句为GPS DOP and Active Satellites（GSA）当前卫星信息
字段1：定位模式，A=自动手动2D/3D，M=手动2D/3D
字段2：定位类型，1=未定位，2=2D定位，3=3D定位
字段3：PRN码（伪随机噪声码），第1信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
字段4：PRN码（伪随机噪声码），第2信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
字段5：PRN码（伪随机噪声码），第3信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
字段6：PRN码（伪随机噪声码），第4信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
字段7：PRN码（伪随机噪声码），第5信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
字段8：PRN码（伪随机噪声码），第6信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
字段9：PRN码（伪随机噪声码），第7信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
字段10：PRN码（伪随机噪声码），第8信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
字段11：PRN码（伪随机噪声码），第9信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
字段12：PRN码（伪随机噪声码），第10信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
字段13：PRN码（伪随机噪声码），第11信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
字段14：PRN码（伪随机噪声码），第12信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
字段15：PDOP综合位置精度因子（0.5 - 99.9）
字段16：HDOP水平精度因子（0.5 - 99.9）
字段17：VDOP垂直精度因子（0.5 - 99.9）
字段18：校验值

$GPGSV
例：$GPGSV,3,1,10,20,78,331,45,01,59,235,47,22,41,069,,13,32,252,45*70
字段0：$GPGSV，语句ID，表明该语句为GPS Satellites in View（GSV）可见卫星信息
字段1：本次GSV语句的总数目（1 - 3）
字段2：本条GSV语句是本次GSV语句的第几条（1 - 3）
字段3：当前可见卫星总数（00 - 12）（前导位数不足则补0）
字段4：PRN 码（伪随机噪声码）（01 - 32）（前导位数不足则补0）
字段5：卫星仰角（00 - 90）度（前导位数不足则补0）
字段6：卫星方位角（00 - 359）度（前导位数不足则补0）
字段7：信噪比（00－99）dbHz
字段8：PRN 码（伪随机噪声码）（01 - 32）（前导位数不足则补0）
字段9：卫星仰角（00 - 90）度（前导位数不足则补0）
字段10：卫星方位角（00 - 359）度（前导位数不足则补0）
字段11：信噪比（00－99）dbHz
字段12：PRN 码（伪随机噪声码）（01 - 32）（前导位数不足则补0）
字段13：卫星仰角（00 - 90）度（前导位数不足则补0）
字段14：卫星方位角（00 - 359）度（前导位数不足则补0）
字段15：信噪比（00－99）dbHz
字段16：校验值
 * */

static void
nmea_reader_parse( NmeaReader*  r )
{
    /* we received a complete sentence, now parse it to generate
     * a new GPS fix...
     */
    NmeaTokenizer  tzer[1];
    Token          tok;
    static int have_Satellite = -1;

//    LOGE("dai = > Received: %.*s", r->pos, r->in);
    if (r->pos < 9) {
        LOGE("Too short. discarded.");
        return;
    }

    nmea_tokenizer_init(tzer, r->in, r->in + r->pos);

    //字段0：$GPGGA，语句ID，表明该语句为Global Positioning System Fix Data（GGA）GPS定位信息
    tok = nmea_tokenizer_get(tzer, 0);
    if (tok.p + 5 > tok.end) {
        LOGE("sentence id '%.*s' too short, ignored.", tok.end-tok.p, tok.p);
        return;
    }
//            "$GPGGA,234910.00(1),2303.63577(2),N(3),11330.82974(4),E,1,04,2.12,73.7,M,-4.7,M,,*78\n"
    // ignore first two characters.
    tok.p += 2;

    if ( !memcmp(tok.p, "GGA", 3) ) {
        // GPS fix
//      LOGE(" ********  GGA  **********");
        //字段1：UTC 时间，hhmmss.sss，时分秒格式
        Token  tok_time          = nmea_tokenizer_get(tzer,1);
        //字段2：纬度ddmm.mmmm，度分格式（前导位数不足则补0）
        Token  tok_latitude      = nmea_tokenizer_get(tzer,2);
//        LOGE("dai = > tok_latitude = %s | = %s | %f",tok_latitude.p,tok_latitude.end, str2float(tok_latitude.p,tok_latitude.end));
        //字段3：纬度N（北纬）或S（南纬）
        Token  tok_latitudeHemi  = nmea_tokenizer_get(tzer,3);
//        LOGE("dai = > tok_latitudeHemi = %s | = %s | %c",tok_latitudeHemi.p,tok_latitudeHemi.end, tok_latitudeHemi.p[0]);
        //字段4：经度dddmm.mmmm，度分格式（前导位数不足则补0）
        Token  tok_longitude     = nmea_tokenizer_get(tzer,4);
        //字段5：经度E（东经）或W（西经）
        Token  tok_longitudeHemi = nmea_tokenizer_get(tzer,5);
        //字段6：GPS状态，0=未定位，1=非差分定位，2=差分定位，3=无效PPS，6=正在估算
        Token tok_gpsstatus = nmea_tokenizer_get(tzer,6);
        //字段7：正在使用的卫星数量（00 - 12）（前导位数不足则补0）
        Token  tok_Satellite_num = nmea_tokenizer_get(tzer,7);
        //字段8：HDOP水平精度因子（0.5 - 99.9）
        //字段9：海拔高度（-9999.9 - 99999.9）
        Token  tok_altitude      = nmea_tokenizer_get(tzer,9);
        //字段10：地球椭球面相对大地水准面的高度
        Token  tok_altitudeUnits = nmea_tokenizer_get(tzer,10);
        //字段11：差分时间（从最近一次接收到差分信号开始的秒数，如果不是差分定位将为空）
        //字段12：差分站ID号0000 - 1023（前导位数不足则补0，如果不是差分定位将为空）
        //字段13：校验值
        have_Satellite = str2num(tok_Satellite_num);
//      LOGE("see have_Satellite = %d\n",have_Satellite);

        nmea_reader_update_gps_status(r, tok_gpsstatus);
        nmea_reader_update_time(r, tok_time);
        nmea_reader_update_latlong(r, tok_latitude,
                                   tok_latitudeHemi.p[0],
                                   tok_longitude,
                                   tok_longitudeHemi.p[0]);
        nmea_reader_update_altitude(r, tok_altitude, tok_altitudeUnits);

    } else if ( !memcmp(tok.p, "GSA", 3) ) {
//      LOGE(" ********  GSA  **********");
        /*$GPGSA
        字段0：$GPGSA，语句ID，表明该语句为GPS DOP and Active Satellites（GSA）当前卫星信息
        字段3：PRN码（伪随机噪声码），第1信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
        字段4：PRN码（伪随机噪声码），第2信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
        字段5：PRN码（伪随机噪声码），第3信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
        字段6：PRN码（伪随机噪声码），第4信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
        字段7：PRN码（伪随机噪声码），第5信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
        字段8：PRN码（伪随机噪声码），第6信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
        字段9：PRN码（伪随机噪声码），第7信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
        字段10：PRN码（伪随机噪声码），第8信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
        字段11：PRN码（伪随机噪声码），第9信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
        字段12：PRN码（伪随机噪声码），第10信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
        字段13：PRN码（伪随机噪声码），第11信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
        字段17：VDOP垂直精度因子（0.5 - 99.9）
        字段18：校验值*/
        int i, j;
        //字段1：定位模式，A=自动手动2D/3D，M=手动2D/3D
        Token tok_sel_mode = nmea_tokenizer_get(tzer,1);
        //字段2：定位类型，1=未定位，2=2D定位，3=3D定位
        Token tok_mode = nmea_tokenizer_get(tzer,2);
        //字段14：PRN码（伪随机噪声码），第12信道正在使用的卫星PRN码编号（00）（前导位数不足则补0）
        Token tok_PDOP = nmea_tokenizer_get(tzer,14);
        //字段15：PDOP综合位置精度因子（0.5 - 99.9）
        Token tok_HDOP = nmea_tokenizer_get(tzer,15);
        //字段16：HDOP水平精度因子（0.5 - 99.9）
        Token tok_VDOP = nmea_tokenizer_get(tzer,16);
        Token tok_pnr[12];

        if (tok_mode.p[0] != '\0' && tok_mode.p[0] != '1')
        {
            r->fix.flags |= GPS_LOCATION_HAS_ACCURACY;
            //r->fix.accuracy = g_nav_data.P_DOP;
            nmea_reader_update_accuracy(r, tok_HDOP);

            for(i = 0; i < 12; i ++)
                tok_pnr[i] = nmea_tokenizer_get(tzer,3+i);

            r->sat_status.used_in_fix_mask = 0;
            int count = 0;
            for(i = 0; i < 12; i ++)
            {
                int pnr = str2int(tok_pnr[i].p, tok_pnr[i].end);
//                LOGD("used in fix sat %d pnr %d\n", i, pnr);
                //used_in_fix_mask is a mask for pnr
                if(pnr > 0)
                    r->sat_status.used_in_fix_mask |= 1 << (pnr - 1);
            }
        }
//        LOGD("used in fix mask %x\n", r->sat_status.used_in_fix_mask);
    }
    else if ( !memcmp(tok.p, "GSV", 3) ) {
        //GPS Satelite view There are 4 satelite info in each GSV sentence
//      LOGE(" ********  GSV  **********");
        /*$GPGSV
        例：$GPGSV,3,1,10,20,78,331,45,01,59,235,47,22,41,069,,13,32,252,45*70
        字段0：$GPGSV，语句ID，表明该语句为GPS Satellites in View（GSV）可见卫星信息
        字段8：PRN 码（伪随机噪声码）（01 - 32）（前导位数不足则补0）
        字段9：卫星仰角（00 - 90）度（前导位数不足则补0）
        字段10：卫星方位角（00 - 359）度（前导位数不足则补0）
        字段11：信噪比（00－99）dbHz
        字段12：PRN 码（伪随机噪声码）（01 - 32）（前导位数不足则补0）
        字段13：卫星仰角（00 - 90）度（前导位数不足则补0）
        字段14：卫星方位角（00 - 359）度（前导位数不足则补0）
        字段15：信噪比（00－99）dbHz
        字段16：校验值*/
        Token tok_sat_pnr[4];
        Token tok_sat_elevation[4];
        Token tok_sat_azimuth[4];
        Token tok_sat_snr[4];

        //字段1：本次GSV语句的总数目（1 - 3）
        Token tok_sat_gsv_total = nmea_tokenizer_get(tzer,1);
        //字段2：本条GSV语句是本次GSV语句的第几条（1 - 3）
        Token tok_sat_gsv_index = nmea_tokenizer_get(tzer,2);

        int sat_index = str2num(tok_sat_gsv_index) - 1;
        int sat_total = str2num(tok_sat_gsv_total) - 1;
//        LOGD("sat_index: %d\n", sat_index);
        //字段3：当前可见卫星总数（00 - 12）（前导位数不足则补0）
        Token tok_sat_num       = nmea_tokenizer_get(tzer,3);
        //字段4：PRN 码（伪随机噪声码）（01 - 32）（前导位数不足则补0）
        tok_sat_pnr[0]          = nmea_tokenizer_get(tzer,4);
        //字段5：卫星仰角（00 - 90）度（前导位数不足则补0）
        tok_sat_elevation[0]    = nmea_tokenizer_get(tzer,5);
        //字段6：卫星方位角（00 - 359）度（前导位数不足则补0）
        tok_sat_azimuth[0]      = nmea_tokenizer_get(tzer,6);
        //字段7：信噪比（00－99）dbHz
        tok_sat_snr[0]          = nmea_tokenizer_get(tzer,7);

        //第二条字段4：PRN 码（伪随机噪声码）（01 - 32）（前导位数不足则补0）
        tok_sat_pnr[1]          = nmea_tokenizer_get(tzer,4+4);
        //第二条字段5：卫星仰角（00 - 90）度（前导位数不足则补0）
        tok_sat_elevation[1]    = nmea_tokenizer_get(tzer,5+4);
        //第二条字段6：卫星方位角（00 - 359）度（前导位数不足则补0）
        tok_sat_azimuth[1]      = nmea_tokenizer_get(tzer,6+4);
        //第二条字段7：信噪比（00－99）dbHz
        tok_sat_snr[1]          = nmea_tokenizer_get(tzer,7+4);

        tok_sat_pnr[2]          = nmea_tokenizer_get(tzer,4+8);
        tok_sat_elevation[2]    = nmea_tokenizer_get(tzer,5+8);
        tok_sat_azimuth[2]      = nmea_tokenizer_get(tzer,6+8);
        tok_sat_snr[2]          = nmea_tokenizer_get(tzer,7+8);

        tok_sat_pnr[3]          = nmea_tokenizer_get(tzer,4+12);
        tok_sat_elevation[3]    = nmea_tokenizer_get(tzer,5+12);
        tok_sat_azimuth[3]      = nmea_tokenizer_get(tzer,6+12);
        tok_sat_snr[3]          = nmea_tokenizer_get(tzer,7+12);

        nmea_reader_update_sat_num(r, tok_sat_num);
        int index;
        float snr;
        for (index = 0; index < 4; index++)
        {
            snr = (float) str2float(tok_sat_snr[index].p, tok_sat_snr[index].end);
            if (snr > 0)
            {
                nmea_reader_update_sat_pnr(r, tok_sat_pnr[index], r->sat_count);
                nmea_reader_update_sat_elevation(r, tok_sat_elevation[index], r->sat_count);
                nmea_reader_update_sat_azimuth(r, tok_sat_azimuth[index], r->sat_count);
                nmea_reader_update_sat_snr(r, tok_sat_snr[index], r->sat_count);
                r->sat_count++;
            }
//            LOGD("satellite infomation : pnr:%d snr:%f \n" ,r->sat_status.sv_list[index+4*sat_index].prn,r->sat_status.sv_list[index+4*sat_index].snr);
        }

        if (sat_total == sat_index) {
            if (r->callbacks.sv_status_cb ) {
                r->callbacks.sv_status_cb(&r->sat_status);//上报卫星信息
            }


            if ( !memcmp(&r->in, "$GP", 3) ) {
                r->sat_status.sv_type = 1;
//                r->sat_status.sv_type = 0;
            } else if ( !memcmp(&r->in, "$GL", 3) ) {
                r->sat_status.sv_type = 2;
            } else if ( !memcmp(&r->in, "$BD", 3) ){
                r->sat_status.sv_type = 4;
            } else{
                r->sat_status.sv_type = 0;
            }


//            sv_status_callback(&r->sat_status);//上报卫星信息

            memset(&r->sat_status, 0, sizeof(GpsSvStatus));
            r->sat_count = 0;

        }

    }
    else if ( !memcmp(tok.p, "VTG", 3) ) {
        //do something ?
    }
    else if ( !memcmp(tok.p, "RMC", 3) ) {
//      LOGE(" ********  RMC  **********");
        Token  tok_time          = nmea_tokenizer_get(tzer,1);
        Token  tok_fixStatus     = nmea_tokenizer_get(tzer,2);
        Token  tok_latitude      = nmea_tokenizer_get(tzer,3);
        Token  tok_latitudeHemi  = nmea_tokenizer_get(tzer,4);
        Token  tok_longitude     = nmea_tokenizer_get(tzer,5);
        Token  tok_longitudeHemi = nmea_tokenizer_get(tzer,6);
        Token  tok_speed         = nmea_tokenizer_get(tzer,7);
        Token  tok_bearing       = nmea_tokenizer_get(tzer,8);
        Token  tok_date          = nmea_tokenizer_get(tzer,9);

//        LOGD("in RMC, fixStatus=%c", tok_fixStatus.p[0]);
        if (tok_fixStatus.p[0] == 'A')
        {
            nmea_reader_update_date( r, tok_date );

            nmea_reader_update_latlong( r, tok_latitude,
                                        tok_latitudeHemi.p[0],
                                        tok_longitude,
                                        tok_longitudeHemi.p[0] );

            nmea_reader_update_bearing( r, tok_bearing );
            nmea_reader_update_speed  ( r, tok_speed );

        }
        else if (tok_fixStatus.p[0] == 'V')
        {
            r->fix.flags  = 0;
        }
    }
    else if (!memcmp(tok.p, "GLL", 3)) {

        Token tok_fixstaus = nmea_tokenizer_get(tzer, 6);

        if (tok_fixstaus.p[0] == 'A') {

            Token tok_latitude = nmea_tokenizer_get(tzer, 1);

            Token tok_latitudeHemi = nmea_tokenizer_get(tzer, 2);

            Token tok_longitude = nmea_tokenizer_get(tzer, 3);

            Token tok_longitudeHemi = nmea_tokenizer_get(tzer, 4);

            Token tok_time = nmea_tokenizer_get(tzer, 5);

          nmea_reader_update_time(r, tok_time);
            nmea_reader_update_latlong(r, tok_latitude,
                                       tok_latitudeHemi.p[0],
                                       tok_longitude,
                                       tok_longitudeHemi.p[0]);
        }
    }
    else if (!memcmp(tok.p, "ZDA", 3)) {
        Token tok_time = nmea_tokenizer_get(tzer, 1);
        Token tok_day  = nmea_tokenizer_get(tzer, 2);
        Token tok_mon  = nmea_tokenizer_get(tzer, 3);
        Token tok_year = nmea_tokenizer_get(tzer, 4);
        r->utc_day  = str2int(tok_day .p, tok_day .end);
        r->utc_mon  = str2int(tok_mon .p, tok_mon .end);
        r->utc_year = str2int(tok_year.p, tok_year.end);
      nmea_reader_update_time(r, tok_time);
    }
    else {
        tok.p -= 2;
        LOGE("unknown sentence '%.*s", tok.end-tok.p, tok.p);
    }


    if (r->callbacks.nmea_cb ) {//上报nmea
        r->callbacks.nmea_cb(r->fix.timestamp, r->in, r->pos);
    }

//    callback_nmea(r->fix.timestamp, r->in, r->pos);
    memset(r->in, 0, sizeof(r->in));

    if (r->callbacks.location_cb) {//上报location
        r->callbacks.location_cb( &r->fix );
    }

    //调用JAVA层方法，上报Location
//    callback_location(&r->fix);


}

static void
nmea_reader_addc( NmeaReader*  r, int  c )
{

    if (r->overflow) {
        r->overflow = (c != '\n' || c != '\r');
        return;
    }

    if (r->pos >= (int) sizeof(r->in)-1 ) {
        r->overflow = 1;
        r->pos      = 0;
        return;
    }


    r->in[r->pos] = (char)c;
    r->pos       += 1;

    if (c == '\n' || c == '\r') {
        nmea_reader_parse( r );
        r->overflow = 0;
        r->pos      = 0;
    }
}

JNIEXPORT void JNICALL
Java_com_gccode_wnlbs_gpsbluetoothbox_BTUpdataServer_decodeNMEA(JNIEnv *env, jclass type,
                                                                jbyteArray data_, jint len) {
    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);

    NmeaReader reader[1];
    nmea_reader_init(reader);

    int i;

    for (i = 0; i < len; i++) {
//        LOGE("dai = > NMEA data reader[%d] %d %c", i, data[i], (char)data[i]);
        nmea_reader_addc(reader, data[i]);
    }

    (*env)->ReleaseByteArrayElements(env, data_, data, 0);
}

//JNIEXPORT jstring JNICALL
//Java_com_gccode_wnlbs_gpsbluetoothbox_MainActivity_stringFromJNI(
//        JNIEnv *env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//
//    (*env)->GetJavaVM(env, &gJavaVM);
//    return env->NewStringUTF(hello.c_str());
//}
//JNIEXPORT void JNICALL
//Java_com_gccode_wnlbs_gpsbluetoothbox_BTUpdataServer_initJNI(JNIEnv *env, jclass type) {
//
//    (*env)->GetJavaVM(env, &gJavaVM);
////    gJavaobj = (*env)->NewGlobalRef(env, instance);
//
//}