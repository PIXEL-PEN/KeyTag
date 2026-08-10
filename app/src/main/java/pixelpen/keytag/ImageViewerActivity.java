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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
            showShareDialog(currentUri);
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

        ImageView btnTag = findViewById(R.id.btnTag);
        btnTag.setOnClickListener(v -> {
            if (imageList == null || imageList.isEmpty()) return;
            int position = viewPager.getCurrentItem();
            if (position < 0 || position >= imageList.size()) return;
            String currentUri = imageList.get(position);
            showSingleImageTagDialog(currentUri);
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
        keywordChipGroup.setVisibility(View.GONE);
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
            keywordChipGroup.setVisibility(View.GONE);
        } else {
            controller.show(android.view.WindowInsets.Type.systemBars());
            keywordChipGroup.setVisibility(View.VISIBLE);
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
            panelWidth = (int) (320 * getResources().getDisplayMetrics().density);
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

// Request unredacted location data
                android.net.Uri originalUri = uri;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    originalUri = android.provider.MediaStore.setRequireOriginal(uri);
                }

                androidx.exifinterface.media.ExifInterface exif;
                try {
                    exif = new androidx.exifinterface.media.ExifInterface(
                            getContentResolver().openInputStream(originalUri));
                } catch (Exception e) {
                    // Fallback to regular uri if setRequireOriginal fails
                    exif = new androidx.exifinterface.media.ExifInterface(
                            getContentResolver().openInputStream(uri));
                }


                android.database.Cursor pathCursor = getContentResolver().query(
                        uri,
                        new String[]{ android.provider.MediaStore.Images.Media.DATA },
                        null, null, null);
                if (pathCursor != null && pathCursor.moveToFirst()) {
                    String path = pathCursor.getString(0);
                    pathCursor.close();
                    exif = new androidx.exifinterface.media.ExifInterface(path);
                } else {
                    if (pathCursor != null) pathCursor.close();
                    exif = new androidx.exifinterface.media.ExifInterface(
                            getContentResolver().openInputStream(uri));
                }

                String make     = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE);
                String model    = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL);
                String iso      = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED_RATINGS);
                String exposure = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME);
                String aperture = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER);
                String focal    = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH);
                String date     = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME);

                // GPS — try built-in first
                double[] latLon = exif.getLatLong();
                if (latLon == null) {
                    String latStr = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE);
                    String latRef = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF);
                    String lonStr = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE);
                    String lonRef = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF);
                    if (latStr != null && lonStr != null) {
                        double lat = parseGpsRational(latStr);
                        double lon = parseGpsRational(lonStr);
                        if (latRef != null && latRef.equals("S")) lat = -lat;
                        if (lonRef != null && lonRef.equals("W")) lon = -lon;
                        if (lat != 0 || lon != 0) {
                            latLon = new double[]{lat, lon};
                        }
                    }
                }



                // MediaStore query
                int imgWidth = 0, imgHeight = 0;
                String displayName = null;
                long dateModified = 0;
                long fileSize = 0;
                String filePath = null;

                try {
                    android.database.Cursor cursor = getContentResolver().query(
                            uri,
                            new String[]{
                                    android.provider.MediaStore.Images.Media.WIDTH,
                                    android.provider.MediaStore.Images.Media.HEIGHT,
                                    android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                                    android.provider.MediaStore.Images.Media.DATE_MODIFIED,
                                    android.provider.MediaStore.Images.Media.SIZE,
                                    android.provider.MediaStore.Images.Media.DATA
                            }, null, null, null);
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            imgWidth     = cursor.getInt(0);
                            imgHeight    = cursor.getInt(1);
                            displayName  = cursor.getString(2);
                            dateModified = cursor.getLong(3) * 1000L;
                            fileSize     = cursor.getLong(4);
                            filePath     = cursor.getString(5);
                        }
                        cursor.close();
                    }
                } catch (Exception ignored) {}

                // Keywords
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

                // Build display string
                StringBuilder sb = new StringBuilder();

                if (displayName != null)
                    sb.append(displayName).append("\n\n");

                if (date != null)
                    sb.append("Date Taken:  ").append(formatExifDate(date)).append("\n");

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
                    double mp = (finalWidth * (long) finalHeight) / 1_000_000.0;
                    sb.append("\nResolution:\n")
                            .append(finalWidth).append(" × ").append(finalHeight)
                            .append("  (").append(String.format("%.1f", mp)).append(" MP)\n");
                }

                if (fileSize > 0) {
                    sb.append("\nSize:\n").append(formatFileSize(fileSize)).append("\n");
                }

                if (dateModified > 0) {
                    java.text.SimpleDateFormat dateSdf =
                            new java.text.SimpleDateFormat("MMM. dd yyyy", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat timeSdf =
                            new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault());
                    java.util.Date modDate = new java.util.Date(dateModified);
                    sb.append("\nModified:\n")
                            .append(dateSdf.format(modDate)).append("\n")
                            .append(timeSdf.format(modDate)).append("\n");
                }
                String altStr = exif.getAttribute(
                        androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE);
                String altRef = exif.getAttribute(
                        androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF);

                if (latLon != null) {
                    sb.append("\nGPS Coordinates:\n")
                            .append(String.format(java.util.Locale.getDefault(),
                                    "%.5f°,  %.5f°", latLon[0], latLon[1]))
                            .append("\n");
                }



                if (altStr != null) {
                    try {
                        double alt = evalRational(altStr);
                        if ("1".equals(altRef)) alt = -alt;
                        if (alt > 0) {
                            sb.append("\nAltitude:\n")
                                    .append(String.format(java.util.Locale.getDefault(),
                                            "%.1f m", alt))
                                    .append("\n");
                        }
                    } catch (Exception ignored) {}
                }

                if (filePath != null) {
                    java.io.File f = new java.io.File(filePath);
                    sb.append("\nPath:\n")
                            .append(f.getParent()).append("/\n")
                            .append(f.getName()).append("\n");
                }
                if (!keywordNames.isEmpty()) {
                    sb.append("\nKeywords:  ")
                            .append(android.text.TextUtils.join("  ·  ", keywordNames))
                            .append("\n");
                }

                String result = sb.toString();
                final double[] finalLatLon = latLon;

                runOnUiThread(() -> {
                    exifText.setText(result);
                    if (finalLatLon != null) {
                        exifText.setOnClickListener(v -> {
                            String mapsUri = String.format(java.util.Locale.getDefault(),
                                    "geo:%.5f,%.5f?q=%.5f,%.5f",
                                    finalLatLon[0], finalLatLon[1],
                                    finalLatLon[0], finalLatLon[1]);
                            android.content.Intent intent = new android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(mapsUri));
                            try {
                                startActivity(intent);
                            } catch (Exception ignored) {}
                        });
                    } else {
                        exifText.setOnClickListener(null);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> exifText.setText("No EXIF data available"));
            }
        }).start();
    }
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        else return String.format("%.1f MB", bytes / (1024.0 * 1024));
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


        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            long mediaId = pixelpen.keytag.util.MediaStoreUtil.getMediaStoreId(
                    getApplicationContext(),
                    android.net.Uri.parse(uri)
            );


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

            String timeFormatted = "";
            if (parts.length > 1) {
                try {
                    java.text.SimpleDateFormat input =
                            new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat output =
                            new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault());
                    timeFormatted = output.format(input.parse(parts[1]));
                } catch (Exception ignored) {
                    timeFormatted = parts[1];
                }
            }

            return monthName + " " + day + "  " + year + "     " + timeFormatted;
        } catch (Exception e) {
            return exifDate;
        }
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            WindowInsetsControllerCompat controller =
                    new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.hide(android.view.WindowInsets.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }

    private double parseGpsRational(String raw) {
        try {
            String[] parts = raw.split(",");
            double deg = 0, min = 0, sec = 0;
            if (parts.length > 0) deg = evalRational(parts[0].trim());
            if (parts.length > 1) min = evalRational(parts[1].trim());
            if (parts.length > 2) sec = evalRational(parts[2].trim());
            return deg + min / 60.0 + sec / 3600.0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double evalRational(String rational) {
        try {
            if (rational.contains("/")) {
                String[] parts = rational.split("/");
                double num = Double.parseDouble(parts[0].trim());
                double den = Double.parseDouble(parts[1].trim());
                return den != 0 ? num / den : 0;
            }
            return Double.parseDouble(rational);
        } catch (Exception e) {
            return 0;
        }
    }

    private void showShareDialog(Uri imageUri) {

        android.view.View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_share_options, null);

        android.widget.CheckBox stripAll =
                dialogView.findViewById(R.id.checkStripAll);

        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_KeyTag_Dialog)
                .setTitle("Share image")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Share", (dialog, which) -> {
                    if (stripAll.isChecked()) {
                        shareWithStrippedExif(imageUri, true);
                    } else {
                        shareDirectly(imageUri);
                    }
                })
                .show();
    }
    private void shareDirectly(Uri imageUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Image"));
    }

    private void shareWithStrippedExif(Uri imageUri, boolean stripAll) {
        new Thread(() -> {
            try {
                // Get file path
                String filePath = null;
                android.database.Cursor cursor = getContentResolver().query(
                        imageUri,
                        new String[]{ android.provider.MediaStore.Images.Media.DATA,
                                android.provider.MediaStore.Images.Media.DISPLAY_NAME },
                        null, null, null);
                String displayName = "image.jpg";
                if (cursor != null && cursor.moveToFirst()) {
                    filePath = cursor.getString(0);
                    displayName = cursor.getString(1);
                    cursor.close();
                }

                // Copy to cache
                java.io.File cacheDir = new java.io.File(getCacheDir(), "share");
                cacheDir.mkdirs();
                java.io.File tempFile = new java.io.File(cacheDir, displayName);

                // Copy original to temp
                java.io.InputStream in = getContentResolver().openInputStream(imageUri);
                java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                in.close();
                out.close();

                // Strip EXIF from temp file
                androidx.exifinterface.media.ExifInterface exif =
                        new androidx.exifinterface.media.ExifInterface(tempFile.getAbsolutePath());

                if (stripAll) {
                    // Strip all tags
                    String[] allTags = {
                            androidx.exifinterface.media.ExifInterface.TAG_MAKE,
                            androidx.exifinterface.media.ExifInterface.TAG_MODEL,
                            androidx.exifinterface.media.ExifInterface.TAG_DATETIME,
                            androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED_RATINGS,
                            androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME,
                            androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER,
                            androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH,
                            androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE,
                            androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE,
                            androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE,
                            androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF,
                            androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF,
                            androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF
                    };
                    for (String tag : allTags) {
                        exif.setAttribute(tag, null);
                    }
                } else {
                    // Strip location only
                    exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE, null);
                    exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE, null);
                    exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE, null);
                    exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF, null);
                    exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF, null);
                    exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF, null);
                }
                exif.saveAttributes();

                // Share temp file via FileProvider
                Uri shareUri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        tempFile);

                runOnUiThread(() -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Share Image"));
                });

            } catch (Exception e) {
                runOnUiThread(() -> android.widget.Toast.makeText(
                        this, "Share failed: " + e.getMessage(),
                        android.widget.Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showSingleImageTagDialog(String uriString) {
        android.view.View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_batch_tag, null);

        android.widget.AutoCompleteTextView tagInput =
                dialogView.findViewById(R.id.tagInput);

        final int[] rating = {0};
        android.view.View ratingRow = dialogView.findViewById(R.id.ratingRow);
        ratingRow.setOnClickListener(v -> {
            rating[0] = (rating[0] + 1) % 4;
            updateDialogStars(dialogView, rating[0]);
        });

        new Thread(() -> {
            pixelpen.keytag.db.AppDatabase db =
                    pixelpen.keytag.db.AppDatabase.getInstance(getApplicationContext());
            pixelpen.keytag.db.TaggingDao dao = db.taggingDao();
            java.util.List<String> keywords = dao.getAllKeywordNames();
            runOnUiThread(() -> {
                android.widget.ArrayAdapter<String> adapter =
                        new android.widget.ArrayAdapter<>(this,
                                R.layout.item_dropdown, keywords);
                tagInput.setAdapter(adapter);
            });
        }).start();

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                this, R.style.ThemeOverlay_KeyTag_Dialog)
                .setTitle("Tag this image")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    String keyword = tagInput.getText().toString().trim();
                    if (!keyword.isEmpty()) {
                        applyTagToSingleImage(uriString, keyword, rating[0]);
                    }
                })
                .show();
    }

    private void applyTagToSingleImage(String uriString, String keyword, int rating) {
        new Thread(() -> {
            pixelpen.keytag.db.AppDatabase db =
                    pixelpen.keytag.db.AppDatabase.getInstance(getApplicationContext());
            pixelpen.keytag.db.TaggingDao dao = db.taggingDao();

            android.net.Uri uri = android.net.Uri.parse(uriString);
            long mediaId = pixelpen.keytag.util.MediaStoreUtil.getMediaStoreId(
                    getApplicationContext(), uri);

            pixelpen.keytag.db.ImageEntity image = null;
            if (mediaId != -1) image = dao.getImageByMediaStoreId(mediaId);
            if (image == null) image = dao.getImageByUri(uriString);
            if (image == null) {
                dao.insertImage(new pixelpen.keytag.db.ImageEntity(
                        uriString, System.currentTimeMillis()));
                if (mediaId != -1) {
                    dao.updateMediaStoreId(uriString, mediaId);
                    image = dao.getImageByMediaStoreId(mediaId);
                } else {
                    image = dao.getImageByUri(uriString);
                }
            }
            if (image == null) return;

            if (rating > 0) {
                if (mediaId != -1) dao.updateQualityByMediaStoreId(mediaId, rating);
                else dao.updateQuality(uriString, rating);
            }

            final String normalized = keyword.trim().toLowerCase();
            final pixelpen.keytag.db.ImageEntity finalImage = image;
            String[] parts = normalized.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) continue;
                pixelpen.keytag.db.KeywordEntity ke = dao.getKeywordByName(trimmed);
                if (ke == null) {
                    dao.insertKeyword(new pixelpen.keytag.db.KeywordEntity(trimmed, 0));
                    ke = dao.getKeywordByName(trimmed);
                }
                if (ke != null) {
                    dao.insertCrossRef(new pixelpen.keytag.db.ImageKeywordCrossRef(
                            finalImage.id, ke.id));
                    dao.incrementUsage(ke.id);
                }
            }

            // Embed in image
            embedKeywordsInImage(getApplicationContext(), uri, normalized);

            runOnUiThread(() -> {
                loadKeywordsForImage(uriString);
                android.widget.Toast.makeText(this, "Tagged",
                        android.widget.Toast.LENGTH_SHORT).show();
            });
        }).start();
    }


    private void updateDialogStars(android.view.View dialogView, int level) {
        android.widget.ImageView star1 = dialogView.findViewById(R.id.dialogStar1);
        android.widget.ImageView star2 = dialogView.findViewById(R.id.dialogStar2);
        android.widget.ImageView star3 = dialogView.findViewById(R.id.dialogStar3);

        int filled = R.drawable.baseline_star_24;
        int empty  = R.drawable.baseline_star_border_24;
        int gold   = android.graphics.Color.parseColor("#FFC107");
        int white  = android.graphics.Color.WHITE;

        star1.setImageResource(level >= 1 ? filled : empty);
        star1.setColorFilter(level >= 1 ? gold : white);
        star2.setImageResource(level >= 2 ? filled : empty);
        star2.setColorFilter(level >= 2 ? gold : white);
        star3.setImageResource(level >= 3 ? filled : empty);
        star3.setColorFilter(level >= 3 ? gold : white);
    }


    private void embedKeywordsInImage(android.content.Context context, android.net.Uri imageUri, String keyword) {
        try {
            String filePath = null;
            android.database.Cursor cursor = context.getContentResolver().query(
                    imageUri,
                    new String[]{ android.provider.MediaStore.Images.Media.DATA },
                    null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) filePath = cursor.getString(0);
                cursor.close();
            }
            if (filePath == null) return;

            java.io.File imageFile = new java.io.File(filePath);
            byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());

            if (imageBytes.length < 2 || (imageBytes[0] & 0xFF) != 0xFF || (imageBytes[1] & 0xFF) != 0xD8) {
                return;
            }

            byte[] xmpNs = "http://ns.adobe.com/xap/1.0/\0".getBytes("UTF-8");

            java.util.List<String> existingKeywords = new java.util.ArrayList<>();
            java.io.ByteArrayOutputStream cleanStream = new java.io.ByteArrayOutputStream();

            cleanStream.write(imageBytes, 0, 2);
            int i = 2;

            while (i + 3 < imageBytes.length) {
                while (i < imageBytes.length && (imageBytes[i] & 0xFF) == 0xFF) i++;
                if (i >= imageBytes.length) break;

                int marker = imageBytes[i] & 0xFF;
                i--;

                if (marker == 0xDA || marker == 0xD9) {
                    cleanStream.write(imageBytes, i, imageBytes.length - i);
                    break;
                }

                int segLen = ((imageBytes[i+2] & 0xFF) << 8) | (imageBytes[i+3] & 0xFF);
                int segTotal = 2 + segLen;
                int segEnd = i + segTotal;

                if (segEnd > imageBytes.length) {
                    cleanStream.write(imageBytes, i, imageBytes.length - i);
                    break;
                }

                boolean isXmpApp1 = false;
                if (marker == 0xE1 && segLen > xmpNs.length + 2) {
                    isXmpApp1 = true;
                    for (int j = 0; j < xmpNs.length; j++) {
                        if (i + 4 + j >= imageBytes.length || imageBytes[i + 4 + j] != xmpNs[j]) {
                            isXmpApp1 = false;
                            break;
                        }
                    }
                }

                if (isXmpApp1) {
                    int dataStart = i + 4 + xmpNs.length;
                    if (dataStart < segEnd) {
                        String segStr = new String(imageBytes, dataStart, segEnd - dataStart, "UTF-8");
                        java.util.regex.Matcher m = java.util.regex.Pattern
                                .compile("<rdf:li>(.+?)</rdf:li>")
                                .matcher(segStr);
                        while (m.find()) {
                            String kw = m.group(1).trim();
                            if (!existingKeywords.contains(kw)) existingKeywords.add(kw);
                        }
                    }
                } else {
                    cleanStream.write(imageBytes, i, segTotal);
                }
                i = segEnd;
            }

            byte[] cleanBytes = cleanStream.toByteArray();

            // Add new keywords
            String[] parts = keyword.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && !existingKeywords.contains(trimmed)) {
                    existingKeywords.add(trimmed);
                }
            }

            StringBuilder items = new StringBuilder();
            for (String kw : existingKeywords) {
                items.append("<rdf:li>").append(kw).append("</rdf:li>");
            }

            String xmpStr =
                    "<?xpacket begin='\uFEFF' id='W5M0MpCehiHzreSzNTczkc9d'?>" +
                            "<x:xmpmeta xmlns:x='adobe:ns:meta/'>" +
                            "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>" +
                            "<rdf:Description rdf:about='' xmlns:dc='http://purl.org/dc/elements/1.1/'>" +
                            "<dc:subject><rdf:Bag>" + items + "</rdf:Bag></dc:subject>" +
                            "</rdf:Description></rdf:RDF></x:xmpmeta>" +
                            "<?xpacket end='w'?>";

            byte[] xmpData = xmpStr.getBytes("UTF-8");
            int segmentLength = 2 + xmpNs.length + xmpData.length;

            java.io.ByteArrayOutputStream xmpSegment = new java.io.ByteArrayOutputStream();
            xmpSegment.write(0xFF);
            xmpSegment.write(0xE1);
            xmpSegment.write((segmentLength >> 8) & 0xFF);
            xmpSegment.write(segmentLength & 0xFF);
            xmpSegment.write(xmpNs);
            xmpSegment.write(xmpData);

            java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile);
            fos.write(cleanBytes, 0, 2);
            fos.write(xmpSegment.toByteArray());
            fos.write(cleanBytes, 2, cleanBytes.length - 2);
            fos.close();

        } catch (Exception e) {
            android.util.Log.d("XMP_DEBUG", "Embed FAILED: " + e.getMessage());
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        View topBar = findViewById(R.id.topOverlayBar);
        if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            topBar.setVisibility(View.GONE);
        } else {
            topBar.setVisibility(View.VISIBLE);
        }
    }
}