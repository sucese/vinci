package com.guoxiaoxing.vinci.ui;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.guoxiaoxing.vinci.Vinci;

import javax.annotation.Nullable;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com.
 *
 * @author guoxiaoxing
 * @since 2017/9/7 下午7:00
 */
public class VinciActivity extends ReactActivity {

    @Override
    protected ReactActivityDelegate createReactActivityDelegate() {
        return new VinciActivituDelegate(this, Vinci.getVinciConfig().getMainComponentName());
    }

    @Nullable
    @Override
    protected String getMainComponentName() {
        return Vinci.getVinciConfig().getMainComponentName();
    }
}
