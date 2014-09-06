/*****************************************************************************/
/*                               R A D A R  3                                */
/*                       (c) 2003-2008 Ilya Shakhat                          */
/*                           (c) 2006-2008 TMS                               */
/*****************************************************************************/

package com.ish.mobimap.radar;

import java.io.*;
import java.util.*;
import javax.microedition.lcdui.*;

import com.ish.mobimap.net.*;
import com.ish.mobimap.ui.*;

import com.ish.mobimap.*;


/**
 * Mobile Objects Manager
 */
public class Locator implements CommandListener, ComponentListener, OnlineLoaderListener,
    Runnable, ListSelectionListener {
    private msrt parent;
    private Map theMap;

    /** List of mobile objects */
    private Vector locatorObjects;
    /** initial size of list */
    private final short locatorObjectsInitialAmount = 20;

    private Vector history;
    private Vector trackList;

    private final static int STATE_SELECTED = 0;
    private final static int STATE_UNSELECTED = 1;
    private final static int STATE_AWAITING = 2;
    private final static int STATE_REJECTED = 3;
    private final static int STATE_LIMIT = 4;

    private int curMobileIndex;
    private Mobile curMobile;
    private Mobile backupMobile;

    private Thread me;
    private boolean isWorking = false;
    private long delay = 0;
    private long lastTime;
    private static final int SLEEP_TOLERANCE = 10000;

    /** Command "Show all objects */
    static Command showOnMapCommand;
    static Command addCommand;
    static Command deleteCommand;
    static Command updateCommand;
    static Command optionsCommand;
    static Command clearCommand;
    static Command selectCommand;
    static Command unselectCommand;
    static Command selectAllCommand;
    static Command unselectAllCommand;
    static Command showZonesCommand;
    static Command addZoneCommand;
    static Command propertiesCommand;
    static Command setTrackingIntervalCommand;
    static Command loadTrackCommand;
    static Command menuCommand;

    private boolean isSwitchToMap;
    private boolean isFirstView;

    /** List of subscribers */
    private ListM locatorList;

    private Image[] abonentIcons;

    /**
     * phone number dialog
     */
    private Form phoneDialog;
    private TextField phoneNumberField;
    private TextField phoneNameField;

    /**
     * options dialog
     */
    private Form optionsDialog;
    private ChoiceGroup choiceTime;
    private ChoiceGroup choiceFollow;
    private ChoiceGroup choiceHistory;

    private static final int[] UPDATE_TIMES = {0, 1, 2, 3, 5, 10, 15, 30};
    private int prevTimeIndex;

    private Alert alertConfirm;
    private Alert infoAlert;

    private Form trackingDialog;
    private ChoiceGroup trackingInterval;

    private ZoneController zoneController;

    private Form loadTrackDialog;
    private DateField startDateField;
    private DateField endDateField;

    static final String REQUEST_PARAMETER_SELECTED = "locator.selected";
    static final String REQUEST_PARAMETER_CURSOR = "locator.cursor";
    static final String REQUEST_PARAMETER_NAME = "locator.name";
    static final String REQUEST_PARAMETER_MSISDN = "locator.msisdn";
    static final String REQUEST_PARAMETER_LBS_RETRIEVE = "locator.lbsRetrieve";
    static final String REQUEST_PARAMETER_INTERVAL = "locator.interval";
    static final String REQUEST_PARAMETER_BEGIN = "locator.timeBegin";
    static final String REQUEST_PARAMETER_END = "locator.timeEnd";
    static final String REQUEST_PARAMETER_ZONE_LIST = "locator.zoneList";

    static final String REQUEST_PARAMETER_ZONE_LAT = "lat";
    static final String REQUEST_PARAMETER_ZONE_LON = "lon";
    static final String REQUEST_PARAMETER_ZONE_RADIUS = "radius";
    static final String REQUEST_PARAMETER_ZONE_NAME = "name";

    /**
     * Mobile objects manager
     * @param parent msrt
     */
    public Locator (msrt parent) {
        this.parent = parent;
        this.theMap = Map.getInstance ();

        locatorObjects = new Vector (locatorObjectsInitialAmount);
        history = new Vector (locatorObjectsInitialAmount);
        trackList = new Vector (locatorObjectsInitialAmount);

        // commands
        showOnMapCommand = new Command (msrt.Resources[105], Command.SCREEN, 11);
        addCommand = new Command (msrt.Resources[124], Command.SCREEN, 12);
        deleteCommand = new Command (msrt.Resources[144], Command.SCREEN, 13);
        updateCommand = new Command (msrt.Resources[133], Command.SCREEN, 1);
        optionsCommand = new Command (msrt.Resources[132], Command.SCREEN, 30);
        clearCommand = new Command (msrt.Resources[149], Command.SCREEN, 20);
        selectCommand = new Command (msrt.Resources[134], Command.SCREEN, 20);
        unselectCommand = new Command (msrt.Resources[135], Command.SCREEN, 20);
        selectAllCommand = new Command (msrt.Resources[164], Command.SCREEN, 25);
        unselectAllCommand = new Command (msrt.Resources[165], Command.SCREEN, 25);
        showZonesCommand = new Command (msrt.Resources[169], Command.SCREEN, 15);
        addZoneCommand = new Command (msrt.Resources[188], Command.SCREEN, 15);
        propertiesCommand = new Command (msrt.Resources[172], Command.SCREEN, 15);
        setTrackingIntervalCommand = new Command (msrt.Resources[171], Command.SCREEN, 19);
        loadTrackCommand = new Command (msrt.Resources[183], Command.SCREEN, 18);
        menuCommand = new Command (msrt.Resources[73], Command.SCREEN, 100);

        // phone dialog
        phoneDialog = new Form (msrt.Resources[124]);
        phoneDialog.setCommandListener (this);
        phoneNumberField = new TextField (msrt.Resources[125], "", 20,
                                          TextField.PHONENUMBER);
        phoneDialog.append (phoneNumberField);
        phoneNameField = new TextField (msrt.Resources[126], "", 50, TextField.ANY);
        phoneDialog.append (phoneNameField);
        phoneDialog.addCommand (parent.backCommand);
        phoneDialog.addCommand (parent.okCommand);

        // options dialog
        optionsDialog = new Form (msrt.Resources[132]);
        choiceFollow = new ChoiceGroup (msrt.Resources[145], ChoiceGroup.MULTIPLE);
        choiceFollow.append (msrt.Resources[146], null);
        choiceFollow.setSelectedIndex (0, msrt.config[msrt.CONFIG_LOCATOR_MONITORING] == 0);
        choiceHistory = new ChoiceGroup (msrt.Resources[148], ChoiceGroup.MULTIPLE);
        choiceHistory.append (msrt.Resources[146], null);
        choiceHistory.setSelectedIndex (0, msrt.config[msrt.CONFIG_LOCATOR_HISTORY] == 0);
        choiceTime = new ChoiceGroup (msrt.Resources[70], ChoiceGroup.POPUP);
        choiceTime.append (msrt.Resources[138], null);
        for (int i = 1; i < UPDATE_TIMES.length; i++)
            choiceTime.append ("" + UPDATE_TIMES[i] + msrt.Resources[13], null);
        msrt.config[msrt.CONFIG_LOCATOR_UPDATE] = 0; // don't restore autoupdate
        choiceTime.setSelectedIndex (msrt.config[msrt.CONFIG_LOCATOR_UPDATE], true);
        optionsDialog.addCommand (parent.cancelCommand);
        optionsDialog.addCommand (parent.okCommand);
//        optionsDialog.append (choiceFollow);
        optionsDialog.append (choiceHistory);
        optionsDialog.append (choiceTime);
        optionsDialog.setCommandListener (this);

        // list of locator objects
        locatorList = new ListM (parent, parent.getFrame (), msrt.Resources[99]);
        locatorList.addCommand (updateCommand);
        locatorList.addCommand (addCommand);
        locatorList.addCommand (parent.mapCommand);
        locatorList.addCommand (menuCommand);
        locatorList.addCommand (optionsCommand);
        locatorList.setMasterCommand (null);
        locatorList.setBackCommand (menuCommand);
        locatorList.setComponentListener (this);
        locatorList.setSelectionListener (this);
        locatorList.setLook (Design.COLOR_LOCATOR_TITLE_BACKGROUND, Design.COLOR_LOCATOR_TITLE_TEXT);

        // zoneController
        zoneController = new ZoneController ();

        // set tracking interval dialog
        trackingDialog = new Form (msrt.Resources[178]);
        trackingInterval = new ChoiceGroup (msrt.Resources[176], ChoiceGroup.POPUP);
        trackingInterval.append (msrt.Resources[177], null);
        for (int i = 10; i <= 120; i += 10) {
            trackingInterval.append (i + msrt.Resources[13], null);
        }
        trackingDialog.append (trackingInterval);
        trackingDialog.append (msrt.Resources[179]);
        trackingDialog.addCommand (parent.backCommand);
        trackingDialog.addCommand (parent.okCommand);
        trackingDialog.setCommandListener (this);

        // load track dialog
        loadTrackDialog = new Form (msrt.Resources[180]);
        startDateField = new DateField (msrt.Resources[181], DateField.DATE);
        startDateField.setDate (new Date (System.currentTimeMillis () - 7 * 24 * 60 * 60 * 1000)); // week ago
        loadTrackDialog.append (startDateField);
        endDateField = new DateField (msrt.Resources[182], DateField.DATE);
        endDateField.setDate (new Date (System.currentTimeMillis () + 24 * 60 * 60 * 1000)); // end of today
        loadTrackDialog.append (endDateField);
        loadTrackDialog.addCommand (parent.backCommand);
        loadTrackDialog.addCommand (parent.okCommand);
        loadTrackDialog.setCommandListener (this);

        // state icons
        abonentIcons = new Image[6];
        for (char c = 'a'; c <= 'f'; c++) {
            try {
                abonentIcons[c - 'a'] = Image.createImage ("/i/" + c);
            } catch (IOException ex1) {
            }
        }

        // alert
        alertConfirm = new Alert (msrt.Resources[11], "", null, AlertType.CONFIRMATION);
        alertConfirm.addCommand (parent.okCommand);
        alertConfirm.addCommand (parent.cancelCommand);
        alertConfirm.setTimeout (Alert.FOREVER);

        infoAlert = new Alert (msrt.Resources[11]);
        infoAlert.setCommandListener (this);
        infoAlert.setTimeout (Alert.FOREVER);

//        isFirstLoad = true;
        me = new Thread (this);
        me.start ();
    }

    public void destroy () {
        isWorking = false;
    }

    public Component getComponent () {
        return locatorList;
    }

    /**
     * Switch the layer on
     */
    private void turnLayerOn () {
        Map.setLayer (Map.LAYER_LOCATOR, true);
    }

    /**
     * Show all objects
     */
    private void showAll () {
        turnLayerOn ();

        int xmax = 0, ymax = 0, xmin = 0xffff, ymin = 0xffff;
        boolean hasOne = false, hasOutCity = false;
        String outCityList = msrt.Resources[100];
        for (int i = 0; i < locatorObjects.size (); i++) {
            Mobile lo = ((Mobile) locatorObjects.elementAt (i));
            if (lo.state == STATE_SELECTED && lo.hasCoordinates) {
                if (lo.isInCity) {
                    xmax = Math.max (xmax, lo.x + lo.precision);
                    ymax = Math.max (ymax, lo.y + lo.precision);
                    xmin = Math.min (xmin, lo.x - lo.precision);
                    ymin = Math.min (ymin, lo.y - lo.precision);
                    hasOne = true;
                } else {
                    outCityList += " " + lo.name + " (" + lo.msisdn + ")";
                    hasOutCity = true;
                }
            }
        }
        if (hasOne && choiceFollow.isSelected (0))
            theMap.moveFocus (xmin, ymin, xmax, ymax, 5, true);

        if (hasOutCity && isFirstView) {
            infoAlert.setString (outCityList);
            parent.display.setCurrent (infoAlert);
        } else {
            parent.changeDisplay (msrt.DISPLAY_MAP);
            theMap.forceUpdate ();
            isSwitchToMap = false;
        }

        isFirstView = false;
    }

    private boolean moveMapFocus (Vector mobiles) {
        int xmax = 0, ymax = 0, xmin = 0xffff, ymin = 0xffff;
        boolean hasOne = false;
        String outCityList = msrt.Resources[100];
        for (int i = 0; i < mobiles.size (); i++) {
            Mobile lo = ((Mobile) mobiles.elementAt (i));
            if (lo.state == STATE_SELECTED && lo.hasCoordinates) {
                if (lo.isInCity) {
                    xmax = Math.max (xmax, lo.x + lo.precision);
                    ymax = Math.max (ymax, lo.y + lo.precision);
                    xmin = Math.min (xmin, lo.x - lo.precision);
                    ymin = Math.min (ymin, lo.y - lo.precision);
                    hasOne = true;
                }
            }
        }
        if (hasOne) {
            theMap.moveFocus (xmin, ymin, xmax, ymax, 5, true);
            return true;
        }
        return false;
    }

    /**
     * Show list of abonents
     */
    public void showLocatorList () {
        parent.changeDisplay (msrt.DISPLAY_LOCATOR);
    }

    /**
     * Commands handler. For Displayable objects.
     * @param c command
     * @param s displayable
     */
    public void commandAction (Command c, Displayable s) {
        if (s == phoneDialog) {
            if (c == parent.okCommand) {
                String phoneNumber = phoneNumberField.getString ();
                String phoneName = phoneNameField.getString ();

                if (Util.isStringNullOrEmpty (phoneName) ||
                    Util.isStringNullOrEmpty (phoneNumber)) {
                    parent.showInfo (msrt.ALERT_ERROR, 157);
                    return;
                } else {
                    OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.
                        METHOD_ADD_SUBSCRIBER, false, false);
                    ol.setOnlineLoaderListener (this);
                    ol.addParameter (REQUEST_PARAMETER_MSISDN, phoneNumber);
                    ol.addParameter (REQUEST_PARAMETER_NAME, phoneName);
                    formSelectedListParam (ol);
                    ol.go ();
                }
            }
        } else if (s == optionsDialog) {
            int i = choiceTime.getSelectedIndex ();

            if (c == parent.cancelCommand) {
                choiceTime.setSelectedIndex (prevTimeIndex, true);
            } else {
                delay = (i == 0) ? Long.MAX_VALUE : UPDATE_TIMES[i] * 60000;
                msrt.config[msrt.CONFIG_LOCATOR_MONITORING] = choiceFollow.isSelected (0) ? 0 : 1;
                msrt.config[msrt.CONFIG_LOCATOR_HISTORY] = choiceHistory.isSelected (0) ? 0 : 1;
                msrt.config[msrt.CONFIG_LOCATOR_UPDATE] = choiceTime.getSelectedIndex ();
            }
        } else if (s == alertConfirm) {
            if (c == parent.okCommand) {
                int i = locatorList.getSelectedIndex ();
                Mobile lo = (Mobile) locatorObjects.elementAt (i);
                OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.METHOD_REMOVE_SUBSCRIBER, false, false);
                ol.setOnlineLoaderListener (this);
                formSelectedListParam (ol);
                Vector v = new Vector ();
                v.addElement (lo.msisdn);
                ol.addParameter (REQUEST_PARAMETER_CURSOR, v);
                ol.go ();
            }

            parent.display.setCurrent (parent.getFrame ());
            return; // changeDisplayBack isn't required, because Alert is on the screen
        } else if (s == infoAlert) {
            if (isSwitchToMap) {
                parent.changeDisplay (msrt.DISPLAY_MAP);
                theMap.forceUpdate ();
                isSwitchToMap = false;
            }
            parent.display.setCurrent (parent.getFrame ());
            return;
        } else if (s == trackingDialog) {
            if (c == parent.okCommand) {
                serverSetTracking (trackingInterval.getSelectedIndex () * 10);
            }
        } else if (s == loadTrackDialog) {
            if (c == parent.okCommand) {
                serverGetLbsTrackList ();
            }
        }

        parent.changeDisplayBack ();
    }

    /**
     * Component actions.
     * @param component Component
     * @param command Command
     */
    public void commandAction (Component component, Command command) {
        int i = locatorList.getSelectedIndex ();
        Mobile lo = (i >= 0) ? (Mobile) locatorObjects.elementAt (i) : null;

        if (command == selectCommand) {
            lo.state = STATE_SELECTED;
            locatorList.setMasterCommand (unselectCommand);
            locatorList.set (i, locatorList.getString (i), getIcon (lo));
        } else if (command == unselectCommand) {
            lo.state = STATE_UNSELECTED;
            locatorList.setMasterCommand (selectCommand);
            locatorList.set (i, locatorList.getString (i), getIcon (lo));
        } else if (command == selectAllCommand) {
            for (int j = 0; j < locatorObjects.size (); j++) {
                Mobile jlo = (Mobile) locatorObjects.elementAt (j);
                if (jlo.state == STATE_UNSELECTED) {
                    jlo.state = STATE_SELECTED;
                    locatorList.set (j, locatorList.getString (j), getIcon (jlo));
                }
            }
        } else if (command == unselectAllCommand) {
            for (int j = 0; j < locatorObjects.size (); j++) {
                Mobile jlo = (Mobile) locatorObjects.elementAt (j);
                if (jlo.state == STATE_SELECTED) {
                    jlo.state = STATE_UNSELECTED;
                    locatorList.set (j, locatorList.getString (j), getIcon (jlo));
                }

            }
        } else if (command == addCommand) {
            parent.changeDisplay (phoneDialog);
        } else if (command == deleteCommand) {
            alertConfirm.setString (msrt.Resources[139] + lo.name + " (" + lo.msisdn + ")" + msrt.Resources[147]);
            alertConfirm.setCommandListener (this);
            parent.display.setCurrent (alertConfirm);
        } else if (command == updateCommand) {
            serverGetSubscribersList (false, true);
        }
        if (command == showOnMapCommand) {
//            showAll ();
            turnLayerOn ();
            Map.getInstance ().moveFocus (lo.latitude, lo.longitude, lo.precision);
            msrt.changeDisplay (msrt.DISPLAY_MAP);
        } else if (command == clearCommand) {
            history.removeAllElements ();
            locatorList.removeCommand (clearCommand);
            trackList.removeAllElements ();
        } else if (command == menuCommand) {
            parent.changeDisplay (msrt.DISPLAY_MENU);
        } else if (command == parent.mapCommand) {
            showAll ();
        } else if (command == optionsCommand) {
            prevTimeIndex = choiceTime.getSelectedIndex ();
            parent.changeDisplay (optionsDialog);
        } else if (command == showZonesCommand) {
            zoneController.showList (curMobile);
        } else if (command == addZoneCommand) {
            zoneController.showAddZoneDialog (curMobile);
        } else if (command == setTrackingIntervalCommand) {
            int index = curMobile.trackingInterval / 10;
            if (index >= 0 && index < trackingInterval.size ()) {
                trackingInterval.setSelectedIndex (index, true);
            }
            parent.changeDisplay (trackingDialog);
        } else if (command == loadTrackCommand) {
            parent.changeDisplay (loadTrackDialog);
        }
    }

    /**
     * Selection of list item has been changed.
     * @param newSelected int
     */
    public void selectionChanged (int newSelected) {
        locatorList.removeAllCommands ();
        locatorList.setMasterCommand (null);

        locatorList.addCommand (updateCommand);
        locatorList.addCommand (addCommand);
        locatorList.addCommand (menuCommand);
        locatorList.addCommand (optionsCommand);

        if (history.size () > 0 || trackList.size () > 0) {
            locatorList.addCommand (clearCommand);
        }

        curMobileIndex = newSelected;

        if (newSelected >= 0) {
            curMobile = (Mobile) locatorObjects.elementAt (newSelected);

            locatorList.addCommand (deleteCommand);

            Mobile mobile = (Mobile) locatorObjects.elementAt (newSelected);
            if (mobile.state == STATE_SELECTED)
                locatorList.setMasterCommand (unselectCommand);
            else if (mobile.state == STATE_UNSELECTED)
                locatorList.setMasterCommand (selectCommand);

            if (mobile.state == STATE_SELECTED || mobile.state == STATE_UNSELECTED) {
                locatorList.addCommand (setTrackingIntervalCommand);
                locatorList.addCommand (loadTrackCommand);

                if (curMobile.zones.size () == 0) {
                    locatorList.addCommand (addZoneCommand);
                } else {
                    locatorList.addCommand (showZonesCommand);
                }
            }

            if (curMobile.hasCoordinates && curMobile.isInCity) {
                locatorList.addCommand (showOnMapCommand);
            }
        } else {
            curMobile = null;
        }

        // add select-all and unselect-all commands
        boolean hasCoors = false;
        boolean oneSelected = false;
        boolean oneUnselected = false;
        for (int i = 0; i < locatorObjects.size (); i++) {
            Mobile ilo = (Mobile) locatorObjects.elementAt (i);
            hasCoors |= ilo.hasCoordinates;
            oneSelected |= ilo.state == STATE_SELECTED;
            oneUnselected |= ilo.state == STATE_UNSELECTED;
        }

        if (oneSelected) {
            locatorList.addCommand (unselectAllCommand);
        }
        if (oneUnselected) {
            locatorList.addCommand (selectAllCommand);
        }
    }

    /**
     * Background thread that updates coordinates.
     */
    public void run () {
        isWorking = true;
        lastTime = 0;
        delay = Long.MAX_VALUE;

        while (isWorking) {
            try {
                Thread.sleep (SLEEP_TOLERANCE);
                long time = System.currentTimeMillis ();
                if (time - lastTime > delay) {
                    lastTime = time;
                    serverGetSubscribersList (true, true);
                }
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * REQ: get abonents list.
     * @param isInBackground boolean true if load is in background.
     * @param lbsRetrieve boolean
     */
    private void serverGetSubscribersList (boolean isInBackground, boolean lbsRetrieve) {
        OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.METHOD_GET_SUBSCRIBER_LIST,
            isInBackground, isInBackground);
        ol.setOnlineLoaderListener (this);
        ol.addParameter (REQUEST_PARAMETER_LBS_RETRIEVE, new Boolean (lbsRetrieve));
        formSelectedListParam (ol);

        ol.go ();
    }

    private void formSelectedListParam (OnlineLoader ol) {
        if (locatorObjects.size () == 0)
            return;

        Vector v = new Vector ();
        for (Enumeration e = locatorObjects.elements (); e.hasMoreElements (); ) {
            Mobile lo = (Mobile) e.nextElement ();
            if (lo.state == STATE_SELECTED)
                v.addElement (lo.msisdn);
        }
        ol.addParameter (REQUEST_PARAMETER_SELECTED, v);
    }

    private void formCursorListParam (OnlineLoader ol) {
        Vector list = new Vector ();
        list.addElement (curMobile.msisdn);
        ol.addParameter (REQUEST_PARAMETER_CURSOR, list);
    }

    private void serverSetTracking (int trackingInterval) {
        OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.METHOD_SET_TRACKING, false, false);
        ol.setOnlineLoaderListener (this);
        ol.addParameter (REQUEST_PARAMETER_INTERVAL, trackingInterval * 60);
        formSelectedListParam (ol);
        formCursorListParam (ol);
        ol.go ();
    }

    private void serverGetLbsTrackList () {
        trackList.removeAllElements ();
//        System.out.println ("Server.loadTrack is called: " + startDateField.getDate() + " / " + endDateField.getDate());

        OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.METHOD_GET_LBS_TRACK_LIST, false, false);
        ol.setOnlineLoaderListener (this);
        formSelectedListParam (ol);
        formCursorListParam (ol);
        ol.addParameter (REQUEST_PARAMETER_BEGIN, (int) (startDateField.getDate ().getTime () / 1000));
        ol.addParameter (REQUEST_PARAMETER_END, (int) (endDateField.getDate ().getTime () / 1000));
        ol.go ();
    }

    public void serverRequestComplete (int errorCode, OnlineLoader onlineLoader) {
        if (errorCode == OnlineLoaderListener.CODE_ERROR) {
//            parent.display.setCurrent (parent.getFrame ());
        }

        if (onlineLoader.isInBackground ()) {
            Map.update = Map.needpaint = true;
            theMap.repaint ();
        } else {
            if (OnlineLoader.METHOD_GET_SUBSCRIBER_LIST.equals (onlineLoader.getMethod ())) {
                isFirstView = true;
                showAll ();
            } else if (OnlineLoader.METHOD_GET_LBS_TRACK_LIST.equals (onlineLoader.getMethod ())) {
                if (moveMapFocus (trackList)) {
                    msrt.changeDisplay (msrt.DISPLAY_MAP);
                    theMap.forceUpdate ();
                } else {
                    msrt.display.setCurrent (Frame.getInstance ());
                }
            } else {
                msrt.display.setCurrent (Frame.getInstance ());
            }
        }
    }

    private static final Character FIELD_MSISDN = new Character ('M');
    private static final Character FIELD_NAME = new Character ('N');
    private static final Character FIELD_STATE = new Character ('S');
    private static final Character FIELD_LATITUDE = new Character ('A');
    private static final Character FIELD_LONGITUDE = new Character ('B');
    private static final Character FIELD_PRECISION = new Character ('P');
    private static final Character FIELD_TIME = new Character ('T');
    private static final Character FIELD_TIME_STRING = new Character ('t');
    private static final Character FIELD_TRACKING_INTERVAL = new Character ('I');
    private static final Character FIELD_ZONE = new Character ('Z');

    private static final Character FIELD_ZONE_LATITUDE = new Character ('A');
    private static final Character FIELD_ZONE_LONGITUDE = new Character ('B');
    private static final Character FIELD_ZONE_RADIUS = new Character ('R');
