/*****************************************************************************/
/*                               m f i n i t y                               */
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                         (c) 2003-2008 Ilya Shakhat                        */
/*****************************************************************************/

package com.ish.mobimap;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;

import com.ish.mobimap.net.*;
import com.ish.mobimap.radar.*;

public class OnlineLoader implements Runnable, NetActivityController, CommandListener {
    private msrt parent;

    /**
     * True, if request was cancelled
     */
    private boolean mCancel;

    /**
     * Connection
     */
    private HttpConnection streamCon = null;

    /**
     * InputStream from connection
     */
    private InputStream inputStream = null;

    private String url;
    private Thread me;

    /**
     * Silent mode, don't show any messages
     */
    private boolean isSilent;

    /**
     * Background mode, don't show progress-bar
     */
    private boolean inBackground;

    /**
     * Net activity listener
     */
    private NetActivityListener netActivityListener;

    /**
     * Remote method name
     */
    private String method;
    /**
     * Method invocation parameters
     */
    private Hashtable params;

    /**
     * Handler for end-of-transmission notifications
     */
    private OnlineLoaderListener loaderListener;

    /**
     * Information or error description
     */
    private Alert infoAlert;

    /**
     * Error code - one of OnlineLoaderListener.CODE_* consts
     */
    private int errorCode;

    /**
     * Text contents of message block
     */
    private String messageText;

    /**
     * Image contents of message block
     */
    private Image messageImage;

    // Protocol version
    static final String PROTOCOL_VERSION = "100";

    // Remote method names
    public static final String METHOD_GET_PAGE = "Server.getPage";
    public static final String METHOD_GET_CATEGORIES = "Server.getCategories";
    public static final String METHOD_GET_POINTS = "Server.getPoints";
    public static final String METHOD_GET_POINT_INFO = "Server.getPoint";
    public static final String METHOD_GET_ADDRESSES = "Server.getAddresses";
    public static final String METHOD_GET_USER_BALANCE_TEXT = "Server.getUserBalanceText";
    public static final String METHOD_ON_START = "Server.onStart";
    public static final String METHOD_GET_SUBSCRIBER_LIST = "Server.getSubscriberList";
    public static final String METHOD_ADD_SUBSCRIBER = "Server.addSubscriber";
    public static final String METHOD_REMOVE_SUBSCRIBER = "Server.removeSubscriber";
    public static final String METHOD_GET_LBS_TRACK_LIST = "Server.getLbsTrackList";
    public static final String METHOD_SET_TRACKING = "Server.setTracking";
    public static final String METHOD_SET_SUBSCRIBER_PROPERTIES = "Server.setSubscriberProperties";
    public static final String METHOD_ADD_MARK = "Server.addMark";
    public static final String METHOD_REMOVE_MARK = "Server.removeMark";
    public static final String METHOD_GET_MARKS = "Server.getMarks";

    // Packet names
    private static final String PACKET_BROWSER = "Page";
    private static final String PACKET_CATEGORIES = "Categories";
    private static final String PACKET_OBJECTS = "Points";
    private static final String PACKET_MESSAGE = "Message";
    private static final String PACKET_LOCATOR = "SubscriberList";
    private static final String PACKET_MARK_TYPES = "MarkTypes";
    private static final String PACKET_MARKS = "Marks";

    // Packet attributes
    private static final String PACKET_ATTRIBUTE_TYPE = "type";

    // IO error codes
    private static final int ERROR_SUCCESS = 0;
    private static final int ERROR_SECURITY = 101;
    private static final int ERROR_CONNECTION_NOT_FOUND = 102;
    private static final int ERROR_ILLEGAL_ARGUMENT = 103;
    private static final int ERROR_IO_CONNECTOR = 111;
    private static final int ERROR_IO_REQUEST_BUILD = 112;
    private static final int ERROR_IO_REQUESTMETHOD = 113;
    private static final int ERROR_IO_OUTPUTSTREAM = 121;
    private static final int ERROR_IO_INPUTSTREAM = 131;
    private static final int ERROR_IO_GETRESPONSECODE = 132;
    private static final int ERROR_IO_READING = 133;
    private static final int ERROR_PROTOCOL = 1000;

    private long timeOfRequest;

    /**
     * Create new OnlineLoader.
     * @param parent msrt
     * @param url String servlet address
     * @param netActivityListener NetActivityListener listene, that is notified about net activity
     * @param isSilent boolean true, if any messages except errors are ignored
     * @param inBackground boolean true, if progress-bar is invisible (just status icon is shown)
     */
    protected OnlineLoader (msrt parent, String url, NetActivityListener netActivityListener,
                            boolean isSilent, boolean inBackground) {
        this.parent = parent;
        this.url = url;
        this.netActivityListener = netActivityListener;
        this.isSilent = isSilent;
        this.inBackground = inBackground;

        params = new Hashtable ();

        infoAlert = new Alert (msrt.Resources[11]);
        infoAlert.setCommandListener (this);
        infoAlert.setTimeout (Alert.FOREVER);

        parent.getFrame ().setNetActivityString (msrt.Resources[67]);
    }

