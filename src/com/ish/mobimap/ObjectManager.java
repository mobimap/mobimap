/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                            (c) 2003-2007 ISh                              */
/*****************************************************************************/

package com.ish.mobimap;

import java.util.Hashtable;
import javax.microedition.lcdui.*;

import com.ish.mobimap.net.*;
import com.ish.mobimap.ui.*;
import java.io.*;
import java.util.Vector;

class ObjectManager implements ListSelectionListener, OnlineLoaderListener {
    private msrt parent;
    private ListM objList;

    /**
     * Contents of list of objects
     */
    static int objListContents[];
    /**
     * Parent of current category
     */
    private static int objListUpnode;
    /**
     * Maximum desired number of items per category
     */
    final static int CONTENTS_LIMIT = 35;
    /**
     * Maximum possible number of elements per category
     */
    private final static int CONTENTS_HARD_LIMIT = 150;

    // commands
    private final static int COMMAND_LOAD_OBJECTS = 1;
    private final static int COMMAND_LOAD_CATEGORIES = 2;

    public boolean areOnlineCategoriesLoaded = false;

    public ObjectManager (msrt parent) {
        this.parent = parent;
        this.objList = parent.objList;

        objList.setSelectionListener (this);
        objList.setBackCommand (parent.backCommand);
        objListContents = new int[CONTENTS_HARD_LIMIT];
    }

    private Component backToComponent;

    private static final String REQUEST_PARAMETER_CATEGORY_ID = "obj.categoryId";
    private static final String REQUEST_PARAMETER_POINT_ID = "obj.pointId";
    private static final String REQUEST_PARAMETER_STREET_NAME = "obj.streetName";
    private static final String REQUEST_PARAMETER_STREET_BOUNDS = "obj.streetBounds";
    private static final String REQUEST_PARAMETER_TYPE_LIST = "obj.typeList";
    private static final String REQUEST_PARAMETER_ID = "obj.id";

    private int pointIdToLoad;
    private boolean continueLoad;
    private boolean selectLoadedPoint;

    //
    //  CREATE LIST OF OBJECTS
    //

