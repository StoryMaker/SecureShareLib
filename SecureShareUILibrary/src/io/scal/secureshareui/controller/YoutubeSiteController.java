package io.scal.secureshareui.controller;

import io.scal.secureshareui.login.YoutubeLoginActivity;
import io.scal.secureshareui.model.Account;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
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
    private static final String TAG = "YoutubeSiteController";
    public static final String SITE_NAME = "YouTube";
    public static final String SITE_KEY = "youtube";
    private static final String CLIENT_ID = "279338940292-7pqin08vmde3nhheekijn6cfetknotbs.apps.googleusercontent.com";
    
    final HttpTransport transport = new NetHttpTransport();
    final JsonFactory jsonFactory = new GsonFactory();
    private YouTube mYoutube;
    
    private static String VIDEO_FILE_FORMAT = "video/*";
    
    public YoutubeSiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public void startAuthentication(Account account) {
    	Intent intent = new Intent(mContext, YoutubeLoginActivity.class);
        intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
        ((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE); // FIXME not a safe cast, context might be a service 
    }
    
	@Override
	public void upload(String title, String body, String mediaPath, Account account, boolean useTor) {	
		List<String> scopes = new ArrayList<String>();
		scopes.add(YouTubeScopes.YOUTUBE_UPLOAD);
		
		GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(super.mContext, scopes);
		
		//set google username
		credential.setSelectedAccountName(account.getCredentials()); 
		
        mYoutube = new com.google.api.services.youtube.YouTube.Builder(transport, jsonFactory, credential)
                        .setApplicationName("StoryMaker")
                        .setGoogleClientRequestInitializer(new YouTubeRequestInitializer(CLIENT_ID))
                        .build();
 
        File mediaFile = new File(mediaPath);  
        YouTube.Videos.Insert videoInsert = prepareUpload(title, body, mediaFile);
        new VideoUploadAsyncTask().execute(videoInsert);
	}

    public YouTube.Videos.Insert prepareUpload(String title, String body, File videoFile) {
        try {
            // Add extra information to the video before uploading.
            Video videoObjectDefiningMetadata = new Video();

            // Set the video to public (default).
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus("private");
            videoObjectDefiningMetadata.setStatus(status);

            // We set a majority of the metadata with the VideoSnippet object.
            VideoSnippet snippet = new VideoSnippet();

            // Video file name.
            snippet.setTitle(title);
            snippet.setDescription(body);

            // Set keywords.
            List<String> tags = new ArrayList<String>();
            tags.add("storymaker");
            snippet.setTags(tags);

            // Set completed snippet to the video object.
            videoObjectDefiningMetadata.setSnippet(snippet);

            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT, new BufferedInputStream(new FileInputStream(videoFile)));
            YouTube.Videos.Insert videoInsert = mYoutube.videos().insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent);

            // Set the upload type and add event listener.
            MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();
            uploader.setDirectUploadEnabled(false);

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
                            Log.d(TAG, "Upload file: Upload in progress");
                            Log.d(TAG, "Upload file: Upload percentage: ");//uploader.getProgress());
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

            return videoInsert;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Progress IOException: " + e.getMessage());
            return null;
        }
    }

    public class VideoUploadAsyncTask extends AsyncTask<YouTube.Videos.Insert, Void, Void> {
        @Override
        protected Void doInBackground( YouTube.Videos.Insert... inserts ) {
            YouTube.Videos.Insert videoInsert = inserts[0];
            try {
                Video returnVideo = videoInsert.execute();
            } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
                //googleplay not available
            } catch (UserRecoverableAuthIOException userRecoverableException) {
                //startActivityForResult(userRecoverableException.getIntent(), TasksSample.REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(TAG, "AsyncTask IOException: " + e.getMessage());
            }
            return null;
        }
    }
}
