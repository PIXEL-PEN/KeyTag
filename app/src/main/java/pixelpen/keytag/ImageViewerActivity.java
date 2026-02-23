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
import java.util.List;

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

    private com.google.android.material.chip.ChipGroup keywordChipGroup;

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
        keywordChipGroup = findViewById(R.id.keywordChipGroup);

        imageList = getIntent().getStringArrayListExtra("image_list");
        int startPosition = getIntent().getIntExtra("start_position", 0);

        if (imageList != null && !imageList.isEmpty()) {

            ViewerPagerAdapter adapter =
                    new ViewerPagerAdapter(imageList);

            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(startPosition, false);

            String currentUri = imageList.get(startPosition);
            loadKeywordsForImage(currentUri);
        }

        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        if (imageList != null) {
                            String uri = imageList.get(position);
                            loadKeywordsForImage(uri);
                        }
                    }
                });

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

            runOnUiThread(() ->
                    loadKeywordsForImage(uriString)
            );

        }).start();
    }

    private void loadKeywordsForImage(String uriString) {

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            ImageEntity image = dao.getImageByUri(uriString);
            if (image == null) return;

            List<KeywordEntity> keywords =
                    dao.getKeywordsForImage(image.id);

            runOnUiThread(() -> {

                keywordChipGroup.removeAllViews();

                for (KeywordEntity keyword : keywords) {

                    com.google.android.material.chip.Chip chip =
                            new com.google.android.material.chip.Chip(this);

                    chip.setText(keyword.name);
                    chip.setCloseIconVisible(true);

                    chip.setOnCloseIconClickListener(v ->
                            confirmRemoveKeyword(image.id, keyword.id)
                    );

                    keywordChipGroup.addView(chip);
                }
            });

        }).start();
    }

    private void confirmRemoveKeyword(long imageId, long keywordId) {

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Remove keyword?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (d, w) ->
                        removeKeywordFromImage(imageId, keywordId)
                )
                .show();
    }

    private void removeKeywordFromImage(long imageId, long keywordId) {

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            dao.removeCrossRef(imageId, keywordId);
            dao.decrementUsage(keywordId);

            int remaining = dao.getUsageCount(keywordId);

            if (remaining <= 0) {
                dao.deleteKeywordById(keywordId);
            }

            runOnUiThread(() -> {

                android.widget.Toast.makeText(
                        this,
                        "Keyword removed",
                        android.widget.Toast.LENGTH_SHORT
                ).show();

                int position = viewPager.getCurrentItem();
                String uri = imageList.get(position);
                loadKeywordsForImage(uri);
            });

        }).start();
    }
}