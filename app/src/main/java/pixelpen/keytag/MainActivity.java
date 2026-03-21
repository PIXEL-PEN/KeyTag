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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import pixelpen.keytag.db.AppDatabase;
import pixelpen.keytag.db.TaggingDao;
import java.util.ArrayList;

import android.widget.TextView;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private static final int REQUEST_FOLDER_PICK = 200;
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
            showHiddenAlbumsDialog();
            return true;
        }

        if (id == R.id.action_include) {
            showManageFoldersDialog();
            return true;
        }

        if (id == R.id.action_sort) {
            showSortDialog();
            return true;
        }
        if (id == R.id.action_search) {
            showGlobalSearchDialog();
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

        // Create ShortList folder if it doesn't exist
        java.io.File picturesRoot =
                android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES
                );

        java.io.File shortListFolder = new java.io.File(picturesRoot, "ShortList");

        if (!shortListFolder.exists()) {
            shortListFolder.mkdirs();
            sendBroadcast(new android.content.Intent(
                    android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    android.net.Uri.fromFile(shortListFolder)
            ));
        }

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

            int bucketIdColumn   = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
            int bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int pathColumn       = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH);

            // Load hidden set once outside the loop
            java.util.Set<String> hiddenBuckets = getSharedPreferences("keytag_prefs", MODE_PRIVATE)
                    .getStringSet("hidden_buckets", new java.util.HashSet<>());

            while (cursor.moveToNext()) {

                long bucketId       = cursor.getLong(bucketIdColumn);
                String bucketName   = cursor.getString(bucketNameColumn);
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

                // Filter hidden albums — stored as "bucketId:bucketName"
                boolean isHidden = false;
                for (String entry : hiddenBuckets) {
                    if (entry.startsWith(bucketId + ":")) { isHidden = true; break; }
                }
                if (isHidden) continue;

                if (!albumMap.containsKey(bucketId)) {
                    albumMap.put(bucketId, new AlbumItem(bucketId, bucketName, null, 1));
                } else {
                    albumMap.get(bucketId).itemCount++;
                }
            }

            cursor.close();
            albumList.addAll(albumMap.values());

            // Add manually included folders
            java.util.Set<String> includedFolders = getSharedPreferences("keytag_prefs", MODE_PRIVATE)
                    .getStringSet("included_folders", new java.util.HashSet<>());

            for (String folderPath : includedFolders) {
                java.io.File folder = new java.io.File(folderPath);
                if (!folder.exists()) continue;

                String folderName = folder.getName();

                android.database.Cursor inc = getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{
                                MediaStore.Images.Media.BUCKET_ID,
                                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                        },
                        MediaStore.Images.Media.DATA + " LIKE ?",
                        new String[]{ folderPath + "/%" },
                        null
                );

                if (inc != null) {
                    if (inc.moveToFirst()) {
                        long bucketId = inc.getLong(0);
                        String bucketName = inc.getString(1);
                        if (bucketName == null) bucketName = folderName;
                        if (!albumMap.containsKey(bucketId)) {
                            albumList.add(new AlbumItem(bucketId, bucketName, null, inc.getCount()));
                        }
                    }
                    inc.close();
                }
            }

        } // end if (cursor != null)

        // Separate ShortList from the rest
        AlbumItem shortListItem = null;
        List<AlbumItem> otherAlbums = new ArrayList<>();

        for (AlbumItem album : albumList) {
            if (album.bucketName != null &&
                    album.bucketName.trim().equalsIgnoreCase("ShortList")) {
                shortListItem = album;
            } else {
                otherAlbums.add(album);
            }
        }

        int sortMode = getSharedPreferences("keytag_prefs", MODE_PRIVATE)
                .getInt("sort_mode", 0);

        if (sortMode == 0) {
            java.util.Collections.sort(otherAlbums,
                    (a, b) -> a.bucketName.compareToIgnoreCase(b.bucketName));
        }

        List<AlbumItem> sortedList = new ArrayList<>();
        if (shortListItem != null) {
            sortedList.add(shortListItem);
        }
        sortedList.addAll(otherAlbums);

        recyclerView.setAdapter(new AlbumAdapter(sortedList));
        recyclerView.addItemDecoration(
                new GridDividerDecoration(4, 0x66FFFFFF, dpToPx(1))
        );
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showGlobalSearchDialog() {

        android.view.View dialogView =
                getLayoutInflater().inflate(R.layout.dialog_global_search, null);

        AutoCompleteTextView searchInput =
                dialogView.findViewById(R.id.searchInput);

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

                searchInput.setAdapter(adapter);

                TextView star1 = dialogView.findViewById(R.id.star1);
                TextView star2 = dialogView.findViewById(R.id.star2);
                TextView star3 = dialogView.findViewById(R.id.star3);


                star1.setOnClickListener(v -> searchByStars(1));
                star2.setOnClickListener(v -> searchByStars(2));
                star3.setOnClickListener(v -> searchByStars(3));


            });

        }).start();

        new MaterialAlertDialogBuilder(this)
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

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            java.util.List<String> uris =
                    dao.getImageUrisForKeyword(keyword);

            runOnUiThread(() -> {

                if (uris.isEmpty()) {
                    android.widget.Toast.makeText(
                            this,
                            "No results found",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                android.content.Intent intent =
                        new android.content.Intent(this,
                                AlbumContentsActivity.class);

                intent.putStringArrayListExtra(
                        "search_results",
                        new ArrayList<>(uris)
                );

                intent.putExtra("bucket_name", "Search Results");

                startActivity(intent);
            });

        }).start();
    }

    private void searchByStars(int level) {

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            List<String> results = dao.getUrisByStarLevel(level);

            runOnUiThread(() -> {

                Intent intent = new Intent(this, AlbumContentsActivity.class);

                intent.putStringArrayListExtra(
                        "search_results",
                        new ArrayList<>(results)
                );

                intent.putExtra("bucket_name", "Search Results");

                startActivity(intent);

            });

        }).start();
    }

    private void showSortDialog() {
        String[] options = {"Alphabetical", "Date Added"};
        int current = getSharedPreferences("keytag_prefs", MODE_PRIVATE)
                .getInt("sort_mode", 0);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Sort Albums")
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    getSharedPreferences("keytag_prefs", MODE_PRIVATE)
                            .edit()
                            .putInt("sort_mode", which)
                            .apply();
                    dialog.dismiss();
                    loadAlbums();
                })
                .show();
    }

    private void showHiddenAlbumsDialog() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("keytag_prefs", MODE_PRIVATE);
        java.util.Set<String> hidden = new java.util.HashSet<>(
                prefs.getStringSet("hidden_buckets", new java.util.HashSet<>()));

        if (hidden.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Hidden Albums")
                    .setMessage("No albums are hidden.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Build display names and parallel entry list
        List<String> entries = new ArrayList<>(hidden);
        String[] displayNames = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i);
            int colon = entry.indexOf(":");
            displayNames[i] = colon >= 0 ? entry.substring(colon + 1) : entry;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Hidden Albums")
                .setItems(displayNames, (dialog, which) -> {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Restore album?")
                            .setMessage("\"" + displayNames[which] + "\" will reappear in KeyTag.")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Restore", (d, w) -> {
                                hidden.remove(entries.get(which));
                                prefs.edit()
                                        .putStringSet("hidden_buckets", hidden)
                                        .apply();
                                loadAlbums();
                            })
                            .show();
                })
                .setNeutralButton("Restore All", (dialog, which) -> {
                    prefs.edit().remove("hidden_buckets").apply();
                    loadAlbums();
                })
                .setNegativeButton("Close", null)
                .show();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FOLDER_PICK
                && resultCode == RESULT_OK
                && data != null) {

            android.net.Uri treeUri = data.getData();
            if (treeUri == null) return;

            String docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri);
            String path = null;

            if (docId.startsWith("primary:")) {
                path = android.os.Environment
                        .getExternalStorageDirectory()
                        .getAbsolutePath()
                        + "/"
                        + docId.substring("primary:".length());
            }

            if (path == null) return;

            android.content.SharedPreferences prefs =
                    getSharedPreferences("keytag_prefs", MODE_PRIVATE);
            java.util.Set<String> included = new java.util.HashSet<>(
                    prefs.getStringSet("included_folders", new java.util.HashSet<>())
            );
            included.add(path);
            prefs.edit().putStringSet("included_folders", included).apply();

            android.widget.Toast.makeText(
                    this,
                    "Folder added: " + path,
                    android.widget.Toast.LENGTH_SHORT
            ).show();

            loadAlbums();
        }


    }

    private void showManageFoldersDialog() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("keytag_prefs", MODE_PRIVATE);
        java.util.Set<String> included = new java.util.HashSet<>(
                prefs.getStringSet("included_folders", new java.util.HashSet<>()));

        List<String> folders = new ArrayList<>(included);

        if (folders.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Manage Folders")
                    .setMessage("No folders added yet.")
                    .setPositiveButton("Add Folder", (dialog, which) -> {
                        startActivityForResult(
                                new android.content.Intent(
                                        android.content.Intent.ACTION_OPEN_DOCUMENT_TREE),
                                REQUEST_FOLDER_PICK
                        );
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        // Show just folder names, store full paths
        String[] displayNames = new String[folders.size()];
        for (int i = 0; i < folders.size(); i++) {
            displayNames[i] = new java.io.File(folders.get(i)).getName();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Manage Folders")
                .setItems(displayNames, (dialog, which) -> {
                    String folderPath = folders.get(which);
                    String folderName = displayNames[which];
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Remove folder?")
                            .setMessage("\"" + folderName + "\" will be removed from KeyTag. Files are not deleted.")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Remove", (d, w) -> {
                                included.remove(folderPath);
                                prefs.edit()
                                        .putStringSet("included_folders", included)
                                        .apply();
                                loadAlbums();
                            })
                            .show();
                })
                .setNeutralButton("Add Folder", (dialog, which) -> {
                    startActivityForResult(
                            new android.content.Intent(
                                    android.content.Intent.ACTION_OPEN_DOCUMENT_TREE),
                            REQUEST_FOLDER_PICK
                    );
                })
                .setNegativeButton("Close", null)
                .show();
    }

}
