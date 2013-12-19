package io.scal.secureshareui;

import io.scal.secureshareui.lib.ChooseAccountFragment;

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
        
        addFragmentToUI();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    
    public void addFragmentToUI(){
    	FragmentManager fragManager = getFragmentManager();
    	FragmentTransaction fragTrans = fragManager.beginTransaction();
    	     
        ChooseAccountFragment caFragment = new ChooseAccountFragment();
        fragTrans.add(R.id.fragmentLayout, caFragment);
        fragTrans.commit();      
    }
}
