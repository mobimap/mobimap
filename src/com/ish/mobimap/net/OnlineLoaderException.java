package com.ish.mobimap.net;

import java.io.IOException;

public class OnlineLoaderException
    extends IOException
{
    public int errorCode;

    public OnlineLoaderException (int errorCode)
    {
        this.errorCode = errorCode;
    }

    public OnlineLoaderException (int errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }
}
