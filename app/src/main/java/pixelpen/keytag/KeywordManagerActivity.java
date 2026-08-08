package pixelpen.keytag;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pixelpen.keytag.db.AppDatabase;
import pixelpen.keytag.db.KeywordEntity;
import pixelpen.keytag.db.TaggingDao;
import pixelpen.keytag.db.ImageKeywordCrossRef;

public class KeywordManagerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<KeywordEntity> keywords = new ArrayList<>();
    private KeywordAdapter adapter;
    private boolean sortByCount = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setDecorFitsSystemWindows(true);
        setContentView(R.layout.activity_keyword_manager);

        MaterialToolbar toolbar = findViewById(R.id.topBar);
        toolbar.setTitle("Keywords");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new KeywordAdapter(keywords, this::showKeywordOptions);
        recyclerView.setAdapter(adapter);

        loadKeywords();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_keyword_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {




        if (item.getItemId() == R.id.action_sort_keywords) {
            sortByCount = !sortByCount;
            item.setTitle(sortByCount ? "Sort A-Z" : "Sort by Count");
            sortAndDisplay();
            return true;
        }
        return super.onOptionsItemSelected(item);



    }

    private void loadKeywords() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();
            List<KeywordEntity> result = dao.getAllKeywords();
            for (KeywordEntity kw : result) {
                kw.usageCount = dao.getImageCountForKeyword(kw.id);
            }
            runOnUiThread(() -> {
                keywords.clear();
                keywords.addAll(result);
                sortAndDisplay();
            });
        }).start();
    }

    private void sortAndDisplay() {
        if (sortByCount) {
            Collections.sort(keywords,
                    (a, b) -> Integer.compare(b.usageCount, a.usageCount));
        } else {
            Collections.sort(keywords,
                    (a, b) -> a.name.compareToIgnoreCase(b.name));
        }
        adapter.notifyDataSetChanged();
    }

    private void showKeywordOptions(KeywordEntity keyword) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(keyword.name)
                .setItems(new String[]{"Rename", "Delete"}, (dialog, which) -> {
                    if (which == 0) showRenameDialog(keyword);
                    else showDeleteDialog(keyword);
                })
                .show();
    }

    private void showRenameDialog(KeywordEntity keyword) {
        EditText input = new EditText(this);
        input.setText(keyword.name);
        input.setTextColor(android.graphics.Color.WHITE);
        input.setSelection(keyword.name.length());

        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_KeyTag_Dialog)
                .setTitle("Rename keyword")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText().toString().trim().toLowerCase();
                    if (!newName.isEmpty() && !newName.equals(keyword.name)) {
                        new Thread(() -> {
                            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                            db.taggingDao().renameKeyword(keyword.id, newName);
                            runOnUiThread(this::loadKeywords);
                        }).start();
                    }
                })
                .show();
    }

    private void showDeleteDialog(KeywordEntity keyword) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete keyword?")
                .setMessage("\"" + keyword.name + "\" will be removed from all " +
                        keyword.usageCount + " images. This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(() -> {
                        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                        TaggingDao dao = db.taggingDao();
                        dao.deleteAllCrossRefsForKeyword(keyword.id);
                        dao.deleteKeywordById(keyword.id);
                        runOnUiThread(this::loadKeywords);
                    }).start();
                })
                .show();
    }

    private void showFixLegacyDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Fix Legacy Keywords")
                .setMessage("This will split all comma-separated keywords into individual searchable keywords. Run once. Cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Fix Now", (dialog, which) -> fixLegacyKeywords())
                .show();
    }

    private void fixLegacyKeywords() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();

            List<KeywordEntity> allKeywords = dao.getAllKeywords();

            for (KeywordEntity kw : allKeywords) {
                if (!kw.name.contains(",")) continue;

                List<Long> imageIds = dao.getImageIdsForKeyword(kw.id);

                String[] parts = kw.name.split(",");
                List<String> newKeywords = new ArrayList<>();
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) newKeywords.add(trimmed);
                }

                for (long imageId : imageIds) {
                    for (String newKw : newKeywords) {
                        KeywordEntity existing = dao.getKeywordByName(newKw);
                        if (existing == null) {
                            dao.insertKeyword(new KeywordEntity(newKw, 0));
                            existing = dao.getKeywordByName(newKw);
                        }
                        if (existing != null) {
                            dao.insertCrossRef(
                                    new ImageKeywordCrossRef(imageId, existing.id));
                            dao.incrementUsage(existing.id);
                        }
                    }
                    dao.removeCrossRef(imageId, kw.id);
                }

                dao.deleteKeywordById(kw.id);
            }

            runOnUiThread(() -> {
                android.widget.Toast.makeText(this,
                        "Legacy keywords fixed",
                        android.widget.Toast.LENGTH_SHORT).show();
                loadKeywords();
            });
        }).start();
    }

    static class KeywordAdapter extends RecyclerView.Adapter<KeywordAdapter.VH> {

        interface OnKeywordClick { void onClick(KeywordEntity keyword); }

        private final List<KeywordEntity> keywords;
        private final OnKeywordClick listener;

        KeywordAdapter(List<KeywordEntity> keywords, OnKeywordClick listener) {
            this.keywords = keywords;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_keyword, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            KeywordEntity kw = keywords.get(position);
            holder.textKeyword.setText(kw.name);
            holder.textCount.setText(kw.usageCount + " images");
            holder.itemView.setOnClickListener(v -> listener.onClick(kw));
        }

        @Override
        public int getItemCount() { return keywords.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView textKeyword, textCount;
            VH(View v) {
                super(v);
                textKeyword = v.findViewById(R.id.textKeyword);
                textCount = v.findViewById(R.id.textCount);
            }
        }
    }}