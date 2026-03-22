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
        layoutManager = new GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(layoutManager);

        int spacing = (int) (5 * getResources().getDisplayMetrics().density);
        recyclerView.addItemDecoration(
                new GridSpacingDecoration(spanCount, spacing)
        );

        loadVideos();

        adapter = new VideoAdapter(videos, uri -> {
            // Tap to open in native player
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
        });


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
                        MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                },
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME + "=?",
                new String[]{ "Videos-fin" },
                MediaStore.Video.Media.DATE_TAKEN + " DESC"
        );

        if (cursor != null) {
            int idCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int dateCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN);
            int durCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);

            while (cursor.moveToNext()) {
                long id        = cursor.getLong(idCol);
                long dateTaken = cursor.getLong(dateCol);
                long duration  = cursor.getLong(durCol);
                String bucket  = cursor.getString(bucketCol);

                Uri uri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        String.valueOf(id)
                );

                ImageItem item = new ImageItem(id, uri);
                item.dateTaken  = dateTaken;
                item.isVideo    = true;
                item.duration   = duration;
                item.bucketName = bucket;
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


}