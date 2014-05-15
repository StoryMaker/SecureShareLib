
package io.scal.secureshareui.controller;

import io.scal.secureshareui.model.Account;
import android.content.Context;
import android.os.Handler;

public class YoutubeSiteController extends SiteController {
    private static final String TAG = "YoutubeSiteController";
    public static final String SITE_NAME = "YouTube";
    public static final String SITE_KEY = "youtube";
    
    public YoutubeSiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public void startAuthentication(Account account) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void upload(String title, String body, String mediaPath, String username, String credentials) {
        // TODO Auto-generated method stub
        
    }
}
