package com.procsec.fast;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.procsec.fast.common.ThemeManager;
import com.procsec.fast.util.Constants;
import com.procsec.fast.vkapi.Auth;

import ru.lischenko_dev.fastmessenger.R;

public class LoginActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar bar;

    private Toolbar tb;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle("Login");

        tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);

        ThemeManager.applyToolbarStyles(tb);

        bar = findViewById(R.id.progress);
        webView = findViewById(R.id.web);

        webView.setVisibility(View.GONE);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.clearCache(true);
        webView.setWebViewClient(new VKWebViewClient());
        CookieSyncManager.createInstance(this);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        String url = Auth.getUrl(Constants.API_ID, Auth.getSettings());
        webView.loadUrl(url);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem ref = menu.add("Restart");
        Drawable d = getResources().getDrawable(R.drawable.ic_refresh);
        d.setTint(ThemeManager.color);
        ref.setIcon(d);
        ref.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        ref.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem p1) {
                recreate();
                return false;
            }

        });

        return true;
    }

    private class VKWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            bar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            parseUrl(url);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack())
            webView.goBack();
        else
            finish();
    }


    private void parseUrl(String url) {
        try {
            if (url == null)
                return;
            if (url.startsWith(Auth.redirect_url)) {
                if (!url.contains("error=")) {
                    String[] auth = Auth.parseRedirectUrl(url);
                    Intent intent = new Intent();
                    intent.putExtra("token", auth[0]);
                    intent.putExtra("id", Integer.parseInt(auth[1]));
                    setResult(Activity.RESULT_OK, intent);
                }
                finish();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
