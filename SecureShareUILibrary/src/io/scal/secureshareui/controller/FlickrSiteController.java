package io.scal.secureshareui.controller;

import io.scal.secureshareui.login.FlickrLoginActivity;
import io.scal.secureshareui.model.Account;
import io.scal.secureshareuilibrary.R;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.Contacts;
import android.util.Log;

import com.flickr.api.Flickr;
import com.flickr.api.FlickrException;
import com.flickr.api.FlickrProperties;
import com.flickr.api.PhotosService;
import com.flickr.api.PhotosetsService;
import com.flickr.api.UploadService;
import com.flickr.api.entities.Paginated;
import com.flickr.api.entities.Photo;
import com.flickr.api.entities.Photoset;

import org.json.JSONException;
import org.json.JSONObject;

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

        Log.d(TAG, "upload() title: " + title);

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
        //try
        //{
            Log.d(TAG, "uploadFile() path: " + mediaPath);
            
            File photoFile = new File(mediaPath);

            // TEMP
            ArrayList<String> duplicates = new ArrayList<String>();
            for (int i = 0; i < 4; i++) {
                duplicates.add(mediaPath);
            }
            return uploadFiles(title, body, duplicates, credentials);

            //UploadService us = f.getUploadService();
            //return us.uploadPhoto(photoFile, title, body); // IS THIS THE CORRECT USE OF "body"?
        //}
        //catch (FlickrException fe)
        //{
        //    Log.e(TAG, "upload failed: " + fe.getMessage());
        //    jobFailed(fe, 3233232, "upload failed: " + fe.getMessage()); // FIXME error code?
        //}
        //return null;
    }

    public String uploadFiles(String title, String body, ArrayList<String> mediaPaths, String credentials)
    {

        int photoCount = 0;
        String oneResult = null;

        ArrayList<String> uploadedPhotoIds = new ArrayList<String>();

        // first upload everything
        for (String mediaPath : mediaPaths) {
            try {
                Log.d(TAG, "uploadFiles() path: " + mediaPath);

                File photoFile = new File(mediaPath);

                UploadService us = f.getUploadService();
                // how do we get a result for the whole set?
                // or
                // what do we need to return?
                oneResult = us.uploadPhoto(photoFile, title + "_" + photoCount, body + "_" + photoCount);  // IS THIS THE CORRECT USE OF "body"?
                Log.d("FLICKR MULTI-UPLOAD", "RESULT: " + oneResult);

                uploadedPhotoIds.add(oneResult);
                Log.d("FLICKR MULTI-UPLOAD", "ADDED PHOTO " + oneResult + " TO LIST");

                photoCount++;
            } catch (FlickrException fe) {
                Log.e(TAG, "upload failed: " + fe.getMessage());
                jobFailed(fe, 3233232, "upload failed: " + fe.getMessage()); // FIXME error code?
                return null;
            }
        }

        Log.d("FLICKR MULTI-UPLOAD", "UPLOADED " + photoCount + " PHOTOS");

        try {
            // get flickr photo objects
            //PhotosService ps = f.getPhotosService();
            //Paginated<Photo> recentPhotos = ps.getRecentlyUpdated(photoCount + 1, 1);

            // add them to a photo set
            PhotosetsService pss = f.getPhotosetsService();
            Photoset photoSet = null;


            String photoSetPhotoId = null;

            //for(int i = 0; i <= photoCount; i++) {
            for(String photoId : uploadedPhotoIds) {


                //Log.d("FLICKR MULTI-UPLOAD", "PROCESSING PHOTO " + recentPhotos.get(i).getId() + " / " + recentPhotos.get(i).getTitle());
                Log.d("FLICKR MULTI-UPLOAD", "PROCESSING PHOTO " + photoId);


                if (photoSetPhotoId == null) {
                    photoSetPhotoId = photoId;
                    photoSet = pss.createPhotoset(title + "_SET", body, photoSetPhotoId); // using "body" as "description"?
                    Log.d("FLICKR MULTI-UPLOAD", "CREATED PHOTO SET: " + photoSet.getId());
                }else {
                    pss.addPhotoToSet(photoSet, photoId);
                    //Log.d("FLICKR MULTI-UPLOAD", "ADDED PHOTO " + recentPhotos.get(i).getTitle() + " TO PHOTO SET: " + photoSet.getTitle());
                    Log.d("FLICKR MULTI-UPLOAD", "ADDED PHOTO " + photoId);
                }


            }

        } catch (FlickrException fe) {
            Log.e(TAG, "failed to get create photo set: " + fe.getMessage());
            fe.printStackTrace();
            Log.e(TAG, "cause: " + fe.getCause().getMessage());
            jobFailed(fe, 3233232, "upload failed: " + fe.getMessage()); // FIXME error code?
            return null;
        }

        return oneResult;
    }

    @Override
    public void startMetadataActivity(Intent intent) {
        return; // nop
    }

}
