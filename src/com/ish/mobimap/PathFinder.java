/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                            (c) 2003-2006 ISh                              */
/*****************************************************************************/

package com.ish.mobimap;

import javax.microedition.lcdui.*;

public class PathFinder
{
    static short [] way;
    static int wayN;

    private static int start;  // start is always PC

    private static int startUid;

    private static String infoStart, infoEnd;
    private static int wayLength;

    private static int casted [];
    private static int startX, startY;
    private static int endX, endY;

    static final int RESULT_OK = 0;
    static final int RESULT_DATA_REQUIRED = 1;
    static final int RESULT_ILLEGAL_TYPE = 2;
    static final int RESULT_END_IS_START = 3;
    static final int RESULT_OUT_OF_MEMORY = 4;
    static final int RESULT_PATH_NOT_EXISTS = 5;

    /**
     * Set the beginning point of path
     * @param n - object id
     * @return - result
     */
    static int setStart (int n)
    {
        startUid = n;
        int cl = n >> 24;
        int item = n & 0xffffff;
        int sg = item >> 16, it = item & 0xffff;
        int r = RESULT_OK;
        casted = null;
        way = null; wayN = 0;

        switch (cl)
        {
            case Map.CLASS_PC:
                start = item;
                infoStart = ObjectManager.getPcName(start);
                startX = Map.pcx [start]; startY = Map.pcy [start];
                break;
            case Map.CLASS_LABEL:
                casted = castXYTo2Pcs(Map.lbx[sg][it], Map.lby[sg][it]);
                start = casted [0];
                startX = casted [3]; startY = casted [4];
                infoStart = ObjectManager.getLabelName(item >> 16, item & 0xffff);
                break;
            case Map.CLASS_ADDRESS:
                casted = castXYTo2Pcs(Map.blx[sg][it], Map.bly[sg][it]);
                start = casted [0];
                startX = casted [3]; startY = casted [4];
                infoStart = ObjectManager.getAddressName(sg, it);
                break;
            default:
                r = RESULT_ILLEGAL_TYPE;
        }

        return r;
    }
    /**
     * Set starting point and its name
     * @param x -
     * @param y -
     * @param name - name
     */
    static void setStart (int x, int y, String name)
    {
        int pc = castXYToPc (x, y) | Map.MASK_PC;
        setStart(pc);
        infoStart = name;
    }
    /**
     * Tests if start is set
     * @return true/false
     */
    static boolean isStartSet ()
    {
        return start != 0;
    }
    /**
     * Tests if object is set as start point
     * @param uid object uid
     * @return true/false
     */
    static boolean isSameToStart (int uid)
    {
        return uid == startUid;
    }

    /**
     * Clears the path
     */
    public static void clear ()
    {
        way = null;
    }
    /**
     * Finds the path from beginning point to end, using direction and quality information
     * @param end - end point
     * @param useDirections -
     * @param useQuality -
     * @return - result
     */
    static int go (int end, boolean useDirections, boolean useQuality)
    {
        int res = RESULT_OK;
        if (start > 0 && end > 0)
        {
            if (end == startUid) return RESULT_END_IS_START;

            try
            {
                if (!findWay (end, useDirections, useQuality))
                    res = RESULT_PATH_NOT_EXISTS;
            }
            catch (OutOfMemoryError z)
            {
                res = RESULT_OUT_OF_MEMORY;
            }
        }
        else res = RESULT_DATA_REQUIRED;

        return res;
    }

