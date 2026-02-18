package pixelpen.keytag;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class GridDividerDecoration extends RecyclerView.ItemDecoration {

    private final int spanCount;
    private final Paint paint;

    public GridDividerDecoration(int spanCount, int color, int thicknessPx) {
        this.spanCount = spanCount;
        paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(thicknessPx);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {

        int childCount = parent.getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            int position = parent.getChildAdapterPosition(child);
            int column = position % spanCount;

            // Draw right divider except last column
            if (column < spanCount - 1) {
                float x = child.getRight();
                c.drawLine(x, child.getTop(), x, child.getBottom(), paint);
            }

            // Draw bottom divider
            float y = child.getBottom();
            c.drawLine(child.getLeft(), y, child.getRight(), y, paint);
        }
    }
}
