
package io.scal.secureshareui.controller;

import io.scal.secureshareui.login.SoundCloudLoginActivity;
import io.scal.secureshareui.model.Account;
import io.scal.secureshareui.soundcloud.ApiWrapper;
import io.scal.secureshareui.soundcloud.Endpoints;
import io.scal.secureshareui.soundcloud.Params;
import io.scal.secureshareui.soundcloud.Request;
import io.scal.secureshareui.soundcloud.Token;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class SoundCloudSiteController extends SiteController {
    private static final String TAG = "SoundCloudSiteController";
    public static final String SITE_NAME = "SoundCloud";
    public static final String SITE_KEY = "soundcloud";

    // TODO move soundcloud SM credentials into xml
    public static final String APP_CLIENT_ID = "e2d4d40b81830314350f0154bc88977a";  // FIXME move these into xml files that are .gitignored
    public static final String APP_CLIENT_SECRET = "00aea4c562e3561614f1d177f1e672a7";

    
    public SoundCloudSiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
    }

    @Override
    public void startAuthentication(Account account) {
        Intent intent = new Intent(mContext, SoundCloudLoginActivity.class);
        intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
        ((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE); // FIXME not a safe cast, context might be a service  
    }

    @Override
    public void upload(Account account, HashMap<String, String> valueMap) {
		Log.d(TAG, "Upload file: Entering upload");
		
		String title = valueMap.get(VALUE_KEY_TITLE);
		String body = valueMap.get(VALUE_KEY_BODY);
		String mediaPath = valueMap.get(VALUE_KEY_MEDIA_PATH);
		boolean useTor = Boolean.getBoolean(valueMap.get(VALUE_KEY_USE_TOR));
		
        new UploadAsync().execute(title, body, mediaPath, account.getCredentials(), Boolean.valueOf(useTor).toString());
    }

    private class UploadAsync extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            uploadFile(params[0], params[1], params[2], params[3], params[4]);
            return "success";
        }
    }

    private void uploadFile(String title, String body, String mediaPath, String credentials, String useTor) {
        final ApiWrapper wrapper = new ApiWrapper(APP_CLIENT_ID,    // client_id
                APP_CLIENT_SECRET,                                  // client_secret
                null,                                               // redirect URI
                new Token(credentials, "0"));                       // token

        if (torCheck(Boolean.parseBoolean(useTor), mContext)) {
            URI uri = null;
            try {
                uri = new URI("http", null, ORBOT_HOST, ORBOT_HTTP_PORT, null, null, null);

            } catch (URISyntaxException e) {
                Log.v(TAG, "URISyntaxException: " + e.toString());
            }

            wrapper.setProxy(uri);
        }

        File audioFile = new File(mediaPath);

        if (audioFile.exists()) {
            try {
                HttpResponse response = wrapper.post(Request.to(Endpoints.TRACKS)
                        .add(Params.Track.TITLE, title)
                        .add(Params.Track.TAG_LIST, "storymaker upload")
                        .withFile(Params.Track.ASSET_DATA, audioFile));
                int code = response.getStatusLine().getStatusCode();
                if ((code >= 200) && (code < 300)) {
                    HttpEntity entity = response.getEntity();
                    String responseString = EntityUtils.toString(entity, "UTF-8");
                    jobSucceeded(responseString);
                } else {
                    Log.d(TAG, "upload failed: " + response.getStatusLine().toString());
                    jobFailed(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                }
            } catch (IOException e) {
                Log.v(TAG, "IOException: " + e.toString());
                jobFailed(98239723, e.toString()); // FIXME error code
            }
        }
    }

    @Override
    public void startMetadataActivity(Intent intent) {
        return; // nop
    }

}
