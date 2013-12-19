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

import static android.provider.Downloads.Impl.STATUS_BAD_REQUEST;
import static android.provider.Downloads.Impl.STATUS_CANNOT_RESUME;
import static android.provider.Downloads.Impl.STATUS_FILE_ERROR;
import static android.provider.Downloads.Impl.STATUS_HTTP_DATA_ERROR;
import static android.provider.Downloads.Impl.STATUS_SUCCESS;
import static android.provider.Downloads.Impl.STATUS_TOO_MANY_REDIRECTS;
import static android.provider.Downloads.Impl.STATUS_WAITING_FOR_NETWORK;
import static android.provider.Downloads.Impl.STATUS_WAITING_TO_RETRY;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static com.android.providers.transfers.Constants.TAG;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.drm.DrmManagerClient;
import android.drm.DrmOutputStream;
import android.net.ConnectivityManager;
import android.net.INetworkPolicyListener;
import android.net.NetworkInfo;
import android.net.NetworkPolicyManager;
import android.net.TrafficStats;
import android.os.FileUtils;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.os.WorkSource;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.android.providers.transfers.Constants;
import com.android.providers.transfers.TransferInfo;
import com.android.providers.transfers.TransferProvider;
import com.android.providers.transfers.StopRequestException;
import com.android.providers.transfers.StorageManager;
import com.android.providers.transfers.SystemFacade;
import com.android.providers.transfers.TransferInfo.NetworkState;

import libcore.io.IoUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Task which executes a given {@link DownloadInfo}: making network requests,
 * persisting data to disk, and updating {@link DownloadProvider}.
 */
public abstract class TransferThread implements Runnable {

    // TODO: bind each transfer to a specific network interface to avoid state
    // checking races once we have ConnectivityManager API

    protected static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    protected static final int HTTP_TEMP_REDIRECT = 307;

    protected static final int DEFAULT_TIMEOUT = (int) (20 * SECOND_IN_MILLIS);

    protected final Context mContext;
    protected final TransferInfo mInfo;
    protected final SystemFacade mSystemFacade;
    protected final StorageManager mStorageManager;
    protected final TransferNotifier mNotifier;

    protected volatile boolean mPolicyDirty;
  
    
    public TransferThread(Context context, SystemFacade systemFacade, TransferInfo info,
            StorageManager storageManager, TransferNotifier notifier) {
        mContext = context;
        mSystemFacade = systemFacade;
        mInfo = info;
        mStorageManager = storageManager;
        mNotifier = notifier;
    }

    /**
     * Returns the user agent provided by the initiating app, or use the default one
     */
    private String userAgent() {
        String userAgent = mInfo.mUserAgent;
        if (userAgent == null) {
            userAgent = Constants.DEFAULT_USER_AGENT;
        }
        return userAgent;
    }

    /**
     * State for the entire run() method.
     */
    static class State {
        public String mFilename;
        public String mMimeType;
        public int mRetryAfter = 0;
        public boolean mGotData = false;
        public String mRequestUri;
        public long mTotalBytes = -1;
        public long mCurrentBytes = 0;
        public String mHeaderETag;
        public boolean mContinuingTransfer = false;
        public long mBytesNotified = 0;
        public long mTimeLastNotification = 0;
        public int mNetworkType = ConnectivityManager.TYPE_NONE;

        /** Historical bytes/second speed of this transfer. */
        public long mSpeed;
        /** Time when current sample started. */
        public long mSpeedSampleStart;
        /** Bytes transferred since current sample started. */
        public long mSpeedSampleBytes;

        public long mContentLength = -1;
        public String mContentDisposition;
        public String mContentLocation;

        public int mRedirectionCount;
        public URL mUrl;

        public State(TransferInfo info) {
            mMimeType = Intent.normalizeMimeType(info.mMimeType);
            mRequestUri = info.mUri;
            mFilename = info.mFileName;
            mTotalBytes = info.mTotalBytes;
            mCurrentBytes = info.mCurrentBytes;
        }

        public void resetBeforeExecute() {
            // Reset any state from previous execution
            mContentLength = -1;
            mContentDisposition = null;
            mContentLocation = null;
            mRedirectionCount = 0;
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        try {
            runInternal();
        } finally {
            mNotifier.notifyTransferSpeed(mInfo.mId, 0);
        }
    }

