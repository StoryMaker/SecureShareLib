package io.scal.secureshareui.controller;

import android.content.Context;
import android.content.Intent;
import io.scal.secureshareui.lib.FacebookActivity;
import io.scal.secureshareui.model.PublishAccount;

public class FacebookPublishController extends PublishController  {
	
	public FacebookPublishController() {
	}

	@Override
	public void startAuthentication(Context context, PublishAccount account) {
		
		if(account.getId() == "1") {
			Intent intent = new Intent(context, FacebookActivity.class);
			context.startActivity(intent);		
		}
		
		super.getOnPublishEventListener().onSuccess(account);
	}
}
