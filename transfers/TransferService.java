/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.providers.transfers;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static com.android.providers.downloads.Constants.TAG;

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.google.android.collect.Maps;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Performs background transfers as requested by applications that use
 * {@link TransferManager}. Multiple start commands can be issued at this
 * service, and it will continue running until no transfers are being actively
 * processed. It may schedule alarms to resume transfers in future.
 * <p>
 * Any database updates important enough to initiate tasks should always be
 * delivered through {@link Context#startService(Intent)}.
 */
public class TransferService extends Service {
    // TODO: migrate WakeLock from individual TransferThreads out into
    // TransferReceiver to protect our entire workflow.

    private static final boolean DEBUG_LIFECYCLE = false;

    @VisibleForTesting
    SystemFacade mSystemFacade;

    private AlarmManager mAlarmManager;
    private StorageManager mStorageManager;

    /** Observer to get notified when the content observer's data changes */
    private TransferManagerContentObserver mObserver;

    /** Class to handle Notification Manager updates */
    private TransferNotifier mNotifier;

    /**
     * The Service's view of the list of transfers, mapping transfer IDs to the corresponding info
     * object. This is kept independently from the content provider, and the Service only initiates
     * transfers based on this data, so that it can deal with situation where the data in the
     * content provider changes or disappears.
     */
    @GuardedBy("mTransfers")
    private final Map<Long, TransferInfo> mTransfers = Maps.newHashMap();

    private final ExecutorService mExecutor = buildTransferExecutor();

    private static ExecutorService buildTransferExecutor() {
        final int maxConcurrent = Resources.getSystem().getInteger(
                com.android.internal.R.integer.config_MaxConcurrentDownloadsAllowed);

        // Create a bounded thread pool for executing transfers; it creates
        // threads as needed (up to maximum) and reclaims them when finished.
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                maxConcurrent, maxConcurrent, 10, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private TransferScanner mScanner;

    private HandlerThread mUpdateThread;
    private Handler mUpdateHandler;

    private volatile int mLastStartId;

    /**
     * Receives notifications when the data in the content provider changes
     */
    private class TransferManagerContentObserver extends ContentObserver {
        public TransferManagerContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(final boolean selfChange) {
            enqueueUpdate();
        }
    }

    /**
     * Returns an IBinder instance when someone wants to connect to this
     * service. Binding to this service is not allowed.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public IBinder onBind(Intent i) {
        throw new UnsupportedOperationException("Cannot bind to Transfer Manager Service");
    }

    /**
     * Initializes the service when it is first created
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "Service onCreate");
        }

        if (mSystemFacade == null) {
            mSystemFacade = new RealSystemFacade(this);
        }

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mStorageManager = new StorageManager(this);

        mUpdateThread = new HandlerThread(TAG + "-UpdateThread");
        mUpdateThread.start();
        mUpdateHandler = new Handler(mUpdateThread.getLooper(), mUpdateCallback);

        mScanner = new TransferScanner(this);

        mNotifier = new TransferNotifier(this);
        mNotifier.cancelAll();

        mObserver = new TransferManagerContentObserver();
        getContentResolver().registerContentObserver(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                true, mObserver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int returnValue = super.onStartCommand(intent, flags, startId);
        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "Service onStart");
        }
        mLastStartId = startId;
        enqueueUpdate();
        return returnValue;
    }

    @Override
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(mObserver);
        mScanner.shutdown();
        mUpdateThread.quit();
        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "Service onDestroy");
        }
        super.onDestroy();
    }

    /**
     * Enqueue an {@link #updateLocked()} pass to occur in future.
     */
    private void enqueueUpdate() {
        mUpdateHandler.removeMessages(MSG_UPDATE);
        mUpdateHandler.obtainMessage(MSG_UPDATE, mLastStartId, -1).sendToTarget();
    }

    /**
     * Enqueue an {@link #updateLocked()} pass to occur after delay, usually to
     * catch any finished operations that didn't trigger an update pass.
     */
    private void enqueueFinalUpdate() {
        mUpdateHandler.removeMessages(MSG_FINAL_UPDATE);
        mUpdateHandler.sendMessageDelayed(
                mUpdateHandler.obtainMessage(MSG_FINAL_UPDATE, mLastStartId, -1),
                5 * MINUTE_IN_MILLIS);
    }

    private static final int MSG_UPDATE = 1;
    private static final int MSG_FINAL_UPDATE = 2;

    private Handler.Callback mUpdateCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            final int startId = msg.arg1;
            if (DEBUG_LIFECYCLE) Log.v(TAG, "Updating for startId " + startId);

            // Since database is current source of truth, our "active" status
            // depends on database state. We always get one final update pass
            // once the real actions have finished and persisted their state.

            // TODO: switch to asking real tasks to derive active state
            // TODO: handle media scanner timeouts

            final boolean isActive;
            synchronized (mTransfers) {
                isActive = updateLocked();
            }

            if (msg.what == MSG_FINAL_UPDATE) {
                // Dump thread stacks belonging to pool
                for (Map.Entry<Thread, StackTraceElement[]> entry :
                        Thread.getAllStackTraces().entrySet()) {
                    if (entry.getKey().getName().startsWith("pool")) {
                        Log.d(TAG, entry.getKey() + ": " + Arrays.toString(entry.getValue()));
                    }
                }

                // Dump speed and update details
                mNotifier.dumpSpeeds();

                Log.wtf(TAG, "Final update pass triggered, isActive=" + isActive
                        + "; someone didn't update correctly.");
            }

            if (isActive) {
                // Still doing useful work, keep service alive. These active
                // tasks will trigger another update pass when they're finished.

                // Enqueue delayed update pass to catch finished operations that
                // didn't trigger an update pass; these are bugs.
                enqueueFinalUpdate();

            } else {
                // No active tasks, and any pending update messages can be
                // ignored, since any updates important enough to initiate tasks
                // will always be delivered with a new startId.

                if (stopSelfResult(startId)) {
                    if (DEBUG_LIFECYCLE) Log.v(TAG, "Nothing left; stopped");
                    getContentResolver().unregisterContentObserver(mObserver);
                    mScanner.shutdown();
                    mUpdateThread.quit();
                }
            }

            return true;
        }
    };

    /**
     * Update {@link #mTransfers} to match {@link TransferProvider} state.
     * Depending on current transfer state it may enqueue {@link TransferThread}
     * instances, request {@link TransferScanner} scans, update user-visible
     * notifications, and/or schedule future actions with {@link AlarmManager}.
     * <p>
     * Should only be called from {@link #mUpdateThread} as after being
     * requested through {@link #enqueueUpdate()}.
     *
     * @return If there are active tasks being processed, as of the database
     *         snapshot taken in this update.
     */
    private boolean updateLocked() {
        final long now = mSystemFacade.currentTimeMillis();

        boolean isActive = false;
        long nextActionMillis = Long.MAX_VALUE;

        final Set<Long> staleIds = Sets.newHashSet(mTransfers.keySet());

        final ContentResolver resolver = getContentResolver();
        final Cursor cursor = resolver.query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                null, null, null, null);
        try {
            final TransferInfo.Reader reader = new TransferInfo.Reader(resolver, cursor);
            final int idColumn = cursor.getColumnIndexOrThrow(Downloads.Impl._ID);
            while (cursor.moveToNext()) {
                final long id = cursor.getLong(idColumn);
                staleIds.remove(id);

                TransferInfo info = mTransfers.get(id);
                if (info != null) {
                    updateTransfer(reader, info, now);
                } else {
                    info = insertTransferLocked(reader, now);
                }

                if (info.mDeleted) {
                    // Delete transfer if requested, but only after cleaning up
                    if (!TextUtils.isEmpty(info.mMediaProviderUri)) {
                        resolver.delete(Uri.parse(info.mMediaProviderUri), null, null);
                    }

                    deleteFileIfExists(info.mFileName);
                    resolver.delete(info.getAllDownloadsUri(), null, null);

                } else {
                    // Kick off transfer task if ready
                    final boolean activeTransfer = info.startTransferIfReady(mExecutor);

                    // Kick off media scan if completed
                    final boolean activeScan = info.startScanIfReady(mScanner);

                    if (DEBUG_LIFECYCLE && (activeTransfer || activeScan)) {
                        Log.v(TAG, "Transfer " + info.mId + ": activeTransfer=" + activeTransfer
                                + ", activeScan=" + activeScan);
                    }

                    isActive |= activeTransfer;
                    isActive |= activeScan;
                }

                // Keep track of nearest next action
                nextActionMillis = Math.min(info.nextActionMillis(now), nextActionMillis);
            }
        } finally {
            cursor.close();
        }

        // Clean up stale transfers that disappeared
        for (Long id : staleIds) {
            deleteTransferLocked(id);
        }

        // Update notifications visible to user
        mNotifier.updateWith(mTransfers.values());

        // Set alarm when next action is in future. It's okay if the service
        // continues to run in meantime, since it will kick off an update pass.
        if (nextActionMillis > 0 && nextActionMillis < Long.MAX_VALUE) {
            if (Constants.LOGV) {
                Log.v(TAG, "scheduling start in " + nextActionMillis + "ms");
            }

            final Intent intent = new Intent(Constants.ACTION_RETRY);
            intent.setClass(this, TransferReceiver.class);
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, now + nextActionMillis,
                    PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT));
        }

        return isActive;
    }

    /**
     * Keeps a local copy of the info about a transfer, and initiates the
     * transfer if appropriate.
     */
    private TransferInfo insertTransferLocked(TransferInfo.Reader reader, long now) {
        final TransferInfo info = reader.newTransferInfo(
                this, mSystemFacade, mStorageManager, mNotifier);
        mTransfers.put(info.mId, info);

        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "processing inserted transfer " + info.mId);
        }

        return info;
    }

    /**
     * Updates the local copy of the info about a transfer.
     */
    private void updateTransfer(TransferInfo.Reader reader, TransferInfo info, long now) {
        reader.updateFromDatabase(info);
        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "processing updated transfer " + info.mId +
                    ", status: " + info.mStatus);
        }
    }

    /**
     * Removes the local copy of the info about a transfer.
     */
    private void deleteTransferLocked(long id) {
        TransferInfo info = mTransfers.get(id);
        if (info.mStatus == Downloads.Impl.STATUS_RUNNING) {
            info.mStatus = Downloads.Impl.STATUS_CANCELED;
        }
        if (info.mDestination != Downloads.Impl.DESTINATION_EXTERNAL && info.mFileName != null) {
            if (Constants.LOGVV) {
                Log.d(TAG, "deleteTransferLocked() deleting " + info.mFileName);
            }
            deleteFileIfExists(info.mFileName);
        }
        mTransfers.remove(info.mId);
    }

    private void deleteFileIfExists(String path) {
        if (!TextUtils.isEmpty(path)) {
            if (Constants.LOGVV) {
                Log.d(TAG, "deleteFileIfExists() deleting " + path);
            }
            final File file = new File(path);
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "file: '" + path + "' couldn't be deleted");
            }
        }
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        synchronized (mTransfers) {
            final List<Long> ids = Lists.newArrayList(mTransfers.keySet());
            Collections.sort(ids);
            for (Long id : ids) {
                final TransferInfo info = mTransfers.get(id);
                info.dump(pw);
            }
        }
    }
}
