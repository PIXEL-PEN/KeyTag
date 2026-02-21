package pixelpen.keytag;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.appbar.MaterialToolbar;


public class AlbumContentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<ImageItem> images = new ArrayList<>();

    private String bucketName;
    private ImageAdapter adapter;

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

        int spanCount = 4;
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));

        int spacing = (int) (5 * getResources().getDisplayMetrics().density);
        recyclerView.addItemDecoration(
                new GridSpacingDecoration(spanCount, spacing)
        );

        loadImages(bucketId);

        adapter = new ImageAdapter(images, selectedCount -> {

            if (selectedCount > 0) {
                toolbar.setTitle(selectedCount + " selected");
                toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);

                toolbar.setNavigationOnClickListener(v -> {
                    adapter.clearSelection();
                });

            } else {
                toolbar.setTitle(bucketName);
                toolbar.setNavigationIcon(null);
            }
        });

        recyclerView.setAdapter(adapter);
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
