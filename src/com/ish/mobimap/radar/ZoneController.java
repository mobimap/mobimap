/*****************************************************************************/
/*                               R A D A R  3                                */
/*                       (c) 2003-2008 Ilya Shakhat                          */
/*                           (c) 2006-2008 TMS                               */
/*****************************************************************************/

package com.ish.mobimap.radar;

import java.util.*;
import javax.microedition.lcdui.*;

import com.ish.mobimap.*;
import com.ish.mobimap.net.*;
import com.ish.mobimap.ui.*;

class ZoneController
    implements CommandListener, ComponentListener, ListSelectionListener, OnlineLoaderListener
{
    private Alert alertConfirm;

    private ListM zoneList;

    private Form zonePropertiesDialog;
    private TextField zoneNameField;
    private TextField zoneRadiusField;

    private static Command pickupCoordinatesCommand;
    private static Command showZoneOnMapCommand;
    private static Command placeZoneCommand;
    private static Command propertiesCommand;
    private static Command saveCommand;
    private static Command locatorCommand;

    private Mobile originalMobile;
    private Mobile curMobile;

    private int curZoneIndex = 0;
    private Zone curZone = null;
    private boolean isNewZone;

    ZoneController() {
        alertConfirm = new Alert (msrt.Resources [11], "", null, AlertType.CONFIRMATION);
        alertConfirm.addCommand(msrt.okCommand);
        alertConfirm.addCommand(msrt.cancelCommand);
        alertConfirm.setTimeout(Alert.FOREVER);

        // list of zones
        zoneList = new ListM(msrt.getInstance(), msrt.getInstance().getFrame(), msrt.Resources [169]);
        zoneList.setComponentListener(this);
        zoneList.setSelectionListener(this);

        // zone properties dialog
        zonePropertiesDialog = new Form(msrt.Resources [172]);
        zoneNameField = new TextField(msrt.Resources[173], "", 50, TextField.ANY);
        zonePropertiesDialog.append(zoneNameField);
        zoneRadiusField = new TextField(msrt.Resources[174], "", 4, TextField.NUMERIC);
        zonePropertiesDialog.append(zoneRadiusField);
        zonePropertiesDialog.addCommand(msrt.backCommand);
        zonePropertiesDialog.addCommand (msrt.okCommand);
        zonePropertiesDialog.setCommandListener(this);

        pickupCoordinatesCommand = new Command (msrt.Resources [185], Command.SCREEN, 1);
        showZoneOnMapCommand = new Command (msrt.Resources [186], Command.SCREEN, 11);
        placeZoneCommand = new Command (msrt.Resources [170], Command.SCREEN, 19);
        propertiesCommand = new Command (msrt.Resources [172], Command.SCREEN, 19);
        saveCommand = new Command (msrt.Resources [187], Command.SCREEN, 1);
        locatorCommand = new Command (msrt.Resources [96], Command.SCREEN, 1);
    }

    private void setCurrentMobile(Mobile mobile) {
        originalMobile = mobile;
        curMobile = originalMobile.clone();
    }

    public void commandAction (Command command, Displayable displayable)
    {
        if (displayable == alertConfirm)
        {
            if (command == msrt.okCommand)
            {
                curMobile.zones.removeElement(curZone);
                curZone = null;
                callSetProperties();
            }

        } else if (displayable == zonePropertiesDialog) {
            if (command == msrt.okCommand) {
                String s = zoneNameField.getString();
                if (s.length() > 0) {
                    curZone.name = zoneNameField.getString ();
                }
                int radius = Integer.parseInt(zoneRadiusField.getString());
                if (radius < Zone.RADIUS_MIN) {
                    radius = Zone.RADIUS_MIN;
                } else if (radius > Zone.RADIUS_MAX) {
                    radius = Zone.RADIUS_MAX;
                }
                curZone.radius = radius;
                if (isNewZone) {
                    isNewZone = false;
                    switchToMap();
                } else {
                    callSetProperties();
                }
            } else {
                rollback();
            }
        }
        msrt.display.setCurrent(Frame.getInstance());
    }

    public void commandAction (Component component, Command command)
    {
        if (component == msrt.getInstance().theMap)
        {
            if (command == saveCommand) {
                Map.getInstance().switchToViewMode ();
                callSetProperties();
            } else if (command == pickupCoordinatesCommand) {
                curZone.setCoordinates (Map.getInstance().getCursorLatitude (),
                    Map.getInstance().getCursorLongitude ());
                Map.getInstance().forceUpdate();
                return; // get out
            } else if (command == msrt.backCommand) {
                Map.getInstance().switchToViewMode ();
                msrt.getInstance().changeDisplayBack();
            }
        }
        else
        {
            if (command == locatorCommand)
            {
                curMobile = null;
                msrt.getInstance().changeDisplay (msrt.DISPLAY_LOCATOR);
            }
            else if (command == Locator.addCommand)
            {
                addZoneCommand();
            }
            else if (command == Locator.deleteCommand)
            {
                alertConfirm.setString (msrt.Resources[175]);
                alertConfirm.setCommandListener (this);

                // CHECK THIS
                msrt.getInstance().display.setCurrent (alertConfirm);
            }
            else if (command == propertiesCommand)
            {
                doPropertiesZone ();
            }
            else if (command == placeZoneCommand)
            {
                switchToMap ();
            }
            else if (command == showZoneOnMapCommand)
            {
                if (Map.getInstance().isInCity(curZone.lat, curZone.lon)) {
                    Map.getInstance ().moveFocus (curZone.lat, curZone.lon, curZone.radius);
                    Map.getInstance ().switchToPickupMode (this, msrt.backCommand, null);
                    msrt.getInstance ().changeDisplay (msrt.DISPLAY_MAP);
                } else {
                    msrt.getInstance().showInfo(msrt.ALERT_INFO, 189);
                }
            }
        }
    }

    /**
     * Handles SetProperties remote call
     */
    protected void callSetProperties() {
        int interval = curMobile.trackingInterval;
        String name = curMobile.name;
        String msisdn = curMobile.msisdn;

        OnlineLoader ol = msrt.getInstance().createOnlineLoader(OnlineLoader.METHOD_SET_SUBSCRIBER_PROPERTIES, false, false);
        ol.setOnlineLoaderListener(this);
        ol.addParameter(Locator.REQUEST_PARAMETER_MSISDN, msisdn);
        ol.addParameter(Locator.REQUEST_PARAMETER_NAME, name);
        ol.addParameter(Locator.REQUEST_PARAMETER_INTERVAL, interval);

        Vector zoneListData = new Vector();
        ol.addParameter(Locator.REQUEST_PARAMETER_ZONE_LIST, zoneListData);

        for (int i=0; i < curMobile.zones.size(); i++) {
            Zone zone = (Zone) curMobile.zones.elementAt(i);
            Hashtable zoneData = new Hashtable();
            zoneData.put(Locator.REQUEST_PARAMETER_ZONE_LAT, new Integer(zone.lat));
            zoneData.put(Locator.REQUEST_PARAMETER_ZONE_LON, new Integer(zone.lon));
            zoneData.put(Locator.REQUEST_PARAMETER_ZONE_RADIUS, new Integer(zone.radius));
            zoneData.put(Locator.REQUEST_PARAMETER_ZONE_NAME, zone.name);

            zoneListData.addElement(zoneData);
        }

        ol.go();
    }

    /**
     * Remote call is completed
     * @param errorCode int
     * @param onlineLoader OnlineLoader
     */
    public void serverRequestComplete (int errorCode, OnlineLoader onlineLoader)
    {
        if (errorCode == OnlineLoaderListener.CODE_OK) {
            if (OnlineLoader.METHOD_SET_SUBSCRIBER_PROPERTIES.equals (onlineLoader.getMethod ()))
            {
                commit();
                msrt.getInstance().changeDisplay(zoneList);
            }
        } else if (errorCode == OnlineLoaderListener.CODE_ERROR) {
            rollback();
            msrt.getInstance().changeDisplay(zoneList);
        }
    }

    private void commit() {
        originalMobile.zones = curMobile.zones;
        curMobile = originalMobile.clone();
        refreshUI();
    }

    private void rollback()
    {
        curMobile = originalMobile.clone();
        refreshUI();
    }

    private void addZoneCommand ()
    {
        isNewZone = true;
        curZone = new Zone (Map.getInstance().getCursorLatitude(), Map.getInstance().getCursorLongitude(), 500,
                            msrt.Resources[184]);
        curZoneIndex = curMobile.zones.size();
        curMobile.zones.addElement (curZone);
        doPropertiesZone ();
    }

    private void doPropertiesZone() {
        zoneNameField.setString (curZone.name);
        zoneRadiusField.setString (Integer.toString (curZone.radius));
        msrt.getInstance().changeDisplay (zonePropertiesDialog);
    }

    private void switchToMap() {
        Map.getInstance ().moveFocus (curZone.lat, curZone.lon, curZone.radius);
        Map.getInstance ().switchToPickupMode (this, saveCommand, pickupCoordinatesCommand);
        msrt.getInstance ().changeDisplay (msrt.DISPLAY_MAP);
    }

    public void selectionChanged (int newSelected)
    {
        zoneList.removeAllCommands ();
        zoneList.setMasterCommand (null);

        zoneList.addCommand (Locator.addCommand);
        zoneList.setMasterCommand (locatorCommand);
        curZoneIndex = newSelected;

        if (newSelected >= 0)
        {
            zoneList.addCommand (Locator.deleteCommand);
            zoneList.addCommand (placeZoneCommand);
            zoneList.addCommand (propertiesCommand);
            zoneList.addCommand(showZoneOnMapCommand);

            curZone = (Zone) curMobile.zones.elementAt(newSelected);
        } else {
            curZone = null;
        }
        isNewZone = false;
    }

    private void refreshUI()
    {
        zoneList.deleteAll();
        for (int i = 0; i < curMobile.zones.size(); i++)
        {
            Zone zone = (Zone)curMobile.zones.elementAt(i);
            zoneList.append(zone.name);
        }
    }

    public void showList(Mobile mobile) {
        setCurrentMobile(mobile);
        refreshUI();
        msrt.getInstance().changeDisplay(zoneList);
    }

    public void showAddZoneDialog(Mobile mobile) {
        setCurrentMobile(mobile);
        addZoneCommand();
    }

    public void draw (Graphics g, Map theMap) {
        if (curMobile == null) {
            return;
        }

        Vector zones = curMobile.zones;
        for (int i=0; i < zones.size(); i++) {
            ((Zone)zones.elementAt(i)).draw(g, theMap, true, curMobile.trackingInterval > 0, i == curZoneIndex);
        }
    }
}
