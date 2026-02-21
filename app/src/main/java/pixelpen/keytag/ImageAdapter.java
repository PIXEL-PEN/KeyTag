package pixelpen.keytag;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.VH> {

    public interface SelectionListener {
        void onSelectionChanged(int selectedCount);
    }

    private List<ImageItem> images;
    private SelectionListener selectionListener;

    public ImageAdapter(List<ImageItem> images, SelectionListener listener) {
        this.images = images;
        this.selectionListener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {

        ImageItem item = images.get(position);

        holder.imageView.post(() -> {
            int width = holder.imageView.getWidth();
            holder.imageView.getLayoutParams().height = width;
            holder.imageView.requestLayout();
        });

        Glide.with(holder.imageView.getContext())
                .load(item.uri)
                .centerCrop()
                .into(holder.imageView);

        // Visual feedback
        holder.itemView.setAlpha(item.isSelected ? 0.5f : 1f);

        // Click behavior
        holder.itemView.setOnClickListener(v -> {
            if (hasSelection()) {
                toggleSelection(position);
            }
        });

        // Long press starts selection
        holder.itemView.setOnLongClickListener(v -> {
            toggleSelection(position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    private void toggleSelection(int position) {
        ImageItem item = images.get(position);
        item.isSelected = !item.isSelected;
        notifyItemChanged(position);

        if (selectionListener != null) {
            selectionListener.onSelectionChanged(getSelectedCount());
        }
    }

    private boolean hasSelection() {
        for (ImageItem i : images) {
            if (i.isSelected) return true;
        }
        return false;
    }

    private int getSelectedCount() {
        int count = 0;
        for (ImageItem i : images) {
            if (i.isSelected) count++;
        }
        return count;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imageView;

        VH(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}