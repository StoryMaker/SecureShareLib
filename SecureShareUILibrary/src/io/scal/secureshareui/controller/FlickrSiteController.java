package io.scal.secureshareui.controller;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

import com.flickr.api.Flickr;
import com.flickr.api.FlickrException;
import com.flickr.api.FlickrProperties;
import com.flickr.api.PeopleService;
import com.flickr.api.PhotosService;
import com.flickr.api.UploadService;
import com.flickr.api.entities.Comment;
import com.flickr.api.entities.Paginated;
import com.flickr.api.entities.Photo;
import com.flickr.api.entities.UserInfos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import info.guardianproject.onionkit.ui.OrbotHelper;
import io.scal.secureshareui.login.FlickrLoginActivity;
import io.scal.secureshareui.model.Account;
import io.scal.secureshareuilibrary.R;

public class FlickrSiteController extends SiteController {
    public static final String SITE_NAME = "Flickr"; 
    public static final String SITE_KEY = "flickr"; 
    private static final String TAG = "FlickrSiteController";
    Flickr f = null;
    String key;
    String secret;
    
 // TOR PROXY SETTINGS
    private static final String ORBOT_HOST = "127.0.0.1";
    private static final int ORBOT_HTTP_PORT = 8118;
    
    public FlickrSiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
        key = mContext.getString(R.string.flickr_key);
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
    public void upload(String title, String body, String mediaPath, Account account)
    {
        String path = Environment.getExternalStorageDirectory() + File.separator + "flickr.conf"; // FIXME this should probably be stored on protected internal storage... or perhaps IOCipher
        
        Log.d(TAG, "upload() path: " + path);
        
        File confFile = new File(path);
        FlickrProperties fProps = new FlickrProperties(confFile); 
        f = new Flickr(key,                          // key
                       secret,                       // secret
                       "http://localhost/callback",  // callback
                       "delete",                     // permissions ("delete" permission allows read/write/delete)
                       fProps);                      // properties

        OrbotHelper orbotHelper = new OrbotHelper(mContext);
        if(orbotHelper.isOrbotRunning()) 
        {    
            Log.d(TAG, "orbot running, setting proxy");
            
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ORBOT_HOST, ORBOT_HTTP_PORT));
            f.setProxy(proxy);
        }
        else
        {
            Log.d(TAG, "orbot not running, proxy not set");
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
                jobFailed(2938732, "Null or empty result from Flickr upload."); // FIXME error code?
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
            jobFailed(3233232, "upload failed: " + fe.getMessage()); // FIXME error code?
        }
        return null;
    }
}
