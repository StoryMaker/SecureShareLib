package io.scal.secureshareui.lib;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareuilibrary.R;

public class ArchiveMetadataActivity extends Activity {
    public static final String TAG = "ArchiveMetadataActivity";
    
    public static final String INTENT_EXTRA_SHARE_TITLE = "archive-share-title";
    public static final String INTENT_EXTRA_SHARE_DESCRIPTION = "archive-share-description";
    public static final String INTENT_EXTRA_SHARE_AUTHOR = "archive-share-author";
    public static final String INTENT_EXTRA_SHARE_TAGS = "archive-share-tags";
    public static final String INTENT_EXTRA_SHARE_LOCATION = "archive-share-location";
    public static final String INTENT_EXTRA_LICENSE_URL = "archive-share-licsense-url";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_archive_metadata);
		Button button = (Button) findViewById(R.id.buttonSubmit);

        final Switch title = (Switch) findViewById(R.id.tb_title);
        final Switch description = (Switch) findViewById(R.id.tb_description);
        final Switch author = (Switch) findViewById(R.id.tb_author);
        final Switch tags = (Switch) findViewById(R.id.tb_tags);
        final Switch location = (Switch) findViewById(R.id.tb_location);

        final Intent i = getIntent();
        Bundle extras = i.getExtras();
        
//        for (String key : extras.keySet()) {
//            Object value = extras.get(key);
//            Log.d(TAG, String.format("%s %s (%s)", key,  
//                value.toString(), value.getClass().getName()));
//        }
        
        final TextView tvtitle = (TextView) findViewById(R.id.tv_title_desc);
        tvtitle.setText(extras.getString(SiteController.VALUE_KEY_TITLE, "(No title shared)"));
                
        final TextView tvdescription = (TextView) findViewById(R.id.tv_description_desc);
        tvdescription.setText(extras.getString(SiteController.VALUE_KEY_BODY, "(No description shared)"));
        
        final TextView tvauthor = (TextView) findViewById(R.id.tv_author_desc);
        tvauthor.setText(extras.getString(SiteController.VALUE_KEY_AUTHOR, "(No author shared)"));
        
        final TextView tvtags = (TextView) findViewById(R.id.tv_tags_desc);
        tvtags.setText(extras.getString(SiteController.VALUE_KEY_TAGS, "(No tags shared)"));
        
        final TextView tvlocation = (TextView) findViewById(R.id.tv_location_desc);
        tvlocation.setText(extras.getString(SiteController.VALUE_KEY_LOCATION_NAME, "(No location shared)"));
        
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
		        RadioGroup licenseRg = (RadioGroup) findViewById(R.id.radioGroupCC);
		        
		        String licenseUrl = null;
		        int id = licenseRg.getCheckedRadioButtonId();
		        if (id == R.id.radioBy) {
		            licenseUrl = "https://creativecommons.org/licenses/by/4.0/";
		        } else if (id == R.id.radioBySa) {
		            licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/";
		        } else { // ByNcNd is default
		            licenseUrl = "http://creativecommons.org/licenses/by-nc-nd/4.0/";
		        }
		        
			    
                i.putExtra(INTENT_EXTRA_SHARE_TITLE, title.isChecked());
                i.putExtra(INTENT_EXTRA_SHARE_DESCRIPTION, description.isChecked());
                i.putExtra(INTENT_EXTRA_SHARE_AUTHOR, author.isChecked());
                i.putExtra(INTENT_EXTRA_SHARE_TAGS, tags.isChecked());
                i.putExtra(INTENT_EXTRA_SHARE_LOCATION, location.isChecked());
                i.putExtra(INTENT_EXTRA_LICENSE_URL, licenseUrl);
				setResult(Activity.RESULT_OK, i);
				finish();
			}
		});
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.press_secure, menu);
//		return true;
//	}

//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		// Handle action bar item clicks here. The action bar will
//		// automatically handle clicks on the Home/Up button, so long
//		// as you specify a parent activity in AndroidManifest.xml.
//		int id = item.getItemId();
//		if (id == R.id.action_settings) {
//			return true;
//		}
//		return super.onOptionsItemSelected(item);
//	}
}
