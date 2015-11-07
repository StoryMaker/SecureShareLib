package io.scal.secureshareui.login;

import timber.log.Timber;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.web.WebkitProxy;
import io.scal.secureshareui.lib.Util;

/**
 * Created by mnbogner on 3/26/15.
 */
public class ZTWebActivity extends LockableActivity {

    private static final String TAG = "ZTWebActivity";

    private WebView mWebview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        String authorizationUrl = null;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            authorizationUrl = extras.getString("authorizationUrl");
        }

        if ((authorizationUrl == null) || (authorizationUrl.length() == 0)) {
            Timber.e("NO AUTHORIZATION URL FOUND");
            finish();
            return;
        }

        // check for tor settings and set proxy
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean useTor = settings.getBoolean("pusetor", false);
        mWebview = new WebView(this);

        if (useTor) {
            Timber.d("user selected \"use tor\"");

            if ((!OrbotHelper.isOrbotInstalled(getApplicationContext())) || (!OrbotHelper.isOrbotRunning(getApplicationContext()))) {
                Timber.e("user selected \"use tor\" but orbot is not installed or not running");
                finish();
                return;
            } else {
                try {
                    WebkitProxy.setProxy("android.app.Application", getApplicationContext(), mWebview, Util.ORBOT_HOST, Util.ORBOT_HTTP_PORT);
                } catch (Exception e) {
                    Timber.e("user selected \"use tor\" but an exception was thrown while setting the proxy: " + e.getLocalizedMessage());
                    finish();
                    return;
                }
            }
        } else {
            Timber.d("user selected \"don't use tor\"");
        }

        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.setVisibility(View.VISIBLE);

        setContentView(mWebview);

        mWebview.setWebViewClient(new WebViewClient() {
            Intent resultIntent = new Intent();

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Timber.d("LOADING URL: " + url);

                // also break on http://www.stichtingrz.co/wp-login.php?checkemail=registered (for signups) ?

                if (url.contains("oauth_token=") && url.contains("oauth_verifier=")) {

                    Timber.d("GOT TOKEN/VERIFIER");

                    Uri uri = Uri.parse(url);
                    String token = uri.getQueryParameter("oauth_token");
                    String verifier = uri.getQueryParameter("oauth_verifier");
                    resultIntent.putExtra("token", token);
                    resultIntent.putExtra("verifier", verifier);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                    return true;
                } else {

                    Timber.d("NO TOKEN/VERIFIER");

                    return super.shouldOverrideUrlLoading(view, url);
                }
            }
        });

        mWebview.loadUrl(authorizationUrl);
    }

    @Override
    public void finish() {
        super.finish();
        Util.clearWebviewAndCookies(mWebview, this);
    }
}
