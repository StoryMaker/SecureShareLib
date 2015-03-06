
package io.scal.secureshareui.login;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.lib.ZWrapper;
import io.scal.secureshareuilibrary.R;

public class ZLoginActivity extends Activity {

    private int mAccessResult = Activity.RESULT_CANCELED;
    private int CODE = 0;

    private String mClientId;
    private String mClientSecret;
    private String mAccessToken = null;

    //private Button btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClientId = getString(R.string.z_key);
        mClientSecret = getString(R.string.z_secret);

        Log.d("OAUTH", "BEGIN");

        Intent i = new Intent(this, ZWebActivity.class);
        startActivityForResult(i, CODE);

        /*
        this.setFinishOnTouchOutside(false);
        setContentView(R.layout.activity_z_login);

        init();
        */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("OAUTH", "RESULT");

        if (requestCode == CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Bundle results = data.getExtras();
                mAccessToken = results.getString("access_token");

                if (mAccessToken != null) {
                    Log.d("OAUTH", "GOT TOKEN: " + mAccessToken);

                    mAccessResult = Activity.RESULT_OK;
                } else {
                    Log.e("OAUTH", "NO TOKEN");
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.e("OAUTH", "ACTIFITY CANCELLED");
            } else {
                Log.e("OAUTH", "UNKNOWN RESULT CODE: " + resultCode);
            }
        } else {
            Log.e("OAUTH", "UNKNOWN REQUEST CODE: " + requestCode);
        }
    }

    /*
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

            final ZWrapper wrapper = new ZWrapper(mClientId, mClientSecret, null);
            String token = null;
            try {
                token = wrapper.login(params[0], params[1], ZLoginActivity.this);
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
    */
}
