/*****************************************************************************/
/*                               R A D A R  4                                */
/*                       (c) 2003-2009 Ilya Shakhat                          */
/*                           (c) 2006-2009 TMS                               */
/*****************************************************************************/
/*****************************************************************************/
/*                               m f i n i t y                               */
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                         (c) 2003-2009 Ilya Shakhat                        */
/*****************************************************************************/

package com.ish.mobimap;

import java.io.*;
import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.rms.*;

import com.ish.mobimap.net.*;
import com.ish.mobimap.ui.*;
import javax.microedition.io.*;
import com.ish.mobimap.radar.*;

/**
 * The main midlet class. Controls program flow, lifecycle of base components.
 * Singleton.
 */
public class msrt extends MIDlet implements CommandListener, ComponentListener, OnlineLoaderListener {
    /* build versions */
    public static final int BUILD_MOBIMAP = 1;
    public static final int BUILD_RADAR = 2;
    public static final int BUILD_VERSION = BUILD_MOBIMAP;

    /* program-wide constants */
    public static final String PROGRAM_BUILD = "091116";
    public static final String PROGRAM_NAME = (BUILD_VERSION == BUILD_RADAR) ?
                                              "\u0420\u0430\u0434\u0430\u0440 4.0" : "Mobimap 4.4 Infinity";
    public static final String PROGRAM_COPYRIGHT = (BUILD_VERSION == BUILD_RADAR) ?
        "(c)2006-2009 \u041E\u041E\u041E \"\u0422\u041C\u0421\"\n(c)2003-2009 \u0418\u043B\u044C\u044F \u0428\u0430\u0445\u0430\u0442" :
        "(c)2003-2009 Ilya Shakhat";
    public static final String PROGRAM_USER_AGENT = (BUILD_VERSION == BUILD_RADAR) ?
        "Radar.Mobile" : "Mobimap.Mobile.Q4";
    public static final String PROGRAM_THANKS = "\n\nTesting & QA: Roman Sinyakov\nThanks for useful advices and testing of previous releases to Natalya Malysheva, Alexander Milevsky and Alexey Emelyanenko ";

    public static String regInfo;
    private String regFirstName, regLastName, regName, regLicense;

    /* version-dependant constants */
    private static final int HARDKEY = (BUILD_VERSION == BUILD_RADAR) ? 5700 : 4500;

    public static Display display;

    private static msrt instance;

    /* main modules */
    public Map theMap;
    DataLoader theDataLoader;
    OnlineLoader theOnlineLoader;
    ObjectManager theObjectManager;
    Navigator theNavigator;
    Locator theLocator;
    Browser theBrowser;
    SearchEngine theSearchEngine;
    SMSSender theSMSSender;

    /* user interface */
    private Form configForm;
    private ChoiceGroup mapDetailChoice;
    private ChoiceGroup namesAmountChoice;
    private ChoiceGroup searchModeChoice;
    private ChoiceGroup keyboardLayoutChoice;
    private ChoiceGroup paletteChoice;
    private ChoiceGroup mapFontChoice;
    private ChoiceGroup uiFontChoice;
    private ChoiceGroup timeChoice;
    private Form layersForm;
    private ChoiceGroup layersChoice;
    private Form autoUpdaterForm;
    private ChoiceGroup autoUpdateChoice;
    private static Frame frame; // main frame for light-weight components

    /* map params */
    private String mapName;
    private int[] params;
    private int paramsN;
    short cityId;
    short userIdHigh, userIdLow;
    String rmsSuffix;
    private int keyA, keyB;
    String ownerKey;
    boolean isFirstTime; // true if program has just been set up
    int[] mainMenuXRef;

    /* configuration */
    public static int[] config;
    private static int[] configCopy;
    public static final byte CONFIG_KEYBOARD_LAYOUT = 3;
    public static final byte CONFIG_MAP_DETAILS = 4;
    public static final byte CONFIG_LETTER_SPACING = 5;
    public static final byte CONFIG_SEARCH_MODE = 6;
    public static final byte CONFIG_PALETTE = 7;
    public static final byte CONFIG_MAP_FONT = 8;
    public static final byte CONFIG_UI_FONT = 9;
    public static final byte CONFIG_TIME = 10;
    public static final byte CONFIG_LOCATOR_MONITORING = 11;
    public static final byte CONFIG_LOCATOR_UPDATE = 12;
    public static final byte CONFIG_LOCATOR_HISTORY = 13;
    public static final byte CONFIG_AUTOUPDATE = 14;
    private static final byte CONFIG_N = 15;

    /* statistics */
    public static int[] statistics;
    public static final byte STATISTICS_LAUNCH_COUNTER = 0;
    public static final byte STATISTICS_OVERALL_TIME = 1;
    public static final byte STATISTICS_MAP_TIME = 2;
    public static final byte STATISTICS_BROWSER_TIME = 3;
    public static final byte STATISTICS_ONLINE_TIME = 4;
    public static final byte STATISTICS_ONLINE_REQUEST_COUNTER = 5;
    public static final byte STATISTICS_ONLINE_RESPONCE_COUNTER = 6;
    public static final byte STATISTICS_TRAFFIC_OUTGOING = 7;
    public static final byte STATISTICS_TRAFFIC_INCOMING = 8;
    public static final byte STATISTICS_N = 9;

    /* ui */
    ListM objList;
    private ListM navigationMenu;
    ListM mainMenu;
    ListM pathMenu;
    private ListM balanceMenu;
    private ListM helpMenu;
    ProgressScreen progressScreen;
    private Alert alertConfirm;
    private Alert infoAlert;

    /*
     * previous display & component
     */
    private static Displayable[] previousDisplay;
    private static Component[] previousComponent;
    private static int previousP;
    private final static byte PREVIOUS_LIMIT = 5;

    Displayable prePauseDisplay = null;
    int onlineFormItems[], onlineFormItemsN;

    public Image icons[];
    private static final int ICONS_N = 12;

    public static String Resources[];

    public static Command okCommand; // used as OK button
    public static Command cancelCommand; // used as Cancel button
    public static Command menuCommand; // used in Map for menu launching
    public static Command backCommand; // used as Back button
    public static Command gotoCommand; // used when object is selected and focus is shifted
    public static Command dirCommand; // used when object is selected and focus stays unchanged
    public static Command pathCommand; // used for 'find route to' command
    public static Command startCommand; // used for 'set route start' command
    public static Command infoCommand; // used as shortcut for Online-Info command
    public static Command mapCommand; // used in menu for switching to map
    public static Command selectCommand; // used in ListM for selecting items
    public static Command backToMapCommand;
    public static Command backToMenuCommand;
    public static Command showAddressesCommand; // show addresses for specified street
    public static Command getAddressesCommand; // getAddresses request
    public static Command getMarksCommand; // getMarks request
    public static Command removeMarkCommand; // removeMark request
    public static Command subscriberListCommand; // subscriberList request

    boolean firstRun;

    boolean onlineConnected; // true if program is connected to remote server

    /**
     * PROFILE
     */
    public static String profile[];

    public final static int PROFILE_PARAMSANDFEATURES = 0;
    public final static int PROFILE_SERVLET = 1;
    public final static int PROFILE_FEEDBACK = 2;
    public final static int PROFILE_NOT_USED_3 = 3;
    public final static int PROFILE_MAP_INFO = 4;
    public final static int PROFILE_PACKAGE = 5;
    public final static int PROFILE_LOCALE = 6;
    public final static int PROFILE_START_LINK_TYPE = 7;
    public final static int PROFILE_START_LINK = 8;
    public final static int PROFILE_FONTSEGS = 9;
    public final static int PROFILE_NOT_USED_10 = 10;
    public final static int PROFILE_NOT_USED_11 = 11;
    public final static int PROFILE_ADV_MENU = 12;
    public final static int PROFILE_ADV_ICON = 13;
    public final static int PROFILE_ADV_LINK_TYPE = 14;
    public final static int PROFILE_ADV_LINK = 15;
    public final static int PROFILE_EXPIRE = 16;
    public final static int PROFILE_PROGRESS = 17;
    public final static int PROFILE_EXPIRE_LINK_TYPE = 18;
    public final static int PROFILE_EXPIRE_LINK = 19;
    public final static int PROFILE_ADV2_MENU = 20;
    public final static int PROFILE_ADV2_ICON = 21;
    public final static int PROFILE_ADV2_LINK_TYPE = 22;
    public final static int PROFILE_ADV2_LINK = 23;