    static boolean findWay (int end, boolean useDirections, boolean useQuality)
    {
        short[] pcparent;
        short[] pcquepos;
        byte[] queSet;
        int[] que2;
        short[] que, que3;

        int pcN = Map.pcN;
        pcparent = new short[pcN];
        pcquepos = new short[pcN];
        queSet = new byte[pcN];
        que2 = new int[pcN];
        que = new short[pcN];
        que3 = new short[pcN];

        for (int i = 0; i < pcN; i++)
        {
            pcparent [i] = queSet[i] = 0;
            pcquepos[i] = -1;
        }

        // mark end-point
        int endCl = end >> 24, endItem = end & 0xffffff;
        endX = endY = -1;

        int sg = endItem >> 16, it = endItem & 0xffff;
        int [] ps = null;

        switch (endCl)
        {
            case Map.CLASS_PC:
                queSet[endItem] = 1;
                infoEnd = ObjectManager.getPcName (endItem);
                break;
            case Map.CLASS_STREET:
                for (int el = 0; el < Map.elementN; el++)
                    if (Map.el2st[el] == endItem)
                    {
                        int aconp = Map.elconp[el], nconp = Map.elconp[el + 1];

                        for (int j = aconp; j < nconp; j++)
                        {
                            short pn = Map.elcon[j];
                            queSet[pn] = 1;
                        }
                    }
                infoEnd = ObjectManager.getStreetName (endItem);
                break;
            case Map.CLASS_LABEL:
                ps = castXYTo2Pcs(Map.lbx[sg][it], Map.lby[sg][it]);
                infoEnd = ObjectManager.getLabelName (sg, it);
                break;
            case Map.CLASS_ADDRESS:
                ps = castXYTo2Pcs(Map.blx[sg][it], Map.bly[sg][it]);
                infoEnd = ObjectManager.getAddressName (sg, it);
                break;
        }
        if (ps != null)
        {
            int direction = ps [2];
            endX = ps [3]; endY = ps [4];
            if (useDirections && (direction == 0x1 || direction == 0x2))
            {
                if ((direction & 0x1) > 0) queSet [ps [0]] = 1;
                else queSet [ps [1]] = 1;
            }
            else
            {
                queSet [ps [0]] = 1; queSet [ps [1]] = 1;
            }
            if (useDirections && direction == 0x3) endX = -1;
        }

        // allocate queue variables
        int queTop = 0, queBack = 1;

        // add start element
        if (casted == null)
        {
            que[0] = (short) (start); que2[0] = 0; que3[0] = 0;
            pcquepos[start] = 0;
        }
        else
        {
            int direction = casted [2];
            if (useDirections && (direction == 0x1 || direction == 0x2))
            {
                if ((direction & 0x1) > 0) placeToQue(pcquepos, que2, que, que3, casted [1], 0);
                else placeToQue(pcquepos, que2, que, que3, casted [0], 0);
            }
            else
            {
                placeToQue(pcquepos, que2, que, que3, casted [0], 0);
                placeToQue(pcquepos, que2, que, que3, casted [1], 1);
                queBack = 2;
            }
        }

        // go!
        short finalPoint = -1;
        while (queBack > queTop)
        {
            short quePc, queParent;
            int queDs;

            quePc = que[queTop];
            queDs = que2[queTop];
            queParent = que3[queTop];
            queTop++;

            pcparent[quePc] = queParent;

            if (queSet[quePc] > 0)
            {
                finalPoint = quePc;
                break;
            }

            queSet[quePc] = -1;

            for (int j = Map.pcconp[quePc]; j < Map.pcconp[quePc + 1]; j++)
            {
                short neigh = (short) (Map.con[j] & 0x7fff);
                if (queSet[neigh] < 0)
                    continue;

                int direction = (Map.conel[j] >> 13) & 0x3;
                if (useDirections && (direction & 0x2) > 0)
                    continue;

                queBack = pushToQue (que, que3, que2, queTop, queBack, quePc, queDs,
                                     neigh, pcquepos,
                                     useQuality && Map.con[j] < 0);
            }
            for (int j = Map.pcconpi[quePc]; j < Map.pcconpi[quePc + 1]; j++)
            {
                short neigh = (short) (Map.coni[j] & 0x7fff);
                if (queSet[neigh] < 0)
                    continue;

                boolean quality = false;

                if (useDirections)
                {
                    boolean m = true;
                    for (int k = Map.pcconp[neigh]; k < Map.pcconp[neigh + 1]; k++)
                        if ( (Map.con[k] & 0x7fff) == quePc)
                        {
                            int direction = (Map.conel[k] >> 13) & 0x3;
                            quality = Map.con[k] < 0;
                            m = ( (direction & 0x1) != 0);
                            break;
                        }
                    if (m)
                        continue;
                }

                queBack = pushToQue (que, que3, que2, queTop, queBack, quePc, queDs,
                                     neigh, pcquepos,
                                     useQuality && quality);
            }
        }

        if (finalPoint < 0) return false;

        way = new short[pcN];
        wayN = 0;
        wayLength = 0;
        short a = finalPoint;
        do
        {
            way[wayN++] = a;
            int b = a;
            a = pcparent[a];
            if (a > 0)
                wayLength += getWeight (a, b);
        }
        while (a != 0);

        if (startX > 0) wayLength += Util.hypoteu (startX - Map.pcx [way [wayN-1]], startY - Map.pcy [way [wayN-1]]);
        if (endX > 0) wayLength += Util.hypoteu (endX - Map.pcx [way [0]], endY - Map.pcy [way [0]]);

        wayLength *= Map.globalScale;

        que = null; que2 = null; que3 = null;
        pcparent = null; queSet = null;

        System.gc ();
        Thread.yield();
        return true;
    }

