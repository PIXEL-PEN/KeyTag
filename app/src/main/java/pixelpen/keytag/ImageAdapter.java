package pixelpen.keytag;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import pixelpen.keytag.db.AppDatabase;
import pixelpen.keytag.db.TaggingDao;

import pixelpen.keytag.util.MediaStoreUtil;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.VH> {

    public interface SelectionListener {
        void onSelectionChanged(int selectedCount);
    }

    private List<ImageItem> images;
    private SelectionListener selectionListener;
    private boolean selectionMode = false;

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

        Glide.with(holder.imageView.getContext())
                .load(item.uri)
                .centerCrop()
                .into(holder.imageView);

        // Visual feedback
        holder.selectionCircle.setVisibility(
                item.isSelected ? View.VISIBLE : View.GONE);

        holder.checkMark.setVisibility(
                item.isSelected ? View.VISIBLE : View.GONE);

        // Click behavior
        holder.itemView.setOnClickListener(v -> {

            if (selectionMode) {
                toggleSelection(position);
            } else {

                android.content.Context context = v.getContext();

                android.content.Intent intent =
                        new android.content.Intent(context, ImageViewerActivity.class);

                java.util.ArrayList<String> uriList = new java.util.ArrayList<>();

                for (ImageItem img : images) {
                    uriList.add(img.uri.toString());
                }

                intent.putStringArrayListExtra("image_list", uriList);
                intent.putExtra("start_position", position);

                context.startActivity(intent);
            }
        });

        // Long press starts selection
        holder.itemView.setOnLongClickListener(v -> {
            toggleSelection(position);
            return true;
        });

        ImageView starOverlay = holder.itemView.findViewById(R.id.starOverlay);

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(holder.imageView.getContext());
            TaggingDao dao = db.taggingDao();

            long mediaId = MediaStoreUtil.getMediaStoreId(
                    holder.imageView.getContext(),
                    item.uri
            );

            Integer tmpLevel = null;

            if (mediaId != -1) {
                tmpLevel = dao.getQualityByMediaStoreId(mediaId);

                if (tmpLevel == null) {
                    dao.updateMediaStoreId(item.uri.toString(), mediaId);
                }
            }

            if (tmpLevel == null) {
                tmpLevel = dao.getQuality(item.uri.toString());
            }
            final Integer level = tmpLevel;

            holder.itemView.post(() -> {

                android.util.Log.d("STAR_DEBUG",
                        "READ uri=" + item.uri +
                                " mediaId=" + mediaId +
                                " level=" + level);

                if (level != null && level > 0) {
                    starOverlay.setVisibility(View.VISIBLE);
                } else {
                    starOverlay.setVisibility(View.GONE);
                }
            });

        }).start();
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
        View selectionCircle;
        ImageView checkMark;

        VH(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            selectionCircle = itemView.findViewById(R.id.selectionCircle);
            checkMark = itemView.findViewById(R.id.checkMark);
        }
    }

    public void clearSelection() {
        for (ImageItem item : images) {
            item.isSelected = false;
        }
        selectionMode = false;
        notifyDataSetChanged();

        if (selectionListener != null) {
            selectionListener.onSelectionChanged(0);
        }
    }

    public void selectAll() {
        for (ImageItem item : images) {
            item.isSelected = true;
        }
        selectionMode = true;
        notifyDataSetChanged();

        if (selectionListener != null) {
            selectionListener.onSelectionChanged(getSelectedCount());
        }
    }

}