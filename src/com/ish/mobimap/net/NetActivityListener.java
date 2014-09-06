package com.ish.mobimap.net;

import com.ish.mobimap.*;

public interface NetActivityListener
{
    /**
     * Start net activity. Controller is an object that contols this net activity
     * @param controller NetActivityController
     */
    public void netStart(NetActivityController controller);

    /**
     * Stop net activity
     */
    public void netStop();
}
