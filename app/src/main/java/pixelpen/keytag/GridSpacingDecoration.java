package pixelpen.keytag;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class GridSpacingDecoration extends RecyclerView.ItemDecoration {

    private final int spanCount;
    private final int spacing;

    public GridSpacingDecoration(int spanCount, int spacing) {
        this.spanCount = spanCount;
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent,
                               RecyclerView.State state) {

        int half = spacing / 2;

        outRect.left = half;
        outRect.right = half;
        outRect.bottom = spacing;

        int position = parent.getChildAdapterPosition(view);
        if (position < spanCount) {
            outRect.top = spacing;
        }
    }



}
