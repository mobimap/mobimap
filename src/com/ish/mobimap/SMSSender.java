/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                            (c) 2003-2005 ISh                              */
/*****************************************************************************/

package com.ish.mobimap;

import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import javax.wireless.messaging.*;

import java.io.IOException;
import com.ish.mobimap.net.*;

public class SMSSender
    implements Runnable, NetActivityController
{
    String phoneAddress;
    String message;
    int count;
    msrt parent;
    Thread me;

    private NetActivityListener netActivityListener;

    public SMSSender (msrt parent)
    {
        this.parent = parent;
        netActivityListener = parent.getFrame();
    }

    public void send (String phoneNumber, String message)
    {
        this.phoneAddress = "sms://" + phoneNumber;
        this.message = message;
        this.count = 1;
        netActivityListener.netStart(this);
        parent.getFrame().setNetActivityString(msrt.Resources[101]);
        me = new Thread(this);
        me.start ();
    }


    public void run()
    {
        MessageConnection smsconn = null;
        byte alertType = msrt.ALERT_INFO;
        int msg = 123;

        try
        {
            /** Open the message connection. */
            smsconn = (MessageConnection)Connector.open (phoneAddress);

            TextMessage txtmessage = (TextMessage)smsconn.newMessage (MessageConnection.TEXT_MESSAGE);
            txtmessage.setAddress (phoneAddress);
            txtmessage.setPayloadText (message);

            for (int i=0; i < count; i++)
                smsconn.send (txtmessage);
        }
        catch (Throwable t)
        {
            alertType = msrt.ALERT_ERROR;
            msg = 122;
        }
        if (smsconn != null)
        {
            try {
                smsconn.close();
            }
            catch (IOException ioe) { }
        }
        netActivityListener.netStop();
        parent.showInfo(alertType, msrt.Resources[msg]);
    }

    public void cancel() {}
    public boolean isInBackground()
    {
        return false;
    }

}
