/*****************************************************************************/
/*                               m f i n i t y                               */
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                         (c) 2003-2008 Ilya Shakhat                        */
/*****************************************************************************/

package com.ish.mobimap;

import java.util.*;
import javax.microedition.lcdui.*;
import com.ish.mobimap.ui.*;
import com.ish.mobimap.radar.*;

public class Map extends Component {
    static int pcN, streetN, elementN, stnamesNmax, categoryN, ctN;
    static int[] blN;
    static int[] lbN;
    static int[] gspN;

    static short[] pcx, pcy; // crossroad: x, y
    static short[] pcconp, pcconpi; // crossroad: pointer in con and coni
    static short[] pcseg; // crossroad segments
    static short[] con, coni, conel; // connection, reverse connection, element for connect
    static short[] elconp, elcon; // connection for elements
    static byte[] condir; // connection direction, quality and traffic data

    static int[] stnamep;
    static char[] stnames;

    // elements
    static byte[] ellen;
    static short[] el2st;

    // buildings
    static short[][] blx, bly;
    static short[][] blst;
    static short[][] blnamep;
    static byte[][] blnumber;

    // categories
    static int[] ctColor;
    static short[] ctParent; // reference to index
    static short[] ctFontStyle;
    static Image[] ctIcon;
    static char[][] ctName;
    static byte[] ctMode;
    static boolean[] ctIsStartupVisibility;
    static boolean[] ctIsFilledWithObjects;
    static byte[] ctShow;
    static Hashtable ctRemoteToLocal = new Hashtable ();
    static Hashtable ctLocalToRemote = new Hashtable ();

    // objects / points
    static short[][] lbx, lby, lbct, lbnamep; // lbct is a reference to category index
    static int[][] lbUid;
    static Hashtable[][] lbMeta;
    static char[][] name;

    // graphic layers
    static short[][] gs, gsp;
    static Hashtable[] img;

    public static boolean hasGeoData;
    public static long latitude, longitude, lat2meters, lon2meters;
    public static short timeZone;

    static boolean hasRoadDirs = false;
    static boolean hasObjects = false;

    static int segmentLargeChunk;
    static int segmentSmallChunk;
    public static final int SEGMENT_LIMIT = 60;
    public static final int SEGMENT_STATIC_POINTS = 0;
    public static final int SEGMENT_STATIC_ADDRESSES = 1;
    public static final int SEGMENT_PRELOADED_POINTS = 2;
    public static final int SEGMENT_ONLINE_ADDRESSES = 3;
    public static final int SEGMENT_LOCAL_MARKS = 4;
    public static final int SEGMENT_FIRST_LARGE_CHUNK = 5;
    public static final int SEGMENT_LAST_LARGE_CHUNK = 14;
    public static final int SEGMENT_FIRST_SMALL_CHUNK = 14;
    public static final int SEGMENT_LAST_SMALL_CHUNK = 60;

    public static final int IMG_SEGMENT_COUNT = 2;
    public static final int IMG_SEGMENT_STATIC = 0;
    public static final int IMG_SEGMENT_DYNAMIC = 1;

    private static short outx[], outy[];
    private static byte out_el[];
    static byte out_sp[];
    static byte out_pc[];
    private static int outpcMinIndex;
    private static char tmp[];
    static char name_alloc[];

    static int layerRef[];
    static boolean layerFlag[];
    static int layerN;

    static int visibleObjects[];
    static short visibleObjectsN;
    final static short visibleObjectsLimit = 250;
    static int visibleLabelsN; // amount of visible objects per type
    static int visibleStreetsN; // (for cursorSwitchMode restrictions)
    static int visibleCrossroadsN;

    // predefined constants for categories...
    final static int CATEGORY_ROOT = 0;
    final static int CATEGORY_VISIBLE = 1;
    final static int CATEGORY_RESULTS = 2;
    final static int CATEGORY_SYSTEM_STREET_ADDRESSES = 0xFFFE;
    final static int CATEGORY_BUILT_IN_COUNT = 3;

    // category show masks
    final static int CATEGORY_SHOW_ICON = 1;
    final static int CATEGORY_SHOW_LIST = 2;
    final static int CATEGORY_SHOW_LABEL = 4;
    final static int CATEGORY_SHOW_DOT = 8;

    // category modes: static or downloadable
    final static byte CATEGORY_MODE_STATIC = 0;
    final static byte CATEGORY_MODE_DOWNLOAD_POINTS = 1;
    final static byte CATEGORY_MODE_DOWNLOAD_MARKS = 2;

    // predefined classes constants...
    final static int CLASS_NONE = 0;
    final static int CLASS_PC = 1;
    final static int CLASS_STREET = 2;
    final static int CLASS_LABEL = 4;
    final static int CLASS_CATEGORY = 8;
    final static int CLASS_ADDRESS = 16;
    final static int CLASS_SYSTEM = 64;
    final static int MASK_PC = 0x1000000;
    final static int MASK_STREET = 0x2000000;
    final static int MASK_LABEL = 0x4000000;
    final static int MASK_CATEGORY = 0x8000000;
    final static int MASK_ADDRESS = 0x10000000;
    final static int MASK_SYSTEM = 0x40000000;

    // predefined constants for layers...
    public static final int LAYER_HORZ = -1;
    public static final int LAYER_VERT = -2;
    public static final int LAYER_GS = -3;
    public static final int LAYER_ADDRESSES = -4;
    public static final int LAYER_ROADDIRS = -6;
    public static final int LAYER_LOCATOR = -7;
    public static final int LAYER_SUN = -8;

    public static int originX, originY;
    public static int scale;
    public static int width, height, widthHalf, heightHalf;
    public static int cityX, cityY;
    public static short globalScale;
    public static int xmin, ymin, xmax, ymax;
    static int savedX, savedY, savedScale;
    static int selectionX, selectionY;
    static boolean changeFocus;
    static boolean showDistance;

    private msrt parent; // The MIDlet
    private static Map instance;

    public static boolean needpaint, update, fastupdate;
    private static boolean isRepainting;

    static int srhContents[];
    static int srhN;
    static int currentObjectClass, currentObjectItem, currentObject;
    static int curElements[];
    static int curElementsN;
    static int curCategory;

    private static boolean pointerSelects = false; // true, if pointer selects objects, false if moves map

    private static final int CURSOR_STEP = 5; // step of cursor moving
    private static final int CURSOR_CROSS_SIZE = 4; // size of cursor cross

    static int cursorMode;
    static int cursorObjectClass, cursorObjectItem, cursorObject;
    static int cursorX, cursorY;
    static int cursorObjectItem2; // allocated for addresses
    static int cursorScreenX, cursorScreenY;
    static boolean cursorActionByPointer;
    private final static int CURSOR_WIDTH = 7;
    private final static int CURSOR_HEIGHT = 10;
    int clipYLo, clipYHi;

    long time;
    private long timeWhenBecameVisible;
    private boolean stopMode;

    boolean isPointerSupported;
    private int pointerButtonX;
    private int pointerButtonY;
    private final static int POINTER_BUTTON_SIZE = 12;

    private static final int GLOBAL_MODE_NORMAL = 0;
    private static final int GLOBAL_MODE_PICKUP = 1;
    private int globalMode = GLOBAL_MODE_NORMAL;

    // colors
    private static final int[] COLORS = {
                                        0xffffff, 0xECA624, 0xDFD6B6, 0xECA624, 0xECA624, 0xDFD6B6, 0xffffff,
                                        0xE9DCA7, 0xffff7f, 0xffffff, 0xBCA064, 0xfff000, 0xfffaf5, 0xD8B786
    };
    static final int COLORS_COUNT = 7;
    static int colorsOffset = 0;

    private static final int COLOR_OFFSET_BACKGROUND = 0;
    private static final int COLOR_OFFSET_MAIN_STREET = 1;
    private static final int COLOR_OFFSET_SECONDARY_STREET = 2;
    private static final int COLOR_OFFSET_NAVIGATOR_STREETS = 3;
    private static final int COLOR_OFFSET_MAIN_STREET_2 = 4;
    private static final int COLOR_OFFSET_SECONDARY_STREET_2 = 5;
    private static final int COLOR_OFFSET_BORDER = 6;

    private static final int COLOR_STREET_NAME_HORIZONTAL = 0x003030;
    private static final int COLOR_STREET_NAME_VERTICAL = 0x800080;
    private static final int COLOR_STREET_NAME_SELECTED = 0x00bf00;
    private static final int COLOR_SELECTED = 0x00ff7f;
    private static final int COLOR_DIRECTION = 0x007FFF;
    private static final int COLOR_NO_TRAFFIC = 0xCF0000;
    private static final int COLOR_CURSOR = 0x00C0FF;
    private static final int COLOR_ADDRESS = 0x209780;

    // levels at which street rendering methods are changed.
    private static final int LEVEL_RENDER_ALL_STREETS = 4;
    private static final int LEVEL_RENDER_ALL_STREETS_THIN = 5;
    private static final int LEVEL_RENDER_SECONDARY_STREETS_THIN = 4;
    private static final int LEVEL_RENDER_SECONDARY_STREETS_WITH_BORDER = 2;
    private static final int LEVEL_RENDER_ADDRESSES = 2;

    private final static int LABEL_NORMAL = 0;
    private final static int LABEL_SELECTED = 1;
    private final static int LABEL_CURSOR = 2;

    // fonts
    public FontM fontHorz, fontVert, fontGreen;
    private int mapFontPrev;

    // commands
    public static Command navigatorCommand;
    public static Command mapCommand;
    public static Command zoomInCommand;
    public static Command zoomOutCommand;

    private static Command addMarkCommand;
    private static Command addMarkJamCommand;
    private static Command addMarkCopCommand;

    private Command pickupModePickupCommand;

    boolean hasInfoCommand;

    private static final String REQUEST_MAP_LATITUDE = "map.lat";
    private static final String REQUEST_MAP_LONGITUDE = "map.lon";
    private static final String REQUEST_MARK_TYPE = "obj.type";
    private static final String REQUEST_TYPE_LIST = "obj.typeList";

    private static final String MARK_JAM = "jam";
    private static final String MARK_COP = "cop";
    private static final String MARK_USER = "user";

    static boolean isNavigator; // true if in Navigator mode, false if in map mode

    Navigator navigator;

