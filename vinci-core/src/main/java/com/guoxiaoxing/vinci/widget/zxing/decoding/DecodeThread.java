package com.guoxiaoxing.vinci.widget.zxing.decoding;

import android.os.Handler;
import android.os.Looper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.ResultPointCallback;
import com.guoxiaoxing.vinci.widget.zxing.activity.CaptureFragment;

import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

final class DecodeThread extends Thread {

    public static final String BARCODE_BITMAP = "barcode_bitmap";
    private final CaptureFragment fragment;
    private final Hashtable<DecodeHintType, Object> hints;
    private Handler handler;
    private final CountDownLatch handlerInitLatch;

    DecodeThread(CaptureFragment fragment,
                 Vector<BarcodeFormat> decodeFormats,
                 String characterSet,
                 ResultPointCallback resultPointCallback) {

        this.fragment = fragment;
        handlerInitLatch = new CountDownLatch(1);

        hints = new Hashtable<DecodeHintType, Object>(3);

        if (decodeFormats == null || decodeFormats.isEmpty()) {
            decodeFormats = new Vector<BarcodeFormat>();
            decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
        }

        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

        if (characterSet != null) {
            hints.put(DecodeHintType.CHARACTER_SET, characterSet);
        }

        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);
    }

    Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecodeHandler(fragment, hints);
        handlerInitLatch.countDown();
        Looper.loop();
    }

}
