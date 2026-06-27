package ru.mogcommunity.rbrproject.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

public class GitBranchLogoView extends View {
    private float progress = 0.0f;
    private ValueAnimator animator;

    public GitBranchLogoView(Context context) {
        super(context);
    }

    public GitBranchLogoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GitBranchLogoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void startAnimation() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(1200); 
        animator.setInterpolator(new DecelerateInterpolator(1.2f));
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;

        float size = Math.min(w, h) * 1.15f;
        float xOffset = (w - size) / 2.0f;
        float yOffset = (h - size) / 2.0f;

        Drawable logoDrawable = getContext().getDrawable(ru.mogcommunity.rbrproject.R.drawable.ic_launcher_foreground);
        if (logoDrawable != null) {
            logoDrawable.setBounds((int) xOffset, (int) yOffset, (int) (xOffset + size), (int) (yOffset + size));
            logoDrawable.setAlpha((int) (progress * 255));
            logoDrawable.draw(canvas);
        }
    }
}
