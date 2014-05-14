
package io.scal.secureshareui.controller;

import java.io.File;
import java.io.FileNotFoundException;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import io.scal.secureshareui.login.FacebookLoginActivity;
import io.scal.secureshareui.model.Account;

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
        Intent intent = new Intent(mContext, FacebookLoginActivity.class);
        intent.putExtra("credentials", account.getCredentials());
        ((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE); // FIXME not a safe cast, context might be a service
    }

    @Override
    public void upload(String title, String body, String mediaPath, String username, String credentials) {
        Session session = Session.openActiveSessionFromCache(mContext);

        // setup callback
        Request.Callback uploadVideoRequestCallback = new Request.OnProgressCallback() {
            @Override
            public void onCompleted(Response response) {

                // post fail
                if (response.getError() != null) {
                    Log.d(TAG, "photo upload problem. Error= " + response.getError());
                    jobFailed(1, response.getError().toString());
                    return;
                }

                Object graphResponse = response.getGraphObject().getProperty("id");

                // upload fail
                if (graphResponse == null || !(graphResponse instanceof String)
                        || TextUtils.isEmpty((String) graphResponse)) {
                    Log.d(TAG, "failed video upload/no response");

                    jobFailed(0, "failed video upload/no response");
                }
                // upload success
                else {
                    jobSucceeded("" + graphResponse);
                    Log.d(TAG, "successful video upload: " + graphResponse);
                }
            }

            @Override
            public void onProgress(long current, long max) {
                float percent = ((float) current) / ((float) max);
                jobProgress(percent, "Facebook uploading...");
            }
        };

        // upload File
        File videoFile = new File(mediaPath);
        Request request = null;
        try {
            request = Request.newUploadVideoRequest(session, videoFile, uploadVideoRequestCallback);
            Bundle parameters = request.getParameters();
            parameters.putString("description", body);

            request.setParameters(parameters);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        request.executeAsync();
    }
}
