package com.flytesoft.persistent.notification.capacitorpersistentnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.flytesoft.persistent.notification.ForeGroundService;
import com.flytesoft.persistent.notification.PersistentNotification;

public class NotificationButtonReceiver extends BroadcastReceiver
{
    static private final String TAG = "FOREGROUND-RECEIVER";
    static private PersistentNotification persistentNotification = null;

    public static void setNotificationRef(PersistentNotification pn)
    {
        persistentNotification = pn;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(intent.getAction().startsWith(ForeGroundService.NOTIFICATION_BUTTON_ACTION))
        {
            Log.d(TAG, "Notification action button clicked: " + intent.getStringExtra("action"));
            if(persistentNotification != null)
            {
                persistentNotification.notifyActionClick(intent.getStringExtra("action"));
            }
        }
        else if(intent.getAction().equals(ForeGroundService.NOTIFICATION_CLICK_ACTION))
        {
            Log.d(TAG, "Notification general click.");
            if(persistentNotification != null)
            {
                persistentNotification.notifyActionClick();
            }
        }
    }
}
