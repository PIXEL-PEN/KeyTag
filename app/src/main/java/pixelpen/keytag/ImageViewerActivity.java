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

import androidx.core.view.ViewCompat;

import android.widget.FrameLayout;

import android.widget.ImageView;
import android.net.Uri;
import android.content.Intent;


import android.graphics.Color;

import android.widget.LinearLayout;

import android.content.ContentUris;
import android.net.Uri;

import android.util.Log;



public class ImageViewerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private boolean isSystemUiVisible = false;

    private View keywordPanel;
    private EditText keywordInput;
    private TextView saveKeyword;

    private ArrayList<String> imageList;

    private com.google.android.material.chip.ChipGroup keywordChipGroup;


    private View exifPanel;
    private TextView exifText;
    private boolean isExifVisible = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageView btnShare = findViewById(R.id.btnShare);

        btnShare.setOnClickListener(v -> {

            if (imageList == null || imageList.isEmpty()) return;

            int position = viewPager.getCurrentItem();
            if (position < 0 || position >= imageList.size()) return;

            Uri currentUri = Uri.parse(imageList.get(position));

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, currentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Image"));
        });

        ImageView btnOpen = findViewById(R.id.btnOpen);

        btnOpen.setOnClickListener(v -> {

            if (imageList == null || imageList.isEmpty()) return;

            int position = viewPager.getCurrentItem();
            if (position < 0 || position >= imageList.size()) return;

            Uri currentUri = Uri.parse(imageList.get(position));

            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(currentUri, "image/*");
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(viewIntent);
        });

        LinearLayout starContainer = findViewById(R.id.starContainer);

        starContainer.setOnClickListener(v -> {
            toggleFavorite();
        });

        View overlay = findViewById(R.id.uiOverlayContainer);

        ViewCompat.setOnApplyWindowInsetsListener(overlay, (v, insets) -> insets);

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

        exifPanel = findViewById(R.id.exifPanel);

        exifPanel.post(() -> {

            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int panelWidth = screenWidth / 3;

            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) exifPanel.getLayoutParams();

            params.width = panelWidth;
            exifPanel.setLayoutParams(params);

            exifPanel.setTranslationX(panelWidth);
        });

        exifText = findViewById(R.id.exifText);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (view, insets) -> {

                    int topInset = insets.getInsets(
                            android.view.WindowInsets.Type.statusBars()
                    ).top;

                    int offset = topInset + dpToPx(48);

                    keywordChipGroup.setPadding(
                            keywordChipGroup.getPaddingLeft(),
                            offset,
                            keywordChipGroup.getPaddingRight(),
                            keywordChipGroup.getPaddingBottom()
                    );

                    exifPanel.setPadding(
                            exifPanel.getPaddingLeft(),
                            offset,
                            exifPanel.getPaddingRight(),
                            exifPanel.getPaddingBottom()
                    );

                    return insets;
                }
        );

        ViewCompat.setOnApplyWindowInsetsListener(exifPanel, (view, insets) -> {

            int topInset = insets.getInsets(
                    android.view.WindowInsets.Type.statusBars()
            ).top;

            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) view.getLayoutParams();

            params.topMargin = topInset + dpToPx(36);
            view.setLayoutParams(params);

            return insets;
        });

        imageList = getIntent().getStringArrayListExtra("image_list");
        int startPosition = getIntent().getIntExtra("start_position", 0);

        if (imageList != null && !imageList.isEmpty()) {

            ViewerPagerAdapter adapter =
                    new ViewerPagerAdapter(imageList);

            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(startPosition, false);

            // ADD THESE TWO LINES
            String uri = imageList.get(startPosition);
            loadKeywordsForImage(uri);
            loadQualityForImage(uri);
        }

        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {

                    public void onPageSelected(int position) {

                        if (imageList == null || position < 0 || position >= imageList.size())
                            return;

                        String uri = imageList.get(position);

                        loadKeywordsForImage(uri);
                        loadQualityForImage(uri);
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

            long mediaId = pixelpen.keytag.util.MediaStoreUtil.getMediaStoreId(
                    getApplicationContext(),
                    android.net.Uri.parse(uriString)
            );

            ImageEntity image = null;

            if (mediaId != -1) {
                image = dao.getImageByMediaStoreId(mediaId);
            }

            if (image == null) {
                image = dao.getImageByUri(uriString);
            }

            if (image == null) {
                runOnUiThread(() -> keywordChipGroup.removeAllViews());
                return;
            }
            final ImageEntity finalImage = image;

            List<KeywordEntity> keywords =
                    dao.getKeywordsForImage(finalImage.id);

            runOnUiThread(() -> {

                keywordChipGroup.removeAllViews();

                for (KeywordEntity keyword : keywords) {

                    com.google.android.material.chip.Chip chip =
                            new com.google.android.material.chip.Chip(this);

                    chip.setText(keyword.name);
                    chip.setCloseIconVisible(true);

                    chip.setOnCloseIconClickListener(v ->
                            confirmRemoveKeyword(finalImage.id, keyword.id)
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

            // Remove relation
            dao.removeCrossRef(imageId, keywordId);

            // Check real usage from crossref table
            int remaining = dao.getKeywordUsageFromCrossRef(keywordId);

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

    public void toggleExifPanel(String uriString) {

        int panelWidth = exifPanel.getWidth();

        if (panelWidth == 0) {
            panelWidth = (int) (280 * getResources().getDisplayMetrics().density);
        }

        if (isExifVisible) {

            exifPanel.animate()
                    .translationX(panelWidth)
                    .setDuration(250);

            isExifVisible = false;

        } else {

            loadExif(uriString);

            exifPanel.animate()
                    .translationX(0)
                    .setDuration(250);

            isExifVisible = true;
        }
    }

    private void loadExif(String uriString) {
        new Thread(() -> {
            try {
                android.net.Uri uri = android.net.Uri.parse(uriString);
                androidx.exifinterface.media.ExifInterface exif =
                        new androidx.exifinterface.media.ExifInterface(
                                getContentResolver().openInputStream(uri));

                String make     = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE);
                String model    = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL);
                String iso      = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED_RATINGS);
                String exposure = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME);
                String aperture = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER);
                String focal    = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH);
                String date     = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME);

                int imgWidth = 0, imgHeight = 0;
                try {
                    android.database.Cursor cursor = getContentResolver().query(
                            uri,
                            new String[]{
                                    android.provider.MediaStore.Images.Media.WIDTH,
                                    android.provider.MediaStore.Images.Media.HEIGHT
                            }, null, null, null);
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            imgWidth  = cursor.getInt(0);
                            imgHeight = cursor.getInt(1);
                        }
                        cursor.close();
                    }
                } catch (Exception ignored) {}

                List<String> keywordNames = new ArrayList<>();
                try {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    TaggingDao dao = db.taggingDao();
                    long mediaId = pixelpen.keytag.util.MediaStoreUtil.getMediaStoreId(
                            getApplicationContext(), uri);
                    ImageEntity image = null;
                    if (mediaId != -1) {
                        image = dao.getImageByMediaStoreId(mediaId);
                    }
                    if (image == null) {
                        image = dao.getImageByUri(uri.toString());
                    }
                    if (image != null) {
                        List<pixelpen.keytag.db.KeywordEntity> kws =
                                dao.getKeywordsForImage(image.id);
                        for (pixelpen.keytag.db.KeywordEntity kw : kws) {
                            keywordNames.add(kw.name);
                        }
                    }
                } catch (Exception ignored) {}

                StringBuilder sb = new StringBuilder();

                if (date != null)
                    sb.append(formatExifDate(date)).append("\n");

                if (make != null || model != null) {
                    sb.append("\n");
                    sb.append(make != null ? make : "")
                            .append(" ")
                            .append(model != null ? model : "")
                            .append("\n");
                }

                boolean hasSpecs = iso != null || exposure != null
                        || aperture != null || focal != null;
                if (hasSpecs) {
                    sb.append("\n");
                    if (iso != null)      sb.append("ISO ").append(iso).append("\n");
                    if (exposure != null) sb.append(formatExposure(exposure)).append("\n");
                    if (aperture != null) sb.append("f/").append(aperture).append("\n");
                    if (focal != null)    sb.append(formatFocal(focal)).append("\n");
                }

                int finalWidth = imgWidth;
                int finalHeight = imgHeight;
                if (finalWidth > 0 && finalHeight > 0) {
                    long mp = Math.round((finalWidth * (long) finalHeight) / 1_000_000.0);
                    sb.append("\n")
                            .append(finalWidth).append(" × ").append(finalHeight)
                            .append("  •  ").append(mp).append(" MP\n");
                }

                if (!keywordNames.isEmpty()) {
                    sb.append("\n")
                            .append(android.text.TextUtils.join("  ·  ", keywordNames))
                            .append("\n");
                }

                String result = sb.toString();
                runOnUiThread(() -> exifText.setText(result));

            } catch (Exception e) {
                runOnUiThread(() -> exifText.setText("No EXIF data available"));
            }
        }).start();
    }

    // Format exposure as fraction e.g. 0.004 → 1/250 sec
    private String formatExposure(String exposure) {
        try {
            double val = Double.parseDouble(exposure);
            if (val > 0 && val < 1) {
                long denom = Math.round(1.0 / val);
                return "1/" + denom + " sec";
            } else {
                return exposure + " sec";
            }
        } catch (Exception e) {
            return exposure + " sec";
        }
    }

    // Format focal length — Exif stores as rational e.g. "24/1"
    private String formatFocal(String focal) {
        try {
            if (focal.contains("/")) {
                String[] parts = focal.split("/");
                double num   = Double.parseDouble(parts[0]);
                double denom = Double.parseDouble(parts[1]);
                if (denom != 0) {
                    long mm = Math.round(num / denom);
                    return mm + " mm";
                }
            }
            return focal + " mm";
        } catch (Exception e) {
            return focal + " mm";
        }
    }
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void toggleFavorite() {

        int position = viewPager.getCurrentItem();
        if (imageList == null || position < 0 || position >= imageList.size())
            return;

        String uri = Uri.parse(imageList.get(position)).toString();

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            ImageEntity image = dao.getImageByUri(uri);

            int newLevel;

            if (image == null) {

                newLevel = 1;

                ImageEntity newImage = new ImageEntity(uri, System.currentTimeMillis());
                newImage.qualityLevel = newLevel;

                dao.insertImage(newImage);

            } else {

                int current = image.qualityLevel;
                newLevel = (current + 1) % 4;

                dao.updateQuality(uri, newLevel);
            }

            Log.d("KEYTAG", "STAR WRITE URI = " + uri);

            int finalLevel = newLevel;

            runOnUiThread(() -> updateStarIconForLevel(finalLevel));

        }).start();
    }

    private void updateStarIconForLevel(int level) {

        ImageView star1 = findViewById(R.id.star1);
        ImageView star2 = findViewById(R.id.star2);
        ImageView star3 = findViewById(R.id.star3);

        int filled = R.drawable.baseline_star_24;
        int empty = R.drawable.baseline_star_border_24;

        int gold = Color.parseColor("#FFC107");
        int white = Color.WHITE;

        // Star 1
        star1.setImageResource(level >= 1 ? filled : empty);
        star1.setColorFilter(level >= 1 ? gold : white);

        // Star 2
        star2.setImageResource(level >= 2 ? filled : empty);
        star2.setColorFilter(level >= 2 ? gold : white);

        // Star 3
        star3.setImageResource(level >= 3 ? filled : empty);
        star3.setColorFilter(level >= 3 ? gold : white);
    }

    private void loadQualityForImage(String uri) {

        android.util.Log.d("KEYTAG", "Viewer lookup URI = " + uri);

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            long mediaId = pixelpen.keytag.util.MediaStoreUtil.getMediaStoreId(
                    getApplicationContext(),
                    android.net.Uri.parse(uri)
            );

            android.util.Log.d("STAR_DEBUG", "Viewer READ uri=" + uri + " mediaId=" + mediaId);

            Integer level = null;

            if (mediaId != -1) {
                level = dao.getQualityByMediaStoreId(mediaId);
            }

            if (level == null) {
                level = dao.getQuality(uri);
            }

            if (level == null) {
                level = 0;
            }

            int finalLevel = level;

            runOnUiThread(() -> updateStarIconForLevel(finalLevel));

        }).start();
    }

    private String formatExifDate(String exifDate) {
        try {
            // Exif format: "2026:03:20 10:15:00"
            String[] parts = exifDate.split(" ");
            String[] dateParts = parts[0].split(":");

            int year  = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int day   = Integer.parseInt(dateParts[2]);

            String[] monthNames = {
                    "Jan.","Feb.","Mar.","Apr.","May","Jun.",
                    "Jul.","Aug.","Sep.","Oct.","Nov.","Dec."
            };

            String monthName = (month >= 1 && month <= 12)
                    ? monthNames[month - 1] : String.valueOf(month);

            String time = parts.length > 1 ? parts[1] : "";

            return year + "  " + monthName + " " + day + "     " + time;

        } catch (Exception e) {
            return exifDate;
        }
    }



}