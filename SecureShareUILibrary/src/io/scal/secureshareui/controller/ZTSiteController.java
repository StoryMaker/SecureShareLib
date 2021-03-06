package io.scal.secureshareui.controller;

import timber.log.Timber;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.flickr.api.Flickr;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.ZTApi;
import org.scribe.builder.api.ZTTestApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuth10aServiceImpl;
import org.scribe.oauth.OAuthService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.boye.httpclientandroidlib.entity.mime.MultipartEntityBuilder;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.conn.params.ConnRoutePNames;
import ch.boye.httpclientandroidlib.entity.StringEntity;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import info.guardianproject.netcipher.client.StrongHttpsClient;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import io.scal.secureshareui.login.ZTLoginActivity;
import io.scal.secureshareui.model.Account;
import io.scal.secureshareuilibrary.R;

/**
 * Created by mnbogner on 3/26/15.
 */
public class ZTSiteController extends SiteController {

    private static final String TAG = "ZTSiteController";

    public static final String SITE_NAME = "Tribune Zamaneh";
    public static final String SITE_KEY = "ZT";

    String key;
    String secret;

    private StrongHttpsClient mClient;
    boolean proxySet = false;

    public ZTSiteController(Context context, Handler handler, String jobId) {

        super(context, handler, jobId);
        key = mContext.getString(R.string.zt_key);
        secret = mContext.getString(R.string.zt_secret);

    }

    @Override
    public void startAuthentication(Account account) {

        Intent intent = new Intent(mContext, ZTLoginActivity.class);
        intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
        ((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE);

    }

    @Override
    public void startMetadataActivity(Intent intent) {

        Timber.d("startMetadataActivity() NOT YET IMPLEMENTED");

    }

    @Override
    public void upload(Account account, HashMap<String, String> valueMap) {

        Timber.d("upload() NOT YET IMPLEMENTED");

    }

    // credentials are required for authentication
    // title and content are required parameters for posts
    // embed is currently unsupported servers-side
    public String post(String credentials, String title, String content, String embed) {

        StrongHttpsClient client = getHttpClientInstance();

        // check for tor
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean useTor = settings.getBoolean("pusetor", false);

        if (useTor) {
            if ((!OrbotHelper.isOrbotInstalled(mContext)) || (!OrbotHelper.isOrbotRunning(mContext))) {
                Timber.e("TOR SELECTED BUT ORBOT IS INACTIVE (ABORTING)");
                return null;
            } else {
                Timber.e("TOR SELECTED, HOST " + mContext.getString(R.string.zt_tor_host) + ", PORT " + mContext.getString(R.string.zt_tor_port) + " (SETTING PROXY)");

                String host = mContext.getString(R.string.zt_tor_host);
                int port = Integer.parseInt(mContext.getString(R.string.zt_tor_port));

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

        //HttpPost post = new HttpPost(mContext.getString(R.string.zt_post));
        String urlWithAuth = mContext.getString(R.string.zt_post) + "?";

        String clientId = mContext.getString(R.string.zt_key);
        String clientSecret = mContext.getString(R.string.zt_secret);

        OAuthService service = new ServiceBuilder()
                .provider(ZTApi.class)
                .apiKey(clientId)
                .apiSecret(clientSecret)
                .build();

        String[] tokenParts = credentials.split(",");
        Token token = new Token(tokenParts[0], tokenParts[1]);

        Map<String, String> parameters = service.getSignedParameters(token, Verb.POST, mContext.getString(R.string.zt_post));

        // build header
        String authorizationHeader = "OAuth ";
        for (String key : parameters.keySet()) {
            //authorizationHeader = authorizationHeader + key + "=\"" + parameters.get(key) + "\", ";
            urlWithAuth = urlWithAuth + URLEncoder.encode(key) + "=" + URLEncoder.encode(parameters.get(key)) + "&";
        }
        // drop trailing comma & space
        //authorizationHeader = authorizationHeader.substring(0, authorizationHeader.length() - 2);

        //Timber.d("BUILT HEADER - Authorization: " + authorizationHeader);

        //post.setHeader("Authorization", authorizationHeader);

        // drop trailing ampersand
        urlWithAuth = urlWithAuth.substring(0, urlWithAuth.length() - 1);

        Timber.d("CONSTRUCTED URL: " + urlWithAuth);

        HttpPost post = new HttpPost(urlWithAuth);

        /*
        for (String key : parameters.keySet()) {
            params.add(new BasicNameValuePair(key, parameters.get(key)));
            Timber.d("ADDING PARAMETER: " + key + ": " + parameters.get(key));
        }

        try {
            UrlEncodedFormEntity foo = new UrlEncodedFormEntity(params, "UTF-8");
            Timber.d("ENTITY: " + foo.toString());
            post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            Timber.d("FAILED TO ENCODE ENTITY");
            uee.printStackTrace();
            return null;
        }
        */

        String jsonString = "{" +
                "\"title\": " + "\"" + title + "\", " +
                "\"content\": { \"raw\": " + "\"" + content + " " + embed + "\"}, " +
                "\"status\": " + "\"" + "publish" + "\"" +
                "}";

        Timber.d("JSON: " + jsonString);

        StringEntity jsonEntity = new StringEntity(jsonString, "UTF-8");
        jsonEntity.setContentType("application/json");

        post.setEntity(jsonEntity);

        try {
            HttpResponse postResponse = client.execute(post);

            Timber.d("RESPONSE CODE: " + postResponse.getStatusLine().getStatusCode());

            BufferedReader rd = new BufferedReader(
                new InputStreamReader(postResponse.getEntity().getContent())
            );

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            Timber.d("RESPONSE: " + result.toString());

            Header[] postHeaders = postResponse.getAllHeaders();

            for (int i = 0; i < postHeaders.length; i++) {
                Timber.d("FOUND HEADER: " + postHeaders[i].getName() + ": " + postHeaders[i].getValue());
            }

            // need to parse post id out of response

            JSONObject obj = new JSONObject(result.toString());
            String link = obj.getString("link");
            if ((link != null) && (link.length() > 0)) {
                Timber.d("GOT LINK: " + link);
                return link;
            } else {
                Timber.d("NO LINK");
            }
        } catch (JSONException je) {
            Timber.e("FAILED TO PARSE RESPONSE");
            je.printStackTrace();
        } catch (ClientProtocolException cpe) {
            Timber.e("FAILED TO EXECUTE REQUEST");
            cpe.printStackTrace();
        } catch (IOException ioe) {
            Timber.e("FAILED TO READ RESPONSE");
            ioe.printStackTrace();
        }

        return null;
    }

    private synchronized StrongHttpsClient getHttpClientInstance() {

        if (mClient == null) {
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
}
