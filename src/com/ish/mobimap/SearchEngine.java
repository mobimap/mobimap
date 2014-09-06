/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                            (c) 2003-2009 ISh                              */
/*****************************************************************************/

package com.ish.mobimap;

import javax.microedition.lcdui.*;
import com.ish.mobimap.ui.*;

/**
 * Search dialog
 */
public class SearchEngine
    extends Component
    implements CommandListener
{
    private static final int BUFFER_SIZE_LIMIT = 30;
    private static final int RESULTS_LIMIT = 30;

    private msrt parent;

    private Command serverSearchCommand;
    private Command showCommand;
    private Command enterRequestCommand;

    private Form requestDialog;
    private TextField requestDialogField;

    private int buf [];            // sequence of pressed keys
    private char sym [];           // buffer of symbols
    private int bufN, bufRealN;    // current buffer position and quantity of corresponding characters
    private int res [];            // list of found objects
    private int resN;              // number of found objects
    private boolean resOverflow;   // true, if number of appropriate variants is more then one
    private String oneRes;         // the most appropriate variant of answer
    private static byte table [];  // table of corespondence between characters and keys
    private boolean isT9Mode;      // true, if algorithm is T9
    private boolean isFullText;    // true, if algorithm is fulltext
    private int numberPrev = -1;   // the last pressed key
    private int tablePosition;     // character position in table
    private char bufsym [];        // buffer for input symbols
    private char curLayout [][];   // current layout
    private long time;             // time when the key was pressed
    private boolean moveNext;      // true, if there were right keystroke

    private char keyLetter [];
    private int keyLetterN;
    private final int keysLetterMax = 100;
    private int keysPerLine, keysTop, keysLeft, keysRight, keysWidth, keysHeight;

    private int mode;            // search mode
    private final static byte MODE_N = 3;
    public final static byte MODE_SEQUENTIAL = 0;
    public final static byte MODE_T9 = 1;
    public final static byte MODE_FULLTEXT = 2;

    private int keyboardLayout;
    public final static byte LAYOUT_CYRILLIC = 0;
    public final static byte LAYOUT_ALTERNATIVE = 1;
    public final static byte LAYOUT_LATIN = 2;
    public final static byte LAYOUT_QWERTY = 3;

    // objects iteration parameters
    private int curSt;
    private int curLb [];
    private char curBuffer [];
    private int curBufLen;

    private static final int TIME_OF_CHOICE = 3000;
    private boolean timerRepaint;
    private boolean forceRepaint;

    private static final int CURSOR_HEIGHT = 2;
    private int cursorX;
    private int cursorW;
    private int cursorState;

    private static final int MAX_CHAR_CODE = 0x500;
    private static char normalForm [];

    private static char layoutEN [] [] = {{'0', '-', ' '}, {' ', '1', '-'},
                {'a', 'b', 'c', '2'},
                {'d', 'e', 'f', '3'},
                {'g', 'h', 'i', '4'},
                {'j', 'k', 'l', '5'},
                {'m', 'n', 'o', '6'},
                {'p', 'q', 'r', 's', '7'},
                {'t', 'u', 'v', '8'},
                {'w', 'x', 'y', 'z', '9'}};

    private static char layoutRU [] [] = {{'0', '-', '`'}, {' ', '1', '`'},
                {'\u0430', '\u0431', '\u0432', '\u0433', '2'},
                {'\u0434', '\u0435', '\u0451', '\u0436', '\u0437', '3'},
                {'\u0438', '\u0439', '\u043A', '\u043B', '4'},
                {'\u043C', '\u043D', '\u043E', '\u043F', '5'},
                {'\u0440', '\u0441', '\u0442', '\u0443', '6'},
                {'\u0444', '\u0445', '\u0446', '\u0447', '7'},
                {'\u0448', '\u0449', '\u044A', '\u044B', '8'},
                {'\u044C', '\u044D', '\u044E', '\u044F', '9'}};

    private static char layoutRU_Siemens [] [] = {{'0', '-'}, {' ', '1'},
                {'\u0430', '\u0431', '\u0432', '\u0433', '2'},
                {'\u0434', '\u0435', '\u0451', '\u0436', '\u0437', '3'},
                {'\u0438', '\u0439', '\u043A', '\u043B', '4'},
                {'\u043C', '\u043D', '\u043E', '5'},
                {'\u043F', '\u0440', '\u0441', '6'},
                {'\u0442', '\u0443', '\u0444', '\u0445', '7'},
                {'\u0446', '\u0447', '\u0448', '\u0449', '\u044A', '8'},
                {'\u044B', '\u044C', '\u044D', '\u044E', '\u044F', '9'}};

    private static char normalizationTable [] = {
        '\u00E0', 'a', '\u00E1', 'a', '\u00E2', 'a', '\u00E3', 'a', '\u00E4', 'a', '\u00E5', 'a',
        '\u00E6', 'a', '\u0101', 'a', '\u010D', 'c',
        '\u00E8', 'e', '\u00E9', 'e', '\u00EA', 'e', '\u00EB', 'e', '\u0113', 'e',
        '\u00EC', 'i', '\u00ED', 'i', '\u00EE', 'i', '\u00EF', 'i', '\u0123', 'g', '\u012B', 'i',
        '\u0137', 'k', '\u013C', 'l', '\u0142', 'l',
        '\u00F1', 'n', '\u0146', 'n', '\u0146', 'n',
        '\u00F2', 'o', '\u00F3', 'o', '\u00F4', 'o',
        '\u00F5', 'o', '\u00F6', 'o', '\u00F8', 'o',
        '\u014D', 'o', '\u014F', 'o', '\u0151', 'o',
        '\u0155', 'r', '\u0157', 'r', '\u0159', 'r', '\u015F', 's', '\u0161', 's',
        '\u0163', 't',
        '\u00F9', 'u', '\u00FA', 'u', '\u00FB', 'u', '\u00FC', 'u', '\u016B', 'u',
        '\u017E', 'z',
        '\u0456', '\u0438', '\u0406', '\u0438',
        '\u0457', '\u0439', '\u0407', '\u0439',
        '\u0454', '\u0435', '\u0404', '\u0435',
        '\u045E', '\u0443', '\u040E', '\u0443',
        '\u0490', '\u0433', '\u0491', '\u0433', '\u0492', '\u0433', '\u0493', '\u0433',
        '\u0496', '\u0436', '\u0497', '\u0436',
        '\u049A', '\u043A', '\u049B', '\u043A', '\u049C', '\u043A', '\u049D', '\u043A',
        '\u04A2', '\u043D', '\u04A3', '\u043D',
        '\u04AE', '\u0443', '\u04AF', '\u0443', '\u04B0', '\u0443', '\u04B1', '\u0443'
    };

    /**
     * Search engine and T9 support
     * @param parent msrt
     * @param frame Frame
     */
    SearchEngine (msrt parent, Frame frame)
    {
        super(frame);
        this.parent = parent;

        requestDialog = new Form (msrt.Resources [0]);
        requestDialog.setCommandListener(this);
        requestDialog.addCommand(parent.cancelCommand);
        requestDialog.addCommand(parent.okCommand);
        requestDialogField = new TextField (msrt.Resources [21], "", 20, TextField.ANY | TextField.NON_PREDICTIVE);
        requestDialog.append(requestDialogField);

        addCommand(parent.backCommand);

        showCommand = new Command(msrt.Resources[69], Command.SCREEN, 1);
        serverSearchCommand = new Command(msrt.Resources[1], Command.SCREEN, 2);
        if (msrt.features[msrt.FEATURE_SERVER_SEARCH])
            addCommand(serverSearchCommand);
        enterRequestCommand = new Command(msrt.Resources[2], Command.SCREEN, 1);
        addCommand(enterRequestCommand);

        setKeyboardLayout (0);
        res = new int [RESULTS_LIMIT];

        keyLetter = new char [keysLetterMax];

        normalForm = new char [MAX_CHAR_CODE];
        for (char i = 0x20; i < MAX_CHAR_CODE; i++)
        {
            char c = i;
            if (c < 0x180 || c > 0x400)
            {
                c = toLowerCase(c);

                for (int j=0; j < normalizationTable.length; j+=2)
                {
                    if (c == normalizationTable [j])
                    {
                        c = normalizationTable [j+1];
                        break;
                    }
                }
            }
            normalForm [i] = c;
        }
    }
    /**
     * Init and show dialog
     */
    public void show ()
    {
        sym = new char [BUFFER_SIZE_LIMIT];
        bufsym = new char [BUFFER_SIZE_LIMIT];
        buf = new int [BUFFER_SIZE_LIMIT];
        reset ();
        parent.changeDisplay (msrt.DISPLAY_SEARCH);
    }
    /**
     * Resets dialog
     */
    private void reset ()
    {
        bufN = bufRealN = resN = 0;
        oneRes = null;
        numberPrev = -1;
        setMasterCommand(null);
    }

    /**
     * Handles keys
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     */
    public void dispatchSingleKey(int keyCommand, int keyCode, int gameAction)
    {
        dispatchRepeatedKey(keyCommand, keyCode, gameAction, 1, 1);
    }

    public void dispatchRepeatedKey(int keyCommand, int keyCode, int gameAction,
                                int count, int acceleration)
    {
        int number = keyCode - 48;
        long timeNow = System.currentTimeMillis();

        if ((number >= 0 && number <= 9) && keyboardLayout != LAYOUT_QWERTY)
        {
            if (isT9Mode)
            {
                if (bufN < BUFFER_SIZE_LIMIT)
                {
                    buf [bufN++] = number;
                }
            }
            else
            {
                if (number != numberPrev || timeNow > time + TIME_OF_CHOICE || moveNext)
                {
                    if (bufN < BUFFER_SIZE_LIMIT)
                    {
                        tablePosition = 0;
                        buf [bufN] = number;
                        bufsym [bufN] = curLayout[number][tablePosition];
                        bufN++;

                        numberPrev = number;
                    }
                }
                else
                {
                    tablePosition ++;
                    if (tablePosition >= curLayout [number].length) tablePosition = 0;

                    bufsym [bufN-1] = curLayout[number][tablePosition];
                }
                moveNext = false;
                time = timeNow;
            }
            engine ();
            repaint ();
        }
//        else if (((keyCode == Canvas.KEY_STAR || keyCommand == Frame.KEY_COMMAND_LEFT) && keyboardLayout != LAYOUT_QWERTY) || keyCode == 8 /*backspace*/)
        else if (keyCommand == Frame.KEY_COMMAND_LEFT || (keyCode == Canvas.KEY_STAR && keyboardLayout != LAYOUT_QWERTY) || keyCode == 8 /*backspace*/)
        {
            deleteLastChar();
            moveNext = true;
        }
        else if ((keyCode == Canvas.KEY_POUND && keyboardLayout != LAYOUT_QWERTY) || keyCode == 10 /*enter*/)
            done();
        else if (keyCommand == Frame.KEY_COMMAND_RIGHT && keyboardLayout != LAYOUT_QWERTY)
        {
            moveNext = true;
            repaint();
        }
        else if (keyCommand == Frame.KEY_COMMAND_UP && keyboardLayout != LAYOUT_QWERTY)
        {
            setMode ( (mode + 1) % MODE_N);
            reset ();
            repaint ();
        }
        else if (keyCommand == Frame.KEY_COMMAND_DOWN && keyboardLayout != LAYOUT_QWERTY)
        {
            setMode ((mode + 2) % MODE_N);
            reset ();
            repaint ();
        }
        else if (gameAction == Canvas.FIRE)
        {
            showRequestDialog();
        }
        else if (keyboardLayout == LAYOUT_QWERTY)
        {
            if (bufN < BUFFER_SIZE_LIMIT && keyCode >= ' ')
            {
                char ch = toLowerCase ((char) keyCode);
                bufsym[bufN++] = ch;
                engine ();
                repaint ();
            }
        }

        timerRepaint = false;
    }
    /**
     * Delete last character
     */
    private void deleteLastChar ()
    {
        if (bufN == 0)
        {
            parent.changeDisplayBack();
        }
        else
        {
            bufN--;

            if (bufN == 0)
            {
                resN = 0;
                setMasterCommand(null);
            }
            else
                engine ();

            repaint ();
        }
    }
    /**
     * Shows request dialog
     */
    protected void showRequestDialog ()
    {
        requestDialogField.setString ((mode == MODE_T9)? new String (sym, 0, bufRealN): new String (bufsym, 0, bufN));
        parent.changeDisplay (requestDialog);
        if (isT9Mode) setMode (MODE_SEQUENTIAL);
    }
    /**
     * Sets buffer contents after calling Request Dialog
     * @param req - contents
     */
    protected void setRequest (String req)
    {
        bufN = req.length();
        if (bufN == 0)
            reset ();
        else
        {
            req = req.toLowerCase();
            if (bufN > BUFFER_SIZE_LIMIT) bufN = BUFFER_SIZE_LIMIT;

            System.arraycopy (req.toCharArray (), 0, bufsym, 0, bufN);
            engine ();
        }

        repaint ();
    }
    /**
     * Handle pointer events
     * @param x -
     * @param y -
     */
    public void pointerReleased (int x, int y)
    {
        if (y > keysTop && x > keysLeft && x < keysRight + keysWidth)
        {
            int row = (y - keysTop) / keysHeight;
            int col = (x - keysLeft) / keysWidth;
            int off = row * keysPerLine + col;
            if (off > keysLetterMax) return;

            char ch = keyLetter [off];

            if (mode == MODE_T9)
            {
                setMode (MODE_SEQUENTIAL);
                reset ();
            }

            if (ch > 0)
            {
                if (ch == '<')
                    deleteLastChar ();
                else
                if (bufN < BUFFER_SIZE_LIMIT)
                {
                    bufsym[bufN++] = ch;
                    engine ();
                    repaint ();
                }
            }
        }
        timerRepaint = false;
    }

    /**
     * Paint dialog contents
     * @param canvasGraphics graphics
     */
    public void paint (Graphics canvasGraphics)
    {
        Font fontPlain = getFont ();
        Font fontBold = Font.getFont (fontPlain.getFace (), Font.STYLE_BOLD, fontPlain.getSize ());
        int h = fontPlain.getHeight () + 3;

        // repaint cursor
        if (timerRepaint && !forceRepaint)
        {
            drawCursor (canvasGraphics, fontBold, h);
            timerRepaint = false;
            return;
        }

        Graphics g = getScreenBufferGraphics();

        int width = componentWidth;
        int height = componentHeight;

        for (int i = bufRealN; i < bufN; i++)
            sym[i] = '_';

        g.setColor (Design.COLOR_SEARCH_ENGINE_BACKGROUND_DARK);
        g.fillRect (0, 0, width, height);

        g.setColor (Design.COLOR_SEARCH_ENGINE_BACKGROUND_LIGHT);
        g.fillRect(0, 0, width, h * 2);

        g.setColor (Design.COLOR_SEARCH_ENGINE_BORDER);
        g.drawLine(0, h*2, width, h*2);
        g.drawLine(0, h*4, width, h*4);

        g.setColor (Design.COLOR_SEARCH_ENGINE_TEXT);

        g.setFont (fontPlain);
        g.drawString (msrt.Resources[21], Design.MARGIN_LEFT, 0,
                      Graphics.TOP | Graphics.LEFT);
        g.drawString (msrt.Resources[22], Design.MARGIN_LEFT, h * 2,
                      Graphics.TOP | Graphics.LEFT);

        g.drawString (msrt.Resources[38] + ":", Design.MARGIN_LEFT, h * 4,
                      Graphics.TOP | Graphics.LEFT);
        g.drawString (msrt.Resources[isFullText ? 41 : isT9Mode ? 40 : 39],
                      Design.MARGIN_LEFT, h * 5, Graphics.TOP | Graphics.LEFT);

        g.setFont (fontBold);

        if (oneRes != null && mode != MODE_FULLTEXT)
        {
            g.setColor (Design.COLOR_SEARCH_ENGINE_TEXT_INACTIVE);
            g.drawString (oneRes, Design.MARGIN_LEFT, h, Graphics.TOP | Graphics.LEFT);
            g.setColor (Design.COLOR_SEARCH_ENGINE_TEXT_BLUE);
            g.drawSubstring (oneRes, 0, bufN, Design.MARGIN_LEFT, h,
                             Graphics.TOP | Graphics.LEFT);
            cursorX = fontBold.substringWidth (oneRes, 0, bufN);
            if (bufN > 0)
                cursorW = fontBold.substringWidth (oneRes, bufN - 1, 1);
        }
        else
        {
            g.setColor (Design.COLOR_SEARCH_ENGINE_TEXT_BLUE);
            char[] line = (isT9Mode) ? sym : bufsym;
            g.drawChars (line, 0, bufN, Design.MARGIN_LEFT, h,
                         Graphics.TOP | Graphics.LEFT);
            cursorX = fontBold.charsWidth (line, 0, bufN);
            cursorW = (bufN > 0) ? fontBold.charsWidth (line, bufN - 1, 1) : 0;
        }

        g.setColor ( (resN == 0) ? Design.COLOR_SEARCH_ENGINE_TEXT_INACTIVE :
                     (resOverflow ? Design.COLOR_SEARCH_ENGINE_TEXT_RED :
                      Design.COLOR_SEARCH_ENGINE_TEXT_GREEN));
        String fs = Integer.toString (resN);
        g.drawString (fs, Design.MARGIN_LEFT, h * 3, Graphics.TOP | Graphics.LEFT);

        if (hasPointerEvents ())
        {
            g.setColor (Design.COLOR_SEARCH_ENGINE_BORDER);
            g.drawLine(0, h*6, width, h*6);

            g.setColor (Design.COLOR_SEARCH_ENGINE_TEXT);

            g.setFont (fontPlain);
            g.drawString (msrt.Resources[71], Design.MARGIN_LEFT, h * 6,
                          Graphics.TOP | Graphics.LEFT);

            drawKeyboard (g, fontPlain, h * 7 + 2);
        }
        cursorState = 0;
        drawCursor (g, fontBold, h);

        paintScreenBuffer(canvasGraphics);

        forceRepaint = false;
    }

    public void showNotify()
    {
        forceRepaint = true;
    }

    /**
     * Draw cursor
     * @param g Graphics
     * @param fontBold Font
     * @param h int
     */
    private void drawCursor (Graphics g, Font fontBold, int h)
    {
        // draw cursor
        long now = System.currentTimeMillis ();
        int x = cursorX;
        if (now - time < TIME_OF_CHOICE && mode != MODE_T9 && !moveNext)
            x -= cursorW;
        else
            cursorW = fontBold.charWidth ('n'); // medium width for blank cursor

        int y = h*2 - 4;
        g.setColor (Design.COLOR_COMPONENT_BACKGROUND);
        g.fillRect (0, y, componentWidth, CURSOR_HEIGHT);

        g.setColor (((cursorState & 0x1) == 0)?
                    Design.COLOR_SEARCH_ENGINE_CURSOR :
                    Design.COLOR_SEARCH_ENGINE_BACKGROUND_LIGHT);
        g.fillRect (x + Design.MARGIN_LEFT, y, cursorW, CURSOR_HEIGHT);

        cursorState++;
    }

    /**
     * Draw virtual keyboard for input chars using stylus
     * @param g Graphics
     * @param font Font
     * @param top int Top margin
     */
    private void drawKeyboard (Graphics g, Font font, int top)
    {
        keysTop = top;
        keysHeight = font.getHeight() + 1;
        int width = componentWidth;
        keysWidth = 12;
        g.setFont (font);
        keysPerLine = (width - 8) / keysWidth;
        keysLeft = 4;
        keysRight = ((width - 8) / keysWidth) * keysWidth;

        keyLetterN = 0;
        drawKeysBlock (g, '<', '<', true);
        drawKeysBlock (g, ' ', ' ', false);
        drawKeysBlock (g, '0', '9', false);
        if (curLayout == layoutRU || curLayout == layoutRU_Siemens || keyboardLayout == LAYOUT_QWERTY)
            drawKeysBlock (g, 0x430, 0x44F, false);
        if (curLayout == layoutEN)
            drawKeysBlock (g, 'a', 'z', false);
    }
    /**
     * Draw one block of keys
     * @param g - graphics
     * @param from - the first char in block
     * @param to - the last char in block
     * @param isSystem - if system, draw it in reversed colors
     */
    private void drawKeysBlock (Graphics g, int from, int to, boolean isSystem)
    {
        int colorBg = isSystem? 0x00007f: 0xffffff;
        int colorFg = isSystem? 0xffffff: 0x000000;

        for (int i = from; i <= to; i++, keyLetterN++)
        {
            int x = (keyLetterN % keysPerLine) * keysWidth + keysLeft;
            int y = (keyLetterN / keysPerLine) * keysHeight + keysTop;

            keyLetter [keyLetterN] = (char)i;
            if (x > keysRight) { x = 4; y += keysHeight; }

            g.setColor (colorBg);
            g.fillRect (x, y, keysWidth, keysHeight);
            g.setColor (0xcfcfcf);
            g.drawRect (x, y, keysWidth, keysHeight);
            g.setColor (colorFg);
            g.drawChar ((char)i, x + keysWidth/2, y + 1, Graphics.HCENTER | Graphics.TOP);
        }
    }

    private int timerFilter = 0;
    public void timer()
    {
        if ((timerFilter++ & 0x1) == 0)
        {
            timerRepaint = true;
            repaint ();
        }
    }


    //**** ENGINE *****//

    private int getNext ()
    {
        int object = 0;

        // street
        boolean found = false;
        for (;curSt < Map.streetN && !found; curSt++)
        {
            int namepA = Map.stnamep[curSt], namepE = Map.stnamep[curSt + 1];
            int len = namepE - namepA;

            if (len < bufN) continue;

            System.arraycopy (Map.stnames, namepA, curBuffer, 0, curBufLen = (len < BUFFER_SIZE_LIMIT)? len: BUFFER_SIZE_LIMIT);
            found = true;
            object = curSt | Map.MASK_STREET;
        }
        if (!found)
        {
            found = false;
            for (int seg = 0; seg < Map.SEGMENT_LIMIT && !found; seg++)
                for (; curLb[seg] < Map.lbN[seg] && !found; curLb[seg]++)
                {
                    int namepA = Map.lbnamep[seg][curLb[seg]],
                        namepE = Map.lbnamep[seg][curLb[seg] + 1];
                    int len = namepE - namepA;

                    if (len < bufN) continue;

                    System.arraycopy (Map.name[seg], namepA, curBuffer, 0,
                                      curBufLen = (len < BUFFER_SIZE_LIMIT) ? len : BUFFER_SIZE_LIMIT);
                    found = true;
                    object = curLb[seg] | (seg << 16) | Map.MASK_LABEL;
                }
        }

        return object;
    }
    /**
     * The Engine
     */
    private void engine ()
    {
        resN = 0; resOverflow = false; oneRes = null;

        char seq [] = new char [bufN];
        char seqprev [] = new char [bufN];
        int weight = 0, weightMax = -1;
        boolean newSequence = true;

        curBuffer = new char [BUFFER_SIZE_LIMIT];
        curSt = 0; curLb = new int [Map.SEGMENT_LIMIT];

        int object = 0;
        while ((object = getNext ()) > 0 && (!resOverflow || isT9Mode))
        {
            boolean isSequence = true;   // true, if first chars of current street name match first chars of previous street name

            int last = isFullText? curBufLen-bufN: 0;

            for (int h=0; h <= last; h++)
            {
                boolean isSatisfied = true;  // true, if name satisfies pressed keys

                for (int j = 0, k = h; j < bufN; j++, k++)
                {
                    char c = curBuffer[k];
                    if (c < MAX_CHAR_CODE) c = normalForm [curBuffer[k]];

                    isSequence = isSequence && (c == seqprev[j]);

                    if (isT9Mode)
                        isSatisfied = isSatisfied && (table[c] == buf[j]);
                    else
                        isSatisfied = isSatisfied && (c == bufsym[j]);

                    if (!isSatisfied)
                        break; // optimization

                    seq[j] = c;
                }
                // if object matches pattern then isSatisfied=true
                if (isSatisfied)
                    if (resN < RESULTS_LIMIT)
                    {
                        res[resN++] = object;
                    }
                    else
                        resOverflow = true;

                if (isSequence)
                {
                    weight++;
                }
                else
                {
                    weight = 0;
                    if (isSatisfied)
                    {
                        System.arraycopy (seq, 0, seqprev, 0, bufN); // change of sequence
                        newSequence = true;
                    }
                }

                if (isSatisfied && weight > weightMax)
                {
                    System.arraycopy (seq, 0, sym, 0, bufN);
                    weightMax = weight; bufRealN = bufN;

                    if (newSequence)
                    {
                        // 'one' contains the most probable street, the first in the sequence
                        char one[] = new char [curBufLen];
                        for (int i = 0; i < curBufLen; i++)
                            one[i] = toLowerCase (curBuffer [i]);
                        oneRes = new String (one);
                    }
                    newSequence = false;
                }
            }
        }

        // master command is set only if there's some query
        setMasterCommand((resN == 0)? null: showCommand);
    }
    /**
     * Fills results list
     */
    private void done ()
    {
        if (resN > 0)
        {
            Map.srhN = resN;
            for (int i = 0; i < resN; i++)
                Map.srhContents[i] = res[i];

            Map.curCategory = Map.CATEGORY_RESULTS;
//            if (resN == 1)
//            {
//                parent.theObjectManager.select(res[0]);
//                parent.changeDisplay (msrt.DISPLAY_MAP);
//            }
//            else
            parent.theObjectManager.createAndShowObjList (null);
        }
    }
    /**
     * Sets keyboard layout
     * @param layout keyboard layout
     */
    public void setKeyboardLayout (int layout)
    {
        keyboardLayout = layout;
        if (keyboardLayout == LAYOUT_QWERTY && mode == MODE_T9)
        {
            setMode (MODE_SEQUENTIAL);
            reset ();
        }

        curLayout = (layout == LAYOUT_CYRILLIC)? layoutRU:
            (layout == LAYOUT_ALTERNATIVE)? layoutRU_Siemens: layoutEN;
        table = new byte [MAX_CHAR_CODE];
        for (byte i=0; i < 10; i++)
        {
            char a [];
            a = (layout == 1)? layoutRU_Siemens [i]: layoutRU [i];
            for (byte j=0; j < a.length; j++)
                table [a [j]] = i;

            a = layoutEN [i];
            for (byte j=0; j < a.length; j++)
                table [a [j]] = i;
        }
    }
    /**
     * Sets search mode
     * @param newMode search mewMode
     */
    public void setMode (int newMode)
    {
        if (keyboardLayout == LAYOUT_QWERTY && newMode == MODE_T9) newMode = MODE_SEQUENTIAL;
        mode = newMode;
        isT9Mode = newMode == MODE_T9;
        isFullText = newMode == MODE_FULLTEXT;
        msrt.config [msrt.CONFIG_SEARCH_MODE] = mode;
    }
    /**
     * Converts character to lower case
     * @param c - char
     * @return - lowercase char
     */
    private static char toLowerCase (char c)
    {
        if (c >= 0x410 && c < 0x430) c += 0x20;         // cyrilic
        else if (c >= 0x41 && c <= 0x5A) c += 0x20;     // ascii
        else if (c >= 0x100 && c < 0x138) c |= 0x1;     // latin-ext - one
        else if ((c >= 0x139 && c < 0x149) || (c >= 0x179 && c < 0x180)) c = (char)((c + 1) & 0x7ffe); // latin-ext - two and four
        else if (c >= 0x14A && c < 0x178) c |= 0x1;     // latin-ext - three
        else if (c >= 0xC0 && c <= 0xDF) c += 0x20;     // latin-1
        else if (c >= 0x401 && c <= 0x40F) c += 0x50;   // cyrilic-ext1

        return c;
    }

    public static int compareStrings (String s1, String s2)
    {
        int len1 = s1.length();
        int len2 = s2.length();

        int n = Math.min(len1, len2);
        for (int i = 0; i < n; i++)
        {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);

            int diff = (c1 < MAX_CHAR_CODE && c2 < MAX_CHAR_CODE)?
                normalForm[c1] - normalForm[c2]:
                c1 - c2;

            if (diff != 0) return diff;
        }
        return len1 - len2;
    }

    /**
     * Command action handler. For Displayable
     * @param c Command
     * @param s Displayable
     */
    public void commandAction (Command c, Displayable s)
    {
        // s == requestDialog
        if (c == parent.okCommand)
        {
            setRequest (requestDialogField.getString ());
        }
        parent.changeDisplayBack();
    }

    /**
     * Command action handlre. For Components
     * @param c Command
     */
    public void commandAction (Command c)
    {
        if (c == showCommand)
        {
            done();
        }
        else if (c == enterRequestCommand)
        {
            showRequestDialog();
        }
        else
            super.commandAction(c);
    }

    /**
     * Finds what key need to be pressed to get character 'c' (T9 mode)
     * @param c char
     * @return int
     */
    public static int characterToKey (char c)
    {
        return (c < MAX_CHAR_CODE)? table[normalForm[c]]: -1;
    }
}
