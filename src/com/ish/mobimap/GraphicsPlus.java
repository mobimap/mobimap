/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                         (c) 2003-2008 Ilya Shakhat                        */
/*****************************************************************************/

package com.ish.mobimap;

import javax.microedition.lcdui.*;

public class GraphicsPlus
{
    private static final int WHITE = 0xFFFFFF;
    private static final int BLACK = 0x000000;

    /**
     * Draw outlined polygon
     * @param g Graphics
     * @param xPoints int[]
     * @param yPoints int[]
     */
    public static void drawPolygon(Graphics g, int xPoints[], int yPoints[]) {
        int max = xPoints.length - 1;
        for(int i = 0; i < max; i++)
            g.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);

        g.drawLine(xPoints[max], yPoints[max], xPoints[0], yPoints[0]);
    }

    /**
     * Draw filled polygon
     * @param g Graphics
     * @param xPoints int[]
     * @param yPoints int[]
     */
    public static void fillPolygon (Graphics g, int xPoints[], int yPoints[])
    {
        while(xPoints.length > 2)  {
            int a = indexOfLeast(xPoints);
            int b = (a + 1) % xPoints.length;
            int c = (a <= 0) ? xPoints.length - 1 : a - 1;
            int leastInternalIndex = -1;
            boolean leastInternalSet = false;
            if(xPoints.length > 3) {
                for(int i = 0; i < xPoints.length; i++)
                    if(i != a && i != b && i != c && withinBounds(xPoints[i], yPoints[i], xPoints[a], yPoints[a], xPoints[b], yPoints[b], xPoints[c], yPoints[c]) && (!leastInternalSet || xPoints[i] < xPoints[leastInternalIndex])) {
                        leastInternalIndex = i;
                        leastInternalSet = true;
                    }

            }
            if(!leastInternalSet) {
                g.fillTriangle(xPoints[a], yPoints[a], xPoints[b], yPoints[b], xPoints[c], yPoints[c]);
                int trimmed[][] = trimEar(xPoints, yPoints, a);
                xPoints = trimmed[0];
                yPoints = trimmed[1];
                continue;
            }
            int split[][][] = split(xPoints, yPoints, a, leastInternalIndex);
            int poly1[][] = split[0];
            int poly2[][] = split[1];
            fillPolygon(g, poly1[0], poly1[1]);
            fillPolygon(g, poly2[0], poly2[1]);
            break;
        }
    }

    private static boolean withinBounds(int px, int py, int ax, int ay, int bx, int by, int cx, int cy) {
        if(px < min(ax, bx, cx) || px > max(ax, bx, cx) || py < min(ay, by, cy) || py > max(ay, by, cy))
            return false;
        else
            return sameSide(px, py, ax, ay, bx, by, cx, cy) && sameSide(px, py, bx, by, ax, ay, cx, cy) && sameSide(px, py, cx, cy, ax, ay, bx, by);
    }

    private static int[][][] split(int xPoints[], int yPoints[], int aIndex, int bIndex) {
        int firstLen;
        if(bIndex < aIndex)
            firstLen = (xPoints.length - aIndex) + bIndex + 1;
        else
            firstLen = (bIndex - aIndex) + 1;
        int secondLen = (xPoints.length - firstLen) + 2;
        int first[][] = new int[2][firstLen];
        int second[][] = new int[2][secondLen];
        for(int i = 0; i < firstLen; i++) {
            int index = (aIndex + i) % xPoints.length;
            first[0][i] = xPoints[index];
            first[1][i] = yPoints[index];
        }

        for(int i = 0; i < secondLen; i++) {
            int index = (bIndex + i) % xPoints.length;
            second[0][i] = xPoints[index];
            second[1][i] = yPoints[index];
        }

        int result[][][] = new int[2][][];
        result[0] = first;
        result[1] = second;
        return result;
    }

    private static int[][] trimEar(int xPoints[], int yPoints[], int earIndex) {
        int newXPoints[] = new int[xPoints.length - 1];
        int newYPoints[] = new int[yPoints.length - 1];
        int newPoly[][] = new int[2][];
        newPoly[0] = newXPoints;
        newPoly[1] = newYPoints;
        int p = 0;
        for(int i = 0; i < xPoints.length; i++)
            if(i != earIndex) {
                newXPoints[p] = xPoints[i];
                newYPoints[p] = yPoints[i];
                p++;
            }

        return newPoly;
    }

    private static int indexOfLeast(int elements[]) {
        int index = 0;
        int least = elements[0];
        for(int i = 1; i < elements.length; i++)
            if(elements[i] < least) {
                index = i;
                least = elements[i];
            }

        return index;
    }

    private static boolean sameSide(int p1x, int p1y, int p2x, int p2y, int l1x, int l1y, int l2x, int l2y) {
        return ((p1x - l1x) * (l2y - l1y) - (l2x - l1x) * (p1y - l1y)) * ((p2x - l1x) * (l2y - l1y) - (l2x - l1x) * (p2y - l1y)) > 0;
    }

    private static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    private static int max(int a, int b, int c) {
        return Math.max(Math.max(a, b), c);
    }

    /**
     * Fill rectangle with horizontal gradient.
     * @param g Graphics
     * @param x1 int
     * @param y1 int
     * @param x2 int
     * @param y2 int
     * @param from int color of leftmost side of rectangle
     * @param to int color of rightmost side of rectangle
     */
    public static void horizontalGradientFill (Graphics g,
                                     int x1, int y1, int x2, int y2,
                                     int from, int to)
    {
        int r1 = from >> 16;
        int g1 = (from >> 8) & 0xff;
        int b1 = from & 0xff;
        int r2 = to >> 16;
        int g2 = (to >> 8) & 0xff;
        int b2 = to & 0xff;

        int n = y2 - y1;
        int dr = r2 - r1;
        int dg = g2 - g1;
        int db = b2 - b1;

        for (int i = 0; i < n; i++)
        {
            g.setColor ( (i * dr) / n + r1, (i * dg) / n + g1, (i * db) / n + b1);
            g.drawLine (x1, i + y1, x2, i + y1);
        }
    }

    public static void horizontalFillWithFlatBevel (Graphics g, int x1, int y1, int x2, int y2, int color)
    {
        horizontalGradientFill(g, x1, y1, x2, y2, mixColors (color, WHITE, 8), mixColors (color, BLACK, 8));
//        g.setColor (color);
//        g.fillRect (x1, y1, x2 - x1, y2 - y1);
        g.setColor (mixColors (color, WHITE, 30));
        g.drawLine (x1, y1, x2, y1);
        g.setColor (mixColors (color, BLACK, 25));
        g.drawLine (x1, y2 - 1, x2, y2 - 1);
    }

    public static void horizontalFillWithRoundBevel (Graphics g, int x1, int y1, int x2, int y2, int color)
    {
        g.setColor (color);
        g.fillRect (x1, y1, x2 - x1, y2 - y1);
        horizontalGradientFill (g, x1, y1, x2, y1 + 5, mixColors(color, WHITE, 15), color);
    }

    public static void horizontalFillWithCavity (Graphics g, int x1, int y1, int x2, int y2, int color)
    {
        int meanY = y1 + ((y2 - y1) * 20) / 100;
//        horizontalGradientFill (g, x1, y1, x2, meanY, 0xDBD6C2, 0x837B59);
//        horizontalGradientFill (g, x1, meanY, x2, y2, 0x837B59, 0xB0A888);

        int light = mixColors(color, WHITE, 20);
        int dark = mixColors(color, BLACK, 30);

        horizontalGradientFill (g, x1, y1, x2, meanY, light, dark);
        horizontalGradientFill (g, x1, meanY, x2, y2, dark, color);
    }

    /**
     * Fill rectangle with vertical gradient.
     * @param g Graphics
     * @param x1 int
     * @param y1 int
     * @param x2 int
     * @param y2 int
     * @param from int color of top side of rectangle
     * @param to int color of bottom side of rectangle
     */
    public static void verticalGradientFill (Graphics g,
                                     int x1, int y1, int x2, int y2,
                                     int from, int to)
    {
        int r1 = from >> 16;
        int g1 = (from >> 8) & 0xff;
        int b1 = from & 0xff;
        int r2 = to >> 16;
        int g2 = (to >> 8) & 0xff;
        int b2 = to & 0xff;

        int n = x2 - x1;
        int dr = r2 - r1;
        int dg = g2 - g1;
        int db = b2 - b1;

        for (int i = 0; i < n; i++)
        {
            g.setColor ( (i * dr) / n + r1, (i * dg) / n + g1, (i * db) / n + b1);
            g.drawLine (x1 + i, y1, x1 + i, y2);
        }
    }

    /**
     * Mix two colors in specified proportion. Proportion is set in percents.
     * 0% means that only first color used, 100% means that only second color is used.
     * Values between 0% and 100% give mixed color as result.
     * @param color1 int
     * @param color2 int
     * @param percent int
     * @return int
     */
    public static int mixColors (int color1, int color2, int percent)
    {
        int r1 = color1 >> 16;
        int g1 = (color1 >> 8) & 0xff;
        int b1 = color1 & 0xff;
        int r2 = color2 >> 16;
        int g2 = (color2 >> 8) & 0xff;
        int b2 = color2 & 0xff;

        int red = r1 + (percent * (r2 - r1)) / 100;
        int green = g1 + (percent * (g2 - g1)) / 100;
        int blue = b1 + (percent * (b2 - b1)) / 100;

        return (red << 16) | (green << 8) | blue;
    }

    /**
     * Render horizontal shadow. Darker shade is at bottom.
     * @param g Graphics
     * @param screenBuffer Image
     * @param x int
     * @param y int
     * @param width int
     * @param height int
     * @param shadowColor int
     * @param shadowStrength int
     */
    public static void horizontalShadow (Graphics g, Image screenBuffer,
                                         int x, int y, int width, int height,
                                         int shadowColor, int shadowStrength)
    {
        try
        {
            int[] area = new int[width * height];
            screenBuffer.getRGB (area, 0, width, x, y, width, height);
            for (int i = 0, offset = 0; i < height; i++)
            {
                int shade = (shadowStrength * (i + 1)) / height;
                for (int j = 0; j < width; j++, offset++)
                {
                    area[offset] = GraphicsPlus.mixColors (
                        area[offset] & 0xffffff, shadowColor, shade);
                }
            }
            Image im = Image.createRGBImage (area, width, height, false);
            g.drawImage (im, x, y, Graphics.LEFT | Graphics.TOP);
        }
        catch (Exception ex)
        {
        }
    }

    /**
     * Render vertical shadow. Darker color is leftmost.
     * @param g Graphics
     * @param screenBuffer Image
     * @param x int
     * @param y int
     * @param width int
     * @param height int
     * @param shadowColor int
     * @param shadowStrength int
     */
    public static void verticalShadow(Graphics g, Image screenBuffer,
                                    int x, int y, int width, int height,
                                    int shadowColor, int shadowStrength)
    {
        try
        {
            int[] area = new int[width * height];
            screenBuffer.getRGB (area, 0, width, x, y, width, height);
            for (int i = 0, offset = 0; i < height; i++)
            {
                for (int j = 0; j < width; j++, offset++)
                {
                    int shade = (shadowStrength * (width - j)) / width;
                    area[offset] = GraphicsPlus.mixColors (
                        area[offset] & 0xffffff, shadowColor, shade);
                }
            }
            Image im = Image.createRGBImage (area, width, height, false);
            g.drawImage (im, x, y, Graphics.LEFT | Graphics.TOP);
        }
        catch (Exception ex)
        { // any exception that can be thrown when one of coordinates is out of image
        }
    }
}
