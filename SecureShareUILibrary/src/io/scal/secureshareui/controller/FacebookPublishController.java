package io.scal.secureshareui.controller;

import java.io.File;
import java.io.FileNotFoundException;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import io.scal.secureshareui.login.FacebookLoginActivity;
import io.scal.secureshareui.model.PublishAccount;
 
public class FacebookPublishController extends PublishController  {
	public static final String SITE_NAME = "Facebook";
	public static final String SITE_KEY = "facebook";
	private static final String TAG = "FacebookPublishController";
	
	public FacebookPublishController() {}

	@Override
	public void startAuthentication(PublishAccount account) {	
		Context currentContext = super.getContext();
		Intent intent = new Intent(currentContext, FacebookLoginActivity.class);
		intent.putExtra("credentials", account.getCredentials());
		((Activity) currentContext).startActivityForResult(intent, PublishController.CONTROLLER_REQUEST_CODE);
	}
	
	@Override
	public void upload(String title, String body, String mediaPath, String username, String credentials) {

		Session session = Session.openActiveSessionFromCache(super.getContext());
			
		//setup callback
		Request.Callback uploadVideoRequestCallback = new Request.Callback() {
			@Override
			public void onCompleted(Response response) {

				// post fail
				if (response.getError() != null) {
					Log.d(TAG, "photo upload problem. Error= "+ response.getError());
				}

				Object graphResponse = response.getGraphObject().getProperty("id");

				// upload fail
				if (graphResponse == null || !(graphResponse instanceof String)
						|| TextUtils.isEmpty((String) graphResponse)) {
					Log.d(TAG, "failed video upload/no response");
				}
				// upload success
				else {
					Log.d(TAG, "successful video upload: "+ graphResponse);
				}
			}
		};

		//upload File
		File videoFile = new File(mediaPath);
		Request request = null;
		try {
			request = Request.newUploadVideoRequest(session, videoFile, uploadVideoRequestCallback);
			Bundle parameters = request.getParameters();
			parameters.putString("description", body);
					
			request.setParameters(parameters);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		request.executeAsync();
	}		
}