    public void commandAction (Command c, Displayable s) {
        if (loaderListener != null)
            loaderListener.serverRequestComplete (errorCode, this);
        else
            parent.display.setCurrent (parent.getFrame ());
    }

    /**
     * Start new session
     */
    public void go () {
        me = new Thread (this);
        me.start ();
        mCancel = false;
    }

    public void run () {
        if (msrt.BUILD_VERSION != msrt.BUILD_RADAR)
            return;

        try {
            prepareToSendRequest ();
            parent.statistics[msrt.STATISTICS_ONLINE_REQUEST_COUNTER]++;
            timeOfRequest = System.currentTimeMillis ();

            // workaround Nokia Series 40 EMU (mb real device too?) bug
            boolean addUserAgent = msrt.vendor != msrt.VENDOR_NOKIA;

            errorCode = OnlineLoaderListener.CODE_OK;

            AlertType messageType = AlertType.INFO;
            String messageTitle = msrt.Resources[11];

            try {
                sendRequest (addUserAgent);

                readResponce ();

                parent.statistics[msrt.STATISTICS_ONLINE_RESPONCE_COUNTER]++;
            } catch (OnlineLoaderException ex) {
                if (!mCancel) {
                    // error has occured
                    messageText = parent.Resources[ex.errorCode < 200 ? 54 : 52];
                    messageText += ex.errorCode;
                    if (ex.getMessage () != null)
                        messageText += "\n" + ex.getMessage ();

                    messageType = AlertType.ERROR;
                    messageTitle = msrt.Resources[17];

                    errorCode = OnlineLoaderListener.CODE_ERROR;
                }
            } catch (OutOfMemoryError e) {
                messageText = msrt.Resources[9];
                messageType = AlertType.ERROR;
                messageTitle = msrt.Resources[17];
                errorCode = OnlineLoaderListener.CODE_ERROR;
            }

            if (mCancel) {
                // close connection and input stream
                close ();

                return; // if process was cancelled, just quit
            }

            netActivityListener.netStop ();

            if (messageText != null && !isSilent) {
                // show message loader isn't in background or if there was an error
                infoAlert.setString (messageText);
                infoAlert.setTitle (messageTitle);
                infoAlert.setType (messageType);
                infoAlert.setImage (messageImage);
                parent.display.setCurrent (infoAlert);
            } else {
                // immediately notify listeners
                if (loaderListener != null)
                    loaderListener.serverRequestComplete (errorCode, this);
            }
            parent.statistics[msrt.STATISTICS_ONLINE_TIME] +=
                (System.currentTimeMillis () - timeOfRequest) / 1000;

            // close connection and inout stream
            close ();
        } catch (NullPointerException e) {
            e.printStackTrace ();
        }
    }

    /**
     * Cancel loading data.
     * It's not possible to close input stream, because:
     * 1. stream can not be closed from main midlet thread (phone can halt);
     * 2. closing stream kills connection on some Nokia Series40 devices.
     */
    public void cancel () {
        mCancel = true;
        netActivityListener.netStop ();

        if (loaderListener != null)
            loaderListener.serverRequestComplete (OnlineLoaderListener.CODE_CANCEL, this);
    }

    /**
     * True, if in silent mode
     * @return boolean
     */
    public boolean isInBackground () {
        return inBackground;
    }

    /**
     * Method getter
     * @return String
     */
    public String getMethod () {
        return method;
    }

    /**
     * MessageText getter
     * @return String
     */
    public String getMessageText () {
        return messageText;
    }

    /**
     * Prepare to send request
     */
    private void prepareToSendRequest () {
        netActivityListener.netStart (this);
    }

    /**
     * Send request to server.
     * @param addUserAgent boolean true, if user-agent HTTP-parameter is added
     * @throws OnlineLoaderException
     */
    private void sendRequest (boolean addUserAgent) throws OnlineLoaderException {
        // Open http connection
        try {
            streamCon = (HttpConnection) Connector.open (url);
        } catch (IllegalArgumentException ex) {
            throw new OnlineLoaderException (ERROR_ILLEGAL_ARGUMENT);
        } catch (ConnectionNotFoundException ex) {
            throw new OnlineLoaderException (ERROR_CONNECTION_NOT_FOUND);
        } catch (IOException ex) {
            throw new OnlineLoaderException (ERROR_IO_CONNECTOR);
        } catch (SecurityException ex) {
            throw new OnlineLoaderException (ERROR_SECURITY);
        }

        // Build POST request
        byte[] binRequest = null;
        try {
            binRequest = buildRequest ();
        } catch (IOException ex3) {
            throw new OnlineLoaderException (ERROR_IO_REQUEST_BUILD);
        }

        // Set HTTP request parameters
        try {
            streamCon.setRequestMethod (HttpConnection.POST);
            streamCon.setRequestProperty ("Connection", "close");

            if (addUserAgent)
                streamCon.setRequestProperty ("User-Agent",
                                              "Profile/MIDP-1.0 Configuration/CLDC-1.0");
            streamCon.setRequestProperty ("Content-Language", "en-US");
            streamCon.setRequestProperty ("Accept", "application/octet-stream");
            streamCon.setRequestProperty ("Content-Type",
                                          "application/x-www-form-urlencoded");

            streamCon.setRequestProperty ("Content-Length",
                                          Integer.toString (binRequest.length));
        } catch (IOException ex1) {
            throw new OnlineLoaderException (ERROR_IO_REQUESTMETHOD);
        }

        // Write HTTP request to output stream
        OutputStream os;
        try {
            os = streamCon.openOutputStream ();
            os.write (binRequest);
        } catch (IOException ex2) {
            throw new OnlineLoaderException (ERROR_IO_OUTPUTSTREAM);
        }
        try {
            os.close ();
        } catch (Exception ex7) {
            // don't care about exception while close()
        }

        parent.statistics[msrt.STATISTICS_TRAFFIC_OUTGOING] += binRequest.length;
    }

