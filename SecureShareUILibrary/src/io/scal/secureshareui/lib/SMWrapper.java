package io.scal.secureshareui.lib;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.conn.params.ConnRoutePNames;
import ch.boye.httpclientandroidlib.entity.StringEntity;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;
import ch.boye.httpclientandroidlib.impl.client.HttpClients;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import info.guardianproject.onionkit.trust.StrongHttpsClient;
import info.guardianproject.onionkit.ui.OrbotHelper;
import io.scal.secureshareuilibrary.R;

/**
 * Created by mnbogner on 1/30/15.
 */
public class SMWrapper {

    private final Context mContext;

    private final String mClientId;
    private final String mClientSecret;

    private String mToken;

    StrongHttpsClient mClient = null;
    boolean proxySet = false;
    //HttpClient mClient = null;

    private String AUTHORIZE_URL; // previously hard coded to "https://storymaker.org/api/oauth2/access_token"
    private String UPLOAD_URL; // previously hard coded to "https://storymaker.org/api/story/"

    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String GRANT_TYPE = "grant_type";
    private static final String CONTENT = "content";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String PUBLISH_DATE = "publish_date";

    // public SMWrapper(String clientId, String clientSecret, String token) {
    public SMWrapper(Context context) {
        mContext = context;
        mClientId = context.getString(R.string.sm_key); // FIXME obfuscate these, and when you do generate new keys
        mClientSecret = context.getString(R.string.sm_secret);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        String url = settings.getString("pserver", "https://storymaker.org/");
        if (!url.endsWith("/")) {
            url = url + "/";
        }

        AUTHORIZE_URL = url + "api/oauth2/access_token";
        UPLOAD_URL = url + "api/story/";
    }

    private synchronized StrongHttpsClient getHttpClientInstance() {
    //private synchronized HttpClient getHttpClientInstance() {
        if (mClient == null) {
            mClient = new StrongHttpsClient(mContext);
            //mClient = HttpClients.createDefault();
        }

        return mClient;
    }

