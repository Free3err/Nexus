package ru.mogcommunity.rbr_project.ui.helper;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int spaceDp;
    private final boolean isHorizontal;

    public SpaceItemDecoration(int spaceDp, boolean isHorizontal) {
        this.spaceDp = spaceDp;
        this.isHorizontal = isHorizontal;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int spacePx = (int) (spaceDp * view.getContext().getResources().getDisplayMetrics().density);

        if (isHorizontal) {
            outRect.right = spacePx;
        } else {
            outRect.bottom = spacePx;
        }
    }
}

