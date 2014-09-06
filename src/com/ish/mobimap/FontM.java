/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                            (c) 2003-2006 ISh                              */
/*****************************************************************************/

package com.ish.mobimap;

import javax.microedition.lcdui.*;
import java.io.*;

/**
 * Font Engine
 */
public class FontM {
    /** character glyphs */
    private Image glyphs[];
    /** offsets */
    private short off[][];
    /** characters width */
    private byte width[][];
    /** number of chars in each segment */
    private short quantity[] = {96, 224, 96};
    /** start of segment: ascii, latin and cyrillic */
    private short start[] = {32, 160, 1024};

    private int color;
    private Font systemFont;
    private int type;
    private int segs;
    private int fontParam;

    public final static int SEG_ASCII = 1;
    public final static int SEG_LATIN = 2;
    public final static int SEG_CYR = 4;

    public final static int FONT_SYSTEM = 0;
    public final static int FONT_SOFTWARE = 1;

    private final static int SOFTWARE_FONT_HEIGHT = 14;

    /**
     * Creates new Font
     * @param segs segments
     * @param color font color
     * @param fontType type of font: system or programm
     * @param fontParam int
     */
    public FontM (int segs, int color, int fontType, int fontParam) {
        this.color = color;
        this.type = fontType;
        this.segs = segs;
        this.fontParam = fontParam;
        off = new short[3][];
        width = new byte[3][];
        glyphs = new Image[3];

        init ();
    }

    private void init () {
        if (type == FONT_SOFTWARE) {
            for (int i = 0; i < 3; i++) {
                if (((segs >> i) & 0x1) != 0) {
                    loadSeg (i, color);
                }
            }
        } else {
            systemFont = Font.getFont (Font.FACE_SYSTEM, Font.STYLE_PLAIN,
                                       (fontParam == 0) ? Font.SIZE_SMALL :
                                       (fontParam == 1) ? Font.SIZE_MEDIUM : Font.SIZE_LARGE);
        }
    }

    /**
     * Changes type of font
     * @param newType int new type: programm or system
     * @param newFontParam int
     */
    public void changeType (int newType, int newFontParam) {
        if (newType != type || (newType == FONT_SYSTEM && newFontParam != fontParam)) {
            type = newType;
            fontParam = newFontParam;
            init ();
        }
    }

    /**
     * Load segment
     * @param seg segment
     * @param color font color
     */
    private void loadSeg (int seg, int color) {
        String glyphsFile = "mg";
        String metricsFile = "mm";
        int segfile = seg + 1;

        try {
            InputStream in = getClass ().getResourceAsStream ("/f/" + metricsFile + segfile);
            if (in == null) {
                return;
            }

            int q = quantity[seg];
            off[seg] = new short[q];
            width[seg] = new byte[q];
            short sum = 0;

            for (int i = 0; i < q; i++) {
                off[seg][i] = sum;
                sum += (width[seg][i] = (byte) in.read ());
            }

            glyphs[seg] = readPNG ("/f/" + glyphsFile + segfile, color);
        } catch (IOException exception) {
        }
    }

    /**
     * Draw one character
     * @param g graphics
     * @param ch character
     * @param x coor-x
     * @param y coor-y
     * @param anchor anchor point
     * @return char width
     */
    private int drawCharInt (Graphics g, char ch, int x, int y, int anchor) {
        int w = 0;
        if (type == FONT_SOFTWARE) {
            int seg = 0;
            if (ch > start[2]) {
                seg = 2;
            } else if (ch > start[1]) {
                seg = 1;
            }

            int sym = ch - start[seg];
            if (sym > quantity[seg] || sym < 0) {
                return 0;
            }
            if (width[seg] == null) {
                return 0;
            }

            int k = g.getClipX ();
            int l = g.getClipY ();
            int i1 = g.getClipWidth ();
            int j1 = g.getClipHeight ();

            w = width[seg][sym];
            if (anchor == Graphics.BASELINE) {
                y -= 10;
            }
            g.setClip (x, y, w, 14);
            g.drawImage (glyphs[seg], x - off[seg][sym], y, Graphics.TOP | Graphics.LEFT);

            g.setClip (k, l, i1, j1);
        } else {
            g.setColor (color);
            g.setFont (systemFont);
            g.drawChar (ch, x, y, anchor | Graphics.LEFT);
            w = systemFont.charWidth (ch);
        }
        return w;
    }

    /**
     * Draw character
     * @param g graphics
     * @param ch character
     * @param x coor-x
     * @param y coor-y
     * @param anchor anchorPoint
     * @return char width
     */
    public int drawChar (Graphics g, char ch, int x, int y, int anchor) {
        return drawCharInt (g, ch, x, y, anchor);
    }

    /**
     * Draws the specified String.
     * @param g graphics
     * @param s string to be drawn
     * @param x coor-x
     * @param y coor-y
     */
    public void drawString (Graphics g, String s, int x, int y) {
        char ch[] = s.toCharArray ();
        int len = ch.length;
        for (int i = 0; i < len; i++) {
            x += drawCharInt (g, ch[i], x, y, Graphics.BASELINE);
        }
    }

