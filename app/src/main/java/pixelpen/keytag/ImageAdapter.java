/*
 * KeyTag — Batch keyword tagging for Android
 * Copyright (C) 2026 TST (PIXEL-PEN)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * KeyTag — Batch keyword tagging for Android
 * Copyright (C) 2026 TST (PIXEL-PEN)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * KeyTag — Batch keyword tagging for Android
 * Copyright (C) 2026 PIXEL-PEN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package pixelpen.keytag;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import pixelpen.keytag.db.AppDatabase;
import pixelpen.keytag.db.TaggingDao;
import pixelpen.keytag.util.MediaStoreUtil;

public class ImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_IMAGE  = 1;

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

    @Override
    public int getItemViewType(int position) {
        return images.get(position).isHeader ? TYPE_HEADER : TYPE_IMAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_header, parent, false);
            return new HeaderVH(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image, parent, false);
            return new ImageVH(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        ImageItem item = images.get(position);

        if (item.isHeader) {
            ((HeaderVH) holder).headerText.setText(item.headerLabel);
            return;
        }

        ImageVH vh = (ImageVH) holder;

        Glide.with(vh.imageView.getContext())
                .load(item.uri)
                .centerCrop()
                .into(vh.imageView);

        vh.selectionCircle.setVisibility(item.isSelected ? View.VISIBLE : View.GONE);
        vh.checkMark.setVisibility(item.isSelected ? View.VISIBLE : View.GONE);

        ImageView starOverlay = vh.itemView.findViewById(R.id.starOverlay);

        if (item.isVideo) {
            // Show play badge, skip star lookup
            starOverlay.setVisibility(View.VISIBLE);
            starOverlay.setImageResource(android.R.drawable.ic_media_play);
            starOverlay.setColorFilter(
                    android.graphics.Color.WHITE,
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
        } else {
            // Restore star drawable in case recycled from video
            starOverlay.setImageResource(R.drawable.baseline_star_24);
            starOverlay.setColorFilter(
                    android.graphics.Color.WHITE,
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(vh.imageView.getContext());
                TaggingDao dao = db.taggingDao();

                long mediaId = MediaStoreUtil.getMediaStoreId(
                        vh.imageView.getContext(), item.uri
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

                vh.itemView.post(() -> {
                    if (level != null && level > 0) {
                        starOverlay.setVisibility(View.VISIBLE);
                    } else {
                        starOverlay.setVisibility(View.GONE);
                    }
                });
            }).start();
        }

        vh.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(holder.getAdapterPosition());
            } else {
                android.content.Context context = v.getContext();

                if (item.isVideo) {
                    android.content.Intent intent =
                            new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    intent.setDataAndType(item.uri, "video/*");
                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        context.startActivity(intent);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(
                                context, "No video player found",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                    }
                    return;
                }

                android.content.Intent intent =
                        new android.content.Intent(context, ImageViewerActivity.class);

                java.util.ArrayList<String> uriList = new java.util.ArrayList<>();
                for (ImageItem img : images) {
                    if (!img.isHeader) {
                        uriList.add(img.uri.toString());
                    }
                }

                int imagePosition = 0;
                for (int i = 0; i < holder.getAdapterPosition(); i++) {
                    if (!images.get(i).isHeader) imagePosition++;
                }

                intent.putStringArrayListExtra("image_list", uriList);
                intent.putExtra("start_position", imagePosition);
                context.startActivity(intent);
            }
        });

        vh.itemView.setOnLongClickListener(v -> {
            toggleSelection(holder.getAdapterPosition());
            return true;
        });
    }
    @Override
    public int getItemCount() {
        return images.size();
    }

    private void toggleSelection(int position) {
        if (position < 0 || position >= images.size()) return;
        ImageItem item = images.get(position);
        if (item.isHeader) return;
        item.isSelected = !item.isSelected;
        notifyItemChanged(position);
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(getSelectedCount());
        }
    }

    private int getSelectedCount() {
        int count = 0;
        for (ImageItem i : images) {
            if (!i.isHeader && i.isSelected) count++;
        }
        return count;
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
            if (!item.isHeader) item.isSelected = true;
        }
        selectionMode = true;
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(getSelectedCount());
        }
    }

    // --- ViewHolders ---

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView headerText;
        HeaderVH(View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
        }
    }

    static class ImageVH extends RecyclerView.ViewHolder {
        ImageView imageView;
        View selectionCircle;
        ImageView checkMark;
        ImageVH(View itemView) {
            super(itemView);
            imageView       = itemView.findViewById(R.id.imageView);
            selectionCircle = itemView.findViewById(R.id.selectionCircle);
            checkMark       = itemView.findViewById(R.id.checkMark);
        }
    }
}