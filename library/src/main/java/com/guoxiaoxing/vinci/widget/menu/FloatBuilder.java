package com.guoxiaoxing.vinci.widget.menu;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.guoxiaoxing.vinci.R;
import com.guoxiaoxing.vinci.Vinci;
import com.guoxiaoxing.vinci.service.LogFloatService;
import com.guoxiaoxing.vinci.widget.zxing.activity.JumpActivity;

public class FloatBuilder implements View.OnClickListener {

    private static FloatBuilder sInstance;
    private FloatWindow floatWindow;

    private FloatBuilder() {

    }

    public static FloatBuilder getInstance() {
        if (sInstance == null) {
            synchronized (FloatBuilder.class) {
                sInstance = new FloatBuilder();
            }
        }
        return sInstance;
    }


    public FloatWindow getFloatWindow(Context context) {
        if (context == null) {
            return null;
        }
        if (floatWindow != null) {
            return floatWindow;
        }

        floatWindow = new FloatWindow(context);
        floatWindow.setFloatView(new CircleButton(context));

        View menuView = LayoutInflater.from(context).inflate(R.layout.vinci_float_menu, null);

        if (menuView != null) {
            Button btnScan = (Button) menuView.findViewById(R.id.vinci_scan);
            Button btnLocal = (Button) menuView.findViewById(R.id.vinci_load_local);
            Button btnLog = (Button) menuView.findViewById(R.id.vinci_log);
            if (btnScan != null) {
                btnScan.setOnClickListener(this);
            }
            if (btnLocal != null) {
                btnLocal.setOnClickListener(this);
            }
            if (btnLog != null) {
                btnLog.setOnClickListener(this);
            }
            floatWindow.setMenuView(menuView);
        }
        return floatWindow;
    }

    @Override
    public void onClick(View view) {
        final Context context = view.getContext();
        floatWindow.turnMini();
        if (view.getId() == R.id.vinci_scan) {
            Intent intent = new Intent(context, JumpActivity.class);
            intent.putExtra(JumpActivity.REQUEST_TYPE, JumpActivity.REQUEST_QR_CODE);
            context.startActivity(intent);
        } else if (view.getId() == R.id.vinci_load_local) {
            Vinci.loadLocalBundle(context);
        } else if (view.getId() == R.id.vinci_log) {
            context.startService(new Intent(context, LogFloatService.class));
        }
    }
}
