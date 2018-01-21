package com.guoxiaoxing.vinci.widget.menu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;

import com.guoxiaoxing.vinci.R;

public class CircleButton extends AppCompatImageButton {

    private int mColorNormal;
    private int mColorPressed;
    private int mColorDisabled;

    private int mButtonSize;

    public CircleButton(Context context) {
        this(context, null);
    }

    public CircleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    void init(Context context, AttributeSet attributeSet) {
        mColorNormal = getColor(R.color.vinci_circle_menu_center_button_color_normal);
        mColorPressed = getColor(R.color.vinci_circle_menu_center_button_color_pressed);
        mColorDisabled = getColor(R.color.vinci_circle_menu_button_color_disabled);
        mButtonSize = (int) getDimension(R.dimen.circle_menu_button_size);
        updateBackground();
    }


    /**
     * @return the current Color for normal state.
     */
    public int getColorNormal() {
        return mColorNormal;
    }

    public void setColorNormalResId(@ColorRes int colorNormal) {
        setColorNormal(getColor(colorNormal));
    }

    public void setColorNormal(int color) {
        if (mColorNormal != color) {
            mColorNormal = color;
            updateBackground();
        }
    }

    /**
     * @return the current color for pressed state.
     */
    public int getColorPressed() {
        return mColorPressed;
    }

    public void setColorPressedResId(@ColorRes int colorPressed) {
        setColorPressed(getColor(colorPressed));
    }

    public void setColorPressed(int color) {
        if (mColorPressed != color) {
            mColorPressed = color;
            updateBackground();
        }
    }

    /**
     * @return the current color for disabled state.
     */
    public int getColorDisabled() {
        return mColorDisabled;
    }

    public void setColorDisabledResId(@ColorRes int colorDisabled) {
        setColorDisabled(getColor(colorDisabled));
    }

    public void setColorDisabled(int color) {
        if (mColorDisabled != color) {
            mColorDisabled = color;
            updateBackground();
        }
    }

    int getColor(@ColorRes int id) {
        return getResources().getColor(id);
    }

    float getDimension(@DimenRes int id) {
        return getResources().getDimension(id);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mButtonSize, mButtonSize);
    }

    void updateBackground() {
        setColorNormalResId(R.color.vinci_circle_menu_center_button_color_normal);
        setColorPressedResId(R.color.vinci_circle_menu_center_button_color_pressed);
        setBackgroundCompat(createBackgroundDrawable());
        setImageDrawable(getIconDrawable());
    }

    Drawable getIconDrawable() {
//        if (isVectorAnimationSupported()) {
//            return ContextCompat.getDrawable(getContext(), R.drawable.vinci_menu_vector);
//        } else {
//            return VectorDrawableCompat.create(getResources(), R.drawable.vinci_menu_vector, getContext().getTheme());
//        }
        return ContextCompat.getDrawable(getContext(), R.drawable.vinci_menu_vector);
    }

    private boolean isVectorAnimationSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    private StateListDrawable createBackgroundDrawable() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{-android.R.attr.state_enabled}, createCircleDrawable(mColorDisabled));
        drawable.addState(new int[]{android.R.attr.state_pressed}, createCircleDrawable(mColorPressed));
        drawable.addState(new int[]{}, createCircleDrawable(mColorNormal));
        return drawable;
    }

    private Drawable createCircleDrawable(int color) {
        ShapeDrawable fillDrawable = new ShapeDrawable(new OvalShape());
        final Paint paint = fillDrawable.getPaint();
        paint.setAntiAlias(true);
        paint.setColor(color);

        return fillDrawable;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void setBackgroundCompat(Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(drawable);
        } else {
            setBackgroundDrawable(drawable);
        }
    }
}
