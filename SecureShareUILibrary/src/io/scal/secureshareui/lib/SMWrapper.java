package io.scal.secureshareui.lib;


import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;
import ch.boye.httpclientandroidlib.impl.client.HttpClients;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;

/**
 * Created by mnbogner on 1/30/15.
 */
public class SMWrapper {

    private final String mClientId;
    private final String mClientSecret;

    private String mToken;

    private static final String AUTHORIZE_URL = "https://beta.storymaker.org/api/v0/oauth2/access_token";
    private static final String UPLOAD_URL = "https://beta.storymaker.org/api/v0/story/";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String GRANT_TYPE = "grant_type";
    private static final String CONTENT = "content";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String PUBLISH_DATE = "publish_date";

    public SMWrapper(String clientId, String clientSecret, String token) {
        mClientId = clientId;
        mClientSecret = clientSecret;
        mToken = token;
    }

    public String login(String username, String password) throws IOException {
        if (username == null && password == null) {
            throw new IllegalArgumentException("username and password are null");
        } else if (username == null) {
            throw new IllegalArgumentException("username is null");
        } else if (password == null) {
            throw new IllegalArgumentException("password is null");
        }

        Log.d(CLIENT_ID, mClientId);
        Log.d(CLIENT_SECRET, mClientSecret);
        Log.d(GRANT_TYPE, "password");
        Log.d(USERNAME, username);
        Log.d(PASSWORD, password);

        HttpClient client = HttpClients.createDefault();
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

        HttpResponse response = client.execute(post);

        Log.d("OAUTH", "RESPONSE CODE: " + response.getStatusLine().getStatusCode());

        BufferedReader rd = new BufferedReader(
            new InputStreamReader(response.getEntity().getContent())
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

        return mToken;
    }

    public HttpResponse upload(String title, String desc, String body, String credentials) throws IOException {

        Date publishDate = new Date();

        Log.d(CLIENT_ID, mClientId);
        Log.d(CLIENT_SECRET, mClientSecret);
        Log.d(TITLE, title);
        Log.d(DESCRIPTION, desc);
        Log.d(CONTENT, body);
        Log.d(PUBLISH_DATE, publishDate.toString());
        Log.d("TOKEN", credentials);

        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(UPLOAD_URL);

        post.setHeader("Authorization", "Bearer " + credentials);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CLIENT_ID, mClientId));
        params.add(new BasicNameValuePair(CLIENT_SECRET, mClientSecret));
        params.add(new BasicNameValuePair(TITLE, title));
        //params.add(new BasicNameValuePair(DESCRIPTION, desc)); <- CURRENTLY NULL
        params.add(new BasicNameValuePair(CONTENT, body));
        params.add(new BasicNameValuePair(PUBLISH_DATE, publishDate.toString()));
        //params.add(new BasicNameValuePair("redirect_uri", "http://localhost/callback"));
        post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        return client.execute(post);
    }
}
