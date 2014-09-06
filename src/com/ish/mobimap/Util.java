/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                            (c) 2003-2007 ISh                              */
/*****************************************************************************/

package com.ish.mobimap;

import java.util.Date;
import java.util.Calendar;

public class Util
{
    //
    //  clip line by bounds of rectangle
    //
    public static int clipx1, clipy1, clipx2, clipy2;

    public static final byte CLIP_CROSS = 0;
    public static final byte CLIP_INSIDE = 1;
    public static final byte CLIP_OUTSIDE = 2;

    public static int OutCode (int x, int y, int X1, int Y1, int X2, int Y2)
    {
        int code = 0;

        if (x < X1)
            code |= 0x01;

        if (y < Y1)
            code |= 0x02;

        if (x > X2)
            code |= 0x04;

        if (y > Y2)
            code |= 0x08;

        return code;
    }

    public static byte ClipLine (int x1, int y1, int x2, int y2,
                          int X1, int Y1, int X2, int Y2)
    {
        int code1 = OutCode (x1, y1, X1, Y1, X2, Y2);
        int code2 = OutCode (x2, y2, X1, Y1, X2, Y2);
        boolean inside = (code1 | code2) == 0;
        boolean outside = (code1 & code2) != 0;

        if (outside)
            return CLIP_OUTSIDE;

        boolean swap = false;

        while (!outside && !inside)
        {
            if (code1 == 0)
            {
                swap = true;
                {
                    int t = x2; x2 = x1; x1 = t;
                } //Swap ( x1, x2 );
                {
                    int t = y2; y2 = y1; y1 = t;
                } //Swap ( y1, y2 );
                {
                    int t = code2; code2 = code1; code1 = t;
                } //Swap ( code1, code2 );
            }

            if ( (code1 & 0x01) != 0) // clip left
            {
                y1 += ( (long)(y2 - y1) * (X1 - x1)) / (x2 - x1);
                x1 = X1;
            }
            else
            if ( (code1 & 0x02) != 0) // clip above
            {
                x1 += ( (long)(x2 - x1) * (Y1 - y1)) / (y2 - y1);
                y1 = Y1;
            }
            else
            if ( (code1 & 0x04) != 0) // clip right
            {
                y1 += ( (long)(y2 - y1) * (X2 - x1)) / (x2 - x1);
                x1 = X2;
            }
            else
            if ( (code1 & 0x08) != 0) // clip below
            {
                x1 += ( (long)(x2 - x1) * (Y2 - y1)) / (y2 - y1);
                y1 = Y2;
            }

            code1 = OutCode (x1, y1, X1, Y1, X2, Y2);
            code2 = OutCode (x2, y2, X1, Y1, X2, Y2);
            inside = (code1 | code2) == 0;
            outside = (code1 & code2) != 0;
        }

        if (swap)
        {
            {
                int t = x2; x2 = x1; x1 = t;
            } //Swap ( x1, x2 );
            {
                int t = y2; y2 = y1; y1 = t;
            } //Swap ( y1, y2 );
        }

        clipx1 = x1; clipy1 = y1;
        clipx2 = x2; clipy2 = y2;
        return CLIP_CROSS;
    }

    //
    //   convert int into array of chars
    //
    public static int offset, length;
    public static void itoa (int n, char s[])
    {
        offset = 5;
        length = 0;
        while (n > 0)
        {
            s[offset--] = (char) ( (n % 10) + '0');
            n /= 10;
            length++;
        }
        offset++;
    }

    /**
     * Converts string into integer. If string is not numeric, than default value is returned.
     * @param s String
     * @param defValue int
     * @return int
     */
    public static int atoi (String s, int defValue)
    {
        try {
            return Integer.parseInt (s);
        }
        catch (NumberFormatException e)
        {
            return defValue;
        }
    }

    //
    //    distance in eucludian space
    //
    public static void distance (int a, int b, char s[])
    {
        int rslt = hypoteu (a, b) * Map.globalScale / 100 + 1;

        itoa (rslt, s);
        int end = offset + length;
        s[end] = s[end - 1];
        s[end - 1] = '.';
        s[end + 1] = 'k'; s[end + 2] = 'm';
        if (rslt < 10)
        {
            s[--offset] = '0'; length++;
        }
        length += 3;
    }

    public static int hypoteu (int a, int b)
    {
        int L = a * a + b * b;
        int temp, div = L;
        int rslt = L;

        if (L > 1)
            while (true)
            {
                temp = L / div + div;
                div = temp >> 1;
                div += temp & 1;
                if (rslt > div)
                    rslt = div;
                else
                    break;
            }
        return rslt;
    }

    /**
     * Distance squared.
     * @param a long
     * @param b long
     * @return long
     */
    public static long distanceSquared (long a, long b)
    {
        return a*a + b*b;
    }
    /**
     * Square root.
     * @param L long
     * @return int
     */
    public static int sqrtLong (long L)
    {
        long temp, div = L;
        long rslt = L;

        if (L > 1)
            while (true)
            {
                temp = L / div + div;
                div = temp >> 1;
                div += temp & 1;
                if (rslt > div)
                    rslt = div;
                else
                    break;
            }
        return (int)rslt;
    }


    //
    //     and other ways to find distance
    //
    public static int hypot (int a, int b)
    {
        return Math.abs (a) + Math.abs (b);
    }

    public static int hypot2 (int a, int b)
    {
        return Math.max (Math.abs (a), Math.abs (b));
    }

    /**
     * Convert timestamp into String
     * @param timestamp int
     * @return String
     */
    public static String getTime (int timestamp)
    {
        Calendar c = Calendar.getInstance();
        c.setTime (new Date (timestamp * 1000L));
        int h = c.get (Calendar.HOUR_OF_DAY), m = c.get (Calendar.MINUTE);
        int y = c.get (Calendar.YEAR), n = c.get (Calendar.MONTH) + 1, d = c.get (Calendar.DAY_OF_MONTH);
        return ((d<10)? "0": "") + d + "-" + ((n<10)? "0": "") + n + "-" + y + " " +
            ((h<10)? "0": "") + h + ':' + ((m<10)? "0": "") + m;
    }

    /**
     * True, if string is null or empty
     * @param s String
     * @return boolean
     */
    public static boolean isStringNullOrEmpty (String s)
    {
        if (s == null) return true;
        else return s.length() == 0;
    }

    /**
     * Sort elements by Shell algorithm
     * @param elements Vector
     * @param ids int[]
     */
    public static void sort (java.util.Vector elements, int[] ids)
    {
        boolean c;
        int e;
        int g;
        int i;
        int j;

        int n = elements.size ();
        n = n - 1;
        g = (n + 1) / 2;

        do
        {
            i = g;
            do
            {
                j = i - g;
                c = true;
                do
                {
                    if ( ( (String) elements.elementAt (j)).compareTo ( (String)
                        elements.elementAt (j + g)) < 0)
                    {
                        c = false;
                    }
                    else
                    {
                        Object tmp = elements.elementAt (j);
                        elements.setElementAt (elements.elementAt (j + g), j);
                        elements.setElementAt (tmp, j + g);
                    }
                    j = j - 1;
                }
                while (j >= 0 && c);
                i = i + 1;
            }
            while (i <= n);
            g = g / 2;
        }
        while (g > 0);
    }

}
