package io.scal.secureshareui.login;

import java.io.IOException;

import io.scal.secureshareui.controller.SoundCloudPublishController;
import io.scal.secureshareui.soundcloud.ApiWrapper;
import io.scal.secureshareui.soundcloud.Token;
import io.scal.secureshareuilibrary.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SoundcloudLoginActivity extends Activity {

	private static int mAccessResult;
	private static String mAccessToken = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth);
		this.setFinishOnTouchOutside(false); 
		setContentView(R.layout.activity_soundcloud_login);
		
		init();
	}
	
	private void init() {
		final EditText etUsername = (EditText) findViewById(R.id.etUsername);
		final EditText etPassword = (EditText) findViewById(R.id.etPassword);
		Button btnSignIn = (Button) findViewById(R.id.btnSignIn);
	
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
			
			final ApiWrapper wrapper = new ApiWrapper(SoundCloudPublishController.APP_CLIENT_ID, 
														SoundCloudPublishController.APP_CLIENT_SECRET, 
														null, 
														null);
			Token token = null;
			try {
				token = wrapper.login(params[0], params[1], Token.SCOPE_NON_EXPIRING);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (token != null) { //success
				mAccessToken = token.access;
				return "-1";
			}
			
			return "0";
		}
		
		@Override 
		protected void onPreExecute() {

		}

		@Override
		protected void onPostExecute(String resultStr) {		
			int result = Integer.parseInt(resultStr);
			
			if (result == 0) {
				mAccessResult = Activity.RESULT_CANCELED;
			}
			else {
				mAccessResult = Activity.RESULT_OK;
				finish();
			}		
		}
	}
	
	@Override
	public void finish() {		
		Intent data = new Intent();
		data.putExtra("credentials", mAccessToken);
		
		setResult(mAccessResult, data);
		super.finish();
	}
}