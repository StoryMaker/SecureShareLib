
package io.scal.secureshareui.login;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SoundCloudSiteController;
import io.scal.secureshareui.lib.SMWrapper;
import io.scal.secureshareui.soundcloud.ApiWrapper;
import io.scal.secureshareui.soundcloud.Token;
import io.scal.secureshareuilibrary.R;

public class SMLoginActivity extends Activity {

    private int mAccessResult = Activity.RESULT_CANCELED;

    private String mClientId;
    private String mClientSecret;
    private String mAccessToken = null;

    private Button btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClientId = getString(R.string.sm_key);
        mClientSecret = getString(R.string.sm_secret);

        this.setFinishOnTouchOutside(false);
        setContentView(R.layout.activity_sm_login);

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

            final SMWrapper wrapper = new SMWrapper(mClientId, mClientSecret, null);
            String token = null;
            try {
                token = wrapper.login(params[0], params[1], SMLoginActivity.this);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (token != null) { // success
                mAccessToken = token;
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
    }
}
