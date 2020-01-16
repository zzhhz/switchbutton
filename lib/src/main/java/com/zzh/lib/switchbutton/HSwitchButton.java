package com.zzh.lib.switchbutton;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import com.zzh.lib.switchbutton.gesture.HGestureManager;
import com.zzh.lib.switchbutton.gesture.HTouchHelper;

public class HSwitchButton extends BaseSwitchButton {
    private HGestureManager mGestureManager;

    public HSwitchButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setDebug(boolean debug) {
        super.setDebug(debug);
        if (mGestureManager != null)
            mGestureManager.setDebug(debug);
    }

    private HGestureManager getGestureManager() {
        if (mGestureManager == null) {
            mGestureManager = new HGestureManager(this, new HGestureManager.Callback() {
                @Override
                public boolean shouldInterceptEvent(MotionEvent event) {
                    boolean shouldInterceptEvent = false;
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (HTouchHelper.isViewUnder(getViewThumb(), (int) event.getX(), (int) event.getY()))
                            shouldInterceptEvent = true;
                    } else {
                        shouldInterceptEvent = canPull();
                    }

                    if (mIsDebug)
                        Log.i(getDebugTag(), "shouldInterceptEvent:" + shouldInterceptEvent);

                    return shouldInterceptEvent;
                }

                @Override
                public boolean shouldConsumeEvent(MotionEvent event) {
                    final boolean shouldConsumeEvent = mGestureManager.getTagHolder().isTagIntercept() || canPull();
                    if (mIsDebug)
                        Log.i(getDebugTag(), "shouldConsumeEvent:" + shouldConsumeEvent);

                    return shouldConsumeEvent;
                }

                @Override
                public void onEventConsume(MotionEvent event) {
                    final int dx = (int) getGestureManager().getTouchHelper().getDeltaX();
                    moveView(dx);
                }

                @Override
                public void onEventFinish(VelocityTracker velocityTracker, MotionEvent event) {
                    if (mGestureManager.getLifecycleInfo().isCancelConsumeEvent())
                        return;

                    if (getGestureManager().getTouchHelper().isClick(event, getContext())) {
                        toggleChecked(mAttrModel.isNeedToggleAnim(), true);
                        return;
                    }

                    if (mGestureManager.getLifecycleInfo().hasConsumeEvent()) {
                        velocityTracker.computeCurrentVelocity(1000);
                        final int velocity = (int) velocityTracker.getXVelocity();
                        final int minFlingVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity() * 8;

                        boolean checked = false;
                        if (Math.abs(velocity) > minFlingVelocity) {
                            checked = velocity > 0;
                        } else {
                            final int leftMiddle = (getLeftNormal() + getLeftChecked()) / 2;
                            checked = getViewThumb().getLeft() >= leftMiddle;
                        }

                        if (mIsDebug)
                            Log.e(getDebugTag(), "onConsumeEventFinish checked:" + checked);

                        setChecked(checked, true, true);
                    }
                }

                @Override
                public void onStateChanged(HGestureManager.State oldState, HGestureManager.State newState) {
                    switch (newState) {
                        case Consume:
                            setScrollState(ScrollState.Drag);
                            break;
                        case Fling:
                            setScrollState(ScrollState.Fling);
                            ViewCompat.postInvalidateOnAnimation(HSwitchButton.this);
                            break;
                        case Idle:
                            setScrollState(ScrollState.Idle);
                            break;
                    }
                }

                @Override
                public void onScrollerCompute(int lastX, int lastY, int currX, int currY) {
                    final int dx = currX - lastX;
                    moveView(dx);
                }
            });
            mGestureManager.setDebug(mIsDebug);
            mGestureManager.getTagHolder().setCallback(new HGestureManager.TagHolder.Callback() {
                @Override
                public void onTagInterceptChanged(boolean tag) {
                    HTouchHelper.requestDisallowInterceptTouchEvent(HSwitchButton.this, tag);
                }

                @Override
                public void onTagConsumeChanged(boolean tag) {
                    HTouchHelper.requestDisallowInterceptTouchEvent(HSwitchButton.this, tag);
                }
            });
        }
        return mGestureManager;
    }

    @Override
    public boolean setChecked(boolean checked, boolean anim, boolean notifyCallback) {
        getGestureManager().cancelConsumeEvent();
        return super.setChecked(checked, anim, notifyCallback);
    }

    @Override
    protected boolean isViewIdle() {
        return getGestureManager().getState() == HGestureManager.State.Idle;
    }

    @Override
    protected void abortAnimation() {
        getGestureManager().getScroller().abortAnimation();
    }

    @Override
    protected boolean smoothScroll(int startLeft, int endLeft) {
        return getGestureManager().getScroller().scrollToX(startLeft, endLeft, -1);
    }

    private boolean canPull() {
        final float deltaX = getGestureManager().getTouchHelper().getDeltaXFromDown();
        if (deltaX == 0)
            return false;

        final boolean checkDegreeX = getGestureManager().getTouchHelper().getDegreeXFromDown() < 30;
        if (!checkDegreeX)
            return false;

        final boolean checkMoveLeft = isChecked() && deltaX < 0;
        final boolean checkMoveRight = !isChecked() && deltaX > 0;

        return checkMoveLeft || checkMoveRight;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        getGestureManager().getScroller().setMaxScrollDistance(getAvailableWidth());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return getGestureManager().onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return getGestureManager().onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (getGestureManager().getScroller().computeScrollOffset())
            ViewCompat.postInvalidateOnAnimation(this);
    }
}