    /**
     * Read server responce.
     * @throws OnlineLoaderException
     */
    private void readResponce () throws OnlineLoaderException {
        // Open input stream
        try {
            inputStream = streamCon.openInputStream ();
        } catch (IOException ex3) {
            throw new OnlineLoaderException (ERROR_IO_INPUTSTREAM);
        }

        // read response code
        int responseCode = 0;
        try {
            responseCode = streamCon.getResponseCode ();
        } catch (IOException ex4) {
            throw new OnlineLoaderException (ERROR_IO_GETRESPONSECODE);
        }
        if (responseCode != 200) {
            // something is wrong at server-side, result is not HTTP OK
            throw new OnlineLoaderException (responseCode);
        }

        try {
            // all data will be loaded via DIS, not IS
            DataInputStream input = new DataInputStream (inputStream);

            int len = input.readInt ();

            if (len != 0 && !mCancel) {
                // vector of packets
                int typeVector = input.readByte (); // type = vector
                if (typeVector != DATA_TYPE_VECTOR)
                    throw new OnlineLoaderException (ERROR_PROTOCOL + typeVector);

                int packetCount = input.readInt ();

                for (int i = 0; i < packetCount; i++) {
                    input.readByte (); // type = vector
                    input.readInt (); // =2

                    Hashtable packetAttributes = (Hashtable) readObject (input);
                    String type = (String) packetAttributes.get (PACKET_ATTRIBUTE_TYPE);

                    if (type.equals (PACKET_MESSAGE)) {
                        readMessage (input);
                    } else if (type.equals (PACKET_OBJECTS)) {
                        readPoints (input);
                    } else if (type.equals (PACKET_CATEGORIES)) {
                        readCategories (input, Map.CATEGORY_MODE_DOWNLOAD_POINTS);
                    } else if (type.equals (PACKET_MARK_TYPES)) {
                        readCategories (input, Map.CATEGORY_MODE_DOWNLOAD_MARKS);
                    } else if (type.equals (PACKET_BROWSER)) {
                        parent.theBrowser.read (input);
                    } else if (type.equals (PACKET_LOCATOR)) {
                        if (msrt.BUILD_VERSION == msrt.BUILD_RADAR)
                            parent.theLocator.readAbonentsChunk (input, method);
                    } else if (type.equals (PACKET_MARKS)) {
                        readPoints (input);
                    } else
                        readObject (input); // read everything we can't understand
                }
            }
            parent.statistics[msrt.STATISTICS_TRAFFIC_INCOMING] += len;
        } catch (OnlineLoaderException ex) {
            throw ex; // forward upwards
        } catch (IOException ex) {
            throw new OnlineLoaderException (ERROR_IO_READING);
        } catch (RuntimeException re) {
            re.printStackTrace ();
        }
    }

    /**
     * Close stream and connection.
     * This method is called from run() and after all user-oriented actions
     * had been done. So if closing connection take some time to be completed,
     * users will feel nothing.
     */
    private void close () {
        if (inputStream != null) {
            try {
                inputStream.close ();
            } catch (IOException ex5) {
            }
        }
        if (streamCon != null) {
            try {
                streamCon.close ();
            } catch (IOException ex6) {
            }
        }
    }


    // Message packet attributes
    private static final Character FIELD_MESSAGE_TEXT = new Character ('T');
    private static final Character FIELD_MESSAGE_ICON = new Character ('I');

    /**
     * Read message packet
     * @param in DataInputStream
     * @return boolean
     * @throws IOException
     */
    private boolean readMessage (DataInputStream in) throws IOException {
        Hashtable ht = (Hashtable) readObject (in);
        String text = (String) ht.get (FIELD_MESSAGE_TEXT);
        byte[] iconData = (byte[]) ht.get (FIELD_MESSAGE_ICON);

        messageText = text;
        try {
            if (iconData != null)
                messageImage = Image.createImage (iconData, 0, iconData.length);
        } catch (IllegalArgumentException ex) {
        }

        return text != null;
    }

