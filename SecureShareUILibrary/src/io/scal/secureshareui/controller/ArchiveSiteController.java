package io.scal.secureshareui.controller;

import io.scal.secureshareui.login.FacebookLoginActivity;
import io.scal.secureshareui.model.Account;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;


public class ArchiveSiteController extends SiteController {
	public static final String SITE_NAME = "Archive";
	public static final String SITE_KEY = "archive";
	private static final String TAG = "ArchiveSiteController";


    private static final String sArchiveAPIEndpoint = "http://s3.us.archive.org/";
    private static final String sAccessKey = "Te8eJIS48D6N32Ju";
    private static final String sSecretKey = "HI4q8EWv1Rn2Bgfu";

    private static final String sBucketName = "theworldisavampire2";
    private static final String sBucketKey = "0.mp4";
    
    public static final MediaType MEDIA_TYPE = MediaType.parse("");

    
	public ArchiveSiteController(Context context, Handler handler, String jobId) {
		super(context, handler, jobId);
	}

	@Override
	public void startAuthentication(Account account) {
		Intent intent = new Intent(mContext, FacebookLoginActivity.class);
		intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
		((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE); 
		// FIXME not a safe cast, context might be a service
	}

	@Override
	public void upload(String title, String body, String mediaPath, Account account, boolean useTor) {
		Log.d(TAG, "Upload file: Entering upload");

		File file = new File(mediaPath);
		if (!file.exists()) {
			jobFailed(4000000, "Archive upload failed: invalid file");
			return;
		}
			
		OkHttpClient client = new OkHttpClient();
		
		useTor=false; //FIXME Hardcoded until we find a Tor workaround
		if(super.torCheck(useTor, super.mContext)) {
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ORBOT_HOST, ORBOT_HTTP_PORT));
	        client.setProxy(proxy);
		}
		
		String url = sArchiveAPIEndpoint + "/" + sBucketName + "/" + sBucketKey;
		
		Request request = new Request.Builder()
        .url(url)
        .put(RequestBody.create(MEDIA_TYPE, file))
        .addHeader("Accept", "*/*")
        .addHeader("x-amz-auto-make-bucket", "1")
        .addHeader("x-archive-meta01-collection", "opensource")
        .addHeader("x-archive-meta-mediatype", "texts")
        .addHeader("x-archive-meta-sponsor", "Sponsor 998")
        .addHeader("x-archive-meta-language", "eng")
        .addHeader("authorization", "LOW " + sAccessKey + ":" + sSecretKey)//authorization: LOW $accesskey:$secret
        .build();

		UploadFileTask uploadFileTask = new UploadFileTask(client, request);
		uploadFileTask.execute();
	}

	class UploadFileTask extends AsyncTask<String, String, String> {
        private OkHttpClient client;
        private Request request;
        private Response response;

        public UploadFileTask(OkHttpClient client, Request request) {
            this.client = client;
            this.request = request;
        }

        @Override
        protected String doInBackground(String... params) {  	
        	Log.d(TAG, "Begin Upload");
        	
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
            	jobFailed(4000001, "Archive upload failed");	
                e.printStackTrace();
            }

            try {
                Log.d(TAG, response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return "-1";
        }
    }
}
