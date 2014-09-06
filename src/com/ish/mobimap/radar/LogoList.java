/*****************************************************************************/
/*                               R A D A R  3                                */
/*                       (c) 2003-2008 Ilya Shakhat                          */
/*                           (c) 2006-2008 TMS                               */
/*****************************************************************************/

package com.ish.mobimap.radar;

import java.io.*;
import javax.microedition.lcdui.*;

import com.ish.mobimap.ui.*;
import com.ish.mobimap.*;

public class LogoList
    extends ListM
{
    private Image logo = null;

    public LogoList (msrt parent, Frame frame, String title)
    {
        super(parent, frame, title);

        try
        {
            logo = Image.createImage ("/i/listr");
        }
        catch (IOException ex)
        {
        }
    }

    public void paint(Graphics canvasGraphics)
    {
        super.paint(canvasGraphics);

        if (logo != null)
        {
            int c = getContentHeight ();

            int ih = logo.getHeight();
            int iw = logo.getWidth();

            int x = (componentWidth - iw) / 2;
            int y = (componentHeight - c - ih) / 2 + c;

            canvasGraphics.drawImage(logo, x, y, Graphics.LEFT | Graphics.TOP);
        }
    }
}
