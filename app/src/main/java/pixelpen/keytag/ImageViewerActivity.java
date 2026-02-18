package pixelpen.keytag;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class ImageViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        Uri uri = getIntent().getParcelableExtra("image_uri");

        ImageView imageView = findViewById(R.id.fullImage);

        Glide.with(this)
                .load(uri)
                .into(imageView);
    }
}
