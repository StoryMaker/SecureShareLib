package io.scal.secureshareui.controller;

import io.scal.secureshareui.lib.Util;
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
    
    //soundcloud SM credentials
    public static final String APP_CLIENT_ID = "e2d4d40b81830314350f0154bc88977a";
    public static final String APP_CLIENT_SECRET = "00aea4c562e3561614f1d177f1e672a7";
    
	@Override
	public void startAuthentication(PublishAccount account) {	
		Context currentContext = super.getContext();
		Intent intent = new Intent(currentContext, SoundcloudLoginActivity.class);
		intent.putExtra("credentials", account.getCredentials());
		((Activity) currentContext).startActivityForResult(intent, PublishController.CONTROLLER_REQUEST_CODE);	
	}
	
	@Override
	public void upload(String title, String body, String mediaPath, String credentials) {
		new UploadAsync().execute(title, body, mediaPath, credentials);
	}

	private class UploadAsync extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			uploadFile(params[0], params[1], params[2], params[3]);
			return "success";
		}
	}
	
	private void uploadFile(String title, String body, String mediaPath, String credentials) {
		
		final ApiWrapper wrapper = new ApiWrapper(APP_CLIENT_ID, // client_id
				APP_CLIENT_SECRET, // client_secret
				null, // redirect URI
				new Token(credentials, "0"));// token

		if(Util.isOrbotInstalledAndRunning(super.getContext())) {
			URI uri = null;
			try {
				uri = new URI("http", null, Util.ORBOT_HOST, Util.ORBOT_HTTP_PORT, null,
						null, null);

			} catch (URISyntaxException e) {
				Log.v(TAG, "URISyntaxException: " + e.toString());
			}

			wrapper.setProxy(uri);
		}
		
	    File audioFile = new File(mediaPath);
		
	    if(audioFile.exists()) {    	
	    	try {
				HttpResponse response = wrapper.post(Request.to(Endpoints.TRACKS)
						.add(Params.Track.TITLE, title)
						.add(Params.Track.TAG_LIST, "storymaker upload")
						.withFile(Params.Track.ASSET_DATA, audioFile));

			} catch (IOException e) {
				Log.v(TAG, "IOException: " + e.toString());
			}
	    }
		
	}
}

