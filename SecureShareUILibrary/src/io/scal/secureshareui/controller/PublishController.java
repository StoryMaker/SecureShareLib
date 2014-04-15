package io.scal.secureshareui.controller;

import android.content.Context;
import io.scal.secureshareui.model.PublishAccount;

public abstract class PublishController {
	private OnPublishEventListener mPublishEventListener;
	
	public interface OnPublishEventListener {
		public void onSuccess(PublishAccount publishAccount);
		public void onFailure(PublishAccount publishAccount, String failureMessage);
	}
	
	public void setOnPublishEventListener(OnPublishEventListener publishEventListener) {
		this.mPublishEventListener = publishEventListener;
	}
	
	public OnPublishEventListener getOnPublishEventListener() {
		return this.mPublishEventListener;
	}
	
    abstract void startAuthentication(Context context, PublishAccount account);
}
