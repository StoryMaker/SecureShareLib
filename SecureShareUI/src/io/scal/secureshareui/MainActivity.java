package io.scal.secureshareui;

import java.util.ArrayList;
import java.util.List;

import io.scal.secureshareui.controller.PublishController.OnPublishEventListener;
import io.scal.secureshareui.lib.*;
import io.scal.secureshareui.model.PublishAccount;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        addChooseAccountFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
     
    public void addChooseAccountFragment() {
    	FragmentManager fragManager = getSupportFragmentManager();
    	FragmentTransaction fragTrans = fragManager.beginTransaction();
    	     
        ChooseAccountFragment caFragment = new ChooseAccountFragment();	
        List<PublishAccount> accounts = new ArrayList<PublishAccount>();
        
        accounts.add(new PublishAccount("1", "facebook", "site", "username", "credentials", true));
        accounts.add(new PublishAccount("2", "soundcloud", "site", "username", "credentials", false));
        accounts.add(new PublishAccount("3", "storymaker cc", "site", "username", "credentials", false));
        accounts.add(new PublishAccount("4", "wordpress", "site", "username", "credentials", false));   
        caFragment.setPublishAccountsList(accounts);
        
        
        caFragment.setOnPublishEventListener(new OnPublishEventListener() {

			@Override
			public void onSuccess(PublishAccount publishAccount) {
				Toast.makeText(getApplicationContext(), publishAccount.getName(), Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onFailure(PublishAccount publishAccount, String failureMessage) {
				
			}
		});
        
        fragTrans.add(R.id.fragmentLayout, caFragment);
        fragTrans.commit();
    }
    
    public void addStoryInfoFragment() {
    	FragmentManager fragManager = getSupportFragmentManager();
    	FragmentTransaction fragTrans = fragManager.beginTransaction();
    	     
    	StoryInfoFragment siFragment = new StoryInfoFragment();
        fragTrans.add(R.id.fragmentLayout, siFragment);
        fragTrans.commit();
    }
    
    public void addStoryInfoEditFragment() {
    	FragmentManager fragManager = getSupportFragmentManager();
    	FragmentTransaction fragTrans = fragManager.beginTransaction();
    	     
    	StoryInfoEditFragment sieFragment = new StoryInfoEditFragment();
        fragTrans.add(R.id.fragmentLayout, sieFragment);
        fragTrans.commit();
    }
}
