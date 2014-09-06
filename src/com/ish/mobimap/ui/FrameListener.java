/*****************************************************************************/
/*                               m f i n i t y                               */
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                         (c) 2003-2008 Ilya Shakhat                        */
/*****************************************************************************/

package com.ish.mobimap.ui;

import javax.microedition.lcdui.*;

public interface FrameListener
{
    /**
     * Paint contents
     * @param g Graphics
     */
    public void paint(Graphics g);

    /**
     * Dispatch single key event
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     */
    public void dispatchSingleKey(int keyCommand, int keyCode, int gameAction);

    /**
     * Dispatch repeated key. Events are generated while key is down and repeated
     * over some specified time interval.
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     * @param count int event counter
     * @param acceleration int accelaration (useful for navigation)
     */
    public void dispatchRepeatedKey(int keyCommand, int keyCode, int gameAction, int count, int acceleration);

    /**
     * Dispatch release of repeated key.
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     */
    public void dispatchRepeatedKeyRelease(int keyCommand, int keyCode, int gameAction);

    /**
     * Pointer is down
     * @param x int
     * @param y int
     */
    public void pointerPressed(int x, int y);
    /**
     * Pointer is up
     * @param x int
     * @param y int
     */
    public void pointerReleased(int x, int y);

    /**
     * Frame became visible
     */
    public void showNotify();
    /**
     * Frame became hidden
     */
    public void hideNotify();

    /**
     * Command performed on this component
     * @param c Command
     */
    public void commandAction (Command c);

    /**
     * Timer event.
     */
    public void timer();
}
