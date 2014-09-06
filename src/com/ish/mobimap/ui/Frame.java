/*****************************************************************************/
/*                               m f i n i t y                               */
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                         (c) 2003-2008 Ilya Shakhat                        */
/*****************************************************************************/

package com.ish.mobimap.ui;

import java.util.*;
import javax.microedition.lcdui.*;

import com.ish.mobimap.*;
import com.ish.mobimap.net.*;

/**
 * Frame with buttons and menu. Main UI component, responsible for translating hardware events into
 * component-understandable form.
 * Singleton.
 */
public class Frame
    extends Canvas
    implements Runnable, NetActivityListener
{
    // the instance
    private static Frame instance;

    // current component
    private Component component;

    // font for button and menu item titles
    private Font font;
    private Font fontBold;

    // margin/padding
    private static final int MENU_PADDING = 5;
    private static final int MARGIN_TEXT = Design.MARGIN_LEFT;
    private static final int MARGIN_TOP = 2;
    private static final int MENU_TOP_MARGIN = 10;

    /**
     * Indicates whether the thread is working or not
     */
    private boolean isWorking;
    private static final int DELAY_INITIAL = 500;
    private static final int DELAY_BETWEEN = 200;
    private static final int ACCELERATION_LIMIT = 4;
    private int sleepFor;
    private long timeKeyPressed;
    private int keyPressedCode;
    private int keyPressedGameAction;
    private int keyPressedCommand;
    private int keyPressedRepeatCount;
    private boolean isKeyUndispatched;
    private boolean isKeyMultiple;
    private boolean isPointerHandledInternally;

    private Thread me;

    // true, if frame is being repainted
    private boolean isRepainting;

    private Command showMenuCommand;
    private String showMenuCommandLabel;
    private String cancelMenuCommandLabel;

    private String selectMenuItemCommandLabel;
    private int[] selectMenuItemDimensions;

    // commands are hosted by component, in frame they're copied at validate()
    private Command masterCommand;
    private Vector commands;
    private int commandsCount;

    // height of buttons
    private int barHeight;
    // true, if menu is visible
    private boolean isMenuVisible;
    // current menu item
    private int currentMenuItem = -1;
    // the first visible menu item
    private int firstVisibleMenuItem;
    // the last visible menu item
    private int lastVisibleMenuItem;
    // count of visible menu items
    private int visibleMenuItemCount;

    private int screenHeight, screenWidth;

    private int[][] dimensions;
    private Command[] items;
    private int menuWidth, menuHeight;
    private int itemsCount;

    private boolean isBarTransparent; // value is taken from component

    private boolean netActivity;              // true, if there's net activity
    private boolean netActivityIsInBackground;  // true, if net activity is in background
    private int netActivityCounter;
    private boolean timerRepaint; // true, if repaint is caused by timer event
    private boolean allowTimerEvents; // true, if timer events are forwarded to component

    private boolean forceRepaint; // true, if full repaint is required independing on net activity

    private Command cancelCommand;
    private String netActivityString;

    private NetActivityController netActivityController;

    private Component eventComponent;

    private Image screenBuffer;

    /**
     * Creates frame.
     */
    public Frame ()
    {
        instance = this;

        setFullScreenMode(true);

        fontBold = font = Font.getFont (Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        showMenuCommandLabel = msrt.Resources[155];
        cancelMenuCommandLabel = msrt.Resources[10];
        selectMenuItemCommandLabel = msrt.Resources[77];
        showMenuCommand = new Command(showMenuCommandLabel, Command.SCREEN, 1);

        cancelCommand = new Command(msrt.Resources[10], Command.SCREEN, 1);
        netActivityString = msrt.Resources[67];

        isWorking = true;
        sleepFor = 0;
        me = new Thread (this);
        me.start ();
    }

    public void destroy()
    {
        isWorking = false;
        stop();
    }

    public static Frame getInstance() {
        return instance;
    }

    public void updateParameters(int fontParam)
    {
        int size = (fontParam == 0? Font.SIZE_SMALL:
                             (fontParam == 1? Font.SIZE_MEDIUM: Font.SIZE_LARGE));
        font = Font.getFont (Font.FACE_SYSTEM, Font.STYLE_PLAIN, size);
        fontBold = Font.getFont (Font.FACE_SYSTEM, Font.STYLE_BOLD, size);
    }

    /**
     * Set new component for the frame. Old component receives hideNotify event,
     * new component receives showNotify event.
     * @param newComponent Component
     */
    synchronized public void setComponent (Component newComponent)
    {
        isKeyUndispatched = false;
        if (component != null)
            component.hideNotify();
        component = newComponent;
        isMenuVisible = false;
        if (newComponent != null)
        {
            newComponent.setDimensions (getWidth (), getHeight ());
            validate ();
            newComponent.showNotify ();
            repaint ();
        }
    }

    /**
     * Get current component.
     * @return Component
     */
    public Component getComponent()
    {
        return component;
    }

    /**
     * Run method of Frame's thread
     */
    synchronized public void run()
    {
        while (isWorking)
        {
            if (component != null)
            {
                // repeating key events
                if (System.currentTimeMillis () - DELAY_INITIAL > timeKeyPressed
                    && isKeyUndispatched && !isRepainting)
                {
                    isKeyMultiple = true;

                    if (eventComponent != null)
                    {
                        keyPressedRepeatCount++;
                        int acceleration = keyPressedRepeatCount / 5 + 1;
                        if (acceleration > ACCELERATION_LIMIT)
                            acceleration = ACCELERATION_LIMIT;
                        dispatchRepeatedKey (keyPressedCommand, keyPressedCode,
                            keyPressedGameAction, keyPressedRepeatCount,
                            acceleration);
                    }
                }
                else
                {
                    timer();
                }
            }

            // sleep for a while
            try
            {
                wait (sleepFor);
            }
            catch (InterruptedException ex)
            {
            }
        }
    }

    /**
     * timer event
     */
    private void timer ()
    {
        if (netActivity)
        {
            timerRepaint = true;
            netActivityCounter++;
            if (msrt.BUILD_VERSION == msrt.BUILD_RADAR)
                repaint();
            else
                repaint(0, screenHeight - barHeight, screenWidth, barHeight);
        }
        else if (!isMenuVisible && allowTimerEvents)
        {
            component.timer ();
        }
    }

    /**
     * System keyPressed event notification.
     * Method just remembers its parameters and wakes thread that processes
     * repeated key management.
     * @param keyCode int
     */
    protected void keyPressed (int keyCode)
    {
//        System.out.println ("keyCode: " + keyCode + " gameAction: " + getGameAction (keyCode) +
//                            " keyName: " + getKeyName(keyCode) +
//                            " keyCommand: " + getKeyCommand(keyCode));

        eventComponent = component;

        timeKeyPressed = System.currentTimeMillis ();
        keyPressedCode = keyCode;
        keyPressedGameAction = getGameAction (keyCode);
        keyPressedCommand = getKeyCommand (keyCode);
        keyPressedRepeatCount = 0;
        isKeyUndispatched = true;
        isKeyMultiple = false;

        synchronized (this)
        {
            notify ();
        }
    }

    /**
     * System keyReleased event notification.
     * Method is synchronized to avoid the case when dispatchSingleKey and
     * dispatchRepeatedKey are both called.
     * @param keyCode int
     */
    synchronized public void keyReleased (int keyCode)
    {
        isKeyUndispatched = false;

        if (component == eventComponent)
        {
            if (isKeyMultiple)
            {
                dispatchRepeatedKeyRelease (getKeyCommand (keyCode), keyCode, getGameAction (keyCode));
            }
            else
            {
                dispatchSingleKey (getKeyCommand (keyCode), keyCode, getGameAction (keyCode));
            }
        }
    }

    /**
     * Dispatch single key stroke. If key wasn't handled internally, call
     * appropriate method from component.
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     */
    private void dispatchSingleKey(int keyCommand, int keyCode, int gameAction)
    {
        if (!handleKey(keyCode, gameAction, 1)) {
            component.dispatchSingleKey (keyCommand, keyCode, gameAction);
        }
    }

    /**
     * Dispatch repeated key stroke. If key wasn't handled internally, call
     * appropriate method from component.
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     * @param count int
     * @param acceleration int
     */
    private void dispatchRepeatedKey(int keyCommand, int keyCode, int gameAction, int count, int acceleration)
    {
        if (!handleKey(keyCode, gameAction, count)) {
            component.dispatchRepeatedKey (keyCommand, keyCode, gameAction, count, acceleration);
        }
    }

    private void dispatchRepeatedKeyRelease(int keyCommand, int keyCode, int gameAction) {
        component.dispatchRepeatedKeyRelease(keyCommand, keyCode, gameAction);
    }

    /**
     * Handle key presses. If key is handled internally returns true.
     * @param keyCode int
     * @param count int
     * @return boolean
     */
    private boolean handleKey (int keyCode, int gameAction, int count)
    {
        int keyCommand = getKeyCommand(keyCode);

        if (netActivity && !netActivityIsInBackground)
        {
            if (keyCommand == KEY_COMMAND_SOFT_RIGHT || keyCommand == KEY_COMMAND_ENTER)
                netActivityController.cancel ();

            return true;
        }

        if (keyCommand == KEY_COMMAND_SOFT_LEFT || (isMenuVisible && gameAction == Canvas.LEFT))
        {
            if (count == 1)
            {
                if (itemsCount > 2)
                    changeMenuVisibility ();
                else if (itemsCount == 2)
                    component.commandAction (items[1]);
            }
        }
        else if (keyCommand == KEY_COMMAND_SOFT_RIGHT || (isMenuVisible && gameAction == Canvas.RIGHT))
        {
            if (count == 1)
            {
                if (isMenuVisible)
                    menuItemSelected ();
                else if (masterCommand != null)
                    component.commandAction (masterCommand);
            }
        }
        else
        {
            if (isMenuVisible)
            {
                switch (gameAction)
                {
                    case Canvas.UP:
                        moveToNextItem (-1);
                        break;
                    case Canvas.DOWN:
                        moveToNextItem (+1);
                        break;
                    case Canvas.FIRE:
                        menuItemSelected();
                        break;
                }
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Menu item is selected. Apply associated command
     */
    private void menuItemSelected ()
    {
        component.commandAction(items[currentMenuItem+2]);
        isMenuVisible = false;
        forceRepaint = true;
        repaint();
    }

    /**
     * Change state of menu: visible / hidden
     */
    private void changeMenuVisibility ()
    {
        isMenuVisible = !isMenuVisible;
        if (commandsCount == 0) {
            isMenuVisible = false;
        }
        if (isMenuVisible) {
            currentMenuItem = 0;
            firstVisibleMenuItem = 0;
            lastVisibleMenuItem = firstVisibleMenuItem + visibleMenuItemCount;
        }
        allowTimerEvents = false;
        repaint();
    }
    /**
     * Move to next menu item
     * @param dir int
     */
    private void moveToNextItem (int dir)
    {
        currentMenuItem = (currentMenuItem + dir + commandsCount) % commandsCount;
        if (currentMenuItem >= lastVisibleMenuItem) {
            lastVisibleMenuItem = currentMenuItem + 1;
            firstVisibleMenuItem = lastVisibleMenuItem - visibleMenuItemCount;
        }
        if (currentMenuItem < firstVisibleMenuItem) {
            firstVisibleMenuItem = currentMenuItem;
            lastVisibleMenuItem = firstVisibleMenuItem + visibleMenuItemCount;
        }
        repaint();
    }

    /**
     * Show notify
     */
    protected void showNotify()
    {
//        System.out.println ("show notify");
        go();
        component.showNotify();
        forceRepaint = true;
        repaint();
    }
    /**
     * Hide notify
     */
    protected void hideNotify()
    {
//        System.out.println ("hide notify");
        pause();
        eventComponent = null;
        component.hideNotify();
    }

    synchronized private void go ()
    {
        sleepFor = DELAY_BETWEEN;
        allowTimerEvents = true;
        notify ();
    }
    synchronized private void pause ()
    {
        sleepFor = 0;
    }
    synchronized private void stop ()
    {
        isWorking = false;
        notify ();
    }

    /**
     * Pointer is pressed
     * @param x int
     * @param y int
     */
    public void pointerPressed(int x, int y)
    {
        isPointerHandledInternally = true;

        if (netActivity && !netActivityIsInBackground)
        {
            netActivityController.cancel ();
            return;
        }

        if (isMenuVisible &&
            x > selectMenuItemDimensions[0] && x < selectMenuItemDimensions[2] &&
            y > selectMenuItemDimensions[1] && y < selectMenuItemDimensions[3])
        {
            menuItemSelected();
            return;
        }

        eventComponent = component;
        for (int i = isMenuVisible? dimensions.length-1: 1; i >=0; i--)
        {
            int[] dim = dimensions[i];
            if (dim != null)
                if (x > dim[0] && x < dim[2] && y > dim[1] && y < dim[3])
                {
                    Command cmd = items[i];
                    if (cmd == showMenuCommand)
                        changeMenuVisibility ();
                    else
                    {
                        component.commandAction (cmd);
                        isMenuVisible = false;
                        forceRepaint = true;
                        repaint();
                    }
                    return;
                }
        }
        if (isMenuVisible)
            changeMenuVisibility();
        else
        {
            isPointerHandledInternally = false;
            component.pointerPressed (x, y);
        }
    }
    /**
     * Pointer is released
     * @param x - x
     * @param y - y
     */
    public void pointerReleased (int x, int y)
    {
        if (component == eventComponent && !isPointerHandledInternally)
            component.pointerReleased(x, y);
    }

    /**
     * Repaint Frame
     * @param g Graphics
     */
    public void paint (Graphics g)
    {
        if (!isWorking) return; // avoid repaint after destroy

        isRepainting = true;

//        msrt.DEBUG_LOG += "\nframe.rep (" + component.getClass().getName() + ")";

        try
        {
            int sw = super.getWidth ();
            int sh = super.getHeight ();
            g.setFont (font);

            // real screen height is set only when displayable becomes visible,
            // so it's possible that it will change (on Nokia S60, Motorola Synergy <2.2)
            if (sh != screenHeight || sw != screenWidth)
            {
                screenHeight = sh;
                screenWidth = sw;
                screenBuffer = null;
                validate ();
            }

            // alloc screen buffer
            if (screenBuffer == null)
            {
                screenBuffer = Image.createImage (sw, sh);
            }

            int fontHeight = font.getHeight ();
            g.setClip (0, 0, screenWidth, screenHeight);

            // normal (not-timer-driven) repaint or forced by showNotify
            if ( (!timerRepaint || forceRepaint) && !netActivity)
            {
                // update component contents
                if (!isMenuVisible)
                {
                    component.setDimensions (getWidth (), getHeight ());
                    component.paint (g);
                    g.setClip (0, 0, screenWidth, screenHeight);
                    g.setFont (font);
                }
                else
                {
                    // show menu
                    int top = screenHeight - menuHeight - barHeight;

                    g.setColor (Design.COLOR_FRAME_MENU_BACKGROUND);
                    g.fillRect (0, top, menuWidth, menuHeight);
                    g.setFont (font);

                    for (int i = firstVisibleMenuItem, j = 2; i < lastVisibleMenuItem && j < items.length; i++, j++)
                    {
                        if (items[i+2] != null)
                        {
                            int[] dimj = dimensions[j];
                            if (i == currentMenuItem)
                            {
                                if (msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP) {
                                    g.setColor(Design.COLOR_FRAME_MENU_SELECTED_BACKGROUND);
                                    g.fillRect(dimj[0], dimj[1], dimj[2] - dimj[0], dimj[3] - dimj[1]);
                                } else if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
                                    GraphicsPlus.horizontalFillWithRoundBevel (g,
                                        dimj[0], dimj[1], dimj[2], dimj[3],
                                        Design.COLOR_FRAME_MENU_SELECTED_BACKGROUND);
                                }
                                g.setColor (Design.COLOR_FRAME_MENU_SELECTED_TEXT);
                            }
                            else
                                g.setColor (Design.COLOR_FRAME_MENU_TEXT);

                            String s = items[i+2].getLabel ();
                            g.drawString (s, dimj[0] + MARGIN_TEXT, dimj[3], Graphics.BOTTOM | Graphics.LEFT);
                        }
                    }
                    int mw = menuWidth + 1;
                    if (visibleMenuItemCount < commandsCount) {
                        drawMenuScrollbar(g, menuWidth, top, Design.SCROLLBAR_WIDTH, menuHeight,
                            firstVisibleMenuItem, visibleMenuItemCount, commandsCount);
                        mw += Design.SCROLLBAR_WIDTH;
                    }
                    g.setColor (Design.COLOR_FRAME_BORDER);
                    g.drawRect ( -1, top, mw, menuHeight);
                }

                // show bar if it's visible
                boolean showBar = component.isBarVisible || isMenuVisible;
                if (showBar)
                {
                    if (component.invalid)
                        validate ();

                    drawBar (g);
                }
            }
            else // net activity
            {
                component.paintScreenBuffer (g);

                if (netActivityIsInBackground)
                {
                    drawBar (g);

                    // show activity icon
                    int a = (screenWidth - barHeight) / 2;
                    int y = screenHeight - barHeight;
                    int t = netActivityCounter % 8;
                    int r = (t + 2) * 2;
                    int color = GraphicsPlus.mixColors (Design.COLOR_FRAME_BUTTON_TEXT,
                        Design.COLOR_FRAME_BAR_BACKGROUND,
                        t * 10);
                    g.setColor (color);
                    g.drawArc ( (screenWidth - r) / 2, screenHeight - (barHeight + r) / 2,
                               r, r, 0, 360);
                }
                else
                {
                    // show progress-bar
                    if (msrt.BUILD_VERSION == msrt.BUILD_RADAR)
                    {
                        int mWidth = screenWidth;
                        int mHeight = screenHeight;

                        int mRadius = Math.min (mWidth, mHeight) / 2;

                        int mX = (mWidth - mRadius) / 2;
                        int mY = (mHeight - mRadius) / 2;

                        int seconds = (netActivityCounter / 5) % 60;
                        int minutes = (netActivityCounter / 5) / 60;

                        // Clear the whole screen
                        g.setColor (0xffffff);
                        g.fillRect (0, 0, screenWidth, screenHeight);

                        // Now draw the clock
                        g.setColor (minutes << 5, 240 - (seconds << 1), 0);
                        g.drawArc (mX, mY, mRadius, mRadius, 0, 360);
                        g.fillArc (mX, mY, mRadius, mRadius, 90 - minutes * 6, -seconds * 6);

                        // Draw the message
                        g.setColor (0x000000);
                        g.drawString (netActivityString, mWidth >> 1, mY + mRadius,
                                      Graphics.TOP | Graphics.HCENTER);

                        int y = screenHeight - barHeight;

                        String cancelString = cancelCommand.getLabel ();
                        int w = font.stringWidth (cancelString) + MARGIN_TEXT * 2;
                        int x = screenWidth - w;

                        g.drawString (cancelString, x + MARGIN_TEXT, screenHeight,
                                      Graphics.BOTTOM | Graphics.LEFT);
                    }
                    else // BUILD_MOBIMAP
                    {
                        int y = screenHeight - barHeight;

                        String cancelString = cancelCommand.getLabel ();
                        int w = font.stringWidth (cancelString) + MARGIN_TEXT * 2;
                        int x = screenWidth - w;

                        GraphicsPlus.horizontalFillWithFlatBevel (g, 0, y, screenWidth, screenHeight,
                            Design.COLOR_FRAME_BAR_BACKGROUND);

                        g.setColor (Design.COLOR_FRAME_PROGRESS_BACKGROUND);
                        g.fillRect (0, y, x, barHeight);

                        int a = font.stringWidth (netActivityString) + MARGIN_TEXT * 2;
                        g.setClip (a, y, x - a, barHeight);
                        g.setColor (Design.COLOR_FRAME_PROGRESS_TEXT);
                        int mid = screenHeight - barHeight / 2;
                        for (int i = netActivityCounter * 3 + a, j = 0; i > 0; i -= 11, j++)
                        {
                            int r = ( (j * 71 + 7) % 47) % 5 + 1;
                            g.fillArc (i - r, mid - r, 2 * r, 2 * r, 0, 360);
                        }
                        g.setClip (0, 0, screenWidth, screenHeight);

                        g.drawString (netActivityString, MARGIN_TEXT, screenHeight,
                                      Graphics.BOTTOM | Graphics.LEFT);

                        g.setColor (Design.COLOR_FRAME_BAR_TEXT);
                        g.drawString (cancelString, x + MARGIN_TEXT, screenHeight,
                                      Graphics.BOTTOM | Graphics.LEFT);

                        g.setColor (Design.COLOR_FRAME_BORDER);
                        g.drawLine (0, y, screenWidth, y);
                    }
                }
            }

            if (msrt.features[msrt.FEATURE_DEBUG])
            {
                long mem = Runtime.getRuntime ().freeMemory ();
                long mem2 = Runtime.getRuntime ().totalMemory ();
                String s = Long.toString (mem) + "/" + Long.toString (mem2);
                g.setColor (0xffffff);
                g.setFont (font);
                g.fillRect (screenWidth - font.stringWidth (s) - 2, 0, screenWidth, fontHeight);
                g.setColor (0x0000df);
                g.drawString (s, screenWidth, 0, Graphics.RIGHT | Graphics.TOP);
            }
        }
        catch (Exception ex)
        {
            // paint can run asynchronously to changes in UI, so E can occur
            ex.printStackTrace();
        }

        forceRepaint = false;
        timerRepaint = false;
        isRepainting = false;
        allowTimerEvents = true;
    }

    /**
     * Draw buttons bar
     * @param g Graphics
     */
    private void drawBar (Graphics g)
    {
        int[] dim0 = dimensions[0];
        int[] dim1 = dimensions[1];

        g.setFont(fontBold);

        // bar
        if (!isBarTransparent || isMenuVisible)
        {
            int y = screenHeight - barHeight;

            if (msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP)
            {
                GraphicsPlus.horizontalFillWithCavity (g, 0, y, screenWidth, screenHeight,
                                                       Design.COLOR_FRAME_BAR_BACKGROUND);
            }
            else if (msrt.BUILD_VERSION == msrt.BUILD_RADAR)
            {
                GraphicsPlus.horizontalFillWithRoundBevel (g, 0, y, screenWidth, screenHeight,
                    Design.COLOR_FRAME_BAR_BACKGROUND);
            }

            if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
                // draw border above bar
                g.setColor (Design.COLOR_FRAME_BORDER);
                g.drawLine (0, y, screenWidth, y);
            }

            g.setColor (Design.COLOR_FRAME_BAR_TEXT);
        }
        else
        {
            g.setColor (Design.COLOR_FRAME_BUTTON_TEXT_GLOW);
            if (masterCommand != null)
                g.drawString (masterCommand.getLabel (), dim0[0] + 1 + MARGIN_TEXT,
                              dim0[3] + 1,
                              Graphics.BOTTOM | Graphics.LEFT);
            if (itemsCount > 1)
                g.drawString (items[1].getLabel (), dim1[0] + 1 + MARGIN_TEXT, dim1[3] + 1,
                              Graphics.BOTTOM | Graphics.LEFT);

            g.setColor (Design.COLOR_FRAME_BUTTON_TEXT);
        }

        // right button
        if (isMenuVisible)
            g.drawString (selectMenuItemCommandLabel,
                          selectMenuItemDimensions[0] + MARGIN_TEXT, selectMenuItemDimensions[3],
                          Graphics.BOTTOM | Graphics.LEFT);
        else
            if (masterCommand != null)
                g.drawString (masterCommand.getLabel (), dim0[0] + MARGIN_TEXT, dim0[3],
                              Graphics.BOTTOM | Graphics.LEFT);

        // left button
        if (itemsCount > 1)
        {
            if (isMenuVisible)
                g.drawString (cancelMenuCommandLabel, dim1[0] + MARGIN_TEXT, dim1[3],
                              Graphics.BOTTOM | Graphics.LEFT);
            else
                g.drawString (items[1].getLabel (), dim1[0] + MARGIN_TEXT, dim1[3],
                              Graphics.BOTTOM | Graphics.LEFT);
        }
    }

    private void drawMenuScrollbar(Graphics g, int x, int y, int w, int h, int top, int visible, int total) {
        g.translate(x, y);

        // background
        GraphicsPlus.verticalGradientFill(g,
                                    0, 0, w, h,
                                    Design.COLOR_SCROLLBAR_BACKGROUND_FROM,
                                    Design.COLOR_SCROLLBAR_BACKGROUND_TO);
        // thumb
        int thumbHeight = (h * visible) / total;
        int thumbYTop = (top * h) / total;
        int thumbYBottom = thumbYTop + thumbHeight;
        GraphicsPlus.verticalGradientFill(g,
                                    0, thumbYTop, w, thumbYBottom,
                                    Design.COLOR_SCROLLBAR_THUMB_TO,
                                    Design.COLOR_SCROLLBAR_THUMB_FROM);

        g.setColor (Design.COLOR_SCROLLBAR_BORDER);
        g.drawLine (1, thumbYTop, w - 1, thumbYTop);
        g.drawLine (1, thumbYBottom, w - 1, thumbYBottom);

        // flare on thumb
        int fy = thumbYTop + thumbHeight / 2 - 2, fx2 = w - 2;
        g.setColor (Design.COLOR_SCROLLBAR_FLARE);
        g.drawLine (2, fy, fx2, fy);
        g.drawLine (2, fy+4, fx2, fy+4);
        fy ++;
        g.setColor (Design.COLOR_SCROLLBAR_BORDER);
        g.drawLine (2, fy, fx2, fy);
        g.drawLine (2, fy+4, fx2, fy+4);

        g.translate(-x, -y);
    }

    /**
     * Validate the Frame.
     */
    private void validate()
    {
        masterCommand = component.masterCommand;
        commands = component.commands;
        isBarTransparent = component.isBarTransparent;

        // calculates dimensions of menu items
        int fontHeight = font.getHeight();
        int itemHeight = fontHeight + MARGIN_TOP;
        barHeight = fontHeight + MARGIN_TOP;
        commandsCount = commands.size();
        itemsCount = (commandsCount == 0)? 1: ((commandsCount == 1)? 2: commandsCount+2);
        dimensions = new int[commandsCount+2][];
        items = new Command[commandsCount+2];

        // master command
        items[0] = masterCommand;
        if (masterCommand != null)
        {
            dimensions[0] = setItemDimensions (screenWidth -
                                               fontBold.stringWidth (masterCommand.getLabel ()) -
                                               MARGIN_TEXT * 2, screenHeight - fontHeight,
                                               screenWidth, screenHeight);
        }

        // if (commandsCount == 1) - there is only one command in menu, we put it on the bar
        Command leftCommand = (commandsCount == 1)? (Command)commands.elementAt(0): showMenuCommand;
        items[1] = leftCommand;
        dimensions[1] = setItemDimensions(0, screenHeight - fontHeight,
                                          fontBold.stringWidth(leftCommand.getLabel()), screenHeight);

        // select menu item command dimensions
        selectMenuItemDimensions = setItemDimensions(screenWidth -
                                               fontBold.stringWidth (selectMenuItemCommandLabel) -
                                               MARGIN_TEXT * 2, screenHeight - fontHeight,
                                               screenWidth, screenHeight);

        if (commandsCount > 1)
        {
            visibleMenuItemCount = (screenHeight - barHeight - MENU_TOP_MARGIN) / itemHeight;
            menuHeight = Math.min(commandsCount, visibleMenuItemCount) * itemHeight;
            menuWidth = 0;
            Command[] list = new Command[commandsCount];
            for (int i = 0; i < commandsCount; i++)
            {
                String s = ( list[i] = (Command) commands.elementAt (i)).getLabel ();
                int sw = font.stringWidth (s);
                if (sw > menuWidth) {
                    menuWidth = sw;
                }
            }
            menuWidth += MENU_PADDING * 2;

            // sort commands by priority
            for (int i=0; i < commandsCount-1; i++)
                for (int j=i+1; j < commandsCount; j++)
                {
                    Command a = list[i];
                    Command b = list[j];
                    if (a.getPriority() > b.getPriority())
                    {
                        list[j] = a;
                        list[i] = b;
                    }
                }

            // calculate items dimensions
            for (int i = 0, y = screenHeight - menuHeight - barHeight; i < commandsCount; i++, y += itemHeight)
            {
                items[i + 2] = list[i];
                dimensions[i + 2] = setItemDimensions (0, y, menuWidth, y + itemHeight);
            }
        }
        component.invalid = false;
    }

    /**
     * Set dimensions of menu item
     * @param x1 int
     * @param y1 int
     * @param x2 int
     * @param y2 int
     * @return int[]
     */
    private int[] setItemDimensions (int x1, int y1, int x2, int y2)
    {
        int[] a = new int[4];
        a[0] = x1;
        a[1] = y1;
        a[2] = x2;
        a[3] = y2;
        return a;
    }

    /**
     * Get height
     * @return int
     */
    public int getHeight()
    {
        return super.getHeight() - (isBarTransparent? 0: barHeight);
    }

    /**
     * Height of bar with buttons
     * @return int
     */
    public int getBarHeight()
    {
        return barHeight;
    }

    /**
     * Start net activity
     * @param olc NetActivityController
     */
    synchronized public void netStart(NetActivityController olc)
    {
        netActivity = true;
        netActivityCounter = 0;
        netActivityController = olc;
        netActivityIsInBackground = olc.isInBackground();

//        System.out.println (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//        System.out.println ("" + System.currentTimeMillis() + "  net start");
    }

    /**
     * Stop net activity
     */
    synchronized public void netStop()
    {
//        System.out.println ("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
//        System.out.println ("" + System.currentTimeMillis() + "  net stop");

        netActivity = false;
        forceRepaint = true;
        repaint();
    }

    /**
     * Set string that is painted while net is active
     * @param s String
     */
    public void setNetActivityString(String s)
    {
        netActivityString = s;
    }

    /**
     * Get Frame's font
     * @return Font
     */
    public Font getFont()
    {
        return font;
    }

    /**
     * Get screen buffer
     * @return Image
     */
    public Image getScreenBuffer()
    {
        return screenBuffer;
    }


    //
    //      K E Y    C O D E S    M A G I C
    //

    public static final int KEY_COMMAND_SOFT_LEFT = -6;
    public static final int KEY_COMMAND_SOFT_RIGHT = -7;
    public static final int KEY_COMMAND_ENTER = Canvas.FIRE;
    public static final int KEY_COMMAND_UP = Canvas.UP;
    public static final int KEY_COMMAND_DOWN = Canvas.DOWN;
    public static final int KEY_COMMAND_LEFT = Canvas.LEFT;
    public static final int KEY_COMMAND_RIGHT = Canvas.RIGHT;

    /**
     * Translate keyCode into command.
     * Different devices have different codes for keys, different names for them,
     * but this function tryies to unify them.
     * @param keyCode int
     * @return int
     */
    public int getKeyCommand (int keyCode)
    {
        int gameAction = 0;
        String keyName = null;
        try
        {
            gameAction = getGameAction (keyCode);
            keyName = getKeyName(keyCode);
        }
        catch (IllegalArgumentException ex)
        {
        }
        if (keyName != null)
        {
            keyName = keyName.toLowerCase();

            if (keyName.indexOf("soft") >= 0)
            {
                char ch = keyName.charAt(keyName.length()-1);
                if (ch == '1' || keyName.startsWith("left"))
                    return KEY_COMMAND_SOFT_LEFT;
                else if (ch == '2' || ch == '4')
                    return KEY_COMMAND_SOFT_RIGHT;
            }
            if (keyName.equals("clear") || keyName.equals("back"))
                return KEY_COMMAND_SOFT_RIGHT;
            else if (keyName.equals("select") || keyName.equals("ok") ||
                     keyName.equals("send") || keyName.equals("fire") ||
                     keyName.equals("navi-center") || keyName.equals("start") ||
                     keyName.equals("enter"))
                return KEY_COMMAND_ENTER;
            else if (keyName.equals("up") || keyName.equals("navi-up") ||
                     keyName.equals("up arrow"))
                return KEY_COMMAND_UP;
            else if (keyName.equals("down") || keyName.equals("navi-down") ||
                     keyName.equals("down arrow"))
                return KEY_COMMAND_DOWN;
            else if (keyName.equals("left") || keyName.equals("navi-left") ||
                     keyName.equals("left arrow") || keyName.equals("sideup"))
                return KEY_COMMAND_LEFT;
            else if (keyName.equals("right") || keyName.equals("navi-right") ||
                     keyName.equals("right arrow") || keyName.equals("sidedown"))
                return KEY_COMMAND_RIGHT;

            if (keyName.equals("q") || keyName.equals("w"))
                return KEY_COMMAND_SOFT_LEFT;

            if (keyName.equals("o") || keyName.equals("p"))
                return KEY_COMMAND_SOFT_RIGHT;

            if (keyName.equals("escape"))
                return KEY_COMMAND_ENTER;
        }

        if (msrt.vendor == msrt.VENDOR_NOKIA && keyCode == -11)
            return 0;

        if (msrt.vendor == msrt.VENDOR_SIEMENS && keyCode == -22)
            return 0;

        switch(keyCode)
        {
            case -6:  // nokia, WTK, samsung
            case -21: // motorola
            case 21:
            case 65:  // A
            case 66:  // B
            case 105: // i
            case -202:
            case 113: // q
            case 57345:
                return KEY_COMMAND_SOFT_LEFT;

            case -7:  // nokia, WTK, samsung
            case -22: // motorola
            case 22:
            case 67:  // C
            case 68:  // D
            case 106: // j
            case -203:
            case 112: // p
            case 57346:
                return KEY_COMMAND_SOFT_RIGHT;

            case -5:   // select key. WTK, samsung
            case -10:  // send key. WTK
            case -20:
            case 20:
            case 23:
            case -14:
            case -26: // navi-center, siemens
            case -200:
            case 13:  // enter
                return KEY_COMMAND_ENTER;

            case -8:
            case -11: // send key. siemens
            case -16:
            case -19:
            case -204:
                return KEY_COMMAND_SOFT_RIGHT;
        }
        if (gameAction == 0)
            return keyCode;
        else return gameAction;
    }
}
