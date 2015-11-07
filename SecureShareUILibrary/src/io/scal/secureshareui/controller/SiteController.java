
package io.scal.secureshareui.controller;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import timber.log.Timber;

import io.scal.secureshareui.model.Account;
import io.scal.secureshareuilibrary.R;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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
    public static final String MESSAGE_KEY_EXCEPTION = "exception";
    public static final String EXTRAS_KEY_DATA = "data";
    public static final String EXTRAS_KEY_USERNAME = "username";
    public static final String EXTRAS_KEY_CREDENTIALS = "credentials";

    public static final String VALUE_KEY_TITLE = "title";
    public static final String VALUE_KEY_SLUG = "slug";
	public static final String VALUE_KEY_BODY = "body";
	public static final String VALUE_KEY_TAGS = "tags";
	public static final String VALUE_KEY_AUTHOR = "author";
	public static final String VALUE_KEY_PROFILE_URL = "profileUrl";
	public static final String VALUE_KEY_LOCATION_NAME = "locationName";
	public static final String VALUE_KEY_MEDIA_PATH = "mediaPath";
	public static final String VALUE_KEY_USE_TOR = "use_tor";
	public static final String VALUE_KEY_LICENSE_URL = "licenseUrl";
	public static final String VALUE_KEY_PUBLISH_TO_STORYMAKER = "publish_to_storymaker"; // TODO generalize this, allow clie
    
    protected static final String ORBOT_HOST = "127.0.0.1";
    protected static final int ORBOT_HTTP_PORT = 8118;
    protected static final int ORBOT_SOCKS_PORT = 9050;
    
    public static int METADATA_REQUEST_CODE = 24153;

    private static final String TAG = "SiteController";

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
    
    /**
     * Gives a SiteController a chance to add metadata to the intent resulting from the ChooseAccounts process
     * that gets passed to each SiteController during publishing
     * @param intent   
     */
    public abstract void startMetadataActivity(Intent intent);

    public abstract void upload(Account account, HashMap<String, String> valueMap);

    public static boolean torCheck(boolean useTor, Context mContext) {

        if(useTor && OrbotHelper.isOrbotRunning(mContext))
        {    
            Timber.d("use tor");
            return true;
        }
        else
        {            
            Timber.d("don't use tor");
            return false;
        }
    }   
    
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
        else if (site.equals(ArchiveSiteController.SITE_KEY)) {
            return new ArchiveSiteController(context, handler, jobId);
        } else if (site.equals(ZTSiteController.SITE_KEY)) {
            return new ZTSiteController(context, handler, jobId);
        } else if (site.equals(S3SiteController.SITE_KEY)) {
            return new S3SiteController(context, handler, jobId);
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

    public void jobFailed(Exception exception, int errorCode, String errorMessage) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_FAILURE);
        data.putString(MESSAGE_KEY_JOB_ID, mJobId);
        data.putInt(MESSAGE_KEY_CODE, errorCode);
        data.putString(MESSAGE_KEY_MESSAGE, errorMessage);
        data.putSerializable("exception", (Serializable) exception);
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
    
    public static int getAccountIcon(String site, boolean isConnected, boolean areCredentialsValid) {
        if (site.equals(FacebookSiteController.SITE_KEY)) {     	
        	if (!isConnected) { //not connected
        		return R.drawable.ic_context_facebook;
        	}
        	//since connected, check if valid
        	return areCredentialsValid ? R.drawable.ic_context_facebook_on : R.drawable.ic_context_facebook_error;
        }
        else if (site.equals(YoutubeSiteController.SITE_KEY)) {
        	if (!isConnected) {
        		return R.drawable.ic_context_youtube;
        	}
        	return areCredentialsValid ? R.drawable.ic_context_youtube_on : R.drawable.ic_context_youtube_error;
        }
        else if (site.equals(SoundCloudSiteController.SITE_KEY)) {
        	if (!isConnected) {
        		return R.drawable.ic_context_soundcloud;
        	}
        	return areCredentialsValid ? R.drawable.ic_context_soundcloud_on : R.drawable.ic_context_soundcloud_error;
        }
        else if (site.equals(FlickrSiteController.SITE_KEY)) {
        	if (!isConnected) {
        		return R.drawable.ic_context_flickr;
        	}
        	return areCredentialsValid ? R.drawable.ic_context_flickr_on : R.drawable.ic_context_flickr_error;
        }
        else if (site.equals(SSHSiteController.SITE_KEY)) {
        	if (!isConnected) {
        		return R.drawable.ic_context_server;
        	}
        	return areCredentialsValid ? R.drawable.ic_context_server_on : R.drawable.ic_context_server_error;
        }
        else if (site.equals(ArchiveSiteController.SITE_KEY)) {
            if (!isConnected) {
                return R.drawable.ic_context_iarchive;
            }
            return areCredentialsValid ? R.drawable.ic_context_iarchive_on : R.drawable.ic_context_iarchive_error;
        }
        else if (site.equals(S3SiteController.SITE_KEY)) {
            if (!isConnected) {
                return R.drawable.ic_context_server; // FIXME we need a storymaker server icon
            }
            return areCredentialsValid ? R.drawable.ic_context_iarchive_on : R.drawable.ic_context_iarchive_error;
        }
        else if (site.equals(ZTSiteController.SITE_KEY)) {
            if (!isConnected) {
                return R.drawable.ic_context_server;
            }
            return areCredentialsValid ? R.drawable.ic_context_server_on : R.drawable.ic_context_server_error;
        }

        return R.drawable.ic_launcher;
    }
}
