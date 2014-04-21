package io.scal.secureshareui.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import io.scal.secureshareui.lib.FacebookActivity;
import io.scal.secureshareui.model.PublishAccount;
 
public class FacebookPublishController extends PublishController  {
	
	public FacebookPublishController() {}

	@Override
	public void startAuthentication(PublishAccount account) {
	
		Context currentContext = super.getContext();
		Intent intent = new Intent(currentContext, FacebookActivity.class);
		((Activity) currentContext).startActivityForResult(intent, super.getControllerRequestCode());
	}	
}
