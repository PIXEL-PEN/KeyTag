package pixelpen.keytag;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;  // ← add this
import android.provider.MediaStore;
import android.database.Cursor;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ShareEntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<String> uriStrings = new ArrayList<>();

        Intent intent = getIntent();
        if (intent != null) {

            String action = intent.getAction();
            String type = intent.getType();

            if (type != null && type.startsWith("image/")) {

                if (Intent.ACTION_SEND.equals(action)) {

                    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (uri != null) {
                        uriStrings.add(resolveToMediaStoreUri(uri));
                    }

                } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {

                    ArrayList<Uri> uris =
                            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

                    if (uris != null) {
                        for (Uri uri : uris) {
                            uriStrings.add(resolveToMediaStoreUri(uri));
                        }
                    }
                }
            }
        }

        if (!uriStrings.isEmpty()) {

            Intent launch = new Intent(this, AlbumContentsActivity.class);
            launch.putStringArrayListExtra("search_results", uriStrings);
            launch.putExtra("share_mode", true);
            startActivity(launch);
        }

        finish();
    }

    private String resolveToMediaStoreUri(Uri uri) {
        Log.d("KEYTAG_URI", "=== resolveToMediaStoreUri ===");
        Log.d("KEYTAG_URI", "Input URI: " + uri.toString());
        Log.d("KEYTAG_URI", "Authority: " + uri.getAuthority());
        Log.d("KEYTAG_URI", "LastPathSegment: " + uri.getLastPathSegment());

        try {
            if ("media".equals(uri.getAuthority())) {
                Log.d("KEYTAG_URI", "Result: already MediaStore URI, returning as-is");
                return uri.toString();
            }

            String name = uri.getLastPathSegment();
            Log.d("KEYTAG_URI", "Querying DISPLAY_NAME = " + name);

            if (name != null) {
                Cursor cursor = getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Images.Media._ID},
                        MediaStore.Images.Media.DISPLAY_NAME + "=?",
                        new String[]{name},
                        null
                );

                if (cursor != null) {
                    try {
                        Log.d("KEYTAG_URI", "Cursor count: " + cursor.getCount());
                        if (cursor.moveToFirst()) {
                            long id = cursor.getLong(0);
                            Uri mediaUri = Uri.withAppendedPath(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    String.valueOf(id)
                            );
                            Log.d("KEYTAG_URI", "Result: resolved to " + mediaUri);
                            return mediaUri.toString();
                        } else {
                            Log.d("KEYTAG_URI", "Result: cursor empty, falling back to raw URI");
                        }
                    } finally {
                        cursor.close();
                    }
                } else {
                    Log.d("KEYTAG_URI", "Result: cursor null, falling back to raw URI");
                }
            }

        } catch (Exception e) {
            Log.d("KEYTAG_URI", "Exception: " + e.getMessage());
        }

        Log.d("KEYTAG_URI", "Fallback URI returned: " + uri.toString());
        return uri.toString();
    }
}