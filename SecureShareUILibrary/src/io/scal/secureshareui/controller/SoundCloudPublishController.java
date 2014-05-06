package io.scal.secureshareui.controller;

import io.scal.secureshareui.login.SoundcloudLoginActivity;
import io.scal.secureshareui.model.PublishAccount;
import io.scal.secureshareui.soundcloud.ApiWrapper;
import io.scal.secureshareui.soundcloud.Endpoints;
import io.scal.secureshareui.soundcloud.Params;
import io.scal.secureshareui.soundcloud.Request;
import io.scal.secureshareui.soundcloud.Token;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;



public class SoundCloudPublishController extends PublishController{
	public static final String SITE_NAME = "SoundCloud"; 
    public static final String SITE_KEY = "soundcloud";
    private static final String TAG = "SoundCloudPublishController";
    
    //soundcloud
    private static final String APP_CLIENT_ID = "e2d4d40b81830314350f0154bc88977a";
    private static final String APP_CLIENT_SECRET = "00aea4c562e3561614f1d177f1e672a7";
    
	// netcipher
	private static final String ORBOT_HOST = "127.0.0.1";
	private static final int ORBOT_HTTP_PORT = 8118;
	private static final int ORBOT_SOCKS_PORT = 9050;
	
    
    //NEED to change to test local audio file
    private static File mAudioFile = new File (Environment.getExternalStorageDirectory().getPath() + "/DCIM/ink.mp3");
    
	@Override
	public void startAuthentication(PublishAccount account) {	
		Context currentContext = super.getContext();
		Intent intent = new Intent(currentContext, SoundcloudLoginActivity.class);
		intent.putExtra("credentials", account.getCredentials());
		((Activity) currentContext).startActivityForResult(intent, PublishController.CONTROLLER_REQUEST_CODE);	
	}
	
	@Override
	public void upload(String title, String body, String filepath) {
		new UploadAsync().execute("");
	}

	private class UploadAsync extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			uploadFile();
			return "success";
		}

		@Override
		protected void onPostExecute(String result) {
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}
	}

	private void uploadFile() {

		String userName = "milucas22";
		String userPass = "bama1986";

		final ApiWrapper wrapper = new ApiWrapper(APP_CLIENT_ID, // client_id
				APP_CLIENT_SECRET, // client_secret
				null, // redirect URI
				null);// token

		URI uri = null;
		try {
			uri = new URI("http", null, ORBOT_HOST, ORBOT_HTTP_PORT, null,
					null, null);

		} catch (URISyntaxException e) {
			Log.v(TAG, "URISyntaxException: " + e.toString());
		}

		//wrapper.setProxy(uri);

		try {
			Token token = wrapper.login(userName, userPass);

			HttpResponse resp2 = wrapper.post(Request.to(Endpoints.TRACKS)
					.add(Params.Track.TITLE, mAudioFile.getName())
					.add(Params.Track.TAG_LIST, "storymaker upload")
					.withFile(Params.Track.ASSET_DATA, mAudioFile));

		} catch (IOException e) {
			Log.v(TAG, "IOException: " + e.toString());
		}
	}
}

