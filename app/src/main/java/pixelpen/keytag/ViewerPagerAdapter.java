package pixelpen.keytag;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.OnPhotoTapListener;

import java.util.List;

public class ViewerPagerAdapter extends RecyclerView.Adapter<ViewerPagerAdapter.ViewHolder> {

    private final List<String> imageUris;

    public ViewerPagerAdapter(List<String> imageUris) {
        this.imageUris = imageUris;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        PhotoView photoView = new PhotoView(parent.getContext());
        photoView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        photoView.setBackgroundColor(android.graphics.Color.BLACK);

        return new ViewHolder(photoView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String uriString = imageUris.get(position);
        Uri uri = Uri.parse(uriString);

        Glide.with(holder.photoView.getContext())
                .load(uri)
                .into(holder.photoView);

        holder.photoView.setOnPhotoTapListener((view, x, y) -> {

            if (view.getContext() instanceof ImageViewerActivity) {

                ImageViewerActivity activity =
                        (ImageViewerActivity) view.getContext();

                activity.toggleSystemUi();

                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    activity.toggleExifPanel(
                            imageUris.get(currentPos)
                    );
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        PhotoView photoView;

        ViewHolder(@NonNull PhotoView itemView) {
            super(itemView);
            photoView = itemView;
        }
    }
}