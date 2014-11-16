package io.scal.secureshareui.login;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.lib.Util;
import io.scal.secureshareuilibrary.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTubeScopes;

public class YoutubeLoginActivity extends Activity implements Runnable {

	private int mAccessResult = RESULT_CANCELED;
	private String mAccessToken = null;

	static final String TAG = "YoutubeLoginActivity";
	
	private String CLIENT_ID;
	private String CLIENT_SECRET;

	private static final String REDIRECT_URI = "http://localhost";
	private static final String YOUTUBE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
	private static final String YOUTUBE_EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";
	
	HttpTransport transport = new NetHttpTransport();
    private final JsonFactory jsonFactory = new GsonFactory();
    // FIXME security: we need to override the webviews cache, cookies, formdata cache to store only in sqlcipher/iocipher, currently it hits disk and then we clear it
    private WebView mWebview;
    
	private String mReturnedWebCode;
	private GoogleTokenResponse mAuthResp;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_youtube_login);
        CLIENT_ID = getString(R.string.google_client_id);
        CLIENT_SECRET = getString(R.string.google_client_secret);
		login();
	}

	@SuppressLint("SetJavaScriptEnabled")
	public void login() {
		mWebview = new WebView(this);
		mWebview.getSettings().setJavaScriptEnabled(true);
		mWebview.setVisibility(View.VISIBLE);

		setContentView(mWebview);

		List<String> scopes = new ArrayList<String>();
		scopes.add(YouTubeScopes.YOUTUBE_UPLOAD);
		scopes.add(YOUTUBE_EMAIL_SCOPE);
			
		String authUrl = new GoogleAuthorizationCodeRequestUrl(CLIENT_ID, REDIRECT_URI, scopes).build();

		// WebViewClient must be set BEFORE calling loadUrl!
		mWebview.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView view, String url, Bitmap bitmap) {
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				if (url.startsWith(REDIRECT_URI)) {
					if (url.indexOf("code=") != -1) {
						if (mReturnedWebCode == null) {
							mReturnedWebCode = extractCodeFromUrl(url);
							view.setVisibility(View.INVISIBLE);
							new Thread(YoutubeLoginActivity.this).start();
							
							//must be done on the main thread
							Util.clearWebviewAndCookies(mWebview, YoutubeLoginActivity.this);
						}
					} else if (url.indexOf("error=") != -1) {
						view.setVisibility(View.INVISIBLE);
						mAccessResult = RESULT_CANCELED;
					}
				}
			}

			private String extractCodeFromUrl(String url) {
				return url.substring(REDIRECT_URI.length() + 7, url.length());
			}
		});

		mWebview.loadUrl(authUrl);
	}

	public void run() {
		try {
			mAuthResp = new GoogleAuthorizationCodeTokenRequest(transport, 
																jsonFactory, 
																CLIENT_ID, 
																CLIENT_SECRET, 
																mReturnedWebCode, REDIRECT_URI).execute();
			
			String userEmail = getUserEmail(mAuthResp.getAccessToken());
			
	        if (null != userEmail) {
	            mAccessResult = RESULT_OK;
	            mAccessToken = userEmail;
	        } else { // failed login
	            mAccessResult = RESULT_CANCELED;
	        }
			
		} catch (IOException ioe) {
			Log.e(TAG, "Authentication Error: " + ioe.getMessage());
		}

		finish();
	}
	
	public String getUserEmail(String accessToken) {
		String userEmail = null;
	    HttpClient httpClient = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(YOUTUBE_USER_INFO_URL + "?access_token=" + accessToken);

	    try {
	    	ResponseHandler<String> responseHandler = new BasicResponseHandler();
	    	String responseBody = httpClient.execute(httpGet, responseHandler);
			JSONObject responseJson = new JSONObject(responseBody);
			
			userEmail = responseJson.getString("email");
	        
	    } catch (ClientProtocolException cpe) {
	    	Log.e(TAG, "ClientProtocolException: " + cpe.getMessage());
	    } 
	    catch (IOException ioe) {
	    	Log.e(TAG, "IOException: " + ioe.getMessage());
	    } 
	    catch (JSONException jsone) {
	    	Log.e(TAG, "JSONException: " + jsone.getMessage());
	    }
	    
	    return userEmail;
	}
	
	@Override
	public void finish() {
		Log.d(TAG, "finish()");
			
		Intent data = new Intent();
        data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, mAccessToken);
        setResult(mAccessResult, data);;

		super.finish();
	}
}