package io.scal.secureshareui.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.util.EntityUtils;
import io.scal.secureshareui.lib.ZWrapper;
import io.scal.secureshareui.login.ZLoginActivity;
import io.scal.secureshareui.login.ZWebActivity;
import io.scal.secureshareui.model.Account;
import io.scal.secureshareuilibrary.R;

public class ZSiteController extends SiteController {
    private static final String TAG = "ZSiteController";
    public static final String SITE_NAME = "ZamanehOAuth";
    public static final String SITE_KEY = "zamanehoauth";

    public final String key;
    public final String secret;

    public static final String VALUE_KEY_DESC = "desc";

    public ZSiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
        key = mContext.getString(R.string.z_key);
        secret = mContext.getString(R.string.z_secret);
    }
    
    @Override
    public void startAuthentication(Account account) {
        Log.d(TAG, "startAuthentication()");
        
        Intent intent = new Intent(mContext, ZLoginActivity.class);
        //Intent intent = new Intent(mContext, ZWebActivity.class);
        intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
        ((Activity)mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE);
    }


    
    @Override
    public void upload(Account account, HashMap<String, String> valueMap) {
        Log.d(TAG, "upload()");

        String title = valueMap.get(VALUE_KEY_TITLE);
        String desc = valueMap.get(VALUE_KEY_DESC); // DOES NOT EXIST?
        String body = valueMap.get(VALUE_KEY_BODY);
        //String mediaPath = valueMap.get(VALUE_KEY_MEDIA_PATH);
        boolean useTor = (valueMap.get(VALUE_KEY_USE_TOR).equals("true")) ? true : false;

        new UploadAsync().execute(title, desc, body, account.getCredentials(), Boolean.valueOf(useTor).toString()); // credentials = token?

        // NEED TO REVISE

        /*
		Log.d(TAG, "Upload file: Entering upload");
		
		String title = valueMap.get(VALUE_KEY_TITLE);
		String body = valueMap.get(VALUE_KEY_BODY);
		String mediaPath = valueMap.get(VALUE_KEY_MEDIA_PATH);
		boolean useTor = (valueMap.get(VALUE_KEY_USE_TOR).equals("true")) ? true : false;
		
        String path = Environment.getExternalStorageDirectory() + File.separator + "sm.conf"; // FIXME this should probably be stored on protected internal storage... or perhaps IOCipher
        
        Log.d(TAG, "upload() path: " + path);
        
        File confFile = new File(path);
        SMProperties smProps = new SMProperties(confFile);
        sm = new SMManager(key,                           // key
                           secret,                        // secret
                           "http://localhost/callback", // callback (was http://localhost/callback?)
                           smProps);                      // properties

        if(torCheck(useTor, mContext))
        {    
            Log.d(TAG, "setting proxy");
            
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ORBOT_HOST, ORBOT_HTTP_PORT));
            sm.setProxy(proxy);
        }
        else
        {
            Log.d(TAG, "proxy not set");
        }
        
        // token stored in properties?  let's assume so for now...
        
        UploadFileTask ufTask = new UploadFileTask(this);
        ufTask.execute(title, body, mediaPath, account.getCredentials());
        */

    }

    private class UploadAsync extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Log.d(TAG, "doInBackground()");

            uploadFile(params[0], params[1], params[2], params[3], params[4]);
            return "success";
        }
    }

    private void uploadFile(String title, String desc, String body, String credentials, String useTor) {
        Log.d(TAG, "uploadFile()");

        final ZWrapper wrapper = new ZWrapper(key, secret, credentials);

        if (torCheck(Boolean.parseBoolean(useTor), mContext)) {
            URI uri = null;
            try {
                uri = new URI("http", null, ORBOT_HOST, ORBOT_HTTP_PORT, null, null, null);

            } catch (URISyntaxException use) {
                Log.v(TAG, "URISyntaxException: " + use.toString());
            }

            Log.d(TAG, "USE TOR...");

            // TODO: wrapper.setProxy(uri);
        }

        try {

            HttpResponse response = wrapper.upload(title, desc, body, credentials, mContext);

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
            jobFailed(98239723, e.toString()); // FIXME need new error code? (same value as soundcloud)
        }
    }

    // NEED TO REVISE

    /*
    class UploadFileTask extends AsyncTask<String, String, String> 
    {
        private SMSiteController smsc;
        
        public UploadFileTask(SMSiteController smsc)
        {
            this.smsc = smsc;
        }
        
        @Override
        protected String doInBackground(String... params) 
        {
            String result = smsc.uploadFile(params[0], params[1], params[2], params[3]);
            if ((result != null) && !result.equals("")) {
                jobSucceeded(result);
                return "success";
            } else {
                jobFailed(2938732, "Null or empty result from StoryMaker upload."); // FIXME error code?
                return "fail";
            }
        }
    }

    public String uploadFile(String title, String body, String mediaPath, String credentials) 
    {
        Log.d(TAG, "uploadFile() path: " + mediaPath);

        File photoFile = new File(mediaPath);
        SMService sms = sm.getSMService();

        // NEED TO SYNCH ARGUMENTS/PARAMETERS(?)
        // UNSURE IF THIS NEEDS TO SUPPORT UPLOADING FILES
        String response = sms.doUpload(title, "description", "foo", new String[1], "medium", "service", "guid"); // us.uploadPhoto(photoFile, title, body); // IS THIS THE CORRECT USE OF "body"?

         // error checking?
        return response;
    }
    */

    @Override
    public void startMetadataActivity(Intent intent) {
        return; // nop
    }
}
