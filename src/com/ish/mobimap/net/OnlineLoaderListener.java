package com.ish.mobimap.net;

import com.ish.mobimap.*;

public interface OnlineLoaderListener
{
    public static final byte CODE_OK = 0;
    public static final byte CODE_ERROR = -1;
    public static final byte CODE_CANCEL = 1;

    /**
     * This function is called after processing online request.
     * Implementation should switch screen to desired displayable or make some
     * clean up in case of failure.
     * @param errorCode int code, one of CODE_* constants
     * @param onlineLoader OnlineLoader that calls this method
     */
    public void serverRequestComplete (int errorCode, OnlineLoader onlineLoader);
}
