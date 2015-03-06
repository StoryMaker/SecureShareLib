package io.scal.secureshareui.lib;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.CookieStore;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.BasicCookieStore;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;
import ch.boye.httpclientandroidlib.impl.client.HttpClients;
import ch.boye.httpclientandroidlib.impl.cookie.BasicClientCookie;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import ch.boye.httpclientandroidlib.params.BasicHttpParams;
import ch.boye.httpclientandroidlib.params.HttpParams;
import info.guardianproject.onionkit.trust.StrongHttpsClient;
import info.guardianproject.onionkit.ui.OrbotHelper;
import io.scal.secureshareuilibrary.R;

/**
 * Created by mnbogner on 1/30/15.
 */
public class ZWrapper {

    private final String mClientId;
    private final String mClientSecret;

    private String mToken;

    StrongHttpsClient mClient = null;

    private static final String LOGIN_URL = "http://storymakertestpub.swn.webfactional.com/wp-login.php";
    private static final String LOGIN_NAME = "log";
    private static final String LOGIN_PASS = "pwd";
    private static final String PRAGMA = "Pragma";
    private static final String NO_CACHE = "no-cache";
    private static final String AUTHORIZE_URL = "http://storymakertestpub.swn.webfactional.com/oauth/authorize/";
    private static final String UPLOAD_URL = "https://foo/";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String USERNAME = "username";
    private static final String GRANT_TYPE = "grant_type";
    private static final String PASSWORD = "password";
    private static final String RESPONSE_TYPE = "response_type";
    private static final String TOKEN = "token";
    private static final String REDIRECT = "redirect_uri";
    private static final String REDIRECT_URL = "http://localhost/storymaker";
    private static final String HANDLE_REDIRECT = "http.protocol.handle-redirects";
    private static final String HANDLE_REDIRECT_SETTING = "false";
    private static final String COOKIE = "Cookie";
    private static final String CONTENT = "content";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String PUBLISH_DATE = "publish_date";

    public ZWrapper(String clientId, String clientSecret, String token) {
        mClientId = clientId;
        mClientSecret = clientSecret;
        mToken = token;
    }

    private synchronized StrongHttpsClient getHttpClientInstance(Context context) {
        if (mClient == null) {
            mClient = new StrongHttpsClient(context);
        }

        return mClient;
    }

