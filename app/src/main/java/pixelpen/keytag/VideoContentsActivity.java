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

}