package pixelpen.keytag;

import android.content.Intent;
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
import android.widget.TextView;

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
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recycler_view);
        layoutManager = new GridLayoutManager(this, spanCount);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (adapter != null && adapter.getItemViewType(position) == 0)
                        ? layoutManager.getSpanCount()
                        : 1;
            }
        });
        recyclerView.setLayoutManager(layoutManager);

        int spacing = (int) (5 * getResources().getDisplayMetrics().density);
        recyclerView.addItemDecoration(
                new GridSpacingDecoration(spanCount, spacing)
        );

        if (searchUris != null) {
            loadSearchResults(searchUris);
        } else {
            loadImages(bucketId);
            insertDateHeaders();
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

        });  // ← closes ImageAdapter callback

        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        // Share mode auto-launch dialog AFTER adapter ready
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

        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_TAKEN
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

            int idColumn   = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);

            while (cursor.moveToNext()) {

                long id        = cursor.getLong(idColumn);
                long dateTaken = cursor.getLong(dateColumn);

                Uri contentUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        String.valueOf(id)
                );

                ImageItem item = new ImageItem(id, contentUri);
                item.dateTaken = dateTaken;
                images.add(item);
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

                    android.util.Log.d("STAR_DEBUG", "Apply pressed — keyword=" + keyword + " rating=" + rating[0]);

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

            // Resolve exotic URIs to stable MediaStore URIs
            if (uri.getAuthority() != null && !uri.getAuthority().equals("media")) {
                long mediaId = pixelpen.keytag.util.MediaStoreUtil.getMediaStoreId(
                        getApplicationContext(), uri);
                if (mediaId != -1) {
                    // Try images first
                    Uri resolved = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            mediaId);
                    uri = resolved;
                    uriString = resolved.toString();
                }
            }

            ImageItem item = new ImageItem(0, uri);

            // Detect video by URI path
            String uriLower = uriString.toLowerCase();
            if (uriString.contains(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())
                    || uriLower.endsWith(".mp4")
                    || uriLower.endsWith(".mov")
                    || uriLower.endsWith(".avi")
                    || uriLower.endsWith(".mkv")) {
                item.isVideo = true;
            }

            images.add(item);
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


    private void writeXmpSidecar(android.content.Context context, Uri imageUri, String keyword) {
        try {
            // Resolve file path from MediaStore
            String filePath = null;
            android.database.Cursor cursor = context.getContentResolver().query(
                    imageUri,
                    new String[]{ android.provider.MediaStore.Images.Media.DATA },
                    null, null, null
            );
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(0);
                }
                cursor.close();
            }

            if (filePath == null) return;

            // Derive sidecar path: same name, .xmp extension
            String xmpPath = filePath.replaceAll("\\.[^.]+$", ".xmp");
            java.io.File xmpFile = new java.io.File(xmpPath);

            // Read existing keywords if sidecar already exists
            List<String> existingKeywords = new ArrayList<>();
            if (xmpFile.exists()) {
                String existing = new String(
                        java.nio.file.Files.readAllBytes(xmpFile.toPath())
                );
                // Extract existing rdf:li entries
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("<rdf:li>(.+?)</rdf:li>")
                        .matcher(existing);
                while (m.find()) {
                    existingKeywords.add(m.group(1).trim());
                }
            }

            // Add new keyword if not already present
            if (!existingKeywords.contains(keyword)) {
                existingKeywords.add(keyword);
            }

            // Build XMP content
            StringBuilder items = new StringBuilder();
            for (String kw : existingKeywords) {
                items.append("        <rdf:li>").append(kw).append("</rdf:li>\n");
            }

            String xmp =
                    "<?xpacket begin='' id='W5M0MpCehiHzreSzNTczkc9d'?>\n" +
                            "<x:xmpmeta xmlns:x='adobe:ns:meta/'>\n" +
                            "  <rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>\n" +
                            "    <rdf:Description rdf:about=''\n" +
                            "        xmlns:dc='http://purl.org/dc/elements/1.1/'>\n" +
                            "      <dc:subject>\n" +
                            "        <rdf:Bag>\n" +
                            items +
                            "        </rdf:Bag>\n" +
                            "      </dc:subject>\n" +
                            "    </rdf:Description>\n" +
                            "  </rdf:RDF>\n" +
                            "</x:xmpmeta>\n" +
                            "<?xpacket end='w'?>";

            // Write sidecar
            java.io.FileWriter writer = new java.io.FileWriter(xmpFile, false);
            writer.write(xmp);
            writer.close();

            android.util.Log.d("XMP_DEBUG", "XMP written: " + xmpPath + " keywords=" + existingKeywords);

        } catch (Exception e) {
            android.util.Log.d("XMP_DEBUG", "XMP write failed: " + e.getMessage());
        }
    }

    private void insertDateHeaders() {

        java.util.List<ImageItem> withHeaders = new java.util.ArrayList<>();

        String lastLabel = null;

        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("MMMM  yyyy", java.util.Locale.getDefault());

        for (ImageItem item : images) {

            String label = item.dateTaken > 0
                    ? sdf.format(new java.util.Date(item.dateTaken))
                    : "Unknown Date";

            if (!label.equals(lastLabel)) {
                withHeaders.add(ImageItem.asHeader(label));
                lastLabel = label;
            }

            withHeaders.add(item);
        }

        images.clear();
        images.addAll(withHeaders);
    }
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_album_contents, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_open_gallery) {
            openFirstImageInGallery();
            return true;
        }
        if (item.getItemId() == R.id.action_search) {
            showGlobalSearchDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openFirstImageInGallery() {
        for (ImageItem item : images) {
            if (item.isHeader) continue;
            android.content.Intent intent =
                    new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(item.uri, "image/*");
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(intent);
            } catch (Exception e) {
                android.widget.Toast.makeText(
                        this, "No gallery app found",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            }
            return;
        }
    }

    private void showGlobalSearchDialog() {

        android.view.View dialogView =
                getLayoutInflater().inflate(R.layout.dialog_global_search, null);

        AutoCompleteTextView searchInput =
                dialogView.findViewById(R.id.searchInput);

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            java.util.List<String> keywords = dao.getAllKeywordNames();

            runOnUiThread(() -> {

                ArrayAdapter<String> kwAdapter =
                        new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                keywords
                        );

                searchInput.setAdapter(kwAdapter);

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

            java.util.List<String> uris = dao.getImageUrisForKeyword(keyword);

            runOnUiThread(() -> {

                if (uris.isEmpty()) {
                    android.widget.Toast.makeText(
                            this,
                            "No results found",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                Intent intent = new Intent(this, AlbumContentsActivity.class);
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

}

