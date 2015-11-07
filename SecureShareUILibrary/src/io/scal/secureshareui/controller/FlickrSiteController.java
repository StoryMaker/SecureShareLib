package io.scal.secureshareui.controller;

import timber.log.Timber;

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
import com.flickr.api.PhotosetsService;
import com.flickr.api.UploadService;
import com.flickr.api.entities.Photoset;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;

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

    public static final String PHOTO_SET_KEY = "PhotoSetPaths";
    
    public FlickrSiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
        key = mContext.getString(R.string.flickr_key); // FIXME move key and secret out of FlickrSiteController
        secret = mContext.getString(R.string.flickr_secret);
    }
    
    @Override
    public void startAuthentication(Account account) 
    {   
        Context currentContext = mContext;
        
        Timber.d("startAuthentication()");
        
        Intent intent = new Intent(currentContext, FlickrLoginActivity.class);
        intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
        ((Activity)currentContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE);
    }
    
    @Override
    public void upload(Account account, HashMap<String, String> valueMap) {
		Timber.d("Upload file: Entering upload");
		
		String title = valueMap.get(VALUE_KEY_TITLE);
		String body = valueMap.get(VALUE_KEY_BODY);

        // check for photo set upload paths
        String mediaPath = "";
        if (valueMap.keySet().contains(PHOTO_SET_KEY)) {
            mediaPath = valueMap.get(PHOTO_SET_KEY);
        } else {
            mediaPath = valueMap.get(VALUE_KEY_MEDIA_PATH);
        }
		boolean useTor = (valueMap.get(VALUE_KEY_USE_TOR).equals("true")) ? true : false;
		
        String path = Environment.getExternalStorageDirectory() + File.separator + "flickr.conf"; // FIXME this should probably be stored on protected internal storage... or perhaps IOCipher
        
        Timber.d("upload() path: " + path);

        Timber.d("upload() title: " + title);

        File confFile = new File(path);
        FlickrProperties fProps = new FlickrProperties(confFile); 
        f = new Flickr(key,                          // key
                       secret,                       // secret
                       "http://localhost/callback",  // callback
                       "delete",                     // permissions ("delete" permission allows read/write/delete)
                       fProps);                      // properties

        if(torCheck(useTor, mContext))
        {    
            Timber.d("setting proxy");
            
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ORBOT_HOST, ORBOT_HTTP_PORT));
            f.setProxy(proxy);
        }
        else
        {
            Timber.d("proxy not set");
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
            Timber.d("uploadFile() path: " + mediaPath);

            if (mediaPath.contains(";")) {

                Timber.d("GOT SET OF MEDIA PATHS");

                String[] photoPaths = mediaPath.split(";");

                ArrayList<String> photos = new ArrayList<String>();
                for (int i = 0; i < photoPaths.length; i++) {
                    photos.add(photoPaths[i]);
                }
                String urlPart = uploadFiles(title, body, photos, credentials);

                Timber.d("SET UPLOAD RETURN VALUE: " + urlPart);

                return urlPart;
            } else {

                Timber.d("GOT SINGLE MEDIA PATH");

                File photoFile = new File(mediaPath);

                UploadService us = f.getUploadService();
                String result = us.uploadPhoto(photoFile, title, body); // IS THIS THE CORRECT USE OF "body"?

                // construct url part, format https://www.flickr.com/photos/{user-id}/{photo-id}
                String urlPart = f.getUser().getId() + "/" + result;

                Timber.d("SINGLE UPLOAD RETURN VALUE: " + urlPart);

                return urlPart;
            }
        }
        catch (FlickrException fe)
        {
            Timber.e("upload failed: " + fe.getMessage());
            jobFailed(fe, 3233232, "upload failed: " + fe.getMessage()); // FIXME error code?
        }
        return null;
    }

    public String uploadFiles(String title, String body, ArrayList<String> mediaPaths, String credentials)
    {

        int photoCount = 0;
        String photoId = null;

        ArrayList<String> uploadedPhotoIds = new ArrayList<String>();

        // first upload everything
        for (String mediaPath : mediaPaths) {
            try {

                File photoFile = new File(mediaPath);

                UploadService us = f.getUploadService();
                photoId = us.uploadPhoto(photoFile, title + "_" + photoCount, body + "_" + photoCount);  // IS THIS THE CORRECT USE OF "body"?
                uploadedPhotoIds.add(photoId);
                Timber.d("ADDED PHOTO " + photoId + " TO LIST");

                photoCount++;
            } catch (FlickrException fe) {
                Timber.e("upload failed: " + fe.getMessage());
                jobFailed(fe, 3233232, "upload failed: " + fe.getMessage()); // FIXME error code?
                return null;
            }
        }

        Timber.d("UPLOADED " + photoCount + " PHOTOS");

        try {

            // create a photo set and add photos
            PhotosetsService pss = f.getPhotosetsService();
            Photoset photoSet = null;

            String photoSetPhotoId = null;

            for(String uploadedPhotoId : uploadedPhotoIds) {

                Timber.d("PROCESSING PHOTO " + photoId);

                if (photoSetPhotoId == null) {
                    photoSetPhotoId = uploadedPhotoId;
                    photoSet = pss.createPhotoset(title, body, photoSetPhotoId); // using "body" as "description"?
                    Timber.d("CREATED PHOTO SET " + photoSet.getId() + " WITH PRIMARY PHOTO " + uploadedPhotoId);
                }else {
                    pss.addPhotoToSet(photoSet, uploadedPhotoId);
                    Timber.d("ADDED PHOTO " + uploadedPhotoId + " TO PHOTO SET");
                }

            }

            // construct url part, format https://www.flickr.com/photos/{user-id}/sets/{photoset-id}
            String urlPart = f.getUser().getId() + "/sets/" + photoSet.getId();

            return urlPart;
        } catch (FlickrException fe) {
            Timber.e("failed to create photo set: " + fe.getMessage());
            fe.printStackTrace();
            Timber.e("cause: " + fe.getCause().getMessage());
            jobFailed(fe, 3233232, "upload failed: " + fe.getMessage()); // FIXME error code?
            return null;
        }
    }

    @Override
    public void startMetadataActivity(Intent intent) {
        return; // nop
    }

}
