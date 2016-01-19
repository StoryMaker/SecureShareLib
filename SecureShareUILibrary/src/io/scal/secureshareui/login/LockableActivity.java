package io.scal.secureshareui.login;

import timber.log.Timber;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import io.scal.secureshareuilibrary.R;

/**
 * Created by mnbogner on 4/14/15.
 */
public class LockableActivity extends Activity implements ICacheWordSubscriber {

    protected CacheWordHandler mCacheWordHandler;
    public static final String CACHEWORD_UNSET = "unset";
    public static final String CACHEWORD_FIRST_LOCK = "first_lock";
    public static final String CACHEWORD_SET = "set";
    public static final String CACHEWORD_TIMEOUT = "300";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int timeout = Integer.parseInt(settings.getString("pcachewordtimeout", CACHEWORD_TIMEOUT));
        mCacheWordHandler = new CacheWordHandler(this, timeout);
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
            Timber.d("pin set, so display notification (lockable)");
            mCacheWordHandler.setNotification(buildNotification(this));
        } else {
            Timber.d("no pin set, so no notification (lockable)");
        }

        mCacheWordHandler.connectToService();
    }

    protected Notification buildNotification(Context c) {

        Timber.d("buildNotification (lockable)");

        NotificationCompat.Builder b = new NotificationCompat.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_menu_info_details);
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
        Timber.d("cacheword uninitialized, activity will not continue");
        finish();

    }

    @Override
    public void onCacheWordLocked() {

        // if we're locked, default behavior should be to stop
        Timber.d("cacheword locked, activity will not continue");
        finish();

    }

    @Override
    public void onCacheWordOpened() {

        // if we're opened, check db and update menu status
        Timber.d("cacheword opened, activity will continue");

    }
}
