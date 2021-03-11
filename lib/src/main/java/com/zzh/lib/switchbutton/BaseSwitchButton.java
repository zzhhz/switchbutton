package com.zzh.lib.switchbutton;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.zzh.lib.switchbutton.gesture.HTouchHelper;

import androidx.core.view.ViewCompat;


public abstract class BaseSwitchButton extends ViewGroup implements SwitchButton {
    private View mViewNormal;
    private View mViewChecked;
    private View mViewThumb;
    protected final HAttrModel mAttrModel = new HAttrModel();

    private boolean mIsChecked;
    private ScrollState mScrollState = ScrollState.Idle;

    private OnCheckedChangeCallback mOnCheckedChangeCallback;
    private OnViewPositionChangeCallback mOnViewPositionChangeCallback;
    private OnScrollStateChangeCallback mOnScrollStateChangeCallback;

    protected boolean mIsDebug;

    public BaseSwitchButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAttrModel.parse(context, attrs);
        mIsChecked = mAttrModel.isChecked();
        mIsDebug = mAttrModel.isDebug();

        final View normal = new View(getContext());
        normal.setBackgroundResource(mAttrModel.getImageNormalResId());
        addView(normal, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mViewNormal = normal;

        final View checked = new View(getContext());
        checked.setBackgroundResource(mAttrModel.getImageCheckedResId());
        addView(checked, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mViewChecked = checked;

        final View thumb = new HThumbView(getContext());
        thumb.setBackgroundResource(mAttrModel.getImageThumbResId());
        addView(thumb, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mViewThumb = thumb;
    }

    public void setDebug(boolean debug) {
        mIsDebug = debug;
    }

    protected final String getDebugTag() {
        return getClass().getSimpleName();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final View normal = findViewById(R.id.lib_sb_view_normal);
        if (normal != null) {
            removeView(normal);
            setViewNormal(normal);
        }

        final View checked = findViewById(R.id.lib_sb_view_checked);
        if (checked != null) {
            removeView(checked);
            setViewChecked(checked);
        }

        final View thumb = findViewById(R.id.lib_sb_view_thumb);
        if (thumb != null) {
            removeView(thumb);
            setViewThumb(thumb);
        }
    }

    private void setViewNormal(View viewNormal) {
        if (replaceOldView(mViewNormal, viewNormal))
            mViewNormal = viewNormal;
    }

    private void setViewChecked(View viewChecked) {
        if (replaceOldView(mViewChecked, viewChecked))
            mViewChecked = viewChecked;
    }

    private void setViewThumb(View viewThumb) {
        if (replaceOldView(mViewThumb, viewThumb))
            mViewThumb = viewThumb;
    }

    private boolean replaceOldView(View viewOld, View viewNew) {
        if (viewNew == null || viewNew == viewOld)
            return false;

        final int index = indexOfChild(viewOld);
        final ViewGroup.LayoutParams params = viewOld.getLayoutParams();
        removeView(viewOld);

        final ViewGroup.LayoutParams paramsNew = viewNew.getLayoutParams();
        if (paramsNew != null) {
            params.width = paramsNew.width;
            params.height = paramsNew.height;
        }

        addView(viewNew, index, params);
        return true;
    }

    /**
     * 返回normal状态下手柄view的left值
     *
     * @return
     */
    protected final int getLeftNormal() {
        return mAttrModel.getMarginLeft();
    }

    /**
     * 返回checked状态下手柄view的left值
     *
     * @return
     */
    protected final int getLeftChecked() {
        return getMeasuredWidth() - mViewThumb.getMeasuredWidth() - mAttrModel.getMarginRight();
    }

    /**
     * 返回手柄view可以移动的宽度大小
     *
     * @return
     */
    protected final int getAvailableWidth() {
        return getLeftChecked() - getLeftNormal();
    }

    /**
     * 返回手柄view滚动的距离
     *
     * @return
     */
    private int getScrollDistance() {
        return mViewThumb.getLeft() - getLeftNormal();
    }

    /**
     * view是否处于空闲状态（静止且未被拖动状态）
     *
     * @return
     */
    protected abstract boolean isViewIdle();

    /**
     * 停止滑动动画
     */
    protected abstract void abortAnimation();

    /**
     * 执行滑动逻辑
     *
     * @param startLeft
     * @param endLeft
     * @return
     */
    protected abstract boolean smoothScroll(int startLeft, int endLeft);

    /**
     * 根据状态刷新View
     *
     * @param checked
     * @param anim
     */
    private void updateViewByState(boolean checked, boolean anim) {
        final int startLeft = mViewThumb.getLeft();
        final int endLeft = checked ? getLeftChecked() : getLeftNormal();

        if (mIsDebug)
            Log.i(getDebugTag(), "updateViewByState " + checked + ":" + startLeft + " -> " + endLeft + " anim:" + anim);

        abortAnimation();
        if (startLeft != endLeft) {
            if (anim) {
                smoothScroll(startLeft, endLeft);
            } else {
                layoutInternal();
            }
        }
    }

    /**
     * 移动手柄view
     *
     * @param delta 移动量
     */
    protected final void moveView(int delta) {
        if (delta == 0)
            return;

        final int current = mViewThumb.getLeft();
        final int min = getLeftNormal();
        final int max = getLeftChecked();
        delta = HTouchHelper.getLegalDelta(current, min, max, delta);

        if (delta == 0)
            return;

        ViewCompat.offsetLeftAndRight(mViewThumb, delta);
        notifyViewPositionChanged();
    }

    private void notifyViewPositionChanged() {
        final float percent = getScrollPercent();
        mViewChecked.setAlpha(percent);
        mViewNormal.setAlpha(1.0f - percent);

        if (mOnViewPositionChangeCallback != null)
            mOnViewPositionChangeCallback.onViewPositionChanged(this);
    }

    /**
     * 设置滚动状态
     *
     * @param state
     */
    protected final void setScrollState(ScrollState state) {
        if (state == null)
            throw new NullPointerException();

        final ScrollState old = mScrollState;
        if (old != state) {
            mScrollState = state;

            if (mIsDebug)
                Log.i(getDebugTag(), "setScrollState:" + old + " -> " + state);

            if (state == ScrollState.Idle)
                layoutInternal();

            if (mOnScrollStateChangeCallback != null)
                mOnScrollStateChangeCallback.onScrollStateChanged(old, state, this);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChild(mViewNormal, widthMeasureSpec, heightMeasureSpec);
        measureChild(mViewChecked, widthMeasureSpec, heightMeasureSpec);

        final ViewGroup.LayoutParams lpThumb = mViewThumb.getLayoutParams();
        measureChild(mViewThumb, getChildMeasureSpec(widthMeasureSpec, mAttrModel.getMarginLeft() + mAttrModel.getMarginRight(), lpThumb.width),
                getChildMeasureSpec(heightMeasureSpec, mAttrModel.getMarginTop() + mAttrModel.getMarginBottom(), lpThumb.height));

        int width = Math.max(mViewThumb.getMeasuredWidth(), Math.max(mViewNormal.getMeasuredWidth(), mViewChecked.getMeasuredWidth()));
        int height = Math.max(mViewThumb.getMeasuredHeight(), Math.max(mViewNormal.getMeasuredHeight(), mViewChecked.getMeasuredHeight()));

        width = getMeasureSize(width, widthMeasureSpec);
        height = getMeasureSize(height, heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    private static int getMeasureSize(int size, int measureSpec) {
        int result = 0;

        final int modeSpec = View.MeasureSpec.getMode(measureSpec);
        final int sizeSpec = View.MeasureSpec.getSize(measureSpec);

        switch (modeSpec) {
            case View.MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case View.MeasureSpec.EXACTLY:
                result = sizeSpec;
                break;
            case View.MeasureSpec.AT_MOST:
                result = Math.min(size, sizeSpec);
                break;
        }
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mIsDebug)
            Log.i(getDebugTag(), "onLayout");

        layoutInternal();
        notifyViewPositionChanged();
    }

    private void layoutInternal() {
        final boolean isViewIdle = isViewIdle();

        if (mIsDebug)
            Log.i(getDebugTag(), "layoutInternal isViewIdle:" + isViewIdle);

        mViewNormal.layout(0, 0, mViewNormal.getMeasuredWidth(), mViewNormal.getMeasuredHeight());
        mViewChecked.layout(0, 0, mViewChecked.getMeasuredWidth(), mViewChecked.getMeasuredHeight());

        int left = 0;
        int top = mAttrModel.getMarginTop();
        if (isViewIdle) {
            left = mIsChecked ? getLeftChecked() : getLeftNormal();
        } else {
            left = mViewThumb.getLeft();
        }
        mViewThumb.layout(left, top,
                left + mViewThumb.getMeasuredWidth(), top + mViewThumb.getMeasuredHeight());

        final float backZ = Math.max(ViewCompat.getZ(mViewNormal), ViewCompat.getZ(mViewChecked));
        if (ViewCompat.getZ(mViewThumb) <= backZ)
            ViewCompat.setZ(mViewThumb, backZ + 1);

        dealViewIdle();
    }

    private void dealViewIdle() {
        if (isViewIdle()) {
            if (mIsDebug)
                Log.i(getDebugTag(), "dealViewIdle isChecked:" + mIsChecked);

            if (mIsChecked) {
                showCheckedView(true);
                showNormalView(false);
                updateBtnState(true);
            } else {
                updateBtnState(false);
                showCheckedView(false);
                showNormalView(true);
            }
        }
    }

    private void showCheckedView(boolean show) {
        float alpha = show ? 1.0f : 0f;
        if (mViewChecked.getAlpha() != alpha)
            mViewChecked.setAlpha(alpha);
    }

    private void showNormalView(boolean show) {
        float alpha = show ? 1.0f : 0f;
        if (mViewNormal.getAlpha() != alpha)
            mViewNormal.setAlpha(alpha);
    }

    //----------SwitchButton implements start----------

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public boolean setChecked(boolean checked, boolean anim, boolean notifyCallback) {
        if (mIsDebug)
            Log.i(getDebugTag(), "setChecked:" + mIsChecked + " -> " + checked);

        final boolean changed = mIsChecked != checked;
        if (changed) {
            mIsChecked = checked;
            mViewThumb.setSelected(checked);
            updateBtnState(checked);
        }

        updateViewByState(mIsChecked, anim);

        if (changed) {
            if (notifyCallback) {
                if (mOnCheckedChangeCallback != null)
                    mOnCheckedChangeCallback.onCheckedChanged(mIsChecked, this);
            }
        }

        return changed;
    }

    private void updateBtnState(boolean checked) {
        Drawable background = null;
        if (mViewThumb instanceof ImageView) {
            background = ((ImageView) mViewThumb).getDrawable();
        } else {
            background = mViewThumb.getBackground();
        }
        if (background != null && background instanceof StateListDrawable) {
            ((StateListDrawable) background).selectDrawable(checked ? 0 : 1);
            if (mIsDebug)
                Log.i(getDebugTag(), "setChecked: 状态图片改变");
        }
    }

    @Override
    public void toggleChecked(boolean anim, boolean notifyCallback) {
        setChecked(!mIsChecked, anim, notifyCallback);
    }

    @Override
    public void setOnCheckedChangeCallback(OnCheckedChangeCallback callback) {
        mOnCheckedChangeCallback = callback;
    }

    @Override
    public void setOnViewPositionChangeCallback(OnViewPositionChangeCallback callback) {
        mOnViewPositionChangeCallback = callback;
    }

    @Override
    public void setOnScrollStateChangeCallback(OnScrollStateChangeCallback callback) {
        mOnScrollStateChangeCallback = callback;
    }

    @Override
    public float getScrollPercent() {
        return getScrollDistance() / (float) getAvailableWidth();
    }

    @Override
    public ScrollState getScrollState() {
        return mScrollState;
    }

    @Override
    public View getViewNormal() {
        return mViewNormal;
    }

    @Override
    public View getViewChecked() {
        return mViewChecked;
    }

    @Override
    public View getViewThumb() {
        return mViewThumb;
    }
}
