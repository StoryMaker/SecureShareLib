package io.scal.secureshareui.controller;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;
import io.scal.secureshareui.lib.FacebookActivity;
import io.scal.secureshareui.model.PublishAccount;
import io.scal.secureshareuilibrary.R;
 
public class FacebookPublishController extends PublishController  {
	public static final String SITE_NAME = "Facebook";
	public static final String SITE_KEY = "facebook"; 
	public FacebookPublishController() {}

	@Override
	public void startAuthentication(PublishAccount account) {	
		Context currentContext = super.getContext();
		Intent intent = new Intent(currentContext, FacebookActivity.class);
		intent.putExtra("credentials", account.getCredentials());
		((Activity) currentContext).startActivityForResult(intent, PublishController.CONTROLLER_REQUEST_CODE);
	}
	
	@Override
	public void upload(String title, String body, String videoPath) {
		
	}
	
	private void publishImage() {
		
		Session session = Session.getActiveSession();
		final String fbPhotoAddress;

		// Part 1: create callback to get URL of uploaded photo
		Request.Callback uploadPhotoRequestCallback = new Request.Callback() {
			@Override
			public void onCompleted(Response response) {

				if (response.getError() != null) { // [IF Failed Posting]
					Log.d("testtest",
							"photo upload problem. Error="
									+ response.getError());
				} // [ENDIF Failed Posting]

				Object graphResponse = response.getGraphObject().getProperty(
						"id");
				if (graphResponse == null || !(graphResponse instanceof String)
						|| TextUtils.isEmpty((String) graphResponse)) { // [IF
																		// Failed
																		// upload/no
																		// results]
					Log.d("testest", "failed photo upload/no response");
				} else { // [ELSEIF successful upload]
					String s = "https://www.facebook.com/photo.php?fbid=" + graphResponse;
				} // [ENDIF successful posting or not]
			} // [END onCompleted]
		};

		// Part 2: upload the photo
		//Request request = Request.newUploadPhotoRequest(session, BitmapFactory.decodeResource(getResources(), R.drawable.snoopy), uploadPhotoRequestCallback);
		//request.executeAsync();
	}
}
