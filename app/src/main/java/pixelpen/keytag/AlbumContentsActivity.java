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
                    android.widget.Toast.makeText(
                            this,
                            selectedCount + " items ready for tagging",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
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
}