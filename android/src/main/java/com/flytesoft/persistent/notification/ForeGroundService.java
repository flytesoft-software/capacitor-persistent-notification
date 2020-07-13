package com.flytesoft.persistent.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.Spanned;
import android.util.Log;

import com.flytesoft.persistent.notification.capacitorpersistentnotification.NotificationButtonReceiver;
import com.getcapacitor.JSArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ForeGroundService extends Service
{
    private final IBinder mBinder = new LocalBinder();

    private NotificationManager mNM = null;
    private Notification.Builder mBuilder = null;
    private Intent mMainIntent = null;
    private PendingIntent mAppIntent = null;

    private static boolean mIsBound = false;

    private PowerManager.WakeLock wakeLock = null;

    static private final String TAG = "FOREGROUND-SERVICE";
    static private final String NOTIFICATION_CHANNEL = "APP-FOREGROUND_NOTIFICATION-2";
    static private final String NOTIFICATION_NAME = "Persistent Notification";
    static private final String NOTIFICATION_DESCRIPTION = "Allows the app to run a continuous foreground service.";
    static private final int ONGOING_NOTIFICATION_ID = 78710;

    static public final String NOTIFICATION_BUTTON_ACTION = "PERSISTENT_BUTTON";
    static public final String NOTIFICATION_CLICK_ACTION = "PERSISTENT_ACTION";

    private Icon mIcon = null;
    private Icon mBigIcon = null;
    private String mIconLocation = "";
    private String mBigIconLocation = "";
    private JSArray mActions = new JSArray();
    private Spanned mTitle = null;
    private Spanned mContent = null;
    private int mColor = Color.BLUE;
    private Notification.BigTextStyle mTextStyle = null;

    private ArrayList<PendingIntent> mStoredButtonIntents = new ArrayList<PendingIntent>();


    public ForeGroundService()
    {

    }

    /*
     *  Not planning on using this, unless it really is needed.
     */
    @SuppressLint("WakelockTimeout")
    private void acquireWakeLock()
    {
        final PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PersistentNotification::WakeLock");
        wakeLock.acquire();
    }

    private void killWakeLock()
    {
        if(wakeLock != null)
        {
            if(wakeLock.isHeld())
            {
                wakeLock.release();
            }
            wakeLock = null;
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.i("FOREGROUND", "Service created. Bound: " + mIsBound);

        if(mIsBound)
        {
            createChannel();
            Notification note = createNotification();
            startForeground(ONGOING_NOTIFICATION_ID, note);
        }
        else
        {
            // TODO: Handle service restart here.
        }
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder
    {
        ForeGroundService getService() {
            return ForeGroundService.this;
        }
    }

    private void createChannel()
    {
        if(mNM == null)
        {
            mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_LOW);

            channel.setDescription(NOTIFICATION_DESCRIPTION);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

            mNM.createNotificationChannel(channel);
        }
    }

    public static void boundByApp()
    {
        mIsBound = true;
    }

    private Icon updateIcon(String iconLocation)
    {
        if(iconLocation != mIconLocation || mIcon == null) // Only update icon if icon location does not already exist or is different.
        {
            mIcon = createIcon((iconLocation));

            mIconLocation = iconLocation;
        }

        return mIcon;
    }

    private Icon updateBigIcon(String iconLocation)
    {
        if(iconLocation == null)
        {
            mBigIcon = null;
            return (Icon)null;
        }
        if(iconLocation.isEmpty())
        {
            mBigIcon = null;
            return (Icon)null;
        }

        if(iconLocation != mBigIconLocation ) // Only update icon if icon location does not already exist or is different.
        {
            mBigIcon = createIcon((iconLocation));

            mBigIconLocation = iconLocation;
        }

        return mBigIcon;
    }

    private Icon createIcon(String iconLocation)
    {
        return createIcon(iconLocation, false);
    }

    private Icon createIcon(String iconLocation, Boolean nullOK)
    {
        Icon newIcon = null;

        if(iconLocation != null) // Only update icon if icon location does not already exist or is different.
        {
            Bitmap bMap = null;

            try
            {
                final AssetManager assets = getResources().getAssets();
                final String assetLocation = "public/" + iconLocation;
                final InputStream iconFileStream = assets.open(assetLocation);
                bMap = BitmapFactory.decodeStream(iconFileStream);
            }
            catch(IOException e)
            {
                e.printStackTrace();
                Log.d(TAG, "Unable to create icon with provided asset, icon will default: " + e.toString());
            }

            if(bMap == null && !nullOK)
            {
                newIcon = Icon.createWithResource(this, android.R.drawable.ic_menu_info_details);
            }
            else
            {
                newIcon = Icon.createWithBitmap(bMap);
            }
        }
        else
        {
            newIcon = Icon.createWithResource(this, android.R.drawable.ic_menu_info_details);
        }

        return newIcon;
    }

    private void setActions(Notification.Builder builder, JSArray actions)
    {
        ArrayList<Notification.Action> notificationActions = new ArrayList<Notification.Action>();

        if(actions == null)
        {
            clearButtonIntents();
            builder.setActions(new Notification.Action[0]);
            return;
        }

        if(actions.length() == 0)
        {
            clearButtonIntents();
            builder.setActions(new Notification.Action[0]);
            return;
        }

        if(!actions.equals(mActions))
        {
            clearButtonIntents();

            if(actions != null)
            {
                if(actions.length() == 0)
                {
                    builder.setActions(new Notification.Action[0]);
                }
                else
                {
                    for(int i = 0; i < actions.length(); i++)
                    {
                        try
                        {
                            JSONObject obj = actions.getJSONObject(i);
                            String title = obj.getString("title");
                            String action = obj.getString("action");
                            String icon = obj.getString(("icon"));

                            if(title != null)
                            {
                                if(!title.isEmpty())
                                {
                                    Notification.Action notAction = createButtonNotificationActions(i, title, action, icon);
                                    if(notAction != null)
                                    {
                                        notificationActions.add(notAction);
                                    }
                                }
                            }
                        } catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    builder.setActions((Notification.Action[]) notificationActions.toArray(new Notification.Action[notificationActions.size()]));
                }
            }
            else
            {
                builder.setActions(new Notification.Action[0]);
            }
        }

        mActions = actions;
    }

    private Notification.Action createButtonNotificationActions(int index, String title, String action, String iconLocation)
    {
        String localAction = title;

        if(action != null)
        {
            if(!action.isEmpty())
            {
                localAction = action;
            }
        }

        if(title != null)
        {
            if(!title.isEmpty())
            {
                Intent intent = new Intent(this, NotificationButtonReceiver.class);
                intent.setAction(NOTIFICATION_BUTTON_ACTION + index);
                intent.putExtra("action", localAction);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                Notification.Action.Builder actionBuild =
                        new Notification.Action.Builder(
                                createIcon(iconLocation, true),
                                title,
                                pendingIntent );

                mStoredButtonIntents.add(pendingIntent);

                return actionBuild.build();
            }
        }

        return null;
    }

    private void clearButtonIntents()
    {
        for(int i = 0; i < mStoredButtonIntents.size(); i++)
        {
            mStoredButtonIntents.get(i).cancel();
        }

        mStoredButtonIntents.clear();
    }


    private Notification createNotification()
    {
        final Spanned title = PersistentNotification.getTitle();
        final Spanned content = PersistentNotification.getContent();
        final JSArray actions = PersistentNotification.getActions();
        final String color = PersistentNotification.getColor();
        final Icon littleIcon =  updateIcon(PersistentNotification.getIcon());
        final Icon bigIcon = updateBigIcon(PersistentNotification.getBadge());
        int rgbColor = mColor;

        if(!color.isEmpty())
        {
            try
            {
                rgbColor = Color.parseColor(color);
            }
            catch (Exception e)
            {
                Log.d(TAG, "Unable to parse color string, defaulting.");
            }
        }

        if(mMainIntent == null)
        {
            Intent intent = new Intent(this, NotificationButtonReceiver.class);
            intent.setAction(NOTIFICATION_CLICK_ACTION);

            mAppIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        if(mBuilder == null)
        {
            String notificationChannel = NOTIFICATION_CHANNEL;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            {
                notificationChannel = NotificationChannel.DEFAULT_CHANNEL_ID;
            }

            mTextStyle = new Notification.BigTextStyle()
                    .bigText(content)
                    .setBigContentTitle(title);

            mBuilder = new Notification.Builder(this, notificationChannel);

            if(mIcon == null)
            {
                Log.d(TAG, "Little icon is null");
            }

            mBuilder.setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(mIcon)
                    .setContentIntent(mAppIntent)
                    .setColor(rgbColor)
                    .setLargeIcon(mBigIcon)
                    .setStyle(mTextStyle)
                    .setOngoing(true);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                mBuilder.setSound(null, null);
            }
        }
        else // Only update what needs to be updated since creation.
        {
            boolean updateStyle = false;

            if(title != mTitle)
            {
                if(!title.equals(mTitle))
                {
                    updateStyle = true;
                    mBuilder.setContentTitle(title);
                    mTextStyle.setBigContentTitle(title);
                }
            }

            if(content != mContent)
            {
                if(!content.equals(mContent))
                {
                    updateStyle = true;
                    mBuilder.setContentText(content);
                    mTextStyle.bigText(content);
                }
            }

            if(rgbColor != mColor)
            {
                mBuilder.setColor(rgbColor);
            }

            if(littleIcon != mIcon)
            {
                mBuilder.setSmallIcon(mIcon);
            }

            if(bigIcon != mBigIcon)
            {
                mBuilder.setLargeIcon(bigIcon);
            }

            if(updateStyle)
            {
               mBuilder.setStyle( mTextStyle );
            }
        }

        setActions(mBuilder, actions); // Actions only updated upon initialization or change.

        mTitle = title;
        mContent = content;
        mIcon = littleIcon;
        mBigIcon = bigIcon;
        mColor = rgbColor;

        return mBuilder.build();
    }

    public void updateNotification()
    {
        mNM.notify(ONGOING_NOTIFICATION_ID, createNotification());
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    private void restartApp()
    {
        Intent appIntent = new Intent(Intent.ACTION_VIEW);
        appIntent.setClassName(getPackageName(), "com.flytesoft.missioncontrol.MainActivity");
        appIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
        Intent.FLAG_ACTIVITY_SINGLE_TOP|
        Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(appIntent);

        Log.i(TAG, "Attempting app restart.");

        /**
        Intent appIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


        PendingIntent pendingAppIntent = PendingIntent.getActivity(getApplicationContext(), 0, appIntent, PendingIntent.FLAG_ONE_SHOT );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, pendingAppIntent);
        System.exit(2);
         ***/
        // getApplicationContext().startActivity(appIntent);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

        if(intent == null)
        {
            Log.i(TAG, "Service restarted.");
            // TODO: Handle restart here.
        }
        else
        {
            Log.i(TAG, "Service started: " + intent.toString());
        }
        return Service.START_NOT_STICKY; // TODO: Change back to sticky if able to find restart solution?
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mIsBound = false;
        killWakeLock();
        clearButtonIntents();
        stopForeground(true);
        if(mNM != null)
        {
            mNM.cancel(ONGOING_NOTIFICATION_ID);
        }
        Log.i(TAG, "Service destroyed.");

    }
}
