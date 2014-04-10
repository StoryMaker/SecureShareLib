package io.scal.secureshareui;

import io.scal.secureshareui.lib.*;

import android.os.Bundle;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.view.Menu;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        addStoryInfoFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
     
    public void addChooseAccountFragment() {
    	FragmentManager fragManager = getFragmentManager();
    	FragmentTransaction fragTrans = fragManager.beginTransaction();
    	     
        ChooseAccountFragment caFragment = new ChooseAccountFragment();
        fragTrans.add(R.id.fragmentLayout, caFragment);
        fragTrans.commit();
    }
    
    public void addStoryInfoFragment() {
    	FragmentManager fragManager = getFragmentManager();
    	FragmentTransaction fragTrans = fragManager.beginTransaction();
    	     
    	StoryInfoFragment siFragment = new StoryInfoFragment();
        fragTrans.add(R.id.fragmentLayout, siFragment);
        fragTrans.commit();
    }
    
    public void addStoryInfoEditFragment() {
    	FragmentManager fragManager = getFragmentManager();
    	FragmentTransaction fragTrans = fragManager.beginTransaction();
    	     
    	StoryInfoEditFragment sieFragment = new StoryInfoEditFragment();
        fragTrans.add(R.id.fragmentLayout, sieFragment);
        fragTrans.commit();
    }
}
