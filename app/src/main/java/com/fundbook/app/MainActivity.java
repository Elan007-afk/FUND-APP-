package com.fundbook.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Locale;

public class MainActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);

        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        Reminder.schedule(this);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.__back && window.__back()", value -> {
            if (!"true".equals(value)) {
                finish();
            }
        });
    }

    /** Methods callable from the web UI as window.Android.xxx() */
    private class Bridge {

        @JavascriptInterface
        public void sync(int pendingCount, double pendingAmount) {
            Reminder.prefs(MainActivity.this).edit()
                    .putInt("pc", pendingCount)
                    .putFloat("pa", (float) pendingAmount)
                    .apply();
        }

        @JavascriptInterface
        public String reminderTime() {
            SharedPreferences p = Reminder.prefs(MainActivity.this);
            return String.format(Locale.US, "%02d:%02d", p.getInt("h", 9), p.getInt("m", 0));
        }

        @JavascriptInterface
        public void setReminderTime(int hour, int minute) {
            Reminder.prefs(MainActivity.this).edit()
                    .putInt("h", hour)
                    .putInt("m", minute)
                    .apply();
            Reminder.schedule(MainActivity.this);
        }

        @JavascriptInterface
        public void testNotification() {
            Reminder.show(MainActivity.this);
        }

        @JavascriptInterface
        public void share(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(i, "Share"));
                }
            });
        }
    }
}
