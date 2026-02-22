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

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.EditText;

import pixelpen.keytag.db.AppDatabase;
import pixelpen.keytag.db.TaggingDao;
import pixelpen.keytag.db.ImageEntity;
import pixelpen.keytag.db.KeywordEntity;
import pixelpen.keytag.db.ImageKeywordCrossRef;

import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;


public class AlbumContentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<ImageItem> images = new ArrayList<>();

    private String bucketName;
    private ImageAdapter adapter;

    private GridLayoutManager layoutManager;
    private int spanCount = 4;
    private final int MIN_SPAN = 2;
    private final int MAX_SPAN = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        loadImages(bucketId);

        adapter = new ImageAdapter(images, selectedCount -> {

            toolbar.getMenu().clear();

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

        // GRID PINCH — Installed ONCE
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

        String[] projection = {
                MediaStore.Images.Media._ID
        };

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

        android.view.View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_batch_tag, null);

        AutoCompleteTextView tagInput =
                dialogView.findViewById(R.id.tagInput);

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            java.util.List<String> keywords =
                    dao.getAllKeywordNames();

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



        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (dialog, which) -> {

                    String keyword = tagInput.getText().toString().trim();

                    if (!keyword.isEmpty()) {
                        applyKeywordToSelected(keyword);
                    }
                })
                .show();
    }

    private void applyKeywordToSelected(String keyword) {

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            KeywordEntity keywordEntity = dao.getKeywordByName(keyword);

            if (keywordEntity == null) {
                dao.insertKeyword(new KeywordEntity(keyword, 0));
                keywordEntity = dao.getKeywordByName(keyword);
            }

            if (keywordEntity == null) return;

            for (ImageItem item : images) {

                if (!item.isSelected) continue;

                String uriString = item.uri.toString();

                ImageEntity image = dao.getImageByUri(uriString);

                if (image == null) {
                    dao.insertImage(new ImageEntity(uriString, System.currentTimeMillis()));
                    image = dao.getImageByUri(uriString);
                }

                if (image == null) continue;

                dao.insertCrossRef(
                        new ImageKeywordCrossRef(image.id, keywordEntity.id)
                );
            }

            runOnUiThread(() -> {
                adapter.clearSelection();
                android.widget.Toast.makeText(
                        this,
                        "Keyword applied",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            });

        }).start();
    }
}