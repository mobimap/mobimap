/*****************************************************************************/
/*                               R A D A R  3                                */
/*                       (c) 2003-2008 Ilya Shakhat                          */
/*                           (c) 2006-2008 TMS                               */
/*****************************************************************************/

package com.ish.mobimap.radar;

import javax.microedition.lcdui.*;
import com.ish.mobimap.*;
import java.util.Vector;

/**
 * Mobile object
 */
public class Mobile
{
    /** Phone number */
    public String msisdn;
    /** Name */
    public String name;
    /** State, one of consts */
    public int state;
    /** true, if object has coordinates, otherwise it's simple name and number */
    public boolean hasCoordinates;
    /** Geographical coordinates - latitude (x100`000 degrees) */
    public int latitude;
    /** Geographical coordinates - longitude (x100`000 degrees) */
    public int longitude;
    /** Local coordinates - x */
    public int x;
    /** Local coordinates - y */
    public int y;
    /** Precision in meters */
    public int precision;
    /** Time */
    public int timestamp;
    /** Time as String */
    public String timeString;
    /** Color */
    public int color;
    /** true, if object is located in city */
    public boolean isInCity;
    /** tracking interval */
    public int trackingInterval;
    /** list of zones */
    public Vector zones;

    final static int COLOR_STANDARD = 0xCF0030;
    final static int COLOR_SELECTED = 0xEF0040;
    final static int COLOR_HISTORICAL = 0x806060;
    final static int COLOR_USER = 0xBF0050;

    /**
     * Create new mobile object
     * @param msisdn String phone number
     * @param name String
     * @param state int
     * @param hasCoordinates boolean
     * @param latitude int
     * @param longitude int
     * @param precision int
     * @param timestamp int
     * @param timeString String
     * @param trackingInterval int
     * @param zones Vector
     */
    public Mobile (String msisdn, String name, int state, boolean hasCoordinates, int latitude,
                          int longitude, int precision, int timestamp, String timeString,
                          int trackingInterval, Vector zones)
    {
        this.msisdn = msisdn;
        this.name = name;
        this.state = state;
        this.hasCoordinates = hasCoordinates;
        this.latitude = latitude;
        this.longitude = longitude;
        isInCity = true;
        x = Map.longitude2x(longitude);
        if (x <= 0 || x >= Map.cityX) { x = 0; isInCity = false; }
        y = Map.latitude2y(latitude);
        if (y <= 0 || y >= Map.cityY) { y = 0; isInCity = false; }
        this.precision = precision;
        this.timestamp = timestamp;
        this.timeString = timeString;
        this.trackingInterval = trackingInterval;
        this.zones = zones;
    }

    public Mobile clone() {
        Vector zonesCopy = new Vector(zones.size());
        for (int i=0; i < zones.size(); i++) {
            zonesCopy.addElement(((Zone)zones.elementAt(i)).clone());
        }
        return new Mobile(msisdn, name, state, hasCoordinates, latitude, longitude, precision,
                              timestamp, timeString, trackingInterval, zonesCopy);
    }

    /**
     * Paint mobile object on map
     * @param g Graphics
     * @param theMap Map
     * @param showName boolean true, if show name
     * @param showTimestamp boolean true, if time should be painted
     * @param isSelected boolean
     * @param isHistory boolean
     */
    public void draw (Graphics g, Map theMap, boolean showName, boolean showTimestamp,
                      boolean isSelected, boolean isHistory)
    {
        if (!isInCity)
            return;

        if (timeString == null) {
            showTimestamp = false;
        }

        boolean showTimestampString = showTimestamp && Map.scale < 5;

        int c = COLOR_STANDARD;
        if (showTimestamp) { // historical object
            c = COLOR_HISTORICAL;
        }

        g.setColor (c);
        g.setStrokeStyle (Graphics.SOLID);

        int px = Map.mx (x), py = Map.my (y);

        FontM font = theMap.fontHorz;

        g.fillArc(px-2, py-2, 5, 5, 0, 360);

        int rad = precision / Map.scale;
        if (rad > 4) {
            g.drawArc (px - rad, py - rad, rad << 1, rad << 1, 0, 360);
        }

        if (showName)
        {
            int w = font.getWidth(name);
            int h = font.getHeight();

            if (showTimestampString)
            {
                int w2 = font.getWidth(timeString);
                if (w2 > w) w = w2;
                h *= 2;
            }
            w += 6;

            g.setColor(isHistory? 0xCFCFAF: 0xFFFF7F);
            g.fillRoundRect(px - w/2, py - h - 1, w, h, 6, 6);

            g.setColor(c);
            g.drawRoundRect(px - w/2, py - h - 1, w, h, 6, 6);

            g.setColor(isHistory? 0x606060: 0x000000);
            font.drawString(g, name, px - w/2 + 3, py-h + 9);
            if (showTimestampString) {
                font.drawString (g, timeString, px - w / 2 + 3, py - 3);
            }
        }
    }
}