    public void createAndShowObjList (Component backToComponent) {
        this.backToComponent = backToComponent;

        // find out current category
        int curct = Map.curCategory & 0xffff;
        int curctSeg = Map.curCategory >> 16;

        Map.changeFocus = true;
        objListUpnode = -1;

        //System.out.println("curct: " + curct);

        objList.deleteAll ();

        if (curct == Map.CATEGORY_SYSTEM_STREET_ADDRESSES) {
            int streetId = Map.currentObjectItem;
            if (Map.currentObjectClass == Map.CLASS_ADDRESS)
                streetId = Map.blst[streetId >> 16][streetId & 0xffff];

            int j = 0;
            for (int seg = 0; seg < Map.SEGMENT_LIMIT; seg++)
                for (int i = 0; i < Map.blN[seg] && j < CONTENTS_HARD_LIMIT; i++) {
                    if (Map.blst[seg][i] == streetId) {
                        objListContents[j++] = Map.MASK_ADDRESS | (seg << 16) | i;
                        objList.append (getAddressNumber (seg, i));
                    }
                }

            if (j > 0) { // have some addresses
                objList.setSelectedIndex (0);
                objList.setTitle (getStreetName (streetId));
                objList.setLook (Design.COLOR_OBJECT_MANAGER_ADDRESSES_TITLE_BACKGROUND,
                                 Design.COLOR_OBJECT_MANAGER_ADDRESSES_TITLE_TEXT);

                parent.changeDisplay (msrt.DISPLAY_OBJLIST);
                objList.showNotify (); // refresh list
                objList.repaint ();
            }
            return;
        }

        objList.setTitle (getCategoryName (curct));

        // visible, search results category
        int j = 0;
        if (curct == Map.CATEGORY_ROOT && msrt.BUILD_VERSION != msrt.BUILD_RADAR) {
            // visible
            objListContents[j++] = 0x8000000 | Map.CATEGORY_VISIBLE;
            objList.append (getCategoryName (Map.CATEGORY_VISIBLE), Map.ctIcon[Map.CATEGORY_VISIBLE]);

            // search results
            if (Map.srhN > 0) {
                objListContents[j++] = Map.MASK_CATEGORY | Map.CATEGORY_RESULTS;
                objList.append (getCategoryName (Map.CATEGORY_RESULTS), Map.ctIcon[Map.CATEGORY_RESULTS]);
            }
        }

        // add list of categories, sorted by name
        int start = j;
        for (int i = Map.CATEGORY_BUILT_IN_COUNT; i < Map.ctN; i++) {
            if (Map.ctParent[i] == curct && (Map.ctShow[i] & Map.CATEGORY_SHOW_LIST) > 0) {
                objListContents[j++] = i | Map.MASK_CATEGORY;
                Image icon = Map.ctIcon[i];
                if (icon == null)
                    icon = parent.icons[msrt.ICON_CATEGORY];

                int bgcolor = Map.ctMode[i] == Map.CATEGORY_MODE_DOWNLOAD_POINTS ?
                              Design.COLOR_OBJECT_MANAGER_ONLINE_POINTS :
                              Map.ctMode[i] == Map.CATEGORY_MODE_DOWNLOAD_MARKS ?
                              Design.COLOR_OBJECT_MANAGER_ONLINE_MARKS : -1;

                objList.append (getCategoryName (i), icon, -1, bgcolor);

                if (msrt.BUILD_VERSION == msrt.BUILD_RADAR && Map.ctMode[i] == Map.CATEGORY_MODE_STATIC)
                    start = j;
            }
        }
        int end = j;

        // add online category if we haven't loaded it yet
        if (msrt.BUILD_VERSION != msrt.BUILD_RADAR)
            if (curct == Map.CATEGORY_ROOT &&
                msrt.features[msrt.FEATURE_ONLINE_CATEGORIES] &&
                !areOnlineCategoriesLoaded) {
                objListContents[j++] = COMMAND_LOAD_CATEGORIES;
                objList.append (msrt.Resources[154]);
            }

        // check if category contents exceed list length limit.
        // in this case add subcategories according to object names first letters
        boolean isSubCategories = false;
        final int limit3 = CONTENTS_LIMIT - 3;

        if (curctSeg == 0) {
            int qn = 0;
            int curctUid = curct; //Map.ctUid[curct];
            for (int seg = 0; seg < Map.SEGMENT_LIMIT; seg++)
                for (int i = 0; i < Map.lbN[seg]; i++)
                    if (Map.lbct[seg][i] == curctUid)
                        qn++;

            if (isSubCategories = qn > limit3) {
                for (int seg = 0, n = 0, nseg = 0; seg < Map.SEGMENT_LIMIT; seg++)
                    for (int i = 0; i < Map.lbN[seg] && j < limit3; i++)
                        if (Map.lbct[seg][i] == curctUid) {
                            if (n % limit3 == 0) {
                                nseg++;

                                objListContents[j++] = curct | 0x8000000 | (nseg << 16);
                                int next = i;
                                for (int k = i + 1, kl = 1; kl < limit3 && k < Map.lbN[seg]; k++)
                                    if (Map.lbct[seg][k] == curctUid) {
                                        kl++;
                                        next = k;
                                    }
                                if (next >= Map.lbN[seg])
                                    next = Map.lbN[seg] - 1;
                                objList.append ("" + Map.name[seg][Map.lbnamep[seg][i]] +
                                                "-" + Map.name[seg][Map.lbnamep[seg][next]]);
                            }
                            n++;
                        }
            }
        }

        // add list of objects owned by current category
        if (!isSubCategories) {
            int curctUid = curct; //Map.ctUid[curct];
            for (int seg = 0, n = 0, nseg = 0; seg < Map.SEGMENT_LIMIT; seg++)
                for (int i = 0; i < Map.lbN[seg] && j < limit3; i++)
                    if (Map.lbct[seg][i] == curctUid) {
                        if (n % limit3 == 0)
                            nseg++;

                        if (curctSeg == 0 || curctSeg == nseg) {
                            objListContents[j++] = i | (seg << 16) |
                                Map.MASK_LABEL;
                            objList.append (getLabelName (seg, i));
                        }

                        n++;
                    }
        }

        // visible objects
        if (curct == Map.CATEGORY_VISIBLE) {
            start = j;
            for (int v = 0; v < Map.visibleObjectsN && j < CONTENTS_HARD_LIMIT - 3; v++) {
                int i = Map.visibleObjects[v];
                int cl = i >> 24;
                if (cl != 1) {
                    objListContents[j++] = i;
                    i &= 0xffffff;
                    int seg = i >> 16;
                    i &= 0xffff;

                    if (cl == 4)
                        objList.append (getLabelFullName (seg, i));
                    else if (cl == 2)
                        objList.append (getStreetName (i));
                }
            }
            end = j;
            Map.changeFocus = false;
        }
        // search results
        if (curct == Map.CATEGORY_RESULTS) {
            int s = j;
            for (int v = 0; v < Map.srhN && j < limit3; v++) {
                int i = Map.srhContents[v];
                objListContents[j++] = i;
                int cl = i >> 24;
                i &= 0xffffff;
                int seg = i >> 16;
                i &= 0xffff;

                if (cl == Map.CLASS_LABEL)
                    objList.append (getLabelFullName (seg, i));
                else if (cl == Map.CLASS_STREET)
                    objList.append (getStreetName (i));
            }
            end = j;
        }

        // sort items
        if (end > start)
            objList.sort (objListContents, start, end - start);

        // add reference to parent category
        if (curct != Map.CATEGORY_ROOT) {
            objListUpnode = ((curctSeg == 0) ? Map.ctParent[curct] : curct) | Map.MASK_CATEGORY;

            if (msrt.BUILD_VERSION != msrt.BUILD_RADAR) {
                objListContents[j++] = objListUpnode;
                objList.append (msrt.Resources[35]);
            }
        }

        objList.setLook (Design.COLOR_OBJECT_MANAGER_TITLE_BACKGROUND,
                         Design.COLOR_OBJECT_MANAGER_TITLE_TEXT);

        if (j > 0)
            objList.setSelectedIndex (0);
        parent.changeDisplay (msrt.DISPLAY_OBJLIST);
        objList.showNotify (); // refresh list
        objList.repaint ();
    }

