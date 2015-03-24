
package io.scal.secureshareui.login;

import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.onionkit.web.WebkitProxy;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SoundCloudSiteController;
import io.scal.secureshareui.lib.Util;
import io.scal.secureshareui.soundcloud.ApiWrapper;
import io.scal.secureshareui.soundcloud.Token;
import io.scal.secureshareuilibrary.R;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SoundCloudLoginActivity extends Activity {

    private static final String TAG = "SoundCloudLoginActivity";

    private int mAccessResult = Activity.RESULT_CANCELED;
    private String mAccessToken = null;

    private Button btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        this.setFinishOnTouchOutside(false);
        setContentView(R.layout.activity_soundcloud_login);

        init();
    }

    private void init() {
        final EditText etUsername = (EditText) findViewById(R.id.etUsername);
        final EditText etPassword = (EditText) findViewById(R.id.etPassword);
        btnSignIn = (Button) findViewById(R.id.btnSignIn);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setEnabled(false);

                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();

                new CheckCredentialsAsync().execute(username, password);
            }
        });
    }

    private class CheckCredentialsAsync extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            final ApiWrapper wrapper = new ApiWrapper(SoundCloudSiteController.APP_CLIENT_ID,
                    SoundCloudSiteController.APP_CLIENT_SECRET,
                    null,
                    null);
            Token token = null;
            try {
                token = wrapper.login(params[0], params[1], Token.SCOPE_NON_EXPIRING);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (token != null) { // success
                mAccessToken = token.access;
                return Integer.toString(RESULT_OK);
            }

            return Integer.toString(RESULT_CANCELED);
        }

        @Override
        protected void onPostExecute(String resultStr) {
            int result = Integer.parseInt(resultStr);

            btnSignIn.setEnabled(true);
            TextView tvLoginError = (TextView) findViewById(R.id.tvLoginError);

            if (result == RESULT_CANCELED) {
                mAccessResult = RESULT_CANCELED;
                tvLoginError.setVisibility(View.VISIBLE);
            }
            else {
                mAccessResult = RESULT_OK;
                tvLoginError.setVisibility(View.GONE);
                finish();
            }
        }
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, mAccessToken);

        setResult(mAccessResult, data);
        super.finish();

        // adding this here to ensure cleanup
        Util.clearWebviewAndCookies(new WebView(this), this);
    }
}
