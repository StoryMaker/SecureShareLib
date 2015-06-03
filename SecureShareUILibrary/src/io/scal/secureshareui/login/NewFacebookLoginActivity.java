package io.scal.secureshareui.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.DefaultAudience;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;
import java.util.Set;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.lib.Util;
import io.scal.secureshareuilibrary.R;

/**
 * Created by mnbogner on 6/3/15.
 */
public class NewFacebookLoginActivity extends Activity {

    private static final String PROFILE_PERMISSIONS = "public_profile";
    private static final String PUBLISH_PERMISSIONS = "publish_actions";

    private int mAccessResult = RESULT_CANCELED;
    private String mAccessToken = null;

    private CallbackManager callbackManager;
    private LoginManager loginManager;

    FacebookCallback<LoginResult> callbackMethod = new FacebookCallback<LoginResult>() {

        @Override
        public void onSuccess(LoginResult loginResult) {

            AccessToken accessTokenObject = AccessToken.getCurrentAccessToken();

            Log.d("FACEBOOK_CB", "GOT APP ID: " + accessTokenObject.getApplicationId());
            Log.d("FACEBOOK_CB", "GOT USER ID: " + accessTokenObject.getUserId());

            if (accessTokenObject.getPermissions().contains(PUBLISH_PERMISSIONS)) {
                mAccessResult = RESULT_OK;
                mAccessToken = accessTokenObject.getToken();

                Log.d("FACEBOOK_CB", "GOT TOKEN AND PUBLISH PERMISSION: " + mAccessToken);

                finish();
            } else {
                Log.d("FACEBOOK_CB", "GOT TOKEN BUT NO PUBLISH PERMISSION: " + mAccessToken);

                requestPublishPermission();
            }
        }

        @Override
        public void onCancel() {
            Log.d("FACEBOOK_CB", "CANCELLED");

            finish();
        }

        @Override
        public void onError(FacebookException fe) {
            Log.d("FACEBOOK_CB", "ERROR: " + fe.getMessage());

            finish();
        }
    };

    private void requestPublishPermission() {
        final AccessToken accessTokenObject = AccessToken.getCurrentAccessToken();
        if (accessTokenObject != null) {
            if (accessTokenObject.getPermissions().contains(PUBLISH_PERMISSIONS)) {
                Log.d("FACEBOOK_DEBUG", "ALREADY HAVE PUBLISH PERMISSION");

                finish();
            } else {
                Log.d("FACEBOOK_DEBUG", "REQUESTING PUBLISH PERMISSION");
                new AlertDialog.Builder(this).setTitle("FOO")
                                             .setMessage("PUBLISH?")
                                             .setPositiveButton("YES", publishYesClickListener)
                                             .setNegativeButton("NO", publishNoClickListener)
                                             .show();
            }
        } else {
            Log.d("FACEBOOK_DEBUG", "NO ACCESS TOKEN");

            finish();
        }
    }

    DialogInterface.OnClickListener publishYesClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (AccessToken.getCurrentAccessToken() != null) {
                Log.d("FACEBOOK_OC", "USER CLICKED YES");

                loginManager.setDefaultAudience(DefaultAudience.ONLY_ME) // check settings?
                            .logInWithPublishPermissions(NewFacebookLoginActivity.this, Arrays.asList(PUBLISH_PERMISSIONS));

                Log.d("FACEBOOK_OC", "DID SOMETHING HAPPEN?");
            }

        }
    };

    DialogInterface.OnClickListener publishNoClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            Log.d("FACEBOOK_OC", "USER CLICKED NO");

            finish();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        loginManager = LoginManager.getInstance();
        loginManager.registerCallback(callbackManager, callbackMethod);

    }

    @Override
    public void onStop() {

        super.onStop();

    }

    @Override
    public void onStart() {

        super.onStart();

        Log.d("FACEBOOK_DEBUG", "LOGGING IN");

        loginManager.logInWithReadPermissions(this, Arrays.asList(PROFILE_PERMISSIONS));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("FACEBOOK_DEBUG", "GOT ACTIVITY RESULT");

        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        // finish();

    }

    @Override
    public void finish() {

        Log.d("FACEBOOK_DEBUG", "FINISHING");

        Log.d("FACEBOOK_DEBUG", "RESULT: " + mAccessResult);
        Log.d("FACEBOOK_DEBUG", "TOKEN: " + mAccessToken);

        Intent data = new Intent();
        data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, mAccessToken);
        setResult(mAccessResult, data);

        super.finish();

    }
}
