package io.scal.secureshareui.login;

import info.guardianproject.onionkit.ui.OrbotHelper;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.lib.Util;
import io.scal.secureshareuilibrary.R;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.flickr.api.Flickr;
import com.flickr.api.FlickrException;
import com.flickr.api.FlickrProperties;

public class FlickrLoginActivity extends LockableActivity
{
    private static final String TAG = "FlickrLoginActivity";
    
    private int mAccessResult = RESULT_CANCELED;
    private String mAccessToken = null;
    private String mAccessVerifier = null;
    
    private int CODE = 0;
    Flickr f = null;
    String url = null;
    String key;
    String secret;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate()");
        key = getString(R.string.flickr_key);
        secret = getString(R.string.flickr_secret);
        setContentView(R.layout.activity_flickr_login);
        GetAuthorizationUrlTask gauTask = new GetAuthorizationUrlTask(this);
        gauTask.execute();
    }    
    
    class GetAuthorizationUrlTask extends AsyncTask<String, String, String> 
    {
        private FlickrLoginActivity fla;
        
        public GetAuthorizationUrlTask(FlickrLoginActivity fla)
        {
            this.fla = fla;
        }

        @Override
        protected String doInBackground(String... params) 
        {
            String result = fla.getAuthorizationUrl();
            return result;
        }
        
        protected void onPostExecute(String result)
        {
            fla.startFlickrWebActivity();
        }
    }
    
    public String getAuthorizationUrl()
    {
        String path = Environment.getExternalStorageDirectory() + File.separator + "flickr.conf";
        
        Log.d(TAG, "getAuthorizationUrl() path: " + path);
        
        File confFile = new File(path);
        FlickrProperties fProps = new FlickrProperties(confFile); 
        f = new Flickr(key,                          // key
                       secret,                       // secret
                       "http://localhost/callback",  // callback
                       "delete",                     // permissions ("delete" permission allows read/write/delete)
                       fProps);                      // properties

        OrbotHelper orbotHelper = new OrbotHelper(getApplicationContext());
        if(orbotHelper.isOrbotRunning()) 
        {    
            Log.d(TAG, "orbot running, setting proxy");
            
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Util.ORBOT_HOST, Util.ORBOT_HTTP_PORT));
            f.setProxy(proxy);
        }
        else
        {
            Log.d(TAG, "orbot not running, proxy not set");
        }

        url = f.getAuthorizationUrl(); 
        return url;
    }
    
    public void startFlickrWebActivity()
    {
        Log.d(TAG, "startFlickrWebActivity() url: " + url);
        
        Intent i = new Intent(this, FlickrWebActivity.class);
        i.putExtra("authURL", url);
        startActivityForResult(i, CODE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        Log.d(TAG, "onActivityResult()");
        
        if (requestCode == CODE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                Bundle results = data.getExtras();
                mAccessToken = results.getString("token");
                mAccessVerifier = results.getString("verifier");
                
                if ((mAccessToken != null) && (mAccessVerifier != null))
                {
                    Log.d(TAG, "got token and verifier");
                    
                    mAccessResult = RESULT_OK;
                    
                    VerifyTokenTask vtTask = new VerifyTokenTask(this);
                    vtTask.execute();    
                }
                else
                {
                    Log.e(TAG, "failed to get token and verifier"); 
                }
            }
            else if (resultCode == Activity.RESULT_CANCELED)
            {
                Log.e(TAG, "activity cancelled"); 
            }
            else
            {
                Log.e(TAG, "unexpected result"); 
            }
        }
        else 
        {
            Log.e(TAG, "unexpected request"); 
        }
    }

    class VerifyTokenTask extends AsyncTask<String, String, String> 
    {
        private FlickrLoginActivity fla;
    
        public VerifyTokenTask(FlickrLoginActivity fla)
        {
            this.fla = fla;
        }

        @Override
        protected String doInBackground(String... params) 
        {
            fla.verifyToken();
            return "foo";
        }
        
        protected void onPostExecute(String result)
        {
            fla.finish();
        }
    }
    
    public void verifyToken()
    {
        Log.d(TAG, "verifyToken()"); 
        
        try
        {
            f.verifyToken(mAccessVerifier, mAccessToken);
        }
        catch (FlickrException fe)
        {
        	mAccessResult = RESULT_OK;
            Log.e(TAG, "token verification failed: " + fe.getMessage()); 
        }
    }
    
    @Override
    public void finish() 
    {  
        Log.d(TAG, "finish()"); 
        
        Intent data = new Intent();
        data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, mAccessToken); // WHAT ABOUT VERIFIER?
        setResult(mAccessResult, data);
        
        super.finish();
    }
}
