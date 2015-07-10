
package io.scal.secureshareui.controller;

import io.scal.secureshareui.login.FacebookLoginActivity;
import io.scal.secureshareui.model.Account;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;

import org.json.JSONException;
import org.json.JSONObject;

public class FacebookSiteController extends SiteController {
    public static final String SITE_NAME = "Facebook";
    public static final String SITE_KEY = "facebook";
    private static final String TAG = "FacebookSiteController";

    public static final String POST_NOT_PUBLIC = "PostNotPublic";

    private String postId = null;

    public FacebookSiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void startAuthentication(Account account) {
        Intent intent = new Intent(mContext, FacebookLoginActivity.class);
        intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
        ((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE); // FIXME not a safe cast, context might be a service
    }

    @Override
    public void upload(Account account, HashMap<String, String> valueMap) {
		Log.d(TAG, "Upload file: Entering upload");
		
		String title = valueMap.get(VALUE_KEY_TITLE);
		String body = valueMap.get(VALUE_KEY_BODY);
		String mediaPath = valueMap.get(VALUE_KEY_MEDIA_PATH);
		boolean useTor = (valueMap.get(VALUE_KEY_USE_TOR).equals("true")) ? true : false;
        Session session = Session.openActiveSessionFromCache(mContext);

        // setup callback
        Request.Callback uploadMediaRequestCallback = new Request.OnProgressCallback() {
            @Override
            public void onCompleted(Response response) {

                // privacy check callback
                // ckeck to see if a video post is public
                Request.Callback privacyCheckRequestCallback = new Request.Callback() {
                    @Override
                    public void onCompleted(Response response) {

                        Log.d(TAG, "PRIVACY CHECK RESPONSE: " + response.toString());

                        // request will fail unless app has user_videos permission
                        // since this feature is incomplete, just continue
                        if (response.getError() != null) {

                            Log.e(TAG, "PRIVACY CHECK, ERROR: " + response.getError() + " (IGNORED)");
                            jobSucceeded(postId);
                            return;
                        }

                        JSONObject graphResponse = response.getGraphObject().getInnerJSONObject();

                        try {

                            String privacyValue = graphResponse.getString("privacy");

                            Log.d(TAG, "PRIVACY CHECK, FOUND: " + privacyValue);

                            if ((privacyValue != null) && (privacyValue.equals("everyone"))) {

                                // post is public, ok to publish
                                jobSucceeded(postId);

                            } else {

                                // post is not public, need to handle this somehow
                                jobSucceeded(POST_NOT_PUBLIC);

                            }

                        } catch (JSONException je) {

                            Log.e(TAG, "FAILED TO EXTRACT PRIVACY SETTINGS FROM RESPONSE: " + je.getMessage() + " (IGNORED)");

                            // currently unable to find privacy field in video responses, so i'm letting this through
                            jobSucceeded(postId);

                        }
                    }
                };


                // post fail
                if (response.getError() != null) {
                    Log.d(TAG, "media upload problem. Error= " + response.getError());
                    jobFailed(null, 1, response.getError().toString());
                    return;
                }

                Object graphResponse = response.getGraphObject().getProperty("id");

                // upload fail
                if (graphResponse == null || !(graphResponse instanceof String)
                        || TextUtils.isEmpty((String) graphResponse)) {
                    Log.d(TAG, "failed media upload/no response");

                    jobFailed(null, 0, "failed media upload/no response");
                }
                // upload success
                else {

                    postId = (String) graphResponse;

                    Log.d(TAG, "LOOKING FOR POST ID " + postId);

                    Session session = Session.openActiveSessionFromCache(mContext);
                    Bundle parameters = null;
                    Request request = new Request(session, postId, parameters, HttpMethod.GET, privacyCheckRequestCallback);

                    request.executeAsync();
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
        Request request = null;
        try {
        	if(super.isVideoFile(mediaFile)) {
        		request = Request.newUploadVideoRequest(session, mediaFile, uploadMediaRequestCallback);
        		
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
    }

    static String userId; // FIXME we should be caching this at login
    public static String getUserId(){
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

    }

    @Override
    public void startMetadataActivity(Intent intent) {
        return; // nop
    }

}
