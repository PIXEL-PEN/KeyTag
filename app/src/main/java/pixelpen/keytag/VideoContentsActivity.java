package pixelpen.keytag;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ScaleGestureDetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class VideoContentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<ImageItem> videos = new ArrayList<>();
    private VideoAdapter adapter;
    private GridLayoutManager layoutManager;
    private int spanCount = 4;
    private final int MIN_SPAN = 2;
    private final int MAX_SPAN = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setDecorFitsSystemWindows(true);
        setContentView(R.layout.activity_album_contents);


        MaterialToolbar toolbar = findViewById(R.id.topBar);
        toolbar.setTitle("Videos-fin");
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setBackgroundColor(android.graphics.Color.parseColor("#E2E2DC"));

        layoutManager = new GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(layoutManager);

        int spacing = (int) (5 * getResources().getDisplayMetrics().density);
        recyclerView.addItemDecoration(
                new GridSpacingDecoration(spanCount, spacing)
        );

        loadVideos();

        adapter = new VideoAdapter(videos,
                uri -> {
                    // Tap — play in native player
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "video/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(
                                this, "No video player found",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                    }
                },
                item -> {
                    // Long press — show TAG dialog
                    showTagDialog(item);
                }
        );


        recyclerView.setAdapter(adapter);

        // Pinch zoom
        ScaleGestureDetector scaleDetector =
                new ScaleGestureDetector(this,
                        new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            @Override
                            public boolean onScale(ScaleGestureDetector detector) {
                                float scaleFactor = detector.getScaleFactor();
                                if (scaleFactor > 1.05f) {
                                    if (spanCount > MIN_SPAN) {
                                        spanCount--;
                                        layoutManager.setSpanCount(spanCount);
                                    }
                                } else if (scaleFactor < 0.95f) {
                                    if (spanCount < MAX_SPAN) {
                                        spanCount++;
                                        layoutManager.setSpanCount(spanCount);
                                    }
                                }
                                return true;
                            }
                        });

        recyclerView.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            return false;
        });
    }

    private void loadVideos() {
        Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DATE_TAKEN,
                        MediaStore.Video.Media.DURATION,
                        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.Video.Media.DISPLAY_NAME
                },
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME + "=?",
                new String[]{ "Videos-fin" },
                MediaStore.Video.Media.DATE_TAKEN + " DESC"
        );

        if (cursor != null) {
            int idCol      = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int dateCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN);
            int durCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int bucketCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
            int nameCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);

            while (cursor.moveToNext()) {
                long id          = cursor.getLong(idCol);
                long dateTaken   = cursor.getLong(dateCol);
                long duration    = cursor.getLong(durCol);
                String bucket    = cursor.getString(bucketCol);
                String displayName = cursor.getString(nameCol);

                Uri uri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        String.valueOf(id)
                );

                ImageItem item = new ImageItem(id, uri);
                item.dateTaken  = dateTaken;
                item.isVideo    = true;
                item.duration   = duration;
                item.bucketName = bucket;

                if (displayName != null && displayName.contains(".")) {
                    item.headerLabel = displayName.substring(0, displayName.lastIndexOf("."));
                } else {
                    item.headerLabel = displayName;
                }

                videos.add(item);
            }
            cursor.close();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_album_contents, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_open_gallery) {
            openFirstVideoInGallery();
            return true;
        }
        if (item.getItemId() == R.id.action_search) {
            showGlobalSearchDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void openFirstVideoInGallery() {
        // Try Xiaomi gallery first
        android.content.Intent intent = getPackageManager()
                .getLaunchIntentForPackage("com.miui.gallery");

        // Try Vivo gallery
        if (intent == null) {
            intent = getPackageManager()
                    .getLaunchIntentForPackage("com.vivo.gallery");
        }

        // Try Samsung gallery
        if (intent == null) {
            intent = getPackageManager()
                    .getLaunchIntentForPackage("com.sec.android.gallery3d");
        }

        // Generic fallback — may hit Google Photos
        if (intent == null) {
            intent = new android.content.Intent(
                    android.content.Intent.ACTION_MAIN);
            intent.addCategory(android.content.Intent.CATEGORY_APP_GALLERY);
        }

        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            android.widget.Toast.makeText(
                    this, "No gallery app found",
                    android.widget.Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showGlobalSearchDialog() {
        android.view.View dialogView =
                getLayoutInflater().inflate(R.layout.dialog_global_search, null);
        android.widget.AutoCompleteTextView searchInput =
                dialogView.findViewById(R.id.searchInput);

        new Thread(() -> {
            pixelpen.keytag.db.AppDatabase db =
                    pixelpen.keytag.db.AppDatabase.getInstance(getApplicationContext());
            pixelpen.keytag.db.TaggingDao dao = db.taggingDao();
            java.util.List<String> keywords = dao.getAllKeywordNames();

            runOnUiThread(() -> {
                android.widget.ArrayAdapter<String> kwAdapter =
                        new android.widget.ArrayAdapter<>(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                keywords
                        );
                searchInput.setAdapter(kwAdapter);

                android.widget.TextView star1 = dialogView.findViewById(R.id.star1);
                android.widget.TextView star2 = dialogView.findViewById(R.id.star2);
                android.widget.TextView star3 = dialogView.findViewById(R.id.star3);
                star1.setOnClickListener(v -> searchByStars(1));
                star2.setOnClickListener(v -> searchByStars(2));
                star3.setOnClickListener(v -> searchByStars(3));
            });
        }).start();

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Search by keyword")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Search", (dialog, which) -> {
                    String keyword =
                            searchInput.getText().toString().trim().toLowerCase();
                    if (!keyword.isEmpty()) {
                        performGlobalSearch(keyword);
                    }
                })
                .show();
    }

    private void performGlobalSearch(String keyword) {
        new Thread(() -> {
            pixelpen.keytag.db.AppDatabase db =
                    pixelpen.keytag.db.AppDatabase.getInstance(getApplicationContext());
            pixelpen.keytag.db.TaggingDao dao = db.taggingDao();
            java.util.List<String> uris = dao.getImageUrisForKeyword(keyword);

            runOnUiThread(() -> {
                if (uris.isEmpty()) {
                    android.widget.Toast.makeText(
                            this, "No results found",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    return;
                }
                android.content.Intent intent =
                        new android.content.Intent(this, AlbumContentsActivity.class);
                intent.putStringArrayListExtra(
                        "search_results", new java.util.ArrayList<>(uris));
                intent.putExtra("bucket_name", "Search Results");
                startActivity(intent);
            });
        }).start();
    }

    private void searchByStars(int level) {
        new Thread(() -> {
            pixelpen.keytag.db.AppDatabase db =
                    pixelpen.keytag.db.AppDatabase.getInstance(getApplicationContext());
            pixelpen.keytag.db.TaggingDao dao = db.taggingDao();
            java.util.List<String> results = dao.getUrisByStarLevel(level);

            runOnUiThread(() -> {
                android.content.Intent intent =
                        new android.content.Intent(this, AlbumContentsActivity.class);
                intent.putStringArrayListExtra(
                        "search_results", new java.util.ArrayList<>(results));
                intent.putExtra("bucket_name", "Search Results");
                startActivity(intent);
            });
        }).start();
    }
    private void showTagDialog(ImageItem item) {

        android.view.View dialogView =
                getLayoutInflater().inflate(R.layout.dialog_batch_tag, null);

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
                android.widget.ArrayAdapter<String> kwAdapter =
                        new android.widget.ArrayAdapter<>(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                keywords
                        );
                tagInput.setAdapter(kwAdapter);
            });
        }).start();

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    String keyword = tagInput.getText().toString().trim();
                    applyVideoTag(item, keyword, rating[0]);
                })
                .show();
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

    private void applyVideoTag(ImageItem item, String keyword, int rating) {

        final String normalized = keyword == null ? "" : keyword.trim().toLowerCase();

        new Thread(() -> {
            pixelpen.keytag.db.AppDatabase db =
                    pixelpen.keytag.db.AppDatabase.getInstance(getApplicationContext());
            pixelpen.keytag.db.TaggingDao dao = db.taggingDao();

            String uriString = item.uri.toString();

            long mediaId = pixelpen.keytag.util.MediaStoreUtil.getMediaStoreId(
                    getApplicationContext(),
                    item.uri
            );

            pixelpen.keytag.db.ImageEntity image = null;

            if (mediaId != -1) {
                image = dao.getImageByMediaStoreId(mediaId);
            }
            if (image == null) {
                image = dao.getImageByUri(uriString);
            }
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

            // Apply rating
            if (mediaId != -1) {
                dao.updateQualityByMediaStoreId(mediaId, rating);
            } else {
                dao.updateQuality(uriString, rating);
            }

            // Apply keyword
            if (!normalized.isEmpty()) {
                pixelpen.keytag.db.KeywordEntity kwEntity =
                        dao.getKeywordByName(normalized);
                if (kwEntity == null) {
                    dao.insertKeyword(new pixelpen.keytag.db.KeywordEntity(normalized, 0));
                    kwEntity = dao.getKeywordByName(normalized);
                }
                if (kwEntity != null) {
                    dao.insertCrossRef(new pixelpen.keytag.db.ImageKeywordCrossRef(
                            image.id, kwEntity.id));
                    dao.incrementUsage(kwEntity.id);
                }
            }

            runOnUiThread(() ->
                    android.widget.Toast.makeText(
                            this, "Metadata applied",
                            android.widget.Toast.LENGTH_SHORT
                    ).show()
            );

        }).start();
    }

}