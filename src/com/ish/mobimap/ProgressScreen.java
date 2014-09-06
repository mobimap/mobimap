/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                            (c) 2003-2007 ISh                              */
/*****************************************************************************/

package com.ish.mobimap;

import java.io.*;
import javax.microedition.lcdui.*;

import com.ish.mobimap.ui.*;

/**
 * Splash-screen. Visible at start-up.
 */
public class ProgressScreen
    extends Component
{
    private static final int SPACE = 10;
    private static final int PROGRESS_HEIGHT = 16;

    private Image logo;
    private String title;
    private int progress;
    private String owner;
    private Image banner;

    private int progressY;
    private int progressW;

    private boolean drawBackground;

    public ProgressScreen (Frame frame, String logoFile, String title, String owner,
        String bannerFile)
    {
        super(frame);

        try
        {
            this.logo = Image.createImage (logoFile);
        }
        catch (IOException ex)
        {
        }
        if (bannerFile != null)
        {
            try
            {
                this.banner = Image.createImage (bannerFile);
            }
            catch (IOException ex)
            {
            }
        }
        this.title = title;
        this.owner = owner;
        drawBackground = true;
        isBarTransparent = true;
    }

    /**
     * Repaint splash screen component
     * @param g Graphics
     */
    public void paint(Graphics canvasGraphics)
    {
        Graphics g = getScreenBufferGraphics();

        if (drawBackground)
        {
            g.setFont(Font.getFont (Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
            g.setColor(Design.COLOR_SPLASH_BACKGROUND);
            g.fillRect(0, 0, componentWidth, componentHeight);

            int y = SPACE*2;
            int cw2 = componentWidth >> 1;
            int fh = getFont().getHeight();

            if (logo != null)
            {
                int iw = logo.getWidth();
                int ih = logo.getHeight();
                progressW = iw;

                g.drawImage(logo, cw2, y, Graphics.TOP | Graphics.HCENTER);
                y += ih;
            }
            g.setColor(Design.COLOR_SPLASH_TEXT);
            g.drawString(title, cw2, y, Graphics.TOP | Graphics.HCENTER);
            y += fh + SPACE;

            progressY = y;
            progressW = Math.max(progressW, getFont().stringWidth(title));

            y += SPACE + PROGRESS_HEIGHT;

            if (banner != null)
            {
                int iw = banner.getWidth();
                int ih = banner.getHeight();
                progressW = iw;

                g.drawImage(banner, cw2, y, Graphics.TOP | Graphics.HCENTER);
                y += ih + SPACE;
            }

            if (owner != null)
            {
                g.drawString(msrt.Resources[117], cw2, y, Graphics.TOP | Graphics.HCENTER);
                g.drawString(owner, cw2, y+fh, Graphics.TOP | Graphics.HCENTER);
            }
            drawBackground = false;
        }

        paintScreenBuffer(canvasGraphics);

        // draw progress-bar
        int x = (componentWidth - progressW) / 2;
        int w = (progress * progressW) / 100;

        canvasGraphics.setColor (GraphicsPlus.mixColors (Design.COLOR_SPLASH_PROGRESS_FROM,
            Design.COLOR_SPLASH_PROGRESS_TO,
            progress));
        canvasGraphics.fillRect (x, progressY, w, PROGRESS_HEIGHT);

        canvasGraphics.setColor (Design.COLOR_SPLASH_PROGRESS_BORDER);
        canvasGraphics.drawRect (x, progressY, progressW, PROGRESS_HEIGHT);
    }
    /**
     * Set progress in percents
     * @param p int
     */
    public void setProgress (int p)
    {
        this.progress = p;
        repaint();
    }
}
