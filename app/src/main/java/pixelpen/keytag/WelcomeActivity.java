package pixelpen.keytag;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs =
                getSharedPreferences("keytag_prefs", MODE_PRIVATE);

        if (prefs.getBoolean("welcome_shown", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        findViewById(R.id.btnGetStarted).setOnClickListener(v -> {
            prefs.edit().putBoolean("welcome_shown", true).apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}