    /**
     * FEATURES
     */
    public static boolean[] features;

    public final static int FEATURE_N = 18;
    public final static int FEATURE_ONLINE_OBJECTS = 0;
    public final static int FEATURE_ONLINE_ADDRESSES = 1;
    public final static int FEATURE_ONLINE_CATEGORIES = 2;
    public final static int FEATURE_XMAP = 3;
    public final static int FEATURE_CFGKEYBOARD = 4;
    public final static int FEATURE_CFGDETAIL = 5;
    public final static int FEATURE_CFGNAMES = 6;
    public final static int FEATURE_CFGSEARCH = 7;
    public final static int FEATURE_CFGPALETTE = 8;
    public final static int FEATURE_CFGFONT = 9;
    public final static int FEATURE_BROWSER = 10;
    public final static int FEATURE_MANUAL = 11;
    public final static int FEATURE_SERVER_SEARCH = 12;
    public final static int FEATURE_NOT_USED_13 = 13;
    public final static int FEATURE_DEBUG = 14;
    public final static int FEATURE_SUN = 15;
    public final static int FEATURE_ROUTING = 16;
    public final static int FEATURE_ROADDIRS = 17;

    /**
     * ICONS
     */
    final static int ICON_SEARCH = 0;
    final static int ICON_OBJECTS = 1;
    final static int ICON_MAP = 2;
    final static int ICON_LAYERS = 3;
    final static int ICON_ROUTE = 4;
    final static int ICON_ONLINE = 5;
    final static int ICON_LOCATOR = 6;
    final static int ICON_PARAMS = 7;
    final static int ICON_HELP = 8;
    final static int ICON_EXIT = 9;
    final static int ICON_BALANCE = 10;
    final static int ICON_CATEGORY = 11;

    /**
     * MAIN MENU
     */
    private final static int MNU_SEARCH = 0;
    private final static int MNU_OBJECTS = 1;
    private final static int MNU_MAP = 2;
    private final static int MNU_LAYERS = 3;
    private final static int MNU_ROUTE = 4;
    private final static int MNU_BROWSER = 5;
    private final static int MNU_LOCATOR = 6;
    private final static int MNU_BALANCE = 7;
    private final static int MNU_CONFIG = 8;
    private final static int MNU_HELP = 9;
    private final static int MNU_EXIT = 10;
    private final static int MNU_ADV = 11;
    private final static int MNU_DEBUG = 12;
    private final static int MNU_ADV2 = 13;

    /*
     * DISPLAY CODES FOR HEAVY-WEIGHT COMPONENTS
     */
    public static final byte DISPLAY_LAYERS = -1;
    public static final byte DISPLAY_CONFIG = -2;
    public static final byte DISPLAY_INFO = -3;
    public static final byte DISPLAY_AUTOUPDATER = -4;

    /*
     * DISPLAY CODES FOR LIGHT-WEIGHT COMPONENTS
     */
    public static final byte DISPLAY_MENU = 1;
    public static final byte DISPLAY_MAP = 2;
    public static final byte DISPLAY_SEARCH = 3;
    public static final byte DISPLAY_OBJLIST = 4;
    public static final byte DISPLAY_BROWSER = 5;
    public static final byte DISPLAY_LOCATOR = 9;
    public static final byte DISPLAY_HELP_FORM = 10;
    public static final byte DISPLAY_ROUTE_MENU = 11;
    public static final byte DISPLAY_NAVIGATION_MENU = 12;
    public static final byte DISPLAY_BALANCE_MENU = 13;

    /**
     * CODES FOR INFO ALERT
     */
    public static final byte ALERT_ERROR = 17;
    public static final byte ALERT_INFO = 11;

    /**
     * list of light-weight components
     */
    public static Component[] components;
    public static Displayable[] displays;

    private long timeWhenStarted;

    private long expire;

    private static String DEBUG_LOG = "";

    public static final int[] AUTOUPDATE_VALUES = {0, 1, 2, 3, 5, 10, 15, 20, 30, 60};
    public static final int AUTOUPDATE_DEFAULT = 8; // index in VALUES list

    public msrt () {
        firstRun = true;
    }

    /**
     * Start App is called by Java machine in two cases:
     * 1. Launch the midlet for the first time
     * or
     * 2. Resume the midlet after pause state
     */
    public void startApp () {
        instance = this;

        if (firstRun) {
            init ();
            startUp ();

            launchMap ();
        } else {
            display.setCurrent ((prePauseDisplay != null) ? prePauseDisplay : frame);
        }

        firstRun = false;
    }

    /**
     * Pause the midlet
     */
    public void pauseApp () {
        saveMapState (theMap.originX, theMap.originY, theMap.scale);
        prePauseDisplay = display.getCurrent ();
    }

    /**
     * Exit the program
     * @param unconditional false if midlet can stop destroying itself
     */
    public void destroyApp (boolean unconditional) {
        frame.destroy ();
        display.setCurrent (null);
        if (BUILD_VERSION == BUILD_RADAR) {
            if (theLocator != null) {
                theLocator.destroy ();
            }
        }
        if (theMap != null) {
            statistics[msrt.STATISTICS_OVERALL_TIME] += (System.currentTimeMillis () - timeWhenStarted) / 1000;
            saveMapState (Map.originX, Map.originY, Map.scale);
            theMap.destroy ();
        }
        theMap = null;
        theOnlineLoader = null;

        notifyDestroyed ();
    }

    public static msrt getInstance () {
        return instance;
    }

    //
    // INITIALIZE MIDLET
    //

    /**
     * Init Midlet
     */
    private void init () {
        // read localization info - all resource strings
        try {
            InputStream is = getClass ().getResourceAsStream ("/locale");
            DataInputStream in = new DataInputStream (is);

            short n = in.readShort ();
            Resources = new String[n];
            for (short i = 0; i < n; i++)
                Resources[i] = in.readUTF ();
            in.close ();
        } catch (IOException z) {}

        // read profile
        try {
            InputStream is = getClass ().getResourceAsStream ("/profile");
            DataInputStream in = new DataInputStream (is);

            short n = in.readShort ();
            profile = new String[n];
            for (short i = 0; i < n; i++) {
                String s = in.readUTF ();
                profile[i] = Util.isStringNullOrEmpty (s) ? null : s;
            }
            in.close ();

            config = new int[CONFIG_N];
            configCopy = new int[CONFIG_N];
            for (int i = 0; i < CONFIG_N; i++)
                config[i] = 0;
            for (int i = 3; i < 9; i++) // only first config params are stored...
                config[i] = profile[PROFILE_PARAMSANDFEATURES].charAt (i + 1) - '0';
            config[CONFIG_AUTOUPDATE] = AUTOUPDATE_DEFAULT;
        } catch (IOException z) {}

        // load icons
        icons = new Image[ICONS_N];
        for (int i = 0; i < ICONS_N; i++) {
            try {
                icons[i] = Image.createImage ("/i/" + i);
            } catch (Exception ex1) {
            }
        }

        // profiled features
        features = new boolean[FEATURE_N];
        for (int i = 0; i < FEATURE_N; i++) {
            features[i] = profile[PROFILE_PARAMSANDFEATURES].charAt (i) != '0';
        }
        statistics = new int[STATISTICS_N];

        // create UI
        display = Display.getDisplay (this);

        /**
         * MAIN FRAME
         */
        frame = new Frame ();

        previousDisplay = new Displayable[PREVIOUS_LIMIT];
        previousComponent = new Component[PREVIOUS_LIMIT];

        /**
         * COMMANDS
         */
        okCommand = new Command (Resources[6], Command.OK, 10);
        cancelCommand = new Command (Resources[10], Command.CANCEL, 10);
        backCommand = new Command (Resources[72], Command.BACK, 101);
        menuCommand = new Command (Resources[73], Command.SCREEN, 12);
        gotoCommand = new Command (Resources[(BUILD_VERSION == BUILD_RADAR) ? 156 : 74], Command.SCREEN, 2);
        dirCommand = new Command (Resources[75], Command.SCREEN, 1);
        pathCommand = new Command (Resources[95], Command.SCREEN, 5);
        startCommand = new Command (Resources[103], Command.SCREEN, 6);
        infoCommand = new Command (Resources[49], Command.SCREEN, 3);
        mapCommand = new Command (Resources[3], Command.SCREEN, 11);
        selectCommand = new Command (Resources[156], Command.SCREEN, 1);
        backToMapCommand = new Command (Resources[106], Command.BACK, 100);
        backToMenuCommand = new Command (Resources[102], Command.BACK, 100);
        showAddressesCommand = new Command (Resources[111], Command.SCREEN, 1);
        getAddressesCommand = new Command (Resources[110], Command.SCREEN, 2);
        getMarksCommand = new Command (Resources[191], Command.SCREEN, 4);
        removeMarkCommand = new Command (Resources[144], Command.SCREEN, 6);
        subscriberListCommand = new Command (Resources[96], Command.SCREEN, 20);

        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            backToMapCommand = mapCommand;
            backToMenuCommand = menuCommand;
        }

