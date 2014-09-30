package io.scal.secureshareui.login;

import io.scal.secureshareui.controller.SiteController;

import java.io.IOException;

import org.holoeverywhere.app.Activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class ZonkeyLoginActivity extends Activity {
	private static final String TAG = "ArchiveLoginActivity";
	
	private final static String ZONKEY_API_AUTHORIZE= "www.api.endpoint/authorize";
	private final static String ZONKEY_API_TOKEN= "www.api.endpoint/request_token";
	private final static String ZONKEY_API_ACCESS= "www.api.endpoint/request_access";

	private int mAccessResult = Activity.RESULT_CANCELED;
	private String mAccessKey = null;
    private String mSecretKey = null;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		authorize();
	}

	private void authorize() {
		OkHttpClient client = new OkHttpClient();
		Request.Builder builder = new Request.Builder()
				.url(ZONKEY_API_AUTHORIZE)
				.addHeader("client_id", "need to get the client id")
				.addHeader("response_type", "code")
                .addHeader("state", "anything here?");
		
		Request request = builder.build();
		HTTPTask httpTask = new HTTPTask(client, request);
		httpTask.execute();
	}
	
	private void requestToken() {
		OkHttpClient client = new OkHttpClient();
		Request.Builder builder = new Request.Builder()
				.url(ZONKEY_API_TOKEN)
				.addHeader("code", "code from authorize()")
                .addHeader("grant_type", "")
				.addHeader("client_id", "need to get client id")
				.addHeader("client_secret", "client secret from authorize()");
		
		Request request = builder.build();
		HTTPTask httpTask = new HTTPTask(client, request);
		httpTask.execute();
	}
	
	private void requestAccess() {
		OkHttpClient client = new OkHttpClient();
		Request.Builder builder = new Request.Builder()
				.url(ZONKEY_API_ACCESS)
				.addHeader("accessToken", "token from requestToken()");
		
		Request request = builder.build();
		HTTPTask httpTask = new HTTPTask(client, request);
		httpTask.execute();
	}
	
	

	class HTTPTask extends AsyncTask<String, String, String> {
		private OkHttpClient client;
		private Request request;
		private Response response;

		public HTTPTask(OkHttpClient client, Request request) {
			this.client = client;
			this.request = request;
		}

		@Override
		protected String doInBackground(String... params) {
			Log.d(TAG, "Begin Authentication");

			try {
				response = client.newCall(request).execute();
                Log.d(TAG, "response: " + response + ", body: " + response.body().string());
				if (!response.isSuccessful()) {
					
				} else {
					
				}
			} catch (IOException e) {
				try {
					Log.d(TAG, response.body().string());
				} catch (IOException e1) {
				    Log.d(TAG, "exception: " + e1.getLocalizedMessage() + ", stacktrace: " + e1.getStackTrace());
				}
			}

			return "-1";
		}
	}

	@Override
	public void finish() {
		Log.d(TAG, "finish()");
		
		Intent data = new Intent();
		data.putExtra(SiteController.EXTRAS_KEY_USERNAME, mAccessKey);
		data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, mSecretKey);
		setResult(mAccessResult, data);
		
		super.finish();
	}
}
