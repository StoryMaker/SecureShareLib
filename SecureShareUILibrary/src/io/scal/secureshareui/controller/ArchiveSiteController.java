package io.scal.secureshareui.controller;

import io.scal.secureshareui.login.FacebookLoginActivity;
import io.scal.secureshareui.model.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public class ArchiveSiteController extends SiteController {
	public static final String SITE_NAME = "Archive";
	public static final String SITE_KEY = "archive";
	private static final String TAG = "ArchiveSiteController";

	private static final String sAccessKey = "Te8eJIS48D6N32Ju";
	private static final String sSecretKey = "HI4q8EWv1Rn2Bgfu";

	private static final String sBucketName = "micah_scal_Dog";
	private static final String sBucketKey = "t0";
	private static final String sArchiveAPIEndpoint = "http://s3.us.archive.org/";

	private int contentLength = -1;

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
		
		/*
		Log.d(TAG, "Upload file: Entering upload");
		AWSCredentials credential = new BasicAWSCredentials(sAccessKey, sSecretKey);
		ClientConfiguration s3Config = new ClientConfiguration();

		useTor=false; //FIXME Hardcoded until we find a Tor workaround
		if(super.torCheck(useTor, super.mContext)) {
			s3Config.setProxyHost(ORBOT_HOST);
			s3Config.setProxyPort(ORBOT_HTTP_PORT);
			s3Config.setProtocol(Protocol.HTTP);
		}
		
		AmazonS3 s3Client = new AmazonS3Client(credential, s3Config);
		s3Client.setEndpoint(sArchiveAPIEndpoint);

		TransferManager manager = new TransferManager(s3Client);

		File file = new File(mediaPath);
		if (!file.exists()) {
			jobFailed(4000000, "Archive upload failed: invalid file");
			return;
		}

		InputStream inputStream = convertFileToInputStream(file);
		if (null == inputStream) {
			jobFailed(4000001, "Archive upload failed: failed to convert file to inputStream");
			return;
		}

		// set metadata
		ObjectMetadata metadata = new ObjectMetadata();
		if (contentLength > 0) {
			metadata.setContentLength(contentLength);
		}

		// Transfer a file to an S3 bucket.
		Upload upload = manager.upload(sBucketName,  //path that will appear in URL
										sBucketKey,  //actual name of file within bucket
										inputStream, //file
										metadata);   //metadata

		try {
			Log.d(TAG, "Upload file: Initiation Started");
			UploadResult result = upload.waitForUploadResult();
		} catch (InterruptedException e) {
			jobFailed(4000002, "Archive upload failed: " + e.getMessage());
			e.printStackTrace();
		}

		while (!upload.isDone()) {
			long transfered = upload.getProgress().getBytesTransfered();
			// publish progress

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private InputStream convertFileToInputStream(File file) {
		InputStream in = null;

		try {
			in = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			
		} finally {
			if (in != null) {
				try {
					contentLength = in.available();
					in.close();
				} catch (IOException e) {}
			}
		}
		return in;*/
	}
}