    /**
     * Notification about changes in selection
     * @param newSelected int
     */
    public void selectionChanged (int newSelected) {
        objList.removeAllCommands ();
        objList.setMasterCommand (null);

        if (newSelected >= 0) {
            int obj = objListContents[newSelected];
            int cl = obj >> 24;
            int id = obj & 0xffff;
            int sg = (obj >> 16) & 0xff;

            if (cl == Map.CLASS_LABEL || cl == Map.CLASS_STREET || cl == Map.CLASS_ADDRESS) {
                if (msrt.BUILD_VERSION != msrt.BUILD_RADAR) {
                    objList.addCommand (msrt.dirCommand);
                }

                if (msrt.features[msrt.FEATURE_ROUTING] && msrt.BUILD_VERSION != msrt.BUILD_RADAR) {
                    if (cl != Map.CLASS_STREET)
                        objList.addCommand (msrt.startCommand);
                    if (PathFinder.isStartSet () && !PathFinder.isSameToStart (obj)) {
                        objList.addCommand (msrt.pathCommand);
                    }
                }

                if (cl == Map.CLASS_LABEL) {
                    int ctmode = Map.ctMode[Map.lbct[sg][id]];

                    if (Map.lbUid[sg][id] > 0) {
                        objList.addCommand (msrt.infoCommand);
                    }

                    if (Map.ctMode[Map.lbct[sg][id]] == Map.CATEGORY_MODE_DOWNLOAD_MARKS) {
                        objList.addCommand (msrt.removeMarkCommand);
                    }
                } else if (cl == Map.CLASS_STREET) {
                    if (msrt.features[msrt.FEATURE_ONLINE_ADDRESSES]) {
                        objList.addCommand (msrt.getAddressesCommand);
                    }
                    if (ObjectManager.hasStreetAddresses (id))
                        objList.addCommand (msrt.showAddressesCommand);
                }
                objList.setMasterCommand (msrt.gotoCommand);
            } else {
                objList.setMasterCommand (msrt.selectCommand);
            }
        }

        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            if (Map.curCategory != Map.CATEGORY_ROOT && Map.curCategory != Map.CATEGORY_RESULTS) {
                objList.addCommand (msrt.backCommand);
            }

            if (Map.curCategory < Map.ctN && Map.ctMode[Map.curCategory] == Map.CATEGORY_MODE_DOWNLOAD_MARKS) {
                objList.addCommand (msrt.getMarksCommand);
            }
        }

        Command c1 = msrt.menuCommand;
        Command c2 = msrt.mapCommand;

