package io.scal.secureshareui.login;

import io.scal.secureshareui.controller.SiteController;
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

@SuppressWarnings("deprecation")
public class YoutubeLoginActivity extends Activity implements Runnable {

	private static int mAccessResult = 0;
	private static String mAccessToken = null;

	static final String TAG = "YoutubeLoginActivity";
	
	private static final String CLIENT_ID = "279338940292-7pqin08vmde3nhheekijn6cfetknotbs.apps.googleusercontent.com";
	private static final String CLIENT_SECRET = "N3uZBIBjr__TPHUWUkmtBUYk";
	
	private static final String REDIRECT_URI = "http://localhost";
	private static final String YOUTUBE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
	private static final String YOUTUBE_EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";
	
	HttpTransport transport = new NetHttpTransport();
    private final JsonFactory jsonFactory = new GsonFactory();
	
	private String mReturnedWebCode;
	private GoogleTokenResponse mAuthResp;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_youtube_login);

		login();
	}

	public void login() {
		WebView webview = new WebView(this);
		webview.clearCache(true);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setVisibility(View.VISIBLE);
		setContentView(webview);

		List<String> scopes = new ArrayList<String>();
		scopes.add(YouTubeScopes.YOUTUBE_UPLOAD);
		scopes.add(YOUTUBE_EMAIL_SCOPE);
		
		
		String authUrl = new GoogleAuthorizationCodeRequestUrl(CLIENT_ID, REDIRECT_URI, scopes).build();

		// WebViewClient must be set BEFORE calling loadUrl!
		webview.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageStarted(WebView view, String url, Bitmap bitmap) {}

			@Override
			public void onPageFinished(WebView view, String url) {

				if (url.startsWith(REDIRECT_URI)) {
					if (url.indexOf("code=") != -1) {
						if (mReturnedWebCode == null) {
							mReturnedWebCode = extractCodeFromUrl(url);
							view.setVisibility(View.INVISIBLE);
							new Thread(YoutubeLoginActivity.this).start();
						}
					} else if (url.indexOf("error=") != -1) {
						view.setVisibility(View.INVISIBLE);
						setResult(RESULT_CANCELED);
					}
				}
			}

			private String extractCodeFromUrl(String url) {
				return url.substring(REDIRECT_URI.length() + 7, url.length());
			}
		});

		webview.loadUrl(authUrl);
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
	            mAccessResult = 0;
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

	public void finish() {
		Intent data = new Intent();
        data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, mAccessToken);
        setResult(mAccessResult, data);;

		super.finish();
	}
}