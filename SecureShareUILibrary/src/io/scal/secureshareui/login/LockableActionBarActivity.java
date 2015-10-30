package io.scal.secureshareui.login;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import io.scal.secureshareuilibrary.R;

/**
 * Created by mnbogner on 4/14/15.
 */
public class LockableActionBarActivity extends ActionBarActivity implements ICacheWordSubscriber {

    protected CacheWordHandler mCacheWordHandler;
    protected String CACHEWORD_UNSET;
    protected String CACHEWORD_FIRST_LOCK;
    protected String CACHEWORD_SET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CACHEWORD_UNSET = getText(R.string.cacheword_state_unset).toString();
        CACHEWORD_FIRST_LOCK = getText(R.string.cacheword_state_first_lock).toString();
        CACHEWORD_SET = getText(R.string.cacheword_state_set).toString();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int timeout = Integer.parseInt(settings.getString("pcachewordtimeout", "600"));
        mCacheWordHandler = new CacheWordHandler(this, timeout); // TODO: timeout of -1 represents no timeout (revisit)
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCacheWordHandler.disconnectFromService();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // only display notification if the user has set a pin
        SharedPreferences sp = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String cachewordStatus = sp.getString("cacheword_status", "default");
        if (cachewordStatus.equals(CACHEWORD_SET)) {
            Log.d("CACHEWORD", "pin set, so display notification (lockable)");
            mCacheWordHandler.setNotification(buildNotification(this));
        } else {
            Log.d("CACHEWORD", "no pin set, so no notification (lockable)");
        }

        mCacheWordHandler.connectToService();
    }

    protected Notification buildNotification(Context c) {

        Log.d("CACHEWORD", "buildNotification (lockable)");

        NotificationCompat.Builder b = new NotificationCompat.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_menu_info_details); //ic_menu_key was missing
        b.setContentTitle(c.getText(R.string.cacheword_notification_cached_title));
        b.setContentText(c.getText(R.string.cacheword_notification_cached_message));
        b.setTicker(c.getText(R.string.cacheword_notification_cached));
        b.setWhen(System.currentTimeMillis());
        b.setOngoing(true);
        b.setContentIntent(CacheWordHandler.getPasswordLockPendingIntent(c));
        return b.build();
    }

    @Override
    public void onCacheWordUninitialized() {

        // if we're uninitialized, default behavior should be to stop
        Log.d("CACHEWORD", "cacheword uninitialized, activity will not continue");
        finish();

    }

    @Override
    public void onCacheWordLocked() {

        // if we're locked, default behavior should be to stop
        Log.d("CACHEWORD", "cacheword locked, activity will not continue");
        finish();

    }

    @Override
    public void onCacheWordOpened() {

        // if we're opened, check db and update menu status
        Log.d("CACHEWORD", "cacheword opened, activity will continue");

    }
}
