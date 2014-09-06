/*****************************************************************************/
/*                               R A D A R  3                                */
/*                       (c) 2003-2008 Ilya Shakhat                          */
/*                           (c) 2006-2008 TMS                               */
/*****************************************************************************/

package com.ish.mobimap.radar;

import com.ish.mobimap.Map;
import javax.microedition.lcdui.Graphics;
import com.ish.mobimap.FontM;

public class Zone
{
    public static final int RADIUS_MIN = 300;
    public static final int RADIUS_MAX = 5000;

    public int lat;
    public int lon;
    public int radius;
    public String name;

    private int x, y;

    public Zone(int lat, int lon, int radius, String name) {
        this.lat = lat;
        this.lon = lon;
        this.radius = radius;
        this.name = name;
        initXY();
    }

    public Zone clone() {
        return new Zone (lat, lon, radius, name);
    }

    private void initXY()
    {
        x = Map.longitude2x(lon);
        if (x <= 0 || x >= Map.cityX) {
            x = 0;
        }
        y = Map.latitude2y(lat);
        if (y <= 0 || y >= Map.cityY)
        {
            y = 0;
        }
    }

    public void setCoordinates(int lat, int lon) {
        this.lat = lat;
        this.lon = lon;
        initXY();
    }

    public void draw (Graphics g, Map theMap, boolean showName, boolean isActive, boolean isSelected)
    {
        g.setColor (isActive? 0x00BF00: 0x7F7F7F);
        g.setStrokeStyle (Graphics.SOLID);

        int px = Map.mx (x), py = Map.my (y);

        FontM font = theMap.fontHorz;

        g.fillArc(px-2, py-2, 5, 5, 0, 360);

        int rad = radius / Map.scale;
        if (rad > 4)
            g.drawArc (px - rad, py - rad, rad << 1, rad << 1, 0, 360);

        if (showName && Map.scale < 50)
        {
            int w = font.getWidth(name) + 4;
            int h = font.getHeight();

            g.translate(px - w/2, py - h - 1);
            g.setColor(isActive? (isSelected? 0x9FFF9F: 0x7FFF7F): (isSelected? 0xD8D8D8: 0xBFBFBF));
            g.fillRoundRect(0, 0, w, h, 6, 6);

            g.setColor(isSelected? 0xFFFF00 : 0x000000);
            g.drawRoundRect(0, 0, w, h, 6, 6);

            font.drawString(g, name, 1, 9);
            g.translate(-g.getTranslateX(), -g.getTranslateY());
        }
    }
}
