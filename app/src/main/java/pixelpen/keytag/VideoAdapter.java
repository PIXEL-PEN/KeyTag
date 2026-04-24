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

import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VH> {

    public interface OnVideoClickListener {
        void onVideoClick(Uri uri);
    }

    public interface OnVideoLongClickListener {
        void onVideoLongClick(ImageItem item);
    }

    private final List<ImageItem> videos;
    private final OnVideoClickListener listener;
    private final OnVideoLongClickListener longListener;

    public VideoAdapter(List<ImageItem> videos,
                        OnVideoClickListener listener,
                        OnVideoLongClickListener longListener) {
        this.videos = videos;
        this.listener = listener;
        this.longListener = longListener;
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
        ImageItem item = videos.get(position);

        new Thread(() -> {
            try {
                Bitmap thumb = MediaStore.Video.Thumbnails.getThumbnail(
                        holder.imageView.getContext().getContentResolver(),
                        item.id,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                );
                holder.imageView.post(() -> {
                    if (thumb != null) {
                        holder.imageView.setImageBitmap(thumb);
                    } else {
                        holder.imageView.setImageResource(
                                android.R.drawable.ic_media_play
                        );
                    }
                });
            } catch (Exception ignored) {}
        }).start();

        holder.starOverlay.setVisibility(View.VISIBLE);
        holder.starOverlay.setImageResource(android.R.drawable.ic_media_play);
        holder.starOverlay.setColorFilter(
                android.graphics.Color.WHITE,
                android.graphics.PorterDuff.Mode.SRC_IN
        );

        // Duration
        if (item.duration > 0) {
            long secs = item.duration / 1000;
            String dur = String.format(java.util.Locale.getDefault(),
                    "%d:%02d", secs / 60, secs % 60);
            holder.durationLabel.setText(dur);
            holder.durationLabel.setVisibility(View.VISIBLE);
        } else {
            holder.durationLabel.setVisibility(View.GONE);
        }

        // Title from display name
        if (item.headerLabel != null && !item.headerLabel.isEmpty()) {
            holder.videoTitle.setText(item.headerLabel);
            holder.videoTitle.setVisibility(View.VISIBLE);
        } else {
            holder.videoTitle.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onVideoClick(item.uri));

        holder.itemView.setOnLongClickListener(v -> {
            longListener.onVideoLongClick(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView starOverlay;
        TextView durationLabel;
        TextView videoTitle;

        VH(View itemView) {
            super(itemView);
            imageView     = itemView.findViewById(R.id.imageView);
            starOverlay   = itemView.findViewById(R.id.starOverlay);
            durationLabel = itemView.findViewById(R.id.durationLabel);
            videoTitle    = itemView.findViewById(R.id.videoTitle);
        }
    }
}