    /**
     * Create new map object. Constructor is called before all data is loaded.
     */
    public Map () {
        super (Frame.getInstance ());
        instance = this;

        isBarTransparent = true;

        this.parent = msrt.getInstance ();

        navigatorCommand = new Command (msrt.Resources[76], Command.SCREEN, 50);
        mapCommand = parent.mapCommand;
        zoomInCommand = new Command (msrt.Resources[128], Command.SCREEN, 1);
        zoomOutCommand = new Command (msrt.Resources[129], Command.SCREEN, 1);

        addMarkJamCommand = new Command (msrt.Resources[192], Command.SCREEN, 10);
        addMarkCopCommand = new Command (msrt.Resources[193], Command.SCREEN, 11);
        addMarkCommand = new Command (msrt.Resources[194], Command.SCREEN, 12);

        segmentLargeChunk = SEGMENT_FIRST_LARGE_CHUNK;
        segmentSmallChunk = SEGMENT_FIRST_SMALL_CHUNK;
        currentObjectClass = currentObjectItem = 0;
        changeFocus = true;
        srhN = 0;

        isPointerSupported = Frame.getInstance ().hasPointerEvents ();

        int mapFont = parent.config[msrt.CONFIG_MAP_FONT];
        int segs = Integer.parseInt (msrt.profile[msrt.PROFILE_FONTSEGS]);
        int fontParam = parent.config[msrt.CONFIG_UI_FONT];

        int fontType = (mapFont == 0) ? FontM.FONT_SYSTEM : FontM.FONT_SOFTWARE;

        fontHorz = new FontM (segs, COLOR_STREET_NAME_HORIZONTAL, fontType, fontParam);
        if (mapFont == 1) { // i.e. software compact
            fontGreen = fontVert = fontHorz;
        } else {
            fontVert = new FontM (segs, COLOR_STREET_NAME_VERTICAL, fontType, fontParam);
            fontGreen = new FontM (segs, COLOR_ADDRESS, fontType, fontParam);
        }
        mapFontPrev = mapFont;

        // init navigator
        navigator = new Navigator (parent, this);

        // init built-in categories
        DataLoader.allocCtVars (CATEGORY_BUILT_IN_COUNT);

        ctName[CATEGORY_ROOT] = msrt.Resources[161].toCharArray ();
//        ctUid[CATEGORY_ROOT] = CATEGORY_ROOT;
        initCtReferences (CATEGORY_ROOT);

        ctName[CATEGORY_VISIBLE] = msrt.Resources[162].toCharArray ();
//        ctUid[CATEGORY_VISIBLE] = CATEGORY_VISIBLE;
        initCtReferences (CATEGORY_VISIBLE);
        ctIcon[CATEGORY_VISIBLE] = parent.icons[msrt.ICON_MAP];

        ctName[CATEGORY_RESULTS] = msrt.Resources[163].toCharArray ();
//        ctUid[CATEGORY_RESULTS] = CATEGORY_RESULTS;
        initCtReferences (CATEGORY_RESULTS);
        ctIcon[CATEGORY_RESULTS] = parent.icons[msrt.ICON_SEARCH];

        initCommands ();
    }

    private void initCtReferences (int ref) {
        ctLocalToRemote.put (new Integer (ref), new Integer (ref));
        ctRemoteToLocal.put (new Integer (ref), new Integer (ref));
    }

    /**
     * Get instance of Map
     * @return Map
     */
    public static Map getInstance () {
        return instance;
    }

