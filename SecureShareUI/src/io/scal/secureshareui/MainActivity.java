package io.scal.secureshareui;

import io.scal.secureshareui.lib.ChooseAccountFragment;
import io.scal.secureshareui.lib.StoryInfoEditFragment;

import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        addStoryInfoEditFragment();
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
        fragTrans.add(R.id.fragmentLayout, caFragment);
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
