package pixelpen.keytag;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import android.provider.MediaStore;
import android.database.Cursor;


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

        // First try direct _ID query (works for real MediaStore URIs)
        String[] projection = {MediaStore.Images.Media._ID};

        try (Cursor cursor = getContentResolver().query(
                uri,
                projection,
                null,
                null,
                null)) {

            if (cursor != null && cursor.moveToFirst()
                    && cursor.getColumnCount() > 0) {

                long id = cursor.getLong(0);

                Uri mediaUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        String.valueOf(id)
                );

                return mediaUri.toString();
            }
        } catch (Exception ignored) {
        }

        // If that fails, resolve via file path
        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[]{MediaStore.Images.Media.DATA},
                null,
                null,
                null)) {

            if (cursor != null && cursor.moveToFirst()) {

                String path = cursor.getString(0);

                try (Cursor mediaCursor = getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Images.Media._ID},
                        MediaStore.Images.Media.DATA + "=?",
                        new String[]{path},
                        null)) {

                    if (mediaCursor != null && mediaCursor.moveToFirst()) {

                        long id = mediaCursor.getLong(0);

                        Uri mediaUri = Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                String.valueOf(id)
                        );

                        return mediaUri.toString();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return uri.toString(); // fallback
    }
}