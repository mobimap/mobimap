/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                            (c) 2003-2007 ISh                              */
/*****************************************************************************/

package com.ish.mobimap;

import java.io.*;
import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import com.ish.mobimap.ui.*;
import com.ish.mobimap.net.*;

public class Browser extends Component implements OnlineLoaderListener, CommandListener {
    // Browser data transfer attribute constants
    private static final char ATTRIBUTE_COLOR = 'c';
    private static final char ATTRIBUTE_BGCOLOR = 'g';
    private static final char ATTRIBUTE_FONT_SIZE = 'z';
    private static final char ATTRIBUTE_FONT_STYLE = 'f';
    private static final char ATTRIBUTE_LINK = 'L';
    private static final char ATTRIBUTE_TYPE = 't';
    private static final char ATTRIBUTE_SPACE = 's';
    private static final char ATTRIBUTE_SPACE_BEFORE = 'e';
    private static final char ATTRIBUTE_PADDING_TOP = 't';
    private static final char ATTRIBUTE_PADDING_BOTTOM = 'b';
    private static final char ATTRIBUTE_ALIGN = 'a';
    private static final char ATTRIBUTE_TEST = 'T';
    private static final char ATTRIBUTE_DATA = 'D';
    private static final char ATTRIBUTE_INPUT_SIZE = 'n';
    private static final char ATTRIBUTE_MAX_LENGTH = 'm';
    private static final char ATTRIBUTE_MODE = 'o';
    private static final char ATTRIBUTE_LABEL = 'k';
    private static final char ATTRIBUTE_DEFAULT = 'u';
    private static final char ATTRIBUTE_PRIORITY = 'p';
    private static final char ATTRIBUTE_CONTROL = 'r';
    private static final char ATTRIBUTE_OPTIONS = 'q';
    private static final char ATTRIBUTE_SELECTED = 'v';

    // Sentence types
    private static final int SENTENCE_TYPE_TEXT = 1;
    private static final int SENTENCE_TYPE_IMAGE = 2;
    private static final int SENTENCE_TYPE_INPUT = 4;
    private static final int SENTENCE_TYPE_CHOICE = 5;
    private static final int SENTENCE_TYPE_DATE = 6;

    // Paragraph parameter indexes
    private static final int PARAGRAPH_PARAM_COUNT = 8;
    private static final int PARAGRAPH_PARAM_COLOR = 0;
    private static final int PARAGRAPH_PARAM_BGCOLOR = 1;
    private static final int PARAGRAPH_PARAM_FONT_SIZE = 2;
    private static final int PARAGRAPH_PARAM_FONT_STYLE = 3;
    private static final int PARAGRAPH_PARAM_SPACE = 4;
    private static final int PARAGRAPH_PARAM_PADDING_TOP = 5;
    private static final int PARAGRAPH_PARAM_PADDING_BOTTOM = 6;
    private static final int PARAGRAPH_PARAM_ALIGN = 7;

    // Sentence parameter indexes
    private static final int SENTENCE_PARAM_COUNT = 5;
    private static final int SENTENCE_PARAM_COLOR = 0;
    private static final int SENTENCE_PARAM_BGCOLOR = 1;
    private static final int SENTENCE_PARAM_FONT_SIZE = 2;
    private static final int SENTENCE_PARAM_FONT_STYLE = 3;
    private static final int SENTENCE_PARAM_SPACE_BEFORE = 4;

    // Link types
    private static final String LINK_WEB = "web";
    private static final String LINK_MMTP = "mmtp";
    private static final String LINK_DATA = "data";
    private static final String LINK_CALL = "call";
    private static final String LINK_SMS = "sms";
    private static final String LINK_FILE = "file";
    private static final String LINK_COMMAND = "command";
    private static final String LINK_INPUT = "input";

    // Link attributes
    private static final String LINK_ATTRIBUTE_TYPE = "type";
    private static final String LINK_ATTRIBUTE_URL = "url";
    private static final String LINK_ATTRIBUTE_PAGE = "page";
    private static final String LINK_ATTRIBUTE_FILE = "file";
    private static final String LINK_ATTRIBUTE_TEXT = "text";
    private static final String LINK_ATTRIBUTE_ID = "id";
    private static final String LINK_ATTRIBUTE_CATEGORY_ID = "categoryId";
    private static final String LINK_ATTRIBUTE_PHONE = "phone";
    private static final String LINK_ATTRIBUTE_COMMAND = "command";

    // MMTP pages
    private static final String PAGE_INFO = "info";
    private static final String PAGE_MANUAL = "manual";
    private static final String PAGE_SEARCH = "search";

    // Local pages
    private static final String LOCAL_PAGE_HOME = "/home";
    private static final String LOCAL_PAGE_HELP = "/help";
    private static final String LOCAL_PAGE_ABOUT = "/about";
    private static final String LOCAL_PAGE_ERROR = "/error";

    // Commands
    private static final String COMMAND_MAP = "map";
    private static final String COMMAND_MENU = "menu";
    private static final String COMMAND_OBJLIST = "objlist";
    private static final String COMMAND_OBJECTS = "objects"; // alias to objlist
    private static final String COMMAND_RELOAD = "reload";
    private static final String COMMAND_BACK = "back";
    private static final String COMMAND_ON_MARK_ADDED = "onMarkAdded";

    // Command parameters
    private static final String LINK_ATTRIBUTE_COMMAND_MAP_X = "x";
    private static final String LINK_ATTRIBUTE_COMMAND_MAP_Y = "y";
    private static final String LINK_ATTRIBUTE_COMMAND_MAP_LATITUDE = "lat";
    private static final String LINK_ATTRIBUTE_COMMAND_MAP_LONGITUDE = "lon";
    private static final String LINK_ATTRIBUTE_COMMAND_MAP_SCALE = "scale";
    private static final String LINK_ATTRIBUTE_COMMAND_OBJECTS_CATEGORY = "category";

    // Font styles
    private static final int FONT_DEFAULT = 0;
    private static final int FONT_NORMAL = 1;
    private static final int FONT_BOLD = 2;
    private static final int FONT_UNDERLINE = 8;
    private static final int FONT_EMPHASIZED = 32;

    // Text align
    private final static int ALIGN_LEFT = 0;
    private final static int ALIGN_RIGHT = 1;
    private final static int ALIGN_CENTER = 2;

    // Visual appearance parameters
    private static final int MARGIN_LEFT = Design.MARGIN_LEFT;
    private static final int MARGIN_RIGHT = Design.MARGIN_RIGHT;

    // Vector capacity
    private static final int CAPACITY_LINES = 50;
    private static final int CAPACITY_LINKS = 20;

    // Request parameters
    private static final String REQUEST_PARAMETER_LINK = "browser.link";
    private static final String REQUEST_PARAMETER_WIDTH = "browser.width";
    private static final String REQUEST_PARAMETER_HEIGHT = "browser.height";
    private static final String REQUEST_PARAMETER_VARS = "browser.vars";

    // Variables
    private static final String VARIABLE_NAME = "mobimap.name";
    private static final String VARIABLE_COPYRIGHT = "mobimap.copyright";
    private static final String VARIABLE_MAP = "mobimap.map";
    private static final String VARIABLE_BUILD = "mobimap.build";
    private static final String VARIABLE_PACKAGE = "mobimap.package";
    private static final String VARIABLE_LICENSE = "mobimap.license";
    private static final String VARIABLE_MESSAGE = "mobimap.message";
    private static final String VARIABLE_PLATFORM = "mobimap.platform";
    private static final String VARIABLE_VENDOR = "mobimap.vendor";
    private static final String VARIABLE_SYSTEM = "mobimap.system";
    private static final String VARIABLE_STAT = "mobimap.stat";

    // midlet
    private msrt parent;

    private Command backCommand; // back
    private Command mapCommand; // show map
    private Command linkCommand; // link type local or mmtp
    private Command linkCallCommand; // link type call
    private Command linkSMSCommand; // link type sms
    private Command linkDataCommand; // link type data
    private Command linkExecuteCommand; // link type command
    private Command linkInputCommand; // link type for input

    /**
     * Current page address
     */
    public Hashtable currentPageLink;

    /**
     * true, if page is loaded fully
     */
    private boolean isPageLoaded;

    /**
     * Link to page that should be reloaded
     */
    public Hashtable errorPageLink;

    /**
     * true, if currentpage is error page.
     */
    private boolean isErrorPage;

    /**
     * true, if page contains form
     */
    private boolean containsForm;

    /**
     * Screen width and height
     */
    private int screenWidth, screenHeight;
    /**
     * Step of scrolling
     */
    private int scrollPageStep;
    /**
     * Text width (space available for content rendering
     */
    private int contentWidth;

