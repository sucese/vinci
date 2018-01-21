package com.guoxiaoxing.vinci.util;

import android.app.Activity;
import android.view.ViewGroup;

import com.facebook.react.ReactRootView;
import com.guoxiaoxing.vinci.Vinci;

import java.util.HashMap;
import java.util.Map;

/**
 * Pre ReactRootView loader
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com.
 *
 * @author guoxiaoxing
 * @since 2017/9/8 下午6:17
 */
public class PreReactRootViewLoader {

    private static final Map<String, ReactRootView> CACHE = new HashMap<>();

    public static void preLoad(Activity activity, String componentName) {
        if (CACHE.get(componentName) == null) {
            return;
        }
        ReactRootView reactRootView = new ReactRootView(activity);
        reactRootView.startReactApplication(Vinci.getReactNativeHost().getReactInstanceManager(),
                componentName,
                null);
        CACHE.put(componentName, reactRootView);
    }

    public static ReactRootView getReactRootView(String componentName){
        return CACHE.get(componentName);
    }

    public static void removeView(String componentName){
        ReactRootView reactRootView = CACHE.get(componentName);
        ViewGroup parentView = (ViewGroup) reactRootView.getParent();
        if(parentView != null){
            parentView.removeView(reactRootView);
        }
    }
}
