/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                            (c) 2003-2007 ISh                              */
/*****************************************************************************/

package com.ish.mobimap;

import javax.microedition.lcdui.*;

/**
 * Calculates sun position, time and place of sunrise and sunset
 */
public class SunDial
{
    private static final long PI_180_I = 3141592 / 180;   // x1'000'000

    private static final int SUN_PARAMSN = 5;
    private static final int SUN_STATE = 0;
    private static final int SUN_X = 1;
    private static final int SUN_Y = 2;
    private static final int SUN_ALTITUDE = 3;
    private static final int SUN_TIME = 4;

    private static final int SUN_NORISE = -1;
    private static final int SUN_NOSET = -2;
    private static final int SUN_INSKY = 0;

    private static long lastCalculateTime = 0;
    private static int latitude, longtitude;
    static int timeZoneOffset;
    private static boolean isTimeLocal;

    private static int daylightSaving;
    private static int dayOfYear;
    public static int hour;
    public static int min;
    public static int sec;

    private static long[] sunCoordinates;
    private static long[] sunrise;
    private static long[] sunset;

    private static int sunriseClipX, sunriseClipY, sunsetClipX, sunsetClipY;

    /**
     * Set coordinates of city center
     * @param placeLatitude latitude x100.000
     * @param placeLongtitude longtitude x100.000
     * @param placeTimeZoneOffset time zone offset in hours
     */
    public static void setPlaceCoordinates (int placeLatitude, int placeLongtitude, int placeTimeZoneOffset)
    {
        latitude = placeLatitude/100;
        longtitude = placeLongtitude/100;
        timeZoneOffset = placeTimeZoneOffset;

        calculateDates();
        timeZoneOffset += daylightSaving;
    }
    /**
     * Sets up time zone offset
     * @param isTimeLocal true, if time returning by phone is local
     */
    public static void setup (boolean isTimeLocal)
    {
        SunDial.isTimeLocal = isTimeLocal;
        lastCalculateTime = 0;
    }

