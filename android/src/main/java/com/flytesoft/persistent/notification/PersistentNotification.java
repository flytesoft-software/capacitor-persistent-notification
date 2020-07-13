package com.flytesoft.persistent.notification;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;

import com.flytesoft.persistent.notification.capacitorpersistentnotification.NotificationButtonReceiver;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import static android.content.Context.BIND_AUTO_CREATE;

@NativePlugin()
public class PersistentNotification extends Plugin
{
    private static final String TAG = "FOREGROUND-NOTIFICATION";
    public static final String NOTIFICATION_ACTION_EVENT = "notificationclick";

    private boolean mBoundCalled = false;
    private boolean mIsBound = false;
    private boolean mIsVisible = false;
    private ForeGroundService mBoundService = null;

    private PluginCall mUpdateCall = null;

    private static String mTitle = "Foreground Notification";
    private static Spanned mSpannedTitle = Html.fromHtml(mTitle, Html.FROM_HTML_MODE_COMPACT);
    private static String mIcon = "";
    private static String mContent = "Running code in the background";
    private static Spanned mSpannedContent = Html.fromHtml(mContent, Html.FROM_HTML_MODE_COMPACT);
    private static JSArray mActions = null;
    private static String mColor = "";
    private static String mBadge = "";

    private boolean mIsStopping = false;

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks = null;