//    private static final Character FIELD_ZONE_MODE = new Character('M');
    private static final Character FIELD_ZONE_NAME = new Character ('N');

    /**
     * Load list of subscribers
     * @param in incoming stream
     * @param method remote method
     * @throws IOException
     */
    public void readAbonentsChunk (DataInputStream in, String method) throws IOException {
        boolean isGetLbsTrackListMethod = OnlineLoader.METHOD_GET_LBS_TRACK_LIST.equals (method);

        boolean hasAllCoordinates = false;

        if (!isGetLbsTrackListMethod) {
            newSession ();
        }
        turnLayerOn ();

        // array of categories
        in.readByte ();
        int count = in.readInt ();

        for (int i = 0; i < count; i++) {
            Hashtable data = (Hashtable) OnlineLoader.readObject (in);

            String msisdn = (String) data.get (FIELD_MSISDN);
            String name = (String) data.get (FIELD_NAME);
            int state = ((Integer) data.get (FIELD_STATE)).intValue ();
            ;

            int latitude = 0;
            int longitude = 0;
            int precision = 0;
            int time = 0;
            String timeString = null;
            boolean hasCoordinates = false;
            int trackingInterval = 0;

            try {
                // try to read location values
                latitude = ((Integer) data.get (FIELD_LATITUDE)).intValue ();
                longitude = ((Integer) data.get (FIELD_LONGITUDE)).intValue ();
                precision = ((Integer) data.get (FIELD_PRECISION)).intValue ();
                time = ((Integer) data.get (FIELD_TIME)).intValue ();
                timeString = (String) data.get (FIELD_TIME_STRING);
                hasCoordinates |= latitude != 0 && longitude != 0;
            } catch (Exception ex) {
            }

            if (data.get (FIELD_TRACKING_INTERVAL) != null) {
                trackingInterval = (((Integer) data.get (FIELD_TRACKING_INTERVAL)).intValue ()) / 60;
            }

            Vector zones = new Vector ();
            if (data.get (FIELD_ZONE) != null && !isGetLbsTrackListMethod) {
                Vector zoneListData = (Vector) data.get (FIELD_ZONE);
                for (int j = 0; j < zoneListData.size (); j++) {
                    Hashtable areaData = (Hashtable) zoneListData.elementAt (j);
                    int zoneLatitude = ((Integer) areaData.get (FIELD_ZONE_LATITUDE)).intValue ();
                    int zoneLongitude = ((Integer) areaData.get (FIELD_ZONE_LONGITUDE)).intValue ();
                    int zoneRadius = ((Integer) areaData.get (FIELD_ZONE_RADIUS)).intValue ();
                    String zoneName = ((String) areaData.get (FIELD_ZONE_NAME));

                    Zone zone = new Zone (zoneLatitude, zoneLongitude, zoneRadius, zoneName);
                    zones.addElement (zone);
                }
            }
            if (state < 0 || state >= STATE_LIMIT)
                state = STATE_REJECTED;

            Mobile mo = new Mobile (msisdn, name, state, hasCoordinates, latitude, longitude, precision,
                                    time, timeString, trackingInterval, zones);

            if (isGetLbsTrackListMethod) {
                trackList.addElement (mo);
            } else {
                locatorObjects.addElement (mo);
                locatorList.append (name + " (" + msisdn + ')', getIcon (mo), 0x000000,
                                    (trackingInterval > 0) ? 0xDFFFDF : 0xFFFFFF);

                hasAllCoordinates |= mo.isInCity;
            }
        }
        if (!isGetLbsTrackListMethod) {
            isSwitchToMap = hasAllCoordinates;
            selectionChanged ( -1);
        }
    }

    /**
     * Start new session of loading coordinates
     */
    private void newSession () {
        for (Enumeration e = locatorObjects.elements (); e.hasMoreElements (); ) {
            Mobile lo = (Mobile) e.nextElement ();
            if (lo.latitude != 0 && lo.longitude != 0) {
                lo.zones.removeAllElements ();
                history.addElement (lo);
            }
        }

        locatorObjects.removeAllElements ();
        locatorList.deleteAll ();
    }

    private Image getIcon (Mobile lo) {
        if (lo.hasCoordinates)
            return abonentIcons[lo.state + 4];
        else
            return abonentIcons[lo.state];
    }

    /**
     * Draw all mobile objects
     * @param g graphics
     * @param showName true, if object name must be painted
     */
    public void draw (Graphics g, boolean showName) {
        // draw local history
        if (msrt.config[msrt.CONFIG_LOCATOR_HISTORY] == 0) {
            for (Enumeration e = locatorObjects.elements (); e.hasMoreElements (); ) {
                Mobile mo = (Mobile) e.nextElement ();
                if (mo.state == STATE_SELECTED) {
                    Mobile prev = null;
                    for (Enumeration o = history.elements (); o.hasMoreElements (); ) {
                        Mobile h = (Mobile) o.nextElement ();
                        if (h.msisdn.compareTo (mo.msisdn) == 0) {
                            drawPath (g, prev, h);
                            h.draw (g, theMap, showName, true, false, true);
                            prev = h;
                        }
                    }
                    drawPath (g, prev, mo);
                }
            }
        }

        // draw tracks
        for (Enumeration e = locatorObjects.elements (); e.hasMoreElements (); ) {
            Mobile mo = (Mobile) e.nextElement ();
            Mobile prev = null;
            for (Enumeration o = trackList.elements (); o.hasMoreElements (); ) {
                Mobile h = (Mobile) o.nextElement ();
                if (h.msisdn.equals (mo.msisdn)) {
                    drawPath (g, prev, h);
                    h.draw (g, theMap, showName, true, false, true);
                    prev = h;
                }
            }
        }

        // draw mobile objects
        for (Enumeration e = locatorObjects.elements (); e.hasMoreElements (); ) {
            Mobile mo = (Mobile) e.nextElement ();
            if (mo.state == STATE_SELECTED) {
                mo.draw (g, theMap, showName, false, false, false);
            }
        }

        // draw zones
        zoneController.draw (g, theMap);
    }

    /**
     * Draw path between mobile objects
     * @param g Graphics
     * @param prev LocatorObject
     * @param cur LocatorObject
     */
    private void drawPath (Graphics g, Mobile prev, Mobile cur) {
        if (prev != null && cur != null) {
            if (prev.isInCity && cur.isInCity) {
                int x1 = Map.mx (prev.x), y1 = Map.my (prev.y);
                int x2 = Map.mx (cur.x), y2 = Map.my (cur.y);

                g.setColor (Mobile.COLOR_HISTORICAL);
                g.setStrokeStyle (Graphics.DOTTED);
                g.drawLine (x1, y1, x2, y2);
            }
        }
    }
}
