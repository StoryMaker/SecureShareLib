package io.scal.secureshareui.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.flickr.api.FlickrException;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.ZTApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuth10aServiceImpl;
import org.scribe.oauth.OAuthService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import info.guardianproject.onionkit.trust.StrongHttpsClient;
import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.onionkit.web.WebkitProxy;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.lib.Util;
import io.scal.secureshareui.lib.ZTWrapper;
import io.scal.secureshareuilibrary.R;

/**
 * Created by mnbogner on 3/25/15.
 */
public class ZTLoginActivity extends Activity {

    private static final String TAG = "ZTLoginActivity";
    private static final int CODE = 0; // NEED A REAL VALUE?

    private String mClientId;
    private String mClientSecret;

    private Token mRequestToken = null;
    private Verifier mAccessVerifier = null;
    private Token mAccessToken = null;

    private OAuthService service;

    private StrongHttpsClient mClient;

    private int mAccessResult = RESULT_CANCELED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClientId = getString(R.string.zt_key);
        mClientSecret = getString(R.string.zt_secret);

        service = new ServiceBuilder()
                .provider(ZTApi.class)
                .apiKey(mClientId)
                .apiSecret(mClientSecret)
                .build();

        GetAuthorizationUrlTask gauTask = new GetAuthorizationUrlTask(this);
        gauTask.execute();
    }

    class GetAuthorizationUrlTask extends AsyncTask<String, String, String> {

        private ZTLoginActivity ztla;

        public GetAuthorizationUrlTask(ZTLoginActivity ztla) {
            this.ztla = ztla;
        }

        @Override
        protected String doInBackground(String... params) {
            String authorizationUrl = ztla.getAuthorizationUrl();
            return authorizationUrl;
        }

        protected void onPostExecute(String result) {
            ztla.startZTWebActivity(result);
        }
    }

    public String getAuthorizationUrl() {

        Log.d("ZT OAUTH", "GETTING REQUEST TOKEN...");

        Map<String, String> parameters = service.getRequestParameters();

        StrongHttpsClient client = getHttpClientInstance();

        HttpPost post = new HttpPost("http://www.stichtingrz.co/oauth1/request"); // TODO: move to values/preferences

        List<NameValuePair> params = new ArrayList<NameValuePair>();

        for (String key : parameters.keySet()) {
            params.add(new BasicNameValuePair(key, parameters.get(key)));
            Log.d("ZT OAUTH", "ADDING PARAMETER: " + key + ": " + parameters.get(key));
        }

        try {
            post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            Log.d("ZT OAUTH", "FAILED TO ENCODE ENTITY");
            uee.printStackTrace();
            return null;
        }

        HttpResponse response = null;

        try {
            response = client.execute(post);
        } catch (ClientProtocolException cpe) {
            Log.d("ZT OAUTH", "FAILED TO EXECUTE REQUEST (CPE)");
            cpe.printStackTrace();
            return null;
        } catch (IOException ioe) {
            Log.d("ZT OAUTH", "FAILED TO EXECUTE REQUEST (IOE)");
            ioe.printStackTrace();
            return null;
        }

        Log.d("ZT OAUTH", "RESPONSE CODE: " + response.getStatusLine().getStatusCode());

        StringBuffer result = new StringBuffer();

        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException ioe) {
            Log.d("ZT OAUTH", "FAILED TO READ RESPONSE");
            ioe.printStackTrace();
            return null;
        }

        Header[] postHeaders = response.getAllHeaders();

        for (int i = 0; i < postHeaders.length; i++) {
            Log.d("ZT OAUTH", "FOUND HEADER: " + postHeaders[i].getName() + ": " + postHeaders[i].getValue());
        }

        Log.d("ZT OAUTH", "RESPONSE: " + result.toString());

        if (result.toString().contains("oauth_token=") && result.toString().contains("oauth_token_secret=")) {
            String[] responseParts = result.toString().split("&");

            String oauthToken = "";
            String oauthTokenSecret = "";

            for (String responsePart : responseParts) {
                if (responsePart.contains("oauth_token=")) {
                    oauthToken = responsePart.substring(responsePart.indexOf("=") + 1);
                }
                if (responsePart.contains("oauth_token_secret=")) {
                    oauthTokenSecret = responsePart.substring(responsePart.indexOf("=") + 1);
                }
            }

            mRequestToken = new Token(oauthToken, oauthTokenSecret, result.toString());

            Log.d("ZT OAUTH", "GOT REQUEST TOKEN: " + mRequestToken.getToken());

            Log.d("ZT OAUTH", "GETTING AUTH URL...");

            String authorizationUrl = service.getAuthorizationUrl(mRequestToken);

            try {
                authorizationUrl = authorizationUrl + "&oauth_callback=" + URLEncoder.encode("https://storymaker.org/home/", "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                Log.e("ZT OAUTH", "ENCODING CALLBACK FAILED");
                uee.printStackTrace();
            }

            Log.d("ZT OAUTH", "GOT AUTH URL: " + authorizationUrl);

            return authorizationUrl;

        } else {
            Log.e("ZT OAUTH", "TOKEN ELEMENTS MISSING FROM RESPONSE");
        }

        return null;
    }

    public void startZTWebActivity(String authorizationUrl) {

        Log.e("ZT OAUTH", "STARTING WEB ACTIVITY FOR " + authorizationUrl);

        Intent i = new Intent(this, ZTWebActivity.class);
        i.putExtra("authorizationUrl", authorizationUrl);
        startActivityForResult(i, CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("ZT OAUTH", "GOT ACTIVITY RESULT");

        if (requestCode == CODE) {

            if (resultCode == Activity.RESULT_OK) {

                Bundle results = data.getExtras();

                mAccessVerifier = new Verifier(results.getString("verifier"));

                if ((mRequestToken != null) && (mAccessVerifier != null)) {

                    Log.d("ZT OAUTH", "GOT TOKEN " + mRequestToken.getToken() + ", GOT VERIFIER " + mAccessVerifier.getValue());

                    VerifyTokenTask vtTask = new VerifyTokenTask(this);
                    vtTask.execute();
                } else {

                    Log.e("ZT OAUTH", "MISSING TOKEN"); // AND/OR VERIFIER");

                }
            } else if (resultCode == Activity.RESULT_CANCELED) {

                Log.e("ZT OAUTH", "ACTIVITY CANCELLED");

            } else {

                Log.e("ZT OAUTH", "UNEXPECTED RESULT");

            }
        } else {

            Log.e("ZT OAUTH", "UNEXPECTED REQUEST");

        }
    }

    class VerifyTokenTask extends AsyncTask<String, String, String> {

        private ZTLoginActivity ztla;

        public VerifyTokenTask(ZTLoginActivity ztla) {
            this.ztla = ztla;
        }

        @Override
        protected String doInBackground(String... params) {
            ztla.verifyToken();
            return "foo";
        }

        protected void onPostExecute(String result) {
            ztla.finish();
        }
    }

    public void verifyToken() {

        Log.d("ZT OAUTH", "VERIFYING TOKEN...");

        Map<String, String> parameters = service.getAccessParameters(mRequestToken, mAccessVerifier);

        StrongHttpsClient client = getHttpClientInstance();

        HttpPost post = new HttpPost("http://www.stichtingrz.co/oauth1/access"); // TODO: move to values/preferences

        List<NameValuePair> params = new ArrayList<NameValuePair>();

        for (String key : parameters.keySet()) {
            params.add(new BasicNameValuePair(key, parameters.get(key)));
            Log.d("ZT OAUTH", "ADDING PARAMETER: " + key + ": " + parameters.get(key));
        }

        try {
            post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            Log.d("ZT OAUTH", "FAILED TO ENCODE ENTITY");
            uee.printStackTrace();
            return;
        }

        HttpResponse response = null;

        try {
            response = client.execute(post);
        } catch (ClientProtocolException cpe) {
            Log.d("ZT OAUTH", "FAILED TO EXECUTE REQUEST (CPE)");
            cpe.printStackTrace();
            return;
        } catch (IOException ioe) {
            Log.d("ZT OAUTH", "FAILED TO EXECUTE REQUEST (IOE)");
            ioe.printStackTrace();
            return;
        }

        Log.d("ZT OAUTH", "RESPONSE CODE: " + response.getStatusLine().getStatusCode());

        StringBuffer result = new StringBuffer();

        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException ioe) {
            Log.d("ZT OAUTH", "FAILED TO READ RESPONSE");
            ioe.printStackTrace();
            return;
        }

        Header[] postHeaders = response.getAllHeaders();

        for (int i = 0; i < postHeaders.length; i++) {
            Log.d("ZT OAUTH", "FOUND HEADER: " + postHeaders[i].getName() + ": " + postHeaders[i].getValue());
        }

        Log.d("ZT OAUTH", "RESPONSE: " + result.toString());

        if (result.toString().contains("oauth_token=") && result.toString().contains("oauth_token_secret=")) {
            String[] responseParts = result.toString().split("&");

            String oauthToken = "";
            String oauthTokenSecret = "";

            for (String responsePart : responseParts) {
                if (responsePart.contains("oauth_token=")) {
                    oauthToken = responsePart.substring(responsePart.indexOf("=") + 1);
                }
                if (responsePart.contains("oauth_token_secret=")) {
                    oauthTokenSecret = responsePart.substring(responsePart.indexOf("=") + 1);
                }
            }

            mAccessToken = new Token(oauthToken, oauthTokenSecret, result.toString());

            Log.d("ZT OAUTH", "GOT ACCESS TOKEN: " + mAccessToken.getToken());

            mAccessResult = RESULT_OK;

            return;

        } else {
            Log.e("ZT OAUTH", "TOKEN ELEMENTS MISSING FROM RESPONSE");
        }

        return;
    }

    private synchronized StrongHttpsClient getHttpClientInstance() {

        if (mClient == null) {
            mClient = new StrongHttpsClient(this);
        }

        return mClient;
    }

    @Override
    public void finish() {

        Intent data = new Intent();
        data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, mAccessToken.getToken());
        setResult(mAccessResult, data);

        super.finish();
    }

}
