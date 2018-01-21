package com.guoxiaoxing.vinci;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.facebook.react.ReactNativeHost;
import com.guoxiaoxing.vinci.common.VinciConfig;
import com.guoxiaoxing.vinci.ui.VinciActivity;
import com.guoxiaoxing.vinci.widget.menu.FloatBuilder;
import com.guoxiaoxing.vinci.widget.menu.FloatWindow;
import com.guoxiaoxing.vinci.widget.zxing.activity.ZXingLibrary;


/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com.
 *
 * @author guoxiaoxing
 * @since 2017/9/7 下午2:07
 */
public final class Vinci {

    private static ReactNativeHost sReactNativeHost;
    private static Application sApplication;

    private static volatile boolean sIsInit = false;

    private Vinci() {
    }

    public static void init(@NonNull Application application, ReactNativeHost reactNativeHost) {
        if (sIsInit) {
            return;
        } else {
            sIsInit = true;
        }
        sApplication = application;
        sReactNativeHost = sReactNativeHost;
        if (sReactNativeHost.getUseDeveloperSupport()) {
            ZXingLibrary.initDisplayOpinion(sApplication);
            sApplication.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    FloatWindow floatWindow = FloatBuilder.getInstance().getFloatWindow(activity);
                    if (floatWindow != null && !floatWindow.isShowing()) {
                        floatWindow.show();
                    }
                }

                @Override
                public void onActivityStarted(Activity activity) {

                }

                @Override
                public void onActivityResumed(Activity activity) {

                }

                @Override
                public void onActivityPaused(Activity activity) {

                }

                @Override
                public void onActivityStopped(Activity activity) {

                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

                }

                @Override
                public void onActivityDestroyed(Activity activity) {

                }
            });
        }
    }

    public static Application getApplication(){
        return sApplication;
    }

    public static ReactNativeHost getReactNativeHost(){
        return sReactNativeHost;
    }

    public static void loadLocalBundle(Context context) {
        Intent intent = new Intent(context, VinciActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public static void loadServerBundle(Context context, String bundleName) {
        Intent intent = new Intent(context, VinciActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }
}