    /**
     * Draws map background
     * @param g the Graphics
     * @param color base background color
     */
    public static void drawBackground (Graphics g, int color)
    {
        calculateSunPosition ();

        int suncolor = GraphicsPlus.mixColors(color, 0xffffff, 6);
        int shadowcolor = GraphicsPlus.mixColors(color, 0x000000, 6);

        if (sunrise[SUN_STATE] == SUN_INSKY)
        {
            if (sunriseClipY > Map.heightHalf)
            {
                g.setColor (shadowcolor);
                g.fillRect (0, 0, Map.width, Map.height);
                g.setColor (suncolor);
                g.fillTriangle(sunriseClipX, sunriseClipY, sunsetClipX, sunsetClipY,
                               Map.widthHalf, Map.heightHalf);

                if (sunriseClipY < Map.height)
                    g.fillRect(0, sunsetClipY, Map.width, Map.height);
            }
            else
            {
                g.setColor (suncolor);
                g.fillRect (0, 0, Map.width, Map.height);
                g.setColor (shadowcolor);
                g.fillTriangle(sunriseClipX, sunriseClipY, sunsetClipX, sunsetClipY,
                               Map.widthHalf, Map.heightHalf);

                if (sunriseClipY > 0)
                    g.fillRect(0, 0, Map.width, sunriseClipY);
            }
        }
    }
    /**
     * Draw sun position on map
     * @param theMap theMap
     * @param g Graphics
     */
    public static void draw (Map theMap, Graphics g)
    {
        FontM font = theMap.fontHorz;
        int h = font.getHeight();
        String note = null;

        if (sunrise[SUN_STATE] == SUN_INSKY)
        {
            g.setColor (0xffA010);
            g.setStrokeStyle(Graphics.DOTTED);
            g.drawLine(Map.widthHalf, Map.heightHalf, sunriseClipX, sunriseClipY);
            g.drawLine(Map.widthHalf, Map.heightHalf, sunsetClipX, sunsetClipY);

            // sunrise and sunset data is not precise - do not print it (Mobimap 4.2)
//            String s = printTime (sunrise[SUN_TIME]);
//            int dy = ((sunriseClipY > Map.heightHalf && sunriseClipY < Map.height - h) || (sunriseClipY < h)) ? h: 0;
//            h-=3;
//            int w = font.getWidth(s);
//            int x = sunriseClipX - w;
//            if (sunriseClipX < Map.width - w) x += w;
//            g.setColor (0xffff00);
//            g.fillRect (x - 1, sunriseClipY + dy - h, w+1, h);
//            font.drawString (g, s, x, sunriseClipY + dy-1);
//
//            s = printTime (sunset[SUN_TIME]);
//            w = font.getWidth(s);
//            g.setColor (0xffff00);
//            g.fillRect (sunsetClipX, sunsetClipY + dy - h, w, h);
//            font.drawString (g, s, sunsetClipX, sunsetClipY + dy-1);
        }
        else
            note = msrt.Resources [sunrise [SUN_STATE] == SUN_NORISE? 140: 141];

        if (sunCoordinates[SUN_ALTITUDE] > 0)
        {
            int xmid = Map.mx (theMap.originX + (int) sunCoordinates[SUN_X] / 10),
                ymid = Map.my (theMap.originY + (int) sunCoordinates[SUN_Y] / 10);

            Util.ClipLine (xmid, ymid, Map.widthHalf, Map.heightHalf, 0, 0, Map.width, Map.height);
            int x1 = Util.clipx1, y1 = Util.clipy1;

            Util.ClipLine (xmid, ymid, Map.widthHalf, Map.heightHalf, 5, 5,
                           Map.width - 12, Map.height - 12);
            int x2 = Util.clipx1, y2 = Util.clipy1;

            g.setColor (0xff7020);
            g.setStrokeStyle(Graphics.DOTTED);
            g.drawLine (Map.widthHalf, Map.heightHalf, x1, y1);
            g.setColor (0xffff00);
            g.drawLine (x2-8, y2, x2+8, y2);
            g.drawLine (x2-5, y2-5, x2+5, y2+5);
            g.drawLine (x2, y2-8, x2, y2+8);
            g.drawLine (x2+5, y2-5, x2-5, y2+5);
            g.fillArc (x2-5, y2-5, 10, 10, 0, 360);
            g.setColor (0xffA020);
            g.setStrokeStyle(Graphics.SOLID);
            g.drawArc (x2-5, y2-5, 10, 10, 0, 360);
        }
        else
            note = msrt.Resources [(sunCoordinates [SUN_ALTITUDE] < -50000)? 143: 142];

        if (note != null)
        {
            int w = font.getWidth (note);
            int x = Map.widthHalf - w/2;
            g.setColor (0xffff00);
            g.fillRect (x, Map.height - h, w, h);
            font.drawString (g, note, x, Map.height-3);
        }
    }
    /**
     * Converts time to char []
     * @param time time in seconds
     * @return string representation of time
     */
    private static String printTime (long time)
    {
        //time += timeZoneOffset;
        int h = (int)time / 3600, m = (int)(time / 60) % 60;
        return timeToString (h, m);
    }

    static String timeToString (int h, int m)
    {
        return ("" + ((h<10)? "0": "") + h + ':' + ((m<10)? "0": "") + m);
    }

    /**
     * Calculates current sun position and time, place of sunrise and sunset
     */
    private static void calculateSunPosition ()
    {
        long time = System.currentTimeMillis();

        if (time - lastCalculateTime > 1000*60)
        {
            calculateDates();

            lastCalculateTime = time;

            if (isTimeLocal) hour -= timeZoneOffset + daylightSaving;
            int tic = (hour - 12) * 3600 + (longtitude * 4 * 60) / 1000 +
                min * 60 + sec; // seconds

            sunCoordinates = new long[SUN_PARAMSN];
            sunCoordinates [SUN_TIME] = hour * 3600 + min * 60 + sec;

            getSunPosition (latitude, dayOfYear, tic, sunCoordinates);

            /*System.out.println ("\tSun coordinates: (" + sunCoordinates [SUN_X] + ", " +
                              sunCoordinates [SUN_Y] + ") alt: " + sunCoordinates [SUN_ALTITUDE] +
                              " time: " + printTime(sunCoordinates [SUN_TIME]));*/

            sunrise = new long[SUN_PARAMSN];
            sunset = new long[SUN_PARAMSN];

            getSunriseAndSunset (latitude, longtitude, dayOfYear, timeZoneOffset,
                                 sunrise, sunset);

            /*long res = sunrise [SUN_STATE];
            if (res == 0)
            {
                long a = sunrise[SUN_TIME];
                System.out.print ("\tSunrise: " + a / 3600 + ":" +
                                  (a / 60) % 60 +
                                  ":" + (a % 60) + " (" + sunrise[SUN_X] + ", " +
                                  sunrise[SUN_Y] + ")");
                a = sunset[SUN_TIME];
                System.out.println ("\tSunset: " + a / 3600 + ":" +
                                    (a / 60) % 60 +
                                    ":" + (a % 60) + " (" + sunset[SUN_X] +
                                    ", " + sunset[SUN_Y] + ")");
            }
            else if (res == SUN_NORISE)
                System.out.println (" No rise");
            else if (res == SUN_NOSET)
                System.out.println (" No set");*/

            int xmid = Map.mx (Map.originX + (int) sunCoordinates[SUN_X] / 10),
                ymid = Map.my (Map.originY + (int) sunCoordinates[SUN_Y] / 10);

            if (sunrise[SUN_STATE] == SUN_INSKY)
            {
                xmid = Map.mx (Map.originX + (int) sunrise[SUN_X] / 10);
                ymid = Map.my (Map.originY + (int) sunrise[SUN_Y] / 10);

                Util.ClipLine (xmid, ymid, Map.widthHalf, Map.heightHalf, 0, 0, Map.width, Map.height);
                sunriseClipX = Util.clipx1; sunriseClipY = Util.clipy1;
                sunsetClipX = Map.width - sunriseClipX; sunsetClipY = sunriseClipY;
            }
        }
    }