    /**
     * Draws the specified characters.
     * @param g graphics
     * @param s the array of characters to be drawn
     * @param offset the start offset in the data
     * @param length the number of characters to be drawn
     * @param x the x coordinate of base point
     * @param y the y coordinate of base point
     */
    public void drawChars (Graphics g, char s[], int offset, int length, int x, int y) {
        for (int i = 0; i < length; i++) {
            x += drawCharInt (g, s[i + offset], x, y, Graphics.BASELINE);
        }
    }

    /**
     * Get character width.
     * @param ch character
     * @return char width
     */
    public int getWidth (char ch) {
        if (type == FONT_SOFTWARE) {
            int seg = 0;
            if (ch > start[2]) {
                seg = 2;
            } else if (ch > start[1]) {
                seg = 1;
            }

            int sym = ch - start[seg];
            if (sym > quantity[seg] || sym < 0) {
                return 0;
            }
            if (width[seg] == null) {
                return 0;
            }
            return width[seg][sym];
        } else {
            return systemFont.charWidth (ch) + 1;
        }
    }

    public int getWidth (String s) {
        return charsWidth (s.toCharArray (), 0, s.length ());
    }

    /**
     * Get width of characters
     * @param ch characters array
     * @param offset the start of data
     * @param length the length of data
     * @return total width
     */
    public int charsWidth (char ch[], int offset, int length) {
        int w = 0;
        for (int i = 0; i < length; i++) {
            w += getWidth (ch[offset + i]);
        }
        return w;
    }

    /**
     * Get character height (constant)
     * @return char height
     */
    public int getHeight () {
        return (type == FONT_SOFTWARE) ? SOFTWARE_FONT_HEIGHT : systemFont.getHeight ();
    }

    /**
     * Read glyphs file and change black color in palette to specified color
     * @param fileName file to be read
     * @param newColor new color
     * @return image that contains glyphs
     */
    private Image readPNG (String fileName, int newColor) {
        final int PNG_IEND = 1229278788;
        final int PNG_PLTE = 1347179589;

        byte[] imageData = null;

        try {
            InputStream is = getClass ().getResourceAsStream (fileName);
            DataInputStream in = new DataInputStream (is);

            ByteArrayOutputStream baos = new ByteArrayOutputStream (1000);
            DataOutputStream daos = new DataOutputStream (baos);

            long signature = in.readLong ();
            daos.writeLong (signature);

            boolean done = false;

            while (!done) {
                int length = in.readInt ();
                daos.writeInt (length);

                byte bf[] = new byte[length + 4];
                DataLoader.readStreamAsByteArray (in, bf, length + 4);

                int type = (bf[0] << 24) | (bf[1] << 16) | (bf[2] << 8) | bf[3];

                if (type == PNG_IEND)
                    done = true;

                if (type == PNG_PLTE) {
                    bf[4] = (byte) (newColor >> 16);
                    bf[5] = (byte) ((newColor >> 8) & 0xff);
                    bf[6] = (byte) (newColor & 0xff);
                }

                daos.write (bf);

                in.readInt (); // CRC of file

                int crcMy = (int) crc (bf, length + 4);
                daos.writeInt (crcMy);
            }

            imageData = baos.toByteArray ();

            daos.close ();
            in.close ();
        } catch (IOException ex) {
        }

        if (imageData != null)
            return Image.createImage (imageData, 0, imageData.length);
        else
            return null;
    }


    /**
     * CRC-32 ALGORITHMS
     * Source: PNG (Portable Network Graphics) Specification, Version 1.2
     */

    /* Table of CRCs of all 8-bit messages. */
    private static long crc_table[];

    /* Flag: has the table been computed? Initially false. */
    private static boolean crc_table_computed = false;

    /**
     * Make the table for a fast CRC.
     */
    private static void make_crc_table () {
        crc_table = new long[256];

        long c;
        int n, k;

        for (n = 0; n < 256; n++) {
            c = (long) n;

            for (k = 0; k < 8; k++) {
                if ((c & 1) > 0) {
                    c = 0xedb88320L ^ (c >> 1);
                }
                else {
                    c = c >> 1;
                }
            }
            crc_table[n] = c;
        }
        crc_table_computed = true;
    }

    /**
     * Update a running CRC with the bytes buf[0..len-1] -- the CRC
     * should be initialized to all 1's, and the transmitted value
     * is the 1�s complement of the final running CRC (see the
     * crc() routine below)).
     *
     * @param crc initial crc
     * @param buf data
     * @param len data length
     * @return resulting crc
     */
    private static long update_crc (long crc, byte[] buf, int len) {
        long c = crc;
        int n;

        if (!crc_table_computed) {
            make_crc_table ();
        }

        for (n = 0; n < len; n++) {
            c = crc_table[(int) (c ^ buf[n]) & 0xff] ^ (c >> 8);
        }
        return c;
    }

    /**
     * Return the CRC of the bytes buf[0..len-1]
     * @param buf data
     * @param len data length
     * @return crc value
     */
    private static long crc (byte[] buf, int len) {
        return update_crc (0xffffffffL, buf, len) ^ 0xffffffffL;
    }
}