    abstract void runInternal();

    /**
     * Fully execute a single transfer request. Setup and send the request,
     * handle the response, and transfer the data to the destination file.
     */
    abstract void executeTransfer(State state) throws StopRequestException;

    /**
     * Transfer data from the given connection to the destination file.
     */
    abstract void transferData(State state, HttpURLConnection conn) throws StopRequestException;

    /**
     * Check if current connectivity is valid for this request.
     */
    private void checkConnectivity() throws StopRequestException {
        // checking connectivity will apply current policy
        mPolicyDirty = false;

        final NetworkState networkUsable = mInfo.checkCanUseNetwork();
        if (networkUsable != NetworkState.OK) {
            int status = Downloads.Impl.STATUS_WAITING_FOR_NETWORK;
            if (networkUsable == NetworkState.UNUSABLE_DUE_TO_SIZE) {
                status = Downloads.Impl.STATUS_QUEUED_FOR_WIFI;
                mInfo.notifyPauseDueToSize(true);
            } else if (networkUsable == NetworkState.RECOMMENDED_UNUSABLE_DUE_TO_SIZE) {
                status = Downloads.Impl.STATUS_QUEUED_FOR_WIFI;
                mInfo.notifyPauseDueToSize(false);
            }
            throw new StopRequestException(status, networkUsable.name());
        }
    }

    /**
     * Transfer as much data as possible from the HTTP response to the
     * destination file.
     */
    abstract void transferData(State state, InputStream in, OutputStream out)
            throws StopRequestException;

    /**
     * Check if the transfer has been paused or canceled, stopping the request appropriately if it
     * has been.
     */
    private void checkPausedOrCanceled(State state) throws StopRequestException {
        synchronized (mInfo) {
            if (mInfo.mControl == Downloads.Impl.CONTROL_PAUSED) {
                throw new StopRequestException(
                		Downloads.Impl.STATUS_PAUSED_BY_APP, "transfer paused by owner");
            }
            if (mInfo.mStatus == Downloads.Impl.STATUS_CANCELED || mInfo.mDeleted) {
                throw new StopRequestException(Downloads.Impl.STATUS_CANCELED, "transfer canceled");
            }
        }

        // if policy has been changed, trigger connectivity check
        if (mPolicyDirty) {
            checkConnectivity();
        }
    }

    /**
     * Report transfer progress through the database if necessary.
     */
    private void reportProgress(State state) {
        final long now = SystemClock.elapsedRealtime();

        final long sampleDelta = now - state.mSpeedSampleStart;
        if (sampleDelta > 500) {
            final long sampleSpeed = ((state.mCurrentBytes - state.mSpeedSampleBytes) * 1000)
                    / sampleDelta;

            if (state.mSpeed == 0) {
                state.mSpeed = sampleSpeed;
            } else {
                state.mSpeed = ((state.mSpeed * 3) + sampleSpeed) / 4;
            }

            // Only notify once we have a full sample window
            if (state.mSpeedSampleStart != 0) {
                mNotifier.notifyTransferSpeed(mInfo.mId, state.mSpeed);
            }

            state.mSpeedSampleStart = now;
            state.mSpeedSampleBytes = state.mCurrentBytes;
        }

        if (state.mCurrentBytes - state.mBytesNotified > Constants.MIN_PROGRESS_STEP &&
            now - state.mTimeLastNotification > Constants.MIN_PROGRESS_TIME) {
            ContentValues values = new ContentValues();
            values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, state.mCurrentBytes);
            mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
            state.mBytesNotified = state.mCurrentBytes;
            state.mTimeLastNotification = now;
        }
    }