        /**
         * MAIN MENU
         */
        mainMenuXRef = new int[13];
        mainMenu = new ListM (this, frame, Resources[73]);
        if (BUILD_VERSION == BUILD_MOBIMAP) {
            mainMenu.setLook (Design.COLOR_MAIN_MENU_TITLE_BACKGROUND, Design.COLOR_MENU_TITLE_TEXT);
        }
        mainMenu.setComponentListener (this);
        int i = 0;
        mainMenu.append (Resources[56], icons[ICON_MAP]);
        mainMenuXRef[i++] = MNU_MAP;
        if (BUILD_VERSION == BUILD_RADAR) {
            mainMenu.append (Resources[96], icons[ICON_LOCATOR]);
            mainMenuXRef[i++] = MNU_LOCATOR;
        }
        if (BUILD_VERSION == BUILD_RADAR) {
            mainMenu.append (Resources[119], icons[ICON_BALANCE]);
            mainMenuXRef[i++] = MNU_BALANCE;
        }
        mainMenu.append (Resources[0], icons[ICON_SEARCH]);
        mainMenuXRef[i++] = MNU_SEARCH;
        mainMenu.append (Resources[34], icons[ICON_OBJECTS]);
        mainMenuXRef[i++] = MNU_OBJECTS;
        if (features[FEATURE_BROWSER]) {
            mainMenu.append (Resources[152], icons[ICON_ONLINE]);
            mainMenuXRef[i++] = MNU_BROWSER;
        }
        if (BUILD_VERSION == BUILD_MOBIMAP) {
            mainMenu.append (Resources[37], icons[ICON_LAYERS]);
            mainMenuXRef[i++] = MNU_LAYERS;
        }
        if (features[FEATURE_ROUTING]) {
            mainMenu.append (Resources[62], icons[ICON_ROUTE]);
            mainMenuXRef[i++] = MNU_ROUTE;
        }
        mainMenu.append (Resources[24], icons[ICON_PARAMS]);
        mainMenuXRef[i++] = MNU_CONFIG;
        mainMenu.append (Resources[8], icons[ICON_HELP]);
        mainMenuXRef[i++] = MNU_HELP;
        if (profile[PROFILE_ADV_MENU] != null && profile[PROFILE_ADV_ICON] != null) {
            try {
                mainMenu.append (profile[PROFILE_ADV_MENU],
                                 Image.createImage (profile[PROFILE_ADV_ICON]));
                mainMenuXRef[i++] = MNU_ADV;
            } catch (Exception ex) {
            }
        }
        if (profile.length >= PROFILE_ADV2_LINK)
            if (profile[PROFILE_ADV2_MENU] != null && profile[PROFILE_ADV2_ICON] != null) {
                try {
                    mainMenu.append (profile[PROFILE_ADV2_MENU],
                                     Image.createImage (profile[PROFILE_ADV2_ICON]));
                    mainMenuXRef[i++] = MNU_ADV2;
                } catch (Exception ex) {
                }
            }
        if (features[FEATURE_DEBUG]) {
            mainMenu.append ("Debug Log", null);
            mainMenuXRef[i++] = MNU_DEBUG;
        }
        mainMenu.append (Resources[5], icons[ICON_EXIT]);
        mainMenuXRef[i++] = MNU_EXIT;
        mainMenu.addCommand (mapCommand);
        mainMenu.setBackCommand (mapCommand);

        /**
         * SEARCH FORM
         */
        theSearchEngine = new SearchEngine (this, frame);
        theSearchEngine.setComponentListener (this);

        /**
         * CONFIG FORM
         */
        configForm = new Form (Resources[24]);
        mapDetailChoice = new ChoiceGroup (Resources[27], Choice.EXCLUSIVE);
        mapDetailChoice.append (Resources[28], null);
        mapDetailChoice.append (Resources[29], null);
        mapDetailChoice.append (Resources[30], null);
        namesAmountChoice = new ChoiceGroup (Resources[31], Choice.EXCLUSIVE);
        namesAmountChoice.append (Resources[32], null);
        namesAmountChoice.append (Resources[33], null);
        keyboardLayoutChoice = new ChoiceGroup (Resources[25], Choice.EXCLUSIVE);
        keyboardLayoutChoice.append (Resources[26], null);
        keyboardLayoutChoice.append (Resources[59], null);
        if (BUILD_VERSION != BUILD_RADAR) {
            keyboardLayoutChoice.append (Resources[61], null);
        }
        keyboardLayoutChoice.append (Resources[121], null);
        searchModeChoice = new ChoiceGroup (Resources[38], Choice.EXCLUSIVE);
        searchModeChoice.append (Resources[39], null);
        searchModeChoice.append (Resources[40], null);
        searchModeChoice.append (Resources[41], null);
        paletteChoice = new ChoiceGroup (Resources[47], Choice.EXCLUSIVE);
        paletteChoice.append (Resources[55], null);
        paletteChoice.append (Resources[53], null);
        mapFontChoice = new ChoiceGroup (Resources[64], Choice.EXCLUSIVE);
        mapFontChoice.append (Resources[16], null);
        mapFontChoice.append (Resources[116], null);
        mapFontChoice.append (Resources[15], null);
        uiFontChoice = new ChoiceGroup (Resources[66], Choice.EXCLUSIVE);
        uiFontChoice.append (Resources[18], null);
        uiFontChoice.append (Resources[19], null);
        uiFontChoice.append (Resources[20], null);
        timeChoice = new ChoiceGroup (Resources[131], Choice.EXCLUSIVE);
        timeChoice.append (Resources[131], null);
        timeChoice.append (Resources[131], null);

        configForm.append (paletteChoice);
        configForm.append (mapDetailChoice);
        configForm.append (uiFontChoice);
        configForm.append (mapFontChoice);
        configForm.append (namesAmountChoice);
        configForm.append (searchModeChoice);
        configForm.append (keyboardLayoutChoice);
        configForm.append (timeChoice);
        configForm.addCommand (okCommand);
        configForm.addCommand (cancelCommand);
        configForm.setCommandListener (this);

        /**
         * LAYERS FORM
         */
        layersForm = new Form (Resources[37]);
        layersForm.addCommand (okCommand);
        layersForm.addCommand (cancelCommand);
        layersForm.setCommandListener (this);

        /**
         * AUTO UPDATER FORM
         */
        if (BUILD_VERSION == BUILD_RADAR) {
            autoUpdaterForm = new Form (Resources[195]);
            autoUpdaterForm.setCommandListener (this);
            autoUpdaterForm.addCommand (okCommand);
            autoUpdaterForm.addCommand (cancelCommand);

            autoUpdateChoice = new ChoiceGroup (msrt.Resources[70], ChoiceGroup.POPUP);
            autoUpdateChoice.append (msrt.Resources[138], null);
            for (int time = 1; time < AUTOUPDATE_VALUES.length; time++)
                autoUpdateChoice.append ("" + AUTOUPDATE_VALUES[time] + msrt.Resources[13], null);
            autoUpdaterForm.append (autoUpdateChoice);
        }

