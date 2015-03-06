package io.scal.secureshareui.login;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.HttpClients;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import io.scal.secureshareuilibrary.R;

public class ZWebActivity extends Activity {

    private static final String LOGIN_URL = "http://storymakertestpub.swn.webfactional.com/wp-login.php";
    private static final String AUTH_URL = "http://storymakertestpub.swn.webfactional.com/oauth/authorize/";
    private static final String TOKEN_URL = "http://storymakertestpub.swn.webfactional.com/oauth/token/";

    private String mClientId;
    private String mClientSecret;

    private WebView webView;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mClientId = getString(R.string.z_key);
        mClientSecret = getString(R.string.z_secret);

        setContentView(R.layout.activity_z_web);

        webView = (WebView)findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {

            Intent resultIntent = new Intent();

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Log.d("OAUTH", "URL: " + url);

                if (url.contains("code=")) {
                    Uri uri = Uri.parse(url);
                    String code = uri.getQueryParameter("code");

                    /*
                    String tokenUrl = TOKEN_URL + "?" +
                                      "grant_type" + "=" + "authorization_code" + "&" +
                                      "code" + "=" + code;

                    Log.d("OAUTH", "URL: " + tokenUrl);
                    */

                    /*
                    HttpClient client = HttpClients.createDefault();

                    HttpPost post = new HttpPost(TOKEN_URL);

                    List<NameValuePair> postParams = new ArrayList<NameValuePair>();
                    postParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
                    postParams.add(new BasicNameValuePair("code", code));

                    post.addHeader("Basic Auth", mClientId + ":" + mClientSecret);

                    try {
                        post.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    try {
                        HttpResponse postResponse = client.execute(post);

                        Log.d("OAUTH", "RESPONSE CODE: " + postResponse.getStatusLine().getStatusCode());

                        Header[] postHeaders = postResponse.getAllHeaders();

                        for (int i = 0; i < postHeaders.length; i++) {
                            Log.d("OAUTH", "FOUND HEADER: " + postHeaders[i].getName() + ": " + postHeaders[i].getValue());
                        }

                        BufferedReader rd = new BufferedReader(
                                new InputStreamReader(postResponse.getEntity().getContent())
                        );

                        StringBuffer result = new StringBuffer();
                        String line = "";
                        while ((line = rd.readLine()) != null) {
                            result.append(line);
                        }

                        Log.d("OAUTH", "RESPONSE: " + result.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    */

                    new CheckCredentialsAsync().execute(code, mClientId, mClientSecret);

                    resultIntent.putExtra("access_token", "FOO");
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                    return true;
                } else {
                    return super.shouldOverrideUrlLoading(view, url);
                }
            }
        });

        String finalUrl = AUTH_URL + "?" +
                          "response_type" + "=" + "code" + "&" +
                          //"grant_type" + "=" + "password" + "&" +
                          //"username" + "=" + "uploader" + "&" +
                          //"password" + "=" + "" + "&" +
                          "client_id" + "=" + mClientId + "&" +
                          //"client_secret" + "=" + mClientId;
                          "redirect_uri" + "=" + "http://localhost/storymaker";

        Log.d("OAUTH", "URL: " + finalUrl);

        webView.loadUrl(finalUrl);
    }

    private class CheckCredentialsAsync extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String code = params[0];

            String token = null;
            try {

                String tokenUrl = TOKEN_URL + "?" +
                //        "grant_type" + "=" + "authorization_code" + "&" +
                //        "code" + "=" + code;
                          "client_id" + "=" + mClientId + "&" +
                          "client_secret" + "=" + mClientSecret;

                HttpClient client = HttpClients.createDefault();

                Log.d("OAUTH", "URL: " + TOKEN_URL);

                HttpPost post = new HttpPost(TOKEN_URL);
                //HttpGet post = new HttpGet("http://httpbin.org/get");

                List<NameValuePair> postParams = new ArrayList<NameValuePair>();
                postParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
                postParams.add(new BasicNameValuePair("code", code));
                postParams.add(new BasicNameValuePair("client_id", mClientId));
                postParams.add(new BasicNameValuePair("client_secret", mClientSecret));
                postParams.add(new BasicNameValuePair("redirect_uri", "http://localhost/storymaker"));

                Log.d("OAUTH", "grant_type: " + "authorization_code");
                Log.d("OAUTH", "code: " + code);
                Log.d("OAUTH", "client_id: " + mClientId);
                Log.d("OAUTH", "client_secret: " + mClientSecret);
                Log.d("OAUTH", "redirect_uri: " + "http://localhost/storymaker");

                String clientIdSecret = mClientId + ":" + mClientSecret;

                Log.d("OAUTH", "id/secret: " + clientIdSecret);

                String encodedString = Base64.encodeToString(clientIdSecret.getBytes(), Base64.DEFAULT);

                String clientAuth = "Basic " + encodedString; // Base64.encode(clientIdSecret.getBytes("UTF-8"), Base64.DEFAULT);
                //String clientAuth = "" + Base64.encode(clientIdSecret.getBytes("UTF-8"), Base64.DEFAULT);

                // cleanup?
                clientAuth = clientAuth.replace("\n", "");

                Log.d("OAUTH", "auth: " + clientAuth);

                post.addHeader("Authorization", clientAuth);

                //post.addHeader("Authorization", mClientId + ":" + mClientSecret);


                try {
                    post.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }


             // try {
                    HttpResponse postResponse = client.execute(post);

                    Log.d("OAUTH", "RESPONSE CODE: " + postResponse.getStatusLine().getStatusCode());

                    Header[] postHeaders = postResponse.getAllHeaders();

                    for (int i = 0; i < postHeaders.length; i++) {
                        Log.d("OAUTH", "FOUND HEADER: " + postHeaders[i].getName() + ": " + postHeaders[i].getValue());
                    }

                    BufferedReader rd = new BufferedReader(
                            new InputStreamReader(postResponse.getEntity().getContent())
                    );

                    StringBuffer result = new StringBuffer();
                    String line = "";
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }

                    Log.d("OAUTH", "RESPONSE: " + result.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

             // token = wrapper.login(params[0], params[1], ZLoginActivity.this);
         // } catch (IOException e) {
         //     e.printStackTrace();
         // }

            if (token != null) { // success
                //mAccessToken = token;
                return Integer.toString(RESULT_OK);
            }

            return Integer.toString(RESULT_CANCELED);
        }

        @Override
        protected void onPostExecute(String resultStr) {
            int result = Integer.parseInt(resultStr);

            //btnSignIn.setEnabled(true);
            //TextView tvLoginError = (TextView) findViewById(R.id.tvLoginError);

            if (result == RESULT_CANCELED) {
                //mAccessResult = RESULT_CANCELED;
                //tvLoginError.setVisibility(View.VISIBLE);
            }
            else {
                //mAccessResult = RESULT_OK;
                //tvLoginError.setVisibility(View.GONE);
                //finish();
            }
        }
    }
}
