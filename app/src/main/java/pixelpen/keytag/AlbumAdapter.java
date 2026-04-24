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
import android.content.Intent;


import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.VH> {

    private final List<AlbumItem> albums;

    public AlbumAdapter(List<AlbumItem> albums) {
        this.albums = albums;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {

        AlbumItem album = albums.get(position);

        holder.textName.setText(album.bucketName);
        holder.textCount.setText(album.itemCount + " items");

        boolean isShortList = album.bucketName != null &&
                album.bucketName.trim().equalsIgnoreCase("ShortList");

        if (isShortList) {
            holder.textName.setTextColor(android.graphics.Color.parseColor("#FFC107"));
            holder.imageFolder.setColorFilter(
                    android.graphics.Color.parseColor("#FFC107"),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
            holder.itemView.setBackgroundColor(
                    android.graphics.Color.parseColor("#1AFFC107")
            );
            holder.shortlistBadge.setVisibility(View.VISIBLE);
        } else if (album.bucketId == -2) {
            // Videos-fin tile
            holder.textName.setTextColor(android.graphics.Color.parseColor("#64B5F6"));
            holder.imageFolder.setImageResource(android.R.drawable.ic_media_play);
            holder.imageFolder.setColorFilter(
                    android.graphics.Color.parseColor("#64B5F6"),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
            holder.itemView.setBackgroundColor(
                    android.graphics.Color.parseColor("#1A64B5F6")
            );
            holder.shortlistBadge.setVisibility(View.GONE);
        } else {
            holder.textName.setTextColor(android.graphics.Color.WHITE);
            holder.imageFolder.setImageResource(R.drawable.baseline_folder_24);
            holder.imageFolder.setColorFilter(
                    android.graphics.Color.parseColor("#FFC107"),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
            holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            holder.shortlistBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (album.bucketId == -2) {
                Intent intent = new Intent(v.getContext(), VideoContentsActivity.class);
                v.getContext().startActivity(intent);
            } else {
                Intent intent = new Intent(v.getContext(), AlbumContentsActivity.class);
                intent.putExtra("bucket_id", album.bucketId);
                intent.putExtra("bucket_name", album.bucketName);
                v.getContext().startActivity(intent);
            }
        });

        // Long-press to hide album
        holder.itemView.setOnLongClickListener(v -> {
            if (isShortList) return true; // ShortList cannot be hidden
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(v.getContext())
                    .setTitle("Hide album?")
                    .setMessage("\"" + album.bucketName + "\" will be hidden from KeyTag.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Hide", (dialog, which) -> {
                        android.content.SharedPreferences prefs =
                                v.getContext().getSharedPreferences("keytag_prefs",
                                        android.content.Context.MODE_PRIVATE);
                        java.util.Set<String> hidden = new java.util.HashSet<>(
                                prefs.getStringSet("hidden_buckets",
                                        new java.util.HashSet<>())
                        );
                        hidden.add(album.bucketId + ":" + album.bucketName);
                        prefs.edit().putStringSet("hidden_buckets", hidden).apply();
                        albums.remove(position);
                        notifyItemRemoved(position);
                    })
                    .show();
            return true;
        });

        // Make tile square
        holder.itemView.post(() -> {
            int width = holder.itemView.getWidth();
            holder.itemView.getLayoutParams().height = width;
            holder.itemView.requestLayout();
        });
    }
    @Override
    public int getItemCount() {
        return albums.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        ImageView imageFolder;
        ImageView shortlistBadge;
        TextView textName;
        TextView textCount;

        VH(View itemView) {
            super(itemView);
            imageFolder = itemView.findViewById(R.id.imageFolder);
            shortlistBadge = itemView.findViewById(R.id.shortlistBadge);
            textName = itemView.findViewById(R.id.textAlbumName);
            textCount = itemView.findViewById(R.id.textCount);
        }
    }
}
