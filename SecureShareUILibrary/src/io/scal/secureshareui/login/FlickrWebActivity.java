package io.scal.secureshareui.login;

import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.onionkit.web.WebkitProxy;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class FlickrWebActivity extends Activity {
    // TOR PROXY SETTINGS
    private static final String ORBOT_HOST = "127.0.0.1";
    private static final int ORBOT_HTTP_PORT = 8118;
    
    private static final String TAG = "FlickrWebActivity";
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate()");
                
        String url = null;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            url = extras.getString("authURL");
        }
        
        if ((url == null) || (url.length() == 0)) {
            Log.e(TAG, "no url found");
            return;
        }
        
        WebView view = new WebView(this);
        
        OrbotHelper orbotHelper = new OrbotHelper(getApplicationContext());

        if(orbotHelper.isOrbotRunning()) {    
            Log.d(TAG, "orbot running, setting proxy");
  
            try {
                WebkitProxy.setProxy("android.app.Application", getApplicationContext() , ORBOT_HOST, ORBOT_HTTP_PORT);
            } catch (Exception e) {
                Log.e(TAG, "exception while setting proxy: " + e.getMessage());
            }            
        }
        else {
          Log.d(TAG, "orbot not running, proxy not set");
        }
       
        view.getSettings().setJavaScriptEnabled(true);
        view.setVisibility(View.VISIBLE);
        setContentView(view);
        
        view.setWebViewClient(new WebViewClient() {
            Intent resultIntent = new Intent();

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("oauth_token=") && url.contains("oauth_verifier=")) {
                    Uri uri = Uri.parse(url);
                    String token = uri.getQueryParameter("oauth_token");
                    String verifier = uri.getQueryParameter("oauth_verifier");
                    resultIntent.putExtra("token", token);
                    resultIntent.putExtra("verifier", verifier);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                    return true;
                } else {
                    return super.shouldOverrideUrlLoading(view, url);
                }
            }
        });
        
        view.loadUrl(url);
    }
}