    /**
     *
     * @param username
     * @param password
     * @return token, if null it was unsuccessful
     * @throws IOException
     */
    public String login(String username, String password) throws IOException {
        if (username == null && password == null) {
            throw new IllegalArgumentException("username and password are null");
        } else if (username == null) {
            throw new IllegalArgumentException("username is null");
        } else if (password == null) {
            throw new IllegalArgumentException("password is null");
        }

        //Log.d(CLIENT_ID, mClientId);
        //Log.d(CLIENT_SECRET, mClientSecret);
        //Log.d(GRANT_TYPE, "password");
        //Log.d(USERNAME, username);
        //Log.d(PASSWORD, password);

        StrongHttpsClient client = getHttpClientInstance();
        //HttpClient client = getHttpClientInstance();

        // check for tor
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean useTor = settings.getBoolean("pusetor", false);

        if (useTor) {
            OrbotHelper oh = new OrbotHelper(mContext);

            if ((!oh.isOrbotInstalled()) || (!oh.isOrbotRunning())) {
                Log.e("PUBLISH", "TOR SELECTED BUT ORBOT IS INACTIVE (ABORTING)");

                return null;
            } else {
                Log.d("PUBLISH", "TOR SELECTED, HOST " + mContext.getString(R.string.sm_tor_host) + ", PORT " + mContext.getString(R.string.sm_tor_port) + " (SETTING PROXY)");

                String host = mContext.getString(R.string.sm_tor_host);
                int port = Integer.parseInt(mContext.getString(R.string.sm_tor_port));

                //client.useProxy(true, "http", host, port);
                HttpHost proxy = new HttpHost(host, port, "http");
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                proxySet = true;
            }
        } else {
            if (proxySet) {
                Log.d("PUBLISH", "TOR NOT SELECTED (CLEARING PROXY)");

                client.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
                proxySet = false;
            } else {
                Log.d("PUBLISH", "TOR NOT SELECTED");
            }
        }

        HttpPost post = new HttpPost(AUTHORIZE_URL);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CLIENT_ID, mClientId));
        params.add(new BasicNameValuePair(CLIENT_SECRET, mClientSecret));
        params.add(new BasicNameValuePair(GRANT_TYPE, "password"));
        params.add(new BasicNameValuePair(USERNAME, username));
        params.add(new BasicNameValuePair(PASSWORD, password));
        //params.add(new BasicNameValuePair("redirect_uri", "http://localhost/callback"));
        post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        /*
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(AUTHORIZE_URL + "?" +
                CLIENT_ID + "=" + mClientId + "&" +
                CLIENT_SECRET + "=" + mClientSecret + "&" +
                GRANT_TYPE + "=" + "password" + "&" +
                USERNAME + "=" + username + "&" +
                PASSWORD + "=" + password);

        /*
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair(CLIENT_ID, mClientId));
        urlParameters.add(new BasicNameValuePair(CLIENT_SECRET, mClientSecret));
        urlParameters.add(new BasicNameValuePair(GRANT_TYPE, "password"));
        urlParameters.add(new BasicNameValuePair(USERNAME, username));
        urlParameters.add(new BasicNameValuePair(PASSWORD, password));

        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        */

        HttpResponse response = null;

        try {
            response = client.execute(post);
        } catch (Exception e) {
            Log.e("OAUTH", e.getMessage());
            e.printStackTrace();
        }

        Log.d("OAUTH", "RESPONSE CODE: " + response.getStatusLine().getStatusCode());

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent())
        );

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        Header[] postHeaders = response.getAllHeaders();

        for (int i = 0; i < postHeaders.length; i++) {
            Log.d("OAUTH", "FOUND HEADER: " + postHeaders[i].getName() + ": " + postHeaders[i].getValue());
        }

        Log.d("OAUTH", "RESPONSE: " + result.toString());

        try {
            JSONObject json = new JSONObject(result.toString());
            mToken = json.getString("access_token");
            //Log.d("OAUTH", "TOKEN: " + mToken);
        } catch (JSONException je) {
            Log.e("OAUTH", "FAILED TO PARSE RESPONSE: " + je.getMessage());
            throw new IOException("unexpected response received");
        }

        return mToken;
    }

    // mimeType and file are nullable
    // link this with upload method
    public String post (String user, String title, String body, String embed, String[] catstrings, String medium, String mediaService, String mediaGuid, String mimeType, File file) throws IOException {

        HttpResponse postResponse = upload(user, title, catstrings, body, embed, mToken);

        Log.d("PUBLISH", "RESPONSE CODE: " + postResponse.getStatusLine().getStatusCode());

        BufferedReader rd = new BufferedReader(
            new InputStreamReader(postResponse.getEntity().getContent())
        );

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        Header[] postHeaders = postResponse.getAllHeaders();

        for (int i = 0; i < postHeaders.length; i++) {
            Log.d("PUBLISH", "FOUND HEADER: " + postHeaders[i].getName() + ": " + postHeaders[i].getValue());
        }

        Log.d("PUBLISH", "RESPONSE: " + result.toString());


        return null; // FIXME need to parse post id out of response
    }

    public HttpResponse upload(String user, String title, String[] catstrings, String body, String embed, String credentials) throws IOException {

        Date publishDate = new Date();

        //Log.d(CLIENT_ID, mClientId);
        //Log.d(CLIENT_SECRET, mClientSecret);
        //Log.d(TITLE, title);
        //Log.d(DESCRIPTION, desc);
        //Log.d(CONTENT, body);
        //Log.d(PUBLISH_DATE, publishDate.toString());
        //Log.d("group_ids", user + "_public");
        //Log.d("TOKEN", credentials);

        StrongHttpsClient client = getHttpClientInstance();
        //HttpClient client = getHttpClientInstance();

        // check for tor
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean useTor = settings.getBoolean("pusetor", false);

        if (useTor) {
            OrbotHelper oh = new OrbotHelper(mContext);

            if ((!oh.isOrbotInstalled()) || (!oh.isOrbotRunning())) {
                Log.e("PUBLISH", "TOR SELECTED BUT ORBOT IS INACTIVE (ABORTING)");

                return null;
            } else {
                Log.e("PUBLISH", "TOR SELECTED, HOST " + mContext.getString(R.string.sm_tor_host) + ", PORT " + mContext.getString(R.string.sm_tor_port) + " (SETTING PROXY)");

                String host = mContext.getString(R.string.sm_tor_host);
                int port = Integer.parseInt(mContext.getString(R.string.sm_tor_port));

                //client.useProxy(true, "http", host, port);
                HttpHost proxy = new HttpHost(host, port, "http");
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                proxySet = true;
            }
        } else {
            if (proxySet) {
                Log.d("PUBLISH", "TOR NOT SELECTED (CLEARING PROXY)");

                client.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
                proxySet = false;
            } else {
                Log.d("PUBLISH", "TOR NOT SELECTED");
            }
        }

        HttpPost post = new HttpPost(UPLOAD_URL);

        post.setHeader("Authorization", "Bearer " + credentials);

        /*
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CLIENT_ID, mClientId));
        params.add(new BasicNameValuePair(CLIENT_SECRET, mClientSecret));
        params.add(new BasicNameValuePair(TITLE, title));
        //params.add(new BasicNameValuePair(DESCRIPTION, desc)); <- CURRENTLY NULL
        params.add(new BasicNameValuePair(CONTENT, body));
        params.add(new BasicNameValuePair(PUBLISH_DATE, publishDate.toString()));
        //new
        params.add(new BasicNameValuePair("group_ids", user + "_public"));
        //params.add(new BasicNameValuePair("redirect_uri", "http://localhost/callback"));
        post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        */

        String catString = "";
        for (int i = 0; i < catstrings.length; i++) {
            catString = catString + "\"" + catstrings[i] + "\"";
            if (i < catstrings.length - 1) {
                catString = catString + ", ";
            }
        }

        // build json string to post
        String jsonString = "{" +
                "\"title\": " + "\"" + title + "\", " +
                "\"description\": " + "\"" + "\", " +
                "\"content\": " + "\"" + body + "\", " +
                "\"keywords_string\": " + "\"" + "\", " +
                "\"categories\": " + "[" + "], " + // leave categories blank for now
                "\"related_posts\": " + "[" + "], " +
                "\"groups\": " + "[{\"name\": " + "\"" + user + "_public\"}], " +
                "\"media_shortcodes\": " + "[{\"shortcode\": " + "\"" + embed + "\"}]" +
                "}";

        /*
        {
          "title": "test1: post to protected ",
          "description": "",
          "content": "<iframe width=\"760\" height=\"430\" src=\"https://www.youtube.com/embed/-Gp56HzYUlw\" frameborder=\"0\" allowfullscreen></iframe>",
          "keywords_string": "",
          "categories": [],
          "related_posts": [],
          "groups": [{"name":"test1_protected"}],
          "media_shortcodes": [{"shortcode":"foo2"},{"shortcode":"bar2"}]
        }
        */

        Log.d("PUBLISH", "JSON: " + jsonString);

        StringEntity jsonEntity = new StringEntity(jsonString, "UTF-8");
        jsonEntity.setContentType("application/json");
        post.setEntity(jsonEntity);

        return client.execute(post);
    }

    // need to implement these methods
    public String getPostUrl (String postId) {
        return null;
    }
    public Object getPost (String postId) {
        return null; // wordpress class returned a "Page"?
    }
    public List<Object> getRecentPosts (int num) {
        return null; // wordpress class returned a "Page"?
    }
    public List<Object> getComments (Object page) {
        return null; // wordpress class took a "Page" and returned a "Comment"?
    }
    public String addMedia (String mimeType, File file) {
        return null; // wordpress class returned the url of a "MediaObject"?
    }
}