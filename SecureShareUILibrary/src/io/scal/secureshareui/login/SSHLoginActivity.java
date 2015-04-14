
package io.scal.secureshareui.login;

import io.scal.secureshareui.controller.SSHSiteController;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareuilibrary.R;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SSHLoginActivity extends LockableActivity {
    private static final String TAG = "SFTPLoginActivity";
    
    public final static String DATA_KEY_SERVER_URL = "server_url";
    public final static String DATA_KEY_SERVER_NICKNAME = "server_nickname";
    public final static String DATA_KEY_REMOTE_PATH = "remote_path";

    private int mAccessResult = Activity.RESULT_CANCELED;
    EditText mUsername;
    EditText mPassword;
    EditText mNickname;
    EditText mURL;
    EditText mRemotePath;
    
    private Button btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth);
        this.setFinishOnTouchOutside(false);
        setContentView(R.layout.activity_ssh_login);

        init();
    }

    private void init() {
        mUsername = (EditText) findViewById(R.id.etUsername);
        mPassword = (EditText) findViewById(R.id.etPassword);
        mNickname = (EditText) findViewById(R.id.etServerName);
        mURL = (EditText) findViewById(R.id.etServerURL);
        mRemotePath = (EditText) findViewById(R.id.etRemotePath);
        btnSignIn = (Button) findViewById(R.id.btnSignIn);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setEnabled(false);

                String username = mUsername.getText().toString();
                String password = mPassword.getText().toString();
                String host = mURL.getText().toString();

                new CheckCredentialsAsync().execute(username, password, host);
            }
        });
    }

    private class CheckCredentialsAsync extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {       
            if (SSHSiteController.SSH.checkCredentials(params[0], params[1], params[2])) { // success
                return Integer.toString(Activity.RESULT_OK);
            }

            return Integer.toString(Activity.RESULT_CANCELED);
        }

        @Override
        protected void onPostExecute(String resultStr) {
            int result = Integer.parseInt(resultStr);

            btnSignIn.setEnabled(true);
            TextView tvLoginError = (TextView) findViewById(R.id.tvLoginError);

            if (result == Activity.RESULT_CANCELED) {
                mAccessResult = Activity.RESULT_CANCELED;
                tvLoginError.setVisibility(View.VISIBLE);
            }
            else {
                mAccessResult = Activity.RESULT_OK;
                tvLoginError.setVisibility(View.GONE);
                finish();
            }
        }
    }

    @Override
    public void finish() {
        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();
        String nickname = mNickname.getText().toString();
        String url = mURL.getText().toString();
        String remotePath = mRemotePath.getText().toString();
        
        Intent data = new Intent();
        data.putExtra(SiteController.EXTRAS_KEY_USERNAME, username);
        data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, password);
        try {
            JSONObject obj = new JSONObject();
            obj.put(DATA_KEY_SERVER_URL, url);
            obj.put(DATA_KEY_SERVER_NICKNAME, nickname);
            obj.put(DATA_KEY_REMOTE_PATH, remotePath);
            data.putExtra(SiteController.EXTRAS_KEY_DATA, obj.toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        setResult(mAccessResult, data);
        super.finish();
    }
}
