package pixelpen.keytag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;

import pixelpen.keytag.db.AppDatabase;
import pixelpen.keytag.db.TaggingDao;

public class WidgetSearchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_widget_search);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        EditText searchInput = findViewById(R.id.widgetSearchField);
        ImageButton searchBtn = findViewById(R.id.widgetSearchGo);

        searchBtn.setOnClickListener(v -> performSearch(
                searchInput.getText().toString().trim()));

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            performSearch(searchInput.getText().toString().trim());
            return true;
        });
    }

    private void performSearch(String keyword) {
        if (keyword.isEmpty()) return;

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            TaggingDao dao = db.taggingDao();
            List<String> uris = dao.getImageUrisForKeyword(keyword.toLowerCase());

            runOnUiThread(() -> {
                if (uris.isEmpty()) {
                    android.widget.Toast.makeText(
                            this, "No results for \"" + keyword + "\"",
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(this, AlbumContentsActivity.class);
                intent.putStringArrayListExtra(
                        "search_results", new ArrayList<>(uris));
                intent.putExtra("bucket_name", "Results: " + keyword);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }).start();
    }
}