        if (backToComponent == parent.theMap)
            c2 = msrt.backToMapCommand;
        else if (backToComponent == parent.mainMenu)
            c1 = msrt.backToMenuCommand;

        objList.addCommand (c1);
        objList.addCommand (c2);
    }

    /**
     * Go to parent node or go back if current is root category
     */
    public void back () {
        if (Map.curCategory == Map.CATEGORY_ROOT ||
            Map.curCategory == Map.CATEGORY_SYSTEM_STREET_ADDRESSES) {
            if (backToComponent != null)
                parent.changeDisplayBack ();
        } else {
            Map.curCategory = objListUpnode & 0xffffff;
            createAndShowObjList (backToComponent);
        }
    }

    //
    //  GET NAME _ FUNCTIONS
    //

    /**
     * Get category name.
     * @param i category id
     * @return name of category
     */
    static String getCategoryName (int i) {
        return new String (Map.ctName[i]);
    }


    /**
     * Convert remote category id to local index
     * @param remoteId int
     * @return short
     */
    static short getCtRemoteToLocal (int remoteId) {
        Integer i = (Integer) Map.ctRemoteToLocal.get (new Integer (remoteId));
        if (i != null) {
            return (short) i.intValue ();
        } else {
            return 0;
        }
    }

    /**
     * Convert remote category id to local index
     * @param remoteId Object
     * @return short
     */
    static short getCtRemoteToLocal (Object remoteId) {
        Integer i = (Integer) Map.ctRemoteToLocal.get (remoteId);
        if (i != null) {
            return (short) i.intValue ();
        } else {
            return 0;
        }
    }

    /**
     * Get label's name.
     * @param seg data segment
     * @param i data offset
     * @return label's name
     */
    static String getLabelName (int seg, int i) {
        return new String (Map.name[seg], Map.lbnamep[seg][i],
                           Map.lbnamep[seg][i + 1] - Map.lbnamep[seg][i]);
    }

    /**
     * Get address' name
     * @param seg data segment
     * @param i data offset
     * @return address' name
     */
    static String getAddressName (int seg, int i) {
        return getStreetName (Map.blst[seg][i]) + ", " +
            getAddressNumber (seg, i);
    }

    static String getAddressNumber (int seg, int i) {
        int offset = Map.blnamep[seg][i];
        int length = Map.blnamep[seg][i + 1] - offset;

        String s = null;
        if (length == 0) {
            s = Integer.toString (((int) Map.blnumber[seg][i]) & 0xff);
        } else {
            s = new String (Map.name[seg], offset, Map.blnamep[seg][i + 1] - offset);
        }

        return s;
    }

    /**
     * Get label's name including its category's name
     * @param seg data segment
     * @param i data offset
     * @return full label's name
     */
    static String getLabelFullName (int seg, int i) {
        int ict = Map.lbct[seg][i];
        return getLabelName (seg, i) + " (" + getCategoryName (ict) + ")";
    }

    /**
     * Get street's name
     * @param st street id
     * @return street's name
     */
    static String getStreetName (int st) {
        int namepA = Map.stnamep[st], namepE = Map.stnamep[st + 1];
        int len = namepE - namepA;

        return new String (Map.stnames, namepA, len);
    }

    /**
     * Get crossroad's name
     * @param pc crossroad id
     * @return crossroad's name
     */
    static String getPcName (int pc) {
        int st1 = 0, st2 = 0;

        String name = msrt.Resources[92];
        for (int j = Map.pcconp[pc]; j < Map.pcconp[pc + 1]; j++) {
            int el = Map.conel[j];
            if (el != Map.elementN) {
                short st = Map.el2st[el];
                if (st != 0)
                    if (st1 == 0) {
                        st1 = st;
                        name = getStreetName (st);
                    } else if (st != st1) {
                        st2 = st;
                        name = name + " / " + getStreetName (st);
                        break;
                    }
            }
        }
        for (int k = Map.pcconpi[pc]; k < Map.pcconpi[pc + 1]; k++) {
            int n = Map.coni[k];
            for (int j = Map.pcconp[n]; j < Map.pcconp[n + 1]; j++)
                if ((Map.con[j] & 0x7fff) == pc) {
                    int el = Map.conel[j];
                    if (el != Map.elementN) {
                        short st = Map.el2st[el];
                        if (st != 0)
                            if (st1 == 0) {
                                st1 = st;
                                name = getStreetName (st);
                            } else if (st2 == 0 && st != st1) {
                                st2 = st;
                                name = name + " / " + getStreetName (st);
                                break;
                            }
                    }
                }
        }
        return name;
    }

    /**
     * Get parent category.
     * @param ct category id
     * @return parent category if ct isn't CATEGORY_ROOT
     */
    static int getCategoryAncestor (int ct) {
        int up = ct;
        try {
            while (ct != Map.CATEGORY_ROOT) {
                up = ct;
                ct = Map.ctParent[ct];
            }
        } catch (Exception ex) {
        }
        return up;
    }

    static boolean hasStreetAddresses (int streetId) {
        for (int seg = 0; seg < Map.SEGMENT_LIMIT; seg++)
            for (int i = 0; i < Map.blN[seg]; i++) {
                if (Map.blst[seg][i] == streetId) {
                    return true;
                }
            }
        return false;
    }

    //
    //      S E L E C T     O B J E C T S
    //

    /**
     * Select specified object
     * @param oj object id
     * @return true if some real object is selected, false if category or command is selected
     */
    public boolean select (int oj) {
        boolean res = true;

        if (oj == -1) { // i.e. back command in root category
            return false;
        }

        int cl = oj >> 24;
        int id = oj & 0xffffff;

//        System.out.println("select: " + cl + " " + id);

        if (cl == Map.CLASS_NONE) { // the only cl==0 reference is "load data"
            if (id == COMMAND_LOAD_OBJECTS) {
                serverGetPoints ();
            } else if (id == COMMAND_LOAD_CATEGORIES) {
                serverGetCategories ();
            }
            res = false;
        } else if (cl == Map.CLASS_CATEGORY) {
            Map.curCategory = id;
            int ctid = id & 0xffff;
            if (Map.ctMode[ctid] == Map.CATEGORY_MODE_DOWNLOAD_POINTS && !Map.ctIsFilledWithObjects[ctid]) {
                serverGetPoints ();
            } else if (Map.ctMode[ctid] == Map.CATEGORY_MODE_DOWNLOAD_MARKS && !Map.ctIsFilledWithObjects[ctid]) {
                serverGetMarks (false);
            } else {
                createAndShowObjList (backToComponent);
            }
            res = false;
        } else {
            Map.currentObjectClass = cl;
            Map.currentObjectItem = id;
            Map.currentObject = oj;
            if (cl > 0) {
                PathFinder.clear ();
            }
            parent.theMap.selectionChanged ();

            if (cl == Map.CLASS_PC) {
                Map.selectionX = Map.pcx[id];
                Map.selectionY = Map.pcy[id];
                parent.theMap.repaint ();
            } else if (cl == Map.CLASS_STREET) {
                selectStreet (id);
            } else if (cl == Map.CLASS_LABEL) {
                int seg = id >> 16;
                id &= 0xffff;
                Map.selectionX = Map.lbx[seg][id];
                Map.selectionY = Map.lby[seg][id];

                parent.theMap.moveFocus (Map.selectionX, Map.selectionY, Map.selectionX,
                                         Map.selectionY, 4, false);

                Map.curCategory = Map.lbct[seg][id];
                int cti = Map.lbct[seg][id];
                Map.setLayer (getCategoryAncestor (cti), true);
            } else if (cl == Map.CLASS_ADDRESS) {
                int seg = id >> 16;
                id &= 0xffff;
                Map.selectionX = Map.blx[seg][id];
                Map.selectionY = Map.bly[seg][id];

                parent.theMap.moveFocus (Map.selectionX, Map.selectionY, Map.selectionX,
                                         Map.selectionY, 4, false);
            }
        }
        Map.showDistance = true;
        return res;
    }

    /**
     * Load points
     */
    private void serverGetPoints () {
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.METHOD_GET_POINTS, false, false);
            ol.setOnlineLoaderListener (this);
            int ct = Map.curCategory; //Map.ctUid[Map.curCategory];
            Object cto = Map.ctLocalToRemote.get (new Integer (ct));
            ol.addParameter (REQUEST_PARAMETER_CATEGORY_ID, cto);
            ol.go ();
        }
    }

    /**
     * Load points
     * @param isInBackground boolean
     */
    public void serverGetMarks (boolean isInBackground) {
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            int ct = Map.curCategory;
            Object cto = Map.ctLocalToRemote.get (new Integer (ct));
            Vector typeList = new Vector ();
            typeList.addElement (cto);
            callGetMarks (typeList, isInBackground);
        }
    }

    /**
     * Load marks in background
     */
    public void serverGetMarksInBackground () {
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            Vector typeList = new Vector ();
            for (int i = Map.CATEGORY_BUILT_IN_COUNT; i < Map.ctN; i++) {
                if (Map.ctMode[i] == Map.CATEGORY_MODE_DOWNLOAD_MARKS) {
                    typeList.addElement (Map.ctLocalToRemote.get (new Integer (i)));
                }
            }
            callGetMarks (typeList, true);
        }
    }

    private void callGetMarks (Vector typeList, boolean isInBackground) {
        OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.METHOD_GET_MARKS, isInBackground, isInBackground);
        ol.setOnlineLoaderListener (this);
        ol.addParameter (REQUEST_PARAMETER_TYPE_LIST, typeList);
        ol.go ();
    }

    /**
     * Remove mark
     */
    public void serverRemoveMark () {
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR &&
            Map.currentObjectClass == Map.CLASS_LABEL && Map.currentObjectItem >= 0) {
            int seg = Map.currentObjectItem >> 16;
            int id = Map.currentObjectItem & 0xffff;
            int uid = Map.lbUid[seg][id];
            Map.getInstance ().deselectObject ();

            OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.METHOD_REMOVE_MARK, false, false);
            ol.setOnlineLoaderListener (this);
            ol.addParameter (REQUEST_PARAMETER_ID, uid);
            Vector typeList = new Vector();
            typeList.addElement(Map.ctLocalToRemote.get(new Integer(Map.lbct[seg][id])));
            ol.addParameter(REQUEST_PARAMETER_TYPE_LIST, typeList);
            ol.go ();
        }
    }

    /**
     * Get categories
     */
    private void serverGetCategories () {
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.METHOD_GET_CATEGORIES, false, false);
            ol.setOnlineLoaderListener (this);
            ol.go ();
        }
    }

    /**
     * Get point info
     * @param id int
     */
    private void serverGetPointInfo (int id) {
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.METHOD_GET_POINT_INFO, false, false);
            ol.setOnlineLoaderListener (this);
            ol.addParameter (REQUEST_PARAMETER_POINT_ID, id);
            ol.go ();
        }
    }

    /**
     * Get addresses
     */
    public void serverGetAddresses () {
        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR) {
            OnlineLoader ol = parent.createOnlineLoader (OnlineLoader.METHOD_GET_ADDRESSES, false, false);
            ol.setOnlineLoaderListener (this);
            ol.addParameter (REQUEST_PARAMETER_STREET_NAME, getStreetName (Map.currentObjectItem));
            ol.addParameter (REQUEST_PARAMETER_STREET_BOUNDS, getStreetBounds (Map.currentObjectItem));
            ol.go ();
            Map.curCategory = Map.CATEGORY_SYSTEM_STREET_ADDRESSES;
        }
    }

    /**
     * Select object by UID
     * @param link Hashtable
     */
    public void select (Hashtable link) {
        int id = Integer.parseInt ((String) link.get ("id"));

        // if point with uid is loaded select it and return
        if (selectPointByUid (id))
            return;

        String s = (String) link.get ("categoryId");
        if (s != null) {
            int rem = Integer.parseInt (s); // remote id
            Integer loci = (Integer) Map.ctRemoteToLocal.get (new Integer (rem));
            if (loci != null) {
                Map.curCategory = loci.intValue (); // local
                serverGetPoints ();
                pointIdToLoad = id;
                selectLoadedPoint = true;
            }
            return;
        }

        // check if categories are loaded
        if (!areOnlineCategoriesLoaded) {
            continueLoad = true;
            pointIdToLoad = id;
            selectLoadedPoint = true;
            serverGetCategories ();
            return;
        }

        // load point
        selectLoadedPoint = true;
        serverGetPointInfo (id);
    }

    /**
     * Select point by given UID. Returns true if point is found, false otherwise
     * @param id int
     * @return boolean
     */
    private boolean selectPointByUid (int id) {
        boolean uidIsFound = false;
        for (int seg = 0; seg < Map.SEGMENT_LIMIT; seg++)
            for (int i = 0; i < Map.lbN[seg]; i++) {
                if (Map.lbUid[seg][i] == id) {
                    // point is already loaded, select it
                    Map.changeFocus = true;
                    select (Map.MASK_LABEL | (seg << 16) | i);
                    parent.changeDisplay (msrt.DISPLAY_MAP);
                    uidIsFound = true;
                }
            }
        return uidIsFound;
    }

    /**
     * Select specified street.
     * @param st street id.
     */
    void selectStreet (int st) {
        int[] bounds = getStreetBounds (st);
        Map.selectionX = (bounds[0] + bounds[2]) / 2;
        Map.selectionY = (bounds[1] + bounds[3]) / 2;
        parent.theMap.moveFocus (bounds[0], bounds[1], bounds[2], bounds[3], 4, false);
        Map.currentObjectClass = 2;
        Map.showDistance = true;
    }

    /**
     * Get street bounds
     * @param st int
     * @return short[]
     */
    public int[] getStreetBounds (int st) {
        int[] res = new int[4];
        short xmin = 0x7fff, xmax = 0, ymin = 0x7fff, ymax = 0;
        Map.curElementsN = 0;

        for (int el = 0; el < Map.elementN; el++)
            if (Map.el2st[el] == Map.currentObjectItem) {
                Map.curElements[Map.curElementsN++] = el;

                short beg = Map.elconp[el], end = Map.elconp[el + 1];

                for (int j = beg; j < end; j++) {
                    short pn = Map.elcon[j];

                    short x = Map.pcx[pn];
                    short y = Map.pcy[pn];

                    if (x < xmin)
                        xmin = x;
                    if (x > xmax)
                        xmax = x;
                    if (y < ymin)
                        ymin = y;
                    if (y > ymax)
                        ymax = y;
                }
            }
        res[0] = xmin;
        res[1] = ymin;
        res[2] = xmax;
        res[3] = ymax;
        return res;
    }

    /**
     * Server request is completed
     * @param errorCode int
     * @param onlineLoader OnlineLoader
     */
    public void serverRequestComplete (int errorCode, OnlineLoader onlineLoader) {
        if (onlineLoader.isInBackground ()) {
            Map.update = Map.needpaint = true;
            Map.getInstance ().repaint ();
            return;
        }

        boolean show = true;

        if (errorCode == OnlineLoaderListener.CODE_OK) {
            if (onlineLoader.getMethod ().equals (OnlineLoader.METHOD_GET_POINTS) ||
                onlineLoader.getMethod ().equals (OnlineLoader.METHOD_GET_MARKS)) {
                Map.ctIsFilledWithObjects[Map.curCategory] = true;
            }

//            if (onlineLoader.getMethod ().equals (OnlineLoader.METHOD_REMOVE_MARK)) {
//                int ct = Map.lbct[Map.currentObjectItem >> 16][Map.currentObjectItem & 0xffff];
//                if (ct > 0) {
//                    Map.lbct[Map.currentObjectItem >> 16][Map.currentObjectItem & 0xffff] = Map.CATEGORY_NONE;
//                    Map.getInstance ().deselectObject ();
//                    Map.curCategory = ct;
//                    serverGetMarks (true);
//                    show = false;
//                }
//            }

            if (continueLoad) {
                serverGetPointInfo (pointIdToLoad);
                show = false;
            } else if (selectLoadedPoint) {
                selectPointByUid (pointIdToLoad);
                selectLoadedPoint = false;
                show = false;
            } else if (show) {
                createAndShowObjList (null);
            }
        }

        continueLoad = false;

        if (show)
            parent.changeDisplay (msrt.DISPLAY_OBJLIST);
    }

    private static final Character FIELD_ID = new Character ('I');

    public Hashtable getPointInfo (int uid) {
        InputStream is = parent.getClass ().getResourceAsStream (DataLoader.file_info);
        if (is == null)
            return null;

        Hashtable data = null;
        try {
            DataInputStream in = new DataInputStream (is);

            in.readByte ();
            int n = in.readInt ();

            for (int i = 0; i < n; i++) {
                data = (Hashtable) OnlineLoader.readObject (in);

                int id = ((Integer) data.get (FIELD_ID)).intValue ();
                if (id == uid) {
                    return data;
                }
            }
            in.close ();
        } catch (IOException ex) {}

        return null;
    }
}