        /**
         * PATH FORM
         */
        pathMenu = new ListM (this, frame, Resources[62]);
        if (BUILD_VERSION == BUILD_MOBIMAP) {
            pathMenu.setLook (Design.COLOR_ROUTE_MENU_TITLE_BACKGROUND, Design.COLOR_MENU_TITLE_TEXT);
        }
        pathMenu.setComponentListener (this);
        pathMenu.addCommand (backCommand);
        pathMenu.setBackCommand (backCommand);
        pathMenu.append (Resources[83]);
        pathMenu.append (Resources[84]);
        pathMenu.append (Resources[85]);
        pathMenu.append (Resources[88]);

        /**
         * NAVIGATION FORM AKA 'MAP' MENU ITEM
         */
        if (BUILD_VERSION == BUILD_RADAR) {
            navigationMenu = new ListM (this, frame, Resources[56]);
            navigationMenu.setComponentListener (this);
            navigationMenu.addCommand (backCommand);
            navigationMenu.setBackCommand (backCommand);
            navigationMenu.append (Resources[109]);
            navigationMenu.append (Resources[76]);
            navigationMenu.append (Resources[57]);
            navigationMenu.append (Resources[58]);
            navigationMenu.append (Resources[60]);
            navigationMenu.append (Resources[195], null, Design.COLOR_COMPONENT_TEXT,
                                   Design.COLOR_OBJECT_MANAGER_ONLINE_MARKS);
        }

        /**
         * BALANCE MENU
         */
        if (BUILD_VERSION == BUILD_RADAR) {
            balanceMenu = new ListM (this, frame, Resources[119]);
            balanceMenu.setComponentListener (this);
            balanceMenu.addCommand (backCommand);
            balanceMenu.setBackCommand (backCommand);
            balanceMenu.append (Resources[120]);
            balanceMenu.append (Resources[137]);
        }

