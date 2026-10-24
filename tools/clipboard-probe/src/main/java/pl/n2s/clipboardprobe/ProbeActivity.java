package pl.n2s.clipboardprobe;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.text.InputType;
import android.view.WindowInsets;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Separate UID test fixture for real clipboard IPC, URI grants and accessible fields. */
public class ProbeActivity extends Activity {
    private TextView result;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(24+bars.left, 16+bars.top, 24+bars.right, 16+bars.bottom);
            return insets;
        });
        TextView title = new TextView(this);
        title.setText("n2s — niezależny test schowka i dostępności");
        title.setTextSize(22); title.setTextColor(Color.WHITE); root.addView(title);
        EditText field = new EditText(this);
        field.setText("N2S_FIELD_2468"); field.setTextSize(20); field.setTextColor(Color.WHITE);
        field.setSingleLine(true); root.addView(field);
        // Positive capture fixture has no sensitive fields. --ez password true enables
        // a separate, deliberately protected negative case for Android 16 and later.
        if (getIntent().getBooleanExtra("password", false)) {
            EditText password = new EditText(this);
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            password.setText("UKRYTE_POLE_TESTOWE"); password.setTextColor(Color.WHITE); root.addView(password);
        }
        Button check = new Button(this); check.setText("Sprawdź schowek");
        check.setOnClickListener(v -> inspect()); root.addView(check);
        result = new TextView(this); result.setTextColor(Color.WHITE); result.setTextSize(16); root.addView(result);
        setContentView(root); root.requestApplyInsets();
    }
    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (focus) new Handler(Looper.getMainLooper()).postDelayed(this::inspect, 750);
    }
    private void inspect() {
        String report;
        try {
            ClipboardManager cm = getSystemService(ClipboardManager.class);
            ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) throw new IllegalStateException("Brak schowka");
            ClipDescription desc = clip.getDescription();
            if (clip.getItemAt(0).getUri() != null) {
                android.net.Uri uri = clip.getItemAt(0).getUri();
                if (!desc.hasMimeType("image/png")) throw new IllegalStateException("MIME nie jest image/png");
                try (InputStream stream = getContentResolver().openInputStream(uri)) {
                    Bitmap image = BitmapFactory.decodeStream(stream);
                    if (image == null) throw new IllegalStateException("Nie można odczytać PNG");
                    report = "IMAGE_OK " + image.getWidth() + "x" + image.getHeight() + " mime=image/png uri=" + uri;
                    image.recycle();
                }
            } else {
                report = "TEXT_OK " + clip.getItemAt(0).coerceToText(this);
            }
        } catch (Exception e) { report = "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage(); }
        result.setText(report);
        try (FileOutputStream out = openFileOutput("clipboard-result.txt", MODE_PRIVATE)) {
            out.write(report.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
}
