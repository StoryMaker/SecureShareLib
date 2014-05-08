package io.scal.secureshareui.controller;

import android.content.Context;
import android.content.Intent;
import io.scal.secureshareui.login.FacebookLoginActivity;
import io.scal.secureshareui.model.PublishAccount;

public abstract class PublishController {
	private OnPublishEventListener mPublishEventListener;
	private Context mContext;
	public static final int CONTROLLER_REQUEST_CODE = 101;
	
	public interface OnPublishEventListener {
		public void onSuccess(PublishAccount publishAccount);
		public void onFailure(PublishAccount publishAccount, String failureMessage);
	}
	
	public abstract void startAuthentication(PublishAccount account);
	public abstract void upload(String title, String body, String mediaPath, String credentials);
    
    public static PublishController getPublishController(String site) {	
    	if(site.equals(FacebookPublishController.SITE_KEY)) {
    		return new FacebookPublishController();
		}
    	else if(site.equals(SoundCloudPublishController.SITE_KEY)) {
    		return new SoundCloudPublishController();
		}
    	 	
    	return null;
    }
	
	public OnPublishEventListener getOnPublishEventListener() {
		return this.mPublishEventListener;
	}
	public void setOnPublishEventListener(OnPublishEventListener publishEventListener) {
		this.mPublishEventListener = publishEventListener;
	}
	
	public Context getContext() {
		return this.mContext;
	}
	public void setContext(Context context) {
		this.mContext = context;
	}
}
