package com.flytesoft.persistent.notification;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

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
    public static final String NOTIFICATION_ACTION_EVENT = "notificationclick";

    private boolean mIsBound = false;
    private boolean mIsVisible = false;
    private ForeGroundService mBoundService = null;

    private static String mTitle = "Foreground Notification";
    private static String mIcon = "";
    private static String mContent = "Running code in the background";
    private static JSArray mActions = null;
    private static String mColor = "";
    private static String mBadge = "";

    private boolean mIsStopping = false;

    private ViewTreeObserver.OnGlobalLayoutListener mLayoutListener  = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((ForeGroundService.LocalBinder)service).getService();

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

    @Override
    public void load()
    {
        super.load();
        registerLifeCycles();
        NotificationButtonReceiver.setNotificationRef(this);
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

        if(mIsBound)
        {
            mBoundService.updateNotification();
        }

        call.success();
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
        Log.d("FOREGROUND", "Visibility change implemented.");
    }

    private void registerLifeCycles()
    {
        bridge.getActivity().getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityPostStopped(Activity activity) {
                Log.d("FOREGROUND", "Post stopped.");
                if(mIsBound)
                {
                    keepWebKitAlive();
                }
            }
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
               Log.d("FOREGROUND", "Started.");
               mIsVisible = true;
            }

            @Override
            public void onActivityResumed(Activity activity) {
                mIsVisible = true;
            }

            @Override
            public void onActivityPaused(Activity activity) {
                mIsVisible = false;
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Log.d("FOREGROUND", "Stopped.");
                mIsVisible = false;
            }

            @Override
            public void onActivityPreStopped(Activity activity)
            {
                Log.d("FOREGROUND", "Pre-stopped.");
                mIsStopping = true;
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                mIsVisible = false;
            }
        });
    }

    private void importNotificationOptions(PluginCall call)
    {
        mIcon = call.getString("icon", mIcon);
        mTitle = call.getString("title", mTitle);
        mContent = call.getString("body", mContent);
        mActions = call.getArray("actions", mActions);
        mColor = call.getString("color", mColor);
        mBadge = call.getString("badge", mBadge);
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

    public static String getTitle()
    {
        return mTitle;
    }

    public static String getIcon() { return mIcon; }

    public static String getContent() { return mContent; }

    public static JSArray getActions() { return mActions; }

    public static String getColor () { return  mColor; }

    public static String getBadge() { return mBadge; }

    private boolean startService()
    {
        if (!mIsBound)
        {
            final Context context = getContext();
            Intent intent = new Intent(context, ForeGroundService.class);

            try
            {
                context.bindService(intent, mConnection, BIND_AUTO_CREATE);

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
                mIsBound = false;
                return false;
            }

            mIsBound = true;
        }

        return true;
    }

    private boolean stopService()
    {
        if(mIsBound)
        {
            final Context context = getContext();
            Intent intent = new Intent(context, ForeGroundService.class);

            context.unbindService(mConnection);

            if(context.stopService(intent))
            {
                mIsBound = false;
            }
            else
            {
                return false;
            }
        }

        return true;
    }
}