    /**
     * Called when we've reached the end of the HTTP response stream, to update the database and
     * check for consistency.
     */
    private void handleEndOfStream(State state) throws StopRequestException {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, state.mCurrentBytes);
        if (state.mContentLength == -1) {
            values.put(Downloads.Impl.COLUMN_TOTAL_BYTES, state.mCurrentBytes);
        }
        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);

        final boolean lengthMismatched = (state.mContentLength != -1)
                && (state.mCurrentBytes != state.mContentLength);
        if (lengthMismatched) {
            if (cannotResume(state)) {
                throw new StopRequestException(STATUS_CANNOT_RESUME,
                        "mismatched content length; unable to resume");
            } else {
                throw new StopRequestException(STATUS_HTTP_DATA_ERROR,
                        "closed socket before end of file");
            }
        }
    }

    private boolean cannotResume(State state) {
        return (state.mCurrentBytes > 0 && !mInfo.mNoIntegrity && state.mHeaderETag == null)
                || TransferDrmHelper.isDrmConvertNeeded(state.mMimeType);
    }

    /**
     * Read some data from the HTTP response stream, handling I/O errors.
     * @param data buffer to use to read data
     * @param entityStream stream for reading the HTTP response entity
     * @return the number of bytes actually read or -1 if the end of the stream has been reached
     */
    private int readFromResponse(State state, byte[] data, InputStream entityStream)
            throws StopRequestException {
        try {
            return entityStream.read(data);
        } catch (IOException ex) {
            // TODO: handle stream errors the same as other retries
            if ("unexpected end of stream".equals(ex.getMessage())) {
                return -1;
            }

            ContentValues values = new ContentValues();
            values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, state.mCurrentBytes);
            mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
            if (cannotResume(state)) {
                throw new StopRequestException(STATUS_CANNOT_RESUME,
                        "Failed reading response: " + ex + "; unable to resume", ex);
            } else {
                throw new StopRequestException(STATUS_HTTP_DATA_ERROR,
                        "Failed reading response: " + ex, ex);
            }
        }
    }

    /**
     * Prepare target file based on given network response. Derives filename and
     * target size as needed.
     */
    private void processResponseHeaders(State state, HttpURLConnection conn)
            throws StopRequestException {
        // TODO: fallocate the entire file if header gave us specific length

        readResponseHeaders(state, conn);

        state.mFilename = Helpers.generateSaveFile(
                mContext,
                mInfo.mUri,
                mInfo.mHint,
                state.mContentDisposition,
                state.mContentLocation,
                state.mMimeType,
                mInfo.mDestination,
                state.mContentLength,
                mStorageManager);

        updateDatabaseFromHeaders(state);
        // check connectivity again now that we know the total size
        checkConnectivity();
    }

    /**
     * Update necessary database fields based on values of HTTP response headers that have been
     * read.
     */
    private void updateDatabaseFromHeaders(State state) {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl._DATA, state.mFilename);
        if (state.mHeaderETag != null) {
            values.put(Constants.ETAG, state.mHeaderETag);
        }
        if (state.mMimeType != null) {
            values.put(Downloads.Impl.COLUMN_MIME_TYPE, state.mMimeType);
        }
        values.put(Downloads.Impl.COLUMN_TOTAL_BYTES, mInfo.mTotalBytes);
        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
    }

    /**
     * Read headers from the HTTP response and store them into local state.
     */
    private void readResponseHeaders(State state, HttpURLConnection conn)
            throws StopRequestException {
        state.mContentDisposition = conn.getHeaderField("Content-Disposition");
        state.mContentLocation = conn.getHeaderField("Content-Location");

        if (state.mMimeType == null) {
            state.mMimeType = Intent.normalizeMimeType(conn.getContentType());
        }

        state.mHeaderETag = conn.getHeaderField("ETag");

        final String transferEncoding = conn.getHeaderField("Transfer-Encoding");
        if (transferEncoding == null) {
            state.mContentLength = getHeaderFieldLong(conn, "Content-Length", -1);
        } else {
            Log.i(TAG, "Ignoring Content-Length since Transfer-Encoding is also defined");
            state.mContentLength = -1;
        }

        state.mTotalBytes = state.mContentLength;
        mInfo.mTotalBytes = state.mContentLength;

        final boolean noSizeInfo = state.mContentLength == -1
                && (transferEncoding == null || !transferEncoding.equalsIgnoreCase("chunked"));
        if (!mInfo.mNoIntegrity && noSizeInfo) {
            throw new StopRequestException(STATUS_CANNOT_RESUME,
                    "can't know size of transfer, giving up");
        }
    }

    private void parseRetryAfterHeaders(State state, HttpURLConnection conn) {
        state.mRetryAfter = conn.getHeaderFieldInt("Retry-After", -1);
        if (state.mRetryAfter < 0) {
            state.mRetryAfter = 0;
        } else {
            if (state.mRetryAfter < Constants.MIN_RETRY_AFTER) {
                state.mRetryAfter = Constants.MIN_RETRY_AFTER;
            } else if (state.mRetryAfter > Constants.MAX_RETRY_AFTER) {
                state.mRetryAfter = Constants.MAX_RETRY_AFTER;
            }
            state.mRetryAfter += Helpers.sRandom.nextInt(Constants.MIN_RETRY_AFTER + 1);
            state.mRetryAfter *= 1000;
        }
    }

    

    /**
     * Add custom headers for this transfer to the HTTP request.
     */
    private void addRequestHeaders(State state, HttpURLConnection conn) {
        for (Pair<String, String> header : mInfo.getHeaders()) {
            conn.addRequestProperty(header.first, header.second);
        }

        // Only splice in user agent when not already defined
        if (conn.getRequestProperty("User-Agent") == null) {
            conn.addRequestProperty("User-Agent", userAgent());
        }

        // Defeat transparent gzip compression, since it doesn't allow us to
        // easily resume partial transfers.
        conn.setRequestProperty("Accept-Encoding", "identity");

        if (state.mContinuingTransfer) {
            if (state.mHeaderETag != null) {
                conn.addRequestProperty("If-Match", state.mHeaderETag);
            }
            conn.addRequestProperty("Range", "bytes=" + state.mCurrentBytes + "-");
        }
    }

    /**
     * Stores information about the completed transfer, and notifies the initiating application.
     */
    private void notifyTransferCompleted(
            State state, int finalStatus, String errorMsg, int numFailed) {
        notifyThroughDatabase(state, finalStatus, errorMsg, numFailed);
        if (Downloads.Impl.isStatusCompleted(finalStatus)) {
            mInfo.sendIntentIfRequested();
        }
    }

    private void notifyThroughDatabase(
            State state, int finalStatus, String errorMsg, int numFailed) {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_STATUS, finalStatus);
        values.put(Downloads.Impl._DATA, state.mFilename);
        values.put(Downloads.Impl.COLUMN_MIME_TYPE, state.mMimeType);
        values.put(Downloads.Impl.COLUMN_LAST_MODIFICATION, mSystemFacade.currentTimeMillis());
        values.put(Downloads.Impl.COLUMN_FAILED_CONNECTIONS, numFailed);
        values.put(Constants.RETRY_AFTER_X_REDIRECT_COUNT, state.mRetryAfter);

        if (!TextUtils.equals(mInfo.mUri, state.mRequestUri)) {
            values.put(Downloads.Impl.COLUMN_URI, state.mRequestUri);
        }

        // save the error message. could be useful to developers.
        if (!TextUtils.isEmpty(errorMsg)) {
            values.put(Downloads.Impl.COLUMN_ERROR_MSG, errorMsg);
        }
        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
    }

    private INetworkPolicyListener mPolicyListener = new INetworkPolicyListener.Stub() {
        @Override
        public void onUidRulesChanged(int uid, int uidRules) {
            // caller is NPMS, since we only register with them
            if (uid == mInfo.mUid) {
                mPolicyDirty = true;
            }
        }

        @Override
        public void onMeteredIfacesChanged(String[] meteredIfaces) {
            // caller is NPMS, since we only register with them
            mPolicyDirty = true;
        }

        @Override
        public void onRestrictBackgroundChanged(boolean restrictBackground) {
            // caller is NPMS, since we only register with them
            mPolicyDirty = true;
        }
    };

    public static long getHeaderFieldLong(URLConnection conn, String field, long defaultValue) {
        try {
            return Long.parseLong(conn.getHeaderField(field));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Return if given status is eligible to be treated as
     * {@link com.android.providers.transfers.Transfers.Impl#STATUS_WAITING_TO_RETRY}.
     */
    public static boolean isStatusRetryable(int status) {
        switch (status) {
            case STATUS_HTTP_DATA_ERROR:
            case HTTP_UNAVAILABLE:
            case HTTP_INTERNAL_ERROR:
                return true;
            default:
                return false;
        }
    }
}

