
package io.scal.secureshareui.login;

import timber.log.Timber;

import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.onionkit.web.WebkitProxy;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.lib.Util;
import io.scal.secureshareuilibrary.R;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.widget.LoginButton;

public class FacebookLoginActivity extends Activity {

	private static final String TAG = "FacebookWebActivity";
	
    private int mAccessResult = RESULT_CANCELED;
    private String mAccessToken = null;

    private LoginButton buttonLoginLogout;
    private Session.StatusCallback statusCallback = new SessionStatusCallback();
    private static final int REAUTH_ACTIVITY_CODE = 100;
    private static final List<String> READ_PERMISSIONS = Arrays.asList("public_profile");
    private static final List<String> WRITE_PERMISSIONS = Arrays.asList("publish_actions", "user_photos", "user_videos");
    private static final List<String> REQUIRED_PERMISSIONS = Arrays.asList("publish_actions", "user_photos"); // user_videos is for an incomplete feature, do not require it at this time

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // attempt to set proxy here, unsure where connection is actually initiated
        // check for tor settings and set proxy
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean useTor = settings.getBoolean("pusetor", false);

        if (useTor) {
            Timber.d("user selected \"use tor\"");

            OrbotHelper orbotHelper = new OrbotHelper(getApplicationContext());
            if ((!orbotHelper.isOrbotInstalled()) || (!orbotHelper.isOrbotRunning())) {
                Timber.e("user selected \"use tor\" but orbot is not installed or not running");
                return;
            } else {
                try {
                    WebkitProxy.setProxy("android.app.Application", getApplicationContext(), Util.ORBOT_HOST, Util.ORBOT_HTTP_PORT);
                } catch (Exception e) {
                    Timber.e("user selected \"use tor\" but an exception was thrown while setting the proxy: " + e.getLocalizedMessage());
                    return;
                }
            }
        } else {
            Timber.d("user selected \"don't use tor\"");
        }

        setContentView(R.layout.activity_facebook_login);
        buttonLoginLogout = (LoginButton) findViewById(R.id.login_button);
        buttonLoginLogout.setPublishPermissions(WRITE_PERMISSIONS);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAccessToken = extras.getString(SiteController.EXTRAS_KEY_CREDENTIALS);
        }
        
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

        updateView();
    }

    @Override
    public void onStart() {
        super.onStart();
        Session.getActiveSession().addCallback(statusCallback);
        buttonLoginLogout.performClick();
    }

    @Override
    public void onStop() {
        super.onStop();
        Session.getActiveSession().removeCallback(statusCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Session session = Session.getActiveSession();     
        if (session == null) {
            return;
        }
        session.onActivityResult(this, requestCode, resultCode, data);

        // successful login && publish permissions authorized
        if ((resultCode == RESULT_OK) && hasPublishPermissions(session)) {
            mAccessResult = RESULT_OK;
        } else { // failed login
            mAccessResult = RESULT_CANCELED;
        }

        mAccessToken = session.getAccessToken();
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
    }

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
            session.openForPublish(new Session.OpenRequest(this).setPermissions(READ_PERMISSIONS).setCallback(statusCallback).setRequestCode(REAUTH_ACTIVITY_CODE));
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

        for (String permission : permissions) {
            Timber.d("PERMISSION GRANTED: " + permission);
        }

        if (permissions.containsAll((REQUIRED_PERMISSIONS))) {
            return true;
        }
        
        return false;
    }
    
    private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            updateView();
        }
    }

    @Override
    public void finish() {
    	Timber.d("finish()");
    	
        Intent data = new Intent();
        data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, mAccessToken);
        setResult(mAccessResult, data);
        
        super.finish();
        Session.setActiveSession(null);
        Util.clearWebviewAndCookies(new WebView(this), this);
    }
}
