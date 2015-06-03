
package io.scal.secureshareui.login;

import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.onionkit.web.WebkitProxy;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.lib.Util;
import io.scal.secureshareuilibrary.R;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
//import com.facebook.Session;
//import com.facebook.SessionState;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

public class FacebookLoginActivity extends Activity {

	private static final String TAG = "FacebookWebActivity";
	
    private int mAccessResult = RESULT_CANCELED;
    private String mAccessToken = null;

    private LoginButton buttonLoginLogout;
    //private Session.StatusCallback statusCallback = new SessionStatusCallback();
    private static final int REAUTH_ACTIVITY_CODE = 100;
    private static final List<String> READ_PERMISSIONS = Arrays.asList("public_profile");
    private static final List<String> WRITE_PERMISSIONS = Arrays.asList("publish_actions");
    private static final List<String> ALL_PERMISSIONS = Arrays.asList("public_profile", "publish_actions");

    CallbackManager callbackManager;
    LoginManager loginManager;

    FacebookCallback<LoginResult> callbackMethod = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            // App code
            // If login succeeds, the LoginResult parameter has the new AccessToken, and the most recently granted or declined permissions.

            // DEFAULT STATUS IS FAILURE
            mAccessResult = RESULT_CANCELED;

            // OK STATUS REQUIRES A TOKEN AND WRITE PERMISSION

            Set<String> permissions = loginResult.getRecentlyGrantedPermissions();