    private static void placeToQue (short[] pcquepos, int[] que2, short[] que, short[] que3, int p, int pos)
    {
        que [pos] = (short)p;
        que2 [pos] = Util.hypoteu (Map.pcx[p] - startX, Map.pcy[p] - startY);
        que3 [pos] = 0;
        pcquepos [p] = (short)pos;
    }

    private static int pushToQue (short[] que, short[] que3, int[] que2,
                                   int queTop, int queBack, short quePc, int queDs,
                                   short neigh, short[] pcquepos, boolean quality)
    {
        int w = getWeight (quePc, neigh);
        if (!quality) w <<= 1;
        int d = queDs + w;

        int qp = pcquepos [neigh];
        if (qp >= 0)  // don't add pc if there is shorter way in que
            if (que2 [qp] <= d) return queBack;

        int back = queBack, top = queTop;
        int qi;
        int insert;
        if (d < que2 [top]) insert=top;
        else if (d > que2 [back-1]) insert=back;
        else
        {
            do
            {
                qi = (back + top) >> 1;
                boolean cond = d < que2 [qi];
                if (cond)
                    back = qi;
                else
                    top = qi;
            } while (back-top > 1);
            insert=back;
        }

        for (int u = (qp == -1)? queBack: qp; u>insert; u--)
        {
            que [u] = que [u-1]; que2 [u] = que2 [u-1];
            que3 [u] = que3 [u-1];
            pcquepos [que [u]]++;
        }
        que [insert] = neigh; que2 [insert] = d;
        que3 [insert] = quePc;
        pcquepos [neigh] = (short) insert;

        if (qp == -1) queBack++;
        return queBack;
    }
    static int getWeight (int pc1, int pc2)
    {
        return Util.hypoteu(Map.pcx[pc2]-Map.pcx[pc1], Map.pcy[pc2]-Map.pcy[pc1]);
    }
    private static int castXYToPc (int x, int y)
    {
        int n = 0, d = 0xffff;
        for (int i = 0; i < Map.pcN; i++)
        {
            int a = Util.hypot (x - Map.pcx[i], y - Map.pcy[i]);
            if (a < 500) a = Util.hypoteu (x - Map.pcx[i], y - Map.pcy[i]);
            if (a < d)
            {
                d = a; n = i;
            }
        }
        return n;
    }

    private static int [] castXYTo2Pcs (int x, int y)
    {
        return castXYTo2Pcs(x, y, null, 500, 1);
    }