    private Font prevFont;

    /**
     * Current sentence link
     */
    private int curSentLink;

    /**
     * Browser history. Links
     */
    private Hashtable[] historyLinks; // Vector<Hashtable>
    private int historyLinksP; // point of insertion
    private static final int CAPACITY_HISTORY = 50; // capacity of history

    private int heightTotal, heightLimit;
    private int visibleTop;
    private int visibleBottom;

    private short[] lineWidths; // width of line
    private short[] lineHeights; // height of line
    private short[] lineTops; // top position of line

    private int lineN; // quantity of lines
    private int lineLimit; // limit quantity of lines, initially equals to LINES_CAPACITY_2

    private int pageBackgroundColor;
    private int pageColor;
    private int[] paraAttribs;
    private Vector sentAttribs; // Vector<int>

    /**
     * Sentence that corresponds to the beginning of line #i
     */
    private short[] lineSentenceStartSentId;
    private short[] lineSentenceStartSentOff;
    private short[] lineSentenceEndSentId;
    private short[] lineSentenceEndSentOff;

    private int paragraphCount;
    private int sentenceCount;
    private Vector sentText;
    private Vector sentParaId;

    /**
     * List of links
     */
    private Vector sentLink; // Vector<Hashtable>
    /**
     * Top of link
     */
    private short[] sentLinkTop;
    /**
     * Bottom of link
     */
    private short[] sentLinkBottom;
    private int sentLinkLimit;

    private int linkTotal; // total quantity of links

    private short[] linkDimensions; // int[5][] in planar form
    private int linkDimensionsN; // number of records in linkDimensions
    private int linkDimensionsLimit; // allocated number of records

    // global variables (mobimap.*)
    private Hashtable variables;

    // local page variables (loaded from point info)
    private Hashtable pageVariables;

    private long timeWhenBecameVisible;

    private Form form;
    private Hashtable formCommands; // map: command -> link
    private Hashtable formItemToVarsMap; // map: item -> var
    private Hashtable formChoiceGroupToOptionsMap;

    private final static char MAX_AVAILABLE_CHARACTER = '\u04F0';
    private static byte[] charWidths = new byte[MAX_AVAILABLE_CHARACTER];

    private final static int INPUT_MARGIN = 4;

    /**
     * Create Browser module
     * @param parent msrt
     * @param frame Frame
     */
    public Browser (msrt parent, Frame frame) {
        super (frame);
        this.parent = parent;

        backCommand = parent.backCommand;
        mapCommand = parent.mapCommand;
        linkCommand = new Command (msrt.Resources[(msrt.BUILD_VERSION == msrt.BUILD_RADAR) ? 156 : 112], Command.OK, 1);
        linkCallCommand = new Command (msrt.Resources[158], Command.OK, 1);
        linkSMSCommand = new Command (msrt.Resources[159], Command.OK, 1);
        linkDataCommand = new Command (msrt.Resources[(msrt.BUILD_VERSION == msrt.BUILD_RADAR) ? 156 : 160], Command.OK,
                                       1);
        linkExecuteCommand = new Command (msrt.Resources[(msrt.BUILD_VERSION == msrt.BUILD_RADAR) ? 156 : 136],
                                          Command.OK, 1);
        linkInputCommand = new Command (msrt.Resources[190], Command.OK, 1);

        addCommand (parent.menuCommand);
        addCommand (mapCommand);

        screenWidth = componentWidth;
        screenHeight = componentHeight;
        contentWidth = screenWidth - MARGIN_LEFT - MARGIN_RIGHT - Design.SCROLLBAR_WIDTH;

        historyLinks = new Hashtable[CAPACITY_HISTORY];

        lineLimit = CAPACITY_LINES;
        lineWidths = new short[CAPACITY_LINES];
        lineHeights = new short[CAPACITY_LINES];
        lineTops = new short[CAPACITY_LINES];

        lineSentenceStartSentId = new short[CAPACITY_LINES];
        lineSentenceStartSentOff = new short[CAPACITY_LINES];
        lineSentenceEndSentId = new short[CAPACITY_LINES];
        lineSentenceEndSentOff = new short[CAPACITY_LINES];

        sentLinkLimit = CAPACITY_LINKS;
        sentLinkTop = new short[CAPACITY_LINKS];
        sentLinkBottom = new short[CAPACITY_LINKS];

        linkDimensionsLimit = CAPACITY_LINES;
        linkDimensions = new short[CAPACITY_LINES];

        // init environment variables
        variables = new Hashtable ();
        variables.put (VARIABLE_NAME, msrt.PROGRAM_NAME);
        variables.put (VARIABLE_COPYRIGHT, msrt.PROGRAM_COPYRIGHT);
        String s = msrt.profile[msrt.PROFILE_MAP_INFO];
        if (s != null)
            variables.put (VARIABLE_MAP, s);
        variables.put (VARIABLE_BUILD, msrt.PROGRAM_BUILD);
        variables.put (VARIABLE_PACKAGE, msrt.profile[msrt.PROFILE_PACKAGE]);
        if (msrt.regInfo != null)
            variables.put (VARIABLE_LICENSE, msrt.regInfo);
        if (msrt.microeditionPlatform != null)
            variables.put (VARIABLE_PLATFORM, msrt.microeditionPlatform);
        variables.put (VARIABLE_VENDOR, new Integer (msrt.vendor));
        variables.put (VARIABLE_SYSTEM, new Integer (msrt.system));

        for (int i = 0; i < msrt.STATISTICS_N; i++) {
            variables.put (VARIABLE_STAT + i, new Integer (msrt.statistics[i]));
        }

        // local page variables
        pageVariables = new Hashtable ();
    }

    /**
     * Init browser.
     * Called after map state is loaded.
     */
    public void init () {
        prevFont = getFont ();
    }

    /**
     * Reset browser -- start new session
     */
    private void reset () {
        historyLinksP = 0;
        for (int i = 0; i < historyLinks.length; i++)
            historyLinks[i] = null;
        currentPageLink = null;

        removeCommand (backCommand);
    }