    /**
     * Time and place of sunrise and sunset
     * @param latitude latitude x1.000
     * @param longtitude longtitude x1.000
     * @param day day of year
     * @param timeZoneOffset time zone offset
     * @param sunrise (x, y, altitude, time) of sunrise
     * @param sunset (x, y, altitude, time) of sunset
     */
    private static void getSunriseAndSunset (int latitude, int longtitude,
                                             int day, int timeZoneOffset,
                                             long [] sunrise, long [] sunset)
    {
        final int halfday = 12*60*60;
        if (getSunPosition (latitude, day, 0, null) < 0)
            sunset [SUN_STATE] = sunrise [SUN_STATE] = SUN_NORISE;
        else if (getSunPosition (latitude, day, halfday, null) > 0)
            sunset [SUN_STATE] = sunrise [SUN_STATE] = SUN_NOSET;
        else
        {
            int local_shift = (longtitude*4*60)/1000 - (timeZoneOffset + 12) * 3600;
            int left = -halfday, right = 0, middle = - halfday/2;
            while (right - left > 60)
            {
                long r = getSunPosition (latitude, day, middle, sunrise);
                if (Math.abs (r) < 5)
                    break;
                if (r < 0)
                {
                    left = middle;
                }
                else
                {
                    right = middle;
                }
                middle = (left + right) / 2;
            }
            sunrise [SUN_TIME] = (middle - local_shift) % (3600*24);;

            left = 0;
            right = halfday;
            middle = halfday / 2;
            while (right - left > 60)
            {
                long r = getSunPosition (latitude, day, middle, sunset);
                if (Math.abs (r) < 5)
                    break;
                if (r > 0)
                {
                    left = middle;
                }
                else
                {
                    right = middle;
                }
                middle = (left + right) / 2;
            }
            sunset [SUN_TIME] = (middle - local_shift) % (3600*24);
            sunset [SUN_STATE] = sunrise [SUN_STATE] = SUN_INSKY;
        }
    }
    /**
     * Returns sun's coordinates and altitude
     * @param latitude latitude x1.000
     * @param day day of year
     * @param tic local time
     * @param params array of results: x, y, altitude
     * @return altitude
     */
    private static long getSunPosition (int latitude, int day, long tic, long [] params)
    {
        long zzi = (360000 * (day - 81)) / 365;  // x1000 degrees
        long a = dcos (zzi);
        long b = dsin (zzi);
        long c = (a * a + ((b * b) / 1000000) * dcos(23450)) / 1000000;
        long ph = dacos (c);  // x1000000
        if (dsin (zzi) < 0) ph = -ph;

        long ti = (tic*1000)/3600 * 15; // x1000

        long cth = dcos (latitude);  // x10^6
        long sth = dsin (latitude);  // x10^6
        long cph = dcos (ph * 1000 / PI_180_I); // x10^6
        long sph = dsin (ph * 1000 / PI_180_I); // x10^6
        long cti = dcos (ti);  // x10^6
        long sti = dsin (ti);  // x10^6
        long x = (-cph * sti) / 1000000;  // x10^6
        long y = (cth * sph - sth * (cph * cti / 1000000)) / 1000000; // x10^6
        long altitude = (sth * sph + cth * (cph * cti / 1000000)) / 1000000;

        if (params != null)
        {
            params [SUN_X] = x;
            params [SUN_Y] = y;
            params [SUN_ALTITUDE] = altitude;
        }

        return altitude;
    }