        /**
         * HELP MENU
         */
        if (BUILD_VERSION == BUILD_RADAR) {
            helpMenu = new LogoList (this, frame, Resources[8]);
        } else {
            helpMenu = new ListM (this, frame, Resources[8]);
            helpMenu.setLook (Design.COLOR_HELP_MENU_TITLE_BACKGROUND, Design.COLOR_MENU_TITLE_TEXT);
        }
        helpMenu.setComponentListener (this);
        helpMenu.addCommand (backCommand);
        helpMenu.setBackCommand (backCommand);
        helpMenu.append (Resources[113]);
        if (features[FEATURE_MANUAL]) {
            helpMenu.append (Resources[107]);
        }
        helpMenu.append (Resources[7]);
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            helpMenu.append (Resources[104]);
        }

        /**
         * OBJ LIST
         */
        objList = new ListM (this, frame, Resources[34]);
        objList.setComponentListener (this);

        /**
         * Device profiling
         */
        deviceProfiling ();

        /**
         * INFO ALERT
         */
        infoAlert = new Alert (msrt.Resources[11]);
        infoAlert.setCommandListener (this);
        infoAlert.setTimeout (Alert.FOREVER);

        /**
         * ALERT CONFIRM
         */
        alertConfirm = new Alert (msrt.Resources[11], "", null, AlertType.CONFIRMATION);
        alertConfirm.addCommand (okCommand);
        alertConfirm.addCommand (cancelCommand);
        alertConfirm.setTimeout (Alert.FOREVER);
        alertConfirm.setCommandListener (this);

        expire = (profile[PROFILE_EXPIRE] == null) ? (long) Long.MAX_VALUE : Long.parseLong (profile[PROFILE_EXPIRE]);

        //System.out.println("Active threads (startup): " + Thread.activeCount());
    }

    //
    //  HEAVY-WEIGHT COMPONENTS EVENT HANDLER
    //

    /**
     * Handle commands
     * @param c command to handle
     * @param s destination of command
     */
    public void commandAction (Command c, Displayable s) {
        /**
         * COMMAND 'OK'
         */
        if (s == layersForm) {
            if (c == okCommand) {
                layersChoice.getSelectedFlags (theMap.layerFlag);
            }
        }

        /**
         * CONFIG FORM
         */
        if (s == configForm) {
            if (c == okCommand) {
                int kl = keyboardLayoutChoice.getSelectedIndex ();
                if (BUILD_VERSION == BUILD_RADAR && kl >= 2)
                    kl++; // correct layout for TMS version
                theSearchEngine.setKeyboardLayout (kl);
                config[CONFIG_KEYBOARD_LAYOUT] = kl;
                config[CONFIG_MAP_DETAILS] = mapDetailChoice.getSelectedIndex ();
                config[CONFIG_LETTER_SPACING] = namesAmountChoice.getSelectedIndex ();
                kl = searchModeChoice.getSelectedIndex ();
                theSearchEngine.setMode (kl);
                config[CONFIG_SEARCH_MODE] = kl;
                config[CONFIG_PALETTE] = kl = paletteChoice.getSelectedIndex ();
                Map.colorsOffset = kl * Map.COLORS_COUNT;
                config[CONFIG_MAP_FONT] = mapFontChoice.getSelectedIndex ();
                config[CONFIG_UI_FONT] = uiFontChoice.getSelectedIndex ();
                config[CONFIG_TIME] = timeChoice.getSelectedIndex ();
                SunDial.setup (config[CONFIG_TIME] == 0);
                frame.updateParameters (config[CONFIG_UI_FONT]);
            } else
                System.arraycopy (configCopy, 0, config, 0, config.length);
        }

        if (s == autoUpdaterForm) {
            if (c == okCommand) {
                config[CONFIG_AUTOUPDATE] = autoUpdateChoice.getSelectedIndex ();
            }
        }

        if (s == alertConfirm) {
            if (c == okCommand) {
                theObjectManager.serverRemoveMark ();
            }
            display.setCurrent (getFrame ());
            return;
        }

        // back to previous display or component (if display is Frame)
        changeDisplayBack ();
    }

    //
    //  COMPONENT EVENTS HANDLER
    //

    /**
     * Listener for component events
     * @param component Component
     * @param command Command
     */
    public void commandAction (Component component, Command command) {
        boolean showMap = false;
        boolean changeNavigator = true;
        boolean newNavigatorState = false;

        // splash-screen
        if (component == progressScreen) {
            theMap = null;
            theDataLoader.stop ();
            destroyApp (true);
        }

        // objList context menu
        if (component == objList) {
            if (command == pathCommand || command == startCommand || command == infoCommand
                || command == selectCommand || command == gotoCommand || command == dirCommand
                || command == getAddressesCommand || command == showAddressesCommand || command == removeMarkCommand) {
                theMap.changeFocus = (command == gotoCommand) || (command == infoCommand);
                int i = objList.getSelectedIndex ();
                if (theObjectManager.select (theObjectManager.objListContents[i])) {
                    showMap = true;
                    changeNavigator = false;

                    if (command == pathCommand) {
                        showMap = pathFinderGo (Map.hasRoadDirs);
                    } else if (command == startCommand) {
                        showMap = pathFinderSetStart ();
                    } else if (command == infoCommand) {
                        pointInfo ();
                        showMap = false;
                    } else if (command == getAddressesCommand) {
                        theObjectManager.serverGetAddresses ();
                        showMap = false;
                    } else if (command == showAddressesCommand) {
                        Map.curCategory = Map.CATEGORY_SYSTEM_STREET_ADDRESSES;
                        theObjectManager.createAndShowObjList (null);
                        showMap = false;
                    } else if (command == removeMarkCommand) {
                        alertConfirm.setString (msrt.Resources[130] + theObjectManager.getLabelFullName (
                            Map.currentObjectItem >> 16, Map.currentObjectItem & 0xffff));
                        display.setCurrent (alertConfirm);
                        showMap = false;
                    }
                }
            } else if (command == getMarksCommand) {
                theObjectManager.serverGetMarks (false);
                showMap = false;
            }
        }
        // map context menu
        else if (component == theMap) {
            showMap = false;
            changeNavigator = false;
            boolean np = false;
            if (command == pathCommand) {
                pathFinderGo (Map.hasRoadDirs);
                np = true;
            } else if (command == startCommand) {
                pathFinderSetStart ();
                np = true;
            } else if (command == infoCommand) {
                pointInfo ();
            } else if (command == getAddressesCommand) {
                theObjectManager.serverGetAddresses ();
            } else if (command == showAddressesCommand) {
                Map.curCategory = Map.CATEGORY_SYSTEM_STREET_ADDRESSES;
                theObjectManager.createAndShowObjList (theMap);
            } else if (command == removeMarkCommand) {
                alertConfirm.setString (msrt.Resources[130] + theObjectManager.getLabelFullName (
                    Map.currentObjectItem >> 16, Map.currentObjectItem & 0xffff));
                display.setCurrent (alertConfirm);
            } else if (command == subscriberListCommand) {
                if (BUILD_VERSION == BUILD_RADAR) {
                    if (onlineConnected) {
                        theLocator.showLocatorList ();
                    } else {
                        onlineRequestStartup ();
                    }
                }
            }

            if (np) {
                theMap.needpaint = theMap.update = true;
                theMap.repaint ();
            }
        }

        // item is selected
        if (command == selectCommand) {
            if (component == mainMenu) {
                int i = mainMenu.getSelectedIndex ();
                i = mainMenuXRef[i];

                switch (i) {
                    case MNU_SEARCH:
                        theSearchEngine.show ();
                        break;
                    case MNU_OBJECTS:
                        theMap.curCategory = Map.CATEGORY_ROOT;
                        theObjectManager.createAndShowObjList (mainMenu);
                        break;
                    case MNU_MAP:
                        if (BUILD_VERSION == BUILD_MOBIMAP) {
                            showMap = true;
                        } else {
                            changeDisplay (DISPLAY_NAVIGATION_MENU);
                        }
                        break;
                    case MNU_LAYERS:
                        showLayersForm ();
                        break;
                    case MNU_ROUTE:
                        changeDisplay (DISPLAY_ROUTE_MENU);
                        break;
                    case MNU_BROWSER:
                        if (BUILD_VERSION == BUILD_RADAR)
                            theBrowser.homepage ();
                        changeDisplay (DISPLAY_BROWSER);
                        break;
                    case MNU_LOCATOR:
                        if (BUILD_VERSION == BUILD_RADAR) {
                            if (onlineConnected) {
                                theLocator.showLocatorList ();
                            } else {
                                onlineRequestStartup ();
                            }
                        }
                        break;
                    case MNU_BALANCE:
                        changeDisplay (DISPLAY_BALANCE_MENU);
                        break;
                    case MNU_CONFIG:
                        setupConfigForm ();
                        changeDisplay (DISPLAY_CONFIG);
                        break;
                    case MNU_HELP:
                        changeDisplay (DISPLAY_HELP_FORM);
                        break;
                    case MNU_EXIT:
                        destroyApp (true);
                        break;
                    case MNU_ADV:
                        theBrowser.page (profile[PROFILE_ADV_LINK_TYPE],
                                         profile[PROFILE_ADV_LINK]);
                        changeDisplay (DISPLAY_BROWSER);
                        break;
                    case MNU_ADV2:
                        theBrowser.page (profile[PROFILE_ADV2_LINK_TYPE],
                                         profile[PROFILE_ADV2_LINK]);
                        changeDisplay (DISPLAY_BROWSER);
                        break;
                    case MNU_DEBUG:
                        showInfo (ALERT_INFO, DEBUG_LOG);
                        break;
                }
            }
            // help submenu
            else if (component == helpMenu) {
                int ix = helpMenu.getSelectedIndex ();
                if (!features[FEATURE_MANUAL] && ix > 0)
                    ix++;
                switch (ix) {
                    case 0:
                        theBrowser.helppage ();
                        changeDisplay (DISPLAY_BROWSER);
                        break;
                    case 1:
                        theBrowser.manual ();
                        changeDisplay (DISPLAY_BROWSER);
                        break;
                    case 2:
                        theBrowser.pageAbout ();
                        changeDisplay (DISPLAY_BROWSER);
                        break;
                    case 3:
                        try {
                            if (BUILD_VERSION == BUILD_RADAR && profile[PROFILE_FEEDBACK] != null)
                                platformRequest (profile[PROFILE_FEEDBACK]);
                        } catch (ConnectionNotFoundException ex) {
                        }
                        break
                            ;
                }
            } else if (component == pathMenu) {
                showMap = true;
                int r = PathFinder.RESULT_OK;
                int ix = pathMenu.getSelectedIndex ();
                if (!theMap.hasRoadDirs && ix >= 2)
                    ix++;
                switch (ix) {
                    case 0:
                        showMap = pathFinderSetStart ();
                        break;
                    case 1:
                        showMap = pathFinderGo (false);
                        break;
                    case 2:
                        showMap = pathFinderGo (true);
                        break;
                    case 3:
                        showInfo (ALERT_INFO, PathFinder.getInfo ());
                        showMap = false;
                        break;
                }
                if (r != PathFinder.RESULT_OK) {
                    showInfo (ALERT_INFO, PathFinder.resultCodeDescription (r));
                    showMap = false;
                }
            } else if (component == navigationMenu) {
                showMap = true;
                switch (navigationMenu.getSelectedIndex ()) {
                    case 1:
                        newNavigatorState = true;
                        break;
                    case 2:
                        Map.savedX = Map.originX;
                        Map.savedY = Map.originY;
                        Map.savedScale = Map.scale;
                        break;
                    case 3:
                        Map.originX = Map.savedX;
                        Map.originY = Map.savedY;
                        Map.scale = Map.savedScale;
                        break;
                    case 4:
                        theMap.showWholeCity ();
                        break;
                    case 5:
                        changeDisplay (DISPLAY_AUTOUPDATER);
                        showMap = false;
                        break;
                }
            } else if (component == balanceMenu) {
                switch (balanceMenu.getSelectedIndex ()) {
                    case 0:
                        sendBalanceMessage ();
                        break;
                    case 1:
                        onlineRequestBalance ();
                        break;
                }
            }
        }
        // back command
        else if (command == backCommand) {
            if (component == objList)
                theObjectManager.back ();
            else
                changeDisplayBack ();
        }
        // menu command
        else if (command == menuCommand || command == backToMenuCommand) {
            changeDisplay (DISPLAY_MENU);
        } else if (command == mapCommand || command == backToMapCommand) {
            showMap = true;
        }

        if (showMap) {
            if (changeNavigator)
                theMap.setNavigator (newNavigatorState);
            changeDisplay (DISPLAY_MAP);
        }
    }

    /**
     * Calls PathFinder.go and shows resulting message
     * @param useQualityAndDirection boolean
     * @return boolean
     */
    private boolean pathFinderGo (boolean useQualityAndDirection) {
        boolean showMap = false;

        if (Map.currentObject == 0) {
            showInfo (ALERT_INFO, 86);
        } else {
            int r = PathFinder.go (Map.currentObject, useQualityAndDirection,
                                   useQualityAndDirection);

            if (r == PathFinder.RESULT_OK) {
                showInfo (ALERT_INFO, PathFinder.getInfo ());
                showMap = true;
            } else {
                showInfo (ALERT_INFO, PathFinder.resultCodeDescription (r));
            }
        }
        return showMap;
    }

    /**
     * Calls PathFinder.setStart and shows error in a case of error
     * @return boolean
     */
    private boolean pathFinderSetStart () {
        boolean showMap = false;
        if (Map.currentObject == 0) {
            showInfo (ALERT_INFO, 86);
        } else {
            int r = PathFinder.setStart (Map.currentObject);

            if (r == PathFinder.RESULT_OK)
                showMap = true;
            else
                showInfo (ALERT_INFO, PathFinder.resultCodeDescription (r));
        }
        return showMap;
    }

    /**
     * Launch map
     */
    public void launchMap () {
        // map
        theMap = new Map ();
        theMap.setComponentListener (this);

        // data loader
        theDataLoader = new DataLoader (this, theMap, params);

        // splash-screen
        progressScreen = new ProgressScreen (frame, "/i/l", mapName, regName,
                                             profile[PROFILE_PROGRESS]);
        progressScreen.setComponentListener (this);
        frame.setComponent (progressScreen);
        display.setCurrent (frame);

        new Thread (theDataLoader).start ();
    }

    /**
     * Called by DataLoader after all data had been loaded
     */
    void dataIsLoaded () {
        theMap.init (config[0], config[1], config[2]);

        if (!isFirstTime) {
            // try to load map state. if load has failed, isFirstTime flag remains true
            loadMapState ();
        }

        if (isFirstTime) {
            saveMapStateFirstTime ();
        }

        /**
         * START UP MODULES...
         */
        theObjectManager = new ObjectManager (this);
        theBrowser = new Browser (this, frame);

        Component locatorComponent = null;
        if (BUILD_VERSION == BUILD_RADAR) {
            theLocator = new Locator (this);
            locatorComponent = theLocator.getComponent ();
        }

        /**
         * Init list of light-weight components
         */
        components = new Component[] {null, mainMenu, theMap, theSearchEngine, objList,
                     theBrowser, null, null, null, locatorComponent,
                     helpMenu, pathMenu, navigationMenu, balanceMenu};

        displays = new Displayable[] {null, layersForm, configForm, infoAlert, autoUpdaterForm};

        setupConfigForm ();
        theBrowser.init ();

        timeWhenStarted = System.currentTimeMillis ();

        if (BUILD_VERSION == BUILD_RADAR) {
            onlineRequestStartup ();
        } else {
            if (timeWhenStarted > expire) {
                theBrowser.page (profile[PROFILE_EXPIRE_LINK_TYPE],
                                 profile[PROFILE_EXPIRE_LINK]);
                changeDisplay (DISPLAY_BROWSER);
            } else {
                if (profile[PROFILE_START_LINK_TYPE] == null) {
                    changeDisplay (DISPLAY_MENU);
                } else {
                    theBrowser.page (profile[PROFILE_START_LINK_TYPE],
                                     profile[PROFILE_START_LINK]);
                    changeDisplay (DISPLAY_BROWSER);
                }
            }
        }

        statistics[STATISTICS_LAUNCH_COUNTER]++;
    }

    /**
     * Changes display from current to new one.
     * @param newDisplay new display
     */
    public void changeDisplay (Displayable newDisplay) {
        Displayable cur = display.getCurrent ();
        if (newDisplay != cur) {
            previousDisplay[previousP] = cur;
            if (cur instanceof Frame)
                previousComponent[previousP] = ((Frame) cur).getComponent ();
            previousP = (previousP + 1) % PREVIOUS_LIMIT;

            display.setCurrent (newDisplay);
        }
    }

    /**
     * Changes display to the one specified by code DISPLAY_*.
     * If display is the same as current one nothing is changed.
     * Previous display/component are stored.
     * @param code byte
     */
    public static void changeDisplay (byte code) {
        if (code < 0) {
            changeDisplayInternal (displays[ -code], null);
        } else {
            changeDisplay (components[code]);
        }
    }

    public static void changeDisplay (Component newComponent) {
        changeDisplayInternal (frame, newComponent);
    }

    private static void changeDisplayInternal (Displayable newDisplay, Component newComponent) {
        Displayable curDisplay = display.getCurrent ();
        Component curComponent = null;
        if (curDisplay.equals (frame)) {
            curComponent = frame.getComponent ();
        }

        if (newDisplay != curDisplay || newComponent != curComponent) {
            previousDisplay[previousP] = curDisplay;
            previousComponent[previousP] = curComponent;
            previousP = (previousP + 1) % PREVIOUS_LIMIT;
            display.setCurrent (newDisplay);
            if (newComponent != null) {
                frame.setComponent (newComponent);
            }
        }
    }

    /**
     * Changes display to previous one
     */
    public static void changeDisplayBack () {
        previousP = (previousP + PREVIOUS_LIMIT - 1) % PREVIOUS_LIMIT;
        Displayable disp = previousDisplay[previousP];
        display.setCurrent (disp);
        if (disp instanceof Frame) {
            Component c = previousComponent[previousP];
            if (!(c instanceof ProgressScreen))
                ((Frame) disp).setComponent (c);
        }
    }


    /**
     * Decoding function. Calculates a linear permutation on int
     * @param r input
     * @return output
     */
    int random (int r) {
        return (int) (((long) r * keyA + keyB) & 0x7fffffff);
    }

    /**
     * Send SMS
     */
    private void sendBalanceMessage () {
        if (BUILD_VERSION != BUILD_RADAR) {
            return;
        }

        try {
            char[] buf = new char[80];
            InputStreamReader reader = new InputStreamReader (getClass ().getResourceAsStream ("/sms"));
            int bytesRead = reader.read (buf, 0, buf.length);

            String a = new String (buf, 0, bytesRead);
            int p = a.indexOf (';');
            String number = a.substring (0, p);
            String message = a.substring (p + 1);

            p = message.indexOf ("$key");
            if (p >= 0) {
                message = message.substring (0, p) + ownerKey + message.substring (p + 4);
            }

            sendMessage (number, message);
        } catch (Exception nested) {
        }
    }

    public void sendMessage (String address, String text) {
        if (supportSms) {
            theSMSSender = new SMSSender (this);
            theSMSSender.send (address, text);
        }
    }

    /**
     * Load point info
     */
    private void pointInfo () {
        if (Map.currentObjectClass == Map.CLASS_LABEL && Map.currentObjectItem >= 0) {
            int seg = Map.currentObjectItem >> 16;
            int id = Map.currentObjectItem & 0xffff;

            int uid = Map.lbUid[seg][id];
            int ctid = Map.lbct[seg][id];

            theBrowser.pageInfo (Map.ctLocalToRemote.get (new Integer (ctid)), uid);
        }
    }

    public Frame getFrame () {
        return frame;
    }

    public static void log (String s) {
        DEBUG_LOG += s + "\n";
    }

    //
    //    H E L P   A N D   A B O U T
    //
    public void showInfo (byte type, int msg) {
        showInfo (type, Resources[msg]);
    }

    public void showInfo (byte type, String msg) {
        infoAlert.setType (type == ALERT_INFO ? AlertType.INFO : AlertType.ERROR);
        infoAlert.setTitle (Resources[type]);
        infoAlert.setString (msg);
        changeDisplay (DISPLAY_INFO);
    }

    //
    //    L A Y E R S    F O R M
    //
    void createLayersForm () {
        layersChoice = theMap.generateLayers ();

        layersForm.deleteAll ();
        layersForm.append (layersChoice);
    }

    void showLayersForm () {
        layersChoice.setSelectedFlags (theMap.layerFlag);

        changeDisplay (DISPLAY_LAYERS);
    }

    //
    //    S T A R T U P
    //
    private void startUp () {
        // read map information
        try {
            InputStream is = getClass ().getResourceAsStream (DataLoader.file_city);
            DataInputStream inLow = new DataInputStream (is);

            // low level
            int len = inLow.readShort ();
            keyA = inLow.readShort ();
            keyB = inLow.readShort ();
            byte[] arr = new byte[len];
            DataLoader.readStreamAsByteArray (inLow, arr, len);

            inLow.close ();

            // data is encrypted from the end to the beginning, thus making the whole stream consistent

            int hardKey = HARDKEY; // version-dependant hardkey

            do {
                len--;
                arr[len] = (byte) (arr[len] ^ hardKey);
                hardKey = random (arr[len]);
            } while (len > 0);

            ByteArrayInputStream bais = new ByteArrayInputStream (arr);
            DataInputStream in = new DataInputStream (bais);

            String title = in.readUTF ();
            mapName = new String (title);

            //System.out.println(mapName);

            // 2 dummy fields (were used in 1.x & M:2)
            in.readShort ();
            in.readInt ();

            cityId = in.readShort ();
            userIdHigh = in.readShort ();
            userIdLow = in.readShort ();

            rmsSuffix = PROGRAM_BUILD + (random (userIdHigh) & 0x7fff) + (random (userIdLow) & 0x7fff);

            paramsN = in.readShort () + 1;
            params = new int[paramsN];

            for (int i = 0; i < paramsN; i++) {
                params[i] = in.readInt ();
            }

            int regInfoN = in.readShort ();
            if (regInfoN > 0 && BUILD_VERSION != BUILD_RADAR) {
                String[] regData = new String[regInfoN];

                /**
                 * REG DATA
                 * [0] = builder license name
                 * [1] = builder license number
                 * [2] = reg first name
                 * [3] = reg last name
                 * [4] = reg license
                 */

                for (int i = 0; i < regInfoN; i++) {
                    regData[i] = in.readUTF ();
                }

                if (regInfoN >= 2) {
                    String buildLicense = Resources[12] + regData[0] + ". " +
                                          Resources[117] + " " + regData[1];
                    if (msrt.profile[msrt.PROFILE_MAP_INFO] == null)
                        msrt.profile[msrt.PROFILE_MAP_INFO] = buildLicense;
                    else
                        msrt.profile[msrt.PROFILE_MAP_INFO] += "\n" + buildLicense;
                }

                if (regInfoN >= 5) {
                    regFirstName = regData[2];
                    regLastName = regData[3];
                    regName = regFirstName + ' ' + regLastName;
                    regInfo = Resources[118] + regName + ". ";

                    regLicense = regData[4];
                    regInfo += Resources[117] + ": " + regLicense + "\n";
                }
            }

            in.close ();

        } catch (java.io.IOException x) {
        }

        // if there's no RMS records then program is launched for the first time
        isFirstTime = true;
        try {
            RecordStore store = RecordStore.openRecordStore ("mobimap3" + rmsSuffix, true);
            if (store.getNumRecords () > 0) {
                isFirstTime = false;
            }
        } catch (RecordStoreException ex) {
        }

        // get default values from .city file
        config[0] = params[10];
        config[1] = params[11];
        config[2] = params[12];

        config[CONFIG_UI_FONT] = system == SYSTEM_SYMBIAN ? 0 : 1; // by def. small font for symbian only

        // read owner key, if any
        if (BUILD_VERSION == BUILD_RADAR) {
            try {
                char[] buf = new char[32];
                InputStreamReader reader = new InputStreamReader (getClass ().getResourceAsStream ("/owner.key"));
                reader.read (buf, 0, buf.length);
                ownerKey = new String (buf);
            } catch (Exception nested) {
                ownerKey = null;
            }
        }
    }

    /**
     * Sets up Options dialog
     */
    private void setupConfigForm () {
        System.arraycopy (config, 0, configCopy, 0, config.length);

        mapDetailChoice.setSelectedIndex (config[CONFIG_MAP_DETAILS], true);
        namesAmountChoice.setSelectedIndex (config[
                                            CONFIG_LETTER_SPACING], true);
        searchModeChoice.setSelectedIndex (config[CONFIG_SEARCH_MODE], true);
        theSearchEngine.setMode (config[CONFIG_SEARCH_MODE]);
        int kl = config[CONFIG_KEYBOARD_LAYOUT];
        if (BUILD_VERSION == BUILD_RADAR && kl >= 2) {
            kl--;
        }
        keyboardLayoutChoice.setSelectedIndex (kl, true);
        theSearchEngine.setKeyboardLayout (kl);
        Map.colorsOffset = Map.COLORS_COUNT * config[CONFIG_PALETTE];
        paletteChoice.setSelectedIndex (config[CONFIG_PALETTE], true);
        mapFontChoice.setSelectedIndex (config[CONFIG_MAP_FONT], true);
        uiFontChoice.setSelectedIndex (config[CONFIG_UI_FONT], true);
        frame.updateParameters (config[CONFIG_UI_FONT]);

        timeChoice.setSelectedIndex (config[CONFIG_TIME], true);
        SunDial.calculateDates ();
        timeChoice.set (0, SunDial.timeToString (SunDial.hour, SunDial.min), null);
        timeChoice.set (1, SunDial.timeToString ((SunDial.hour + SunDial.timeZoneOffset) % 24, SunDial.min), null);
        SunDial.setup (msrt.config[msrt.CONFIG_TIME] == 0);

        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            autoUpdateChoice.setSelectedIndex (config[CONFIG_AUTOUPDATE], true);
        }
    }

    //
    //   M A P   S T A T E   S T O R A G E
    //

    private static final String RMS_CONFIG = "config";
    private static final String RMS_STAT = "stat";
    private static final String RMS_LAYERS = "layers";

    /**
     * Load map state
     */
    private void loadMapState () {
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore ("mobimap3" + rmsSuffix, true);

            if (store.getNumRecords () > 0) {
                byte bf[] = store.getRecord (1);

                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream (bf);
                    DataInputStream dis = new DataInputStream (bais);

                    Hashtable data = (Hashtable) OnlineLoader.readObject (dis);
                    copyVectorIntoArray ((Vector) data.get (RMS_CONFIG), config);
                    copyVectorIntoArray ((Vector) data.get (RMS_STAT), statistics);
                    theMap.loadLayersState ((Hashtable) data.get (RMS_LAYERS));

                    dis.close ();

                    isFirstTime = false;
                    Map.savedX = Map.originX = config[0];
                    Map.savedY = Map.originY = config[1];
                    Map.savedScale = Map.scale = config[2];
                } catch (IOException ex2) {
                }
            }

            store.closeRecordStore ();
        } catch (RecordStoreException ex) {
            if (store != null) {
                try {
                    store.closeRecordStore ();
                } catch (RecordStoreException ex1) {
                }
            }
            isFirstTime = true;
        } catch (RuntimeException re) {
            isFirstTime = true;
        }
    }

    private void copyVectorIntoArray (Vector src, int[] dest) {
        for (int i = 0; i < src.size () && i < dest.length; i++) {
            dest[i] = ((Integer) src.elementAt (i)).intValue ();
        }
    }

    /**
     * Save map state into
     * @return byte[]
     */
    private byte[] saveMapStateToBinaryStream () {
        config[0] = Map.originX;
        config[1] = Map.originY;
        config[2] = Map.scale;

        ByteArrayOutputStream baos = new ByteArrayOutputStream (CONFIG_N * 4 + theMap.layerN + STATISTICS_N * 4);

        try {
            Hashtable data = new Hashtable ();
            data.put (RMS_CONFIG, config);
            data.put (RMS_STAT, statistics);
            data.put (RMS_LAYERS, theMap.saveLayersState ());

            DataOutputStream dos = new DataOutputStream (baos);
            OnlineLoader.writeObject (dos, data);

            dos.close ();
        } catch (IOException ex) {
        }
        return baos.toByteArray ();
    }

    /**
     * Saves map state for the first time
     */
    private void saveMapStateFirstTime () {
        // change default keyboard layout for siemens phones
        if (vendor == VENDOR_SIEMENS && config[CONFIG_KEYBOARD_LAYOUT] == SearchEngine.LAYOUT_CYRILLIC) {
            config[CONFIG_KEYBOARD_LAYOUT] = SearchEngine.LAYOUT_ALTERNATIVE;
        }

        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore ("mobimap3" + rmsSuffix, true);

            byte[] data = saveMapStateToBinaryStream ();
            store.addRecord (data, 0, data.length);
            store.closeRecordStore ();
        } catch (RecordStoreException ex) {
            if (store != null) {
                try {
                    store.closeRecordStore ();
                } catch (RecordStoreException ex1) {
                }
            }
        }
    }

    /**
     * Saves map state
     * @param _ox originx
     * @param _oy originy
     * @param _scale scale
     */
    void saveMapState (int _ox, int _oy, int _scale) {
        config[0] = _ox;
        config[1] = _oy;
        config[2] = _scale;
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore ("mobimap3" + rmsSuffix, true);
            byte[] data = saveMapStateToBinaryStream ();
            store.setRecord (1, data, 0, data.length);

            store.closeRecordStore ();
        } catch (RecordStoreException ex) {
            if (store != null) {
                try {
                    store.closeRecordStore ();
                } catch (RecordStoreException ex1) {
                }
            }
        }
    }

    //
    //      O N L I N E    F U N C T I O N S
    //

    private static final String REQUEST_CORE_BUILD = "core.build";
    private static final String REQUEST_CORE_KEY = "core.key";
    private static final String REQUEST_CORE_CITY_ID = "core.cityId";
    private static final String REQUEST_CORE_USER_ID_HIGH = "core.userIdHigh";
    private static final String REQUEST_CORE_USER_ID_LOW = "core.userIdLow";
    private static final String REQUEST_CORE_LOCALE = "core.locale";
    private static final String REQUEST_CORE_STATISTICS = "core.stat";
    private static final String REQUEST_CORE_MICROEDITION_PLATFORM = "core.platform";
    private static final String REQUEST_CORE_VENDOR = "core.vendor";
    private static final String REQUEST_MAP_VISIBLE_BOUNDS = "map.visibleBounds";
    private static final String REQUEST_PARAMETER_WIDTH = "browser.width";
    private static final String REQUEST_PARAMETER_HEIGHT = "browser.height";

    /**
     * Create new OnlineLoader and add root parameters
     * @param method String
     * @param isSilent boolean
     * @param isInBackground boolean
     * @return OnlineLoader
     */
    public OnlineLoader createOnlineLoader (String method, boolean isSilent, boolean isInBackground) {
        theOnlineLoader = new OnlineLoader (this, profile[PROFILE_SERVLET], frame, isSilent, isInBackground);

        theOnlineLoader.setMethod (method);

        // core parameters
        theOnlineLoader.addParameter (REQUEST_CORE_BUILD, PROGRAM_BUILD);
        theOnlineLoader.addParameter (REQUEST_CORE_KEY, ownerKey);
        theOnlineLoader.addParameter (REQUEST_CORE_CITY_ID, cityId);
        theOnlineLoader.addParameter (REQUEST_CORE_USER_ID_HIGH, userIdHigh);
        theOnlineLoader.addParameter (REQUEST_CORE_USER_ID_LOW, userIdLow);
        theOnlineLoader.addParameter (REQUEST_CORE_LOCALE, profile[PROFILE_LOCALE]);
        theOnlineLoader.addParameter (REQUEST_CORE_STATISTICS, statistics);
        theOnlineLoader.addParameter (REQUEST_CORE_MICROEDITION_PLATFORM, microeditionPlatform);
        theOnlineLoader.addParameter (REQUEST_CORE_VENDOR, vendor);
        theOnlineLoader.addParameter (REQUEST_MAP_VISIBLE_BOUNDS,
                                      new int[] {Map.y2latitude (Map.ymin), Map.x2longitude (Map.xmin),
                                      Map.y2latitude (Map.ymax), Map.x2longitude (Map.xmax)});
        theOnlineLoader.addParameter (REQUEST_PARAMETER_WIDTH, frame.getWidth ());
        theOnlineLoader.addParameter (REQUEST_PARAMETER_HEIGHT, frame.getHeight ());

        return theOnlineLoader;
    }

    private void onlineRequestBalance () {
        OnlineLoader ol = createOnlineLoader (OnlineLoader.METHOD_GET_USER_BALANCE_TEXT, false, false);
        ol.go ();
    }

    private void onlineRequestStartup () {
        OnlineLoader ol = createOnlineLoader (OnlineLoader.METHOD_ON_START, false, false);
        ol.setOnlineLoaderListener (this);
        ol.go ();
    }

    public void serverRequestComplete (int errorCode, OnlineLoader onlineLoader) {
        if (BUILD_VERSION == BUILD_RADAR) {
            if (OnlineLoader.METHOD_ON_START.equals (onlineLoader.getMethod ())) {
                onlineConnected = errorCode == OnlineLoaderListener.CODE_OK;

                if (profile[PROFILE_START_LINK_TYPE] == null) {
                    changeDisplay (DISPLAY_MENU);
                } else {
                    theBrowser.page (profile[PROFILE_START_LINK_TYPE],
                                     profile[PROFILE_START_LINK]);
                    changeDisplay (DISPLAY_BROWSER);
                }
            }
        }
    }

    //
    //      D E V I C E   P R O F I L I N G
    //
    public static final byte VENDOR_UNKNOWN = 0;
    public static final byte VENDOR_NOKIA = 0x10;
    public static final byte VENDOR_MOTOROLA = 0x20;
    public static final byte VENDOR_SIEMENS = 0x30;
    public static final byte VENDOR_SONYERICSSON = 0x40;
    public static final byte VENDOR_SAMSUNG = 0x50;
    public static final byte VENDOR_WINDOWS = 0x60;
    public static final byte VENDOR_PALM = 0x70;

    public static final byte SYSTEM_UNKNOWN = 0;
    public static final byte SYSTEM_SYMBIAN = 0x10;
    public static final byte SYSTEM_WINDOWS = 0x20;

    public static String microeditionPlatform = null;
    public static byte vendor;
    public static byte system;

    public boolean supportSms;
    public boolean supportMMAPI;

    private void deviceProfiling () {
        // find out vendor
        microeditionPlatform = getAppProperty ("microedition.platform");
        microeditionPlatform = microeditionPlatform != null ?
                               microeditionPlatform.toLowerCase () : "";

        if (microeditionPlatform.indexOf ("nokia") != -1 ||
            microeditionPlatform.equals ("siemens_sx1") ||
            microeditionPlatform.equals ("sendo x") ||
            microeditionPlatform.equals ("panasonic x700")) {
            vendor = VENDOR_NOKIA;
        } else if (microeditionPlatform.indexOf ("palm") != -1) {
            vendor = VENDOR_PALM;
        } else if (checkClass ("com.siemens.mp.lcdui.Image")) {
            vendor = VENDOR_SIEMENS;
        } else if (microeditionPlatform.indexOf ("sonyericsson") >= 0 ||
            getAppProperty ("com.sonyericsson.IMEI") != null || getAppRoperty ("com.sonyericsson.imei") != null) {
            vendor = VENDOR_SONYERICSSON;
        } else if (checkClass ("com.motorola.phonebook.PhoneBookRecord") ||
            checkClass ("com.motorola.Dialer") || checkClass ("com.motorola.phone.Dialer") ||
            checkClass ("com.motorola.graphics.j3d.Light") ||
            checkClass ("com.motorola.lwt.ComponentScreen") ||
            checkClass ("com.motorola.game.GameScreen") ||
            checkClass ("com.motorola.funlight.FunLight") ||
            checkClass ("com.motorola.multimedia.Lighting")) {
            vendor = VENDOR_MOTOROLA;
        } else if (checkClass ("com.samsung.util.AudioClip") || checkClass ("com.samsung.util.LCDLight")
            || checkClass ("com.samsung.util.Vibration")) {
            vendor = VENDOR_SAMSUNG;
        }

        if (microeditionPlatform.indexOf ("windows") >= 0) {
            system = SYSTEM_WINDOWS;
        } else if (microeditionPlatform.indexOf ("symbian") >= 0) {
            system = SYSTEM_SYMBIAN;
        }

        // sms support
        supportSms = checkClass ("javax.wireless.messaging.MessageConnection") &&
                     checkClass ("javax.wireless.messaging.TextMessage") && vendor != VENDOR_SAMSUNG;

        // MMAPI support
        supportMMAPI = checkClass ("javax.microedition.media.Manager");

        log (vendor + " " + system);
    }

    /**
     * Get system property with specified name
     * @param propertyName String
     * @return String
     */
    private String getAppRoperty (String propertyName) {
        try {
            return System.getProperty (propertyName);
        } finally {
            return null;
        }
    }

    /**
     * Check, if specified class exists
     * @param className String
     * @return boolean
     */
    private boolean checkClass (String className) {
        try {
            Class.forName (className);
            return true;
        } catch (Exception x) {
            return false;
        }
    }
}