    // Categories packet attributes
    private static final char FIELD_CATEGORY_ID = 'i';
    private static final char FIELD_CATEGORY_PARENT = 'P';
    private static final char FIELD_CATEGORY_COLOR = 'C';
    private static final char FIELD_CATEGORY_NAME = 'N';
    private static final char FIELD_CATEGORY_ICON = 'I';
    private static final char FIELD_CATEGORY_FONTSTYLE = 'F';
    private static final char FIELD_CATEGORY_IS_ONLINE = 'O';
    private static final char FIELD_CATEGORY_STARTUP_VISIBILITY = 'V';
    private static final char FIELD_CATEGORY_SHOW = 'S';

    private void readCategories (DataInputStream in, byte mode) throws IOException {
        readCategoriesData (in, mode);

        // recreate layers form
        parent.createLayersForm ();
        parent.theObjectManager.areOnlineCategoriesLoaded = true;
    }

    /**
     * Read categories packet
     * @param in DataInputStream
     * @param mode byte category mode, one of Map.CATEGORY_MODE_* constants
     * @throws IOException
     */
    static public void readCategoriesData (DataInputStream in, byte mode) throws IOException {
        /**
         * first, read data into temporary buffers
         */

        // array of categories
        in.readByte ();
        int count = in.readInt ();

        if (count == 0)
            return;

        // store old categories data
        int s_ctN = Map.ctN;
        int[] s_ctColor = Map.ctColor;
        short[] s_ctParent = Map.ctParent;
        short[] s_ctFontStyle = Map.ctFontStyle;
        Image[] s_ctIcon = Map.ctIcon;
        char[][] s_ctName = Map.ctName;
        byte[] s_ctIsDownloaded = Map.ctMode;
        boolean[] s_ctIsStartupVisibility = Map.ctIsStartupVisibility;
        boolean[] s_ctIsFilledWithObjects = Map.ctIsFilledWithObjects;
        byte[] s_ctShow = Map.ctShow;

        // alloc arrays for new data
        DataLoader.allocCtVars (s_ctN + count);

        // copy old data to new arrays
        System.arraycopy (s_ctColor, 0, Map.ctColor, 0, s_ctN);
        System.arraycopy (s_ctParent, 0, Map.ctParent, 0, s_ctN);
        System.arraycopy (s_ctFontStyle, 0, Map.ctFontStyle, 0, s_ctN);
        System.arraycopy (s_ctIcon, 0, Map.ctIcon, 0, s_ctN);
        System.arraycopy (s_ctName, 0, Map.ctName, 0, s_ctN);
        System.arraycopy (s_ctIsDownloaded, 0, Map.ctMode, 0, s_ctN);
        System.arraycopy (s_ctIsStartupVisibility, 0, Map.ctIsStartupVisibility, 0, s_ctN);
        System.arraycopy (s_ctIsFilledWithObjects, 0, Map.ctIsFilledWithObjects, 0, s_ctN);
        System.arraycopy (s_ctShow, 0, Map.ctShow, 0, s_ctN);

        Object[] parents = new Object[Map.ctN];

        // load data from input stream
        for (int i = s_ctN; i < Map.ctN; i++) {
            // hashtable size
            int hashtableType = in.readByte ();
            int hashtableSize = in.readInt ();

            Object id = null, parentId = null;
            int color = 0, fontStyle = 0;
            Image icon = null;
            char[] name = null;
            boolean isStartupVisibility = false;
            byte show = 0x7F;

            for (int j = 0; j < hashtableSize; j++) {
                // read key
                int keyType = in.readByte ();
                char key = in.readChar ();

                // read value
                switch (key) {
                    case FIELD_CATEGORY_ID:
                        id = readObject (in);
                        break;
                    case FIELD_CATEGORY_COLOR:
                        in.readByte ();
                        color = in.readInt ();
                        break;
                    case FIELD_CATEGORY_NAME:
                        in.readByte ();
                        name = in.readUTF ().toCharArray ();
                        break;
                    case FIELD_CATEGORY_PARENT:
                        parents[i] = readObject (in);
                        break;
                    case FIELD_CATEGORY_FONTSTYLE:
                        in.readByte ();
                        fontStyle = in.readInt ();
                        break;
                    case FIELD_CATEGORY_ICON:
                        byte[] b = (byte[]) readObject (in);
                        try {
                            icon = Image.createImage (b, 0, b.length);
                        } catch (IllegalArgumentException ex) {
                        }
                        break;
                    case FIELD_CATEGORY_STARTUP_VISIBILITY:
                        in.readByte ();
                        isStartupVisibility = in.readBoolean ();
                        break;
                    case FIELD_CATEGORY_SHOW:
                        in.readByte ();
                        show = in.readByte ();
                        break;
                    default:
                        // extra data
                        readObject (in);
                }
            }

            // store received data into buffers
            Map.ctColor[i] = color;
            Map.ctIcon[i] = icon;
            Map.ctFontStyle[i] = (short) fontStyle;
            Map.ctName[i] = name;
            Map.ctMode[i] = mode;
            Map.ctIsStartupVisibility[i] = isStartupVisibility;
            Map.ctShow[i] = show;
            Map.ctRemoteToLocal.put (id, new Integer (i));
            Map.ctLocalToRemote.put (new Integer (i), id);
        }

        // reindex parents for remote categories
        for (int i = s_ctN; i < Map.ctN; i++) {
            if (parents[i] != null) {
                Integer idx = (Integer) Map.ctRemoteToLocal.get (parents[i]);
                if (idx != null) {
                    Map.ctParent[i] = (short) idx.intValue ();
                }
            }
        }
    }

