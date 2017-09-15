package com.guoxiaoxing.vinci.widget.menu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class FloatWindow {
    private static final int WHAT_HIDE = 0x275;
    private final float DISTANCE = 15.0f;  //  点击偏移量   在上、下、左、右这个范围之内都会触发点击事件
    private WindowManager.LayoutParams mLayoutParams;
    private DisplayMetrics mDisplayMetrics;
    private WindowManager mWindowManager;
    private Context mContext;
    private View mContentView;
    private float offsetX, offsetY;

    private long lastTouchTimeMillis;
    private long downTimeMillis;  //  按下事件  暂未使用，可以拓展长按事件

    private boolean mIsShowing;
    private float downX, downY;
    private float oldX, oldY;
    private boolean isOpen;

    private View mFloatView, mPlayerView;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case WHAT_HIDE:
                    if (System.currentTimeMillis() - lastTouchTimeMillis >= 3500) {
                        if (!isOpen) {
                            getLayoutParams().alpha = 0.4f;
                            getWindowManager().updateViewLayout(getContentView(), getLayoutParams());
                        }
                    } else {
                        if (isOpen) {
                            lastTouchTimeMillis = System.currentTimeMillis() + 3500;
                        }
                        handler.sendEmptyMessageDelayed(WHAT_HIDE, 200);
                    }
                    break;
            }
        }
    };

    /**
     * 不带布局参数的构造方法
     */
    public FloatWindow(Context context) {
        this(context, null, null);
    }

    /**
     * 带布局参数的构造方法
     */
    public FloatWindow(Context context, View PlayerView, View floatView) {
        this.mContext = context;
        setFloatView(floatView);
        setMenuView(PlayerView);
        initWindowManager();
        initLayoutParams();
    }

    /**
     * 设置开启状态的布局视图
     */
    public void setMenuView(View menuView) {
        if (menuView != null) {
            BackgroundView backgroundView = new BackgroundView(getContext());
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            menuView.setOnTouchListener(new TouchIntercept());
            menuView.setLayoutParams(layoutParams);
            backgroundView.addView(menuView);
            this.mPlayerView = backgroundView;
        }
    }

    /**
     * 设置关闭状态的布局视图
     */
    public void setFloatView(View floatView) {
        if (floatView != null) {
            this.mFloatView = floatView;
            setContentView(mFloatView);
        }
    }

    /**
     * 配置布局View， 需要在此处获得View的宽、高，并由此获得偏移量
     */
    private void createContentView(View contentView) {
        this.mContentView = contentView;
        if (this.mContentView != null) {
            this.mContentView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
            this.mContentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED); // 主动计算视图View的宽高信息
            this.offsetY = getStatusBarHeight(getContext()) + this.mContentView.getMeasuredHeight() / 2;
            this.offsetX = this.mContentView.getMeasuredWidth() / 2;
            this.mContentView.setOnTouchListener(new WindowTouchListener());
        }

    }

    /**
     * 获得上下文信息
     */
    public Context getContext() {
        return this.mContext;
    }

    public WindowManager getWindowManager() {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        }
        return mWindowManager;
    }

    /**
     * 获得WindowManager.LayoutParams参数
     */
    public WindowManager.LayoutParams getLayoutParams() {
        if (mLayoutParams == null) {
            mLayoutParams = new WindowManager.LayoutParams();
            initLayoutParams();
        }
        return mLayoutParams;
    }

    /**
     * 获得显示信息
     */
    public DisplayMetrics getDisplayMetrics() {
        if (mDisplayMetrics == null) {
            mDisplayMetrics = getContext().getResources().getDisplayMetrics();
        }
        return mDisplayMetrics;
    }

    /**
     * 获得当前视图
     */
    public View getContentView() {
        return mContentView;
    }

    /**
     * 设置窗口当前布局
     */
    private void setContentView(View contentView) {
        if (contentView != null) {
            if (isShowing()) {
                getWindowManager().removeView(mContentView);
                createContentView(contentView);
                getWindowManager().addView(mContentView, getLayoutParams());
                updateLocation(getDisplayMetrics().widthPixels / 2, getDisplayMetrics().heightPixels / 2, true);
            } else {
                createContentView(contentView);
            }
        }
    }

    /**
     * 初始化窗口管理器
     */
    private void initWindowManager() {
        mWindowManager = (WindowManager) getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        mDisplayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(mDisplayMetrics);
    }

    /**
     * 初始化WindowManager.LayoutParams参数
     */
    private void initLayoutParams() {
        getLayoutParams().flags = getLayoutParams().flags
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        getLayoutParams().dimAmount = 0.2f;
        getLayoutParams().type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        getLayoutParams().height = WindowManager.LayoutParams.WRAP_CONTENT;
        getLayoutParams().width = WindowManager.LayoutParams.WRAP_CONTENT;
        getLayoutParams().gravity = Gravity.LEFT | Gravity.TOP;
        getLayoutParams().format = PixelFormat.RGBA_8888;
        getLayoutParams().alpha = 1.0f;  //  设置整个窗口的透明度
        offsetX = 0;
        offsetY = getStatusBarHeight(getContext());
        getLayoutParams().x = (int) (mDisplayMetrics.widthPixels - offsetX);
        getLayoutParams().y = (int) (mDisplayMetrics.heightPixels * 1 / 4 - offsetY);
    }

    /**
     * 获取状态栏的高度
     */
    public int getStatusBarHeight(Context context) {
        int height = 0;
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            height = context.getResources().getDimensionPixelSize(resId);
        }
        return height;
    }

    /**
     * 更新窗口的位置
     */
    private void updateLocation(float x, float y, boolean offset) {
        if (getContentView() != null) {
            if (offset) {
                getLayoutParams().x = (int) (x - offsetX);
                getLayoutParams().y = (int) (y - offsetY);
            } else {
                getLayoutParams().x = (int) x;
                getLayoutParams().y = (int) y;
            }
            getWindowManager().updateViewLayout(mContentView, getLayoutParams());
        }
    }

    /**
     * 显示窗口
     */
    public void show() {
        if (getContentView() != null && !isShowing()) {
            try {
                getWindowManager().addView(getContentView(), getLayoutParams());
                mIsShowing = true;
                if (!isOpen) {
                    handler.sendEmptyMessage(WHAT_HIDE);
                }
            } catch (Exception e) { // 没有弹框权限
                Toast.makeText(mContext, "没有开启弹框权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 隐藏当前显示窗口
     */
    public void dismiss() {
        if (getContentView() != null && isShowing()) {
            handler.removeMessages(WHAT_HIDE);
            getWindowManager().removeView(getContentView());
            mIsShowing = false;
        }
    }

    /**
     * 判断当前是否有显示窗口
     */
    public boolean isShowing() {
        return mIsShowing;
    }

    /**
     * 自动对齐的一个小动画（自定义属性动画），使自动贴边的时候显得不那么生硬
     */
    private ValueAnimator alignAnimator(float x, float y) {
        ValueAnimator animator = null;
        if (x <= getDisplayMetrics().widthPixels / 2) {
            animator = ValueAnimator.ofObject(new PointEvaluator(), new Point((int) x, (int) y), new Point(0, (int) y));
        } else {
            animator = ValueAnimator.ofObject(new PointEvaluator(), new Point((int) x, (int) y), new Point(getDisplayMetrics().widthPixels, (int) y));
        }
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Point point = (Point) animation.getAnimatedValue();
                updateLocation(point.x, point.y, true);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                lastTouchTimeMillis = System.currentTimeMillis();
                handler.sendEmptyMessage(WHAT_HIDE);
            }
        });
        animator.setDuration(160);
        return animator;
    }

    /**
     * 打开选项菜单
     */
    public void showPlayer() {
        if (isOpen) {
            return;
        }
        getLayoutParams().flags &= ~(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);// 取消WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE属性
        getLayoutParams().height = WindowManager.LayoutParams.MATCH_PARENT;  //
        getLayoutParams().width = WindowManager.LayoutParams.MATCH_PARENT;  //
        oldX = getLayoutParams().x;
        oldY = getLayoutParams().y;
        setContentView(mPlayerView);
        handler.removeMessages(WHAT_HIDE);
        isOpen = true;
    }

    /**
     * 关闭选项菜单
     */
    public void turnMini() {
        if (!isOpen) {
            return;
        }
        isOpen = false;
        getLayoutParams().flags &= ~(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);// 取消WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE属性
        getLayoutParams().flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;//重新设置WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE属性
        getLayoutParams().height = WindowManager.LayoutParams.WRAP_CONTENT;
        getLayoutParams().width = WindowManager.LayoutParams.WRAP_CONTENT;
        setContentView(mFloatView);
        getLayoutParams().alpha = 1.0f;
        updateLocation(oldX, oldY, false);
        lastTouchTimeMillis = System.currentTimeMillis();
        handler.sendEmptyMessage(WHAT_HIDE);
    }

    /**
     * 按下事件处理
     */
    private void down(MotionEvent event) {
        downX = event.getRawX();
        downY = event.getRawY();
        getLayoutParams().alpha = 1.0f;
        downTimeMillis = System.currentTimeMillis();
        lastTouchTimeMillis = System.currentTimeMillis();
        getWindowManager().updateViewLayout(getContentView(), getLayoutParams());
//        updateLocation(event.getRawX(), event.getRawY(), true);
    }

    /**
     * 移动事件处理
     */
    private void move(MotionEvent event) {
        lastTouchTimeMillis = System.currentTimeMillis();
        updateLocation(event.getRawX(), event.getRawY(), true);
    }

    /**
     * 抬起事件处理
     */
    private void up(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        if (x >= downX - DISTANCE && x <= downX + DISTANCE && y >= downY - DISTANCE && y <= downY + DISTANCE) {
            if (System.currentTimeMillis() - downTimeMillis > 1200) {
                //  长按
            } else {
                showPlayer();  //点击
            }
        } else {
            ValueAnimator animator = alignAnimator(x, y);
            animator.start();
        }
    }

    /**
     *
     * */
    public class PointEvaluator implements TypeEvaluator {

        @Override
        public Object evaluate(float fraction, Object from, Object to) {
            Point startPoint = (Point) from;
            Point endPoint = (Point) to;
            float x = startPoint.x + fraction * (endPoint.x - startPoint.x);
            float y = startPoint.y + fraction * (endPoint.y - startPoint.y);
            Point point = new Point((int) x, (int) y);
            return point;
        }
    }

    /**
     * 带有按键监听事件和触摸事件的BackgroundView
     */
    class BackgroundView extends RelativeLayout {

        public BackgroundView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if (getKeyDispatcherState() == null) {
                    return super.dispatchKeyEvent(event);
                }

                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                    final KeyEvent.DispatcherState state = getKeyDispatcherState();
                    if (state != null) {
                        state.startTracking(event, this);
                    }
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    final KeyEvent.DispatcherState state = getKeyDispatcherState();
                    if (state != null && state.isTracking(event) && !event.isCanceled()) {
                        turnMini();
                        return true;
                    }
                }
                return super.dispatchKeyEvent(event);
            } else {
                return super.dispatchKeyEvent(event);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    turnMini();
                    return true;
                case MotionEvent.ACTION_OUTSIDE:
                    turnMini();
                    return true;
            }
            return super.onTouchEvent(event);
        }
    }

    class TouchIntercept implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    lastTouchTimeMillis = System.currentTimeMillis();
                    break;
            }
            return true;
        }
    }

    class WindowTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!isOpen) {
                        down(event);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!isOpen) {
                        move(event);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (!isOpen) {
                        up(event);
                    }
                    break;
                case MotionEvent.ACTION_OUTSIDE:
                    if (isOpen) {
                        turnMini();
                        return true;
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    }
}
