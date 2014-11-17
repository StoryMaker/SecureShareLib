package io.scal.secureshareui.controller;

import io.scal.secureshareui.login.YoutubeLoginActivity;
import io.scal.secureshareui.model.Account;
import io.scal.secureshareuilibrary.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;

public class YoutubeSiteController extends SiteController {
    private static final String TAG = "YouTubeSiteController";
    public static final String SITE_NAME = "YouTube";
    public static final String SITE_KEY = "youtube";
    private static final String CLIENT_ID = "279338940292-7pqin08vmde3nhheekijn6cfetknotbs.apps.googleusercontent.com";
    
    HttpTransport transport = new NetHttpTransport();
    final JsonFactory jsonFactory = new GsonFactory();
    private YouTube mYoutube;
    
    private static String VIDEO_FILE_FORMAT = "video/*";
    
    
    public YoutubeSiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
    }
    
    @Override
    public void startAuthentication(Account account) {
    	Intent intent = new Intent(mContext, YoutubeLoginActivity.class);
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
		
		List<String> scopes = new ArrayList<String>();
		scopes.add(YouTubeScopes.YOUTUBE_UPLOAD);
		
		//set username
		GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(super.mContext, scopes);
		credential.setSelectedAccountName(account.getCredentials());
		
		//set proxy
		useTor=false; //FIXME Hardcoded until we find a Tor workaround
		if(super.torCheck(useTor, super.mContext)) {
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ORBOT_HOST, ORBOT_HTTP_PORT));
			transport = new NetHttpTransport.Builder().setProxy(proxy).build();
		}
		
        mYoutube = new com.google.api.services.youtube.YouTube.Builder(transport, jsonFactory, credential)
                        .setApplicationName(mContext.getString(R.string.google_app_name))
                        .setGoogleClientRequestInitializer(new YouTubeRequestInitializer(CLIENT_ID))
                        .build();
 
        File mediaFile = new File(mediaPath);  
        YouTube.Videos.Insert requestInsert = prepareUpload(title, body, mediaFile);
        new VideoUploadAsyncTask().execute(requestInsert);
	}

    public YouTube.Videos.Insert prepareUpload(String title, String body, File mediaFile) {
        try {
        	if(!super.isVideoFile(mediaFile)){
        		jobFailed(1231291, mContext.getString(R.string.invalid_file_format));
        		return null;
        	}
        	
            Video videoObjectDefiningMetadata = new Video();

            //set the video to private by default
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus("unlisted");
            videoObjectDefiningMetadata.setStatus(status);

            //we set a majority of the metadata with the VideoSnippet object
            VideoSnippet snippet = new VideoSnippet();

            //video file name.
            snippet.setTitle(title);
            snippet.setDescription(body);

            //set keywords.
            List<String> tags = new ArrayList<String>();
            tags.add("storymaker");
            snippet.setTags(tags);

            //set completed snippet to the video object
            videoObjectDefiningMetadata.setSnippet(snippet);

            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT, new BufferedInputStream(new FileInputStream(mediaFile)));
            mediaContent.setLength(mediaFile.length());
            
            YouTube.Videos.Insert requestInsert = mYoutube.videos().insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent);

            //set the upload type and add event listener.
            MediaHttpUploader uploader = requestInsert.getMediaHttpUploader();
            uploader.setDirectUploadEnabled(false);
            uploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE);

            MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            Log.d(TAG, "Upload file: Initiation Started");
                            break;
                        case INITIATION_COMPLETE:
                            Log.d(TAG, "Upload file: Initiation Completed");
                            break;
                        case MEDIA_IN_PROGRESS:
                            Log.d(TAG, "YouTube Upload: Upload in progress");
                            float uploadPercent = (float) (uploader.getProgress());
                            jobProgress(uploadPercent, mContext.getString(R.string.youtube_uploading));
                            break;
                        case MEDIA_COMPLETE:
                            Log.d(TAG, "Upload file: Upload Completed!");
                            break;
                        case NOT_STARTED:
                            Log.d(TAG, "Upload file: Upload Not Started!");
                            break;
                    }
                }
            };
            
            uploader.setProgressListener(progressListener);      
            return requestInsert;
        } catch (FileNotFoundException e) {
            String msg = e.getMessage() != null ? e.getMessage() + ", " : "";
            String errorMessage = e.getCause() + msg;
            Log.e(TAG, "File not found: " + errorMessage);
            return null;
        } catch (IOException e) {
            String msg = e.getMessage() != null ? e.getMessage() + ", " : "";
            String errorMessage = e.getCause() + msg;
            Log.e(TAG, "Progress IOException: " + errorMessage);
            return null;
        }
    }

	public class VideoUploadAsyncTask extends AsyncTask<YouTube.Videos.Insert, Void, Void> {
		@Override
		protected Void doInBackground(YouTube.Videos.Insert... requestInserts) {
			int errorId = -1;
			String errorMessage = null;
			String uploadedVideoId = null;
			YouTube.Videos.Insert requestInsert = requestInserts[0];

			if (null == requestInsert) {
				errorId = 1231231;
				errorMessage = "Video is Null";
			} else {
				try {
					Video uploadedVideo = requestInsert.execute();
					uploadedVideoId = uploadedVideo.getId();
				} catch (final GooglePlayServicesAvailabilityIOException e) {
					errorId = 1231232;
					String msg = e.getMessage() != null ? e.getMessage() + ", " : "";
					errorMessage = mContext.getString(R.string.google_play_services_not_avail) + ": " + e.getCause() + msg;
				} catch (UserRecoverableAuthIOException e) {
					errorId = 1231233;
                    String msg = e.getMessage() != null ? e.getMessage() + ", " : "";
					errorMessage = mContext.getString(R.string.insufficient_permissions) + ": " + e.getCause() + msg;
				} catch (GoogleAuthIOException e) {
					errorId = 1231234;
                    String msg = e.getMessage() != null ? e.getMessage() + ", " : "";
                    String cause = "" + e.getCause();
                    
                    //if bad username error, explain to user how to fix
                    if(cause.contains("BadUsername")) {
                    	errorMessage = mContext.getString(R.string.connect_your_google_account_to_device);
                    } else {
                    	errorMessage = "GoogleAuth IOException: " + e.getCause() + msg; // FIXME move to strings
                    }
				}catch (IOException e) {
					errorId = 1231235;
                    String msg = e.getMessage() != null ? e.getMessage() + ", " : "";
					errorMessage = "AsyncTask IOException: " + e.getCause() + msg; // FIXME move to strings
				}
			}

			//success
			if (errorId == -1) {
				jobSucceeded(uploadedVideoId);
			} else {
				Log.e(TAG, errorId + "_" + errorMessage);
				jobFailed(errorId, errorMessage);
			}

			return null;
		}
	}

    @Override
    public void startMetadataActivity(Intent intent) {
        return; // nop
    }

}
