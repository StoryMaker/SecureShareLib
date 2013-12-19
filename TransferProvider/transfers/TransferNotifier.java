/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION;
import static android.provider.Downloads.Impl.STATUS_RUNNING;
import static com.android.providers.downloads.Constants.TAG;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Downloads;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.LongSparseLongArray;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import com.android.providers.downloads.R;

import javax.annotation.concurrent.GuardedBy;

/**
 * Update {@link NotificationManager} to reflect current {@link TransferInfo}
 * states. Collapses similar transfers into a single notification, and builds
 * {@link PendingIntent} that launch towards {@link TransferReceiver}.
 */
public class TransferNotifier {

    private static final int TYPE_ACTIVE = 1;
    private static final int TYPE_WAITING = 2;
    private static final int TYPE_COMPLETE = 3;

    private final Context mContext;
    private final NotificationManager mNotifManager;

    /**
     * Currently active notifications, mapped from clustering tag to timestamp
     * when first shown.
     *
     * @see #buildNotificationTag(TransferInfo)
     */
    @GuardedBy("mActiveNotifs")
    private final HashMap<String, Long> mActiveNotifs = Maps.newHashMap();

    /**
     * Current speed of active transfers, mapped from {@link TransferInfo#mId}
     * to speed in bytes per second.
     */
    @GuardedBy("mTransferSpeed")
    private final LongSparseLongArray mTransferSpeed = new LongSparseLongArray();

    /**
     * Last time speed was reproted, mapped from {@link TransferInfo#mId} to
     * {@link SystemClock#elapsedRealtime()}.
     */
    @GuardedBy("mTransferSpeed")
    private final LongSparseLongArray mTransferTouch = new LongSparseLongArray();

