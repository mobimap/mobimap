package com.ish.mobimap.ui;

import javax.microedition.lcdui.*;

public interface ComponentListener
{
    /**
     * Command is performed for Component
     * @param component Component
     * @param command Command
     */
    public void commandAction(Component component, Command command);
}
