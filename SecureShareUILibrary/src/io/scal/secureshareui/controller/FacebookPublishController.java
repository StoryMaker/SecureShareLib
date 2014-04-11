package io.scal.secureshareui.controller;

import io.scal.secureshareui.model.PublishAccount;

public class FacebookPublishController extends PublishController  {
	
	public FacebookPublishController() {
	}

	@Override
	public void startAuthentication(PublishAccount account) {
		super.getOnPublishEventListener().onSuccess(account);
	}
}
