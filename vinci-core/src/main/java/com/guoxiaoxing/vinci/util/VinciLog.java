package com.guoxiaoxing.vinci.util;

import android.text.TextUtils;
import android.util.Log;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com.
 *
 * @author guoxiaoxing
 * @since 2017/9/8 下午3:33
 */
public final class VinciLog {

    private static final String TAG = "Vinci";

    public static void log(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        Log.d(TAG, message);
    }
}
