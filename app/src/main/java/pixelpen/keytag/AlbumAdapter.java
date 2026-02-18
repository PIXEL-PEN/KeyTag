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

        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(v.getContext(), AlbumContentsActivity.class);
            intent.putExtra("bucket_id", album.bucketId);
            intent.putExtra("bucket_name", album.bucketName);
            v.getContext().startActivity(intent);
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
        TextView textName;
        TextView textCount;

        VH(View itemView) {
            super(itemView);
            imageFolder = itemView.findViewById(R.id.imageFolder);
            textName = itemView.findViewById(R.id.textAlbumName);
            textCount = itemView.findViewById(R.id.textCount);
        }
    }
}
