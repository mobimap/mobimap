/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                            (c) 2003-2007 ISh                              */
/*****************************************************************************/

package com.ish.mobimap;

import java.io.*;
import java.util.*;
import javax.microedition.lcdui.*;

/**
 * Module of loading data from files
 * Loading is performed in a separated thread and can be cancelled.
 */
public class DataLoader implements Runnable {
    private msrt parent;
    private int params[];
    boolean isWorking;
    static int providerId;

    // file names obfuscation
    static final String file_blname = "/d/a";
    static final String file_blst = "/d/b";
    static final String file_blx = "/d/c";
    static final String file_bly = "/d/d";
    static final String file_city = "/d/e";
    static final String file_conn = "/d/f";
    static final String file_conp = "/d/g";
    static final String file_conpi = "/d/h";
    static final String file_ct = "/d/i";
    static final String file_elcon = "/d/j";
    static final String file_ellen = "/d/k";
    static final String file_gs = "/d/l3";
    static final String file_gsi = "/d/m3";
    static final String file_lb = "/d/n";
    static final String file_name = "/d/o";
    static final String file_pcx = "/d/p";
    static final String file_pcy = "/d/q";
    static final String file_rt = "/d/r";
    static final String file_sp = "/d/s";
    static final String file_sprtref = "/d/t";
    static final String file_stname = "/d/u";
    static final String file_img = "/d/v";
    static final String file_info = "/d/w";
    static final String file_gsindex = "/d/x";

    static final String file_map_categories = "/map/categories";
    static final String file_map_points = "/map/points";

    /**
     * @param _parent main class
     * @param _map map
     * @param _params parameters
     */
    public DataLoader (msrt _parent, Map _map, int _params[]) {
        parent = _parent;
        params = _params;
    }

    /**
     * Start loading data. The process is in separated thread to give users ability
     * to cancel loading.
     */
    public void run () {
        isWorking = true;
        try {
            readData ();
            parent.dataIsLoaded ();
        } catch (OutOfMemoryError x) {
            Alert errorAlert = new Alert (parent.Resources[17], parent.Resources[9], null, AlertType.ERROR);
            errorAlert.setTimeout (3000);
            parent.display.setCurrent (errorAlert);
            parent.config[msrt.CONFIG_MAP_FONT] = 0;
            parent.destroyApp (true);
        } catch (Exception x) {
            x.printStackTrace ();
            parent.destroyApp (true);
        }
    }

    /**
     * Force cancel of loading the data
     */
    void stop () {
        isWorking = false;
    }

    /**
     * Load data
     * @throws java.lang.Exception
     */
    private void readData () throws Exception {
        Map.pcN = params[0];
        Map.streetN = params[1];
        Map.elementN = params[2];
        Map.stnamesNmax = params[3];
        providerId = params[4];
        Map.cityX = params[5];
        Map.cityY = params[6];

        try {
            readPcs ();
            System.gc ();

            readElements ();
            System.gc ();

            Map.name = new char[Map.SEGMENT_LIMIT][];
            Map.blN = new int[Map.SEGMENT_LIMIT];
            Map.blx = new short[Map.SEGMENT_LIMIT][];
            Map.bly = new short[Map.SEGMENT_LIMIT][];
            Map.blst = new short[Map.SEGMENT_LIMIT][];
            Map.blnamep = new short[Map.SEGMENT_LIMIT][];
            Map.blnumber = new byte[Map.SEGMENT_LIMIT][];

            readBuildings ();

            System.gc ();
            Thread.yield ();

            Map.lbN = new int[Map.SEGMENT_LIMIT];
            Map.lbx = new short[Map.SEGMENT_LIMIT][];
            Map.lby = new short[Map.SEGMENT_LIMIT][];
            Map.lbct = new short[Map.SEGMENT_LIMIT][];
            Map.lbnamep = new short[Map.SEGMENT_LIMIT][];
            Map.lbUid = new int[Map.SEGMENT_LIMIT][];
            Map.lbMeta = new Hashtable[Map.SEGMENT_LIMIT][];

            Map.img = new Hashtable[Map.IMG_SEGMENT_COUNT];
            Map.img[Map.IMG_SEGMENT_STATIC] = new Hashtable (30);
            Map.img[Map.IMG_SEGMENT_DYNAMIC] = new Hashtable (30);

            readCategoriesAndPoints ();
            readGS ();
            readImages ();

            System.gc ();
            Thread.yield ();

            status (100);
        } catch (IOException x) {
            // cancel load (prevent from halting)
            throw new Exception ();
        }
    }

