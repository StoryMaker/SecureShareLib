package io.scal.secureshareui.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.flickr.api.Flickr;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Date;
import java.util.HashMap;

import info.guardianproject.onionkit.trust.StrongHttpsClient;
import io.scal.secureshareui.login.ZTLoginActivity;
import io.scal.secureshareui.model.Account;
import io.scal.secureshareuilibrary.R;

/**
 * Created by mnbogner on 3/26/15.
 */
public class ZTSiteController extends SiteController {

    private static final String TAG = "ZTSiteController";

    public static final String SITE_NAME = "Zamaneh Tribune";
    public static final String SITE_KEY = "ZT";

    String key;
    String secret;

    private StrongHttpsClient mClient;

    public ZTSiteController(Context context, Handler handler, String jobId) {

        super(context, handler, jobId);
        key = mContext.getString(R.string.zt_key);
        secret = mContext.getString(R.string.zt_secret);

    }

    @Override
    public void startAuthentication(Account account) {

        Intent intent = new Intent(mContext, ZTLoginActivity.class);
        intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
        ((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE);

    }

    @Override
    public void startMetadataActivity(Intent intent) {

        Log.d(TAG, "startMetadataActivity() NOT YET IMPLEMENTED");

    }

    // unclear whether actual uploads will be required, using this method to support publication
    @Override
    public void upload(Account account, HashMap<String, String> valueMap) {

        Date publishDate = new Date();




    }

    public String post(String user, String title, String body, String embed, String[] catstrings, String medium, String mediaService, String mediaGuid, String mimeType, File file) throws IOException {

    }


}
