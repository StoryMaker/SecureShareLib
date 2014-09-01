package io.scal.secureshareui.login;

import io.scal.secureshareui.controller.SiteController;

import org.holoeverywhere.app.Activity;

import android.content.Intent;
import android.os.Bundle;

public class ArchiveLoginActivity extends Activity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }

    @Override
    public void finish() {      
        Intent data = new Intent();
        data.putExtra(SiteController.EXTRAS_KEY_USERNAME, "username"); // FIXME dummy login activity, needs for reals
        data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, "password");

        setResult(Activity.RESULT_OK, data);
        super.finish();
    }
}