    /**
     * Show homepage
     */
    public void homepage () {
        reset ();

        Hashtable link = new Hashtable ();
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            link.put (LINK_ATTRIBUTE_PAGE, "index");
            link.put (LINK_ATTRIBUTE_TYPE, LINK_MMTP);
        } else {
            link.put (LINK_ATTRIBUTE_FILE, LOCAL_PAGE_HOME);
            link.put (LINK_ATTRIBUTE_TYPE, LINK_FILE);
        }
        goToLink (link);
    }

    /**
     * Show help page
     */
    public void helppage () {
        reset ();

        Hashtable link = new Hashtable ();
        link.put (LINK_ATTRIBUTE_FILE, LOCAL_PAGE_HELP);
        link.put (LINK_ATTRIBUTE_TYPE, LINK_FILE);
        goToLink (link);
    }

    /**
     * Show manual
     */
    public void manual () {
        reset ();

        Hashtable link = new Hashtable ();
        link.put (LINK_ATTRIBUTE_PAGE, PAGE_MANUAL);
        link.put (LINK_ATTRIBUTE_TYPE, LINK_MMTP);
        goToLink (link);
    }

    /**
     * Show about
     */
    public void pageAbout () {
        reset ();

        Hashtable link = new Hashtable ();
        link.put (LINK_ATTRIBUTE_FILE, LOCAL_PAGE_ABOUT);
        link.put (LINK_ATTRIBUTE_TYPE, LINK_FILE);
        if (!goToLink (link)) {
            String s = msrt.PROGRAM_NAME + "\n" + msrt.PROGRAM_COPYRIGHT +
                       "\n" + msrt.profile[msrt.PROFILE_MAP_INFO];
            if (msrt.regInfo != null)
                s += "\n" + msrt.regInfo;
            parent.showInfo (msrt.ALERT_INFO, s);
        }
    }

    /**
     * Show error page
     * @param message error message
     */
    private void pageError (String message) {
        Hashtable link = new Hashtable ();
        link.put (LINK_ATTRIBUTE_FILE, LOCAL_PAGE_ERROR);
        link.put (LINK_ATTRIBUTE_TYPE, LINK_FILE);
        variables.put (VARIABLE_MESSAGE, message);
        isErrorPage = true;
        goToLink (link);
        isErrorPage = true;
    }

    /**
     * Show info page
     * @param categoryRemoteId Object
     * @param uid int
     */
    public void pageInfo (Object categoryRemoteId, int uid) {
        reset ();

        if (categoryRemoteId == null) {
            // get data from local source
            Hashtable data = parent.theObjectManager.getPointInfo (uid);
            if (data != null) {
                String page = (String) data.get (new Character ('P'));
                if (page != null) {
                    pageVariables = data;

                    Hashtable link = new Hashtable ();
                    link.put (LINK_ATTRIBUTE_FILE, page);
                    link.put (LINK_ATTRIBUTE_TYPE, LINK_FILE);
                    goToLink (link);
                    parent.changeDisplay (msrt.DISPLAY_BROWSER);
                }
            }
        } else {
            // get data from server
            Hashtable link = new Hashtable ();
            link.put (LINK_ATTRIBUTE_PAGE, PAGE_INFO);
            link.put (LINK_ATTRIBUTE_ID, new Integer (uid).toString ());
            link.put (LINK_ATTRIBUTE_TYPE, LINK_MMTP);
            link.put (LINK_ATTRIBUTE_CATEGORY_ID, categoryRemoteId);
            goToLink (link);
        }
    }

    /**
     * Show page
     * @param linkType String
     * @param linkValue String
     */
    public void page (String linkType, String linkValue) {
        reset ();

        Hashtable link = new Hashtable ();
        link.put (LINK_ATTRIBUTE_TYPE, linkType);
        link.put (LINK_ATTRIBUTE_PAGE, linkValue);
        link.put (LINK_ATTRIBUTE_COMMAND, linkValue);
        link.put (LINK_ATTRIBUTE_FILE, linkValue);
        link.put (LINK_ATTRIBUTE_URL, linkValue);
        goToLink (link);
    }

    /**
     * Dispatch single key.
     * @param keyCommand int
     * @param keyCode int
     * @param action int
     */
    public void dispatchSingleKey (int keyCommand, int keyCode, int action) {
        dispatchRepeatedKey (keyCommand, keyCode, action, 1, 1);
    }

    /**
     * Dispatch repeated key
     * @param keyCommand int
     * @param keyCode int
     * @param action int
     * @param count int
     * @param acceleration int
     */
    public void dispatchRepeatedKey (int keyCommand, int keyCode, int action,
                                     int count, int acceleration) {
        boolean needPaint = false;

        switch (keyCommand) {
            case Frame.KEY_COMMAND_LEFT:
                if (count == 1)
                    goBack ();
                break;
            case Frame.KEY_COMMAND_UP:
                if (count <= 2)
                    moveToNextLink ( -1, count == 1);
                else {
                    scrollToY (visibleTop - scrollPageStep * acceleration);
                    needPaint = true;
                }
                break;
            case Frame.KEY_COMMAND_DOWN:
                if (count <= 2)
                    moveToNextLink (1, count == 1);
                else {
                    scrollToY (visibleTop + scrollPageStep * acceleration);
                    needPaint = true;
                }
                break;
            case Frame.KEY_COMMAND_ENTER:
            case Frame.KEY_COMMAND_RIGHT:
                if (count == 1)
                    goToLinkInternal ();
                break;
            default:
                if (keyCode >= '1' && keyCode <= '9') {
                    int dy = heightTotal - screenHeight;
                    if (dy > 0) {
                        scrollToY (((keyCode - '1') * dy) / 8);
                        needPaint = true;
                    }
                }
                break;
        }
        if (needPaint)
            repaint ();
    }

    public void showNotify () {
        if (!getFont ().equals (prevFont) && isPageLoaded) {
            goToLink (currentPageLink);
        }
        prevFont = getFont ();
        timeWhenBecameVisible = System.currentTimeMillis ();

        if (isPageLoaded && containsForm)
            parent.display.setCurrent (form);
    }

    public void hideNotify () {
        msrt.statistics[msrt.STATISTICS_BROWSER_TIME] +=
            (System.currentTimeMillis () - timeWhenBecameVisible) / 1000;

        if (isErrorPage) {
            currentPageLink = null;
            isErrorPage = false;
        }
    }

    /**
     * Handle pointer event - scroll the page
     * @param x - x
     * @param y - y
     */
    public void pointerReleased (int x, int y) {
        if (x >= componentWidth - Design.SCROLLBAR_WIDTH) {
            boolean isHigher = (y - thumbY - thumbH / 2) > 0;
            int step = screenHeight - getFont ().getHeight ();
            if (isHigher)
                scrollDown (step);
            else
                scrollUp (step);

            repaint ();
        } else {
            boolean f = false;
            for (int i = 0; i < linkDimensionsN; i += 5) {
                if (x > linkDimensions[i] && x < linkDimensions[i + 2] &&
                    y > linkDimensions[i + 1] && y < linkDimensions[i + 3]) {
                    if (curSentLink == linkDimensions[i + 4])
                        goToLinkInternal ();
                    else {
                        curSentLink = linkDimensions[i + 4];
                        f = true;
                        break;
                    }
                }
            }
            if (!f)
                curSentLink = -1;
            repaint ();
        }
    }

    /**
     * Scroll down
     * @param step - number of lines
     */
    private void scrollDown (int step) {
        visibleTop += step;
        if (visibleTop > heightLimit)
            visibleTop = heightLimit;
    }

    /**
     * Scroll up
     * @param step - number of lines
     */
    private void scrollUp (int step) {
        visibleTop -= step;
        if (visibleTop < 0)
            visibleTop = 0;
    }

    /**
     * Handle commands
     * @param c - command
     */
    public void commandAction (Command c) {
        if (c == parent.menuCommand) {
            parent.changeDisplay (msrt.DISPLAY_MENU);
        } else if (c == backCommand) {
            goBack ();
        } else if (c == mapCommand) {
            parent.changeDisplay (msrt.DISPLAY_MAP);
        } else {
            goToLinkInternal ();
        }
    }

    private void goBack () {
        historyLinksP = (historyLinksP + CAPACITY_HISTORY - 1) % CAPACITY_HISTORY;
        Hashtable link = historyLinks[historyLinksP]; // fetch previous page
        historyLinks[historyLinksP] = null; // clear top of history stack
        currentPageLink = null; // current is null

        if (link != null) {
            goToLink (link);

            if (historyLinks[(historyLinksP + CAPACITY_HISTORY - 1) % CAPACITY_HISTORY] == null)
                removeCommand (backCommand);
        } else
            parent.changeDisplayBack ();
    }

    /**
     * Reads data from local resource file
     * @param link Hashtable
     * @return boolean
     * @throws IOException
     */
    private boolean goLocal (Hashtable link) throws IOException {
        isPageLoaded = false;
        setMasterCommand (null);

        boolean result = false;
        String file = (String) link.get (LINK_ATTRIBUTE_FILE);
        InputStream is = getClass ().getResourceAsStream (file);
        if (is != null) {
            try {
                DataInputStream in = new DataInputStream (is);

                in.readByte (); // type = vector
                in.readInt (); // =2
                OnlineLoader.readObject (in); // packet attributes (type=Page)

                read (in);
                in.close ();
                repaint ();
                result = true;

                addToHistory (link);
            } catch (OutOfMemoryError ex) {
                parent.showInfo (msrt.ALERT_ERROR, 9);
            }
        }
        return result;
    }

    /**
     * Read Browser block from incoming stream
     * @param in DataInputStream
     * @throws IOException
     */
    public void read (DataInputStream in) throws IOException {
        /**
         * Calculate character widths
         */
        if (!getFont ().equals (prevFont) || charWidths['i'] == 0) {
            prevFont = getFont ();
            for (char ch = ' '; ch < MAX_AVAILABLE_CHARACTER; ch++)
                charWidths[ch] = (byte) prevFont.charWidth (ch);
        }

        /**
         * INIT
         */
        lineN = 0;
        curSentLink = -1;
        visibleTop = 0;
        heightTotal = 0;
        pageBackgroundColor = Design.COLOR_COMPONENT_BACKGROUND;
        pageColor = Design.COLOR_COMPONENT_TEXT;
        containsForm = false;

        visibleTop = 0;
        visibleBottom = screenHeight;

        scrollPageStep = screenHeight - getFont ().getHeight () * 2;

        // parser constants
        LINE_LENGTH_LIMIT = contentWidth / charWidths['i'];
        curLine = new char[LINE_LENGTH_LIMIT];
        curLineSentId = new short[LINE_LENGTH_LIMIT];
        curLineSentOff = new short[LINE_LENGTH_LIMIT];

        pageVariables.clear ();

        form = new Form ("The Form");
        form.setCommandListener (this);
        formCommands = new Hashtable ();
        formItemToVarsMap = new Hashtable ();
        formChoiceGroupToOptionsMap = new Hashtable ();

        /**
         * READ DATA
         */

        // page record : hashtable
        in.readByte (); // type = hashtable
        int pageAttributesCount = in.readInt ();

        for (int pageAttr = 0; pageAttr < pageAttributesCount; pageAttr++) {
            in.readByte (); // type = char
            char ch = in.readChar ();

            switch (ch) {
                case ATTRIBUTE_BGCOLOR:
                    in.readByte (); // type = int
                    pageBackgroundColor = in.readInt ();
                    break;
                case ATTRIBUTE_COLOR:
                    in.readByte (); // type = int
                    pageColor = in.readInt ();
                    break;
                case ATTRIBUTE_DATA:
                    readParagraphes (in);
                    break;
                case ATTRIBUTE_CONTROL:
                    readControl (in);
                    break;
                default:
                    OnlineLoader.readObject (in);
            }
        }

        curLine = null;
        curLineSentId = null;
        curLineSentOff = null;

        isPageLoaded = true;

        if (!Frame.getInstance ().getComponent ().equals (this))
            parent.changeDisplay (msrt.DISPLAY_BROWSER);

        if (containsForm)
            parent.display.setCurrent (form);

        System.gc ();
        Thread.yield ();
    }

    private void readControl (DataInputStream in) throws IOException {
        in.readByte (); // type = vector
        int n = in.readInt ();

        for (int i = 0; i < n; i++) {
            in.readByte (); // type = hashtable
            String label = null;
            Hashtable link = null;
            int mode = 0;

            int rf = in.readInt ();
            for (int j = 0; j < rf; j++) {
                in.readByte (); // type = char
                char ch = in.readChar ();
                switch (ch) {
                    case ATTRIBUTE_LABEL:
                        label = (String) OnlineLoader.readObject (in);
                        break;
                    case ATTRIBUTE_LINK:
                        link = (Hashtable) OnlineLoader.readObject (in);
                        break;
                    case ATTRIBUTE_MODE:
                        mode = ((Integer) OnlineLoader.readObject (in)).intValue ();
                        break;
                    default:
                        OnlineLoader.readObject (in);
                }
            }

            if (label != null && link != null) {
                Command command = new Command (label, mode, 1);
                form.addCommand (command);
                formCommands.put (command, link);
                containsForm = true;
            }
        }
    }

    private void readParagraphes (DataInputStream in) throws IOException {
        in.readByte (); // type = vector
        paragraphCount = in.readInt ();

        paraAttribs = new int[paragraphCount * PARAGRAPH_PARAM_COUNT];
        sentAttribs = new Vector ();

        sentText = new Vector ();
        sentParaId = new Vector ();

        sentLink = new Vector ();
        linkTotal = 0;

        int[] parAttribsBuf = new int[PARAGRAPH_PARAM_COUNT];

        // list of paragraphs
        for (int i = 0, sent = 0; i < paragraphCount; i++) {
            in.readByte (); // type = hashtable

            for (int j = 0; j < PARAGRAPH_PARAM_COUNT; j++) // clear params
                parAttribsBuf[j] = 0;
            parAttribsBuf[PARAGRAPH_PARAM_BGCOLOR] = -1;
            parAttribsBuf[PARAGRAPH_PARAM_COLOR] = -1;

            int startSent = sent;
            String test = null;

            int rf = in.readInt ();
            for (int j = 0; j < rf; j++) {
                in.readByte (); // type = char
                char ch = in.readChar ();

                switch (ch) {
                    case ATTRIBUTE_COLOR:
                        in.readByte (); // type = int
                        parAttribsBuf[PARAGRAPH_PARAM_COLOR] = in.readInt ();
                        break;
                    case ATTRIBUTE_BGCOLOR:
                        in.readByte (); // type = int
                        parAttribsBuf[PARAGRAPH_PARAM_BGCOLOR] = in.readInt ();
                        break;
                    case ATTRIBUTE_SPACE:
                        in.readByte (); // type = int
                        parAttribsBuf[PARAGRAPH_PARAM_SPACE] = in.readInt ();
                        break;
                    case ATTRIBUTE_FONT_STYLE:
                        in.readByte (); // type = int
                        parAttribsBuf[PARAGRAPH_PARAM_FONT_STYLE] = in.readInt ();
                        break;
                    case ATTRIBUTE_FONT_SIZE:
                        in.readByte (); // type = int
                        parAttribsBuf[PARAGRAPH_PARAM_FONT_SIZE] = in.readInt ();
                        break;
                    case ATTRIBUTE_PADDING_TOP:
                        in.readByte (); // type = int
                        parAttribsBuf[PARAGRAPH_PARAM_PADDING_TOP] = in.readInt ();
                        break;
                    case ATTRIBUTE_PADDING_BOTTOM:
                        in.readByte (); // type = int
                        parAttribsBuf[PARAGRAPH_PARAM_PADDING_BOTTOM] = in.readInt ();
                        break;
                    case ATTRIBUTE_ALIGN:
                        in.readByte (); // type = int
                        parAttribsBuf[PARAGRAPH_PARAM_ALIGN] = in.readInt ();
                        break;
                    case ATTRIBUTE_TEST:
                        test = (String) OnlineLoader.readObject (in);
                        break;
                    case ATTRIBUTE_DATA:
                        in.readByte (); // type = vector
                        sent = readSentences (in, i, sent);
                        break;
                    default:
                        OnlineLoader.readObject (in);
                }
            }
            if (test != null) {
                // ignore sentence if test is null or empty string
                if (Util.isStringNullOrEmpty (resolveVariables (test)))
                    continue;
            }

            // copy attributes
            System.arraycopy (parAttribsBuf, 0, paraAttribs, i * PARAGRAPH_PARAM_COUNT, PARAGRAPH_PARAM_COUNT);

            // add padding-top
            addPadding (parAttribsBuf[PARAGRAPH_PARAM_PADDING_TOP], i);

            // parse content
            sentenceCount = sent;
            parse (startSent, sent);

            // add padding-bottom
            addPadding (parAttribsBuf[PARAGRAPH_PARAM_PADDING_BOTTOM], i);

            // add space after paragraph
            heightTotal += parAttribsBuf[PARAGRAPH_PARAM_SPACE];
        }
    }

    /**
     * Read sentences.
     * Attrbiutes are read explicitly as it speeds up the process and requires less memory than reading everything
     * into map and extract the data from it.
     * @param in DataInputStream
     * @param para int
     * @param sent int
     * @return int
     * @throws IOException
     */
    private int readSentences (DataInputStream in, int para, int sent) throws IOException {
        int n = in.readInt ();

        // resize vectors of elements
        int newsize = sent + n;
        sentText.setSize (newsize);
        sentAttribs.setSize (newsize);
        sentParaId.setSize (newsize);
        sentLink.setSize (newsize);

        for (int i = 0; i < n; i++) {
            in.readByte (); // type = hashtable
            int rf = in.readInt ();
            Object data = null;
            String test = null;
            int type = 0;
            int inputSize = 10;
            int inputMaxLength = 10;
            int inputMode = 0;
            Object inputDefault = null;
            String inputLabel = null;
            Vector options = null;

            int[] senAttribsBuf = new int[SENTENCE_PARAM_COUNT];
            senAttribsBuf[SENTENCE_PARAM_COLOR] = -1;
            senAttribsBuf[SENTENCE_PARAM_BGCOLOR] = -1;

            for (int j = 0; j < rf; j++) {
                in.readByte (); // type = char
                char ch = in.readChar ();

                switch (ch) {
                    case ATTRIBUTE_COLOR:
                        in.readByte (); // type = int
                        senAttribsBuf[SENTENCE_PARAM_COLOR] = in.readInt ();
                        break;
                    case ATTRIBUTE_BGCOLOR:
                        in.readByte (); // type = int
                        senAttribsBuf[SENTENCE_PARAM_BGCOLOR] = in.readInt ();
                        break;
                    case ATTRIBUTE_FONT_STYLE:
                        in.readByte (); // type = int
                        senAttribsBuf[SENTENCE_PARAM_FONT_STYLE] = in.readInt ();
                        break;
                    case ATTRIBUTE_SPACE_BEFORE:
                        in.readByte (); // type = int
                        senAttribsBuf[SENTENCE_PARAM_SPACE_BEFORE] = in.readInt ();
                        break;
                    case ATTRIBUTE_LINK:
                        Hashtable link = (Hashtable) OnlineLoader.readObject (in);
                        sentLink.setElementAt (link, sent);
                        linkTotal++;
                        break;
                    case ATTRIBUTE_TYPE:
                        in.readByte (); // type = int
                        type = in.readInt ();
                        break;
                    case ATTRIBUTE_TEST:
                        test = (String) OnlineLoader.readObject (in);
                        break;
                    case ATTRIBUTE_DATA:
                        data = OnlineLoader.readObject (in);
                        break;
                    case ATTRIBUTE_INPUT_SIZE:
                        in.readByte (); // type = int
                        inputSize = in.readInt ();
                        break;
                    case ATTRIBUTE_MAX_LENGTH:
                        in.readByte (); // type = int
                        inputMaxLength = in.readInt ();
                        break;
                    case ATTRIBUTE_MODE:
                        in.readByte (); // type = int
                        inputMode = in.readInt ();
                        break;
                    case ATTRIBUTE_LABEL:
                        inputLabel = (String) OnlineLoader.readObject (in);
                        break;
                    case ATTRIBUTE_DEFAULT:
                        inputDefault = OnlineLoader.readObject (in);
                        break;
                    case ATTRIBUTE_OPTIONS:
                        options = (Vector) OnlineLoader.readObject (in);
                        break;
                    default:
                        OnlineLoader.readObject (in);
                }
            }

            sentAttribs.setElementAt (senAttribsBuf, sent);
            sentParaId.setElementAt (new Integer (para), sent);
            sentText.setElementAt ("", sent);

            if (data != null) {
                if (test != null) {
                    // ignore sentence if test is null or empty string
                    if (Util.isStringNullOrEmpty (resolveVariables (test)))
                        continue;
                }
                if (type == SENTENCE_TYPE_TEXT) {
                    String value = resolveVariables (data.toString ());
                    if (Util.isStringNullOrEmpty (value))
                        continue;

                    char[] text = value.toCharArray ();
                    for (int z = 0; z < text.length; z++) {
                        char ch = text[z];
                        if (ch >= charWidths.length) {
                            if (ch == '\u2013' || ch == '\u2014')
                                ch = '-';
                            else if (ch == '\u201C' || ch == '\u201E' ||
                                     ch == '\u201D')
                                ch = '"';
                            else if (ch == '\u2116')
                                ch = 'N';
                            else
                                ch = ' ';
                            text[z] = ch;
                        }
                    }
                    sentText.setElementAt (text, sent);

                    form.append (value);
                } else if (type == SENTENCE_TYPE_IMAGE) {
                    byte[] buf = (byte[]) data;
                    if (buf.length > 0) {
                        try {
                            Image img = Image.createImage (buf, 0, buf.length);
                            sentText.setElementAt (img, sent);

                            form.append (img);
                        } catch (RuntimeException ex) {
                            continue;
                        }
                    } else
                        continue;
                } else if (type == SENTENCE_TYPE_INPUT) {
                    String variable = data.toString ();
                    String value = null;
                    if (inputDefault != null) {
                        pageVariables.put (variable, inputDefault);
                        value = inputDefault.toString ();
                    }
                    TextField textField = new TextField (inputLabel, value, inputSize, inputMode);
                    textField.setMaxSize (inputMaxLength);
                    form.append (textField);
                    formItemToVarsMap.put (textField, variable);
                    containsForm = true;
                } else if (type == SENTENCE_TYPE_DATE) {
                    String variable = data.toString ();
                    int timestamp = 0;
                    if (inputDefault != null) {
                        timestamp = (inputDefault instanceof Integer) ?
                                    ((Integer) inputDefault).intValue () :
                                    Util.atoi (inputDefault.toString (), 0);
                        if (msrt.config[msrt.CONFIG_TIME] != 0)
                            timestamp += SunDial.timeZoneOffset * 60 * 60;
                        pageVariables.put (variable, new Integer (timestamp));
                    }

                    DateField dateField = new DateField (inputLabel, inputMode);
                    if (timestamp > 0)
                        dateField.setDate (new Date (timestamp * 1000L));
                    form.append (dateField);
                    formItemToVarsMap.put (dateField, variable);
                    containsForm = true;
                } else if (type == SENTENCE_TYPE_CHOICE) {
                    String variable = data.toString ();
                    ChoiceGroup choiceGroup = new ChoiceGroup (inputLabel, inputMode);
                    for (int opt = 0; opt < options.size (); opt++) {
                        Hashtable option = (Hashtable) options.elementAt (opt);
                        choiceGroup.append ((String) option.get (new Character (ATTRIBUTE_LABEL)), null);

                        Boolean isSelected = (Boolean) option.get (new Character (ATTRIBUTE_SELECTED));
                        if (isSelected != null && isSelected.booleanValue ())
                            choiceGroup.setSelectedIndex (opt, true);
                    }
                    form.append (choiceGroup);
                    formItemToVarsMap.put (choiceGroup, variable);
                    formChoiceGroupToOptionsMap.put (choiceGroup, options);
                    containsForm = true;
                }

                // change variables in link
                Hashtable link = (Hashtable) sentLink.elementAt (sent);
                if (link != null) {
                    for (Enumeration en = link.keys (); en.hasMoreElements (); ) {
                        Object key = en.nextElement ();
                        Object val = link.get (key);

                        if (!(val instanceof String))
                            continue;

                        String v = (String) val;
                        link.put (key, resolveVariables (v));
                    }
                }
            }
            sent++;
        }
        return sent;
    }

    /**
     * Resolve names of variables.
     * Variable name is inclosed into curly braces and prefixed with dollar sign.
     * If variable isn't found then it's name is removed from source string.
     * Ex. "${var}"
     * @param source String
     * @return String
     */
    private String resolveVariables (String source) {
        if (source.indexOf ('$') < 0)
            return source;

        char[] a = source.toCharArray ();
        StringBuffer target = new StringBuffer ();
        target.ensureCapacity (a.length);

        char prev = '\u0000';
        int varBegin = -1;
        boolean hasVariable = false;

        for (int i = 0; i < a.length; i++) {
            char ch = a[i];
            target.append (ch);

            if (ch == '{' && prev == '$')
                varBegin = i + 1;

            if (ch == '}' && varBegin >= 0) {
                int len = i - varBegin;
                String value = evalVariable (new String (a, varBegin, len));

                int tl = target.length ();
                target.delete (tl - len - 3, tl);
                if (value != null)
                    target.append (value);

                hasVariable = true;
                varBegin = -1;
            }

            prev = ch;
        }

        return hasVariable ? resolveVariables (target.toString ()) : target.toString ();
    }

    /**
     * Get value of variable. First variable is checked in global context and then in page
     * context.
     * @param name String
     * @return String
     */
    private String evalVariable (String name) {
        Object value = variables.get (name);
        if (value == null) {
            value = pageVariables.get (name);
        }
        return (String) value;
    }

    /**
     * Add padding for paragraphes
     * @param padding int
     * @param i int
     */
    private void addPadding (int padding, int i) {
        if (padding > 0) {
            storeLine (0, padding, heightTotal, -1, i, 0, 0);
            heightTotal += padding;
        }
    }

    private void storeLine (int width, int height, int top,
                            int startSentId, int startSentOff,
                            int endSentId, int endSentOff) {

//        System.out.println ("store_ ssi: " + startSentId + " sso: " + startSentOff +
//            " esi: " + endSentId + " eso: " + endSentOff + " width: " + width +
//            " height: " + height + " top: " + top);

        if (lineN >= lineLimit) {
            lineLimit += CAPACITY_LINES;
            lineWidths = arrayResize (lineWidths, lineLimit);
            lineHeights = arrayResize (lineHeights, lineLimit);
            lineTops = arrayResize (lineTops, lineLimit);
            lineSentenceStartSentId = arrayResize (lineSentenceStartSentId, lineLimit);
            lineSentenceStartSentOff = arrayResize (lineSentenceStartSentOff, lineLimit);
            lineSentenceEndSentId = arrayResize (lineSentenceEndSentId, lineLimit);
            lineSentenceEndSentOff = arrayResize (lineSentenceEndSentOff, lineLimit);
        }
        lineWidths[lineN] = (short) width;
        lineHeights[lineN] = (short) height;
        lineTops[lineN] = (short) top;
        lineSentenceStartSentId[lineN] = (short) startSentId;
        lineSentenceStartSentOff[lineN] = (short) startSentOff;
        lineSentenceEndSentId[lineN] = (short) endSentId;
        lineSentenceEndSentOff[lineN] = (short) endSentOff;
        lineN++;
    }

    /**
     * Resize array
     * @param a short[]
     * @param newsize int
     * @return short[]
     */
    private static short[] arrayResize (short[] a, int newsize) {
        short[] b = a;
        int n = a.length;
        a = new short[newsize];
        System.arraycopy (b, 0, a, 0, n);
        b = null;
        return a;
    }


    /**
     * Parse incoming stream: break it into lines and extract links
     */
    private int LINE_LENGTH_LIMIT = 256; // line width limit
    private int curLinePtr = 0; // current line pointer
    private char curLine[]; // current line
    private short curLineSentId[];
    private short curLineSentOff[];
    private int curLineWidth; // current line width
    private int curLineHeight; // current line height
    private int lastSpacePtr; // line offset of last space
    private int lastSpaceWidth; // line width at the moment when last space occur
    private int lastSpaceHeight; // line height at the moment when last space occur
    private int afterLastSpaceHeight; // line height after last space

    private static final char CHAR_NEW_LINE = '\n';
    private static final char CHAR_NULL = '\0';

    private void parse (int startSent, int endSent) {
//        System.out.println ("contentWidth: " + contentWidth);

        curLinePtr = 0;
        curLineWidth = 0;
        curLineHeight = 0;
        lastSpacePtr = -1;
        lastSpaceHeight = 0;

        int fontHeight = getFont ().getHeight ();

        // iterate over sentences
        for (int k = startSent; k < endSent; k++) {
            Object obj = sentText.elementAt (k);

            if (obj instanceof Image) {
                Image im = (Image) obj;
                int imWidth = im.getWidth ();
                int imHeight = im.getHeight ();
                if (sentLink.elementAt (k) != null)
                    imHeight += 3;
                addSymbolToLine (CHAR_NULL, imWidth, imHeight, k, 0);
            } else if (obj instanceof char[]) {
                char[] source = (char[]) obj;
                int len = source.length;

                int spaceBefore = ((int[]) sentAttribs.elementAt (k))[SENTENCE_PARAM_SPACE_BEFORE];
                curLineWidth += spaceBefore;

                for (int i = 0; i < len; i++) {
                    char ch = source[i];
                    if (ch == CHAR_NEW_LINE) {
                        addLine (0, curLinePtr, curLineWidth, curLineHeight);

                        curLinePtr = 0;
                        curLineWidth = 0;
                        curLineHeight = 0;
                        lastSpacePtr = -1;
                        lastSpaceHeight = 0;
                    } else {
                        addSymbolToLine (ch, charWidths[ch], fontHeight, k, i);
                    }
                }
            }

            // add space b/w sentences
            lastSpacePtr = curLinePtr;
            lastSpaceWidth = curLineWidth;
            lastSpaceHeight = curLineHeight;
            afterLastSpaceHeight = 0;
        }
        if (curLinePtr > 0)
            addLine (0, curLinePtr, curLineWidth, curLineHeight);
    }

    /**
     * Add symbol to line.
     * @param sym char
     * @param symWidth int
     * @param symHeight int
     * @param sentenceId int
     * @param sentenceOffset int
     */
    private void addSymbolToLine (char sym, int symWidth, int symHeight,
                                  int sentenceId, int sentenceOffset) {
        if (curLineWidth + symWidth >= contentWidth) {
            // move to new line
            int spacePtr = 0, height = 0, width = 0;
            if (lastSpacePtr < 0) {
                spacePtr = curLinePtr;
                width = curLineWidth;
                height = curLineHeight;
            } else {
                spacePtr = lastSpacePtr;
                width = lastSpaceWidth;
                height = lastSpaceHeight;
            }

            addLine (0, spacePtr, width, height);

            curLinePtr -= spacePtr;
            if (curLinePtr > 0) {
                System.arraycopy (curLine, spacePtr, curLine, 0, curLinePtr);
                System.arraycopy (curLineSentId, spacePtr, curLineSentId, 0, curLinePtr);
                System.arraycopy (curLineSentOff, spacePtr, curLineSentOff, 0, curLinePtr);

                curLineWidth -= lastSpaceWidth;
                lastSpaceHeight = afterLastSpaceHeight;
                curLineHeight = lastSpaceHeight;
            } else {
                curLineWidth = 0;
                curLineHeight = 0;
            }
            lastSpacePtr = -1;
        }

        // add symbol to current line
        if (curLinePtr < LINE_LENGTH_LIMIT) {
            curLine[curLinePtr] = sym;
            curLineSentId[curLinePtr] = (short) sentenceId;
            curLineSentOff[curLinePtr] = (short) sentenceOffset;
            curLinePtr++;

            curLineWidth += symWidth;
            if (symHeight > curLineHeight)
                curLineHeight = symHeight;
            if (symHeight > afterLastSpaceHeight)
                afterLastSpaceHeight = symHeight;

            if (sym == '\u0020') {
                lastSpacePtr = curLinePtr;
                lastSpaceWidth = curLineWidth;
                lastSpaceHeight = curLineHeight;
                afterLastSpaceHeight = symHeight;
            }
        }
    }

    /**
     * Add new line
     * @param start int
     * @param N int
     * @param lineWidth int
     * @param lineHeight int
     */
    private void addLine (int start, int N, int lineWidth, int lineHeight) {
        int startSentId = curLineSentId[start];
        int startSentOff = curLineSentOff[start];
        int endSentId = (N > 0) ? curLineSentId[start + N - 1] : startSentId;
        int endSentOff = (N > 0) ? curLineSentOff[start + N - 1] : startSentOff;

        storeLine (lineWidth, lineHeight, heightTotal,
                   startSentId, startSentOff, endSentId, endSentOff);

        // assign tops and bottoms for links
        for (int i = startSentId; i <= endSentId; i++) {
            if (sentLink.elementAt (i) != null) {
                if (i >= sentLinkLimit) {
                    sentLinkLimit = i + CAPACITY_LINKS;
                    sentLinkTop = arrayResize (sentLinkTop, sentLinkLimit);
                    sentLinkBottom = arrayResize (sentLinkBottom, sentLinkLimit);
                }
                sentLinkTop[i] = (short) heightTotal;
                sentLinkBottom[i] = (short) (heightTotal + lineHeight);
            }
        }

        heightTotal += lineHeight;
    }

    /**
     * THE PAINT
     * @param canvasGraphics Graphics
     */
    public void paint (Graphics canvasGraphics) {
        if (!isPageLoaded) {
            // blank page
            Graphics g = getScreenBufferGraphics ();

            g.setColor (0xffffff);
            g.fillRect (0, 0, screenWidth, screenHeight);

            paintScreenBuffer (canvasGraphics);

            return;
        }
        try {
            screenWidth = componentWidth;
            screenHeight = componentHeight;
            contentWidth = screenWidth - MARGIN_LEFT - MARGIN_RIGHT - Design.SCROLLBAR_WIDTH;

            heightLimit = heightTotal - screenHeight;
            if (heightLimit < 0)
                heightLimit = 0;

            linkDimensionsN = 0;

            int fontHeight = getFont ().getHeight ();

            Graphics g = getScreenBufferGraphics ();

            g.setColor (pageBackgroundColor);
            g.fillRect (0, 0, screenWidth, screenHeight);
            g.setClip (0, 0, screenWidth, screenHeight);

            int lineStart = 0;
            int lineSentenceN = lineN;
            int lineEnd = lineSentenceN;

            visibleBottom = visibleTop + screenHeight;

            // find which lines are visible
            for (int i = 0, t = 0; i < lineSentenceN; i++) {
                int curh = lineTops[i];
                if (curh > visibleTop && t == 0) {
                    lineStart = i - 1;
                    if (lineStart < 0)
                        lineStart = 0;
                    t = 1;
                }
                if (curh > visibleBottom) {
                    lineEnd = i;
                    break;
                }
            }

            boolean isCurSentLinkVisible = false;
            int lastSentenceId = -1; // id of last processed sentence

            // paint all visible lines
            for (int i = lineStart; i < lineEnd; i++) {
                int lineCode = lineSentenceStartSentId[i];
                int x = MARGIN_LEFT;
                int y = lineTops[i] - visibleTop;
                int lineHeight = lineHeights[i];

                if (lineCode < 0) {
                    int paraId = lineSentenceStartSentOff[i];

                    int bgcolor = paraAttribs[paraId * PARAGRAPH_PARAM_COUNT +
                                  PARAGRAPH_PARAM_BGCOLOR];
                    if (bgcolor < 0)
                        bgcolor = pageBackgroundColor;

                    g.setColor (bgcolor);
                    g.fillRect (0, y, screenWidth, lineHeight);
                } else {
                    // draw text/image line
                    int startSentId = lineSentenceStartSentId[i];
                    int startSentOff = lineSentenceStartSentOff[i];
                    int endSentId = lineSentenceEndSentId[i];
                    int endSentOff = lineSentenceEndSentOff[i];

                    isCurSentLinkVisible |= curSentLink >= startSentId &&
                        curSentLink <= endSentId;

                    int sentId = startSentId;
                    int sentOff = startSentOff;
                    int prevSentId = -1;
                    Object objSent = null;
                    int color = 0;
                    int bgcolor = 0;
                    int fontStyle = FONT_NORMAL;

                    int paraId = 0;

                    paraId = ((Integer) sentParaId.elementAt (sentId)).intValue ();

                    int paraBgcolor = paraAttribs[paraId * PARAGRAPH_PARAM_COUNT +
                                      PARAGRAPH_PARAM_BGCOLOR];
                    int paraColor = paraAttribs[paraId * PARAGRAPH_PARAM_COUNT +
                                    PARAGRAPH_PARAM_COLOR];
                    int align = paraAttribs[paraId * PARAGRAPH_PARAM_COUNT +
                                PARAGRAPH_PARAM_ALIGN];

                    if (paraBgcolor < 0)
                        paraBgcolor = pageBackgroundColor;
                    if (paraColor < 0)
                        paraColor = pageColor;

                    // fill paragraph background
                    g.setColor (paraBgcolor);
                    g.fillRect (0, y, screenWidth, lineHeight);

                    boolean sentenceHasLink = false;

                    int lineGutter = contentWidth - lineWidths[i];
                    int xSentStart = 0;

                    if (align != ALIGN_LEFT)
                        x += (align == ALIGN_RIGHT) ? lineGutter : (lineGutter >> 1);

                    while (sentId < endSentId ||
                           (sentId == endSentId && sentOff <= endSentOff)) {
                        if (sentId != prevSentId) {
                            // change attributes
                            objSent = sentText.elementAt (sentId);
                            int[] attribs = (int[]) sentAttribs.elementAt (sentId);
                            color = attribs[SENTENCE_PARAM_COLOR];
                            bgcolor = attribs[SENTENCE_PARAM_BGCOLOR];
                            fontStyle = attribs[SENTENCE_PARAM_FONT_STYLE];

                            if (lastSentenceId != sentId && sentOff == 0)
                                x += attribs[SENTENCE_PARAM_SPACE_BEFORE];

                            if (color < 0)
                                color = paraColor;
                            if (bgcolor < 0)
                                bgcolor = paraBgcolor;

                            sentenceHasLink = false;

                            if (sentLink.elementAt (sentId) != null) {
                                sentenceHasLink = true;
                                if (sentId == curSentLink) {
                                    int t = color;
                                    color = bgcolor;
                                    bgcolor = t;
                                }
                                xSentStart = x;
                            }
                            prevSentId = sentId;
                            lastSentenceId = sentId;
                        }

                        if (objSent instanceof Image) {
                            Image im = (Image) objSent;
                            int imWidth = im.getWidth ();
                            int imHeight = im.getHeight ();

                            g.drawImage (im, x, y, Graphics.LEFT | Graphics.TOP);

                            if (sentenceHasLink) {
                                g.setColor (color);
                                g.drawRect (x - 1, y - 1, imWidth + 1, imHeight + 1);
                                g.setColor (bgcolor);
                                g.drawRect (x - 2, y - 2, imWidth + 3, imHeight + 3);

                                addLinkDimensions (sentId, x, y, x + imWidth,
                                    y + imHeight);
                            }

                            x += imWidth;
                            sentId++;
                        } else if (objSent instanceof char[]) {
                            char[] source = (char[]) objSent;
                            int len = source.length;

                            int start = sentOff;
                            int end = (endSentId > sentId) ?
                                      len : endSentOff + 1;

                            int w = 0;
                            for (int z = start; z < end; z++) {
                                w += charWidths[source[z]];
                            }

                            if (w > 0) {
                                g.setColor (bgcolor);
                                g.fillRect (x, y, w, lineHeight);
                                g.setColor (color);

                                g.drawChars (source, start, end - start, x, y + 1,
                                             Graphics.TOP | Graphics.LEFT);

                                // underline
                                if ((fontStyle & FONT_UNDERLINE) != 0)
                                    g.drawLine (x, y + fontHeight - 1, x + w,
                                                y + fontHeight - 1);

                                x += w;
                            }

                            sentOff = end + 1;

                            if (sentOff >= len) { // end of sentence is reached
                                if (sentenceHasLink)
                                    addLinkDimensions (sentId, xSentStart, y, x,
                                        y + lineHeight);

                                sentId++;
                                sentOff = 0;
                                sentenceHasLink = false;
                            }
                        } else {
                            // ignore unkown contents
                            sentId++;
                        }
                    } // iterate sentences contens
                    if (sentenceHasLink) {
                        addLinkDimensions (sentId, xSentStart, y, x, y + lineHeight);
                    }
                }
            } // iterate lines

            if (isCurSentLinkVisible) {
                Command c = linkCommand;
                String type = null;
                Hashtable link = (Hashtable) sentLink.elementAt (curSentLink);
                type = (String) link.get (LINK_ATTRIBUTE_TYPE);

                if (type.equals (LINK_CALL))
                    c = linkCallCommand;
                else if (type.equals (LINK_SMS))
                    c = linkSMSCommand;
                else if (type.equals (LINK_DATA))
                    c = linkDataCommand;
                else if (type.equals (LINK_COMMAND))
                    c = linkExecuteCommand;
                else if (type.equals (LINK_INPUT))
                    c = linkInputCommand;

                setMasterCommand (c);
            } else
                setMasterCommand (null);

            drawScrollbar (g, heightTotal, 0, visibleTop);

            paintScreenBuffer (canvasGraphics);
        } catch (Exception ex) {
            // paint() can work in parallel to read(), in this case some data
            // could change during repaint causing for ex. ArrayOutOfBoundsException.
            // but, it's not possible to make these methods synchronized, because
            // in this case main thread could lock until reading is over, making
            // impossible to cancel or somehow interrupt reading process.
        }
    }

    /**
     * Add dimensions of link
     * @param sentId int
     * @param x1 int
     * @param y1 int
     * @param x2 int
     * @param y2 int
     */
    private void addLinkDimensions (int sentId, int x1, int y1, int x2, int y2) {
        if (linkDimensionsN + 5 >= linkDimensionsLimit) {
            linkDimensionsLimit += CAPACITY_LINES;
            linkDimensions = arrayResize (linkDimensions, linkDimensionsLimit);
        }

        linkDimensions[linkDimensionsN] = (short) x1;
        linkDimensions[linkDimensionsN + 1] = (short) y1;
        linkDimensions[linkDimensionsN + 2] = (short) x2;
        linkDimensions[linkDimensionsN + 3] = (short) y2;
        linkDimensions[linkDimensionsN + 4] = (short) sentId;
        linkDimensionsN += 5;
    }

    /**
     * Move to next sentence link in specified direction.
     * @param dir int
     * @param walkAround boolean
     */
    private void moveToNextLink (int dir, boolean walkAround) {
        if (sentLink.size () == 0)
            return;

        int i = ((curSentLink < 0 && dir < 0) ? sentenceCount : curSentLink) + dir;
        int newLink = -1;

        boolean isCurLinkVisible = false;
        if (curSentLink >= 0)
            isCurLinkVisible = sentLinkTop[curSentLink] <= visibleBottom &&
                               sentLinkBottom[curSentLink] > visibleTop;

        int ymax = 0xffff;
        int ymin = 0;

        if (isCurLinkVisible) {
            // find next link on visible page
            while (i >= 0 && i < sentenceCount) {
                if (sentLink.elementAt (i) != null) {
                    ymin = sentLinkTop[i];
                    ymax = sentLinkBottom[i];

                    newLink = i;
                    break;
                }
                i += dir;
            }
        } else {
            // find first visible link
            for (int j = 0, k = 0; k < sentenceCount && newLink < 0; j = (j + dir + sentenceCount) % sentenceCount, k++) {
                if (sentLink.elementAt (j) != null) {
                    int top = sentLinkTop[j];
                    int bottom = sentLinkBottom[j];

                    if ((dir > 0 && top >= visibleTop) ||
                        (dir < 0 && bottom < visibleBottom)) {
                        newLink = j;
                        ymax = bottom;
                        ymin = top;
                    }
                }
            }
        }

        int prevVisibleTop = visibleTop;
        int prevSentLink = curSentLink;

        if (dir > 0) {
            if (ymax > visibleBottom)
                scrollToY (Math.min (ymax - screenHeight, visibleTop + scrollPageStep * dir));
        } else {
            if (ymin < visibleTop)
                scrollToY (Math.max (ymin, visibleTop + scrollPageStep * dir));
        }
        if (newLink >= 0)
            if (ymin <= visibleTop + screenHeight &&
                ymax > visibleTop)
                curSentLink = newLink;

        // nothing has changed since the last view, walk-around the page
        if (visibleTop == prevVisibleTop && prevSentLink == curSentLink && walkAround) {
            if (dir > 0)
                scrollToY (0);
            else
                scrollToY (Math.max (0, heightTotal - screenHeight));
            curSentLink = -1;
        }

        repaint ();
    }

    /**
     * Scroll map to Y
     * @param y int
     */
    private void scrollToY (int y) {
        if (y > heightTotal - screenHeight)
            y = heightTotal - screenHeight;
        if (y < 0)
            y = 0;
        visibleTop = y;
    }

    /**
     * Go To Selected Link
     */
    private void goToLinkInternal () {
        if (curSentLink >= 0) {
            Hashtable link = (Hashtable) sentLink.elementAt (curSentLink);
            goToLink (link);
        }
    }

    /**
     * Go to the specified link
     * @param link Hashtable
     * @return boolean
     */
    public boolean goToLink (Hashtable link) {
        boolean res = true;
        try {
            String type = (String) link.get (LINK_ATTRIBUTE_TYPE);

            if (type.equals (LINK_MMTP)) {
                goRemote (link);
            } else if (type.equals (LINK_FILE)) {
                res = goLocal (link);
            } else if (type.equals (LINK_DATA)) {
                // select specified object
                parent.theObjectManager.select (link);
            } else if (type.equals (LINK_WEB)) {
                // call built-in browser
                parent.platformRequest ((String) link.get (LINK_ATTRIBUTE_URL));
            } else if (type.equals (LINK_SMS)) {
                // send sms
                String smsNumber = (String) link.get (LINK_ATTRIBUTE_PHONE);
                String smsText = (String) link.get (LINK_ATTRIBUTE_TEXT);
                parent.sendMessage (smsNumber, smsText);
            } else if (type.equals (LINK_CALL)) {
                // call
                parent.platformRequest ("tel:" + (String) link.get (LINK_ATTRIBUTE_PHONE));
            } else if (type.equals (LINK_COMMAND)) {
                // call internal command
                processCommand (link);
            }
        } catch (ConnectionNotFoundException ex) {
            res = false;
        } catch (SecurityException ex) {
            res = false;
        } catch (IOException ex) {
            res = false;
        }
        isErrorPage = false;

        return res;
    }

    /**
     * Process command
     * @param link Hashtable
     */
    private void processCommand (Hashtable link) {
        String command = (String) link.get (LINK_ATTRIBUTE_COMMAND);

        // show map
        if (COMMAND_MAP.equals (command)) {
            Object xObj = link.get (LINK_ATTRIBUTE_COMMAND_MAP_X);
            Object yObj = link.get (LINK_ATTRIBUTE_COMMAND_MAP_Y);
            Object latObj = link.get (LINK_ATTRIBUTE_COMMAND_MAP_LATITUDE);
            Object lonObj = link.get (LINK_ATTRIBUTE_COMMAND_MAP_LONGITUDE);
            Object scaleObj = link.get (LINK_ATTRIBUTE_COMMAND_MAP_SCALE);

            try {
                int x = Map.originX;
                int y = Map.originY;

                if (xObj != null && yObj != null) {
                    x = Map.x2local (Integer.parseInt ((String) xObj));
                    y = Map.y2local (Integer.parseInt ((String) yObj));
                } else if (latObj != null && lonObj != null) {
                    x = Map.longitude2x (Integer.parseInt ((String) lonObj));
                    y = Map.latitude2y (Integer.parseInt ((String) latObj));
                }
                int scale = parent.theMap.scale;
                if (scaleObj != null) {
                    scale = Integer.parseInt ((String) scaleObj);
                }
                parent.theMap.moveFocus (x, y, x, y, scale, true);
            } catch (NumberFormatException ex1) {
            }

            parent.changeDisplay (msrt.DISPLAY_MAP);
        }
        // show menu
        else if (COMMAND_MENU.equals (command)) {
            parent.changeDisplay (msrt.DISPLAY_MENU);
        }
        // show objects
        else if (COMMAND_OBJLIST.equals (command) || COMMAND_OBJECTS.equals (command)) {
            Object catObj = link.get (LINK_ATTRIBUTE_COMMAND_OBJECTS_CATEGORY);
            if (catObj != null) {
                try {
                    Integer loci = (Integer) Map.ctRemoteToLocal.get (Integer.valueOf ((String) catObj));
                    if (loci != null) {
                        Map.curCategory = loci.intValue (); // local
                    } else {
                        Map.curCategory = Map.CATEGORY_ROOT;
                    }
                } catch (NumberFormatException ex2) {
                }
            } else {
                Map.curCategory = Map.CATEGORY_ROOT;
            }
            parent.theObjectManager.createAndShowObjList (this);
        }
        // reload page
        else if (COMMAND_RELOAD.equals (command)) {
            if (errorPageLink != null) {
                goToLink (errorPageLink);
            } else if (currentPageLink != null) {
                goToLink (currentPageLink);
            }
        } else if (COMMAND_BACK.equals (command)) {
            goBack ();
        } else if (COMMAND_ON_MARK_ADDED.equals (command)) {
            parent.theObjectManager.serverGetMarksInBackground();
            parent.changeDisplay (msrt.DISPLAY_MAP);
        }
    }

    /**
     * Load remote page (LINK_MMTP)
     * @param link Hashtable
     */
    private void goRemote (Hashtable link) {
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            isPageLoaded = false;
            setMasterCommand (null);

            addToHistory (link);

            OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.METHOD_GET_PAGE, true, false);
            ol.setOnlineLoaderListener (this);
            ol.addParameter (REQUEST_PARAMETER_LINK, link);
            ol.addParameter (REQUEST_PARAMETER_WIDTH, contentWidth);
            ol.addParameter (REQUEST_PARAMETER_HEIGHT, screenHeight);
            ol.addParameter (REQUEST_PARAMETER_VARS, pageVariables);

            ol.go ();
        }
    }

    private void addToHistory (Hashtable link) {
        if (currentPageLink != null && !isErrorPage) {
            int prev = (historyLinksP + CAPACITY_HISTORY - 1) % CAPACITY_HISTORY;
            if (!currentPageLink.equals (historyLinks[prev])) {
                historyLinks[historyLinksP] = currentPageLink;
                historyLinksP = (historyLinksP + 1) % CAPACITY_HISTORY;
                addCommand (backCommand);
            }
        }
        currentPageLink = link;
    }

    /**
     * Server request is completed
     * @param errorCode int
     * @param onlineLoader OnlineLoader
     */
    public void serverRequestComplete (int errorCode, OnlineLoader onlineLoader) {
        if (errorCode == OnlineLoaderListener.CODE_ERROR) {
            errorPageLink = currentPageLink;
            currentPageLink = null;

            pageError (onlineLoader.getMessageText ());
        } else {
            if (errorCode == OnlineLoaderListener.CODE_CANCEL)
                repaint ();
        }
    }

    public void commandAction (Command command, Displayable displayable) {
        if (!displayable.equals (form))
            return;

        Hashtable link = (Hashtable) formCommands.get (command);

        for (Enumeration en = formItemToVarsMap.keys (); en.hasMoreElements (); ) {
            Object item = en.nextElement ();
            Object value = null;

            if (item instanceof TextField) {
                value = ((TextField) item).getString ();
            } else if (item instanceof DateField) {
                int timestamp = (int) (((DateField) item).getDate ().getTime () / 1000);
                if (msrt.config[msrt.CONFIG_TIME] != 0)
                    timestamp -= SunDial.timeZoneOffset * 60 * 60;

                value = new Integer (timestamp);
            } else if (item instanceof ChoiceGroup) {
                ChoiceGroup choiceGroup = (ChoiceGroup) item;
                boolean[] flags = new boolean[choiceGroup.size ()];
                choiceGroup.getSelectedFlags (flags);
                Vector options = (Vector) formChoiceGroupToOptionsMap.get (choiceGroup);

                Vector selected = new Vector ();
                for (int i = 0; i < flags.length; i++) {
                    if (flags[i]) {
                        selected.addElement (((Hashtable) options.elementAt (i)).get (new Character (
                            ATTRIBUTE_DATA)));
                    }
                }
                value = selected;
            }

            String var = (String) formItemToVarsMap.get (item);
            if (var != null && value != null) {
                pageVariables.put (var, value);
            }
        }

        // close the form
        parent.display.setCurrent (Frame.getInstance ());

        goToLink (link);
    }
}
