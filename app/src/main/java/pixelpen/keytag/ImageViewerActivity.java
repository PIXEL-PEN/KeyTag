package pixelpen.keytag;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;

public class ImageViewerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        viewPager = findViewById(R.id.viewPager);

        ArrayList<String> imageList =
                getIntent().getStringArrayListExtra("image_list");

        int startPosition =
                getIntent().getIntExtra("start_position", 0);

        if (imageList != null && !imageList.isEmpty()) {

            ViewerPagerAdapter adapter =
                    new ViewerPagerAdapter(imageList);

            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(startPosition, false);
        }
    }
}