    static int [] castXYTo2Pcs (int x, int y, byte[] pc_out, int threshold, int minPcIndex)
    {
        long min = 0xffffff;
        int [] ps = new int [7];
        ps [6] = 0xffffff;

        for (int i=minPcIndex; i < Map.pcN; i++)
        {
            boolean isOutOfZone = (pc_out != null && pc_out[i] != 2);

            int y0 = Map.pcy [i];

            int diffY = y0 - y;
            if (diffY < 0) {
                continue;  // keep on moving
            }
            if (diffY > 1000) {
                break;   // all reasonable crossroads are passed
            }

            int x0 = Map.pcx [i];
            if (Math.abs(x0 - x) > 1000) {
                continue;
            }

            for (int j = Map.pcconp[i]; j < Map.pcconp[i + 1]; j++)
            {
                int neigh = Map.con [j] & 0x7fff;
                if (isOutOfZone && (pc_out != null && pc_out[neigh] != 2)) {
                    continue;
                }

                long x1 = Map.pcx [neigh], y1 = Map.pcy [neigh];

                // check if (x,y) is inside of extended rectangle formed by crossroads
                if (x < Math.min(x0, x1) - threshold || x > Math.max(x0, x1) + threshold ||
                    y < Math.min(y0, y1) - threshold || y > Math.max(y0, y1) + threshold) {
                    continue;
                }

                long v2 = Util.distanceSquared (x0 - x1, y0 - y1);
                if (v2 == 0) continue;

                // distance from point to segment
                // formula is taken from http://algolist.manual.ru/maths/geom/distance/pointline.php
                long u = (y0 - y1)*x + (x1 - x0)*y + (x0*y1 - x1*y0);
                long u2 = u * u;
                long d2 = u2 / v2;

                long delta2 = d2;

                long w2 = Util.distanceSquared(x - x0, y - y0);
                long t2 = Util.distanceSquared(x - x1, y - y1);

                long a2 = w2 - d2;
                if (a2 > v2) delta2 = t2;

                long b2 = t2 - d2;
                if (b2 > v2) delta2 = w2;

                if (min < delta2) continue;

                min = delta2;
                int v = Util.sqrtLong(v2);
                int a = Util.sqrtLong(a2);

                ps [0] = i; ps [1] = neigh;
                ps [2] = Map.condir[j] & 0x3;
                ps [3] = ((v > 1)? (int)(x0 + ((x1-x0)*a)/v) : x0 + 1);
                ps [4] = ((v > 1)? (int)(y0 + ((y1-y0)*a)/v) : y0 + 1);
                ps [5] = Map.conel[j];
                ps [6] = Util.sqrtLong(delta2);

//                System.out.println ("v = " + v + " a = " + a + " d = " + ps[6]);
//                System.out.println ("");
            }
        }
        if (ps [0] == 0 && pc_out == null)
        {
            ps [1] = ps [0] = castXYToPc(x, y);
        }
        return ps;
    }

    static void drawWay (Graphics g)
    {
        g.setColor(0x0000ff);
        if (way != null)
        {
            for (int i=1; i < PathFinder.wayN; i++)
            {
                int a = PathFinder.way [i-1];
                int n = PathFinder.way [i];

                int ax = Map.pcx [a], ay = Map.pcy [a];
                int nx = Map.pcx [n], ny = Map.pcy [n];

                int x1 = mx (ax),
                    y1 = my (ay),
                    x2 = mx (nx),
                    y2 = my (ny);

                g.drawLine(x1, y1, x2, y2);
            }
            if (endX > 0)
            {
                int x1 = mx (Map.pcx [way [0]]),
                    y1 = my (Map.pcy [way [0]]),
                    x2 = mx (endX),
                    y2 = my (endY);

                g.drawLine (x1, y1, x2, y2);
            }
            if (startX > 0)
            {
                int x1 = mx (Map.pcx [way [wayN-1]]),
                    y1 = my (Map.pcy [way [wayN-1]]),
                    x2 = mx (startX),
                    y2 = my (startY);

                g.drawLine (x1, y1, x2, y2);
            }

        }
        if (start != 0)
        {
            int x = mx(startX), y = my(startY);

            g.fillRect (x-5, y-1, 11, 2);
            g.fillRect (x-1, y-5, 2, 11);

        }
    }

    static int mx (int x)
    {
        return (x - Map.originX) / Map.scale;
    }
    static int my (int y)
    {
        return Map.height - (y - Map.originY) / Map.scale;
    }
    /**
     * Get information about the path
     * @return info
     */
    static String getInfo ()
    {
        return (way == null)?
            msrt.Resources [89] + '\n' + msrt.Resources [86]:
            msrt.Resources [90] + " \"" + infoStart + "\" - \"" + infoEnd + "\". "
            + msrt.Resources [91] + (wayLength / 10)*10;
    }
    /**
     * Get textual description of result code
     * @param r result code
     * @return text
     */
    static String resultCodeDescription (int r)
    {
        return msrt.Resources [r == RESULT_DATA_REQUIRED ? 87 :
                  r == RESULT_ILLEGAL_TYPE ? 127 :
                  r == RESULT_END_IS_START ? 63 :
                  r == RESULT_PATH_NOT_EXISTS ? 48 : 9];
    }
}
