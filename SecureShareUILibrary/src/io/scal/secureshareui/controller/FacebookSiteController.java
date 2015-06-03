
package io.scal.secureshareui.controller;

import io.scal.secureshareui.login.FacebookLoginActivity;
import io.scal.secureshareui.login.NewFacebookLoginActivity;
import io.scal.secureshareui.model.Account;
import io.scal.secureshareuilibrary.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
//import com.facebook.Session;
//import com.facebook.model.GraphUser;
import com.facebook.login.LoginManager;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;

import org.json.JSONException;

public class FacebookSiteController extends SiteController {
    public static final String SITE_NAME = "Facebook";
    public static final String SITE_KEY = "facebook";
    private static final String TAG = "FacebookSiteController";

    public FacebookSiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void startAuthentication(Account account) {
        Intent intent = new Intent(mContext, NewFacebookLoginActivity.class);
        intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
        ((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE); // FIXME not a safe cast, context might be a service
    }

    @Override
    public void upload(Account account, HashMap<String, String> valueMap) {

        String title = valueMap.get(VALUE_KEY_TITLE);
        String body = valueMap.get(VALUE_KEY_BODY);
        String mediaPath = valueMap.get(VALUE_KEY_MEDIA_PATH);

        Log.d("FACEBOOK_DEBUG", "MEDIA FILE: " + mediaPath);

        File mediaFile = new File(mediaPath);

        if(super.isVideoFile(mediaFile)) {
            Log.d("FACEBOOK_DEBUG", "UPLOAD VIDEO FILE");

            Uri uri = Uri.parse(mediaPath);

            ShareVideo video = new ShareVideo.Builder().setLocalUrl(uri).build();

            ShareVideoContent content = new ShareVideoContent.Builder().setVideo(video).build();

            ShareApi.share(content, shareCallback);

        } else if(super.isImageFile(mediaFile)) {
            Log.d("FACEBOOK_DEBUG", "UPLOAD IMAGE FILE");

            Uri uri = Uri.parse(mediaPath);

            SharePhoto photo = new SharePhoto.Builder().setImageUrl(uri).build();

            ArrayList<SharePhoto> photos = new ArrayList<SharePhoto>();
            photos.add(photo);

            SharePhotoContent content = new SharePhotoContent.Builder().setPhotos(photos).build();

            ShareApi.share(content, shareCallback);

        } else {
            Log.d("FACEBOOK_DEBUG", "FILE TYPE NOT SUPPORTED");

        }

        // 4.0 API IS COMPLETELY DIFFERENT
        /*
		Log.d(TAG, "Upload file: Entering upload");
		
		String title = valueMap.get(VALUE_KEY_TITLE);
		String body = valueMap.get(VALUE_KEY_BODY);
		String mediaPath = valueMap.get(VALUE_KEY_MEDIA_PATH);
		boolean useTor = (valueMap.get(VALUE_KEY_USE_TOR).equals("true")) ? true : false;
        Session session = Session.openActiveSessionFromCache(mContext);

        // setup callback
        GraphRequest.Callback uploadMediaRequestCallback = new GraphRequest.OnProgressCallback() {
            @Override
            public void onCompleted(GraphResponse response) {

                // post fail
                if (response.getError() != null) {
                    Log.d(TAG, "media upload problem. Error= " + response.getError());
                    jobFailed(null, 1, response.getError().toString());
                    return;
                }

                Object graphResponse = null;
                try {
                    graphResponse = response.getJSONObject().get("id");

                    // upload fail
                    if (graphResponse == null || !(graphResponse instanceof String)
                            || TextUtils.isEmpty((String) graphResponse)) {
                        Log.d(TAG, "failed media upload/no response");
                        jobFailed(null, 0, "failed media upload/no response");
                    }
                    // upload success
                    else {
                        jobSucceeded("" + graphResponse);
                        Log.d(TAG, "successful media upload: " + graphResponse);
                    }
                } catch (JSONException je) {
                    Log.d(TAG, "failed to process response");
                    jobFailed(null, 0, "failed to process response");
                }
            }

            @Override
            public void onProgress(long current, long max) {
                float percent = ((float) current) / ((float) max);
                jobProgress(percent, "Facebook uploading...");
            }
        };

        // upload File
        File mediaFile = new File(mediaPath);
        Bundle parameters = null;    
        GraphRequest request = null;
        try {
        	if(super.isVideoFile(mediaFile)) {
        		request = GraphRequest.newUploadVideoRequest(session, mediaFile, uploadMediaRequestCallback);

                // DEAL WITH TOR LATER
        		if (torCheck(useTor, mContext))
        		{
        		    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ORBOT_HOST, ORBOT_HTTP_PORT));
                    request.setProxy(proxy);
        		}

        		parameters = request.getParameters();
        		
        		//video params
        		parameters.putString("title", title);
        		parameters.putString("description", body);
        	}
        	else if(super.isImageFile(mediaFile)){
        		request = Request.newUploadPhotoRequest(session, mediaFile, uploadMediaRequestCallback);
        		
        		if (torCheck(useTor, mContext))
                {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ORBOT_HOST, ORBOT_HTTP_PORT));
                    request.setProxy(proxy);
                }
        		
        		parameters = request.getParameters();
        		
        		//image params
        		parameters.putString("name", title);
        	}
        	else {
        		Log.d(TAG, "media type not supported");
        		return;
        	}
        	
            request.setParameters(parameters);
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        request.executeAsync();
        */
    }

    // TEMP
    private FacebookCallback<Sharer.Result> shareCallback = new FacebookCallback<Sharer.Result>() {
        @Override
        public void onCancel() {
            Log.d("FACEBOOK_DEBUG", "CANCELLED");
        }

        @Override
        public void onError(FacebookException error) {
            String alertMessage = error.getMessage();
            Log.d("FACEBOOK_DEBUG", "ERROR, MESSAGE: " + alertMessage);
        }

        @Override
        public void onSuccess(Sharer.Result result) {
            if (result.getPostId() != null) {
                String id = result.getPostId();
                Log.d("FACEBOOK_DEBUG", "SUCCESS, POST ID: " + id);
            } else {
                Log.d("FACEBOOK_DEBUG", "SUCCESS, NO POST ID?");
            }
        }
    };

    static String userId; // FIXME we should be caching this at login
    public static String getUserId(){

        // HOW DO WE GET THE USER ID OR POST URL?

        return "foo";

        /*
        final Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
            Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user, Response response) {
                    if (session == Session.getActiveSession()) {
                        if (user != null) {
                            userId = user.getId();
                        }
                    }
                }
            });
            Request.executeAndWait(request);
            return userId;
        }else{
            return null;
        }
        */
    }

    @Override
    public void startMetadataActivity(Intent intent) {
        return; // nop
    }

}