    public TransferNotifier(Context context) {
        mContext = context;
        mNotifManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    public void cancelAll() {
        mNotifManager.cancelAll();
    }

    /**
     * Notify the current speed of an active transfer, used for calculating
     * estimated remaining time.
     */
    public void notifyTransferSpeed(long id, long bytesPerSecond) {
        synchronized (mTransferSpeed) {
            if (bytesPerSecond != 0) {
                mTransferSpeed.put(id, bytesPerSecond);
                mTransferTouch.put(id, SystemClock.elapsedRealtime());
            } else {
                mTransferSpeed.delete(id);
                mTransferTouch.delete(id);
            }
        }
    }

    /**
     * Update {@link NotificationManager} to reflect the given set of
     * {@link TransferInfo}, adding, collapsing, and removing as needed.
     */
    public void updateWith(Collection<TransferInfo> transfers) {
        synchronized (mActiveNotifs) {
            updateWithLocked(transfers);
        }
    }

    private void updateWithLocked(Collection<TransferInfo> transfers) {
        final Resources res = mContext.getResources();

        // Cluster transfers together
        final Multimap<String, TransferInfo> clustered = ArrayListMultimap.create();
        for (TransferInfo info : transfers) {
            final String tag = buildNotificationTag(info);
            if (tag != null) {
                clustered.put(tag, info);
            }
        }

        // Build notification for each cluster
        for (String tag : clustered.keySet()) {
            final int type = getNotificationTagType(tag);
            final Collection<TransferInfo> cluster = clustered.get(tag);

            final Notification.Builder builder = new Notification.Builder(mContext);

            // Use time when cluster was first shown to avoid shuffling
            final long firstShown;
            if (mActiveNotifs.containsKey(tag)) {
                firstShown = mActiveNotifs.get(tag);
            } else {
                firstShown = System.currentTimeMillis();
                mActiveNotifs.put(tag, firstShown);
            }
            builder.setWhen(firstShown);

            // Show relevant icon
            if (type == TYPE_ACTIVE) {
                builder.setSmallIcon(android.R.drawable.stat_sys_download);
            } else if (type == TYPE_WAITING) {
                builder.setSmallIcon(android.R.drawable.stat_sys_warning);
            } else if (type == TYPE_COMPLETE) {
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
            }

            // Build action intents
            if (type == TYPE_ACTIVE || type == TYPE_WAITING) {
                // build a synthetic uri for intent identification purposes
                final Uri uri = new Uri.Builder().scheme("active-dl").appendPath(tag).build();
                final Intent intent = new Intent(Constants.ACTION_LIST,
                        uri, mContext, TransferReceiver.class);
                intent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS,
                        getTransferIds(cluster));
                builder.setContentIntent(PendingIntent.getBroadcast(mContext,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
                builder.setOngoing(true);

            } else if (type == TYPE_COMPLETE) {
                final TransferInfo info = cluster.iterator().next();
                final Uri uri = ContentUris.withAppendedId(
                        Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, info.mId);
                builder.setAutoCancel(true);

                final String action;
                if (Downloads.Impl.isStatusError(info.mStatus)) {
                    action = Constants.ACTION_LIST;
                } else {
                    if (info.mDestination != Downloads.Impl.DESTINATION_SYSTEMCACHE_PARTITION) {
                        action = Constants.ACTION_OPEN;
                    } else {
                        action = Constants.ACTION_LIST;
                    }
                }

                final Intent intent = new Intent(action, uri, mContext, TransferReceiver.class);
                intent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS,
                        getTransferIds(cluster));
                builder.setContentIntent(PendingIntent.getBroadcast(mContext,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

                final Intent hideIntent = new Intent(Constants.ACTION_HIDE,
                        uri, mContext, TransferReceiver.class);
                builder.setDeleteIntent(PendingIntent.getBroadcast(mContext, 0, hideIntent, 0));
            }

            // Calculate and show progress
            String remainingText = null;
            String percentText = null;
            if (type == TYPE_ACTIVE) {
                long current = 0;
                long total = 0;
                long speed = 0;
                synchronized (mTransferSpeed) {
                    for (TransferInfo info : cluster) {
                        if (info.mTotalBytes != -1) {
                            current += info.mCurrentBytes;
                            total += info.mTotalBytes;
                            speed += mTransferSpeed.get(info.mId);
                        }
                    }
                }

                if (total > 0) {
                    final int percent = (int) ((current * 100) / total);
                    percentText = res.getString(R.string.download_percent, percent);

                    if (speed > 0) {
                        final long remainingMillis = ((total - current) * 1000) / speed;
                        remainingText = res.getString(R.string.download_remaining,
                                DateUtils.formatDuration(remainingMillis));
                    }

                    builder.setProgress(100, percent, false);
                } else {
                    builder.setProgress(100, 0, true);
                }
            }

            // Build titles and description
            final Notification notif;
            if (cluster.size() == 1) {
                final TransferInfo info = cluster.iterator().next();

                builder.setContentTitle(getTransferTitle(res, info));

                if (type == TYPE_ACTIVE) {
                    if (!TextUtils.isEmpty(info.mDescription)) {
                        builder.setContentText(info.mDescription);
                    } else {
                        builder.setContentText(remainingText);
                    }
                    builder.setContentInfo(percentText);

                } else if (type == TYPE_WAITING) {
                    builder.setContentText(
                            res.getString(R.string.notification_need_wifi_for_size));

                } else if (type == TYPE_COMPLETE) {
                    if (Downloads.Impl.isStatusError(info.mStatus)) {
                        builder.setContentText(res.getText(R.string.notification_download_failed));
                    } else if (Downloads.Impl.isStatusSuccess(info.mStatus)) {
                        builder.setContentText(
                                res.getText(R.string.notification_download_complete));
                    }
                }

                notif = builder.build();

            } else {
                final Notification.InboxStyle inboxStyle = new Notification.InboxStyle(builder);

                for (TransferInfo info : cluster) {
                    inboxStyle.addLine(getTransferTitle(res, info));
                }

                if (type == TYPE_ACTIVE) {
                    builder.setContentTitle(res.getQuantityString(
                            R.plurals.notif_summary_active, cluster.size(), cluster.size()));
                    builder.setContentText(remainingText);
                    builder.setContentInfo(percentText);
                    inboxStyle.setSummaryText(remainingText);

                } else if (type == TYPE_WAITING) {
                    builder.setContentTitle(res.getQuantityString(
                            R.plurals.notif_summary_waiting, cluster.size(), cluster.size()));
                    builder.setContentText(
                            res.getString(R.string.notification_need_wifi_for_size));
                    inboxStyle.setSummaryText(
                            res.getString(R.string.notification_need_wifi_for_size));
                }

                notif = inboxStyle.build();
            }

            mNotifManager.notify(tag, 0, notif);
        }

        // Remove stale tags that weren't renewed
        final Iterator<String> it = mActiveNotifs.keySet().iterator();
        while (it.hasNext()) {
            final String tag = it.next();
            if (!clustered.containsKey(tag)) {
                mNotifManager.cancel(tag, 0);
                it.remove();
            }
        }
    }

    private static CharSequence getTransferTitle(Resources res, TransferInfo info) {
        if (!TextUtils.isEmpty(info.mTitle)) {
            return info.mTitle;
        } else {
            return res.getString(R.string.download_unknown_title);
        }
    }

    private long[] getTransferIds(Collection<TransferInfo> infos) {
        final long[] ids = new long[infos.size()];
        int i = 0;
        for (TransferInfo info : infos) {
            ids[i++] = info.mId;
        }
        return ids;
    }

    public void dumpSpeeds() {
        synchronized (mTransferSpeed) {
            for (int i = 0; i < mTransferSpeed.size(); i++) {
                final long id = mTransferSpeed.keyAt(i);
                final long delta = SystemClock.elapsedRealtime() - mTransferTouch.get(id);
                Log.d(TAG, "Transfer " + id + " speed " + mTransferSpeed.valueAt(i) + "bps, "
                        + delta + "ms ago");
            }
        }
    }

    /**
     * Build tag used for collapsing several {@link TransferInfo} into a single
     * {@link Notification}.
     */
    private static String buildNotificationTag(TransferInfo info) {
        if (info.mStatus == Downloads.Impl.STATUS_QUEUED_FOR_WIFI) {
            return TYPE_WAITING + ":" + info.mPackage;
        } else if (isActiveAndVisible(info)) {
            return TYPE_ACTIVE + ":" + info.mPackage;
        } else if (isCompleteAndVisible(info)) {
            // Complete transfers always have unique notifs
            return TYPE_COMPLETE + ":" + info.mId;
        } else {
            return null;
        }
    }

    /**
     * Return the cluster type of the given tag, as created by
     * {@link #buildNotificationTag(TransferInfo)}.
     */
    private static int getNotificationTagType(String tag) {
        return Integer.parseInt(tag.substring(0, tag.indexOf(':')));
    }

    private static boolean isActiveAndVisible(TransferInfo transfer) {
        return transfer.mStatus == STATUS_RUNNING &&
                (transfer.mVisibility == VISIBILITY_VISIBLE
                || transfer.mVisibility == VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
    }

    private static boolean isCompleteAndVisible(TransferInfo transfer) {
        return Downloads.Impl.isStatusCompleted(transfer.mStatus) &&
                (transfer.mVisibility == VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                || transfer.mVisibility == VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
    }
}