    // Points packet attributes
    private static final char FIELD_POINT_ID = 'i';
    private static final char FIELD_POINT_CATEGORY = 'C';
    private static final char FIELD_POINT_TYPE = 'T';
    private static final char FIELD_POINT_X = 'X';
    private static final char FIELD_POINT_Y = 'Y';
    private static final char FIELD_POINT_NAME = 'N';
    private static final char FIELD_POINT_ICON = 'I';
    private static final char FIELD_POINT_LATITUDE = 'A';
    private static final char FIELD_POINT_LONGITUDE = 'B';
    private static final char FIELD_POINT_META = 'M';

    /**
     * Read points data from incoming stream
     * @param in DataInputStream
     * @throws IOException
     */
    private void readPoints (DataInputStream in) throws IOException {
        // clear all data dependent on points
        Map.srhN = 0;
        Map.currentObjectClass = Map.cursorObjectClass = 0;
        Map.needpaint = Map.update = true;
        PathFinder.clear ();

        int freeSegment = -1; // which segment is freed during removing of old points

        // check what method was called
        boolean isAddMarkMethod = METHOD_ADD_MARK.equals (method);
        if (!isAddMarkMethod) {
            // clear old loaded data
            Vector typeList = new Vector ();
            if (METHOD_GET_MARKS.equals (method) || METHOD_REMOVE_MARK.equals (method)) {
                typeList = (Vector) params.get ("obj.typeList");
            } else if (METHOD_GET_POINTS.equals (method)){
                typeList.addElement (params.get ("obj.categoryId"));
            }

            // iterate all large and small chunks
            for (int seg = Map.SEGMENT_FIRST_LARGE_CHUNK; seg < Map.SEGMENT_LAST_SMALL_CHUNK; seg++) {
                boolean empty = true;
                int removed = 0;
                for (int i = 0; i < Map.lbN[seg]; i++) {
                    int ctid = Map.lbct[seg][i];
                    Object remoteCt = Map.ctLocalToRemote.get (new Integer (ctid));
                    if (remoteCt == null) {
                        continue;
                    }
                    if (typeList.contains (remoteCt)) {
                        // remove element
                        Map.lbct[seg][i] = -1;
                        Map.img[Map.IMG_SEGMENT_DYNAMIC].remove (new Integer (Map.lbUid[seg][i]));
                        removed++;
                    } else {
                        empty = false;
                    }
                }
                if (empty) {  // clear the whole segment
                    // store as a free segment
                    if (seg < Map.SEGMENT_LAST_LARGE_CHUNK && freeSegment < 0) {
                        freeSegment = seg;
                    }

                    // clear the segment
                    Map.lbN[seg] = 0;
                    Map.lbx[seg] = null;
                    Map.lby[seg] = null;
                    Map.lbct[seg] = null;
                    Map.lbUid[seg] = null;
                    Map.lbnamep[seg] = null;
                } else if (removed > 0) { // realloc the segment
                    int newn = Map.lbN[seg] - removed;
                    short[] lbx = new short[newn];
                    short[] lby = new short[newn];
                    short[] lbct = new short[newn];
                    int[] lbUid = new int[newn];
                    short[] lbnamep = new short[newn + 1];
                    char[] name = new char[Map.name[seg].length];

                    for (int i = 0, targetI = 0, namep = 0; i < Map.lbN[seg]; i++) {
                        int ctid = Map.lbct[seg][i];
                        if (ctid > 0) {
                            lbx[targetI] = Map.lbx[seg][i];
                            lby[targetI] = Map.lby[seg][i];
                            lbct[targetI] = Map.lbct[seg][i];
                            lbUid[targetI] = Map.lbUid[seg][i];
                            lbnamep[targetI] = (short)namep;

                            int len = Map.lbnamep[seg][i+1] - Map.lbnamep[seg][i];
                            System.arraycopy(Map.name[seg], Map.lbnamep[seg][i], name, namep, len);
                            namep += len;

                            targetI++;
                            lbnamep[targetI] = (short)namep;
                        }
                    }

                    Map.lbN[seg] = newn;
                    Map.lbx[seg] = lbx;
                    Map.lby[seg] = lby;
                    Map.lbct[seg] = lbct;
                    Map.lbUid[seg] = lbUid;
                    Map.lbnamep[seg] = lbnamep;
                    Map.name[seg] = name;
                }
            }

            // check if some segment was freed
            if (freeSegment < 0) {
                // no segment was freed, let's reuse existing
                Map.segmentLargeChunk++;
                if (Map.segmentLargeChunk == Map.SEGMENT_LAST_LARGE_CHUNK) {
                    Map.segmentLargeChunk = Map.SEGMENT_FIRST_LARGE_CHUNK;
                }

                freeSegment = Map.segmentLargeChunk;
                Map.blN[freeSegment] = 0;
            }

            // remove old icons
            for (int i = 0; i < Map.lbN[freeSegment]; i++) {
                Map.img[Map.IMG_SEGMENT_DYNAMIC].remove (new Integer (Map.lbUid[freeSegment][i]));
            }
        } else {
            // add single point, store it in a small chunk
            for (int seg = Map.SEGMENT_FIRST_SMALL_CHUNK; seg < Map.SEGMENT_LAST_SMALL_CHUNK; seg++) {
                if (Map.lbN[seg] == 0) {
                    freeSegment = seg;
                    break;
                }
            }
            if (freeSegment < 0) {
                Map.segmentSmallChunk++;
                if (Map.segmentSmallChunk == Map.SEGMENT_LAST_SMALL_CHUNK) {
                    Map.segmentSmallChunk = Map.SEGMENT_FIRST_SMALL_CHUNK;
                }

                freeSegment = Map.segmentSmallChunk;
            }
        }

        // array of points
        in.readByte ();
        short lbN = (short) in.readInt ();

        short[] lbx = new short[lbN];
        short[] lby = new short[lbN];
        short[] lbct = new short[lbN];
        int[] lbUid = new int[lbN];
        short[] lbnamep = new short[lbN + 1];
        Hashtable[] lbMeta = new Hashtable[lbN];

        char[][] names = new char[lbN][];

        int namep = 0;
        for (int i = 0; i < lbN; i++) {
            // hashtable size
            int hashtableType = in.readByte ();
            int hashtableSize = in.readInt ();

            Image icon = null;
            int id = 0;

            for (int j = 0; j < hashtableSize; j++) {
                // read key
                int keyType = in.readByte ();
                char key = in.readChar ();

                // read value
                switch (key) {
                    case FIELD_POINT_ID:
                        in.readByte ();
                        id = lbUid[i] = in.readInt ();
                        break;
                    case FIELD_POINT_CATEGORY:
                    case FIELD_POINT_TYPE:
                        lbct[i] = ObjectManager.getCtRemoteToLocal (readObject (in));
                        break;
                    case FIELD_POINT_X:
                        in.readByte ();
                        lbx[i] = (short) (Map.x2local (in.readInt ()));
                        break;
                    case FIELD_POINT_Y:
                        in.readByte ();
                        lby[i] = (short) (Map.y2local (in.readInt ()));
                        break;
                    case FIELD_POINT_LATITUDE:
                        in.readByte ();
                        lby[i] = (short) (Map.latitude2y (in.readInt ()));
                        break;
                    case FIELD_POINT_LONGITUDE:
                        in.readByte ();
                        lbx[i] = (short) (Map.longitude2x (in.readInt ()));
                        break;
                    case FIELD_POINT_NAME:
                        in.readByte ();
                        names[i] = in.readUTF ().toCharArray ();
                        lbnamep[i] = (short) namep;
                        namep += names[i].length;
                        break;
                    case FIELD_POINT_META:
                        lbMeta[i] = (Hashtable) readObject (in);
                        break;
                    case FIELD_POINT_ICON:
                        byte[] b = (byte[]) readObject (in);
                        try {
                            icon = Image.createImage (b, 0, b.length);
                        } catch (Exception ex) {
                        }
                        break;
                    default:
                        // extra data
                        readObject (in);
                }
            }
            if (icon != null)
                Map.img[Map.IMG_SEGMENT_DYNAMIC].put (new Integer (id), icon);

            if (mCancel)
                throw new OnlineLoaderException (0);
        }

        // store loaded points into data segment
        Map.lbN[freeSegment] = lbN;

        Map.lbx[freeSegment] = lbx;
        Map.lby[freeSegment] = lby;
        Map.lbct[freeSegment] = lbct;
        Map.lbUid[freeSegment] = lbUid;
        Map.lbnamep[freeSegment] = lbnamep;
        Map.lbMeta[freeSegment] = lbMeta;

        // array of chars
        Map.lbnamep[freeSegment][lbN] = (short) namep;
        Map.name[freeSegment] = new char[namep];
        for (int i = 0; i < lbN; i++) {
            System.arraycopy (names[i], 0, Map.name[freeSegment], Map.lbnamep[freeSegment][i], names[i].length);
        }
    }