    private Thread.UncaughtExceptionHandler mExceptionHandler = null;
    private ComponentCallbacks componentCallbacks = null;
    private static Intent mLaunchIntent = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mIsBound = true;
            mBoundService = ((ForeGroundService.LocalBinder)service).getService();
            completeAwaitingCalls();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mIsBound = false;
            mBoundService = null;
        }

    };

    public PersistentNotification()
    {
    }

    private void completeAwaitingCalls()
    {
        if(mUpdateCall != null)
        {
            mBoundService.updateNotification();
            mUpdateCall.success();
            mUpdateCall = null;
        }
    }

    @Override
    public void load()
    {
        super.load();
        registerLifeCycles();
        NotificationButtonReceiver.setNotificationRef(this);
        mLaunchIntent = bridge.getActivity().getIntent();
    }

    @PluginMethod()
    public void close(PluginCall call)
    {
        if(stopService())
        {
            call.success();
        }
        else
        {
            call.reject("Background service is started, but unable to stop it.");
        }
    }

    @PluginMethod()
    public void appToForeground(PluginCall call)
    {
        if(!mIsVisible)
        {
            Intent appIntent = getContext()
                    .getPackageManager()
                    .getLaunchIntentForPackage(getContext().getPackageName());
            getContext().startActivity(appIntent);
        }
        call.success();
    }

    @PluginMethod()
    public void update(PluginCall call)
    {
       importNotificationOptions(call);

        if(mIsBound && mBoundService != null)
        {
            mBoundService.updateNotification();
            call.success();
        }
        else
        {
            mUpdateCall = call;
        }
    }

    @PluginMethod
    public void getState(PluginCall call)
    {
        JSObject ret = new JSObject();
        ret.put("isOpen", mIsBound);

        call.success(ret);
    }

    private void keepWebKitAlive()
    {
        bridge.getWebView().dispatchWindowVisibilityChanged(View.VISIBLE);
        Log.d(TAG, "Visibility change implemented.");
    }


    private void restartApp()
    {
        mLaunchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP|
                Intent.FLAG_ACTIVITY_NEW_TASK);

         PendingIntent pendingAppIntent = PendingIntent.getActivity(bridge.getContext(), 0, mLaunchIntent, PendingIntent.FLAG_ONE_SHOT );

         AlarmManager alarmManager = (AlarmManager) bridge.getActivity().getApplication().getSystemService(Context.ALARM_SERVICE);
         alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, pendingAppIntent);
         //System.exit(2);
    }

    private void registerLifeCycles()
    {
        if(mExceptionHandler == null)
        {
            mExceptionHandler = (thread, throwable) -> {
                bridge.getActivity().finish();
                Log.d(TAG, "Crashed.");
                // restartApp(); TODO: Implement if solution is found.
            };

            Thread.setDefaultUncaughtExceptionHandler(mExceptionHandler);
        }

        if(componentCallbacks == null)
        {
            componentCallbacks = new ComponentCallbacks()
            {
                @Override
                public void onConfigurationChanged(Configuration configuration)
                {

                }

                @Override
                public void onLowMemory()
                {
                    Log.i(TAG, "Low memory");
                }
            };

            bridge.getActivity().getApplication().registerComponentCallbacks(componentCallbacks);
        }

        if(activityLifecycleCallbacks == null)
        {
            activityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityPostStopped(Activity activity) {
                    Log.i(TAG, "Post stopped.");
                    if(mIsBound)
                    {
                        keepWebKitAlive();
                    }
                }
                @Override
                public void onActivityCreated(Activity activity, Bundle bundle) {
                    Log.i(TAG, "Created");
                }

                @Override
                public void onActivityPreDestroyed(Activity activity)
                {
                    Log.i(TAG, "Activity PRE-DESTROY");
                }

                @Override
                public void onActivityStarted(Activity activity) {
                    Log.i(TAG, "Started.");
                    mIsVisible = true;
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    mIsVisible = true;
                    Log.i(TAG, "Resumed.");
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    mIsVisible = false;
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    Log.i(TAG, "Stopped.");
                    mIsVisible = false;
                }

                @Override
                public void onActivityPreStopped(Activity activity)
                {
                    Log.i(TAG, "Pre-stopped.");
                    mIsStopping = true;
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                    mIsVisible = false;
                    Log.i(TAG, "Destroyed");

                    if(componentCallbacks != null)
                    {
                        bridge.getActivity().getApplication().unregisterComponentCallbacks(componentCallbacks);
                        componentCallbacks = null;
                    }

                    if(activityLifecycleCallbacks != null)
                    {
                        bridge.getActivity().getApplication().unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
                        activityLifecycleCallbacks = null;
                    }
                    stopService();
                }
            };

            bridge.getActivity().getApplication().registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
        }

    }

    private void importNotificationOptions(PluginCall call)
    {
        String oldTitle = mTitle;
        String oldContent = mContent;

        mIcon = call.getString("icon", mIcon);
        mTitle = call.getString("title", mTitle);
        mContent = call.getString("body", mContent);
        mActions = call.getArray("actions", mActions);
        mColor = call.getString("color", mColor);
        mBadge = call.getString("badge", mBadge);

        if(mTitle != oldTitle)
        {
            mSpannedTitle = Html.fromHtml(mTitle, Html.FROM_HTML_MODE_COMPACT);
        }

        if(mContent != oldContent)
        {
            mSpannedContent = Html.fromHtml(mContent, Html.FROM_HTML_MODE_COMPACT);
        }
    }

    public void notifyActionClick(String action)
    {
        JSObject ret = new JSObject();

        ret.put("action", action);

        notifyListeners(NOTIFICATION_ACTION_EVENT, ret);
    }

    public void notifyActionClick()
    {
        JSObject ret = new JSObject();

        ret.put("action", "");

        notifyListeners(NOTIFICATION_ACTION_EVENT, ret);
    }

    @PluginMethod()
    public void open(PluginCall call)
    {
        Log.d("FOREGROUND", "Open called.");

        importNotificationOptions(call);


        if(startService())
        {
            if(mIsStopping)
            {
                keepWebKitAlive();
                mIsStopping = false;
            }
            call.success();
        }
        else
        {
            call.reject("Unable to start foreground service.");
        }
    }

    public static Spanned getTitle()
    {
        return mSpannedTitle;
    }

    public static String getIcon() { return mIcon; }

    public static Spanned getContent() { return mSpannedContent; }

    public static JSArray getActions() { return mActions; }

    public static String getColor () { return  mColor; }

    public static String getBadge() { return mBadge; }

    private boolean startService()
    {
        if (!mIsBound && !mBoundCalled)
        {
            final Context context = getContext();
            Intent intent = new Intent(context, ForeGroundService.class);

            try
            {
                ForeGroundService.boundByApp();
                Log.d(TAG, "Class: " + bridge.getActivity().getClass().getCanonicalName());
                mBoundCalled = context.bindService(intent, mConnection, BIND_AUTO_CREATE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                {
                    context.startForegroundService(intent);
                }
                else
                {
                    context.startService(intent);
                }
            }
            catch (Exception e)
            {
                mBoundCalled = false;
                mIsBound = false;
                return false;
            }
        }

        return true;
    }

    private boolean stopService()
    {
        if(mIsBound || mBoundCalled)
        {
            final Context context = getContext();
            Intent intent = new Intent(context, ForeGroundService.class);

            context.unbindService(mConnection);
            mBoundCalled = false;
            mIsBound = false;

            if(!context.stopService(intent))
            {
                return false;
            }
        }

        return true;
    }
}
