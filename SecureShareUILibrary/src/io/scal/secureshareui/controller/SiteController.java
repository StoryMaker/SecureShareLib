package io.scal.secureshareui.controller;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import io.scal.secureshareui.login.FacebookLoginActivity;
import io.scal.secureshareui.model.Account;

public abstract class SiteController {
	private OnPublishEventListener mPublishEventListener;
	protected Context mContext;
	protected Handler mHandler;
	protected String mJobId;      // this is whatever the app wants it to be, we'll pass it back with our callbacks  
	public static final int CONTROLLER_REQUEST_CODE = 101;
	public static final int MESSAGE_TYPE_SUCCESS = 23423430;
	public static final int MESSAGE_TYPE_FAILURE = 23423431;
	public static final int MESSAGE_TYPE_PROGRESS = 23423432;
    public static final String MESSAGE_KEY_TYPE = "message_type";
    public static final String MESSAGE_KEY_JOB_ID = "job_id";
    public static final String MESSAGE_KEY_CODE = "code";
    public static final String MESSAGE_KEY_MESSAGE = "message";
    public static final String MESSAGE_KEY_RESULT = "result";
    public static final String MESSAGE_KEY_PROGRESS = "progress";
	
	public interface OnPublishEventListener {
		public void onSuccess(Account publishAccount);
		public void onFailure(Account publishAccount, String failureMessage);
	}
	
	public SiteController(Context context, Handler handler, String jobId) {
        mContext = context;
        mHandler = handler;
        mJobId = jobId;
	}
	
	public abstract void startAuthentication(Account account);
	public abstract void upload(String title, String body, String mediaPath, String username, String credentials);
    
    public static SiteController getPublishController(String site, Context context, Handler handler, String jobId) {
    	if(site.equals(FacebookSiteController.SITE_KEY)) {
    		return new FacebookSiteController(context, handler, jobId);
		}
    	else if(site.equals(SoundCloudSiteController.SITE_KEY)) {
    		return new SoundCloudSiteController(context, handler, jobId);
		}
    	else if(site.equals(FlickrSiteController.SITE_KEY)) {
            return new FlickrSiteController(context, handler, jobId);
        }
    	 	
    	return null;
    }
	
	public OnPublishEventListener getOnPublishEventListener() {
		return this.mPublishEventListener;
	}
	
	public void setOnPublishEventListener(OnPublishEventListener publishEventListener) {
		this.mPublishEventListener = publishEventListener;
	}
	
	/**
	 * result is a site specific unique id that we can use to fetch the data, build an embed tag, etc.  for some sites this might be a URL
	 * @param result
	 */
    public void jobSucceeded(String result) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_SUCCESS);
        data.putString(MESSAGE_KEY_JOB_ID, mJobId);
        data.putString(MESSAGE_KEY_RESULT, result);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    public void jobFailed(int errorCode, String errorMessage) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_FAILURE);
        data.putString(MESSAGE_KEY_JOB_ID, mJobId);
        data.putInt(MESSAGE_KEY_CODE, errorCode);
        data.putString(MESSAGE_KEY_MESSAGE, errorMessage);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }
    
    public void jobProgress(int progress, String message) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_PROGRESS);
        data.putString(MESSAGE_KEY_JOB_ID, mJobId);
        data.putInt(MESSAGE_KEY_PROGRESS, progress);
        data.putString(MESSAGE_KEY_RESULT, message);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }
}
