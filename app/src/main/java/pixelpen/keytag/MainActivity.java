package pixelpen.keytag;

import android.os.Bundle;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.ContentUris;
import android.database.Cursor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatDelegate;


public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<ImageItem> imageList = new ArrayList<>();
    private ImageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));

        adapter = new ImageAdapter(imageList);
        recyclerView.setAdapter(adapter);

        int spacing = dpToPx(6);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(4, spacing, true));



        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.READ_MEDIA_IMAGES},
                        100
                );
            } else {
                loadImages();
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        100
                );
            } else {
                loadImages();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            loadImages();
        }
    }

    private void loadImages() {

        imageList.clear();

        String[] projection = {
                MediaStore.Images.Media._ID
        };

        String sortOrder =
                MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
        )) {

            if (cursor != null) {
                int idColumn =
                        cursor.getColumnIndexOrThrow(
                                MediaStore.Images.Media._ID);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);

                    Uri uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                    );

                    imageList.add(new ImageItem(id, uri));
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }



}