    public String login(String username, String password, Context context) throws IOException {

        // NEW CODE


        // OLD CODE

        /*
        // TEMP - FOR TESTING
        username = "uploader";
        password = "";

        if (username == null && password == null) {
            throw new IllegalArgumentException("username and password are null");
        } else if (username == null) {
            throw new IllegalArgumentException("username is null");
        } else if (password == null) {
            throw new IllegalArgumentException("password is null");
        }

        HttpClient client = HttpClients.createDefault();
        //StrongHttpsClient client = getHttpClientInstance(context);

        // check for tor
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useTor = settings.getBoolean("pusetor", false);

        if (useTor) {
            OrbotHelper oh = new OrbotHelper(context);

            if ((!oh.isOrbotInstalled()) || (!oh.isOrbotRunning())) {
                Log.e("PUBLISH", "TOR SELECTED BUT ORBOT IS INACTIVE (ABORTING)");

                return null;
            } else {
                Log.e("PUBLISH", "TOR SELECTED, HOST " + context.getString(R.string.z_tor_host) + ", PORT " + context.getString(R.string.z_tor_port) + " (SETTING PROXY)");

                String host = context.getString(R.string.z_tor_host);
                int port = Integer.parseInt(context.getString(R.string.z_tor_port));
                client.useProxy(true, "http", host, port);
            }
        }

        // get cookies
        HttpPost post = new HttpPost(LOGIN_URL);

        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair(LOGIN_NAME, username));
        postParams.add(new BasicNameValuePair(LOGIN_PASS, password));

        post.addHeader(PRAGMA, NO_CACHE);
        post.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));

        HttpResponse postResponse = client.execute(post);

        Log.d("OAUTH", "LOGIN RESPONSE CODE: " + postResponse.getStatusLine().getStatusCode());

        // assemble cookie for authentication request
        Header[] postHeaders = postResponse.getAllHeaders();

        // unsure which to use
        String cookie = "";
        CookieStore cookieStore = new BasicCookieStore();

        for (int i = 0; i < postHeaders.length; i++) {
            Log.d("OAUTH", "FOUND HEADER: " + postHeaders[i].getName() + ": " + postHeaders[i].getValue());

            if ((postHeaders[i].getName().equals("Set-Cookie")) && (postHeaders[i].getValue().contains("wordpress_test_cookie"))) {
                cookie = cookie + postHeaders[i].getValue().substring(0, postHeaders[i].getValue().indexOf(";") + 1);
                String[] parts = postHeaders[i].getValue().substring(0, postHeaders[i].getValue().indexOf(";")).split("=");
                BasicClientCookie cookie1 = new BasicClientCookie(parts[0], parts[1]);
                cookie1.setPath("/");
                cookie1.setDomain("http://storymakertestpub.swn.webfactional.com");
                cookieStore.addCookie(cookie1);
            }
            if ((postHeaders[i].getName().equals("Set-Cookie")) && (postHeaders[i].getValue().contains("wordpress_logged_in"))) {
                cookie = cookie + postHeaders[i].getValue().substring(0, postHeaders[i].getValue().indexOf(";") + 1);
                String[] parts = postHeaders[i].getValue().substring(0, postHeaders[i].getValue().indexOf(";")).split("=");
                BasicClientCookie cookie2 = new BasicClientCookie(parts[0], parts[1]);
                cookie2.setPath("/");
                cookie2.setDomain("http://storymakertestpub.swn.webfactional.com");
                cookieStore.addCookie(cookie2);
            }
        }

        // cookie cleanup (drop last ;, add space)
        cookie = cookie.substring(0, cookie.lastIndexOf(";"));
        cookie = cookie.replace(";", "; ");

        Log.d("OAUTH", "COOKIE: " + cookie);

        // assemble url w/ query parameters
        String finalUrl = AUTHORIZE_URL + "?" +
                          GRANT_TYPE + "=" + PASSWORD + "&" +
                          CLIENT_ID + "=" + mClientId + "&" +
                          CLIENT_SECRET + "=" + mClientSecret + "&" +
                          REDIRECT + "=" + REDIRECT_URL + "&" +
                          RESPONSE_TYPE + "=" + TOKEN;

        Log.d("OAUTH", "URL: " + finalUrl);

        HttpGet get = new HttpGet(finalUrl);
        //HttpGet get = new HttpGet("http://httpbin.org/cookies");

        HttpParams getParams = new BasicHttpParams();
        getParams.setParameter(HANDLE_REDIRECT, false);

        //List<NameValuePair> getParams = new ArrayList<NameValuePair>();
        //getParams.add(new BasicNameValuePair(HANDLE_REDIRECT, HANDLE_REDIRECT_SETTING));

        get.setParams(getParams);
        //get.setEntity(new UrlEncodedFormEntity(getParams, "UTF-8"));

        get.addHeader(COOKIE, cookie);
        //client.setCookieStore(cookieStore);
        HttpResponse getResponse = client.execute(get);

        Log.d("OAUTH", "RESPONSE CODE: " + getResponse.getStatusLine().getStatusCode());

        // assemble cookie for authentication request
        Header[] getHeaders = getResponse.getAllHeaders();

        for (int i = 0; i < getHeaders.length; i++) {
            Log.d("OAUTH", "FOUND HEADER: " + getHeaders[i].getName() + ": " + getHeaders[i].getValue());
        }

        BufferedReader rd = new BufferedReader(
            new InputStreamReader(getResponse.getEntity().getContent())
        );

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        Log.d("OAUTH", "RESPONSE: " + result.toString());

        try {
            JSONObject json = new JSONObject(result.toString());
            mToken = json.getString("access_token");
            Log.d("OAUTH", "TOKEN: " + mToken);
        } catch (JSONException je) {
            Log.e("OAUTH", "FAILED TO PARSE RESPONSE: " + je.getMessage());
        }
        */

        return mToken;
    }

    public HttpResponse upload(String title, String desc, String body, String credentials, Context context) throws IOException {

        Date publishDate = new Date();

        Log.d(CLIENT_ID, mClientId);
        Log.d(CLIENT_SECRET, mClientSecret);
        Log.d(TITLE, title);
        Log.d(DESCRIPTION, desc);
        Log.d(CONTENT, body);
        Log.d(PUBLISH_DATE, publishDate.toString());
        Log.d("TOKEN", credentials);

        // HttpClient client = HttpClients.createDefault();
        StrongHttpsClient client = getHttpClientInstance(context);

        // check for tor
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useTor = settings.getBoolean("pusetor", false);

        if (useTor) {
            OrbotHelper oh = new OrbotHelper(context);

            if ((!oh.isOrbotInstalled()) || (!oh.isOrbotRunning())) {
                Log.e("PUBLISH", "TOR SELECTED BUT ORBOT IS INACTIVE (ABORTING)");

                return null;
            } else {
                Log.e("PUBLISH", "TOR SELECTED, HOST " + context.getString(R.string.z_tor_host) + ", PORT " + context.getString(R.string.z_tor_port) + " (SETTING PROXY)");

                String host = context.getString(R.string.z_tor_host);
                int port = Integer.parseInt(context.getString(R.string.z_tor_port));
                client.useProxy(true, "http", host, port);
            }
        }

        HttpPost post = new HttpPost(UPLOAD_URL);

        post.setHeader("Authorization", "Bearer " + credentials);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CLIENT_ID, mClientId));
        params.add(new BasicNameValuePair(CLIENT_SECRET, mClientSecret));
        params.add(new BasicNameValuePair(TITLE, title));
        params.add(new BasicNameValuePair(CONTENT, body));
        params.add(new BasicNameValuePair(PUBLISH_DATE, publishDate.toString()));
        params.add(new BasicNameValuePair("redirect_uri", "http://localhost/storymaker"));
        post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        return client.execute(post);
    }
}
