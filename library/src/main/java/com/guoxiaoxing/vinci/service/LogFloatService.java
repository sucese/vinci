package com.guoxiaoxing.vinci.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import com.guoxiaoxing.vinci.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogFloatService extends Service implements Runnable {
    private int statusBarHeight;// 状态栏高度
    private View view;// 透明窗体
    private TextView mlogTextView = null;
    private ScrollView mScrollView = null;
    private boolean viewAdded = false;// 透明窗体是否已经显示
    private boolean viewHide = false; // 窗口隐藏
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private boolean isObserverLog; //是否监听log
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                refresh((String) msg.obj, true);
            }
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createFloatView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        viewHide = false;
        startLogObserver();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLogObserver();

    }

    public void removeView() {
        if (viewAdded) {
            windowManager.removeView(view);
            viewAdded = false;
        }
    }

    private void createFloatView() {
        view = LayoutInflater.from(this).inflate(R.layout.vinci_float_log, null);
        mScrollView = (ScrollView) view.findViewById(R.id.vinci_log_view);
        mlogTextView = (TextView) view.findViewById(R.id.vinci_log_tv_show);
        windowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);

        layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSPARENT);
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;

        view.setOnTouchListener(new View.OnTouchListener() {
            float[] temp = new float[]{0f, 0f};

            public boolean onTouch(View v, MotionEvent event) {
                layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN: // 按下事件，记录按下时手指在悬浮窗的XY坐标值
                        temp[0] = event.getX();
                        temp[1] = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        refreshView((int) (event.getRawX() - temp[0]),
                                (int) (event.getRawY() - temp[1]));
                        break;
                }
                return true;
            }
        });

        view.findViewById(R.id.vinci_log_btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh(null, false);
            }
        });
        view.findViewById(R.id.vinci_log_btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh(null, false);
                stopSelf();
            }
        });
    }


    private void refreshView(int x, int y) {
        // 状态栏高度不能立即取，不然得到的值是0
        if (statusBarHeight == 0) {
            View rootView = view.getRootView();
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            statusBarHeight = r.top;
        }
        layoutParams.x = x;
        // y轴减去状态栏的高度，因为状态栏不是用户可以绘制的区域，不然拖动的时候会有跳动
        layoutParams.y = y - statusBarHeight;// STATUS_HEIGHT;
        refresh(null, true);
    }


    private void refresh(String value, boolean isAppend) {
        if (!isObserverLog) {
            return;
        }
        if (isAppend) {
            if (value != null) {
                mlogTextView.append(value);
            }
        } else {
            mlogTextView.setText(value);
        }
        mScrollView.post(new Runnable() {
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
        // 如果已经添加了就只更新view
        if (viewAdded) {
            windowManager.updateViewLayout(view, layoutParams);
        } else {
            windowManager.addView(view, layoutParams);
            viewAdded = true;
        }
    }


    public void startLogObserver() {
        if (!isObserverLog) {
            isObserverLog = true;
            Thread mThread = new Thread(this);
            mThread.start();
        }
    }


    public void stopLogObserver() {
        isObserverLog = false;
        if (handler != null) {
            handler.removeMessages(0);
        }
        removeView();
    }


    @Override
    public void run() {
        Process pro = null;
        try {
            //Runtime.getRuntime().exec("logcat -c").waitFor(); // 清理日志
            // 日志输出格式参照https://developer.android.com/studio/command-line/logcat.html?hl=zh-cn
            pro = Runtime.getRuntime().exec("logcat -s vinci -v tag");
            //} catch (InterruptedException e) {
            //    e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (pro == null) {
            return;
        }

        BufferedReader dis = new BufferedReader(new InputStreamReader(pro.getInputStream()));
        String line;
        while (isObserverLog) {
            try {
                while ((line = dis.readLine()) != null) {
                    if (handler != null) {
                        Message msg = handler.obtainMessage();
                        msg.what = 0;
                        msg.obj = line + "\n";
                        handler.sendMessage(msg);
                    }
                    Thread.yield();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
