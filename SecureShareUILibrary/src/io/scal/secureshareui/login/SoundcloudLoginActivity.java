package io.scal.secureshareui.login;

import io.scal.secureshareuilibrary.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SoundcloudLoginActivity extends Activity {

	private static int mAccessResult;
	private static String mAccessToken;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_soundcloud_login);
		
		finish();
	}
	
	@Override
	public void finish() {
		mAccessResult = -1;
		mAccessToken = "het";
		
		Intent data = new Intent();
		data.putExtra("credentials", mAccessToken);
		
		setResult(mAccessResult, data);
		super.finish();
	}
}
