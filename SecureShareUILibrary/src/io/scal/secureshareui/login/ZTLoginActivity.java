package io.scal.secureshareui.login;

import timber.log.Timber;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.ZTApi;
import org.scribe.builder.api.ZTTestApi;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.conn.params.ConnRoutePNames;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import info.guardianproject.netcipher.client.StrongHttpsClient;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareuilibrary.R;

/**
 * Created by mnbogner on 3/25/15.
 */
public class ZTLoginActivity extends LockableActivity {

    private static final String TAG = "ZTLoginActivity";
    private static final int CODE = 0; // NEED A REAL VALUE?

    private String mClientId;
    private String mClientSecret;

    private Token mRequestToken = null;
    private Verifier mAccessVerifier = null;
    private Token mAccessToken = null;

    private OAuthService service;

    private StrongHttpsClient mClient;
    private boolean proxySet = false;

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

        Timber.d("GETTING REQUEST TOKEN...");

        //
        Map<String, String> parameters = service.getRequestParameters();

        StrongHttpsClient client = getHttpClientInstance();

        // check for tor
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useTor = settings.getBoolean("pusetor", false);

        if (useTor) {

            if ((!OrbotHelper.isOrbotInstalled(this)) || (!OrbotHelper.isOrbotRunning(this))) {
                Timber.e("TOR SELECTED BUT ORBOT IS INACTIVE (ABORTING)");
                return null;
            } else {
                Timber.e("TOR SELECTED, HOST " + getString(R.string.zt_tor_host) + ", PORT " + getString(R.string.zt_tor_port) + " (SETTING PROXY)");

                String host = getString(R.string.zt_tor_host);
                int port = Integer.parseInt(getString(R.string.zt_tor_port));

                HttpHost proxyHost = new HttpHost(host, port, "http");
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);
                proxySet = true;

                // set proxy for scribe oauth service
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
                service.setProxy(proxy);
            }
        } else {
            if (proxySet) {
                Timber.d("TOR NOT SELECTED (CLEARING PROXY)");

                client.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
                proxySet = false;

                // un-set proxy for scribe oauth service
                service.setProxy(null);
            } else {
                Timber.d("TOR NOT SELECTED");
            }
        }

        //HttpPost post = new HttpPost(getString(R.string.zt_request));
        String urlWithAuth = getString(R.string.zt_request) + "?";

        List<NameValuePair> params = new ArrayList<NameValuePair>();

        for (String key : parameters.keySet()) {
            //params.add(new BasicNameValuePair(key, parameters.get(key)));
            urlWithAuth = urlWithAuth + URLEncoder.encode(key) + "=" + URLEncoder.encode(parameters.get(key)) + "&";
            Timber.d("ADDING PARAMETER: " + key + ": " + parameters.get(key));
        }

        // drop trailing ampersand
        urlWithAuth = urlWithAuth.substring(0, urlWithAuth.length() - 1);

        Timber.d("CONSTRUCTED URL: " + urlWithAuth);

        HttpPost post = new HttpPost(urlWithAuth);

        /*
        try {
            post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            Timber.e("FAILED TO ENCODE ENTITY");
            uee.printStackTrace();
            return null;
        }
        */

        HttpResponse response = null;

        try {
            response = client.execute(post);
        } catch (ClientProtocolException cpe) {
            Timber.e("FAILED TO EXECUTE REQUEST (CPE)");
            cpe.printStackTrace();
            return null;
        } catch (IOException ioe) {
            Timber.e("FAILED TO EXECUTE REQUEST (IOE)");
            ioe.printStackTrace();
            return null;
        }

        Timber.d("RESPONSE CODE: " + response.getStatusLine().getStatusCode());

        StringBuffer result = new StringBuffer();

        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException ioe) {
            Timber.e("FAILED TO READ RESPONSE");
            ioe.printStackTrace();
            return null;
        }

        Timber.d("RESPONSE: " + result.toString());

        Header[] postHeaders = response.getAllHeaders();

        for (int i = 0; i < postHeaders.length; i++) {
            Timber.d("FOUND HEADER: " + postHeaders[i].getName() + ": " + postHeaders[i].getValue());
        }

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
            //

            //mRequestToken = service.getRequestToken();


            Timber.d("GOT REQUEST TOKEN: " + mRequestToken.getToken());

            Timber.d("GETTING AUTH URL...");

            String authorizationUrl = service.getAuthorizationUrl(mRequestToken);

            // wp-api requires valid oauth callback url as well
            try {
                authorizationUrl = authorizationUrl + "&oauth_callback=" + URLEncoder.encode(getString(R.string.zt_callback), "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                Timber.e("ENCODING CALLBACK FAILED");
                uee.printStackTrace();
                return null;
            }

            Timber.d("GOT AUTH URL: " + authorizationUrl);
            return authorizationUrl;

        //
        } else {
            Timber.e("TOKEN ELEMENTS MISSING FROM RESPONSE");
            return null;
        }
        //
    }

    public void startZTWebActivity(String authorizationUrl) {

        Timber.d("STARTING WEB ACTIVITY FOR " + authorizationUrl);

        Intent i = new Intent(this, ZTWebActivity.class);
        i.putExtra("authorizationUrl", authorizationUrl);
        startActivityForResult(i, CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Timber.d("GOT ACTIVITY RESULT");

        if (requestCode == CODE) {

            if (resultCode == Activity.RESULT_OK) {

                Bundle results = data.getExtras();

                mAccessVerifier = new Verifier(results.getString("verifier"));

                if ((mRequestToken != null) && (mAccessVerifier != null)) {

                    Timber.d("GOT TOKEN " + mRequestToken.getToken() + ", GOT VERIFIER " + mAccessVerifier.getValue());

                    VerifyTokenTask vtTask = new VerifyTokenTask(this);
                    vtTask.execute();

                } else {

                    Timber.e("MISSING TOKEN AND/OR VERIFIER");

                }
            } else if (resultCode == Activity.RESULT_CANCELED) {

                Timber.e("ACTIVITY CANCELLED");

            } else {

                Timber.e("UNEXPECTED RESULT");

            }
        } else {

            Timber.e("UNEXPECTED REQUEST");

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

        Timber.d("VERIFYING TOKEN...");

        //
        Map<String, String> parameters = service.getAccessParameters(mRequestToken, mAccessVerifier);

        StrongHttpsClient client = getHttpClientInstance();

        // check for tor
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useTor = settings.getBoolean("pusetor", false);

        if (useTor) {

            if ((!OrbotHelper.isOrbotInstalled(this)) || (!OrbotHelper.isOrbotRunning(this))) {
                Timber.e("TOR SELECTED BUT ORBOT IS INACTIVE (ABORTING)");
                return;
            } else {
                Timber.e("TOR SELECTED, HOST " + getString(R.string.zt_tor_host) + ", PORT " + getString(R.string.zt_tor_port) + " (SETTING PROXY)");

                String host = getString(R.string.zt_tor_host);
                int port = Integer.parseInt(getString(R.string.zt_tor_port));

                HttpHost proxyHost = new HttpHost(host, port, "http");
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);
                proxySet = true;

                // set proxy for scribe oauth service
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
                service.setProxy(proxy);
            }
        } else {
            if (proxySet) {
                Timber.d("TOR NOT SELECTED (CLEARING PROXY)");

                client.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
                proxySet = false;

                // un-set proxy for scribe oauth service
                service.setProxy(null);
            } else {
                Timber.d("TOR NOT SELECTED");
            }
        }

        //HttpPost post = new HttpPost(getString(R.string.zt_access));
        String urlWithAuth = getString(R.string.zt_access) + "?";

        List<NameValuePair> params = new ArrayList<NameValuePair>();

        for (String key : parameters.keySet()) {
            //params.add(new BasicNameValuePair(key, parameters.get(key)));
            urlWithAuth = urlWithAuth + URLEncoder.encode(key) + "=" + URLEncoder.encode(parameters.get(key)) + "&";
            Timber.d("ADDING PARAMETER: " + key + ": " + parameters.get(key));
        }

        // drop trailing ampersand
        urlWithAuth = urlWithAuth.substring(0, urlWithAuth.length() - 1);

        Timber.d("CONSTRUCTED URL: " + urlWithAuth);

        HttpPost post = new HttpPost(urlWithAuth);

        /*
        try {
            post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            Timber.e("FAILED TO ENCODE ENTITY");
            uee.printStackTrace();
            return;
        }
        */

        HttpResponse response = null;

        try {
            response = client.execute(post);
        } catch (ClientProtocolException cpe) {
            Timber.e("FAILED TO EXECUTE REQUEST (CPE)");
            cpe.printStackTrace();
            return;
        } catch (IOException ioe) {
            Timber.e("FAILED TO EXECUTE REQUEST (IOE)");
            ioe.printStackTrace();
            return;
        }

        Timber.d("RESPONSE CODE: " + response.getStatusLine().getStatusCode());

        StringBuffer result = new StringBuffer();

        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException ioe) {
            Timber.e("FAILED TO READ RESPONSE");
            ioe.printStackTrace();
            return;
        }

        Timber.d("RESPONSE: " + result.toString());

        Header[] postHeaders = response.getAllHeaders();

        for (int i = 0; i < postHeaders.length; i++) {
            Timber.d("FOUND HEADER: " + postHeaders[i].getName() + ": " + postHeaders[i].getValue());
        }

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
            //

            //mAccessToken = service.getAccessToken(mRequestToken, mAccessVerifier);

            Timber.d("GOT ACCESS TOKEN: " + mAccessToken.getToken());

            mAccessResult = RESULT_OK;

        //
        } else {
            Timber.e("TOKEN ELEMENTS MISSING FROM RESPONSE");
        }
        //

        return;
    }

    private synchronized StrongHttpsClient getHttpClientInstance() {

        if (mClient == null) {
            try {
                mClient = new StrongHttpsClient(this, R.raw.debiancacerts, null);
                //mClient = HttpClients.createDefault();
            } catch (Exception e)
            {
                Log.e("NetCipher","error init'd stronghttpsclient",e);
            }
        }

        return mClient;
    }

    @Override
    public void finish() {

        Intent data = new Intent();

        if (mCacheWordHandler.isLocked()) {

            Timber.d("cacheword was locked, no result to return from finish()");

        } else {
            // need complete credentials
            String completeToken = mAccessToken.getToken() + "," + mAccessToken.getSecret();

            data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, completeToken);
        }
        
        setResult(mAccessResult, data);

        // clear token to avoid confusion
        mAccessToken = null;

        super.finish();
    }

}
