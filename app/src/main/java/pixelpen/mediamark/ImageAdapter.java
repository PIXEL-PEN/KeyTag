package pixelpen.mediamark;

import pixelpen.mediamark.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import com.bumptech.glide.Glide;




public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.VH> {

    private List<ImageItem> images;

    public ImageAdapter(List<ImageItem> images) {
        this.images = images;
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
        Glide.with(holder.imageView.getContext())
                .load(images.get(position).uri)
                .centerCrop()
                .into(holder.imageView);

    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imageView;

        VH(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
