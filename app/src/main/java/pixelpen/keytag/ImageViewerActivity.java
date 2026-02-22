package pixelpen.keytag;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;

import pixelpen.keytag.db.AppDatabase;
import pixelpen.keytag.db.TaggingDao;
import pixelpen.keytag.db.ImageEntity;
import pixelpen.keytag.db.KeywordEntity;
import pixelpen.keytag.db.ImageKeywordCrossRef;

public class ImageViewerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private boolean isSystemUiVisible = false;

    private View keywordPanel;
    private EditText keywordInput;
    private TextView saveKeyword;

    private ArrayList<String> imageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        // Immersive mode
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        controller.hide(android.view.WindowInsets.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        viewPager = findViewById(R.id.viewPager);

        keywordPanel = findViewById(R.id.keywordPanel);
        keywordInput = findViewById(R.id.keywordInput);
        saveKeyword = findViewById(R.id.saveKeyword);

        imageList = getIntent().getStringArrayListExtra("image_list");
        int startPosition =
                getIntent().getIntExtra("start_position", 0);

        if (imageList != null && !imageList.isEmpty()) {

            ViewerPagerAdapter adapter =
                    new ViewerPagerAdapter(imageList);

            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(startPosition, false);
        }

        saveKeyword.setOnClickListener(v -> {

            String keyword = keywordInput.getText().toString().trim();

            if (!keyword.isEmpty() && imageList != null) {

                String currentUri =
                        imageList.get(viewPager.getCurrentItem());

                addKeywordToCurrentImage(currentUri, keyword);

                keywordInput.setText("");
                keywordPanel.setVisibility(View.GONE);
            }
        });
    }

    public void toggleSystemUi() {

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        if (isSystemUiVisible) {
            controller.hide(android.view.WindowInsets.Type.systemBars());
        } else {
            controller.show(android.view.WindowInsets.Type.systemBars());
        }

        isSystemUiVisible = !isSystemUiVisible;
    }

    public void showKeywordPanel() {
        keywordPanel.setVisibility(View.VISIBLE);
        keywordInput.requestFocus();
    }

    private void addKeywordToCurrentImage(String uriString, String keywordName) {

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            ImageEntity image = dao.getImageByUri(uriString);

            if (image == null) {
                dao.insertImage(new ImageEntity(uriString, System.currentTimeMillis()));
                image = dao.getImageByUri(uriString);
            }

            if (image == null) return;

            KeywordEntity keyword = dao.getKeywordByName(keywordName);

            if (keyword == null) {
                dao.insertKeyword(new KeywordEntity(keywordName, 0));
                keyword = dao.getKeywordByName(keywordName);
            }

            if (keyword == null) return;

            dao.insertCrossRef(
                    new ImageKeywordCrossRef(image.id, keyword.id)
            );

        }).start();
    }
}