package io.scal.secureshareui.controller;

import java.io.File;
import java.io.FileNotFoundException;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import io.scal.secureshareui.lib.FacebookActivity;

import io.scal.secureshareui.model.PublishAccount;
 
public class FacebookPublishController extends PublishController  {
	public static final String SITE_NAME = "Facebook";
	public static final String SITE_KEY = "facebook";
	private static final String TAG = "FacebookPublishController";
	
	public FacebookPublishController() {}

	@Override
	public void startAuthentication(PublishAccount account) {	
		Context currentContext = super.getContext();
		Intent intent = new Intent(currentContext, FacebookActivity.class);
		intent.putExtra("credentials", account.getCredentials());
		((Activity) currentContext).startActivityForResult(intent, PublishController.CONTROLLER_REQUEST_CODE);
	}
	
	public void upload(String title, String body, String filepath) {

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
					Log.d(TAG, "sucees video upload: "+ graphResponse);
				}
			}
		};

		//upload File
		File videoFile = new File(filepath);
		Request request = null;
		try {
			request = Request.newUploadVideoRequest(session, videoFile, uploadVideoRequestCallback);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		request.executeAsync();
	}		
}
