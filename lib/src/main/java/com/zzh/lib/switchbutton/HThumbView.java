package com.zzh.lib.switchbutton;

import android.content.Context;
import android.view.View;

class HThumbView extends View {
    public HThumbView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, heightMeasureSpec);
    }
}
