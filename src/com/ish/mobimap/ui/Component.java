/*****************************************************************************/
/*                               m f i n i t y                               */
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                         (c) 2003-2008 Ilya Shakhat                        */
/*****************************************************************************/

package com.ish.mobimap.ui;

import javax.microedition.lcdui.*;
import java.util.Vector;

import com.ish.mobimap.GraphicsPlus;

public class Component
    implements FrameListener
{
    /**
     * Frame controller
     */
    private Frame frame;

    /**
     * Component Events Listener
     */
    private ComponentListener componentListener;

    /**
     * Width of component
     */
    protected int componentWidth;
    /**
     * Height of component
     */
    protected int componentHeight;

    /**
     * Master command -- shown at the right side
     */
    protected Command masterCommand;
    /**
     * Ordinary commands -- showm on the left side
     */
    protected Vector commands;
    /**
     * true, if component must be revalidated
     */
    protected boolean invalid = true;

    /**
     * true, if menu bar is transparent (i.e. no background is painted)
     */
    protected boolean isBarTransparent;

    /**
     * true, if menu bar is visible
     */
    protected boolean isBarVisible;

    /**
     * Width and height of scrollbar's thumb
     */
    protected int thumbH, thumbY;

    /**
     * x-position of scrollbar
     */
    protected int scrollbarX;

    /**
     * New component
     * @param frame Frame
     */
    public Component (Frame frame)
    {
        this.frame = frame;
        commands = new Vector();
        componentWidth = frame.getWidth();
        componentHeight = frame.getHeight();
        isBarVisible = true;
    }

    public void setComponentListener (ComponentListener fcl)
    {
        componentListener = fcl;
    }

    protected void setDimensions (int width, int height)
    {
        componentWidth = width;
        componentHeight = height;
    }

    public void paint(Graphics g) {}


    /**
     * Dispatch single key event
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     */
    public void dispatchSingleKey(int keyCommand, int keyCode, int gameAction) {
    }

    /**
     * Dispatch repeated key. Events are generated while key is down and repeated
     * over some specified time interval.
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     * @param count int event counter
     * @param acceleration int accelaration (useful for navigation)
     */
    public void dispatchRepeatedKey(int keyCommand, int keyCode, int gameAction, int count, int acceleration) {
    }

    /**
     * Dispatch release of repeated key.
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     */
    public void dispatchRepeatedKeyRelease(int keyCommand, int keyCode, int gameAction) {
    }

    /**
     * Pointer is down
     * @param x int
     * @param y int
     */
    public void pointerPressed(int x, int y) {}
    /**
     * Pointer is up
     * @param x int
     * @param y int
     */
    public void pointerReleased(int x, int y) {}

    /**
     * Frame became visible
     */
    public void showNotify() {}
    /**
     * Frame became hidden
     */
    public void hideNotify() {}

    /**
     * Command performed on this component
     * @param c Command
     */
    public void commandAction (Command c)
    {
        if (componentListener != null)
            componentListener.commandAction(this, c);
    }

    public void timer()
    {
    }

    /**
     * Set master command
     * @param command Command
     */
    public void setMasterCommand (Command command)
    {
        masterCommand = command;
        invalid = true;
    }

    /**
     * Add ordinary command
     * @param command Command
     */
    public void addCommand (Command command)
    {
        for (int i=0; i < commands.size(); i++)
            if (commands.elementAt(i).equals(command)) return;

        commands.addElement(command);
        invalid = true;
    }
    /**
     * Remove ordinary command
     * @param command Command
     */
    public void removeCommand (Command command)
    {
        commands.removeElement(command);
        invalid = true;
    }

    /**
     * Remove all commands for this component
     */
    public void removeAllCommands()
    {
        commands.removeAllElements();
        invalid = true;
    }

    /**
     * Repaint component
     */
    public void repaint()
    {
//        System.out.println ("" + System.currentTimeMillis() + "  component.repaint");
        frame.repaint();
    }

    /**
     * Check if component supports pointer events
     * @return boolean
     */
    public boolean hasPointerEvents()
    {
        return frame.hasPointerEvents();
    }

    /**
     * Get screen buffer
     * @return Image
     */
    public Graphics getScreenBufferGraphics()
    {
        Graphics g = frame.getScreenBuffer().getGraphics();
        g.setFont(frame.getFont());
        return g;
    }

    /**
     * Paint contents of screen buffer onto the Graphics
     * @param g Graphics
     */
    public void paintScreenBuffer(Graphics g)
    {
        g.drawImage(frame.getScreenBuffer(), 0, 0, Graphics.TOP | Graphics.LEFT);
    }

    /**
     * Get current Frame font
     * @return Font
     */
    public Font getFont()
    {
        return frame.getFont();
    }

    /**
     * Get underlying Canvas object
     * @return Canvas
     */
    public Canvas getCanvas()
    {
        return frame;
    }

    /**
     * Draw scrollbar. X-position and thumb dimensions are stored locally
     * @param g Graphics
     * @param heightTotal int total height of component content
     * @param titleHeight int top offset of scrollbar
     * @param visibleTop int top offset of component content
     */
    protected void drawScrollbar (Graphics g, int heightTotal, int titleHeight,
                                  int visibleTop)
    {
        int x = componentWidth - Design.SCROLLBAR_WIDTH;
        int realHeight = componentHeight - titleHeight;

        if (heightTotal > realHeight)
        {
            scrollbarX = componentWidth - Design.SCROLLBAR_WIDTH;

            // background
            GraphicsPlus.verticalGradientFill(g,
                                        scrollbarX, titleHeight, componentWidth, componentHeight,
                                        Design.COLOR_SCROLLBAR_BACKGROUND_FROM,
                                        Design.COLOR_SCROLLBAR_BACKGROUND_TO);
            // thumb
            thumbH = realHeight * realHeight / heightTotal;
            thumbY = (visibleTop * realHeight) / heightTotal + titleHeight;
            int ty = thumbY + thumbH;
            GraphicsPlus.verticalGradientFill(g,
                                        scrollbarX, thumbY, componentWidth, ty,
                                        Design.COLOR_SCROLLBAR_THUMB_TO,
                                        Design.COLOR_SCROLLBAR_THUMB_FROM);

            g.setColor (Design.COLOR_SCROLLBAR_BORDER);
            g.drawLine (scrollbarX + 1, thumbY, componentWidth - 1, thumbY);
            g.drawLine (scrollbarX + 1, ty, componentWidth - 1, ty);

            // flare on thumb
            int fx1 = scrollbarX + 2, fy = thumbY + thumbH / 2 - 2, fx2 = componentWidth - 2;
            g.setColor (Design.COLOR_SCROLLBAR_FLARE);
            g.drawLine (fx1, fy, fx2, fy);
            g.drawLine (fx1, fy+4, fx2, fy+4);
            fy ++;
            g.setColor (Design.COLOR_SCROLLBAR_BORDER);
            g.drawLine (fx1, fy, fx2, fy);
            g.drawLine (fx1, fy+4, fx2, fy+4);

            // border
            g.setColor (Design.COLOR_SCROLLBAR_BORDER);
            g.drawLine (scrollbarX, titleHeight, scrollbarX, componentHeight);
        }
        else
            scrollbarX = componentWidth;
    }
}
