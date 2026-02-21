package pixelpen.keytag;

import android.os.Bundle;
import android.provider.MediaStore;
import android.database.Cursor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        super.onCreate(savedInstanceState);

        getWindow().setDecorFitsSystemWindows(true);

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.topBar);
        toolbar.setTitle("Albums");
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.READ_MEDIA_IMAGES},
                        100
                );
            } else {
                loadAlbums();
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        100
                );
            } else {
                loadAlbums();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_hide) {
            android.widget.Toast.makeText(this, "Hide clicked", android.widget.Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.action_include) {
            android.widget.Toast.makeText(this, "Include Folder clicked", android.widget.Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.action_sort) {
            android.widget.Toast.makeText(this, "Sort clicked", android.widget.Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            loadAlbums();
        }
    }

    private void loadAlbums() {

        List<AlbumItem> albumList = new ArrayList<>();

        String[] projection = {
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH
        };

        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );

        if (cursor != null) {

            Map<Long, AlbumItem> albumMap = new LinkedHashMap<>();

            int bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
            int bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH);

            while (cursor.moveToNext()) {

                long bucketId = cursor.getLong(bucketIdColumn);
                String bucketName = cursor.getString(bucketNameColumn);
                String relativePath = cursor.getString(pathColumn);

                if (bucketName == null || relativePath == null) continue;

                String lowerName = bucketName.toLowerCase();
                String lowerPath = relativePath.toLowerCase();

                if (!lowerPath.startsWith("dcim/") &&
                        !lowerPath.startsWith("pictures/")) continue;

                if (lowerPath.contains("android/")) continue;
                if (lowerPath.contains("whatsapp")) continue;
                if (lowerPath.contains("telegram")) continue;
                if (lowerName.startsWith("_")) continue;
                if (lowerName.startsWith(".")) continue;

                if (!albumMap.containsKey(bucketId)) {
                    albumMap.put(bucketId,
                            new AlbumItem(bucketId, bucketName, null, 1));
                } else {
                    albumMap.get(bucketId).itemCount++;
                }
            }

            cursor.close();
            albumList.addAll(albumMap.values());
        }

        recyclerView.setAdapter(new AlbumAdapter(albumList));
        recyclerView.addItemDecoration(
                new GridDividerDecoration(4, 0x66FFFFFF, dpToPx(1))
        );
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
