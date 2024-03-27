package com.example.share;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ProgressLineView extends View {
    public ProgressLineView(Context context) {
        super(context);
        init();
    }

    public ProgressLineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();

    }

    public ProgressLineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ProgressLineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    Paint progress;
    Paint cir;


    private void init() {
        progress = new Paint();
        progress.setColor(getResources().getColor(R.color.my_light_primary, null));
        progress.setStyle(Paint.Style.FILL);
        progress.setStrokeWidth(dpToPx(7));
        cir = new Paint();
        cir.setColor(Color.parseColor("#4D2196F3"));
        cir.setStyle(Paint.Style.FILL);
        cir.setStrokeWidth(dpToPx(7));

    }


    public interface OnFinishRecordTime {
        void onFinishTime();
    }

    float progressN = 0f;

    ValueAnimator animator = null;

    public void startProgress(float progress) {

        progressN = progress;
        invalidate();

    }

    public void cancelProgress() {
        if (animator != null)
            animator.cancel();

        animator = null;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRoundRect(dpToPx(5), dpToPx(5),getWidth()-dpToPx(5),getHeight()-dpToPx(5),15,15, cir);
        canvas.drawRoundRect(dpToPx(5), dpToPx(5),(getWidth()-dpToPx(5))* progressN,getHeight()-dpToPx(5),15,15, progress);

    }

    private float dpToPx(float f) {
        return f * getResources().getDisplayMetrics().density;
    }

}
