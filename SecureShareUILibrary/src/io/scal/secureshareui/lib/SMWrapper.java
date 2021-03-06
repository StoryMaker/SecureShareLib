package io.scal.secureshareui.lib;

import timber.log.Timber;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.squareup.okhttp.OkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
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
import info.guardianproject.netcipher.client.StrongHttpsClient;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import io.scal.secureshareuilibrary.R;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.mime.TypedOutput;

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

        String url = getUrl();
        if (!url.endsWith("/")) {
            url = url + "/";
        }

        AUTHORIZE_URL = url + "api/oauth2/access_token";
        UPLOAD_URL = url + "api/story/";
    }

    private synchronized StrongHttpsClient getHttpClientInstance() {
    //private synchronized HttpClient getHttpClientInstance() {
        if (    mClient == null) {

            try {
                mClient = new StrongHttpsClient(mContext, R.raw.debiancacerts, null);
                //mClient = HttpClients.createDefault();
            } catch (Exception e)
            {
                Log.e("NetCipher","error init'd stronghttpsclient",e);
            }

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

        //Timber.d(mClientId);
        //Timber.d(mClientSecret);
        //Timber.d("password");
        //Timber.d(username);
        //Timber.d(password);

        StrongHttpsClient client = getHttpClientInstance();
        //HttpClient client = getHttpClientInstance();

        // check for tor
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean useTor = settings.getBoolean("pusetor", false);

        /*
        if (useTor) {
            OrbotHelper oh = new OrbotHelper(mContext);

            if ((!oh.isOrbotInstalled()) || (!oh.isOrbotRunning())) {
                Timber.e("TOR SELECTED BUT ORBOT IS INACTIVE (ABORTING)");

                return null;
            } else {
                Timber.d("TOR SELECTED, HOST " + mContext.getString(R.string.sm_tor_host) + ", PORT " + mContext.getString(R.string.sm_tor_port) + " (SETTING PROXY)");

                String host = mContext.getString(R.string.sm_tor_host);
                int port = Integer.parseInt(mContext.getString(R.string.sm_tor_port));

                //client.useProxy(true, "http", host, port);
                HttpHost proxy = new HttpHost(host, port, "http");
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                proxySet = true;
            }
        } else {
            if (proxySet) {
                Timber.d("TOR NOT SELECTED (CLEARING PROXY)");

                client.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
                proxySet = false;
            } else {
                Timber.d("TOR NOT SELECTED");
            }
        }
        */

        /*
        HttpPost post = new HttpPost(AUTHORIZE_URL);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CLIENT_ID, mClientId));
        params.add(new BasicNameValuePair(CLIENT_SECRET, mClientSecret));
        params.add(new BasicNameValuePair(GRANT_TYPE, "password"));
        params.add(new BasicNameValuePair(USERNAME, username));
        params.add(new BasicNameValuePair(PASSWORD, password));
        //params.add(new BasicNameValuePair("redirect_uri", "http://localhost/callback"));
        post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(AUTHORIZE_URL + "?" +
                CLIENT_ID + "=" + mClientId + "&" +
                CLIENT_SECRET + "=" + mClientSecret + "&" +
                GRANT_TYPE + "=" + "password" + "&" +
                USERNAME + "=" + username + "&" +
                PASSWORD + "=" + password);

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair(CLIENT_ID, mClientId));
        urlParameters.add(new BasicNameValuePair(CLIENT_SECRET, mClientSecret));
        urlParameters.add(new BasicNameValuePair(GRANT_TYPE, "password"));
        urlParameters.add(new BasicNameValuePair(USERNAME, username));
        urlParameters.add(new BasicNameValuePair(PASSWORD, password));

        post.setEntity(new UrlEncodedFormEntity(urlParameters));

        HttpResponse response = null;
        */



        // TRY NEW RETROFIT STUFF

        RestAdapter.Builder builder = new RestAdapter.Builder()
                                          .setLogLevel(RestAdapter.LogLevel.BASIC)
                                          .setEndpoint(getUrl());

        // check for tor
        if (useTor) {

            if ((!OrbotHelper.isOrbotInstalled(mContext)) || (!OrbotHelper.isOrbotRunning(mContext))) {
                Timber.e("TOR SELECTED BUT ORBOT IS INACTIVE (ABORTING)");

                throw new IOException("tor selected but orbot inactive");
            } else {

                // get tor parameters
                String torHost = mContext.getString(R.string.sm_tor_host);
                String torPort = mContext.getString(R.string.sm_tor_port);

                Timber.d("TOR SELECTED, HOST " + torHost + ", PORT " + torPort + " (BUILDING CUSTOM CLIENT)");

                // build a client with a proxy
                OkHttpClient httpClient = new OkHttpClient();
                SocketAddress torSocket = new InetSocketAddress(torHost, Integer.parseInt(torPort));
                Proxy torProxy = new Proxy(Proxy.Type.HTTP, torSocket);
                httpClient.setProxy(torProxy);

                // create retrofit wrapper class
                OkClient retrofitClient = new OkClient(httpClient);

                // add to builder
                builder.setClient(retrofitClient);
            }
        }

        /*
        RestAdapter restAdapter = new RestAdapter.Builder()
                                      .setLogLevel(RestAdapter.LogLevel.FULL)
                                      .setEndpoint(getUrl())
                                      .build();
        */

        RestAdapter restAdapter = builder.build();

        LoginInterface loginService = restAdapter.create(LoginInterface.class);

        try {

            Response rResponse = loginService.getAccessToken(mClientId, mClientSecret, "password", username, password);

        /*
        try {
            response = client.execute(post);
        } catch (Exception e) {
            Timber.e(e.getMessage());
            e.printStackTrace();
        }
        */

            //Timber.d("RESPONSE CODE: " + response.getStatusLine().getStatusCode());
            Timber.d("RESPONSE CODE: " + rResponse.getStatus());


            BufferedReader rd = new BufferedReader(
                    //new InputStreamReader(response.getEntity().getContent())
                    new InputStreamReader(rResponse.getBody().in())
            );

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            //Header[] postHeaders = response.getAllHeaders();
            List<retrofit.client.Header> postHeaders = rResponse.getHeaders();

            //for (int i = 0; i < postHeaders.length; i++) {
            for (int i = 0; i < postHeaders.size(); i++) {
                //Timber.d("FOUND HEADER: " + postHeaders[i].getName() + ": " + postHeaders[i].getValue());
                Timber.d("FOUND HEADER: " + postHeaders.get(i).getName() + ": " + postHeaders.get(i).getValue());
            }

            Timber.d("RESPONSE: " + result.toString());

            // need to attempt to deal with cloudflare captcha challenge over tor
            if ((useTor) && result.toString().contains("chk_captcha")) {

                Timber.e("ENCOUNTERED CAPTCHA CHALLENGE PAGE (TOR IP ADDRESSES MAY BE CONSIDERED SUSPICIOUS)");

                throw new CaptchaException();

            }

            try {
                JSONObject json = new JSONObject(result.toString());
                mToken = json.getString("access_token");
                //Timber.d("TOKEN: " + mToken);
            } catch (JSONException je) {
                Timber.e("FAILED TO PARSE RESPONSE: " + je.getMessage());
                throw new IOException("unexpected response received");
            }
        } catch (retrofit.RetrofitError re) {
            Timber.e("FAILED TO CONNECT: " + re.getMessage());
            throw new IOException("no response received");
        }

        return mToken;
    }

    // mimeType and file are nullable
    // link this with upload method
    public String post (String user, String title, String body, String embed, String[] catstrings, String medium, String mediaService, String mediaGuid, String mimeType, File file) throws IOException {

        //HttpResponse postResponse = upload(user, title, catstrings, body, embed, mToken);
        Response postResponse = upload(user, title, catstrings, body, embed, mToken);

        // catch null (probably caused by retrofit handling of 404)
        if(postResponse == null) {

            Timber.e("PUBLICATION FAILED");
            return "0" + ":" + "Publishing to StoryMaker failed.";
        }

        //Timber.d("RESPONSE CODE: " + postResponse.getStatusLine().getStatusCode());
        Timber.d("RESPONSE CODE: " + postResponse.getStatus());

        BufferedReader rd = new BufferedReader(
            //new InputStreamReader(postResponse.getEntity().getContent())
                new InputStreamReader(postResponse.getBody().in())
        );

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        //Header[] postHeaders = postResponse.getAllHeaders();
        List<retrofit.client.Header> postHeaders = postResponse.getHeaders();

        //for (int i = 0; i < postHeaders.length; i++) {
        for (int i = 0; i < postHeaders.size(); i++) {
            //Timber.d("FOUND HEADER: " + postHeaders[i].getName() + ": " + postHeaders[i].getValue());
            Timber.d("FOUND HEADER: " + postHeaders.get(i).getName() + ": " + postHeaders.get(i).getValue());
        }

        Timber.d("RESPONSE: " + result.toString());
        
        // check for tor
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean useTor = settings.getBoolean("pusetor", false);

        // need to attempt to deal with cloudflare captcha challenge over tor
        if ((useTor) && result.toString().contains("chk_captcha")) {

            Timber.e("ENCOUNTERED CAPTCHA CHALLENGE PAGE (TOR IP ADDRESSES MAY BE CONSIDERED SUSPICIOUS)");
            return postResponse.getStatus() + ":" + "Publishing to StoryMaker failed.  Try restarting TOR";
        }

        // catch other failures
        if((postResponse.getStatus() < 200) || (postResponse.getStatus() > 299)) {

            Timber.e("PUBLICATION FAILED");
            return postResponse.getStatus() + ":" + "Publishing to StoryMaker failed.";
        }

        return null; // FIXME need to parse post id out of response (response currently appears to be the json object representing the post and has no id)
    }

    // NEW/TEMP
    // DOWNLOAD AVAILABE INDEX FOR CURRENT USER AND SAVE TO TARGET FILE
    // RETURN TRUE IF SUCCESSFUL
    // CAN'T SAVE TO FILE, CONVERSION REQUIRED AND DON'T WANT CLASS DEPENDENT ON LIGER
    public JSONArray index(int version) {

        // check for tor
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean useTor = settings.getBoolean("pusetor", false);

        // TRY NEW RETROFIT STUFF

        RestAdapter.Builder builder = new RestAdapter.Builder()
                                          .setLogLevel(RestAdapter.LogLevel.BASIC)
                                          .setEndpoint(getUrl());

        // check for tor
        if (useTor) {

            if ((!OrbotHelper.isOrbotInstalled(mContext)) || (!OrbotHelper.isOrbotRunning(mContext))) {

                Timber.e("INDEX", "TOR SELECTED BUT ORBOT IS INACTIVE (ABORTING)");

                return null;
            } else {

                // get tor parameters
                String torHost = mContext.getString(R.string.sm_tor_host);
                String torPort = mContext.getString(R.string.sm_tor_port);

                Timber.d("TOR SELECTED, HOST " + torHost + ", PORT " + torPort + " (BUILDING CUSTOM CLIENT)");

                // build a client with a proxy
                OkHttpClient httpClient = new OkHttpClient();
                SocketAddress torSocket = new InetSocketAddress(torHost, Integer.parseInt(torPort));
                Proxy torProxy = new Proxy(Proxy.Type.HTTP, torSocket);
                httpClient.setProxy(torProxy);

                // create retrofit wrapper class
                OkClient retrofitClient = new OkClient(httpClient);

                // add to builder
                builder.setClient(retrofitClient);
            }
        }

        /*
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(getUrl())
                .build();
        */

        RestAdapter restAdapter = builder.build();

        IndexInterface indexService = restAdapter.create(IndexInterface.class);

        try {

            Response rResponse = indexService.getIndex(version, "Bearer " + mToken);

            Timber.d("RESPONSE CODE: " + rResponse.getStatus());

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(rResponse.getBody().in())
            );

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            List<retrofit.client.Header> postHeaders = rResponse.getHeaders();

            for (int i = 0; i < postHeaders.size(); i++) {
                Timber.d("FOUND HEADER: " + postHeaders.get(i).getName() + ": " + postHeaders.get(i).getValue());
            }

            Timber.d("RESPONSE: " + result.toString());

            // response should be a collection of json objects to convert to index items

            try {
                JSONArray jArray = new JSONArray(result.toString());

                return jArray;

            } catch (JSONException je) {
                Timber.e("FAILED TO PARSE RESPONSE: " + je.getMessage());
                return null;
            }
        } catch (retrofit.RetrofitError re) {
            Timber.e("FAILED TO CONNECT: " + re.getMessage());
            return null;
        } catch (IOException ioe) {
            Timber.e("FAILED TO READ RESPONSE: " + ioe.getMessage());
            return null;
        }
    }

    //public HttpResponse upload(String user, String title, String[] catstrings, String body, String embed, String credentials) throws IOException {
    public Response upload(String user, String title, String[] catstrings, String body, String embed, String credentials) throws IOException {

        Date publishDate = new Date();

        //Timber.d(mClientId);
        //Timber.d(mClientSecret);
        //Timber.d(title);
        //Timber.d(desc);
        //Timber.d(body);
        //Timber.d(publishDate.toString());
        //Timber.d(user + "_public");
        //Timber.d(credentials);

        StrongHttpsClient client = getHttpClientInstance();
        //HttpClient client = getHttpClientInstance();

        // check for tor
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean useTor = settings.getBoolean("pusetor", false);

        /*
        if (useTor) {
            OrbotHelper oh = new OrbotHelper(mContext);

            if ((!oh.isOrbotInstalled()) || (!oh.isOrbotRunning())) {
                Timber.e("TOR SELECTED BUT ORBOT IS INACTIVE (ABORTING)");

                return null;
            } else {
                Timber.e("TOR SELECTED, HOST " + mContext.getString(R.string.sm_tor_host) + ", PORT " + mContext.getString(R.string.sm_tor_port) + " (SETTING PROXY)");

                String host = mContext.getString(R.string.sm_tor_host);
                int port = Integer.parseInt(mContext.getString(R.string.sm_tor_port));

                //client.useProxy(true, "http", host, port);
                HttpHost proxy = new HttpHost(host, port, "http");
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                proxySet = true;
            }
        } else {
            if (proxySet) {
                Timber.d("TOR NOT SELECTED (CLEARING PROXY)");

                client.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
                proxySet = false;
            } else {
                Timber.d("TOR NOT SELECTED");
            }
        }
        */

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

        Timber.d("JSON: " + jsonString);

        StringEntity jsonEntity = new StringEntity(jsonString, "UTF-8");
        jsonEntity.setContentType("application/json");
        post.setEntity(jsonEntity);


        // TRY NEW RETROFIT STUFF

        RestAdapter.Builder builder = new RestAdapter.Builder()
                                          .setLogLevel(RestAdapter.LogLevel.BASIC)
                                          .setEndpoint(getUrl());

        // check for tor
        if (useTor) {

            if ((!OrbotHelper.isOrbotInstalled(mContext)) || (!OrbotHelper.isOrbotRunning(mContext))) {
                Timber.e("TOR SELECTED BUT ORBOT IS INACTIVE (ABORTING)");

                return null;
            } else {

                // get tor parameters
                String torHost = mContext.getString(R.string.sm_tor_host);
                String torPort = mContext.getString(R.string.sm_tor_port);

                Timber.d("TOR SELECTED, HOST " + torHost + ", PORT " + torPort + " (BUILDING CUSTOM CLIENT)");

                // build a client with a proxy
                OkHttpClient httpClient = new OkHttpClient();
                SocketAddress torSocket = new InetSocketAddress(torHost, Integer.parseInt(torPort));
                Proxy torProxy = new Proxy(Proxy.Type.HTTP, torSocket);
                httpClient.setProxy(torProxy);

                // create retrofit wrapper class
                OkClient retrofitClient = new OkClient(httpClient);

                // add to builder
                builder.setClient(retrofitClient);
            }
        }

        /*
        RestAdapter restAdapter = new RestAdapter.Builder()
                                      .setLogLevel(RestAdapter.LogLevel.FULL)
                                      .setEndpoint(getUrl())
                                      .build();
        */

        RestAdapter restAdapter = builder.build();

        PostInterface postService = restAdapter.create(PostInterface.class);

        EntityWrapper jsonEntityWrapper = new EntityWrapper(jsonEntity);

        try {

            Response rResponse = postService.postContent("Bearer " + credentials, jsonEntityWrapper);

            //return client.execute(post);
            return rResponse;

        } catch (retrofit.RetrofitError re) {
            Timber.e("FAILED TO CONNECT: " + re.getMessage());
            return null;
        }
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

    // FIXME this could be more robust and needs a test
    private String getUrl() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        String url = settings.getString("pserver", Constants.DEFAULT_SERVER_URL);
        // Because of the peculiarities of using cloudflare ssl certs and therefore only being allowed
        // sub domains one deep, we need to do a bit of massaging here to figure out the right api domain
        // if our domain is https://storymaker.org we want https://api.storymaker.org
        // if our domain is https://demo.storymaker.org we want: https://api-demo.storymaker.org

        // we want to just use api. not api-www since we have a real api. cert that goes outside of cloudflare
        if (url.equals(Constants.DEFAULT_SERVER_URL_WWW)) {
            url = Constants.DEFAULT_SERVER_URL;
        }

        String prefix = "api-";
        if (url.equals(Constants.DEFAULT_SERVER_URL)) {
            prefix = "api.";
        }
        String[] splits = url.split("://");
        return splits[0] + "://" + prefix + splits[1];
    }
}
