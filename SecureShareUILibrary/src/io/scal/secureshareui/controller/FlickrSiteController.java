package io.scal.secureshareui.controller;

import io.scal.secureshareui.login.FlickrLoginActivity;
import io.scal.secureshareui.model.Account;
import io.scal.secureshareuilibrary.R;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.flickr.api.Flickr;
import com.flickr.api.FlickrException;
import com.flickr.api.FlickrProperties;
import com.flickr.api.UploadService;

public class FlickrSiteController extends SiteController {
    public static final String SITE_NAME = "Flickr"; 
    public static final String SITE_KEY = "flickr"; 
    private static final String TAG = "FlickrSiteController";
    Flickr f = null;
    String key;
    String secret;
    
    
    public FlickrSiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
        key = mContext.getString(R.string.flickr_key); // FIXME move key and secret out of FlickrSiteController
        secret = mContext.getString(R.string.flickr_secret);
    }
    
    @Override
    public void startAuthentication(Account account) 
    {   
        Context currentContext = mContext;
        
        Log.d(TAG, "startAuthentication()");
        
        Intent intent = new Intent(currentContext, FlickrLoginActivity.class);
        intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
        ((Activity)currentContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE);
    }
    
    @Override
    public void upload(Account account, HashMap<String, String> valueMap) {
		Log.d(TAG, "Upload file: Entering upload");
		
		String title = valueMap.get(VALUE_KEY_TITLE);
		String body = valueMap.get(VALUE_KEY_BODY);
		String mediaPath = valueMap.get(VALUE_KEY_MEDIA_PATH);
		boolean useTor = (valueMap.get(VALUE_KEY_USE_TOR).equals("true")) ? true : false;
		
        String path = Environment.getExternalStorageDirectory() + File.separator + "flickr.conf"; // FIXME this should probably be stored on protected internal storage... or perhaps IOCipher
        
        Log.d(TAG, "upload() path: " + path);
        
        File confFile = new File(path);
        FlickrProperties fProps = new FlickrProperties(confFile); 
        f = new Flickr(key,                          // key
                       secret,                       // secret
                       "http://localhost/callback",  // callback
                       "delete",                     // permissions ("delete" permission allows read/write/delete)
                       fProps);                      // properties

        if(torCheck(useTor, mContext))
        {    
            Log.d(TAG, "setting proxy");
            
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ORBOT_HOST, ORBOT_HTTP_PORT));
            f.setProxy(proxy);
        }
        else
        {
            Log.d(TAG, "proxy not set");
        }
        
        // token stored in properties?  let's assume so for now...
        
        UploadFileTask ufTask = new UploadFileTask(this);
        ufTask.execute(title, body, mediaPath, account.getCredentials());
    }
    
    class UploadFileTask extends AsyncTask<String, String, String> 
    {
        private FlickrSiteController fpc;
        
        public UploadFileTask(FlickrSiteController fpc)
        {
            this.fpc = fpc;
        }
        
        @Override
        protected String doInBackground(String... params) 
        {
            String result = fpc.uploadFile(params[0], params[1], params[2], params[3]);
            if ((result != null) && !result.equals("")) {
                jobSucceeded(result);
                return "success";
            } else {
                jobFailed(null, 2938732, "Null or empty result from Flickr upload."); // FIXME error code?
                return "fail";
            }
        }
    }

    public String uploadFile(String title, String body, String mediaPath, String credentials) 
    {
        try
        {
            Log.d(TAG, "uploadFile() path: " + mediaPath);
            
            File photoFile = new File(mediaPath);
            UploadService us = f.getUploadService();
            return us.uploadPhoto(photoFile, title, body); // IS THIS THE CORRECT USE OF "body"?
        }
        catch (FlickrException fe)
        {
            Log.e(TAG, "upload failed: " + fe.getMessage());
            jobFailed(fe, 3233232, "upload failed: " + fe.getMessage()); // FIXME error code?
        }
        return null;
    }

    @Override
    public void startMetadataActivity(Intent intent) {
        return; // nop
    }

}
