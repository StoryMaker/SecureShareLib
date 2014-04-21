package io.scal.secureshareui.controller;

import android.content.Context;
import android.content.Intent;
import io.scal.secureshareui.lib.FacebookActivity;
import io.scal.secureshareui.model.PublishAccount;

public abstract class PublishController {
	private OnPublishEventListener mPublishEventListener;
	private Context mContext;
	private static final int CONTROLLER_REQUEST_CODE = 101;
	
	public interface OnPublishEventListener {
		public void onSuccess(PublishAccount publishAccount);
		public void onFailure(PublishAccount publishAccount, String failureMessage);
	}
	
	abstract void startAuthentication(PublishAccount account);
    
    public static PublishController getPublishController(String key) {	
    	if(key.equals("facebook.com")) {			
    		return new FacebookPublishController();
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
	
	public static int getControllerRequestCode() {
		return CONTROLLER_REQUEST_CODE;
	}
}
