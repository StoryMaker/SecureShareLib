package io.scal.secureshareui.controller;

import timber.log.Timber;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;

import java.io.File;
import java.util.HashMap;

import io.scal.secureshareui.model.Account;
import io.scal.secureshareuilibrary.R;

public class S3SiteController extends SiteController {
    private static final String TAG = "S3SiteController";
    public static final String SITE_NAME = "S3";
    public static final String SITE_KEY = "s3";

    private ClientConfiguration s3Config = new ClientConfiguration();

    public S3SiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
    }

    @Override
    public void startAuthentication(Account account) {
        /*
        Intent intent = new Intent(mContext, S3.class);
        intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
        ((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE); // FIXME not a safe cast, context might be a service
        */
    }

    @Override
    public void upload(Account account, HashMap<String, String> valueMap) {
        String mediaPath = valueMap.get(VALUE_KEY_MEDIA_PATH);
        boolean useTor = valueMap.get(VALUE_KEY_USE_TOR).equals("true");

        //set proxy
        useTor=false; //FIXME Hardcoded until we find a Tor workaround
        if(super.torCheck(useTor, super.mContext)) {
            s3Config.setProtocol(Protocol.HTTP);
            s3Config.setProxyHost(ORBOT_HOST);
            s3Config.setProxyPort(ORBOT_HTTP_PORT);
        }

        new AmazonS3UploadTask().execute(mediaPath);
    }

    private class AmazonS3UploadTask extends AsyncTask<String, Integer, UploadResult> {
        String bucket;
        String pathPrefix;

        @Override
        protected UploadResult doInBackground(String... mediaPaths) {
            UploadResult result = null;

            if(null == mediaPaths[0]) {
                jobFailed(null, 7000000, "S3 media path is null");
                return result;
            }

            File mediaFile = new File(mediaPaths[0]);
            if (!mediaFile.exists()) {
                jobFailed(null, 7000001, "S3 media path invalid");
                return result;
            }

            try {
                final AWSCredentials credentials = new BasicAWSCredentials(mContext.getString(R.string.s3_key), mContext.getString(R.string.s3_secret));
                Log.i(TAG, "upload file: " + mediaFile.getName());

                AmazonS3Client s3Client = new AmazonS3Client(credentials, s3Config);
                TransferManager transferManager = new TransferManager(s3Client);
                Upload upload = transferManager.upload(bucket, pathPrefix + mediaFile.getName(), mediaFile);

                result = upload.waitForUploadResult();
            } catch (Exception e) {
                Timber.e("upload error: " + e.getMessage());
                jobFailed(null, 7000002, "S3 upload failed: " + e.getMessage());
            }

            return result;
        }

        @Override
        protected void onPreExecute() {
            bucket = mContext.getString(R.string.s3_bucket);
            pathPrefix = mContext.getString(R.string.s3_path_prefix);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }

        @Override
        protected void onPostExecute(UploadResult result) {
            if(null != result) {
                Log.i(TAG, "upload result: " + result.getKey());
                String url = "https://s3-us-west-1.amazonaws.com/" + bucket + "/" + result.getKey();
                jobSucceeded(url);
            } else {
                jobFailed(null, 7000002, "S3 upload failed: PostExecute");
            }
        }
    }

    @Override
    public void startMetadataActivity(Intent intent) {
        return; // nop
    }

}