    //
    // PROTOCOL FUNCTION
    //

    /**
     * Encoding alphabet for Base64 algorithm
     * Alphabet according to RFC3548, par.4, The "URL and Filename safe" Base 64 Alphabet
     */
    private final static byte[] ENCODING_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes ();
    /**
     * Encode binary array in base64
     * @param source byte[]
     * @return byte[]
     */
    public static byte[] encode (byte[] source) {
        int requiredLength = 3 * ((source.length + 2) / 3);
        byte[] sourceBytes = new byte[requiredLength];
        System.arraycopy (source, 0, sourceBytes, 0, source.length);

        byte[] target = new byte[4 * (requiredLength / 3)];

        for (int i = 0, j = 0; i < requiredLength; i += 3, j += 4) {
            int b1 = sourceBytes[i] & 0xff;
            int b2 = sourceBytes[i + 1] & 0xff;
            int b3 = sourceBytes[i + 2] & 0xff;

            target[j] = ENCODING_ALPHABET[b1 >>> 2];
            target[j + 1] = ENCODING_ALPHABET[((b1 & 0x03) << 4) | (b2 >>> 4)];
            target[j + 2] = ENCODING_ALPHABET[((b2 & 0x0f) << 2) | (b3 >>> 6)];
            target[j + 3] = ENCODING_ALPHABET[b3 & 0x3f];
        }

        int numPadBytes = requiredLength - source.length;

        for (int i = target.length - numPadBytes; i < target.length; i++)
            target[i] = '=';
        return target;
    }