    /**
     * Update progress-bar
     * Yield thread for case of pressing pause command
     * @param p percent
     * @throws java.lang.Exception
     */
    final void status (int p) throws Exception {
        if (!isWorking)
            throw new Exception ();

        parent.progressScreen.setProgress (p);
        Thread.yield ();
    }

    /**
     * --------  DATA LOAD  --------------
     */

    /**
     * Read crossroads
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    void readPcs () throws java.io.IOException, Exception {
        InputStream is;
        DataInputStream in;

        is = parent.getClass ().getResourceAsStream (file_pcx);
        in = new DataInputStream (is);

        int pcN = Map.pcN, elementN = Map.elementN;

        short pcx[] = Map.pcx = new short[pcN];
        short pcy[] = Map.pcy = new short[pcN];
        short pcconp[] = Map.pcconp = new short[pcN + 1];
        short pcconpi[] = Map.pcconpi = new short[pcN + 1];
        Map.pcseg = new short[(Map.cityY >> 8) + 1];

        Map.globalScale = in.readShort ();

        readStreamAsShortArray (in, pcx, pcN);

        if (in.readShort () == 1) {
            Map.hasGeoData = true;
            Map.latitude = in.readInt ();
            Map.longitude = in.readInt ();
            Map.lon2meters = in.readInt ();
            Map.lat2meters = in.readInt ();
            /*System.out.println("lat: " + Map.latitude + " lon: " + Map.longitude +
                               " lon2met: " + Map.lon2meters + " lat2met: " + Map.lat2meters);*/
            Map.timeZone = in.readShort ();
        }
        in.close ();

        is = parent.getClass ().getResourceAsStream (file_pcy);
        in = new DataInputStream (is);

        readStreamAsShortArray (in, pcy, pcN);

        short prev = 0;
        for (short j = 0; j < pcN; j++) {
            pcx[j] ^= providerId;

            pcy[j] += prev;
            prev = pcy[j];
            int prev8 = prev >> 8;
            if (Map.pcseg[prev8] == 0) {
                Map.pcseg[prev8] = j;
            }
        }

        status (10);

        in.close ();

        is = parent.getClass ().getResourceAsStream (file_conp);
        in = new DataInputStream (is);

        byte[] bfpc = new byte[pcN];
        readStreamAsByteArray (in, bfpc, pcN);

        short ptr = 0;
        for (short j = 0; j < pcN; j++) {
            pcconp[j] = ptr;
            ptr += bfpc[j];
        }
        pcconp[pcN] = ptr;

        in.close ();

        is = parent.getClass ().getResourceAsStream (file_conpi);
        in = new DataInputStream (is);

        short temp[] = new short[pcN + 1];

        readStreamAsByteArray (in, bfpc, pcN);

        ptr = 0;
        for (short j = 0; j < pcN; j++) {
            temp[j] = pcconpi[j] = ptr;
            ptr += bfpc[j];
        }
        pcconpi[pcN] = ptr;

        in.close ();

        is = parent.getClass ().getResourceAsStream (file_conn);
        in = new DataInputStream (is);

        short conN = in.readShort ();

        //System.out.println ("pcN: " + pcN);
        //System.out.println ("conN: " + conN);

        short con[] = Map.con = new short[conN];
        short coni[] = Map.coni = new short[pcconpi[pcN]];
        short conel[] = Map.conel = new short[conN];
        Map.condir = new byte[conN];

        readStreamAsShortArray (in, con, conN);

        status (15);

        in.close ();

        // reconstruct crossroads connection data and
        // calculate backward links
        int hasDirections = 0;
        for (short j = 0; j < pcN; j++) {
            int beg = pcconp[j], end = pcconp[j + 1];
            for (int k = beg; k < end; k++) {
                int conk = con[k];
                int f = (conk < 0) ? 0x8000 : 0;
                int d = conk & 0x6000;
                hasDirections |= d;
                conel[k] = (short) elementN;
                conk = con[k] = (short) (j - (conk & 0x1fff));
                Map.condir[k] = (byte) ((d >> 13) | (f >>> 13));
                con[k] |= f;
                coni[temp[conk]++] = j;
            }
        }
        Map.hasRoadDirs = hasDirections > 0;
    }

    /**
     * Read elements
     * @throws IOException
     * @throws Exception
     */
    void readElements () throws java.io.IOException, Exception {
        InputStream is;
        DataInputStream in;

        int pcN = Map.pcN, elementN = Map.elementN, streetN = Map.streetN;

        status (20);

        int stSeg = 1;
        int[] stnamep = Map.stnamep = new int[streetN + 1];

        byte[] bf = new byte[streetN];
        byte[] bfn = new byte[Map.stnamesNmax * 2];

        is = parent.getClass ().getResourceAsStream (file_stname);
        in = new DataInputStream (is);

        readStreamAsByteArray (in, bf, streetN);

        int ptr = 0;
        for (short j = 0; j < streetN; j++) {
            stnamep[j] = ptr;
            ptr += bf[j];
        }
        stnamep[streetN] = ptr;

        int stnamesN = in.readInt ();
        char[] stnames = Map.stnames = new char[stnamesN];

        //System.out.println ("streetN: " + streetN);
        //System.out.println ("stnamesN: " + stnamesN);

        readStreamAsCharArray (in, stnames, stnamesN);

        for (int j = 0; j < stnamesN; j++) {
            stnames[j] = (char) (stnames[j] ^ providerId);
        }

        in.close ();

        /**
         * Load elcon - list of element's pcs
         */
        is = parent.getClass ().getResourceAsStream (file_elcon);
        in = new DataInputStream (is);

        short elconp[] = Map.elconp = new short[elementN + 1];

        short elconN = in.readShort ();

        status (40);

        //System.out.println ("elconN: " + elconN);

        short elcon[] = Map.elcon = new short[elconN];
        readStreamAsShortArray (in, elcon, elconN);

        in.close ();

        int phase = 1;
        ptr = 0;
        for (short j = 0; j < elconN; j++) {
            if ((elcon[j] >> 15) != phase) {
                phase = elcon[j] >> 15;
                elconp[ptr] = j;
                ptr++;
            }
            elcon[j] &= 0x7fff;
        }

        /**
         * Load element length
         */
        is = parent.getClass ().getResourceAsStream (file_ellen);
        in = new DataInputStream (is);

        byte ellen[] = Map.ellen = new byte[elementN + 1];
        short el2st[] = Map.el2st = new short[elementN];
        readStreamAsByteArray (in, ellen, elementN);

        in.close ();

        for (short i = 0, st = 1; i < elementN; i++) {
            if ((ellen[i] & 0x10) == 0)
                el2st[i] = st++;
            else {
                ellen[i] &= 0xf;
                el2st[i] = (short) (st - 1);
            }
        }
        ellen[0] = 3; // to make visible elementless pcs

        // calculate
        short con[] = Map.con, coni[] = Map.coni;
        short conel[] = Map.conel;
        short pcconp[] = Map.pcconp, pcconpi[] = Map.pcconpi;
        for (short i = 0; i < elementN; i++) {
            short beg = elconp[i], end = elconp[i + 1];
            int p = elcon[beg];

            for (int j = beg + 1; j < end; j++) {
                int c = elcon[j];
                short n = 0;
                if (c < 8) { // прямая связь или обратная?
                    int off = pcconp[p] + c;
                    n = (short) (con[off] & 0x7fff);
                    conel[off] = i;
                } else {
                    int off = pcconpi[p] + (c & 0x7);
                    n = coni[off];
                    for (int k = pcconp[n]; k < pcconp[n + 1]; k++)
                        if ((con[k] & 0x7fff) == p) {
                            conel[k] = i;
                            break;
                        }
                }

                elcon[j] = n;
                p = n;
            }
        }
    }

    /**
     * Read buildings
     * @throws Exception
     */
    void readBuildings () throws Exception {
        InputStream is;
        DataInputStream in;

        status (60);

        try {
            is = parent.getClass ().getResourceAsStream (file_blname);
            if (is == null) {
                return;
            }
            in = new DataInputStream (is);

            int seg = Map.SEGMENT_STATIC_ADDRESSES;

            int buildingN = Map.blN[seg] = in.readInt ();

            short blx[] = Map.blx[seg] = new short[buildingN];
            short bly[] = Map.bly[seg] = new short[buildingN];
            short blst[] = Map.blst[seg] = new short[buildingN];
            short blnamep[] = Map.blnamep[seg] = new short[buildingN + 1];
            byte blnumber[] = Map.blnumber[seg] = new byte[buildingN];

            readStreamAsByteArray (in, blnumber, buildingN);

            short len = 0;
            for (int i = 0; i < buildingN; i++) {
                int number = ((int) blnumber[i]) & 0xff;
                blnamep[i] = len;
                if (number >= 230)
                    len += (number - 230);
            }

            Map.name[Map.SEGMENT_STATIC_ADDRESSES] = new char[len];
            readStreamAsCharArray (in, Map.name[Map.SEGMENT_STATIC_ADDRESSES], len);

            in.close ();

            is = parent.getClass ().getResourceAsStream (file_blx);
            in = new DataInputStream (is);
            readStreamAsShortArray (in, blx, buildingN);
            in.close ();

            is = parent.getClass ().getResourceAsStream (file_bly);
            in = new DataInputStream (is);
            readStreamAsShortArray (in, bly, buildingN);
            in.close ();

            is = parent.getClass ().getResourceAsStream (file_blst);
            in = new DataInputStream (is);
            readStreamAsShortArray (in, blst, buildingN);
            in.close ();

            // reconstruct coordinates
            short prevr = -1, pre = 0, prx = 0, pry = 0;
            for (int i = 0; i < buildingN; i++) {
                short r = (blst[i] += pre);
                pre = r;
                if (r == prevr) {
                    blx[i] += prx - 0x100;
                    bly[i] += pry - 0x100;
                }
                prevr = r;
                prx = blx[i];
                pry = bly[i];
            }
            status (80);
        } catch (IOException ex) {
            //System.err.println(ex);
        }
    }

    void readCategoriesAndPoints () throws Exception {
        status (90);

        try {
            readCategories (file_ct);
            readCategories (file_map_categories);

            readLabelsSeg (file_lb, Map.SEGMENT_STATIC_POINTS);
            readLabelsSeg (file_map_points, Map.SEGMENT_PRELOADED_POINTS);
        } catch (java.io.IOException x) {}
    }

    /**
     * Read categories
     * @param fileName String
     * @throws IOException
     */
    void readCategories (String fileName) throws IOException {
        InputStream is = parent.getClass ().getResourceAsStream (fileName);
        if (is == null) {
            return;
        }
        DataInputStream in = new DataInputStream (is);

        in.readByte (); // type = vector
        in.readInt (); // =2
        OnlineLoader.readObject (in); // packet attributes

        OnlineLoader.readCategoriesData (in, Map.CATEGORY_MODE_STATIC);

        in.close ();
    }

    /**
     * Allocate arrays for categories
     * @param n int
     */
    public static void allocCtVars (int n) {
        Map.ctN = n;
        Map.ctName = new char[n][];
        Map.ctIcon = new Image[n];
        Map.ctParent = new short[n];
        Map.ctColor = new int[n];
        Map.ctFontStyle = new short[n];
        Map.ctMode = new byte[n];
        Map.ctIsStartupVisibility = new boolean[n];
        Map.ctIsFilledWithObjects = new boolean[n];
        Map.ctShow = new byte[n];
    }

    /**
     * Read segment of labels
     * @param fileName String
     * @param seg int
     * @throws IOException
     */
    void readLabelsSeg (String fileName, int seg) throws IOException {
        InputStream is = parent.getClass ().getResourceAsStream (fileName);
        if (is == null)
            return;

        DataInputStream in = new DataInputStream (is);

        int lbN = in.readInt ();
//        System.out.println(" seg=" + seg + " lbN=" + lbN);

        Map.lbN[seg] = lbN;

        if (lbN > 0) {
            Map.lbx[seg] = new short[lbN];
            Map.lby[seg] = new short[lbN];
            Map.lbct[seg] = new short[lbN];
            Map.lbUid[seg] = new int[lbN];
            Map.lbnamep[seg] = new short[lbN + 1];

            readStreamAsShortArray (in, Map.lbx[seg], lbN);
            readStreamAsShortArray (in, Map.lby[seg], lbN);
            readStreamAsShortArray (in, Map.lbct[seg], lbN);
            readStreamAsIntArray (in, Map.lbUid[seg], lbN);

            byte[] lens = new byte[lbN];
            readStreamAsByteArray (in, lens, lbN);
            short totalLength = 0;
            for (int i = 0; i < lbN; i++) {
                Map.lbnamep[seg][i] = totalLength;
                totalLength += lens[i];
                Map.lbct[seg][i] = ObjectManager.getCtRemoteToLocal (Map.lbct[seg][i]);
            }
            Map.lbnamep[seg][lbN] = totalLength;

            Map.name[seg] = new char[totalLength];
            readStreamAsCharArray (in, Map.name[seg], totalLength);
        }
        in.close ();
    }

    void readNames () throws IOException {
        InputStream is = parent.getClass ().getResourceAsStream (file_name);
        DataInputStream in = new DataInputStream (is);

        readNamesSeg (in, 0);
        in.close ();
    }

    static void readNamesSeg (DataInputStream in, int seg) throws IOException {
        int nameN = in.readShort ();
        Map.name[seg] = new char[nameN];
        readStreamAsCharArray (in, Map.name[seg], nameN);
    }

    /**
     * READ GRAPHIC STREAM
     */
    void readGS () {

        Map.gs = new short[3][];
        Map.gsp = new short[3][];
        Map.gspN = new int[3];

        try {
            InputStream is = parent.getClass ().getResourceAsStream (file_gsi);
            if (is == null) {
                return;
            }
            DataInputStream in = new DataInputStream (is);
            readGSSeg (in, 1);
            in.close ();

            is = parent.getClass ().getResourceAsStream (file_gs);
            if (is == null)
                return;
            in = new DataInputStream (is);
            readGSSeg (in, 0);
            in.close ();
        } catch (IOException x) {}
    }

    static void readGSSeg (DataInputStream in, int seg) throws IOException {
        int gsN = in.readInt ();
        Map.gs[seg] = new short[gsN];
        readStreamAsShortArray (in, Map.gs[seg], gsN);
        int gspN = Map.gspN[seg] = in.readInt ();
        Map.gsp[seg] = new short[gspN];
        readStreamAsShortArray (in, Map.gsp[seg], gspN);
    }

    void readGSIndex () {
        try {
            InputStream is = parent.getClass ().getResourceAsStream (file_gsindex);
            if (is == null) {
                return;
            }
            DataInputStream in = new DataInputStream (is);

            Hashtable v = (Hashtable) OnlineLoader.readObject (in);

            in.close ();
        } catch (IOException x) {}
    }

    /**
     * READ ICONS AND PHOTOS
     */
    void readImages () {
        try {
            InputStream is = parent.getClass ().getResourceAsStream (file_img);
            if (is == null) {
                return;
            }
            DataInputStream in = new DataInputStream (is);

            readImgSeg (in, 0);

            in.close ();
        } catch (IOException x) {}
    }

    static void readImgSeg (DataInputStream in, int seg) throws IOException {
        short blockN = in.readShort ();

        for (int i = 0; i < blockN; i++) {
            int len = in.readInt ();
            byte[] buf = new byte[len];
            readStreamAsByteArray (in, buf, len);
            Image img = null;
            try {
                img = Image.createImage (buf, 0, len);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            int n = in.readShort ();
            for (; n > 0; n--) {
                int ref = in.readInt ();

                if ((ref >> 24) == Map.CLASS_LABEL) {
                    Map.img[seg].put (new Integer (ref), img);
                }
            }
        }
    }

    /**
     * Convert array of bytes into array of shorts
     * @param dest short[] destination
     * @param src byte[] source
     * @param n int number of elements
     */
    static void readShortArray (short dest[], byte src[], int n) {
        for (int j = 0; j < n; j++) {
            dest[j] = (short) (((src[j << 1] << 8) | (src[(j << 1) | 1] & 0xff)) & 0xffff);
        }
    }

    /**
     * Convert array of bytes into array of chars
     * @param dest destination
     * @param src source
     * @param n number of chars
     */
    static void readCharArray (char dest[], byte src[], int n) {
        for (int j = 0; j < n; j++) {
            dest[j] = (char) (((src[j << 1] << 8) | (src[(j << 1) | 1] & 0xff)) & 0xffff);
        }
    }

    /**
     * Convert array of bytes into array of ints
     * @param dest int[] destination
     * @param src byte[] source
     * @param n int number of ints
     */
    static void readIntArray (int[] dest, byte[] src, int n) {
        for (int i = 0, j = 0; i < (n << 2); i += 4, j++)
            dest[j] = (src[i + 3] & 0xff) | ((src[i + 2] & 0xff) << 8) | ((src[i + 1] & 0xff) << 16) |
                      (src[i] << 24);
    }

    /**
     * Reads data from stream into byte array
     * @param in input stream
     * @param data destination array
     * @param N number of elements to read
     * @throws IOException
     */
    static public void readStreamAsByteArray (DataInputStream in, byte[] data, int N) throws IOException {
        in.readFully (data, 0, N);
    }

    /**
     * Reads data from stream into short array
     * @param in input stream
     * @param data destination array
     * @param N number of elements to read
     * @throws IOException
     */
    static public void readStreamAsShortArray (DataInputStream in, short[] data, int N) throws IOException {
        byte bf[] = new byte[N<<1];
        readStreamAsByteArray (in, bf, N << 1);
        readShortArray (data, bf, N);
    }

    /**
     * Reads data from stream into char array
     * @param in input stream
     * @param data destination array
     * @param N number of elements to read
     * @throws IOException
     */
    static public void readStreamAsCharArray (DataInputStream in, char[] data, int N) throws IOException {
        byte bf[] = new byte[N<<1];
        readStreamAsByteArray (in, bf, N << 1);
        readCharArray (data, bf, N);
    }

    static public void readStreamAsIntArray (DataInputStream in, int[] data, int N) throws IOException {
        byte bf[] = new byte[N<<2];
        readStreamAsByteArray (in, bf, N << 2);
        readIntArray (data, bf, N);
    }
}