            for (String permission : permissions) {
                Log.d("FACEBOOK_CB", "GOT PERMISSION: " + permission);

                AccessToken actualToken = AccessToken.getCurrentAccessToken();
                if (actualToken != null) {
                    mAccessToken = actualToken.getToken();
                    Log.d("FACEBOOK_CB", "GOT TOKEN: " + mAccessToken);

                    Log.d("FACEBOOK_CB", "GOT APP ID: " + actualToken.getApplicationId());
                    Log.d("FACEBOOK_CB", "GOT USER ID: " + actualToken.getUserId());
                    Log.d("FACEBOOK_CB", "GOT DECLINED: " + actualToken.getDeclinedPermissions().toString());
                    Log.d("FACEBOOK_CB", "GOT ALLOWED: " + actualToken.getPermissions().toString());

                    mAccessResult = RESULT_OK;
                } else {
                    Log.d("FACEBOOK_CB", "NO TOKEN");
                }

                /*
                if (permission.equals("publish_actions")) {
                    Log.d("FACEBOOK_CB", "WRITE PERMISSION FOUND");

                    AccessToken actualToken = AccessToken.getCurrentAccessToken();
                    if (actualToken != null) {
                        mAccessToken = actualToken.getToken();
                        Log.d("FACEBOOK_CB", "GOT TOKEN: " + mAccessToken);

                        Log.d("FACEBOOK_CB", "GOT APP ID: " + actualToken.getApplicationId());
                        Log.d("FACEBOOK_CB", "GOT USER ID: " + actualToken.getUserId());
                        Log.d("FACEBOOK_CB", "GOT DECLINED: " + actualToken.getDeclinedPermissions().toString());
                        Log.d("FACEBOOK_CB", "GOT ALLOWED: " + actualToken.getPermissions().toString());

                        mAccessResult = RESULT_OK;
                    } else {
                        Log.d("FACEBOOK_CB", "NO TOKEN");
                    }
                }
                */

            }
        }

        @Override
        public void onCancel() {
            Log.d("FACEBOOK_CB", "RESULT IS CANCELLED");
            mAccessResult = RESULT_CANCELED;

            finish();
        }

        @Override
        public void onError(FacebookException fe) {
            Log.d("FACEBOOK_CB", "RESULT IS ERROR: " + fe.getMessage());
            mAccessResult = RESULT_CANCELED; // NO ERROR OPTION

            finish();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NEW
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        loginManager = LoginManager.getInstance();



        // attempt to set proxy here, unsure where connection is actually initiated
        // check for tor settings and set proxy
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean useTor = settings.getBoolean("pusetor", false);

        if (useTor) {
            Log.d(TAG, "user selected \"use tor\"");

            OrbotHelper orbotHelper = new OrbotHelper(getApplicationContext());
            if ((!orbotHelper.isOrbotInstalled()) || (!orbotHelper.isOrbotRunning())) {
                Log.e(TAG, "user selected \"use tor\" but orbot is not installed or not running");
                return;
            } else {
                try {
                    WebkitProxy.setProxy("android.app.Application", getApplicationContext(), Util.ORBOT_HOST, Util.ORBOT_HTTP_PORT);
                } catch (Exception e) {
                    Log.e(TAG, "user selected \"use tor\" but an exception was thrown while setting the proxy: " + e.getLocalizedMessage());
                    return;
                }
            }
        } else {
            Log.d(TAG, "user selected \"don't use tor\"");
        }

        setContentView(R.layout.activity_facebook_login);
        buttonLoginLogout = (LoginButton) findViewById(R.id.login_button);

        // NEW
        // REDUNDANT?
        // buttonLoginLogout.setPublishPermissions(WRITE_PERMISSIONS);
        // buttonLoginLogout.registerCallback(callbackManager, callbackMethod);

        // NEW
        loginManager.registerCallback(callbackManager, callbackMethod);

        buttonLoginLogout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin();
            }
        });











        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAccessToken = extras.getString(SiteController.EXTRAS_KEY_CREDENTIALS);
        }

        // Session class no longer exists
        /*
        //reset session on new login
        Session.setActiveSession(null);

        Session session = Session.getActiveSession();
        if (session == null) {
            if (savedInstanceState != null) {
                session = Session.restoreSession(this, null, statusCallback, savedInstanceState);
            }
            if (session == null) {
                session = new Session(this);
            }
            Session.setActiveSession(session);
            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
                session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback));
            }
        }
        */

        // updateView();
    }

    public void doLogin() {
        Log.d("FACEBOOK_DEBUG", "LOGGING IN");
        loginManager.logInWithReadPermissions(this, READ_PERMISSIONS);
    }

    @Override
    public void onStart() {
        super.onStart();
        //Session.getActiveSession().addCallback(statusCallback);
        buttonLoginLogout.performClick();
    }

    @Override
    public void onStop() {
        super.onStop();
        //Session.getActiveSession().removeCallback(statusCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("FACEBOOK_DEBUG", "REQUEST WAS " + requestCode);
        Log.d("FACEBOOK_DEBUG", "RESULT IS (1) " + resultCode);

        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        // Session class removed
        /*
        Session session = Session.getActiveSession();     
        if (session == null) {
            Log.d("FACEBOOK_DEBUG", "SESSION IS NULL");
            return;
        }
        session.onActivityResult(this, requestCode, resultCode, data);
        */

        // I THINK THIS IS NOW HANDLED BY THE CALLBACK REGISTERED WITH THE MANAGER
        /*
        Log.d("FACEBOOK_DEBUG", "RESULT IS " + resultCode);
        Log.d("FACEBOOK_DEBUG", "PERMISSION IS " + hasPublishPermissions(session));

        // successful login && publish permissions authorized
        if ((resultCode == RESULT_OK) && hasPublishPermissions(session)) {
            Log.d("FACEBOOK_DEBUG", "RESULT IS OK");
            mAccessResult = RESULT_OK;
        } else { // failed login
            Log.d("FACEBOOK_DEBUG", "RESULT IS CANCELLED");
            mAccessResult = RESULT_CANCELED;
        }

        //mAccessToken = session.getAccessToken();
        AccessToken actualToken = AccessToken.getCurrentAccessToken();
        if (actualToken != null) {
            mAccessToken = actualToken.getToken();
            Log.d("FACEBOOK_DEBUG", "GOT TOKEN: " + mAccessToken);
        } else {
            Log.d("FACEBOOK_DEBUG", "NO TOKEN");
        }
        */

        // STILL NEED TO FINISH?
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Session session = Session.getActiveSession();
        //Session.saveSession(session, outState);
    }

    /*
    private void updateView() {
        Session session = Session.getActiveSession();

        if (session.isOpened()) {
            buttonLoginLogout.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    onClickLogout();
                }
            });
        } else {
            buttonLoginLogout.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    onClickLogin();
                }
            });
        }
    }

    private void onClickLogin() {
        Session session = Session.getActiveSession();
        if (!session.isOpened() && !session.isClosed()) {
            session.openForPublish(new Session.OpenRequest(this).setPermissions(ALL_PERMISSIONS).setCallback(statusCallback).setRequestCode(REAUTH_ACTIVITY_CODE));
        } else {
            Session.openActiveSession(this, true, statusCallback);
        }
    }

    private void onClickLogout() {
        Session session = Session.getActiveSession();
        if (!session.isClosed()) {
            session.closeAndClearTokenInformation();
        }
    }

    private boolean hasPublishPermissions(Session session) {
    	List<String> permissions = session.getPermissions();

        Log.d("FACEBOOK_DEBUG", "PERMISSIONS ARE " + permissions);
        Log.d("FACEBOOK_DEBUG", "WRITE PERMISSIONS ARE " + WRITE_PERMISSIONS);

        if (permissions.containsAll((WRITE_PERMISSIONS))) {
            return true;
        }

        return false;
    }

    private Session.StatusCallback permissionRequestCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            if (exception != null || !session.isOpened() || !session.getPermissions().contains("publish_actions")) {
                Log.d("FACEBOOK_DEBUG", "PERMISSIONS REJECTED?");
            } else {
                Log.d("FACEBOOK_DEBUG", "PERMISSIONS ACCEPTED?");
            }
        }
    };
    
    private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            updateView();
        }
    }
    */

    @Override
    public void finish() {
    	Log.d(TAG, "finish()");

        Log.d("FACEBOOK_DEBUG", "TOKEN: " + mAccessToken);

        Intent data = new Intent();
        data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, mAccessToken);
        setResult(mAccessResult, data);
        
        super.finish();
        // Session.setActiveSession(null);
        Util.clearWebviewAndCookies(new WebView(this), this);
    }
}