    /**
     * Form POST-request
     * @return byte[]
     * @throws IOException
     */
    private byte[] buildRequest () throws IOException {
        // serialize request parameters into byte[]
        ByteArrayOutputStream baos = new ByteArrayOutputStream ();
        DataOutputStream binaryStream = new DataOutputStream (baos);

        // request consists of two objects: method : String, params : Hashtable
        writeObject (binaryStream, method);
        writeObject (binaryStream, params);

        binaryStream.close ();

        // form post parameters
        String s = null;

        s = "k=" + parent.ownerKey +
            "&v=" + PROTOCOL_VERSION +
            "&b=" + msrt.PROGRAM_BUILD +
            "&p=" + msrt.profile[msrt.PROFILE_PACKAGE] +
            "&u=" + msrt.PROGRAM_USER_AGENT +
            "&c=" + parent.cityId +
            "&d=";

        // merge post parameters and serialized request parameters
        byte[] part1 = s.getBytes ();
        byte[] part2 = encode (baos.toByteArray ());
        byte[] full = new byte[part1.length + part2.length];
        System.arraycopy (part1, 0, full, 0, part1.length);
        System.arraycopy (part2, 0, full, part1.length, part2.length);

        return full;
    }

    // constants for data type encoding
    private static final int DATA_TYPE_EOF = 0x0;
    private static final int DATA_TYPE_BYTE = 0x1;
    private static final int DATA_TYPE_CHAR = 0x2;
    private static final int DATA_TYPE_SHORT = 0x3;
    private static final int DATA_TYPE_INT = 0x4;
    private static final int DATA_TYPE_LONG = 0x5;
    private static final int DATA_TYPE_BOOLEAN = 0x6;
    private static final int DATA_TYPE_DOUBLE = 0x7;
    private static final int DATA_TYPE_STRING_UTF8 = 0x11;
    private static final int DATA_TYPE_STRING_UTF16 = 0x12;
    private static final int DATA_TYPE_VECTOR = 0x21;
    private static final int DATA_TYPE_HASHTABLE = 0x31;
    private static final int DATA_TYPE_BINARY = 0x41;
    private static final int DATA_TYPE_EXCEPTION = 0x7F;

