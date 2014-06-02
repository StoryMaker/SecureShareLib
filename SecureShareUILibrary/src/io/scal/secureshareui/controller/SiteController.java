
package io.scal.secureshareui.controller;

import io.scal.secureshareui.model.Account;

import java.io.File;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.webkit.MimeTypeMap;

public abstract class SiteController {
    private OnEventListener mPublishEventListener;
    protected Context mContext;
    protected Handler mHandler;
    protected String mJobId; // this is whatever the app wants it to be, we'll pass it back with our callbacks
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
    public static final String EXTRAS_KEY_DATA = "data";
    public static final String EXTRAS_KEY_USERNAME = "username";
    public static final String EXTRAS_KEY_CREDENTIALS = "credentials";

    public interface OnEventListener {
        public void onSuccess(Account publishAccount);

        public void onFailure(Account publishAccount, String failureMessage);
        
        public void onRemove(Account account);
    }

    public SiteController(Context context, Handler handler, String jobId) {
        mContext = context;
        mHandler = handler;
        mJobId = jobId;
    }

    public abstract void startAuthentication(Account account);

    public abstract void upload(String title, String body, String mediaPath, Account account);

    public static SiteController getSiteController(String site, Context context, Handler handler, String jobId) {
        if (site.equals(FacebookSiteController.SITE_KEY)) {
            return new FacebookSiteController(context, handler, jobId);
        }
        else if (site.equals(SoundCloudSiteController.SITE_KEY)) {
            return new SoundCloudSiteController(context, handler, jobId);
        }
        else if (site.equals(YoutubeSiteController.SITE_KEY)) {
            return new YoutubeSiteController(context, handler, jobId);
        }
        else if (site.equals(FlickrSiteController.SITE_KEY)) {
            return new FlickrSiteController(context, handler, jobId);
        }
        else if (site.equals(SSHSiteController.SITE_KEY)) {
            return new SSHSiteController(context, handler, jobId);
        }

        return null;
    }
    
    public static boolean isAudioFile(File mediaFile) {
    	if(getMimeType(mediaFile).contains("audio")) {
    		return true;
    	}
    	return false;
    }
    
    public static boolean isImageFile(File mediaFile) {
    	if(getMimeType(mediaFile).contains("image")) {
    		return true;
    	}
    	return false;
    }
    
    public static boolean isVideoFile(File mediaFile) {
    	if(getMimeType(mediaFile).contains("video")) {
    		return true;
    	}
    	return false;
    }
    
    private static String getMimeType(File mediaFile) {
    	Uri fileUri = Uri.fromFile(mediaFile);
    	String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
    	return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
    }

    public OnEventListener getOnPublishEventListener() {
        return this.mPublishEventListener;
    }

    public void setOnEventListener(OnEventListener publishEventListener) {
        this.mPublishEventListener = publishEventListener;
    }

    /**
     * result is a site specific unique id that we can use to fetch the data,
     * build an embed tag, etc. for some sites this might be a URL
     * 
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

    public void jobProgress(float progress, String message) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_PROGRESS);
        data.putString(MESSAGE_KEY_JOB_ID, mJobId);
        data.putFloat(MESSAGE_KEY_PROGRESS, progress);
        data.putString(MESSAGE_KEY_MESSAGE, message);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }
}
