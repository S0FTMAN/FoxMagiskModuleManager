package com.fox2code.mmm.markdown;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.fox2code.mmm.Constants;
import com.fox2code.mmm.MainApplication;
import com.fox2code.mmm.R;
import com.fox2code.mmm.compat.CompatActivity;
import com.fox2code.mmm.utils.Http;
import com.fox2code.mmm.utils.IntentHelper;

import java.nio.charset.StandardCharsets;


public class MarkdownActivity extends CompatActivity {
    private static final String TAG = "MarkdownActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setDisplayHomeAsUpEnabled(true);
        Intent intent = this.getIntent();
        if (intent == null || !MainApplication.checkSecret(intent)) {
            Log.e(TAG, "Impersonation detected!");
            this.onBackPressed();
            return;
        }
        String url = intent.getExtras()
                .getString(Constants.EXTRA_MARKDOWN_URL);
        String title = intent.getExtras()
                .getString(Constants.EXTRA_MARKDOWN_TITLE);
        String config = intent.getExtras()
                .getString(Constants.EXTRA_MARKDOWN_CONFIG);
        if (title != null && !title.isEmpty()) setTitle(title);
        if (config != null && !config.isEmpty()) {
            String configPkg = IntentHelper.getPackageOfConfig(config);
            try {
                this.getPackageManager().getPackageInfo(configPkg, 0);
                this.setActionBarExtraMenuButton(R.drawable.ic_baseline_app_settings_alt_24, menu -> {
                    IntentHelper.openConfig(this, config);
                    return true;
                });
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Config package \"" +
                        configPkg + "\" missing for markdown view");
            }
        }
        Log.i(TAG, "Url for markdown " + url);
        setContentView(R.layout.markdown_view);
        ViewGroup markdownBackground = findViewById(R.id.markdownBackground);
        TextView textView = findViewById(R.id.markdownView);
        new Thread(() -> {
            try {
                String markdown = new String(Http.doHttpGet(url, true), StandardCharsets.UTF_8);
                Log.i(TAG, "Download successful");
                runOnUiThread(() -> {
                    MainApplication.getINSTANCE().getMarkwon().setMarkdown(textView, markdown);
                    if (markdownBackground != null) {
                        markdownBackground.setClickable(true);
                        markdownBackground.setOnClickListener(v -> this.onBackPressed());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed download", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.failed_download,
                            Toast.LENGTH_SHORT).show();
                    this.onBackPressed();
                });
            }
        }, "Markdown load thread").start();
    }
}
