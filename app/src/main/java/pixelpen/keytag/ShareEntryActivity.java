package pixelpen.keytag;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

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
                        uriStrings.add(uri.toString());
                    }

                } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {

                    ArrayList<Uri> uris =
                            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

                    if (uris != null) {
                        for (Uri uri : uris) {
                            uriStrings.add(uri.toString());
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
}