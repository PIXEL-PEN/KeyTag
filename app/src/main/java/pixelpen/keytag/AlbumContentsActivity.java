package pixelpen.keytag;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.ScaleGestureDetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;

import pixelpen.keytag.db.AppDatabase;
import pixelpen.keytag.db.TaggingDao;
import pixelpen.keytag.db.ImageEntity;
import pixelpen.keytag.db.KeywordEntity;
import pixelpen.keytag.db.ImageKeywordCrossRef;
import android.widget.ImageView;

import pixelpen.keytag.util.MediaStoreUtil;

public class AlbumContentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<ImageItem> images = new ArrayList<>();

    private String bucketName;
    private ImageAdapter adapter;

    private GridLayoutManager layoutManager;
    private int spanCount = 4;
    private final int MIN_SPAN = 2;
    private final int MAX_SPAN = 6;


    private boolean shareMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<String> searchUris =
                getIntent().getStringArrayListExtra("search_results");

        shareMode =
                getIntent().getBooleanExtra("share_mode", false);
        android.util.Log.d("SHARE_DEBUG", "shareMode = " + shareMode);

        getWindow().setDecorFitsSystemWindows(true);
        setContentView(R.layout.activity_album_contents);

        long bucketId = getIntent().getLongExtra("bucket_id", -1);
        bucketName = getIntent().getStringExtra("bucket_name");

        MaterialToolbar toolbar = findViewById(R.id.topBar);
        toolbar.setTitle(bucketName);

        recyclerView = findViewById(R.id.recycler_view);

        layoutManager = new GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(layoutManager);

        int spacing = (int) (5 * getResources().getDisplayMetrics().density);
        recyclerView.addItemDecoration(
                new GridSpacingDecoration(spanCount, spacing)
        );

        if (searchUris != null) {
            loadSearchResults(searchUris);
        } else {
            loadImages(bucketId);
        }

        adapter = new ImageAdapter(images, selectedCount -> {

            toolbar.getMenu().clear();
            toolbar.setNavigationIcon(null);
            toolbar.setOnMenuItemClickListener(null);

            if (selectedCount > 0) {

                toolbar.setTitle(selectedCount + " selected");

                toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);
                toolbar.setNavigationOnClickListener(v -> adapter.clearSelection());

                toolbar.getMenu().add("TAG")
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                toolbar.setOnMenuItemClickListener(item -> {
                    showBatchTagDialog();
                    return true;
                });

            } else {
                toolbar.setTitle(bucketName);
                toolbar.setNavigationIcon(null);
            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        // 🔥 Share mode auto-launch dialog AFTER adapter ready
        if (shareMode && searchUris != null && !searchUris.isEmpty()) {
            recyclerView.post(() -> {
                adapter.selectAll();
                showBatchTagDialog();
            });
        }

        // Grid pinch zoom
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

    private void loadImages(long bucketId) {

        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = { MediaStore.Images.Media._ID };

        String selection = MediaStore.Images.Media.BUCKET_ID + "=?";
        String[] selectionArgs = { String.valueOf(bucketId) };

        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        Cursor cursor = getContentResolver().query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );

        if (cursor != null) {

            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

            while (cursor.moveToNext()) {

                long id = cursor.getLong(idColumn);

                Uri contentUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        String.valueOf(id)
                );

                images.add(new ImageItem(id, contentUri));
            }

            cursor.close();
        }
    }

    private void showBatchTagDialog() {

        android.view.View dialogView =
                getLayoutInflater().inflate(R.layout.dialog_batch_tag, null);

        AutoCompleteTextView tagInput =
                dialogView.findViewById(R.id.tagInput);

        // Rating state
        final int[] rating = {0};

        // Setup dialog stars
        android.view.View ratingRow = dialogView.findViewById(R.id.ratingRow);

        ratingRow.setOnClickListener(v -> {
            rating[0] = (rating[0] + 1) % 4;
            updateDialogStars(dialogView, rating[0]);
        });

        // Load autocomplete keywords
        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            List<String> keywords = dao.getAllKeywordNames();

            runOnUiThread(() -> {
                ArrayAdapter<String> adapter =
                        new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                keywords
                        );
                tagInput.setAdapter(adapter);
            });

        }).start();

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (dialog, which) -> {

                    String keyword = tagInput.getText().toString().trim();

                    applyMetadataToSelected(keyword, rating[0]);

                })
                .show();
    }
    private void applyMetadataToSelected(String keyword, int rating) {

        final String normalized = keyword == null
                ? ""
                : keyword.trim().toLowerCase();

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            KeywordEntity keywordEntity = null;

            if (!normalized.isEmpty()) {

                keywordEntity = dao.getKeywordByName(normalized);

                if (keywordEntity == null) {
                    dao.insertKeyword(new KeywordEntity(normalized, 0));
                    keywordEntity = dao.getKeywordByName(normalized);
                }
            }

            for (ImageItem item : images) {

                if (!item.isSelected) continue;

                String uriString = item.uri.toString();

                long mediaId = MediaStoreUtil.getMediaStoreId(
                        getApplicationContext(),
                        Uri.parse(uriString)
                );

                ImageEntity image = null;

                if (mediaId != -1) {
                    image = dao.getImageByMediaStoreId(mediaId);
                }

                if (image == null) {
                    image = dao.getImageByUri(uriString);
                }

                if (image == null) {

                    dao.insertImage(new ImageEntity(uriString, System.currentTimeMillis()));

                    if (mediaId != -1) {
                        dao.updateMediaStoreId(uriString, mediaId);
                        image = dao.getImageByMediaStoreId(mediaId);
                    } else {
                        image = dao.getImageByUri(uriString);
                    }
                }
                if (image == null) continue;

                // Apply rating
                if (mediaId != -1) {
                    if (mediaId != -1) {
                        dao.updateQualityByMediaStoreId(mediaId, rating);

                        android.util.Log.d("STAR_DEBUG",
                                "WRITE uri=" + uriString +
                                        " mediaId=" + mediaId +
                                        " rating=" + rating);

                    } else {
                        dao.updateQuality(uriString, rating);
                    }
                } else {
                    dao.updateQuality(uriString, rating);
                }
                // Apply keyword (if provided)
                if (keywordEntity != null) {
                    dao.insertCrossRef(
                            new ImageKeywordCrossRef(image.id, keywordEntity.id)
                    );
                }
            }

            if (keywordEntity != null) {
                dao.incrementUsage(keywordEntity.id);
            }

            runOnUiThread(() -> {
                adapter.clearSelection();
                android.widget.Toast.makeText(
                        this,
                        "Metadata applied",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            });

        }).start();
    }
    private void loadSearchResults(ArrayList<String> uriStrings) {

        images.clear();

        for (String uriString : uriStrings) {

            Uri uri = Uri.parse(uriString);

            images.add(new ImageItem(0, uri));
        }
    }
    private void updateDialogStars(android.view.View dialogView, int level) {

        ImageView star1 = dialogView.findViewById(R.id.dialogStar1);
        ImageView star2 = dialogView.findViewById(R.id.dialogStar2);
        ImageView star3 = dialogView.findViewById(R.id.dialogStar3);

        int filled = R.drawable.baseline_star_24;
        int empty  = R.drawable.baseline_star_border_24;

        int gold = android.graphics.Color.parseColor("#FFC107");
        int white = android.graphics.Color.WHITE;

        star1.setImageResource(level >= 1 ? filled : empty);
        star1.setColorFilter(level >= 1 ? gold : white);

        star2.setImageResource(level >= 2 ? filled : empty);
        star2.setColorFilter(level >= 2 ? gold : white);

        star3.setImageResource(level >= 3 ? filled : empty);
        star3.setColorFilter(level >= 3 ? gold : white);
    }
}