    private void initCommands () {
        removeAllCommands ();

        if (msrt.system == msrt.SYSTEM_WINDOWS || msrt.vendor == msrt.VENDOR_PALM) {
            addCommand (zoomInCommand);
            addCommand (zoomOutCommand);
        }

        if (globalMode == GLOBAL_MODE_NORMAL) {
            setMasterCommand (parent.menuCommand);
            if (!isNavigator) {
                addCommand (navigatorCommand);

                if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
                    addCommand (addMarkJamCommand);
                    addCommand (addMarkCopCommand);
                    addCommand (addMarkCommand);

                    addCommand(parent.subscriberListCommand);
                }

            } else {
                addCommand (mapCommand);
            }
        }
    }

    /**
     * Init the Map. Method is called after all data have been loaded
     * @param ox int
     * @param oy int
     * @param s int
     */
    public void init (int ox, int oy, int s) {
        savedX = originX = ox;
        savedY = originY = oy;
        savedScale = scale = s;

        parent.createLayersForm ();

        if (!hasRoadDirs)
            parent.pathMenu.delete (2);

        if (hasGeoData) {
            int lat = (int) ((cityY / 2 * 100000L) / lat2meters + latitude);
            int lon = (int) ((cityX / 2 * 100000L) / lon2meters + longitude);
            //System.out.println("lat: " + lat + " lon: " + lon);
            SunDial.setPlaceCoordinates (lat, lon, timeZone);
        }

        hasObjects = lbN[SEGMENT_STATIC_POINTS] + lbN[SEGMENT_PRELOADED_POINTS] > 0;

        // INIT VARS
        outx = new short[128];
        outy = new short[128];
        out_el = new byte[elementN + 1];
        out_pc = new byte[pcN];
        tmp = new char[128];
        name_alloc = new char[128];
        curElements = new int[20];
        curCategory = CATEGORY_ROOT;
        visibleObjects = new int[visibleObjectsLimit];
        srhContents = new int[ObjectManager.CONTENTS_LIMIT];

        width = componentWidth;
        height = componentHeight;

        isNavigator = false;

        update = needpaint = true;
    }

    /**
     * Destroy the map
     */
    public void destroy () {
        elconp = elcon = con = conel = pcx = pcy = null;
        pcconp = null;
        stnamep = null;
        blx = bly = blst = blnamep = null;
        stnames = null;
        name = null;
    }

    /**
     * Generate Layers Dialog
     * @return - dialog content
     */
    ChoiceGroup generateLayers () {
        ChoiceGroup layers = new ChoiceGroup (parent.Resources[37], ChoiceGroup.MULTIPLE);

        boolean[] oldLayerFlag = layerFlag;

        layerN = ctN + 6;
        layerRef = new int[layerN];
        layerFlag = new boolean[layerN];

        if (oldLayerFlag != null)
            System.arraycopy (oldLayerFlag, 0, layerFlag, 0, Math.min (oldLayerFlag.length, layerN));

        layers.append (parent.Resources[42], null);
        layerRef[0] = LAYER_HORZ;
        layerFlag[0] = true;
        layers.append (parent.Resources[43], null);
        layerRef[1] = LAYER_VERT;
        layerFlag[1] = true;
        int n = 2;
        if (blN[SEGMENT_STATIC_ADDRESSES] > 0) {
            layers.append (parent.Resources[45], null);
            layerRef[n++] = LAYER_ADDRESSES;
        }
        if (hasRoadDirs) {
            layers.append (msrt.Resources[93], null);
            layerRef[n] = LAYER_ROADDIRS;
            layerFlag[n] = false;
            n++;
        }
        layers.append (msrt.Resources[94], null);
        layerRef[n] = LAYER_GS;
        layerFlag[n] = true;
        n++;
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            layers.append (parent.Resources[115], null);
            layerRef[n++] = LAYER_LOCATOR;
        }

        if (hasGeoData && msrt.features[msrt.FEATURE_SUN]) {
            layers.append (msrt.Resources[114], null);
            layerRef[n] = LAYER_SUN;
            n++;
        }

        for (int i = 0; i < ctN; i++) {
            if (i >= CATEGORY_BUILT_IN_COUNT && ctParent[i] == CATEGORY_ROOT &&
                (ctShow[i] & CATEGORY_SHOW_LIST) > 0) {
                layers.append (ObjectManager.getCategoryName (i), null);
                layerRef[n] = i;
                layerFlag[n] = ctIsStartupVisibility[i];
                ctIsStartupVisibility[i] = false; // make it false to avoid resetting at onlineloader
                n++;
            }
        }

        return layers;
    }

    /**
     * Change layer's state
     * @param layerNumber - layer
     * @param state - new state
     */
    public static void setLayer (int layerNumber, boolean state) {
        for (int i = 0; i < layerN; i++)
            if (layerRef[i] == layerNumber)
                layerFlag[i] = state;
    }

    /**
     * Get layer's state
     * @param layerNumber - layer
     * @return - state
     */
    public static boolean getLayer (int layerNumber) {
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR && layerNumber > 0)
            return true;

        boolean ret = false;
        for (int i = 0; i < layerN; i++)
            if (layerRef[i] == layerNumber)
                ret = layerFlag[i];

        return ret;
    }

    public Hashtable saveLayersState () {
        Hashtable res = new Hashtable (layerN << 1);

        for (int i = 0; i < layerN; i++) {
            int lr = layerRef[i];
            if (lr < 0 ||
                (lr > 0 && ctMode[lr] == CATEGORY_MODE_STATIC)) {
                res.put (new Integer (layerRef[i]), new Boolean (layerFlag[i]));
            }
        }

        return res;
    }

    public void loadLayersState (Hashtable data) {
        Enumeration e = data.keys ();
        while (e.hasMoreElements ()) {
            Integer layer = (Integer) e.nextElement ();
            Boolean state = (Boolean) data.get (layer);

            setLayer (layer.intValue (), state.booleanValue ());

//            System.out.println ("load: " + layer + " - " + state);
        }
    }

    /*
     BLINK
     */
    boolean isBlinkSelected;
    boolean blinkSelectedState;
    boolean isBlinkCursor;
    boolean blinkCursorState;

    int timerCounter = 0;
    long lastAutoupdate = 0;

    /**
     * Dispatch timer event
     */
    public void timer () {
        int autoupdate = msrt.AUTOUPDATE_VALUES[msrt.config[msrt.CONFIG_AUTOUPDATE]];
        if (autoupdate > 0) {
            long now = System.currentTimeMillis ();
            if (now - lastAutoupdate > autoupdate * 1000 * 60) {
                parent.theObjectManager.serverGetMarksInBackground ();
                lastAutoupdate = now;
            }
        }

        if ((timerCounter & 0x1) == 0) {
            boolean doRepaint = false;

            if (currentObject != 0) {
                /*isBlinkSelected = blinkSelectedState;
                                 if (isBlinkSelected)*/
                isBlinkSelected = true;
                doRepaint = true;
            }
//            if (!cursorActionByPointer)
            {
                isBlinkCursor = true;
                doRepaint = true;
            }

            if (doRepaint) {
                needpaint = false;
                repaint ();
            }
        }
        timerCounter++;
    }

    /**
     * Handle component commands
     * @param c Command
     */
    public void commandAction (Command c) {
        if (c == navigatorCommand) {
            setNavigator (true);
        } else if (c == mapCommand) {
            setNavigator (false);
        } else if (c == zoomInCommand) {
            zoomIn ();
        } else if (c == zoomOutCommand) {
            zoomOut ();
        } else if (c == addMarkJamCommand) {
            serverRequestAddMark (MARK_JAM);
        } else if (c == addMarkCopCommand) {
            serverRequestAddMark (MARK_COP);
        } else if (c == addMarkCommand) {
            serverRequestAddMark (null);
        } else {
            super.commandAction (c);
        }
    }

    private void serverRequestAddMark (String markType) {
        OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.METHOD_ADD_MARK, false, false);
        ol.addParameter (REQUEST_MAP_LATITUDE, getCursorLatitude ());
        ol.addParameter (REQUEST_MAP_LONGITUDE, getCursorLongitude ());
        if (markType != null)
            ol.addParameter (REQUEST_MARK_TYPE, markType);
        ol.go ();
    }

    //
    //   KEYBOARD
    //

    /**
     * Dispatch single key pressure
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     */
    public void dispatchSingleKey (int keyCommand, int keyCode, int gameAction) {
        if (isNavigator) {
            if (navigator.dispatchSingleKey (keyCode, gameAction))
                return;
        }

        boolean invalidate = true;
        long timeNow = System.currentTimeMillis ();
        if (timeNow > time + 3000)
            stopMode = false;

        if (stopMode) { // 0+<key> sequence
            stopMode = false;
            if (keyCode == Canvas.KEY_STAR) {
                if (srhN > 0) {
                    curCategory = CATEGORY_RESULTS;
                    parent.theObjectManager.createAndShowObjList (this);
                }
                invalidate = false;
            } else if (keyCode == Canvas.KEY_POUND) {
                showDistance = true;
                update = true;
            }
        } else { // single key
            switch (keyCode) {
                case Canvas.KEY_NUM3:
                    zoomIn ();
                    break;
                case Canvas.KEY_NUM1:
                    zoomOut ();
                    break;
                case Canvas.KEY_NUM5:
                    boolean a = getLayer (LAYER_HORZ);
                    boolean b = getLayer (LAYER_VERT);
                    b = !b;
                    if (b)
                        a = !a;
                    setLayer (LAYER_HORZ, a);
                    setLayer (LAYER_VERT, b);
                    update = true;
                    break;
                case Canvas.KEY_NUM7:
                    if (globalMode == GLOBAL_MODE_NORMAL) {
                        switchOffCursor ();
                        curCategory = CATEGORY_ROOT;
                        parent.theObjectManager.createAndShowObjList (this);
                    }
                    invalidate = false;
                    break;
                case Canvas.KEY_NUM9:
                    switchOffCursor ();
                    setNavigator (true);
                    break;
                case Canvas.KEY_STAR:
                    if (globalMode == GLOBAL_MODE_NORMAL) {
                        switchOffCursor ();
                        parent.theSearchEngine.show ();
                    }
                    invalidate = false;
                    break;
                case Canvas.KEY_NUM0:
                    if (globalMode == GLOBAL_MODE_NORMAL) {
                        stopMode = true;
                        time = timeNow;
                        invalidate = false;
                    }
                    break;
                default:
                    if (keyCommand == Frame.KEY_COMMAND_ENTER) {
                        if (globalMode == GLOBAL_MODE_PICKUP) {
                            if (pickupModePickupCommand != null) {
                                commandAction (pickupModePickupCommand);
                            }
                        } else {
                            if (cursorObject != currentObject) {
                                changeFocus = false;
                                if (cursorObjectItem2 > 0)
                                    parent.theObjectManager.select (cursorObjectItem2 | MASK_ADDRESS);
                                else
                                    parent.theObjectManager.select (cursorObject);
                                selectionChanged ();
//                                switchOffCursor ();
                                update = true;
                            } else {
                                if (hasInfoCommand) {
                                    super.commandAction (parent.infoCommand);
                                } else if (currentObjectClass == CLASS_STREET) {
                                    if (ObjectManager.hasStreetAddresses (currentObjectItem))
                                        super.commandAction (parent.showAddressesCommand);
                                }
                            }
                        }
                    } else {
                        if (gameAction == Canvas.UP || gameAction == Canvas.DOWN ||
                            gameAction == Canvas.LEFT || gameAction == Canvas.RIGHT) {
                            int divider = (keyCode > 48) ? 5 : 3;
                            dispatchArrows (keyCommand, keyCode, gameAction, 1, divider, false);
                        }
                    }
            }
        }
        needpaint = true;
        if (invalidate)
            repaint ();
    }

    /**
     * Dispatch repeated key pressure
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     * @param count int
     * @param acceleration int
     */
    public void dispatchRepeatedKey (int keyCommand, int keyCode, int gameAction, int count, int acceleration) {
        if (isNavigator) {
            if (navigator.dispatchRepeatedKey (keyCode, gameAction, count, acceleration))
                return;
        }

        needpaint = true;
        update = true;
        boolean invalidate = true;
        switch (keyCode) {
            case Canvas.KEY_POUND: /* fall-through */
            case Canvas.KEY_NUM0:
                deselectObject ();
                switchOffCursor ();
                break;
            case Canvas.KEY_NUM7:
                curCategory = CATEGORY_VISIBLE;
                parent.theObjectManager.createAndShowObjList (this);
                invalidate = false;
                break;
            case Canvas.KEY_STAR:
                if (srhN > 0) {
                    curCategory = CATEGORY_RESULTS;
                    parent.theObjectManager.createAndShowObjList (this);
                    invalidate = false;
                }
                break;
            case Canvas.KEY_NUM5:
                if (msrt.BUILD_VERSION == msrt.BUILD_RADAR)
                    parent.theLocator.showLocatorList ();
                else
                    parent.showLayersForm ();
                invalidate = false;
                break;
            case Canvas.KEY_NUM1:
                scale = 10;
                break;
            case Canvas.KEY_NUM3:
                scale = 2;
                break;
            case Canvas.KEY_NUM9:
                System.gc ();
                break;
            default:
                if (gameAction == Canvas.UP || gameAction == Canvas.DOWN ||
                    gameAction == Canvas.LEFT || gameAction == Canvas.RIGHT) {
                    int divider = (keyCode > 48) ? 5 : 3;
                    dispatchArrows (keyCommand, keyCode, gameAction, acceleration, divider, true);
                } else {
                    /*
                     * workaround Motorola bug (pressing 'menu' key generates keyPressed
                     * but doesn't generate keyReleased)
                     */
                    invalidate = false;
                }
        }
        if (invalidate)
            repaint ();
    }

    /**
     * Dispatch release of repeated key.
     * @param keyCommand int
     * @param keyCode int
     * @param gameAction int
     */
    public void dispatchRepeatedKeyRelease (int keyCommand, int keyCode, int gameAction) {
        if (fastupdate) {
            fastupdate = false;
            update = true;
            needpaint = true;
            repaint ();
        }
    }

    private void dispatchArrows (int keyCommand, int keyCode, int gameAction, int acceleration, int divider,
                                 boolean isRepeated) {
        if (keyCode < 48) {
            // cursor is moved by arrows only
            update = false;

            // cursor selects object under it
            moveCursor (gameAction, acceleration);
            pointerClickPoint (cursorScreenX, cursorScreenY);
            isBlinkCursor = false;
        } else {
            update = true;
            if (isRepeated && parent.config[msrt.CONFIG_MAP_DETAILS] <= 1) {
                fastupdate = true;
            }

            switch (gameAction) {
                case Canvas.LEFT:
                    originX -= width * scale * acceleration / divider;
                    if (originX < 0)
                        originX = 0;
                    break;
                case Canvas.RIGHT:
                    originX += width * scale * acceleration / divider;
                    if (originX > cityX)
                        originX = cityX;
                    break;
                case Canvas.DOWN:
                    originY -= height * scale * acceleration / divider;
                    if (originY < 0)
                        originY = 0;
                    break;
                case Canvas.UP:
                    originY += height * scale * acceleration / divider;
                    if (originY > cityY)
                        originY = cityY;
                    break;
            }

            cursorObject = cursorObjectClass = cursorObjectItem = 0; // switch off cursor selection
        }
        cursorActionByPointer = false;
    }

    private void moveCursor (int action, int acceleration) {
        int step = CURSOR_STEP * acceleration;
        switch (action) {
            case Canvas.LEFT:
                cursorScreenX -= step;
                if (cursorScreenX < 0) {
                    moveFocusRelative (( -cursorScreenX - width / 4) * scale, 0);
                    cursorScreenX = width / 4;
                }
                break;
            case Canvas.RIGHT:
                cursorScreenX += step;
                if (cursorScreenX > width - CURSOR_WIDTH) {
                    moveFocusRelative (((cursorScreenX - width) + width / 4) * scale, 0);
                    cursorScreenX = (width * 3) / 4;
                }
                break;
            case Canvas.UP:
                cursorScreenY -= step;
                if (cursorScreenY < 0) {
                    moveFocusRelative (0, (height / 4 - cursorScreenY) * scale);
                    cursorScreenY = height / 4;
                }
                break;
            case Canvas.DOWN:
                cursorScreenY += step;
                if (cursorScreenY > height - CURSOR_HEIGHT) {
                    moveFocusRelative (0, -((cursorScreenY - height) + height / 4) * scale);
                    cursorScreenY = (height * 3) / 4;
                }
                break;
        }
        cursorObject = cursorObjectClass = cursorObjectItem = 0; // switch off cursor selection
    }

    private void validateCursor () {
        if (cursorX > xmin && cursorX < xmax && cursorY > ymin && cursorY < ymax) {
            cursorScreenX = mx2 (cursorX);
            cursorScreenY = my2 (cursorY);
            if (cursorScreenX <= 0 || cursorScreenX >= width || cursorScreenY <= 0 || cursorScreenY >= height) {
                cursorScreenX = width >> 1;
                cursorScreenY = height >> 1;
            }
        } else {
            cursorScreenX = width >> 1;
            cursorScreenY = height >> 1;
        }
    }

    private void switchOffCursor () {
        cursorObject = cursorObjectClass = cursorObjectItem = cursorObjectItem2 = 0;
    }

    /**
     * Deselect object
     */
    public void deselectObject () {
        currentObject = currentObjectClass = currentObjectItem = 0;
        PathFinder.clear ();
        selectionChanged ();
    }


    //
    //  POINTER COMMANDS
    //
    int pointerLastX, pointerLastY;
    private static final long POINTER_LONG_PRESSURE = 1000;
    long pointerPressedTime;

    /**
     * Pointer is pressed handler
     * @param x int
     * @param y int
     */
    public void pointerPressed (int x, int y) {
        pointerLastX = x;
        pointerLastY = y;
        pointerPressedTime = System.currentTimeMillis ();
    }

    /**
     * Pointer is released handler
     * @param x int
     * @param y int
     */
    public void pointerReleased (int x, int y) {
        if (isNavigator) {
            navigator.pointerPressed (x, y);
            return;
        }

        cursorActionByPointer = true;
        update = false;
        boolean invalidate = true;
        if (x > pointerButtonX && y >= pointerButtonY && y <= pointerButtonY + POINTER_BUTTON_SIZE) {
            pointerSelects = !pointerSelects;
            switchOffCursor ();
        } else {
            int cursorObjectStored = cursorObject;
            switchOffCursor ();

            if (pointerSelects) {
                pointerClickPoint (x, y);
                cursorScreenX = x;
                cursorScreenY = y;
            } else {
                // pointer was dragged
                if (Math.abs (x - pointerLastX) + Math.abs (y - pointerLastY) > 8) {
                    // zoom
                    if (pointerLastX + y - x - pointerLastY > 0)
                        zoomOut ();
                    else
                        zoomIn ();
                }
                // pointer is clicked
                else {
                    if (System.currentTimeMillis () - pointerPressedTime >
                        POINTER_LONG_PRESSURE && currentObjectClass > 0) {
                        // deselect
                        deselectObject ();
                        switchOffCursor ();
                    } else {
                        // move
                        int nx = (x - widthHalf) * scale + originX;
                        int ny = (heightHalf - y) * scale + originY;

                        if (nx > 0 && nx < cityX)
                            originX = nx;
                        if (ny > 0 && ny < cityY)
                            originY = ny;
                    }
                }
                update = true;
            }
            if (cursorObject > 0 && cursorObject == cursorObjectStored) {
                changeFocus = false;
                parent.theObjectManager.select (cursorObject);
                update = true;
                switchOffCursor ();
            }
        }
        if (invalidate) {
            needpaint = true;
            repaint ();
        }
    }

    /**
     * Find and select object under cursor
     * @param x int
     * @param y int
     */
    private void pointerClickPoint (int x, int y) {
        if (!pointerClickObject (x, y))
            if (!pointerClickPc (x, y))
                pointerClickStreet (x, y);
    }

    /**
     * Check if pointer is above crossroad
     * @param x int
     * @param y int
     * @return boolean
     */
    private boolean pointerClickPc (int x, int y) {
        boolean res = false;
        if (msrt.features[msrt.FEATURE_ROUTING]) {
            int xr = mx2r (x), yr = my2r (y);
            int r = 5 * scale;

            int diffY = yr;
            for (int i = outpcMinIndex; i < pcN; i++) {
                if (out_pc[i] != 2) {
                    continue;
                }

                diffY = pcy[i] - yr;
                if (diffY < -r) {
                    continue;
                }
                if (diffY > r) {
                    // crossroads are ordered by Y coordinate, if diffY is greater than radius we can leave iteartion
                    break;
                }

                if (Math.abs (pcx[i] - xr) < r) {
                    Map.cursorObjectClass = Map.CLASS_PC;
                    Map.cursorObjectItem = i;
                    Map.cursorObject = Map.MASK_PC | i;
                    Map.cursorX = Map.pcx[i];
                    Map.cursorY = Map.pcy[i];
                    res = true;
                    break;
                }
            }
        }
        return res;
    }

    /**
     * Check if pointer is above street
     * @param x int
     * @param y int
     * @return boolean
     */
    private boolean pointerClickStreet (int x, int y) {
        boolean res = false;
        int threshold = 5 * scale + 10;
        int r[] = PathFinder.castXYTo2Pcs (mx2r (x), my2r (y), Map.out_pc, threshold, outpcMinIndex);
        if (r[6] < threshold && r[5] < elementN) {
            int st = el2st[r[5]];

//            System.out.print ("ST: " + st + "  " );
//            for (int i=0; i < r.length; i++)
//                System.out.print (" r[" + i + "]=" + r[i]);
//            System.out.println ("");

            Map.cursorObjectClass = Map.CLASS_STREET;
            Map.cursorObjectItem = st;
            Map.cursorObject = Map.MASK_STREET | st;
            res = true;
        }
        return res;
    }

    /**
     * Check if pointer is above object (point)
     * @param x int
     * @param y int
     * @return boolean
     */
    private boolean pointerClickObject (int x, int y) {
        boolean res = false;
        for (int i = 0; i < Map.visibleObjectsN; i++) {
            int v = Map.visibleObjects[i];
            int vi = v & 0xffffff;
            if ((v >> 24) == Map.CLASS_LABEL) {
                int ax = Map.lbx[vi >> 16][vi & 0xffff],
                         ay = Map.lby[vi >> 16][vi & 0xffff];

                if (Math.abs (mx2 (ax) - x)
                    + Math.abs (my2 (ay) - y) < 6) {
                    Map.cursorObjectClass = Map.CLASS_LABEL;
                    Map.cursorObjectItem = vi;
                    Map.cursorObject = Map.MASK_LABEL | vi;
                    Map.cursorX = ax;
                    Map.cursorY = ay;
                    res = true;
                    break;
                }
            }
        }
        return res;
    }

    //
    // NAVIGATION COMMANDS
    //

    /**
     * Show the whole city map
     */
    public void showWholeCity () {
        originX = cityX >> 1;
        originY = cityY >> 1;
        int sx = cityX / width, sy = cityY / height;
        scale = (sx > sy) ? sx : sy;
        if (scale > 70)
            scale = 70;
    }

    /**
     * Zoom In
     */
    static void zoomIn () {
        if (scale > 1) {
            update = true;
            needpaint = true;
            int oldScale = scale;
            if (scale < 8)
                scale--;
            else
                scale = (scale * 2) / 3;

            if (!isNavigator) {
                originX = mx2r (((width / 2 - cursorScreenX) * scale) / oldScale + cursorScreenX);
                originY = my2r (((height / 2 - cursorScreenY) * scale) / oldScale + cursorScreenY);
            }
        }
    }

    /**
     * Zoom Out
     */
    static void zoomOut () {
        update = true;
        needpaint = true;
        int oldScale = scale;
        if (scale < 8)
            scale++;
        else if (scale < 70)
            scale = (scale * 3) / 2;

        if (!isNavigator) {
            originX = mx2r (((width / 2 - cursorScreenX) * scale) / oldScale + cursorScreenX);
            originY = my2r (((height / 2 - cursorScreenY) * scale) / oldScale + cursorScreenY);
        }
    }

    //
    //   SHOW / HIDE MAP
    //
    /**
     * Map component becomes visible
     */
    public void showNotify () {
        if (isNavigator) {
            navigator.showNotify ();
            return;
        }

        needpaint = true;
        update = true;

        // update fonts (if config was changed)
        try {
            int mapFont = parent.config[msrt.CONFIG_MAP_FONT];
            int fontParam = parent.config[msrt.CONFIG_UI_FONT];
            int fontType = (mapFont == 0) ? FontM.FONT_SYSTEM : FontM.FONT_SOFTWARE;

            if (mapFontPrev == 1 && mapFont != 1) { // create software fonts
                int segs = Integer.parseInt (msrt.profile[msrt.PROFILE_FONTSEGS]);
                fontVert = new FontM (segs, COLOR_STREET_NAME_VERTICAL, fontType, fontParam);
                fontGreen = new FontM (segs, COLOR_ADDRESS, fontType, fontParam);
            } else if (mapFont == 1) {
                fontGreen = fontVert = fontHorz;
            }

            fontHorz.changeType (fontType, fontParam);
            fontVert.changeType (fontType, fontParam);
            fontGreen.changeType (fontType, fontParam);

            mapFontPrev = mapFont;
        } catch (OutOfMemoryError err) {
            parent.config[msrt.CONFIG_MAP_FONT] = 0;
            fontHorz.changeType (FontM.FONT_SYSTEM, 0);
            fontVert.changeType (FontM.FONT_SYSTEM, 0);
            fontGreen.changeType (FontM.FONT_SYSTEM, 0);
        }

        timeWhenBecameVisible = System.currentTimeMillis ();

        // avoid corruption of data stored in RMS
        if (scale == 0) {
            scale = 10;
        }
        if (originX < 0 || originX > cityX) {
            originX = cityX / 2;
        }
        if (originY < 0 || originY > cityY) {
            originY = cityY / 2;
        }

        validateCursor ();
    }

    /**
     * Map component becomed hidden
     */
    public void hideNotify () {
        switchOffCursor ();
        parent.statistics[msrt.STATISTICS_MAP_TIME] +=
            (System.currentTimeMillis () - timeWhenBecameVisible) / 1000;
    }

    //
    //   PAINT
    //
    /**
     * Repaint map component
     * @param g Graphics
     */
    public void paint (Graphics g) {
        width = componentWidth;
        height = componentHeight;
        widthHalf = width >> 1;
        heightHalf = height >> 1;

//        msrt.DEBUG_LOG += "\nrepaint " + needpaint + "," + update;

        if (isNavigator) {
            navigator.paint (g, getScreenBufferGraphics ());
            return;
        }

        g.setFont (getFont ());

        if (isBlinkSelected) {
            blinkSelectedState = !blinkSelectedState;
            g.setClip (0, clipYLo, width, clipYHi); // for the case when cursor is on but selected object is still inversed
            originX -= widthHalf * scale;
            originY -= heightHalf * scale;
            drawSelectedObject (g);

            // direction triangle
            if (currentObjectClass == CLASS_LABEL ||
                (currentObjectClass == CLASS_STREET && !isCurrentStreetVisible) ||
                currentObjectClass == CLASS_PC || currentObjectClass == CLASS_ADDRESS)
                drawArrow (g, selectionX, selectionY, blinkSelectedState, null, false);

            originX += widthHalf * scale;
            originY += heightHalf * scale;
            g.setClip (0, 0, width, height);

            isBlinkSelected = false;
        }
        if (isBlinkCursor) {
            blinkCursorState = !blinkCursorState;
            originX -= widthHalf * scale;
            originY -= heightHalf * scale;
            drawCursor (g);
            originX += widthHalf * scale;
            originY += heightHalf * scale;
            drawPointerButton (g);
            isBlinkCursor = false;
        }

        if (needpaint) {
            isRepainting = true;

            Graphics bufferGraphics = getScreenBufferGraphics ();

            if (update) {
                blinkSelectedState = false;
                draw (bufferGraphics);
            }

            paintScreenBuffer (g);

            originX -= widthHalf * scale;
            originY -= heightHalf * scale;

            clipYLo = 0;
            clipYHi = height;

            // draw cursor
            blinkCursorState = false;
            drawCursor (g);

            // draw scale indicator
            if (clipYLo == 0) {
                int step;
                String sc;
                if (scale < 7) {
                    step = 100 / globalScale / scale;
                    sc = "100 m";
                } else {
                    step = 1000 / globalScale / scale;
                    sc = "1 km";
                }
                g.setColor (0x000060);
                g.fillRect (0, clipYLo + 1, step + 2, 5);
                fontHorz.drawString (g, sc, step + 5, clipYLo + 7);
                g.setColor (0xFFF000);
                g.fillRect (1, clipYLo + 2, step, 3);
            }
            originX += widthHalf * scale;
            originY += heightHalf * scale;

            // if we have pointer support draw button to switch b/w navigation and selection
            drawPointerButton (g);
            isRepainting = false;
            update = false;
        }
        needpaint = true;
    }

    private void drawPointerButton (Graphics g) {
        if (isPointerSupported && (visibleCrossroadsN + visibleStreetsN + visibleLabelsN > 0)) {
            pointerButtonX = width - POINTER_BUTTON_SIZE;
            pointerButtonY = 0;

            g.translate (pointerButtonX, pointerButtonY);
            g.setColor (pointerSelects ? (0xbf << (cursorMode << 3)) : 0xffffff);
            g.fillRect (0, 0, POINTER_BUTTON_SIZE, POINTER_BUTTON_SIZE);
            g.setColor (pointerSelects ? 0xffffff : 0x000000);
            g.drawRect (0, 0, POINTER_BUTTON_SIZE, POINTER_BUTTON_SIZE);
            g.fillTriangle (1, 7, POINTER_BUTTON_SIZE - 1, 1, POINTER_BUTTON_SIZE - 5, POINTER_BUTTON_SIZE - 1);
            g.translate ( -pointerButtonX, -pointerButtonY);
        }
    }

    private void drawCursor (Graphics g) {
        int fontHeight = getFont ().getHeight ();
        String cursorText = null;
        int cursorTextY = 0;
        int cursorColor = blinkCursorState ? (COLOR_CURSOR ^ 0xffffff) : COLOR_CURSOR;
        g.setColor (cursorColor);

        switch (cursorObjectClass) {
            case CLASS_PC:
                int py = my (cursorY);
                int px = mx (cursorX);
                int r1 = scale < 3 ? 4 : 3;
                int r2 = r1 + 1;
                if (!blinkCursorState) {
                    g.drawArc (px - r1, py - r1, r1 << 1, r1 << 1, 0, 360);
                    g.drawArc (px - r2, py - r2, r2 << 1, r2 << 1, 0, 360);
                }
                cursorTextY = py > heightHalf ? 0 : height - fontHeight;
                cursorText = ObjectManager.getPcName (cursorObjectItem);
                if (cursorText == msrt.Resources[92]) {
                    cursorText = null; // don't print unnamed crossroads
                }
                break;
            case CLASS_STREET:
                boolean isHidden = true;
                for (int el = 0; el < elementN; el++)
                    if (el2st[el] == cursorObjectItem) {
                        int aconp = elconp[el], nconp = elconp[el + 1];

                        for (int j = aconp; j < nconp; j++) {
                            short pn = elcon[j];

                            short x = pcx[pn], y = pcy[pn];

                            if (j + 1 < nconp) {
                                short pf = elcon[j + 1];
                                int nx = pcx[pf], ny = pcy[pf];
                                if (!((x > xmax && nx > xmax) || (x < xmin && nx < xmin) ||
                                      (y > ymax && ny > ymax) || (y < ymin && ny < ymin))) {
                                    int yc = my (y);
                                    if (!blinkCursorState) {
                                        g.drawLine (mx (x), yc, mx (nx), my (ny));
                                    }
                                    if (yc > fontHeight)
                                        isHidden = false;
                                }
                            }
                        }
                    }
                drawAddresses (g, cursorObjectItem);
                cursorTextY = isHidden ? height - fontHeight : 0;
                cursorText = ObjectManager.getStreetName (cursorObjectItem);
                if (Map.cursorObjectItem2 > 0) {
                    drawOneAddress (g, cursorObjectItem, cursorObjectItem2 >> 16,
                                    cursorObjectItem2 & 0xffff, true);
                    cursorText = ObjectManager.getAddressName (cursorObjectItem2 >> 16,
                        cursorObjectItem2 & 0xffff);
                    py = my (cursorY);
                    cursorTextY = py > heightHalf ? 0 : height - fontHeight;
                }
                break;
            case CLASS_LABEL:
                drawOneLabel (g, cursorObjectItem >> 16, cursorObjectItem & 0xffff, LABEL_CURSOR, false);
                cursorText = ObjectManager.getLabelFullName (cursorObjectItem >> 16, cursorObjectItem & 0xffff);
                py = my (cursorY);
                cursorTextY = py > heightHalf ? 0 : height - fontHeight;
                break;
        }

        // draw real cursor
        g.translate (cursorScreenX, cursorScreenY);
        // body
        g.setColor (blinkCursorState ? 0xFFFFFF : 0xFFFFCF);
        g.drawLine (1, 1, 1, 7);
        g.drawLine (2, 2, 2, 6);
        g.drawLine (3, 3, 3, 5);
        g.drawLine (4, 4, 4, 5);
        // contour
        g.setColor (0x000000);
        g.drawLine (0, 0, 6, 6);
        g.drawLine (0, 9, 0, 0);
        g.drawLine (6, 6, 3, 6);
        g.drawLine (3, 6, 0, 9);

        g.translate ( -cursorScreenX, -cursorScreenY);

        // print info
        if (cursorText != null) {
            drawCursorText (g, cursorText, cursorTextY);
        }
    }

    /**
     * Draw text for cursor
     * @param g Graphics
     * @param cursorText String
     * @param cursorTextY int
     */
    private void drawCursorText (Graphics g, String cursorText, int cursorTextY) {
        cursorTextY = 0;
        int fontHeight = getFont ().getHeight ();
        g.setColor (0xFFFFFF);
        g.fillRect (0, cursorTextY, width, fontHeight);
        g.setColor (0x000000);
        if (cursorTextY == 0) {
            g.drawLine (0, fontHeight, width, fontHeight);
        } else {
            g.drawLine (0, cursorTextY, width, cursorTextY);
        }
        g.setColor (cursorObjectClass > 0 ? 0x007F00 : 0x7F0000);
        g.drawString (cursorText, 1, cursorTextY, Graphics.TOP | Graphics.LEFT);

        if (cursorTextY == 0) {
            clipYLo = fontHeight;
            clipYHi = height;
        } else {
            clipYLo = 0;
            clipYHi = height - fontHeight;
        }
    }

    /*
     DRAW MAP
     */
    boolean isCurrentStreetVisible = false;
    private void draw (Graphics g) {
        visibleObjectsN = 0;
        visibleLabelsN = 0;
        visibleStreetsN = 0;
        visibleCrossroadsN = 0;
        isCurrentStreetVisible = false;

        g.setColor (COLORS[colorsOffset + COLOR_OFFSET_BACKGROUND]);
        g.fillRect (0, 0, width, height);

        if (getLayer (LAYER_SUN) && msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP) {
            SunDial.drawBackground (g, COLORS[colorsOffset + COLOR_OFFSET_BACKGROUND]);
        }

        g.setFont (getFont ());

        originX -= widthHalf * scale;
        originY -= heightHalf * scale;

        xmin = originX;
        ymin = originY;
        if (ymin < 0) {
            ymin = 0;
        }
        xmax = width * scale + originX;
        ymax = height * scale + originY;

        // draw graphic layer
        if (getLayer (LAYER_GS)) {
            if (scale < 30) {
                drawGS (g, gs[0], gsp[0], gspN[0]);
            } else {
                drawGS (g, gs[1], gsp[1], gspN[1]);
            }

            // gsx
            if (scale < 3 && gs[2] != null) {
                drawGS (g, gs[2], gsp[2], gspN[2]);
            }
        }

        // clear lists of visible objects
        for (int i = 0; i < elementN; i++)
            out_el[i] = 0;

        for (int i = 0; i < pcN; i++)
            out_pc[i] = 0;

        outpcMinIndex = pcN;

        // draw map internal
        if (scale <= LEVEL_RENDER_ALL_STREETS_THIN && msrt.CONFIG_MAP_DETAILS > 0) {
            drawStreets (g, false, 1, true, true); // border
            drawStreets (g, false, 2, false, true); // secondary
            drawStreets (g, false, 2, true, false); // main
        } else
            drawStreets (g, false, 2, true, true); // all

        // draw selected street and find the distance to it
        if (currentObjectClass == CLASS_STREET) {
            selectionX = 0;
            selectionY = 0;
            int dist = 0x7fff;
            int centerx = (xmin + xmax) >> 1, centery = (ymin + ymax) >> 1;

            for (int i = 0; i < curElementsN; i++) {
                int el = curElements[i];
                int aconp = elconp[el], nconp = elconp[el + 1];
                g.setColor (COLOR_SELECTED);

                for (int j = aconp; j < nconp; j++) {
                    short pn = elcon[j];

                    short x = pcx[pn], y = pcy[pn];
                    int d = Util.hypot2 (x - centerx, y - centery);
                    if (d < dist) {
                        dist = d;
                        selectionX = x;
                        selectionY = y;
                    }

                    if (j + 1 < nconp) {
                        short pf = elcon[j + 1];
                        int nx = pcx[pf], ny = pcy[pf];
                        if ((x < xmax && x > xmin && y < ymax && y > ymin) ||
                            (nx < xmax && nx > xmin && ny < ymax && ny > ymin)) {
                            g.drawLine (mx (x), my (y),
                                        mx (nx), my (ny));
                            isCurrentStreetVisible = true;
                        }
                    }
                }
            }
        }

        // print street names
        if (!fastupdate) {
            for (int i = 1; i < elementN; i++) {
                if (out_el[i] > 0) {
                    drawElementName (g, i);
                }
            }
        }

        // draw addresses
        if (!fastupdate) {
            int streetid = -1;
            if (currentObjectClass == CLASS_STREET) {
                streetid = currentObjectItem;
            } else if (currentObjectClass == CLASS_ADDRESS) {
                streetid = blst[currentObjectItem >> 16][currentObjectItem & 0xffff];
            }
            drawAddresses (g, streetid);
        }

        drawLabels (g);

        /* draw mobile objects */
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR)
            if (getLayer (LAYER_LOCATOR))
                parent.theLocator.draw (g, true);

        /* draw the way */
        PathFinder.drawWay (g);

        /* draw selected object */
        drawSelectedObject (g);

        // direction triangle
        if (currentObjectClass == CLASS_LABEL ||
            (currentObjectClass == CLASS_STREET && !isCurrentStreetVisible) ||
            currentObjectClass == CLASS_PC || currentObjectClass == CLASS_ADDRESS)
            drawArrow (g, selectionX, selectionY, true, null, false);

        // add to visible list
        for (int i = 1; i < elementN && visibleObjectsN < visibleObjectsLimit; i++) {
            if (out_el[i] < 0 || (out_el[i] > 0 && scale < 7)) {
                int st = el2st[i] | MASK_STREET;
                for (int j = 0; j < visibleObjectsN; j++)
                    if (visibleObjects[j] == st) {
                        st = 0;
                        break;
                    }

                if (st > 0)
                    visibleObjects[visibleObjectsN++] = st;
            }
        }

        if (getLayer (LAYER_SUN) && msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP)
            SunDial.draw (this, g);

        originX += widthHalf * scale;
        originY += heightHalf * scale;

        showDistance = false;
    }

    void drawSelectedObject (Graphics g) {
        switch (currentObjectClass) {
            case CLASS_PC:
                g.setColor (blinkSelectedState ? 0xCC00CC : 0x00CC00);
                int x = pcx[currentObjectItem], y = pcy[currentObjectItem];
                int py = my (y);
                int r = (scale < 3) ? 4 : 3;
                g.fillArc (mx (x) - r, py - r, r << 1, r << 1, 0, 360);
                break;
            case CLASS_STREET:
                drawSelectedStreet (g);
                break;
            case CLASS_LABEL:
                drawOneLabel (g, currentObjectItem >> 16, currentObjectItem & 0xffff, LABEL_SELECTED, false);
                break;
            case CLASS_ADDRESS:
                int sg = currentObjectItem >> 16, it = currentObjectItem & 0xffff;
                drawOneAddress (g, blst[sg][it], currentObjectItem >> 16,
                                currentObjectItem & 0xffff, false);
                break;
        }
    }

    void drawSelectedStreet (Graphics g) {
        for (int i = 0; i < curElementsN; i++) {
            int el = curElements[i];
            int aconp = elconp[el], nconp = elconp[el + 1];
            g.setColor (blinkSelectedState ? (COLOR_SELECTED ^ 0xffffff) :
                        COLOR_SELECTED);

            for (int j = aconp; j < nconp; j++) {
                short pn = elcon[j];
                short x = pcx[pn], y = pcy[pn];

                if (j + 1 < nconp) {
                    short pf = elcon[j + 1];
                    g.drawLine (mx (x), my (y), mx (pcx[pf]), my (pcy[pf]));
                }
            }
        }
    }

    /**
     * Draw streets layer.
     * @param g Graphics
     * @param isNavigator boolean true, if in navigator mode
     * @param pass int 1 - border, 2 - body
     * @param showMain boolean true, if only main streets are rendered
     * @param showSecondary boolean true, if only secondary streets are rendered
     */
    void drawStreets (Graphics g, boolean isNavigator, int pass, boolean showMain, boolean showSecondary) {
        final int executionTimeLimit = 2500;
        long limitedTime = System.currentTimeMillis () + executionTimeLimit;

        final int details = parent.config[msrt.CONFIG_MAP_DETAILS];
        final boolean showRoadDirs = getLayer (LAYER_ROADDIRS) && (scale < 4);

        final int ymaxPlus1000 = ymax + 1000;
        final int lenLimit = ((scale - details) >> (details + 1)) - 1; // element length limit
        final int thinLineMainColor = isNavigator ?
                                      COLORS[colorsOffset + COLOR_OFFSET_NAVIGATOR_STREETS] :
                                      COLORS[colorsOffset + COLOR_OFFSET_MAIN_STREET_2];
        final int thinLineSecondaryColor = isNavigator ?
                                           COLORS[colorsOffset + COLOR_OFFSET_NAVIGATOR_STREETS] :
                                           COLORS[colorsOffset + (scale >= LEVEL_RENDER_ALL_STREETS_THIN ?
            COLOR_OFFSET_SECONDARY_STREET_2 : COLOR_OFFSET_SECONDARY_STREET)];
        final int borderColor = COLORS[colorsOffset + COLOR_OFFSET_BORDER];
        final int bodyMainColor = COLORS[colorsOffset + COLOR_OFFSET_MAIN_STREET];
        final int bodySecondaryColor = COLORS[colorsOffset + COLOR_OFFSET_SECONDARY_STREET];

        for (int i = pcseg[ymin >> 8]; i < pcN; i++) {
            if (isNavigator && System.currentTimeMillis () > limitedTime) {
                break; // limit repaint time
            }

            int ay = pcy[i];

            if (ay < ymin) {
                continue;
            }
            if (ay > ymaxPlus1000) {
                break;
            }

            int ax = pcx[i];
            int aconp = pcconp[i];
            int aconpnext = pcconp[i + 1];

            // start of segment
            int x1 = (ax - originX) / scale;
            int y1 = height - (ay - originY) / scale;

            int aconpcount = aconpnext - aconp;
            boolean isPcVisible = false;

            for (int j = aconp; j < aconpnext; j++) {
                int np = con[j];
                boolean isMain = np < 0;

                if ((isMain && !showMain) || (!isMain && !showSecondary)) {
                    continue;
                }

                np &= 0x7fff; // clear 'quality' bit

                int nx = pcx[np];
                int ny = pcy[np];

                if (ny > ymax || (ax > xmax && nx > xmax) || (ax < xmin && nx < xmin)) {
                    continue; // end is invisible
                }

                int cl = conel[j];
                int direction = condir[j] & 0x3;

                if (isMain || scale <= LEVEL_RENDER_ALL_STREETS || ellen[cl] > lenLimit) {
                    out_el[cl]++;
                    visibleStreetsN++;

                    int x2 = (nx - originX) / scale;
                    int y2 = height - (ny - originY) / scale;

                    if (scale >= LEVEL_RENDER_ALL_STREETS_THIN ||
                        (scale >= LEVEL_RENDER_SECONDARY_STREETS_THIN && !isMain) ||
                        details == 0 || fastupdate) {
                        // thin line
                        g.setColor (isMain ? thinLineMainColor : thinLineSecondaryColor);
                        g.drawLine (x1, y1, x2, y2);
                    } else {
                        /**
                         * Draw thick lines with outline.
                         * This done in two passes: outlines are drawn in the first one
                         * and fills are drawn in the second.
                         */
                        int dx = x2 - x1, dy = y2 - y1;
                        int ln = Util.hypot2 (dx, dy);
                        if (ln < 2)
                            continue;

                        //int w = (isMain?5: 3)/scale;
                        int w = 4 - scale;
                        if (isMain)
                            w += 1;
                        int sx = (w * dy) / ln, sy = (w * dx) / ln;
                        int w2 = w + 1;
                        int sx2 = (w2 * dy) / ln, sy2 = (w2 * dx) / ln;

                        if (pass == 1 &&
                            (isMain || scale <= LEVEL_RENDER_SECONDARY_STREETS_WITH_BORDER)) {
                            // border
                            g.setColor (borderColor);
                            g.fillTriangle (x1 + sx2, y1 - sy2, x1 - sx2, y1 + sy2, x2 + sx2, y2 - sy2);
                            g.fillTriangle (x2 - sx2, y2 + sy2, x1 - sx2, y1 + sy2, x2 + sx2, y2 - sy2);

                            if (aconpcount > 1) {
                                int wm2 = w2 * 2;
                                g.fillArc (x2 - w2, y2 - w2, wm2, wm2, 0, 360);
                            }
                        } else if (pass == 2) {
                            // body
                            g.setColor (isMain ? bodyMainColor : bodySecondaryColor);
                            g.fillTriangle (x1 + sx, y1 - sy, x1 - sx, y1 + sy, x2 + sx, y2 - sy);
                            g.fillTriangle (x2 - sx, y2 + sy, x1 - sx, y1 + sy, x2 + sx, y2 - sy);

                            if (aconpcount > 1) {
                                int wm2 = w * 2;
                                g.fillArc (x2 - w, y2 - w, wm2, wm2, 0, 360);
                            }
                        }
                    }

                    isPcVisible = true;

                    if (nx > xmin && nx < xmax && ny > ymin) {
                        out_pc[np] = 2;
                        if (np < outpcMinIndex) {
                            outpcMinIndex = np;
                        }
                    }

                    /**
                     * Draw street direction.
                     */
                    if (showRoadDirs && direction > 0) {
                        int dx = x2 - x1, dy = y2 - y1;
                        int ln = Util.hypot2 (dx, dy);

                        if (ln > 15) {
                            int arrowLength = (direction & 0x2) == 0x2 ? -7 : 7;

                            int sx = (dx * arrowLength) / ln,
                                     sy = (dy * arrowLength) / ln;
                            int sx3 = sx / 3, sy3 = sy / 3;
                            int mx = ((x1 + x2) >> 1) - sx3, my = ((y1 + y2) >> 1) - sy3;

                            int xa1 = mx - sy, ya1 = my + sx;
                            int xa2 = xa1 + sx, ya2 = ya1 + sy;

                            g.setColor ((direction == 0x3) ? COLOR_NO_TRAFFIC : COLOR_DIRECTION);

                            g.drawLine (xa1, ya1, xa2, ya2);
                            if (direction != 0x3) { // arrow wings
                                g.drawLine (xa2, ya2, xa2 - sx3 + sy3,
                                            ya2 - sy3 - sx3);
                                g.drawLine (xa2, ya2, xa2 - sx3 - sy3,
                                            ya2 - sy3 + sx3);
                            }
                        }
                    }
                }
            }
            if (isPcVisible && (ax > xmin && ax < xmax && ay > ymin && ay < ymax)) {
                out_pc[i] = 2;
                visibleCrossroadsN++;
                if (i < outpcMinIndex) {
                    outpcMinIndex = i;
                }
            }
        }
    }

    /**
     * Draw arrow that shows direction to the object
     * @param g graphics
     * @param pointX object's x
     * @param pointY object's y
     * @param state boolean blink state
     * @param note words near arrow
     * @param forceNoDistance true, if there can't be distance information
     */
    void drawArrow (Graphics g, int pointX, int pointY, boolean state, char note[], boolean forceNoDistance) {
        int xmid = mx (pointX),
                   ymid = my (pointY);

        if (xmid < 0 || xmid > width || ymid < 0 || ymid > height) {
            Util.ClipLine (xmid, ymid, widthHalf, heightHalf, 1, 1, width - 1, height - 1);
            int x1 = Util.clipx1, y1 = Util.clipy1;

            Util.ClipLine (xmid, ymid, widthHalf, heightHalf, 6, 6,
                           width - 6, height - 6);
            int x2 = Util.clipx1, y2 = Util.clipy1;
            int dx = x1 - x2, dy = y1 - y2;

            g.setColor (0xFFFFFF);
            g.fillTriangle (x2 - dy, y2 + dx, x2 + dy, y2 - dx, x1, y1);

            int color = state ? COLOR_SELECTED : (COLOR_SELECTED ^ 0xFFFFFF);
            g.setColor (color);
            g.drawLine (x2 - dy, y2 + dx, x2 + dy, y2 - dx);
            g.drawLine (x2 - dy, y2 + dx, x1, y1);
            g.drawLine (x1, y1, x2 + dy, y2 - dx);

            if (showDistance || note != null) {
                int off = 0, len = 0;

                if (showDistance && !forceNoDistance) {
                    Util.distance (x1 * scale + originX - pointX,
                                   (height - y1) * scale + originY - pointY, name_alloc);
                    off = Util.offset;
                    len = Util.length;
                }
                if (note != null) {
                    name_alloc[off + len] = ' ';
                    System.arraycopy (note, 0, name_alloc, off + len + 1,
                                      note.length);
                    len += note.length + 1;
                }

                int w = getFont ().charsWidth (name_alloc, off, len);
                int h = getFont ().getHeight ();
                g.setColor (0xffffff);
                g.fillRect (x2 - ((dx > 0) ? w : 0), y2 - ((dy > 0) ? h : 0), w, h);
                g.setColor (0x00CC00);
                g.drawChars (name_alloc, off, len, x2, y2 - 1,
                             ((dy > 0) ? Graphics.BASELINE : Graphics.TOP) |
                             ((dx > 0) ? Graphics.RIGHT : Graphics.LEFT));
            }
        }
    }

    /**
     * Draw addresses
     * @param g graphics
     * @param street selected street
     */
    private void drawAddresses (Graphics g, int street) {
        if (scale > LEVEL_RENDER_ADDRESSES) {
            return;
        }
        boolean isLayerOn = getLayer (LAYER_ADDRESSES);
        if (!isLayerOn && street < 0) {
            return;
        }

        // draw addresses only if layer is on or street is not null
        for (int seg = 0; seg < SEGMENT_LIMIT; seg++) {
            for (int i = 0; i < blN[seg]; i++) {
                if (street == blst[seg][i] || isLayerOn) {
                    drawOneAddress (g, street, seg, i, false);
                }
            }
        }
    }

    /**
     * Draw one address
     * @param g graphics
     * @param street selected street
     * @param seg data segment
     * @param i address in-segment number
     * @param isUnderCursor true, if under cursor
     */
    private void drawOneAddress (Graphics g, int street, int seg, int i, boolean isUnderCursor) {
        int ax = blx[seg][i], ay = bly[seg][i];
        if (ax > xmin && ax < xmax && ay > ymin && ay < ymax) {
            int offset = blnamep[seg][i];
            int length = blnamep[seg][i + 1] - offset;

            char s[] = name[seg];
            if (length == 0) {
                s = name_alloc;
                Util.itoa (((int) blnumber[seg][i]) & 0xff, s);
                offset = Util.offset;
                length = Util.length;
            }

            int sx = mx (ax);
            int sy = my (ay) - 1;

            g.setColor (COLORS[colorsOffset + COLOR_OFFSET_BACKGROUND]);
            int w = fontGreen.charsWidth (s, offset, length) + 1;
            int w2 = (w >> 1);
            int sh = fontGreen.getHeight () - 2;
            g.fillRect (sx - w2, sy - sh + 3, w, sh);

            boolean isSelected = currentObjectClass == CLASS_ADDRESS &&
                                 currentObjectItem == (i | seg << 16);

            if (isUnderCursor || isSelected) {
                if (!blinkCursorState) {
                    g.setColor (isUnderCursor ? COLOR_CURSOR :
                                blinkSelectedState ? (COLOR_SELECTED ^ 0xffffff) :
                                COLOR_SELECTED);
                    g.drawRect (sx - w2, sy - sh + 3, w, sh);
                }
            }
            fontGreen.drawChars (g, s, offset, length, sx - w2 + 1, sy);
        }
    }

    private boolean hasOwnIcon = false; // true if there're objects that has own icons
    /**
     * Draw labels
     * @param g graphics
     */
    private void drawLabels (Graphics g) {
//        if (scale < 12) {
            hasOwnIcon = false;
            drawLabelsIconMode (g, false);

            if (hasOwnIcon) {
                drawLabelsIconMode (g, true);
            }
//        }
    }

    private void drawLabelsIconMode (Graphics g, boolean showOwnIconOnly) {
        for (int seg = 0; seg < SEGMENT_LIMIT; seg++) {
            for (int i = 0; i < lbN[seg]; i++) {
                drawOneLabel (g, seg, i, LABEL_NORMAL, showOwnIconOnly);
            }
        }
    }

    /**
     * Draw one label
     * @param g graphics
     * @param seg data segment
     * @param i data offset
     * @param mode mode: normal, selected or cursor
     * @param showOwnIconOnly boolean true, if show private icon only
     */
    private void drawOneLabel (Graphics g, int seg, int i, int mode, boolean showOwnIconOnly) {
        int ax = lbx[seg][i], ay = lby[seg][i];
        int ct = lbct[seg][i];

        boolean forceShow = false;
        if (scale > 12) {
            if (Map.ctMode[ct] != CATEGORY_MODE_DOWNLOAD_MARKS) {
                return;
            }
            Object ctRemote = Map.ctLocalToRemote.get (new Integer (ct));
            if (! ("jam".equals (ctRemote) || "cop".equals (ctRemote))) {
                return;
            }
            forceShow = true;
        }

        // if layer is on and point is on screen
        if (getLayer (ObjectManager.getCategoryAncestor (ct)) &&
            ax > xmin && ax < xmax && ay > ymin && ay < ymax) {

            int sx = mx (ax);
            int sy = my (ay);

            // add to list of visible
            if (visibleObjectsN < visibleObjectsLimit && mode == LABEL_NORMAL && !showOwnIconOnly) {
                visibleObjects[visibleObjectsN++] = i | (seg << 16) | MASK_LABEL;
                visibleLabelsN++;
            }

            final int LEVEL_RENDER_LABEL_NAME = 3;
//            final int LEVEL_RENDER_LABEL_NAME_FOR_SELECTED = 7;
//            final int LEVEL_RENDER_LABEL_ICON = 12;
//            final int LEVEL_RENDER_LABEL_DOT = 20;
            final int LEVEL_RENDER_LABEL = 20;

            char[] nameseg = null;
            int s = 0, f = 0, w = 0, w2 = 0;
            int align = Graphics.HCENTER;

            // show name
            if (scale <= LEVEL_RENDER_LABEL_NAME || (mode == LABEL_SELECTED && scale <= 7) || mode == LABEL_CURSOR) {
                nameseg = name[seg];
                s = lbnamep[seg][i];
                f = lbnamep[seg][i + 1];
                w = getFont ().charsWidth (nameseg, s, f - s) + 3;
                w2 = (w >> 1) + 1;

                // name align
                if ((ctFontStyle[ct] & 0x18) == 0x8) {
                    align = Graphics.LEFT;
                    w2 = 0;
                } else if ((ctFontStyle[ct] & 0x18) == 0x10) {
                    align = Graphics.RIGHT;
                    w2 = w;
                }
            }

            // colors
            int bgcolor = COLORS[colorsOffset + COLOR_OFFSET_BACKGROUND];
            int forecolor = ctColor[ct];
            int rectcolor = forecolor;

            if (mode != LABEL_NORMAL) {
                // exchange
                int t = bgcolor;
                bgcolor = forecolor;
                forecolor = t;
            }

            if ((blinkSelectedState && mode == LABEL_SELECTED) || (blinkCursorState && mode == LABEL_CURSOR)) {
                // inverse
                bgcolor ^= 0xffffff;
                forecolor ^= 0xffffff;
                rectcolor ^= (mode == LABEL_SELECTED) ? 0xffffff : COLOR_CURSOR;
            }

            if (scale < LEVEL_RENDER_LABEL || mode != LABEL_NORMAL || forceShow) {
                int sh = getFont ().getHeight () + 1;

                boolean hasIcon = false;
                Image oneImg = null;

                if (seg == 0) { // static objects
                    if (img[IMG_SEGMENT_STATIC] != null) {
                        oneImg = (Image) img[IMG_SEGMENT_STATIC].get (new Integer (MASK_LABEL | i));
                    }
                }
                if (seg > 0 || oneImg == null) { // if object is dynamic or static but with uid>0
                    if (img[IMG_SEGMENT_DYNAMIC] != null) {
                        oneImg = (Image) img[IMG_SEGMENT_DYNAMIC].get (new Integer (lbUid[seg][i]));
                    }
                }

                if (oneImg != null) {
                    if (oneImg.getHeight () * oneImg.getWidth () > 32 * 32 && scale > 2) {
                        oneImg = null;
                    }
                }

                if (showOwnIconOnly) {
                    if (oneImg == null) {
                        return;
                    }
                } else {
                    hasOwnIcon |= oneImg != null;

                    // if no own icon, take it from category
                    Image ctImg = ctIcon[ct];
                    if (oneImg == null && (ctShow[ct] & CATEGORY_SHOW_ICON) > 0) {
                        oneImg = ctImg;
                    }
                }

                // draw icon
                if (oneImg != null) {
                    int wi = oneImg.getWidth () + 1;
                    int h = oneImg.getHeight () + 1;
                    sx -= wi / 2;
                    sy -= h / 2;
                    if (mode == LABEL_NORMAL) {
                        g.drawImage (oneImg, sx, sy, Graphics.LEFT | Graphics.TOP);
                    }
                    if (mode != LABEL_NORMAL) {
                        g.setColor (rectcolor);
                        g.drawRect (sx - 1, sy - 1, wi, h);
                    }
                    hasIcon = true;
                    sy += h + (sh >> 1) - 1;
                    sx += wi / 2;
                }

                if ((hasIcon && scale > 2) || (hasIcon && mode == LABEL_NORMAL)) {
                    return;
                }

                if (nameseg != null) {
                    if ((Map.ctShow[ct] & Map.CATEGORY_SHOW_LABEL) > 0) {
                        boolean showMeta = false;
                        Hashtable meta = null;
                        if (mode == LABEL_CURSOR) {
                            if (lbMeta[seg] != null) {
                                meta = lbMeta[seg][i];
                                if (meta != null) {
                                    showMeta = true;
                                }
                            }
                        }

                        if (showMeta) {
                            String[] info = new String[meta.size ()];

                            Enumeration en = meta.keys ();
                            int infoIndex = 0;
                            while (en.hasMoreElements ()) {
                                Object key = en.nextElement ();
                                info[infoIndex] = key.toString () + ": " + meta.get (key).toString ();
                                infoIndex++;
                            }

                            sy -= sh >> 1;
                            sh = getFont ().getHeight () * info.length;
                            w = 0;
                            for (int index = 0; index < info.length; index++) {
                                int localW = getFont ().stringWidth (info[index]);
                                if (localW > w) {
                                    w = localW;
                                }
                            }
                            w2 = w / 2;

                            g.setColor (bgcolor);
                            g.fillRoundRect (sx - w2, sy, w, sh, 6, 6);
                            g.setColor (COLOR_CURSOR);
                            g.drawRoundRect (sx - w2, sy, w, sh, 6, 6);
                            g.setColor (forecolor);

                            for (int index = 0; index < info.length; index++) {
                                g.drawString (info[index], sx, sy + getFont ().getHeight () * index,
                                              Graphics.TOP | Graphics.HCENTER);
                            }
                        } else {
                            // draw name
                            sy -= sh >> 1;
                            g.setColor (bgcolor);
                            g.fillRect (sx - w2, sy, w, sh);

                            if (mode == LABEL_CURSOR) {
                                g.setColor (COLOR_CURSOR);
                                g.drawRect (sx - w2, sy, w, sh);
                            }

                            g.setColor (forecolor);
                            g.drawChars (nameseg, s, f - s, sx, sy + 2, Graphics.TOP | align);
                        }
                    }
                } else {
                    if ((Map.ctShow[ct] & Map.CATEGORY_SHOW_DOT) > 0) {
                        g.setColor (rectcolor);
                        g.fillRect (sx - 2, sy - 2, 4, 4);
                        if (mode == LABEL_SELECTED)
                            g.drawRect (sx - 4, sy - 4, 8, 8);
                    }
                }
            }
        }
    }

    /**
     * Draw element (street) name
     * @param g graphics
     * @param el element id
     * @return -1 if the name was drawn
     */
    private int prevSt;
    private int prevStXMin, prevStYMin, prevStXMax, prevStYMax;
    private int drawElementName (Graphics g, int el) {
        int st = el2st[el];

        // find list of visible crossroads
        int outN = 0;
        int aconp = elconp[el], nconp = elconp[el + 1];
        int prevx = 0, prevy = 0;

        for (int j = aconp; j < nconp && outN < 120; j++) {
            int pn = elcon[j];

            int x = (pcx[pn] - originX) / scale;
            int y = (pcy[pn] - originY) / scale;

            // check if name of current street was already printed
            if (el2st[el] == prevSt && x > prevStXMin && x < prevStXMax && y > prevStYMin && y < prevStYMax) {
                if (outN > 0) {
                    break;
                }
            } else if (j > aconp) {
                byte r = Util.ClipLine (prevx, prevy, x, y, 0, 0, width, height);
                if (r != Util.CLIP_OUTSIDE) {
                    outx[outN] = (short) Util.clipx1;
                    outy[outN] = (short) Util.clipy1;
                    outN++;
                    outx[outN] = (short) Util.clipx2;
                    outy[outN] = (short) Util.clipy2;
                    outN++;

                    // second point is out of screen i.e. polyline is exited
                    // if part of polyline was already visible, abort iteration to avoid
                    // the case when polyline returns, because it may cause the name
                    // to be printed incorrectly, i.e. not along street, but beginning
                    // of name will be along one visible part of street and the rest of name
                    // will be somewhere in between start and end
                    if (x < 0 || x > width || y < 0 || y > height) {
                        break;
                    }
                }
            }
            prevx = x;
            prevy = y;
        }

        if (outN < 2) { // less than one crossroad is visible
            return 0;
        }

        int dx = outx[outN - 1] - outx[0],
                 dy = outy[outN - 1] - outy[0];

        boolean isHorz = Math.abs (dx) > Math.abs (dy);
        if (!((getLayer (LAYER_HORZ) && isHorz) ||
              (getLayer (LAYER_VERT) && !isHorz)))
            return 0; // layers are off

        // calculate length of element
        int length = 0;
        for (int k = 0; k < outN - 1; k++) {
            length += Util.hypot (outx[k] - outx[k + 1], outy[k] - outy[k + 1]);
        }

        // calculate length of name
        int sbs = (scale > 9) ? 2 : 1;
        if (parent.config[msrt.CONFIG_LETTER_SPACING] == 0) {
            sbs++;
        }

        // prepare name
        char name[] = name_alloc;
        int lgn = stnamep[st + 1] - stnamep[st] + 2;
        System.arraycopy (stnames, stnamep[st], name, 1, lgn - 2);
        name[0] = ' ';
        name[lgn - 1] = ' ';

        // calculate name length
        int nameLength = 0;
        if (isHorz) {
            for (int i = 0; i < lgn; i++) {
                nameLength += fontHorz.getWidth (name[i]);
            }
        } else {
            nameLength = lgn * (fontHorz.getHeight () - 4);
        }

        if (length < nameLength + sbs * lgn) { // not enough space for name
            return 0;
        }

        if (el2st[el] == currentObjectItem && currentObjectClass == CLASS_STREET) {
            isCurrentStreetVisible = true;
        }

        if (dx < 0) { // string need to be reversed
            for (int i = 0; i < lgn; i++)
                tmp[i] = name[lgn - i - 1];
            for (int i = 0; i < lgn; i++)
                name[i] = tmp[i];
        }

        lgn--; // remove last char = space
        int space = ((length - nameLength) << 2) / (lgn - 1);
        int spaceLimit = sbs * 8;
        if (space > spaceLimit)
            space = spaceLimit; // limit space b/w symbols
        int spaceOffset = space;
        int dev = (((length - nameLength) << 2) - (space * (lgn - 1))) >> 1; // free space
        if (dev > 10) {
            spaceOffset += parent.random (el) % dev;
        }
        int distance = 0;
        int kv_length = 0;
        int xnow = 0, ynow = 0, xnext = 0, ynext = 0;

        out_el[el] = -1;

        int color = ((st | MASK_STREET) == currentObject) ? COLOR_STREET_NAME_SELECTED :
                    ((isHorz) ? COLOR_STREET_NAME_HORIZONTAL : COLOR_STREET_NAME_VERTICAL);

        FontM font = (isHorz) ? fontHorz : fontVert;
        int fontHeight = font.getHeight () - 4;

        prevStXMin = 0xffff;
        prevStYMin = 0xffff;
        prevStXMax = 0;
        prevStYMax = 0;
        prevSt = el2st[el];

        for (int n = 1, p = 0, shift = spaceOffset; n < lgn; n++) {
            while (distance <= (shift >> 2) && p < outN - 1) {
                xnow = outx[p];
                xnext = outx[p + 1];
                ynow = outy[p];
                ynext = outy[p + 1];

                kv_length = Util.hypot (xnext - xnow, ynext - ynow);
                distance += kv_length;
                p++;
            }

            int wx, wy;

            if (kv_length < 8) {
                wx = (xnow + xnext) >> 1;
                wy = (ynow + ynext) >> 1;
            } else {
                wx = xnext - ((distance - (shift >> 2)) * (xnext - xnow)) / kv_length;
                wy = ynext - ((distance - (shift >> 2)) * (ynext - ynow)) / kv_length;
            }

            if (wx > prevStXMax)
                prevStXMax = wx;
            if (wx < prevStXMin)
                prevStXMin = wx;
            if (wy > prevStYMax)
                prevStYMax = wy;
            if (wy < prevStYMin)
                prevStYMin = wy;

            wy = height - wy + 3;

            int charw = font.getWidth (name[n]);

            if (isHorz) {
                if (dx < 0) {
                    wx -= charw;
                }
            } else {
                wx += (6 - charw) >> 1;
            }

            g.setColor (color);
            font.drawChar (g, name[n], wx, wy, Graphics.BASELINE);

            shift += space + ((isHorz ? charw : fontHeight) << 2);
        }

        return -1;
    }

    /**
     * Draw graphic stream
     * @param g graphics
     * @param stream stream data
     * @param streamp stream pointers
     * @param streampN number of elements in stream
     */
    void drawGS (Graphics g, short stream[], short streamp[], int streampN) {
        boolean useFill = (parent.config[msrt.CONFIG_MAP_DETAILS] == 2 ||
                           (scale < 9 && parent.config[msrt.CONFIG_MAP_DETAILS] == 1)) && !fastupdate;

        for (int i = 0, ptr = 0; i < streampN; i++) {
            short cx = stream[ptr], cy = stream[ptr + 1];
            short w = stream[ptr + 2], h = stream[ptr + 3];
            int bx1 = cx - w, by1 = cy - h;
            int bx2 = cx + w, by2 = cy + h;

            if (w > scale && h > scale)
                if (!(bx2 < xmin || by2 < ymin || bx1 > xmax || by1 > ymax)) {
                    short command = stream[ptr + 4];
                    short cr = stream[ptr + 5], cg = stream[ptr + 6], cb = stream[ptr + 7];
                    short fr = stream[ptr + 8], fg = stream[ptr + 9], fb = stream[ptr + 10];
                    g.setColor (cr, cg, cb);

                    int ax = stream[ptr + 11] + bx1, ay = stream[ptr + 12] + by1;

                    int n = (streamp[i] - 11) / 2;
                    int xPoints[] = null, yPoints[] = null;
                    boolean isFilled = (command & 0x2) > 0;
                    if (isFilled) {
                        xPoints = new int[n];
                        yPoints = new int[n];
                        xPoints[0] = mx (ax);
                        yPoints[0] = my (ay);
                    }

                    for (int j = 13, jp = 1; j < streamp[i]; j += 2, jp++) {
                        int nx = stream[ptr + j] + ax, ny = stream[ptr + j + 1] + ay;

                        if (isFilled) {
                            xPoints[jp] = mx (nx);
                            yPoints[jp] = my (ny);
                        }

                        if ((command & 0x1) > 0) {
                            g.drawLine ((ax - originX) / scale,
                                        height - (ay - originY) / scale,
                                        (nx - originX) / scale,
                                        height - (ny - originY) / scale);
                        }
                        ax = nx;
                        ay = ny;
                    }
                    if (isFilled) {
                        g.setColor (fr, fg, fb);
                        if (useFill) {
                            GraphicsPlus.fillPolygon (g, xPoints, yPoints);
                        } else {
                            GraphicsPlus.drawPolygon (g, xPoints, yPoints);
                        }
                    }
                }
            ptr += streamp[i];
        }
    }

    /**
     * Move map focus to make specified rectangle visible
     * @param ax rect's x1
     * @param ay rect's y1
     * @param bx rect's x2
     * @param by rect's y2
     * @param scaleTo desired scale
     * @param forced true if origin change is forced even if parts of specified rectangle are visible
     */
    public void moveFocus (int ax, int ay, int bx, int by, int scaleTo, boolean forced) {
        if (!forced) {
            if (ax > xmin && bx < xmax && ay > ymin && by < ymax) {
                return;
            }
        }

        if (forced || (curCategory != CATEGORY_VISIBLE && changeFocus)) {
            if (ax < 0) {
                ax = 0;
            } else if (ax > cityX) {
                ax = cityX;
            }
            if (ay < 0) {
                ay = 0;
            } else if (ay > cityY) {
                ay = cityY;
            }
            if (bx < 0) {
                bx = 0;
            } else if (bx > cityX) {
                bx = cityX;
            }
            if (by < 0) {
                by = 0;
            } else if (by > cityY) {
                by = cityY;
            }
            originX = (ax + bx) >> 1;
            originY = (ay + by) >> 1;

            scale = Math.max (Math.abs (by - ay) / height, Math.abs (bx - ax) / width);
            if (scale < scaleTo) {
                scale = scaleTo;
            }

            update = true;
            repaint ();
        }
        changeFocus = true;
    }

    /**
     * Move focus to point (lat, lon) to include circle with specified radius
     * @param latitude int
     * @param longitude int
     * @param radius int
     */
    public void moveFocus (int latitude, int longitude, int radius) {
        int x = longitude2x (longitude);
        int y = latitude2y (latitude);
        if (!(x > 0 && x < cityX && y > 0 && y < cityY)) {
            x = originX;
            y = originY;
        }
        moveFocus (x - radius, y - radius, x + radius, y + radius, 2, true);
    }

    private void moveFocusRelative (int dx, int dy) {
        originX += dx;
        if (originX < 0) {
            originX = 0;
        } else if (originX > cityX) {
            originX = cityX;
        }

        originY += dy;
        if (originY < 0) {
            originY = 0;
        } else if (originY > cityY) {
            originY = cityY;
        }

        update = true;
        repaint ();
    }

    /**
     * Return true if point with specified coordinates is located inside city bounds
     * @param latitude int
     * @param longitude int
     * @return boolean
     */
    public boolean isInCity (int latitude, int longitude) {
        int x = longitude2x (longitude);
        int y = latitude2y (latitude);
        return x > 0 && x < cityX && y > 0 && y < cityY;
    }

    /**
     * Sets up context menu depending on current object
     */
    void selectionChanged () {
        initCommands ();
        hasInfoCommand = false;

        if (currentObjectClass == CLASS_ADDRESS || currentObjectClass == CLASS_LABEL ||
            currentObjectClass == CLASS_STREET || currentObjectClass == CLASS_PC) {
            if (msrt.features[msrt.FEATURE_ROUTING]) {
                if (currentObjectClass != CLASS_STREET)
                    addCommand (parent.startCommand);
                if (PathFinder.isStartSet () && !PathFinder.isSameToStart (currentObject)) {
                    addCommand (parent.pathCommand);
                }
            }

            if (currentObjectClass == CLASS_LABEL) {
                int seg = currentObjectItem >> 16;
                int off = currentObjectItem & 0xffff;
                if (lbUid[seg][off] > 0 && msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
                    int mode = ctMode[lbct[seg][off]];
                    if (mode == CATEGORY_MODE_DOWNLOAD_MARKS) {
                        addCommand (parent.removeMarkCommand);
                    }

                    addCommand (parent.infoCommand);
                    hasInfoCommand = true;
                }
            } else if (currentObjectClass == CLASS_STREET) {
                if (msrt.features[msrt.FEATURE_ONLINE_ADDRESSES])
                    addCommand (parent.getAddressesCommand);

                if (ObjectManager.hasStreetAddresses (currentObjectItem))
                    addCommand (parent.showAddressesCommand);
            } else if (currentObjectClass == CLASS_ADDRESS) {
                addCommand (parent.showAddressesCommand);
            }
        }
    }

    /*
     CONVERT COORDINATES
     */

    /**
     * Converts meter to pixel. For use in paint function.
     * @param x coordinate in meters
     * @return coordinate in pixels
     */
    public static int mx (int x) {
        return (x - originX) / scale;
    }

    /**
     * Converts meter to pixel. For use in paint function.
     * @param y coordinate in meters
     * @return coordinate in pixels
     */
    public static int my (int y) {
        return height - (y - originY) / scale;
    }

    // functions not for use in paint cycle
    static int mx2 (int x) {
        return (x - (originX - widthHalf * scale)) / scale;
    }

    static int my2 (int y) {
        return height - (y - (originY - heightHalf * scale)) / scale;
    }

    static int mx2r (int x) {
        return x * scale + (originX - widthHalf * scale);
    }

    static int my2r (int y) {
        return (height - y) * scale + (originY - heightHalf * scale);
    }

    /**
     * Convert latitude to local y coordinate
     * @param lat int
     * @return int
     */
    public static int latitude2y (int lat) {
        return (int) (((lat - Map.latitude) * Map.lat2meters) / 100000);
    }

    /**
     * Convert longitude to local x coordinate
     * @param lon int
     * @return int
     */
    public static int longitude2x (int lon) {
        return (int) (((lon - Map.longitude) * Map.lon2meters) / 100000);
    }

    public static int x2longitude (int x) {
        return (int) ((x * 100000L) / Map.lon2meters + Map.longitude);
    }

    public static int y2latitude (int y) {
        return (int) ((y * 100000L) / Map.lat2meters + Map.latitude);
    }

    /**
     * Converts x in global coordinates into local one
     * @param x int
     * @return int
     */
    static int x2local (int x) {
        return x / globalScale;
    }

    /**
     * Converts y in global coordinates into local one
     * @param y int
     * @return int
     */
    static int y2local (int y) {
        return y / globalScale;
    }

    /**
     * Get cursor latitude
     * @return int
     */
    public static int getCursorLatitude () {
        return y2latitude (my2r (cursorScreenY));
    }

    /**
     * Get cursor longitude
     * @return int
     */
    public static int getCursorLongitude () {
        return x2longitude (mx2r (cursorScreenX));
    }

    /**
     * Set Navigator state: true if switch to navigator, false if switch to map
     * @param f boolean
     */
    public void setNavigator (boolean f) {
        isNavigator = f;
        if (isNavigator) {
            hideNotify ();
            initCommands ();
            navigator.showNotify ();
        } else {
            initCommands ();
            showNotify ();
        }
        repaint ();
    }

    public void forceUpdate () {
        update = true;
        needpaint = true;
        repaint ();
    }

    public void switchToViewMode () {
        globalMode = GLOBAL_MODE_NORMAL;
        initCommands ();
        parent.theMap.setComponentListener (parent);
    }

    public void switchToPickupMode (ComponentListener componentListener, Command returnCommand, Command pickupCommand) {
        if (isNavigator) {
            setNavigator (false);
        }
        globalMode = GLOBAL_MODE_PICKUP;

        initCommands ();

        pickupModePickupCommand = pickupCommand;
        if (pickupCommand != null) {
            addCommand (pickupCommand);
        }

        validateCursor ();
        setMasterCommand (returnCommand);
        setComponentListener (componentListener);
    }
}
