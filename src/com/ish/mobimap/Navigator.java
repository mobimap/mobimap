/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                            (c) 2003-2007 ISh                              */
/*****************************************************************************/

package com.ish.mobimap;

import javax.microedition.lcdui.*;
import com.ish.mobimap.ui.*;
import com.ish.mobimap.radar.*;

public class Navigator
{
    private msrt parent;
    private Map map;

    private int step;
    private boolean refresh;

    public Navigator (msrt parent, Map map)
    {
        this.parent = parent;
        this.map = map;
    }

    /**
     * Dispatch single key press
     * @param keyCode int
     * @param gameAction int
     * @return boolean
     */
    public boolean dispatchSingleKey(int keyCode, int gameAction)
    {
        return dispatchRepeatedKey(keyCode, gameAction, 1, 1);
    }

    /**
     * Dispatch repeating key presses
     * @param keyCode int
     * @param gameAction int
     * @param count int
     * @param acceleration int
     * @return boolean
     */
    public boolean dispatchRepeatedKey(int keyCode, int gameAction, int count, int acceleration)
    {
        boolean dispatched = true;
        int number = keyCode;
        boolean invalidate = true;
        int s = step * acceleration;

        switch (gameAction)
        {
            case Canvas.LEFT:
                if (Map.originX > 0)
                    Map.originX -= s;
                break;
            case Canvas.RIGHT:
                if (Map.originX < Map.cityX)
                    Map.originX += s;
                break;
            case Canvas.DOWN:
                if (Map.originY > 0)
                    Map.originY -= s;
                break;
            case Canvas.UP:
                if (Map.originY < Map.cityY)
                    Map.originY += s;
                break;
            case Canvas.FIRE:
                map.setNavigator (false);
                break;
            default:
            {
                switch (number)
                {
                    case Canvas.KEY_NUM1:
                        Map.zoomOut ();
                        break;
                    case Canvas.KEY_NUM3:
                        Map.zoomIn ();
                        break;
                    case Canvas.KEY_NUM9:
                        map.setNavigator (false);
                        break;
                    case Canvas.KEY_NUM0:
                        if (Map.currentObjectClass > 0)
                        {
                            Map.originX = Map.selectionX;
                            Map.originY = Map.selectionY;
                        }
                        break;
                    case Canvas.KEY_POUND:
                        dispatched = true;
                        break;
                    default:
                        dispatched = false;
                }
            }
        }
        if (dispatched)
        {
            if (invalidate)
                map.repaint ();
            else
                map.setNavigator (false);
        }
        return dispatched;
    }
    /**
     * Pointer is released
     * @param x int
     * @param y int
     */
    public void pointerPressed (int x, int y)
    {
        int scale = Math.max (Map.cityX / Map.width, Map.cityY / Map.height);
        int originX = Map.cityX / 2;
        int originY = Map.cityY / 2;

        int nx = (x - Map.widthHalf) * scale + originX;
        int ny = (Map.heightHalf - y) * scale + originY;

        if (nx < 0) nx = 0;
        else if (nx > Map.cityX) nx = Map.cityX;
        if (ny < 0) ny = 0;
        else if (ny > Map.cityY) ny = Map.cityY;

        Map.originX = nx; Map.originY = ny;
        map.repaint ();
    }

    /**
     * Navigetor becomes visible
     */
    protected void showNotify()
    {
        refresh = true;
    }

    /**
     * Repaint navigator
     * @param g Graphics
     * @param screenBufferGraphics Graphics
     */
    public void paint (Graphics g, Graphics screenBufferGraphics)
    {
        Map.xmin = Map.ymin = 0;
        Map.xmax = Map.cityX; Map.ymax = Map.cityY;
        int orx = Map.originX, ory = Map.originY, sc = Map.scale;
        int vw = sc * Map.widthHalf, vh = sc * Map.heightHalf;
        Map.scale = Math.max (Map.cityX / Map.width, Map.cityY / Map.height);
        Map.originX = Map.cityX / 2 - Map.widthHalf * Map.scale;
        Map.originY = Map.cityY / 2 - Map.heightHalf * Map.scale;
        step = Map.scale * 4;
        vw = vw / Map.scale; vh = vh / Map.scale;

        if (refresh)
        {
            screenBufferGraphics.setColor (0xffffff);
            screenBufferGraphics.fillRect (0, 0, Map.width, Map.height);
            map.drawGS (screenBufferGraphics, Map.gs [1], Map.gsp [1], Map.gspN [1]);
            map.drawStreets (screenBufferGraphics, true, 1, true, false);
            refresh = false;
        }
        map.paintScreenBuffer(g);

        if (Map.currentObjectClass > 0)
        {
            g.setColor (0x00ff00);
            g.drawLine (mx(Map.selectionX), 0, mx(Map.selectionX),
                        Map.height);
            g.drawLine (0, my(Map.selectionY), Map.width,
                        my(Map.selectionY));
        }

        g.setColor (0x0000ff);
        g.drawLine (mx(orx), 0, mx(orx) , Map.height);
        g.drawLine (0, my(ory), Map.width, my(ory));

        g.setColor(0x7f7fff);
        g.drawRect(mx(orx) - vw, my(ory) - vh, vw*2, vh*2);

        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR)
            parent.theLocator.draw (g, false);

        Map.originX = orx; Map.originY = ory; Map.scale = sc;
    }
    static int mx (int x)
    {
        return (x - Map.originX) / Map.scale;
    }
    static int my (int y)
    {
        return Map.height - (y - Map.originY) / Map.scale;
    }
}
