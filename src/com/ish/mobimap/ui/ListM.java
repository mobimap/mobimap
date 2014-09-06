/*****************************************************************************/
/*                               m f i n i t y                               */
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                         (c) 2003-2008 Ilya Shakhat                        */
/*****************************************************************************/

package com.ish.mobimap.ui;

import javax.microedition.lcdui.*;
import java.util.*;

import com.ish.mobimap.*;

public class ListM
    extends Component
{
    private Vector listString;  // text part of item
    private Vector listImage;   // image part of item
    private Vector listColor;   // foreground color of item
    private Vector listBackground; // background color of item

    private String title;       // title of list

    private boolean invalid;

    private int selected = 0;

    private int itemHeight;
    private int visibleTop;
    private int visibleBottom;
    private int imageWidth;
    private int realHeight;
    private int titleHeight;
    private int itemsPerPage;
    private int itemsWidth;

    private int colorTitleBackground = Design.COLOR_LIST_TITLE_BACKGROUND;
    private int colorTitleText = Design.COLOR_LIST_TITLE_TEXT;

    private int selectedTop;
    private int selectedBottom;
    private int selectedX;
    private int selectedOffset;
    private int selectedWidth;
    private boolean isSelectedFitIn;
    private int selectedIndexAtLastPaint;

    private boolean timerRepaint;
    private int timerCounter, timerCounter2;

    private Command backCommand;

    private static final int ICON_MARGIN = 4;

    private static final int SPACE = 4;
    private static final int MARGIN_TOP = 2;

    private static final int SELECTED_SCROLL_STEP = 8;
    private static final int PAUSE = 3;

    private ListSelectionListener selectionListener;

    public ListM (msrt parent, Frame frame, String title)
    {
        super(frame);
        this.title = title;

        setMasterCommand(parent.selectCommand);

        listString = new Vector();
        listImage = new Vector();
        listColor = new Vector();
        listBackground = new Vector();

        invalid = true;
    }

    /**
     * Set List look - title background and text color
     * @param colorTitleBackground int
     * @param colorTitleText int
     */
    public void setLook (int colorTitleBackground, int colorTitleText)
    {
        this.colorTitleBackground = colorTitleBackground;
        this.colorTitleText = colorTitleText;
    }

    /**
     * Sets ListSelectionListener. This listener is notified about changes of selection.
     * @param selectionListener ListSelectionListener
     */
    public void setSelectionListener (ListSelectionListener selectionListener)
    {
        this.selectionListener = selectionListener;
    }

    /**
     * Set command that will be executed on pressing 'left' button
     * @param backCommand Command
     */
    public void setBackCommand(Command backCommand)
    {
        this.backCommand = backCommand;
    }

    /**
     * Paint contents
     * @param canvasGraphics Graphics
     */
    public void paint(Graphics canvasGraphics)
    {
        Graphics g = getScreenBufferGraphics();

        if (timerRepaint && selectedIndexAtLastPaint == selected)
            paintOnTimer(g);
        else
            paintNormal(g);

        paintScreenBuffer(canvasGraphics);
        timerRepaint = false;
    }

    private void paintOnTimer(Graphics g)
    {
        selectedOffset = Math.min(selectedOffset, selectedWidth - itemsWidth);

        g.setClip(selectedX, selectedTop, itemsWidth, itemHeight);

        if (msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP) {
            g.setColor(Design.COLOR_LIST_SELECTED_BACKGROUND);
            g.fillRect(0, selectedTop, componentWidth, selectedBottom - selectedTop);
        } else if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            GraphicsPlus.horizontalFillWithRoundBevel (g,
                                                  0, selectedTop, componentWidth, selectedBottom,
                                                  Design.COLOR_LIST_SELECTED_BACKGROUND);
        }

        g.setColor(Design.COLOR_LIST_SELECTED_TEXT);
        int b = (itemHeight - getFont().getHeight())/2;
        g.drawString((String)listString.elementAt(selected), selectedX - selectedOffset,
                     selectedTop + b, Graphics.TOP | Graphics.LEFT);
    }

    private void paintNormal(Graphics g)
    {
        g.setColor(Design.COLOR_LIST_BACKGROUND);
        g.fillRect(0, 0, componentWidth, componentHeight);

        if (invalid) validate();

        // draw title
        if (title != null)
        {
            titleHeight = getFont().getHeight () + SPACE;
            if (msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP) {
//                GraphicsPlus.horizontalFillWithFlatBevel (g, 0, 0, componentWidth,
//                    titleHeight, colorTitleBackgroundTo);
                g.setColor(colorTitleBackground);
                g.fillRect(0, 0, componentWidth, titleHeight);
            } else if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
                GraphicsPlus.horizontalFillWithRoundBevel (g, 0, 0, componentWidth,
                    titleHeight, colorTitleBackground);
            }
            g.setColor (colorTitleText);
            g.drawString (title, Design.MARGIN_LEFT, MARGIN_TOP, Graphics.TOP | Graphics.LEFT);
//            g.setColor (Design.COLOR_LIST_BORDER);
//            g.drawLine (0, titleHeight - 1, componentWidth, titleHeight - 1);
        }
        else
            titleHeight = 0;

        realHeight = componentHeight - titleHeight;
        visibleBottom = visibleTop + realHeight;
        itemsPerPage = realHeight / itemHeight;

        g.setClip(0, titleHeight, componentWidth, realHeight);

        // draw list contents
        int x = Design.MARGIN_LEFT;
        if (imageWidth > 0)
            x += imageWidth + ICON_MARGIN;
        int count = listString.size();
        for (int i = 0; i < count; i++)
        {
            int y = itemHeight * i - visibleTop + titleHeight;

            if (y < -itemHeight)
                continue;
            if (y > componentHeight)
                break;

            String text = (String)listString.elementAt(i);

            if (i == selected)
            {
                selectedIndexAtLastPaint = selected;
                selectedTop = y;
                selectedBottom = y + itemHeight;
                selectedX = x;
                selectedOffset = 0;
                selectedWidth = getFont().stringWidth(text);
                timerCounter = 0;
                timerCounter2 = 0;

                if (msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP)
                {
                    g.setColor(Design.COLOR_LIST_SELECTED_BACKGROUND);
                    g.fillRect(0, selectedTop, componentWidth, selectedBottom - selectedTop);
                }
                else if (msrt.BUILD_VERSION == msrt.BUILD_RADAR)
                {
                    GraphicsPlus.horizontalFillWithRoundBevel(g, 0, selectedTop, componentWidth, selectedBottom,
                        Design.COLOR_LIST_SELECTED_BACKGROUND);
                }
            }
            else
            {
                int c = ((Integer)listBackground.elementAt(i)).intValue();
                if (c < 0) c = Design.COLOR_LIST_BACKGROUND;
                g.setColor(c);
                g.fillRect(0, y, componentWidth, itemHeight);
            }

            Object imageObj = listImage.elementAt(i);
            if (imageObj != null)
            {
                Image image = (Image)imageObj;
                int b = (itemHeight - image.getHeight())/2;
                int c = (imageWidth - image.getWidth())/2;
                g.drawImage (image, Design.MARGIN_LEFT + c, y + b, Graphics.TOP | Graphics.LEFT);
            }

            g.setColor((i == selected)? Design.COLOR_LIST_SELECTED_TEXT: Design.COLOR_LIST_TEXT);
            int b = (itemHeight - getFont().getHeight())/2;
            g.drawString(text, x, y + b, Graphics.TOP | Graphics.LEFT);
        }

        // draw scrollbar
        int heightTotal = count * itemHeight;
        drawScrollbar(g, heightTotal, titleHeight, visibleTop);

        itemsWidth = scrollbarX - x; // valid value for scrollbarX is known only now
        isSelectedFitIn = selectedWidth < itemsWidth;

        g.setClip(0, 0, componentWidth, componentHeight);
    }

    /**
     * Validate list. Calculate lines position.
     */
    private void validate()
    {
        // tune width for icon size
        imageWidth = 0;
        itemHeight = 0;
        for (int i=0; i < listImage.size(); i++)
        {
            Image im = (Image)listImage.elementAt (i);
            if (im != null)
            {
                int w = im.getWidth();
                if (w > imageWidth) imageWidth = w;

                int h = im.getHeight();
                if (h > itemHeight) itemHeight = h;
            }
        }

        int h = getFont().getHeight();
        if (h > itemHeight)
            itemHeight = h;

        itemHeight += SPACE;

        invalid = false;
        visibleTop = 0;
    }

    public void timer()
    {
        if (!isSelectedFitIn && selectedIndexAtLastPaint == selected)
        {
            timerCounter2++;
            if (timerCounter2 < PAUSE) return;

            selectedOffset += SELECTED_SCROLL_STEP;

            if (selectedOffset > selectedWidth - itemsWidth)
                timerCounter++;

            if (timerCounter > PAUSE)
            {
                timerCounter = 0;
                selectedOffset = 0;
                timerCounter2 = 0;
            }

            timerRepaint = true;
            repaint ();
        }
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
    public void dispatchRepeatedKey(int keyCommand, int keyCode, int gameAction,
                                    int count, int acceleration)
    {
        if (keyCode >= Canvas.KEY_NUM0 && keyCode <= Canvas.KEY_NUM9)
        {
            // scroll list until item starting with this key
            scrollListByKey(keyCode);
        }
        else
        {
            if (keyCommand == Frame.KEY_COMMAND_DOWN)
                moveToNextItem ( +1, count == 1);
            else if (keyCommand == Frame.KEY_COMMAND_UP)
                moveToNextItem ( -1, count == 1);
            else if (keyCommand == Frame.KEY_COMMAND_RIGHT)
                moveToNextItem ( +itemsPerPage, false);
            else if (keyCommand == Frame.KEY_COMMAND_LEFT)
                moveToNextItem ( -itemsPerPage, false);
        }
    }
    /**
     * Dispatch single key event
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     */
    public void dispatchSingleKey(int keyCommand, int keyCode, int gameAction)
    {
        boolean dispatched = false;

        if (!(keyCode >= Canvas.KEY_NUM0 && keyCode <= Canvas.KEY_NUM9))  // ignore numerical codes
        {
            dispatched = true;

            if (keyCommand == Frame.KEY_COMMAND_ENTER)
            {
                // item is chosen, apply master command
                super.commandAction (masterCommand);
            }
            else if (keyCommand == Frame.KEY_COMMAND_LEFT && backCommand != null)
                super.commandAction (backCommand);
            else if (keyCommand == Frame.KEY_COMMAND_RIGHT)
                super.commandAction (masterCommand);
            else
                dispatched = false;
        }

        if (!dispatched)
            dispatchRepeatedKey(keyCommand, keyCode, gameAction, 1, 1);
    }

    /**
     * Move to next list item...
     * @param step int
     * @param walkAround boolean
     */
    private void moveToNextItem (int step, boolean walkAround)
    {
        int count = listString.size();
        if (count > 0)
        {
            int old = selected;
            selected += step;

            if (walkAround)
                selected = (selected + count) % count;
            else
            {
                if (selected >= count) selected = count-1;
                else if (selected < 0) selected = 0;
            }

            selectionChanged(old);
            scrollToSelected();
        }
    }

    /**
     * Scroll list to selected item
     */
    private void scrollToSelected ()
    {
        int h = selected * itemHeight;
        if (h + itemHeight >= visibleBottom)
        {
            visibleTop = h + itemHeight - realHeight;
            if (visibleTop < 0)
                visibleTop = 0;
        }
        else if (h < visibleTop)
            visibleTop = h;

        repaint ();
    }

    /**
     * Selection is changed.
     * @param ol int previously selected item.
     */
    private void selectionChanged (int ol)
    {
        if (selectionListener != null && (ol != selected || selected < 0))
            selectionListener.selectionChanged(selected);
    }

    /**
     * When alpha-numeric key is pressed, list is scrolled upto the first item
     * which name starts with letter associated with pressed key.
     * Correspondence between key and letters is taken from SearchEngine and
     * works like T9 algorithm.
     * @param keyCode int
     */
    private void scrollListByKey (int keyCode)
    {
        int old = selected;
        keyCode -= Canvas.KEY_NUM0;
        int count = listString.size();
        for (int i=1; i < count; i++)
        {
            int index = (selected + i) % count;
            String s = (String)listString.elementAt(index);

            if (keyCode == SearchEngine.characterToKey(s.charAt(0)))
            {
                selected = index;
                scrollToSelected();
                selectionChanged(old);
                break;
            }
        }
    }

    /**
     * Pointer is down
     * @param x int
     * @param y int
     */
    public void pointerPressed(int x, int y)
    {
        if (y > titleHeight)
        {
            if (x < scrollbarX)
            {
                int old = selected;
                int index = (y - titleHeight + visibleTop) / itemHeight;
                if (index >= 0 && index < listString.size())
                {
                    if (index == selected)
                        super.commandAction (masterCommand);
                    else
                    {
                        selected = index;
                        repaint();
                        selectionChanged(old);
                    }
                }
            }
            else
            {
                boolean isHigher = (y - titleHeight - thumbY - thumbH / 2) > 0;
                int step = realHeight;
                if (isHigher)
                {
                    int heightLimit = listString.size () * itemHeight - realHeight;
                    visibleTop += step;
                    if (visibleTop > heightLimit)
                        visibleTop = heightLimit;
                }
                else
                {
                    visibleTop -= step;
                    if (visibleTop < 0)
                        visibleTop = 0;
                }
                repaint();
            }
        }
    }

    public void showNotify()
    {
        // reset selection only if list became invalid
        if (invalid)
        {
            resetSelection ();
            visibleTop = 0;
            selectionChanged ( -1);
        }
    }

    /**
     * Append item to the list
     * @param stringPart String
     * @return int
     */
    public int append (String stringPart)
    {
        return append(stringPart, null);
    }

    /**
     * Append item to the list
     * @param stringPart String
     * @param imagePart Image
     * @return int
     */
    public int append (String stringPart, Image imagePart)
    {
        return append (stringPart, imagePart, -1, -1);
    }

    /**
     * Append item to the list with specified text color and background color
     * @param stringPart String
     * @param imagePart Image
     * @param color int
     * @param background int
     * @return int
     */
    public int append (String stringPart, Image imagePart, int color, int background)
    {
        invalid = true;
        listString.addElement(stringPart);
        listImage.addElement(imagePart);
        listColor.addElement(new Integer(color));
        listBackground.addElement(new Integer(background));
        return listString.size();
    }

    /**
     * Delete #itemIndex item
     * @param itemIndex int
     */
    public void delete(int itemIndex)
    {
        invalid = true;
        listString.removeElementAt(itemIndex);
        listImage.removeElementAt(itemIndex);
        resetSelection();
    }

    /**
     * Delete all items
     */
    public void deleteAll()
    {
        invalid = true;
        listString.removeAllElements();
        listImage.removeAllElements();
        listColor.removeAllElements();
        listBackground.removeAllElements();
        resetSelection();
    }

    /**
     * Set value for specified item
     * @param itemIndex int
     * @param stringPart String
     * @param imagePart Image
     */
    public void set(int itemIndex, String stringPart, Image imagePart)
    {
        invalid = true;
        listString.setElementAt(stringPart, itemIndex);
        listImage.setElementAt(imagePart, itemIndex);
        repaint();
        selectionChanged(-1);
    }

    /**
     * Set item look
     * @param itemIndex int
     * @param color int
     * @param background int
     */
    public void setItemLook(int itemIndex, int color, int background)
    {
        invalid = true;
        listColor.setElementAt(new Integer(color), itemIndex);
        listImage.setElementAt(new Integer(background), itemIndex);
        repaint();
    }

    /**
     * Get #i item
     * @param i int
     * @return String
     */
    public String getString(int i)
    {
        return (String)listString.elementAt(i);
    }

    /**
     * Test if specified element is selected
     * @param itemIndex int
     * @return boolean
     */
    public boolean isSelected(int itemIndex)
    {
        return itemIndex == selected;
    }

    /**
     * Get index of selected item
     * @return int
     */
    public int getSelectedIndex()
    {
        return selected;
    }

    /**
     * Select list item.
     * @param itemIndex int
     */
    public void setSelectedIndex(int itemIndex)
    {
        if (itemIndex < 0 || itemIndex >= listString.size())
            throw new IndexOutOfBoundsException();

        selected = itemIndex;
    }

    /**
     * Reset selection
     */
    private void resetSelection()
    {
        selected = (listString.size() > 0)? 0: -1;
    }

    /**
     * Set list title
     * @param title String
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * Get height of list contents
     * @return int
     */
    public int getContentHeight()
    {
        return itemHeight * listString.size() + titleHeight;
    }

    /**
     * Sort elements by Shell algorithm
     * @param ids int[] indexes
     * @param start int first element to sort
     * @param n int number of elements to sort
     */
    public void sort (int[] ids, int start, int n)
    {
        if (n == 0) return;

        int comp = 0;

        boolean c;
        int g;
        int i;
        int j;

        n = n - 1;
        g = (n + 1) / 2;

        do
        {
            i = g;
            do
            {
                j = i - g;
                c = true;
                do
                {
                    int js = j + start;
                    int jgs = j + g + start;

                    comp++;
                    if (SearchEngine.compareStrings((String) listString.elementAt (js),
                        (String) listString.elementAt (jgs)) < 0)
                    {
                        c = false;
                    }
                    else
                    {
                        Object tmp = listString.elementAt (js);
                        listString.setElementAt (listString.elementAt (jgs), js);
                        listString.setElementAt (tmp, jgs);

                        Object tmn = listImage.elementAt (js);
                        listImage.setElementAt (listImage.elementAt (jgs), js);
                        listImage.setElementAt (tmn, jgs);

                        Object tmc = listColor.elementAt (js);
                        listColor.setElementAt (listColor.elementAt (jgs), js);
                        listColor.setElementAt (tmc, jgs);

                        Object tmb = listBackground.elementAt (js);
                        listBackground.setElementAt (listBackground.elementAt (jgs), js);
                        listBackground.setElementAt (tmb, jgs);

                        int tmi = ids[js];
                        ids[js] = ids[jgs];
                        ids[jgs] = tmi;
                    }
                    j = j - 1;
                }
                while (j >= 0 && c);
                i = i + 1;
            }
            while (i <= n);
            g = g / 2;
        }
        while (g > 0);
    }
}
