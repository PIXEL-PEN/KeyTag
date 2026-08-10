/*
 * KeyTag — Batch keyword tagging for Android
 * Copyright (C) 2026 TST (PIXEL-PEN)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * KeyTag — Batch keyword tagging for Android
 * Copyright (C) 2026 TST (PIXEL-PEN)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * KeyTag — Batch keyword tagging for Android
 * Copyright (C) 2026 PIXEL-PEN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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



        getWindow().setDecorFitsSystemWindows(true);
        setContentView(R.layout.activity_album_contents);

        long bucketId = getIntent().getLongExtra("bucket_id", -1);
        bucketName = getIntent().getStringExtra("bucket_name");
        MaterialToolbar toolbar = findViewById(R.id.topBar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(bucketName);
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

        int spacing = (int) (3 * getResources().getDisplayMetrics().density);
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

                android.view.MenuItem tagItem = toolbar.getMenu().add("TAG");
                tagItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                android.widget.TextView tagView = new android.widget.TextView(this);
                tagView.setText("TAG");
                tagView.setTextColor(android.graphics.Color.parseColor("#FFC107"));
                tagView.setTextSize(16);
                tagView.setTypeface(null, android.graphics.Typeface.BOLD);
                tagView.setPadding(8, 0, 8, 0);
                tagItem.setActionView(tagView);
                tagView.setOnClickListener(v -> showBatchTagDialog());

                toolbar.setOnMenuItemClickListener(item -> {
                    showBatchTagDialog();
                    return true;
                });

            } else {
                boolean isSearch = getIntent().getStringArrayListExtra("search_results") != null;
                toolbar.setTitle(isSearch ? "Results (" + images.size() + ")" : bucketName);
                toolbar.setNavigationIcon(null);
            }

        });

        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        if (searchUris != null) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Results (" + images.size() + ")");
            } else {
                toolbar.setTitle("Results (" + images.size() + ")");
            }
        }

        if (shareMode && searchUris != null && !searchUris.isEmpty()) {
            recyclerView.post(() -> {
                adapter.selectAll();
                showBatchTagDialog();
            });
        }



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
                collection, projection, selection, selectionArgs, sortOrder);

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

    private void loadSearchResults(ArrayList<String> uriStrings) {
        images.clear();
        for (String uriString : uriStrings) {
            Uri uri = Uri.parse(uriString);

            if (uri.getAuthority() != null && !uri.getAuthority().equals("media")) {
                long mediaId = MediaStoreUtil.getMediaStoreId(getApplicationContext(), uri);
                if (mediaId != -1) {
                    Uri resolved = android.content.ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId);
                    uri = resolved;
                    uriString = resolved.toString();
                }
            }

            ImageItem item = new ImageItem(0, uri);
            String uriLower = uriString.toLowerCase();
            if (uriString.contains(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())
                    || uriLower.endsWith(".mp4")
                    || uriLower.endsWith(".mov")
                    || uriLower.endsWith(".avi")
                    || uriLower.endsWith(".mkv")) {
                item.isVideo = true;
            }
            images.add(item);
        }


    }

    private void showBatchTagDialog() {

        android.view.View dialogView =
                getLayoutInflater().inflate(R.layout.dialog_batch_tag, null);

        AutoCompleteTextView tagInput = dialogView.findViewById(R.id.tagInput);

        final int[] rating = {0};

        android.view.View ratingRow = dialogView.findViewById(R.id.ratingRow);
        ratingRow.setOnClickListener(v -> {
            rating[0] = (rating[0] + 1) % 4;
            updateDialogStars(dialogView, rating[0]);
        });

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();
            List<String> keywords = dao.getAllKeywordNames();

            runOnUiThread(() -> {
                ArrayAdapter<String> adapter =
                        new ArrayAdapter<>(this,
                                R.layout.item_dropdown, keywords);
                tagInput.setAdapter(adapter);
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                tagInput.setDropDownHeight(screenHeight / 5);
            });
        }).start();

        androidx.appcompat.app.AlertDialog d =
                new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_KeyTag_Dialog)
                        .setView(dialogView)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Apply", (dialog, which) -> {
                            String keyword = tagInput.getText().toString().trim();
                            applyMetadataToSelected(keyword, rating[0]);
                        })
                        .create();

        d.show();

        d.getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                        android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        d.getWindow().setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT);
    }
    private void applyMetadataToSelected(String keyword, int rating) {

        final String normalized = keyword == null ? "" : keyword.trim().toLowerCase();

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            for (ImageItem item : images) {

                if (!item.isSelected) continue;

                String uriString = item.uri.toString();

                long mediaId = MediaStoreUtil.getMediaStoreId(
                        getApplicationContext(), Uri.parse(uriString));

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

                if (mediaId != -1) {
                    dao.updateQualityByMediaStoreId(mediaId, rating);
                } else {
                    dao.updateQuality(uriString, rating);
                }

                // Split on comma, store each keyword individually
                if (!normalized.isEmpty()) {
                    String[] parts = normalized.split(",");
                    for (String part : parts) {
                        String trimmed = part.trim();
                        if (trimmed.isEmpty()) continue;
                        KeywordEntity ke = dao.getKeywordByName(trimmed);
                        if (ke == null) {
                            dao.insertKeyword(new KeywordEntity(trimmed, 0));
                            ke = dao.getKeywordByName(trimmed);
                        }
                        if (ke != null) {
                            dao.insertCrossRef(new ImageKeywordCrossRef(image.id, ke.id));
                            dao.incrementUsage(ke.id);
                        }
                    }
                    embedKeywordsInImage(getApplicationContext(), Uri.parse(uriString), normalized);
                }

            } // end for loop

            runOnUiThread(() -> {
                adapter.clearSelection();
                android.widget.Toast.makeText(
                        this, "Metadata applied",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            });

        }).start();
    }
    private void updateDialogStars(android.view.View dialogView, int level) {

        ImageView star1 = dialogView.findViewById(R.id.dialogStar1);
        ImageView star2 = dialogView.findViewById(R.id.dialogStar2);
        ImageView star3 = dialogView.findViewById(R.id.dialogStar3);

        int filled = R.drawable.baseline_star_24;
        int empty  = R.drawable.baseline_star_border_24;
        int gold   = android.graphics.Color.parseColor("#FFC107");
        int white  = android.graphics.Color.WHITE;

        star1.setImageResource(level >= 1 ? filled : empty);
        star1.setColorFilter(level >= 1 ? gold : white);
        star2.setImageResource(level >= 2 ? filled : empty);
        star2.setColorFilter(level >= 2 ? gold : white);
        star3.setImageResource(level >= 3 ? filled : empty);
        star3.setColorFilter(level >= 3 ? gold : white);
    }
    private void embedKeywordsInImage(android.content.Context context, Uri imageUri, String keyword) {
        try {

            String filePath = null;
            android.database.Cursor cursor = context.getContentResolver().query(
                    imageUri,
                    new String[]{ MediaStore.Images.Media.DATA },
                    null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) filePath = cursor.getString(0);
                cursor.close();
            }
            if (filePath == null) return;

            java.io.File imageFile = new java.io.File(filePath);
            byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());

            boolean isJpeg = imageBytes.length >= 2 && (imageBytes[0] & 0xFF) == 0xFF && (imageBytes[1] & 0xFF) == 0xD8;
            boolean isPng = imageBytes.length >= 4 && (imageBytes[0] & 0xFF) == 0x89 && (imageBytes[1] & 0xFF) == 0x50;

            if (!isJpeg && !isPng) {
                return;
            }

            if (isPng) {
                return;
            }

            byte[] xmpNs = "http://ns.adobe.com/xap/1.0/\0".getBytes("UTF-8");

            List<String> existingKeywords = new ArrayList<>();
            java.io.ByteArrayOutputStream cleanStream = new java.io.ByteArrayOutputStream();

            // Write SOI
            cleanStream.write(imageBytes, 0, 2);
            int i = 2;

            while (i + 3 < imageBytes.length) {
                // Skip padding bytes
                while (i < imageBytes.length && (imageBytes[i] & 0xFF) == 0xFF) i++;
                if (i >= imageBytes.length) break;

                int marker = imageBytes[i] & 0xFF;
                i--; // back up to FF

                // SOS or EOI — write everything remaining and stop
                if (marker == 0xDA || marker == 0xD9) {
                    cleanStream.write(imageBytes, i, imageBytes.length - i);
                    break;
                }

                // Read segment length
                int segLen = ((imageBytes[i+2] & 0xFF) << 8) | (imageBytes[i+3] & 0xFF);
                int segTotal = 2 + segLen; // marker(2) not included in segLen
                int segEnd = i + segTotal;

                if (segEnd > imageBytes.length) {
                    // Malformed segment — write rest and bail
                    cleanStream.write(imageBytes, i, imageBytes.length - i);
                    break;
                }

                // Check if APP1 XMP
                boolean isXmpApp1 = false;
                if (marker == 0xE1 && segLen > xmpNs.length + 2) {
                    isXmpApp1 = true;
                    for (int j = 0; j < xmpNs.length; j++) {
                        if (i + 4 + j >= imageBytes.length || imageBytes[i + 4 + j] != xmpNs[j]) {
                            isXmpApp1 = false;
                            break;
                        }
                    }
                }

                if (isXmpApp1) {
                    // Extract existing keywords
                    int dataStart = i + 4 + xmpNs.length;
                    if (dataStart < segEnd) {
                        String segStr = new String(imageBytes, dataStart, segEnd - dataStart, "UTF-8");
                        java.util.regex.Matcher m = java.util.regex.Pattern
                                .compile("<rdf:li>(.+?)</rdf:li>")
                                .matcher(segStr);
                        while (m.find()) {
                            String kw = m.group(1).trim();
                            if (!existingKeywords.contains(kw)) existingKeywords.add(kw);
                        }
                    }
                    // Skip — don't copy to clean stream
                } else {
                    cleanStream.write(imageBytes, i, segTotal);
                }

                i = segEnd;
            }

            byte[] cleanBytes = cleanStream.toByteArray();

            // Add new keyword
            if (!existingKeywords.contains(keyword)) existingKeywords.add(keyword);

            // Build XMP
            StringBuilder items = new StringBuilder();
            for (String kw : existingKeywords) {
                items.append("<rdf:li>").append(kw).append("</rdf:li>");
            }

            String xmpStr =
                    "<?xpacket begin='\uFEFF' id='W5M0MpCehiHzreSzNTczkc9d'?>" +
                            "<x:xmpmeta xmlns:x='adobe:ns:meta/'>" +
                            "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>" +
                            "<rdf:Description rdf:about='' xmlns:dc='http://purl.org/dc/elements/1.1/'>" +
                            "<dc:subject><rdf:Bag>" + items + "</rdf:Bag></dc:subject>" +
                            "</rdf:Description></rdf:RDF></x:xmpmeta>" +
                            "<?xpacket end='w'?>";

            byte[] xmpData = xmpStr.getBytes("UTF-8");
            int segmentLength = 2 + xmpNs.length + xmpData.length;

            java.io.ByteArrayOutputStream xmpSegment = new java.io.ByteArrayOutputStream();
            xmpSegment.write(0xFF);
            xmpSegment.write(0xE1);
            xmpSegment.write((segmentLength >> 8) & 0xFF);
            xmpSegment.write(segmentLength & 0xFF);
            xmpSegment.write(xmpNs);
            xmpSegment.write(xmpData);

            // Write: SOI(2) + XMP segment + rest of clean bytes after SOI
            java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile);
            fos.write(cleanBytes, 0, 2);
            fos.write(xmpSegment.toByteArray());
            fos.write(cleanBytes, 2, cleanBytes.length - 2);
            fos.close();

            // Verify
            byte[] verify = java.nio.file.Files.readAllBytes(imageFile.toPath());
            boolean found = new String(verify, "UTF-8").contains(keyword);

        } catch (Exception e) {
        }
    }
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_album_contents, menu);
        boolean isSearchResults = getIntent().getStringArrayListExtra("search_results") != null;
        menu.findItem(R.id.action_tag_all).setVisible(!isSearchResults);
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
        if (item.getItemId() == R.id.action_tag_all) {
            showTagAllDialog();
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

        AutoCompleteTextView searchInput = dialogView.findViewById(R.id.searchInput);

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();
            java.util.List<String> keywords = dao.getAllKeywordNames();

            runOnUiThread(() -> {
                ArrayAdapter<String> kwAdapter =
                        new ArrayAdapter<>(this,
                                R.layout.item_dropdown, keywords);
                searchInput.setAdapter(kwAdapter);
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                searchInput.setDropDownHeight(screenHeight / 5);
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
                            this, "No results found",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    return;
                }
                Intent intent = new Intent(this, AlbumContentsActivity.class);
                intent.putStringArrayListExtra(
                        "search_results", new ArrayList<>(uris));
                intent.putExtra("bucket_name", "Results");
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
                        "search_results", new ArrayList<>(results));
                intent.putExtra("bucket_name", "Search Results");
                startActivity(intent);
            });
        }).start();
    }

    private void showTagAllDialog() {
        String suggestedKeyword = bucketName != null
                ? bucketName.trim().toLowerCase() : "";

        android.view.View dialogView =
                getLayoutInflater().inflate(R.layout.dialog_batch_tag, null);

        AutoCompleteTextView tagInput = dialogView.findViewById(R.id.tagInput);
        tagInput.setText(suggestedKeyword);

        final int[] rating = {0};
        android.view.View ratingRow = dialogView.findViewById(R.id.ratingRow);
        ratingRow.setOnClickListener(v -> {
            rating[0] = (rating[0] + 1) % 4;
            updateDialogStars(dialogView, rating[0]);
        });

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();
            List<String> keywords = dao.getAllKeywordNames();
            runOnUiThread(() -> {
                ArrayAdapter<String> adapter =
                        new ArrayAdapter<>(this, R.layout.item_dropdown, keywords);
                tagInput.setAdapter(adapter);
            });
        }).start();

        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_KeyTag_Dialog)
                .setTitle("Tag all images in this album")
                .setMessage("Keyword will be applied to all " + images.size() + " images.")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    String keyword = tagInput.getText().toString().trim();
                    if (!keyword.isEmpty()) {
                        selectAllAndTag(keyword, rating[0]);
                    }
                })
                .show();
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



    private void selectAllAndTag(String keyword, int rating) {
        for (ImageItem item : images) {
            if (!item.isHeader) item.isSelected = true;
        }
        applyMetadataToSelected(keyword, rating);
    }

}