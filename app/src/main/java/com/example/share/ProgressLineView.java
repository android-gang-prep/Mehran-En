package com.example.share;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ProgressView extends View {
    public ProgressView(Context context) {
        super(context);
        init();
    }

    public ProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();

    }

    public ProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    Paint progress;
    Paint cir;
    Path path;


    private void init() {
        progress = new Paint();
        progress.setColor(getResources().getColor(R.color.my_light_primary, null));
        progress.setStyle(Paint.Style.STROKE);
        progress.setStrokeWidth(dpToPx(7));
        cir = new Paint();
        cir.setColor(Color.parseColor("#4D2196F3"));
        cir.setStyle(Paint.Style.STROKE);
        cir.setStrokeWidth(dpToPx(7));

        path = new Path();
    }


    public interface OnFinishRecordTime {
        void onFinishTime();
    }

    float progressN = 0f;

    ValueAnimator animator = null;

    public void startProgress(OnFinishRecordTime onFinishRecordTime) {
        if (animator != null)
            animator.cancel();
        progressN = 0f;
        animator = ObjectAnimator.ofFloat(0f, 1f);
        animator.setDuration(60000);
        animator.addUpdateListener(animation -> {
            progressN = (float) animation.getAnimatedValue();
            invalidate();
        });
        if (onFinishRecordTime != null) {
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(@NonNull Animator animation) {

                }

                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
                    onFinishRecordTime.onFinishTime();
                }

                @Override
                public void onAnimationCancel(@NonNull Animator animation) {

                }

                @Override
                public void onAnimationRepeat(@NonNull Animator animation) {

                }
            });
        }
        animator.start();
    }

    public void cancelProgress() {
        if (animator != null)
            animator.cancel();

        animator = null;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float radius = Math.min(getWidth() - dpToPx(10), getHeight() - dpToPx(10)) / 2;
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius, cir);
        canvas.drawArc((getWidth() / 2) - radius, (getHeight() / 2) - radius, (getWidth() / 2) + radius, (getHeight() / 2) + radius, 180, 360 * progressN, false, progress);

    }

    private float dpToPx(float f) {
        return f * getResources().getDisplayMetrics().density;
    }

}