    /**
     * Sine x
     * @param x angle in 1.000 degrees
     * @return value x1.000.000
     */
    private static long dsin (long x) // x1000 degrees
    {
        while (x < 0) x+= 360000;
        x = x % 360000;
        int sign = 1;
        if (x > 180000)
        {
            sign = -1;
            x = 360000 - x;
        }
        if (x > 90000) x = 180000 - x;
        x = (x * PI_180_I) / 1000;
        long r = x;  // x1.000.000
        long s = r;
        for (int i=2, g = -1; i < 18 && r > 1; i+=2)
        {
            r = r * ( (x * x / i / (i + 1)) / 1000000) / 1000000;
            s = s + g * r;
            g = (g > 0)? -1: 1;
        }
        return s * sign;
    }
    /**
     * Cosine x
     * @param x angle in 1.000 degrees
     * @return value x1.000.000
     */
    private static long dcos (long x)
    {
        x = Math.abs (x);
        x = x % 360000;
        int sign = 1;
        if (x > 180000) x = 360000 - x;
        if (x > 90000) { x = 180000 - x; sign = -1; }

        x = (x * PI_180_I) / 1000;
        return dcosrad (x) * sign;
    }
    /**
     * Cosine x
     * @param x angle in 1.000.000 radians [0;pi/2]
     * @return x1.000.000
     */
    private static long dcosrad (long x) // x1000000 radians, no normalizing
    {
        long r = 1000000;  // x1.000.000
        long s = 1000000;
        for (int i=2, g = -1; i < 14; i+=2)
        {
            r = r * ( (x * x / i / (i - 1)) / 1000000) / 1000000;
            s = s + g * r;
            g = (g > 0)? -1: 1;
        }
        return s;
    }
    /**
     * Arccosine x
     * @param x x1.000.000
     * @return angle in 1.000.000 radians
     */
    private static long dacos (long x)
    {
        final long pihalf = 3141592 / 2;
        final long pi = 3141592;

        if (x >= 1000000) return 0;
        else if (x <= -1000000) return -pi;

        long left = 0, right = pi, middle = pihalf;

        while (right - left > 1)
        {
            long s = dcosrad (middle);
            if (Math.abs (x - s) < 5) break;
            if (x < s)
            {
                left = middle;
            }
            else
            {
                right = middle;
            }
            middle = (left + right) / 2;
        }
        return middle;
    }

    private static int dayOfWeek(int $d, int $m, int $y)
    {
      int $a = (14 - $m) / 12;
      $y = $y - $a;
      $m = $m + 12 * $a - 2;
      return (7000 + ($d + $y + $y / 4 - $y / 100 + $y / 400 +
             (31 * $m) / 12)) % 7;
    }

    public static void calculateDates()
    {
        int $now = (int) (System.currentTimeMillis () / 1000);
        int $daysSinceUnixEpoch = $now / 24 / 60 / 60 + 1;

        int $tt = $now % (60 * 60 * 24);
        hour = $tt / (60 * 60);
        min = ($tt / 60) % 60;
        sec = $tt % 60;

        dayOfYear = $daysSinceUnixEpoch;
        int $year = 1970;
        while (true)
        {
            int $d = $year % 4 == 0 ? 366 : 365;
            dayOfYear -= $d;

            if (dayOfYear < 0)
            {
                dayOfYear += $d;
                break;
            }

            $year++;
        }

        int $dayOfWeek3103 = dayOfWeek (31, 03, $year);
        int $dayOfWeek3110 = dayOfWeek (31, 10, $year);

        int $leap = $year % 4 == 0 ? 1 : 0;
        int $dayOfYear3103 = 31 + 28 + $leap + 31;
        int $dayOfYear3110 = 31 + 28 + $leap + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31;

        int $springDay = $dayOfYear3103 - $dayOfWeek3103 - 1;
        int $autumnDay = $dayOfYear3110 - $dayOfWeek3110 - 1;

        daylightSaving = (dayOfYear >= $springDay && dayOfYear < $autumnDay) ? 1 : 0;
    }
}
