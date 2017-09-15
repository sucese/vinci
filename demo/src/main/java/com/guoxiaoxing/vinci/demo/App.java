package com.guoxiaoxing.vinci.demo;

import android.app.Application;

import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactPackage;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.soloader.SoLoader;
import com.guoxiaoxing.vinci.Vinci;
import com.guoxiaoxing.vinci.common.VinciConfig;
import com.guoxiaoxing.vinci.module.VinciPackage;

import java.util.Arrays;
import java.util.List;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com.
 *
 * @author guoxiaoxing
 * @since 2017/9/7 下午3:43
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Vinci.init(this, new ReactNativeHost() {
            @Override
            public boolean getUseDeveloperSupport() {
                return true;
            }

            @Override
            protected List<ReactPackage> getPackages() {
                return Arrays.asList(new MainReactPackage(), new VinciPackage();
            }
        });
    }
}