    /**
     * Write single object into stream.
     * Some types are not supported
     * @param binaryStream DataOutputStream
     * @param value Object
     * @throws IOException
     */
    public static void writeObject (DataOutputStream binaryStream, Object value) throws IOException {
        if (value instanceof Integer) {
            binaryStream.writeByte (DATA_TYPE_INT);
            binaryStream.writeInt (((Integer) value).intValue ());
        }
//        else if (value instanceof Byte)
//        {
//            binaryStream.writeByte (DATA_TYPE_BYTE);
//            binaryStream.writeByte (((Byte)value).byteValue());
//        }
//        else if (value instanceof Short)
//        {
//            binaryStream.writeByte (DATA_TYPE_SHORT);
//            binaryStream.writeShort (((Short)value).shortValue());
//        }
//        else if (value instanceof Character)
//        {
//            binaryStream.writeByte (DATA_TYPE_CHAR);
//            binaryStream.writeChar (((Character)value).charValue());
//        }
        else if (value instanceof Long) {
            binaryStream.writeByte (DATA_TYPE_LONG);
            binaryStream.writeLong (((Long) value).longValue ());
        } else if (value instanceof Boolean) {
            binaryStream.writeByte (DATA_TYPE_BOOLEAN);
            binaryStream.writeBoolean (((Boolean) value).booleanValue ());
        }
//        else if (value instanceof Double)
//        {
//            binaryStream.writeByte (DATA_TYPE_DOUBLE);
//            binaryStream.writeDouble (((Double)value).doubleValue());
//        }
        else if (value instanceof String) {
            binaryStream.writeByte (DATA_TYPE_STRING_UTF8);
            binaryStream.writeUTF ((String) value);
        } else if (value instanceof Vector) {
            binaryStream.writeByte (DATA_TYPE_VECTOR);
            Vector v = (Vector) value;
            int vsize = v.size ();
            binaryStream.writeInt (vsize);
            for (int i = 0; i < vsize; i++)
                writeObject (binaryStream, v.elementAt (i));
        } else if (value instanceof Hashtable) {
            binaryStream.writeByte (DATA_TYPE_HASHTABLE);
            Hashtable h = (Hashtable) value;
            int hsize = h.size ();
            binaryStream.writeInt (hsize);
            for (Enumeration e = h.keys (); e.hasMoreElements (); ) {
                Object key = e.nextElement ();
                writeObject (binaryStream, key);
                writeObject (binaryStream, h.get (key));
            }
        } else if (value instanceof byte[]) {
            binaryStream.writeByte (DATA_TYPE_BINARY);
            byte[] v = (byte[]) value;
            binaryStream.writeInt (v.length);
            binaryStream.write (v);
        } else if (value instanceof char[]) {
            binaryStream.writeByte (DATA_TYPE_STRING_UTF16);
            char[] v = (char[]) value;
            binaryStream.writeInt (v.length);
            for (int i = 0; i < v.length; i++)
                binaryStream.writeChar (v[i]);
        } else if (value instanceof int[]) {
            binaryStream.writeByte (DATA_TYPE_VECTOR);
            int[] v = (int[]) value;
            binaryStream.writeInt (v.length);
            for (int i = 0; i < v.length; i++)
                writeObject (binaryStream, new Integer (v[i]));
        }
    }

    /**
     * Read single data item from stream
     * @param dis DataInputStream
     * @return Object
     * @throws IOException
     */
    public static Object readObject (DataInputStream dis) throws IOException {
        byte type = dis.readByte ();
        Object value = null;

        switch (type) {
            case DATA_TYPE_EOF:
                throw new EOFException ();
            case DATA_TYPE_BYTE:
                value = new Byte (dis.readByte ());
                break;
            case DATA_TYPE_CHAR:
                value = new Character (dis.readChar ());
                break;
            case DATA_TYPE_SHORT:
                value = new Short (dis.readShort ());
                break;
            case DATA_TYPE_INT:
                value = new Integer (dis.readInt ());
                break;
            case DATA_TYPE_LONG:
                value = new Long (dis.readLong ());
                break;
            case DATA_TYPE_BOOLEAN:
                value = new Boolean (dis.readBoolean ());
                break;
//            case DATA_TYPE_DOUBLE:
//                value = new Double(dis.readDouble());
//                break;
            case DATA_TYPE_STRING_UTF8:
                value = dis.readUTF ();
                break;
            case DATA_TYPE_STRING_UTF16:
                int csize = dis.readInt ();
                char[] c = new char[csize];
                DataLoader.readStreamAsCharArray (dis, c, csize);
                value = c;
                break;
            case DATA_TYPE_VECTOR:
                Vector v = new Vector ();
                int vsize = dis.readInt ();
                for (int i = 0; i < vsize; i++)
                    v.addElement (readObject (dis));
                value = v;
                break;
            case DATA_TYPE_HASHTABLE:
                Hashtable h = new Hashtable ();
                int hsize = dis.readInt ();
                for (int i = 0; i < hsize; i++) {
                    Object rn = readObject (dis);
                    Object rv = readObject (dis);
                    h.put (rn, rv);
                }
                value = h;
                break;
            case DATA_TYPE_BINARY:
                int bsize = dis.readInt ();
                byte[] b = new byte[bsize];
                dis.readFully (b);
                value = b;
                break;
            case DATA_TYPE_EXCEPTION:
                int code = dis.readInt ();
                int extcode = dis.readInt ();
                String message = dis.readUTF ();
                throw new OnlineLoaderException (code, message);
        }
        return value;
    }

    /**
     * Set method and reset all parameters
     * @param method String
     */
    public void setMethod (String method) {
        this.method = method;
    }

    /**
     * Add one named parameter
     * @param name String
     * @param value Object
     */
    public void addParameter (String name, Object value) {
        if (value != null)
            params.put (name, value);
    }

    /**
     * Add one integer parameter
     * @param name String
     * @param value int
     */
    public void addParameter (String name, int value) {
        params.put (name, new Integer (value));
    }

    /**
     * Set listener handler
     * @param oll OnlineLoaderListener
     */
    public void setOnlineLoaderListener (OnlineLoaderListener oll) {
        this.loaderListener = oll;
    }
}
