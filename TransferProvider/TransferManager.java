package android.app;

/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.ConnectivityManager;
import android.net.NetworkPolicyManager;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.Transfers;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * The transfer manager is a system service that handles long-running HTTP transfers. Clients may
 * request that a URI be transfered to a particular destination file. The transfer manager will
 * conduct the transfer in the background, taking care of HTTP interactions and retrying transfers
 * after failures or across connectivity changes and system reboots.
 *
 * Instances of this class should be obtained through
 * {@link android.content.Context#getSystemService(String)} by passing
 * {@link android.content.Context#TRANSFER_SERVICE}.
 *
 * Apps that request transfers through this API should register a broadcast receiver for
 * {@link #ACTION_NOTIFICATION_CLICKED} to appropriately handle when the user clicks on a running
 * transfer in a notification or from the transfers UI.
 *
 * Note that the application must have the {@link android.Manifest.permission#INTERNET}
 * permission to use this class.
 */
public class TransferManager {

    /**
     * An identifier for a particular transfer, unique across the system.  Clients use this ID to
     * make subsequent calls related to the transfer.
     */
    public final static String COLUMN_ID = Transfers.Impl._ID;

    /**
     * The client-supplied title for this transfer.  This will be displayed in system notifications.
     * Defaults to the empty string.
     */
    public final static String COLUMN_TITLE = Transfers.Impl.COLUMN_TITLE;

    /**
     * The client-supplied description of this transfer.  This will be displayed in system
     * notifications.  Defaults to the empty string.
     */
    public final static String COLUMN_DESCRIPTION = Transfers.Impl.COLUMN_DESCRIPTION;

    /**
     * URI to be transfered.
     */
    public final static String COLUMN_URI = Transfers.Impl.COLUMN_URI;

    /**
     * Internet Media Type of the transfered file.  If no value is provided upon creation, this will
     * initially be null and will be filled in based on the server's response once the transfer has
     * started.
     *
     * @see <a href="http://www.ietf.org/rfc/rfc1590.txt">RFC 1590, defining Media Types</a>
     */
    public final static String COLUMN_MEDIA_TYPE = "media_type";

    /**
     * Total size of the transfer in bytes.  This will initially be -1 and will be filled in once
     * the transfer starts.
     */
    public final static String COLUMN_TOTAL_SIZE_BYTES = "total_size";

    /**
     * Uri where transfered file will be stored.  If a destination is supplied by client, that URI
     * will be used here.  Otherwise, the value will initially be null and will be filled in with a
     * generated URI once the transfer has started.
     */
    public final static String COLUMN_LOCAL_URI = "local_uri";

    /**
     * The pathname of the file where the transfer is stored.
     */
    public final static String COLUMN_LOCAL_FILENAME = "local_filename";

    /**
     * Current status of the transfer, as one of the STATUS_* constants.
     */
    public final static String COLUMN_STATUS = Transfers.Impl.COLUMN_STATUS;

    /**
     * Provides more detail on the status of the transfer.  Its meaning depends on the value of
     * {@link #COLUMN_STATUS}.
     *
     * When {@link #COLUMN_STATUS} is {@link #STATUS_FAILED}, this indicates the type of error that
     * occurred.  If an HTTP error occurred, this will hold the HTTP status code as defined in RFC
     * 2616.  Otherwise, it will hold one of the ERROR_* constants.
     *
     * When {@link #COLUMN_STATUS} is {@link #STATUS_PAUSED}, this indicates why the transfer is
     * paused.  It will hold one of the PAUSED_* constants.
     *
     * If {@link #COLUMN_STATUS} is neither {@link #STATUS_FAILED} nor {@link #STATUS_PAUSED}, this
     * column's value is undefined.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1.1">RFC 2616
     * status codes</a>
     */
    public final static String COLUMN_REASON = "reason";

    /**
     * Number of bytes transfer so far.
     */
    public final static String COLUMN_BYTES_TRANSFERED_SO_FAR = "bytes_so_far";

    /**
     * Timestamp when the transfer was last modified, in {@link System#currentTimeMillis
     * System.currentTimeMillis()} (wall clock time in UTC).
     */
    public final static String COLUMN_LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";

    /**
     * The URI to the corresponding entry in MediaProvider for this transfered entry. It is
     * used to delete the entries from MediaProvider database when it is deleted from the
     * transfered list.
     */
    public static final String COLUMN_MEDIAPROVIDER_URI = Transfers.Impl.COLUMN_MEDIAPROVIDER_URI;

    /**
     * @hide
     */
    public final static String COLUMN_ALLOW_WRITE = Transfers.Impl.COLUMN_ALLOW_WRITE;


    /**
     * The type of transfer (transfer or upload)
     */
    public static final String COLUMN_IS_TYPE_UPLOAD = Transfers.Impl.COLUMN_IS_TYPE_UPLOAD;//ml

    /**
     * If the transfer is using Tor
     */
    public static final String COLUMN_IS_USING_TOR = Transfers.Impl.COLUMN_IS_USING_TOR;

    /**
     * Value of {@link #COLUMN_STATUS} when the transfer is waiting to start.
     */
    public final static int STATUS_PENDING = 1 << 0;

    /**
     * Value of {@link #COLUMN_STATUS} when the transfer is currently running.
     */
    public final static int STATUS_RUNNING = 1 << 1;

    /**
     * Value of {@link #COLUMN_STATUS} when the transfer is waiting to retry or resume.
     */
    public final static int STATUS_PAUSED = 1 << 2;

    /**
     * Value of {@link #COLUMN_STATUS} when the transfer has successfully completed.
     */
    public final static int STATUS_SUCCESSFUL = 1 << 3;

    /**
     * Value of {@link #COLUMN_STATUS} when the transfer has failed (and will not be retried).
     */
    public final static int STATUS_FAILED = 1 << 4;

    /**
     * Value of COLUMN_ERROR_CODE when the transfer has completed with an error that doesn't fit
     * under any other error code.
     */
    public final static int ERROR_UNKNOWN = 1000;

    /**
     * Value of {@link #COLUMN_REASON} when a storage issue arises which doesn't fit under any
     * other error code. Use the more specific {@link #ERROR_INSUFFICIENT_SPACE} and
     * {@link #ERROR_DEVICE_NOT_FOUND} when appropriate.
     */
    public final static int ERROR_FILE_ERROR = 1001;

    /**
     * Value of {@link #COLUMN_REASON} when an HTTP code was received that transfer manager
     * can't handle.
     */
    public final static int ERROR_UNHANDLED_HTTP_CODE = 1002;

    /**
     * Value of {@link #COLUMN_REASON} when an error receiving or processing data occurred at
     * the HTTP level.
     */
    public final static int ERROR_HTTP_DATA_ERROR = 1004;

    /**
     * Value of {@link #COLUMN_REASON} when there were too many redirects.
     */
    public final static int ERROR_TOO_MANY_REDIRECTS = 1005;

    /**
     * Value of {@link #COLUMN_REASON} when there was insufficient storage space. Typically,
     * this is because the SD card is full.
     */
    public final static int ERROR_INSUFFICIENT_SPACE = 1006;

    /**
     * Value of {@link #COLUMN_REASON} when no external storage device was found. Typically,
     * this is because the SD card is not mounted.
     */
    public final static int ERROR_DEVICE_NOT_FOUND = 1007;

    /**
     * Value of {@link #COLUMN_REASON} when some possibly transient error occurred but we can't
     * resume the transfer.
     */
    public final static int ERROR_CANNOT_RESUME = 1008;

    /**
     * Value of {@link #COLUMN_REASON} when the requested destination file already exists (the
     * transfer manager will not overwrite an existing file).
     */
    public final static int ERROR_FILE_ALREADY_EXISTS = 1009;

    /**
     * Value of {@link #COLUMN_REASON} when the transfer has failed because of
     * {@link NetworkPolicyManager} controls on the requesting application.
     *
     * @hide
     */
    public final static int ERROR_BLOCKED = 1010;

    /**
     * Value of {@link #COLUMN_REASON} when the transfer is paused because some network error
     * occurred and the transfer manager is waiting before retrying the request.
     */
    public final static int PAUSED_WAITING_TO_RETRY = 1;

    /**
     * Value of {@link #COLUMN_REASON} when the transfer is waiting for network connectivity to
     * proceed.
     */
    public final static int PAUSED_WAITING_FOR_NETWORK = 2;

    /**
     * Value of {@link #COLUMN_REASON} when the transfer exceeds a size limit for transfers over
     * the mobile network and the transfer manager is waiting for a Wi-Fi connection to proceed.
     */
    public final static int PAUSED_QUEUED_FOR_WIFI = 3;

    /**
     * Value of {@link #COLUMN_REASON} when the transfer is paused for some other reason.
     */
    public final static int PAUSED_UNKNOWN = 4;

    /**
     * Broadcast intent action sent by the transfer manager when a transfer completes.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public final static String ACTION_TRANSFER_COMPLETE = "android.intent.action.TRANSFER_COMPLETE";

    /**
     * Broadcast intent action sent by the transfer manager when the user clicks on a running
     * transfer, either from a system notification or from the transfers UI.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public final static String ACTION_NOTIFICATION_CLICKED =
            "android.intent.action.TRANSFER_NOTIFICATION_CLICKED";

    /**
     * Intent action to launch an activity to display all transfers.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public final static String ACTION_VIEW_TRANSFERS = "android.intent.action.VIEW_TRANSFERS";

    /**
     * Intent extra included with {@link #ACTION_VIEW_TRANSFERS} to start TransferApp in
     * sort-by-size mode.
     */
    public final static String INTENT_EXTRAS_SORT_BY_SIZE =
            "android.app.TransferManager.extra_sortBySize";

    /**
     * Intent extra included with {@link #ACTION_TRANSFER_COMPLETE} intents, indicating the ID (as a
     * long) of the transfer that just completed.
     */
    public static final String EXTRA_TRANSFER_ID = "extra_transfer_id";

    /**
     * When clicks on multiple notifications are received, the following
     * provides an array of transfer ids corresponding to the transfer notification that was
     * clicked. It can be retrieved by the receiver of this
     * Intent using {@link android.content.Intent#getLongArrayExtra(String)}.
     */
    public static final String EXTRA_NOTIFICATION_CLICK_TRANSFER_IDS = "extra_click_transfer_ids";

    /**
     * columns to request from TransferProvider.
     * @hide
     */
    public static final String[] UNDERLYING_COLUMNS = new String[] {
            Transfers.Impl._ID,
            Transfers.Impl._DATA + " AS " + COLUMN_LOCAL_FILENAME,
            Transfers.Impl.COLUMN_MEDIAPROVIDER_URI,
            Transfers.Impl.COLUMN_DESTINATION,
            Transfers.Impl.COLUMN_TITLE,
            Transfers.Impl.COLUMN_DESCRIPTION,
            Transfers.Impl.COLUMN_URI,
            Transfers.Impl.COLUMN_STATUS,
            Transfers.Impl.COLUMN_FILE_NAME_HINT,
            Transfers.Impl.COLUMN_MIME_TYPE + " AS " + COLUMN_MEDIA_TYPE,
            Transfers.Impl.COLUMN_TOTAL_BYTES + " AS " + COLUMN_TOTAL_SIZE_BYTES,
            Transfers.Impl.COLUMN_LAST_MODIFICATION + " AS " + COLUMN_LAST_MODIFIED_TIMESTAMP,
            Transfers.Impl.COLUMN_CURRENT_BYTES + " AS " + COLUMN_BYTES_TRANSFERED_SO_FAR,
            Transfers.Impl.COLUMN_ALLOW_WRITE,
            Transfers.Impl.COLUMN_IS_TYPE_UPLOAD, //ml
            Transfers.Impl.COLUMN_IS_USING_TOR,
        /* add the following 'computed' columns to the cursor.
         * they are not 'returned' by the database, but their inclusion
         * eliminates need to have lot of methods in CursorTranslator
         */
            "'placeholder' AS " + COLUMN_LOCAL_URI,
            "'placeholder' AS " + COLUMN_REASON
    };

    /**
     * This class contains all the information necessary to request a new transfer. The URI is the
     * only required parameter.
     *
     * Note that the default transfer destination is a shared volume where the system might delete
     * your file if it needs to reclaim space for system use. If this is a problem, use a location
     * on external storage (see {@link #setDestinationUri(Uri)}.
     */
    public static class Request {
        /**
         * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
         * {@link ConnectivityManager#TYPE_MOBILE}.
         */
        public static final int NETWORK_MOBILE = 1 << 0;

        /**
         * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
         * {@link ConnectivityManager#TYPE_WIFI}.
         */
        public static final int NETWORK_WIFI = 1 << 1;

        /**
         * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
         * {@link ConnectivityManager#TYPE_BLUETOOTH}.
         * @hide
         */
        public static final int NETWORK_BLUETOOTH = 1 << 2;

        private Uri mUri;
        private Uri mDestinationUri;
        private List<Pair<String, String>> mRequestHeaders = new ArrayList<Pair<String, String>>();
        private CharSequence mTitle;
        private CharSequence mDescription;
        private String mMimeType;
        private int mAllowedNetworkTypes = ~0; // default to all network types allowed
        private boolean mRoamingAllowed = true;
        private boolean mMeteredAllowed = true;
        private boolean mIsVisibleInTransfersUi = true;
        private boolean mScannable = false;
        private boolean mUseSystemCache = false;
        private boolean mIsTypeUpload = false;//ml
        private boolean mIsUsingTor = false;

        /** if a file is designated as a MediaScanner scannable file, the following value is
         * stored in the database column {@link Transfers.Impl#COLUMN_MEDIA_SCANNED}.
         */
        private static final int SCANNABLE_VALUE_YES = 0;
        // value of 1 is stored in the above column by TransferProvider after it is scanned by
        // MediaScanner
        /** if a file is designated as a file that should not be scanned by MediaScanner,
         * the following value is stored in the database column
         * {@link Transfers.Impl#COLUMN_MEDIA_SCANNED}.
         */
        private static final int SCANNABLE_VALUE_NO = 2;

        /**
         * This transfer is visible but only shows in the notifications
         * while it's in progress.
         */
        public static final int VISIBILITY_VISIBLE = 0;

        /**
         * This transfer is visible and shows in the notifications while
         * in progress and after completion.
         */
        public static final int VISIBILITY_VISIBLE_NOTIFY_COMPLETED = 1;

        /**
         * This transfer doesn't show in the UI or in the notifications.
         */
        public static final int VISIBILITY_HIDDEN = 2;

        /**
         * This transfer shows in the notifications after completion ONLY.
         * It is usuable only with
         * {@link TransferManager#addCompletedTransfer(String, String,
         * boolean, String, String, long, boolean)}.
         */
        public static final int VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION = 3;

        /** can take any of the following values: {@link #VISIBILITY_HIDDEN}
         * {@link #VISIBILITY_VISIBLE_NOTIFY_COMPLETED}, {@link #VISIBILITY_VISIBLE},
         * {@link #VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION}
         */
        private int mNotificationVisibility = VISIBILITY_VISIBLE;

        /**
         * @param uri the HTTP URI to transfer.
         */
        public Request(Uri uri) {
            if (uri == null) {
                throw new NullPointerException();
            }
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                throw new IllegalArgumentException("Can only transfer HTTP/HTTPS URIs: " + uri);
            }
            mUri = uri;
        }

        Request(String uriString) {
            mUri = Uri.parse(uriString);
        }

        /**
         * Set the local destination for the transfered file. Must be a file URI to a path on
         * external storage, and the calling application must have the WRITE_EXTERNAL_STORAGE
         * permission.
         * <p>
         * The transfered file is not scanned by MediaScanner.
         * But it can be made scannable by calling {@link #allowScanningByMediaScanner()}.
         * <p>
         * By default, transfers are saved to a generated filename in the shared transfer cache and
         * may be deleted by the system at any time to reclaim space.
         *
         * @return this object
         */
        public Request setDestinationUri(Uri uri) {
            mDestinationUri = uri;
            return this;
        }

        /**
         * Set the local destination for the transfered file to the system cache dir (/cache).
         * This is only available to System apps with the permission
         * {@link android.Manifest.permission#ACCESS_CACHE_FILESYSTEM}.
         * <p>
         * The transfered file is not scanned by MediaScanner.
         * But it can be made scannable by calling {@link #allowScanningByMediaScanner()}.
         * <p>
         * Files transfered to /cache may be deleted by the system at any time to reclaim space.
         *
         * @return this object
         * @hide
         */
        public Request setDestinationToSystemCache() {
            mUseSystemCache = true;
            return this;
        }

        /**
         * Set the local destination for the transfered file to a path within
         * the application's external files directory (as returned by
         * {@link Context#getExternalFilesDir(String)}.
         * <p>
         * The transfered file is not scanned by MediaScanner. But it can be
         * made scannable by calling {@link #allowScanningByMediaScanner()}.
         *
         * @param context the {@link Context} to use in determining the external
         *            files directory
         * @param dirType the directory type to pass to
         *            {@link Context#getExternalFilesDir(String)}
         * @param subPath the path within the external directory, including the
         *            destination filename
         * @return this object
         * @throws IllegalStateException If the external storage directory
         *             cannot be found or created.
         */
        public Request setDestinationInExternalFilesDir(Context context, String dirType,
                                                        String subPath) {
            final File file = context.getExternalFilesDir(dirType);
            if (file == null) {
                throw new IllegalStateException("Failed to get external storage files directory");
            } else if (file.exists()) {
                if (!file.isDirectory()) {
                    throw new IllegalStateException(file.getAbsolutePath() +
                            " already exists and is not a directory");
                }
            } else {
                if (!file.mkdirs()) {
                    throw new IllegalStateException("Unable to create directory: "+
                            file.getAbsolutePath());
                }
            }
            setDestinationFromBase(file, subPath);
            return this;
        }

        /**
         * Set the local destination for the transfered file to a path within
         * the public external storage directory (as returned by
         * {@link Environment#getExternalStoragePublicDirectory(String)}).
         * <p>
         * The transfered file is not scanned by MediaScanner. But it can be
         * made scannable by calling {@link #allowScanningByMediaScanner()}.
         *
         * @param dirType the directory type to pass to {@link Environment#getExternalStoragePublicDirectory(String)}
         * @param subPath the path within the external directory, including the
         *            destination filename
         * @return this object
         * @throws IllegalStateException If the external storage directory
         *             cannot be found or created.
         */
        public Request setDestinationInExternalPublicDir(String dirType, String subPath) {
            File file = Environment.getExternalStoragePublicDirectory(dirType);
            if (file == null) {
                throw new IllegalStateException("Failed to get external storage public directory");
            } else if (file.exists()) {
                if (!file.isDirectory()) {
                    throw new IllegalStateException(file.getAbsolutePath() +
                            " already exists and is not a directory");
                }
            } else {
                if (!file.mkdirs()) {
                    throw new IllegalStateException("Unable to create directory: "+
                            file.getAbsolutePath());
                }
            }
            setDestinationFromBase(file, subPath);
            return this;
        }

        private void setDestinationFromBase(File base, String subPath) {
            if (subPath == null) {
                throw new NullPointerException("subPath cannot be null");
            }
            mDestinationUri = Uri.withAppendedPath(Uri.fromFile(base), subPath);
        }

        /**
         * If the file to be transfered is to be scanned by MediaScanner, this method
         * should be called before {@link TransferManager#enqueue(Request)} is called.
         */
        public void allowScanningByMediaScanner() {
            mScannable = true;
        }

        /**
         * Add an HTTP header to be included with the transfer request.  The header will be added to
         * the end of the list.
         * @param header HTTP header name
         * @param value header value
         * @return this object
         * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">HTTP/1.1
         *      Message Headers</a>
         */
        public Request addRequestHeader(String header, String value) {
            if (header == null) {
                throw new NullPointerException("header cannot be null");
            }
            if (header.contains(":")) {
                throw new IllegalArgumentException("header may not contain ':'");
            }
            if (value == null) {
                value = "";
            }
            mRequestHeaders.add(Pair.create(header, value));
            return this;
        }

        /**
         * Set the title of this transfer, to be displayed in notifications (if enabled).  If no
         * title is given, a default one will be assigned based on the transfer filename, once the
         * transfer starts.
         * @return this object
         */
        public Request setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Set a description of this transfer, to be displayed in notifications (if enabled)
         * @return this object
         */
        public Request setDescription(CharSequence description) {
            mDescription = description;
            return this;
        }

        /**
         * Set the MIME content type of this transfer.  This will override the content type declared
         * in the server's response.
         * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7">HTTP/1.1
         *      Media Types</a>
         * @return this object
         */
        public Request setMimeType(String mimeType) {
            mMimeType = mimeType;
            return this;
        }

        /**
         * Control whether a system notification is posted by the transfer manager while this
         * transfer is running. If enabled, the transfer manager posts notifications about transfers
         * through the system {@link android.app.NotificationManager}. By default, a notification is
         * shown.
         *
         * If set to false, this requires the permission
         * android.permission.TRANSFER_WITHOUT_NOTIFICATION.
         *
         * @param show whether the transfer manager should show a notification for this transfer.
         * @return this object
         * @deprecated use {@link #setNotificationVisibility(int)}
         */
        @Deprecated
        public Request setShowRunningNotification(boolean show) {
            return (show) ? setNotificationVisibility(VISIBILITY_VISIBLE) :
                    setNotificationVisibility(VISIBILITY_HIDDEN);
        }

        /**
         * Control whether a system notification is posted by the transfer manager while this
         * transfer is running or when it is completed.
         * If enabled, the transfer manager posts notifications about transfers
         * through the system {@link android.app.NotificationManager}.
         * By default, a notification is shown only when the transfer is in progress.
         *<p>
         * It can take the following values: {@link #VISIBILITY_HIDDEN},
         * {@link #VISIBILITY_VISIBLE},
         * {@link #VISIBILITY_VISIBLE_NOTIFY_COMPLETED}.
         *<p>
         * If set to {@link #VISIBILITY_HIDDEN}, this requires the permission
         * android.permission.TRANSFER_WITHOUT_NOTIFICATION.
         *
         * @param visibility the visibility setting value
         * @return this object
         */
        public Request setNotificationVisibility(int visibility) {
            mNotificationVisibility = visibility;
            return this;
        }

        /**
         * Restrict the types of networks over which this transfer may proceed.
         * By default, all network types are allowed. Consider using
         * {@link #setAllowedOverMetered(boolean)} instead, since it's more
         * flexible.
         *
         * @param flags any combination of the NETWORK_* bit flags.
         * @return this object
         */
        public Request setAllowedNetworkTypes(int flags) {
            mAllowedNetworkTypes = flags;
            return this;
        }

        /**
         * Set whether this transfer may proceed over a roaming connection.  By default, roaming is
         * allowed.
         * @param allowed whether to allow a roaming connection to be used
         * @return this object
         */
        public Request setAllowedOverRoaming(boolean allowed) {
            mRoamingAllowed = allowed;
            return this;
        }

        /**
         * Set whether this transfer may proceed over a metered network
         * connection. By default, metered networks are allowed.
         *
         * @see ConnectivityManager#isActiveNetworkMetered()
         */
        public Request setAllowedOverMetered(boolean allow) {
            mMeteredAllowed = allow;
            return this;
        }

        /**
         * Set whether this transfer should be displayed in the system's Transfers UI. True by
         * default.
         * @param isVisible whether to display this transfer in the Transfers UI
         * @return this object
         */
        public Request setVisibleInTransfersUi(boolean isVisible) {
            mIsVisibleInTransfersUi = isVisible;
            return this;
        }

        /**
         * Set whether this transfer is an upload (or transfer). False(transfer) by default.
         * @param isTypeUpload whether this transfer is an upload.
         * @return this object
         */
        public Request setIsTypeUpload(boolean isTypeUpload) {
            mIsTypeUpload = isTypeUpload;
            return this;
        }//ml

        /**
         * Set whether this transfer is using Tor for encryption. False by default.
         * @param isUsingTor whether this transfer is using Tor.
         * @return this object
         */
        public Request setIsUsingTor(boolean isUsingTor) {
            mIsUsingTor = isUsingTor;
            return this;
        }

        /**
         * @return ContentValues to be passed to TransferProvider.insert()
         */
        ContentValues toContentValues(String packageName) {
            ContentValues values = new ContentValues();
            assert mUri != null;
            values.put(Transfers.Impl.COLUMN_URI, mUri.toString());
            values.put(Transfers.Impl.COLUMN_IS_PUBLIC_API, true);
            values.put(Transfers.Impl.COLUMN_NOTIFICATION_PACKAGE, packageName);

            if (mDestinationUri != null) {
                values.put(Transfers.Impl.COLUMN_DESTINATION, Transfers.Impl.DESTINATION_FILE_URI);
                values.put(Transfers.Impl.COLUMN_FILE_NAME_HINT, mDestinationUri.toString());
            } else {
                values.put(Transfers.Impl.COLUMN_DESTINATION,
                        (this.mUseSystemCache) ?
                                Transfers.Impl.DESTINATION_SYSTEMCACHE_PARTITION :
                                Transfers.Impl.DESTINATION_CACHE_PARTITION_PURGEABLE);
            }
            // is the file supposed to be media-scannable?
            values.put(Transfers.Impl.COLUMN_MEDIA_SCANNED, (mScannable) ? SCANNABLE_VALUE_YES :
                    SCANNABLE_VALUE_NO);

            if (!mRequestHeaders.isEmpty()) {
                encodeHttpHeaders(values);
            }

            putIfNonNull(values, Transfers.Impl.COLUMN_TITLE, mTitle);
            putIfNonNull(values, Transfers.Impl.COLUMN_DESCRIPTION, mDescription);
            putIfNonNull(values, Transfers.Impl.COLUMN_MIME_TYPE, mMimeType);

            values.put(Transfers.Impl.COLUMN_VISIBILITY, mNotificationVisibility);
            values.put(Transfers.Impl.COLUMN_ALLOWED_NETWORK_TYPES, mAllowedNetworkTypes);
            values.put(Transfers.Impl.COLUMN_ALLOW_ROAMING, mRoamingAllowed);
            values.put(Transfers.Impl.COLUMN_ALLOW_METERED, mMeteredAllowed);
            values.put(Transfers.Impl.COLUMN_IS_VISIBLE_IN_TRANSFERS_UI, mIsVisibleInTransfersUi);
            values.put(Transfers.Impl.COLUMN_IS_TYPE_UPLOAD, mIsTypeUpload);//ml
            values.put(Transfers.Impl.COLUMN_IS_USING_TOR, mIsUsingTor);

            return values;
        }

        private void encodeHttpHeaders(ContentValues values) {
            int index = 0;
            for (Pair<String, String> header : mRequestHeaders) {
                String headerString = header.first + ": " + header.second;
                values.put(Transfers.Impl.RequestHeaders.INSERT_KEY_PREFIX + index, headerString);
                index++;
            }
        }

        private void putIfNonNull(ContentValues contentValues, String key, Object value) {
            if (value != null) {
                contentValues.put(key, value.toString());
            }
        }
    }

    /**
     * This class may be used to filter transfer manager queries.
     */
    public static class Query {
        /**
         * Constant for use with {@link #orderBy}
         * @hide
         */
        public static final int ORDER_ASCENDING = 1;

        /**
         * Constant for use with {@link #orderBy}
         * @hide
         */
        public static final int ORDER_DESCENDING = 2;

        private long[] mIds = null;
        private Integer mStatusFlags = null;
        private String mOrderByColumn = Transfers.Impl.COLUMN_LAST_MODIFICATION;
        private int mOrderDirection = ORDER_DESCENDING;
        private boolean mOnlyIncludeVisibleInTransfersUi = false;
        private boolean mOnlyIncludeUploads = false;//ml
        private boolean mOnlyIncludeTransfers = false;

        /**
         * Include only the transfers with the given IDs.
         * @return this object
         */
        public Query setFilterById(long... ids) {
            mIds = ids;
            return this;
        }

        /**
         * Include only transfers with status matching any the given status flags.
         * @param flags any combination of the STATUS_* bit flags
         * @return this object
         */
        public Query setFilterByStatus(int flags) {
            mStatusFlags = flags;
            return this;
        }

        /**
         * Include only transfers that are Uploads
         * @param flag either true or false
         * @return this object
         */
        public Query setFilterByUploads(boolean flag) {
            mOnlyIncludeUploads = flag;//ml
            return this;
        }

        /**
         * Include only transfers that are Transfers
         * @param flag either true or false
         * @return this object
         */
        public Query setFilterByTransfers(boolean flag) {
            mOnlyIncludeTransfers = flag;//ml
            return this;
        }

        /**
         * Controls whether this query includes transfers not visible in the system's Transfers UI.
         * @param value if true, this query will only include transfers that should be displayed in
         *            the system's Transfers UI; if false (the default), this query will include
         *            both visible and invisible transfers.
         * @return this object
         * @hide
         */
        public Query setOnlyIncludeVisibleInTransfersUi(boolean value) {
            mOnlyIncludeVisibleInTransfersUi = value;
            return this;
        }

        /**
         * Change the sort order of the returned Cursor.
         *
         * @param column one of the COLUMN_* constants; currently, only
         *         {@link #COLUMN_LAST_MODIFIED_TIMESTAMP} and {@link #COLUMN_TOTAL_SIZE_BYTES} are
         *         supported.
         * @param direction either {@link #ORDER_ASCENDING} or {@link #ORDER_DESCENDING}
         * @return this object
         * @hide
         */
        public Query orderBy(String column, int direction) {
            if (direction != ORDER_ASCENDING && direction != ORDER_DESCENDING) {
                throw new IllegalArgumentException("Invalid direction: " + direction);
            }

            if (column.equals(COLUMN_LAST_MODIFIED_TIMESTAMP)) {
                mOrderByColumn = Transfers.Impl.COLUMN_LAST_MODIFICATION;
            } else if (column.equals(COLUMN_TOTAL_SIZE_BYTES)) {
                mOrderByColumn = Transfers.Impl.COLUMN_TOTAL_BYTES;
            } else {
                throw new IllegalArgumentException("Cannot order by " + column);
            }
            mOrderDirection = direction;
            return this;
        }

        /**
         * Run this query using the given ContentResolver.
         * @param projection the projection to pass to ContentResolver.query()
         * @return the Cursor returned by ContentResolver.query()
         */
        Cursor runQuery(ContentResolver resolver, String[] projection, Uri baseUri) {
            Uri uri = baseUri;
            List<String> selectionParts = new ArrayList<String>();
            String[] selectionArgs = null;

            if (mIds != null) {
                selectionParts.add(getWhereClauseForIds(mIds));
                selectionArgs = getWhereArgsForIds(mIds);
            }

            if (mStatusFlags != null) {
                List<String> parts = new ArrayList<String>();
                if ((mStatusFlags & STATUS_PENDING) != 0) {
                    parts.add(statusClause("=", Transfers.Impl.STATUS_PENDING));
                }
                if ((mStatusFlags & STATUS_RUNNING) != 0) {
                    parts.add(statusClause("=", Transfers.Impl.STATUS_RUNNING));
                }
                if ((mStatusFlags & STATUS_PAUSED) != 0) {
                    parts.add(statusClause("=", Transfers.Impl.STATUS_PAUSED_BY_APP));
                    parts.add(statusClause("=", Transfers.Impl.STATUS_WAITING_TO_RETRY));
                    parts.add(statusClause("=", Transfers.Impl.STATUS_WAITING_FOR_NETWORK));
                    parts.add(statusClause("=", Transfers.Impl.STATUS_QUEUED_FOR_WIFI));
                }
                if ((mStatusFlags & STATUS_SUCCESSFUL) != 0) {
                    parts.add(statusClause("=", Transfers.Impl.STATUS_SUCCESS));
                }
                if ((mStatusFlags & STATUS_FAILED) != 0) {
                    parts.add("(" + statusClause(">=", 400)
                            + " AND " + statusClause("<", 600) + ")");
                }
                selectionParts.add(joinStrings(" OR ", parts));
            }

            if (mOnlyIncludeVisibleInTransfersUi) {
                selectionParts.add(Transfers.Impl.COLUMN_IS_VISIBLE_IN_TRANSFERS_UI + " != '0'");
            }

            if (mOnlyIncludeUploads) {
                selectionParts.add(Transfers.Impl.COLUMN_IS_TYPE_UPLOAD + " != '0'");
            }//ml

            if (mOnlyIncludeTransfers) {
                selectionParts.add(Transfers.Impl.COLUMN_IS_TYPE_UPLOAD + " != '1'");
            }

            // only return rows which are not marked 'deleted = 1'
            selectionParts.add(Transfers.Impl.COLUMN_DELETED + " != '1'");

            String selection = joinStrings(" AND ", selectionParts);
            String orderDirection = (mOrderDirection == ORDER_ASCENDING ? "ASC" : "DESC");
            String orderBy = mOrderByColumn + " " + orderDirection;

            return resolver.query(uri, projection, selection, selectionArgs, orderBy);
        }

        private String joinStrings(String joiner, Iterable<String> parts) {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String part : parts) {
                if (!first) {
                    builder.append(joiner);
                }
                builder.append(part);
                first = false;
            }
            return builder.toString();
        }

        private String statusClause(String operator, int value) {
            return Transfers.Impl.COLUMN_STATUS + operator + "'" + value + "'";
        }
    }

    private ContentResolver mResolver;
    private String mPackageName;
    private Uri mBaseUri = Transfers.Impl.CONTENT_URI;

    /**
     * @hide
     */
    public TransferManager(ContentResolver resolver, String packageName) {
        mResolver = resolver;
        mPackageName = packageName;
    }

    /**
     * Makes this object access the transfer provider through /all_transfers URIs rather than
     * /my_transfers URIs, for clients that have permission to do so.
     * @hide
     */
    public void setAccessAllTransfers(boolean accessAllTransfers) {
        if (accessAllTransfers) {
            mBaseUri = Transfers.Impl.ALL_TRANSFERS_CONTENT_URI;
        } else {
            mBaseUri = Transfers.Impl.CONTENT_URI;
        }
    }

    /**
     * Enqueue a new transfer.  The transfer will start automatically once the transfer manager is
     * ready to execute it and connectivity is available.
     *
     * @param request the parameters specifying this transfer
     * @return an ID for the transfer, unique across the system.  This ID is used to make future
     * calls related to this transfer.
     */
    public long enqueue(Request request) {
        ContentValues values = request.toContentValues(mPackageName);
        Uri transferUri = mResolver.insert(Transfers.Impl.CONTENT_URI, values);
        long id = Long.parseLong(transferUri.getLastPathSegment());
        return id;
    }

    /**
     * Marks the specified transfer as 'to be deleted'. This is done when a completed transfer
     * is to be removed but the row was stored without enough info to delete the corresponding
     * metadata from Mediaprovider database. Actual cleanup of this row is done in TransferService.
     *
     * @param ids the IDs of the transfers to be marked 'deleted'
     * @return the number of transfers actually updated
     * @hide
     */
    public int markRowDeleted(long... ids) {
        if (ids == null || ids.length == 0) {
            // called with nothing to remove!
            throw new IllegalArgumentException("input param 'ids' can't be null");
        }
        ContentValues values = new ContentValues();
        values.put(Transfers.Impl.COLUMN_DELETED, 1);
        // if only one id is passed in, then include it in the uri itself.
        // this will eliminate a full database scan in the transfer service.
        if (ids.length == 1) {
            return mResolver.update(ContentUris.withAppendedId(mBaseUri, ids[0]), values,
                    null, null);
        }
        return mResolver.update(mBaseUri, values, getWhereClauseForIds(ids),
                getWhereArgsForIds(ids));
    }

    /**
     * Cancel transfers and remove them from the transfer manager.  Each transfer will be stopped if
     * it was running, and it will no longer be accessible through the transfer manager.
     * If there is a transfered file, partial or complete, it is deleted.
     *
     * @param ids the IDs of the transfers to remove
     * @return the number of transfers actually removed
     */
    public int remove(long... ids) {
        return markRowDeleted(ids);
    }

    /**
     * Query the transfer manager about transfers that have been requested.
     * @param query parameters specifying filters for this query
     * @return a Cursor over the result set of transfers, with columns consisting of all the
     * COLUMN_* constants.
     */
    public Cursor query(Query query) {
        Cursor underlyingCursor = query.runQuery(mResolver, UNDERLYING_COLUMNS, mBaseUri);
        if (underlyingCursor == null) {
            return null;
        }
        return new CursorTranslator(underlyingCursor, mBaseUri);
    }

    /**
     * Open a transfered file for reading.  The transfer must have completed.
     * @param id the ID of the transfer
     * @return a read-only {@link ParcelFileDescriptor}
     * @throws FileNotFoundException if the destination file does not already exist
     */
    public ParcelFileDescriptor openTransferedFile(long id) throws FileNotFoundException {
        return mResolver.openFileDescriptor(getTransferUri(id), "r");
    }

    /**
     * Returns {@link Uri} for the given transfered file id, if the file is
     * transfered successfully. otherwise, null is returned.
     *<p>
     * If the specified transfered file is in external storage (for example, /sdcard dir),
     * then it is assumed to be safe for anyone to read and the returned {@link Uri} corresponds
     * to the filepath on sdcard.
     *
     * @param id the id of the transfered file.
     * @return the {@link Uri} for the given transfered file id, if transfer was successful. null
     * otherwise.
     */
    public Uri getUriForTransferedFile(long id) {
        // to check if the file is in cache, get its destination from the database
        Query query = new Query().setFilterById(id);
        Cursor cursor = null;
        try {
            cursor = query(query);
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                if (TransferManager.STATUS_SUCCESSFUL == status) {
                    int indx = cursor.getColumnIndexOrThrow(
                            Transfers.Impl.COLUMN_DESTINATION);
                    int destination = cursor.getInt(indx);
                    // TODO: if we ever add API to TransferManager to let the caller specify
                    // non-external storage for a transfered file, then the following code
                    // should also check for that destination.
                    if (destination == Transfers.Impl.DESTINATION_CACHE_PARTITION ||
                            destination == Transfers.Impl.DESTINATION_SYSTEMCACHE_PARTITION ||
                            destination == Transfers.Impl.DESTINATION_CACHE_PARTITION_NOROAMING ||
                            destination == Transfers.Impl.DESTINATION_CACHE_PARTITION_PURGEABLE) {
                        // return private uri
                        return ContentUris.withAppendedId(Transfers.Impl.CONTENT_URI, id);
                    } else {
                        // return public uri
                        String path = cursor.getString(
                                cursor.getColumnIndexOrThrow(COLUMN_LOCAL_FILENAME));
                        return Uri.fromFile(new File(path));
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        // transfered file not found or its status is not 'successfully completed'
        return null;
    }

    /**
     * Returns {@link Uri} for the given transfered file id, if the file is
     * transfered successfully. otherwise, null is returned.
     *<p>
     * If the specified transfered file is in external storage (for example, /sdcard dir),
     * then it is assumed to be safe for anyone to read and the returned {@link Uri} corresponds
     * to the filepath on sdcard.
     *
     * @param id the id of the transfered file.
     * @return the {@link Uri} for the given transfered file id, if transfer was successful. null
     * otherwise.
     */
    public String getMimeTypeForTransferedFile(long id) {
        Query query = new Query().setFilterById(id);
        Cursor cursor = null;
        try {
            cursor = query(query);
            if (cursor == null) {
                return null;
            }
            while (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDIA_TYPE));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        // transfered file not found or its status is not 'successfully completed'
        return null;
    }

    /**
     * Restart the given transfers, which must have already completed (successfully or not).  This
     * method will only work when called from within the transfer manager's process.
     * @param ids the IDs of the transfers
     * @hide
     */
    public void restartTransfer(long... ids) {
        Cursor cursor = query(new Query().setFilterById(ids));
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                int status = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS));
                if (status != STATUS_SUCCESSFUL && status != STATUS_FAILED) {
                    throw new IllegalArgumentException("Cannot restart incomplete transfer: "
                            + cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                }
            }
        } finally {
            cursor.close();
        }

        ContentValues values = new ContentValues();
        values.put(Transfers.Impl.COLUMN_CURRENT_BYTES, 0);
        values.put(Transfers.Impl.COLUMN_TOTAL_BYTES, -1);
        values.putNull(Transfers.Impl._DATA);
        values.put(Transfers.Impl.COLUMN_STATUS, Transfers.Impl.STATUS_PENDING);
        values.put(Transfers.Impl.COLUMN_FAILED_CONNECTIONS, 0);
        mResolver.update(mBaseUri, values, getWhereClauseForIds(ids), getWhereArgsForIds(ids));
    }

    /**
     * Returns maximum size, in bytes, of transfers that may go over a mobile connection; or null if
     * there's no limit
     *
     * @param context the {@link Context} to use for accessing the {@link ContentResolver}
     * @return maximum size, in bytes, of transfers that may go over a mobile connection; or null if
     * there's no limit
     */
    public static Long getMaxBytesOverMobile(Context context) {
        try {
            return Settings.Global.getLong(context.getContentResolver(),
                    Settings.Global.TRANSFER_MAX_BYTES_OVER_MOBILE);
        } catch (SettingNotFoundException exc) {
            return null;
        }
    }

    /**
     * Returns recommended maximum size, in bytes, of transfers that may go over a mobile
     * connection; or null if there's no recommended limit.  The user will have the option to bypass
     * this limit.
     *
     * @param context the {@link Context} to use for accessing the {@link ContentResolver}
     * @return recommended maximum size, in bytes, of transfers that may go over a mobile
     * connection; or null if there's no recommended limit.
     */
    public static Long getRecommendedMaxBytesOverMobile(Context context) {
        try {
            return Settings.Global.getLong(context.getContentResolver(),
                    Settings.Global.TRANSFER_RECOMMENDED_MAX_BYTES_OVER_MOBILE);
        } catch (SettingNotFoundException exc) {
            return null;
        }
    }

    /** {@hide} */
    public static boolean isActiveNetworkExpensive(Context context) {
        // TODO: connect to NetworkPolicyManager
        return false;
    }

    /** {@hide} */
    public static long getActiveNetworkWarningBytes(Context context) {
        // TODO: connect to NetworkPolicyManager
        return -1;
    }

    /**
     * Adds a file to the transfers database system, so it could appear in Transfers App
     * (and thus become eligible for management by the Transfers App).
     * <p>
     * It is helpful to make the file scannable by MediaScanner by setting the param
     * isMediaScannerScannable to true. It makes the file visible in media managing
     * applications such as Gallery App, which could be a useful purpose of using this API.
     *
     * @param title the title that would appear for this file in Transfers App.
     * @param description the description that would appear for this file in Transfers App.
     * @param isMediaScannerScannable true if the file is to be scanned by MediaScanner. Files
     * scanned by MediaScanner appear in the applications used to view media (for example,
     * Gallery app).
     * @param mimeType mimetype of the file.
     * @param path absolute pathname to the file. The file should be world-readable, so that it can
     * be managed by the Transfers App and any other app that is used to read it (for example,
     * Gallery app to display the file, if the file contents represent a video/image).
     * @param length length of the transfered file
     * @param showNotification true if a notification is to be sent, false otherwise
     * @return  an ID for the transfer entry added to the transfers app, unique across the system
     * This ID is used to make future calls related to this transfer.
     */
    
    public long addCompletedTransfer(String title, String description,
            boolean isMediaScannerScannable, String mimeType, String path, long length,
            boolean showNotification) {
				return addCompletedTransfer(title, description, isMediaScannerScannable, mimeType, path,
				length, showNotification, false, false, false);
			}
    
    public long addCompletedUploadTransfer(String title, String description,
                                     boolean isMediaScannerScannable, String mimeType, String path, long length,
                                     boolean showNotification, boolean isUsingTor) {
        return addCompletedTransfer(title, description, isMediaScannerScannable, mimeType, path,
                length, showNotification, false, true, isUsingTor);
    }

    public long addCompletedTransferTransfer(String title, String description,
                                     boolean isMediaScannerScannable, String mimeType, String path, long length,
                                     boolean showNotification, boolean isUsingTor) {
        return addCompletedTransfer(title, description, isMediaScannerScannable, mimeType, path,
                length, showNotification, false, false, isUsingTor);
    }

    /** {@hide} */
    public long addCompletedTransfer(String title, String description,
                                     boolean isMediaScannerScannable, String mimeType, String path, long length,
                                     boolean showNotification, boolean allowWrite, boolean isTypeUpload, boolean isUsingTor) {
        // make sure the input args are non-null/non-zero
        validateArgumentIsNonEmpty("title", title);
        validateArgumentIsNonEmpty("description", description);
        validateArgumentIsNonEmpty("path", path);
        validateArgumentIsNonEmpty("mimeType", mimeType);
        if (length < 0) {
            throw new IllegalArgumentException(" invalid value for param: totalBytes");
        }

        // if there is already an entry with the given path name in transfers.db, return its id
        Request request = new Request(NON_TRANSFERMANAGER_TRANSFER)
                .setTitle(title)
                .setDescription(description)
                .setMimeType(mimeType)
                .setIsTypeUpload(isTypeUpload)//ml
                .setIsUsingTor(isUsingTor);
        ContentValues values = request.toContentValues(null);
        values.put(Transfers.Impl.COLUMN_DESTINATION,
                Transfers.Impl.DESTINATION_NON_TRANSFERMANAGER_TRANSFER);
        values.put(Transfers.Impl._DATA, path);
        values.put(Transfers.Impl.COLUMN_STATUS, Transfers.Impl.STATUS_SUCCESS);
        values.put(Transfers.Impl.COLUMN_TOTAL_BYTES, length);
        values.put(Transfers.Impl.COLUMN_MEDIA_SCANNED,
                (isMediaScannerScannable) ? Request.SCANNABLE_VALUE_YES :
                        Request.SCANNABLE_VALUE_NO);
        values.put(Transfers.Impl.COLUMN_VISIBILITY, (showNotification) ?
                Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION : Request.VISIBILITY_HIDDEN);
        values.put(Transfers.Impl.COLUMN_ALLOW_WRITE, allowWrite ? 1 : 0);
        Uri transferUri = mResolver.insert(Transfers.Impl.CONTENT_URI, values);
        if (transferUri == null) {
            return -1;
        }
        return Long.parseLong(transferUri.getLastPathSegment());
    }

    private static final String NON_TRANSFERMANAGER_TRANSFER =
            "non-dwnldmngr-transfer-dont-retry2transfer";

    private static void validateArgumentIsNonEmpty(String paramName, String val) {
        if (TextUtils.isEmpty(val)) {
            throw new IllegalArgumentException(paramName + " can't be null");
        }
    }

    /**
     * Get the TransferProvider URI for the transfer with the given ID.
     *
     * @hide
     */
    public Uri getTransferUri(long id) {
        return ContentUris.withAppendedId(mBaseUri, id);
    }

    /**
     * Get a parameterized SQL WHERE clause to select a bunch of IDs.
     */
    static String getWhereClauseForIds(long[] ids) {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("(");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                whereClause.append("OR ");
            }
            whereClause.append(Transfers.Impl._ID);
            whereClause.append(" = ? ");
        }
        whereClause.append(")");
        return whereClause.toString();
    }

    /**
     * Get the selection args for a clause returned by {@link #getWhereClauseForIds(long[])}.
     */
    static String[] getWhereArgsForIds(long[] ids) {
        String[] whereArgs = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            whereArgs[i] = Long.toString(ids[i]);
        }
        return whereArgs;
    }

    /**
     * This class wraps a cursor returned by TransferProvider -- the "underlying cursor" -- and
     * presents a different set of columns, those defined in the TransferManager.COLUMN_* constants.
     * Some columns correspond directly to underlying values while others are computed from
     * underlying data.
     */
    private static class CursorTranslator extends CursorWrapper {
        private Uri mBaseUri;

        public CursorTranslator(Cursor cursor, Uri baseUri) {
            super(cursor);
            mBaseUri = baseUri;
        }

        @Override
        public int getInt(int columnIndex) {
            return (int) getLong(columnIndex);
        }

        @Override
        public long getLong(int columnIndex) {
            if (getColumnName(columnIndex).equals(COLUMN_REASON)) {
                return getReason(super.getInt(getColumnIndex(Transfers.Impl.COLUMN_STATUS)));
            } else if (getColumnName(columnIndex).equals(COLUMN_STATUS)) {
                return translateStatus(super.getInt(getColumnIndex(Transfers.Impl.COLUMN_STATUS)));
            } else {
                return super.getLong(columnIndex);
            }
        }

        @Override
        public String getString(int columnIndex) {
            return (getColumnName(columnIndex).equals(COLUMN_LOCAL_URI)) ? getLocalUri() :
                    super.getString(columnIndex);
        }

        private String getLocalUri() {
            long destinationType = getLong(getColumnIndex(Transfers.Impl.COLUMN_DESTINATION));
            if (destinationType == Transfers.Impl.DESTINATION_FILE_URI ||
                    destinationType == Transfers.Impl.DESTINATION_EXTERNAL ||
                    destinationType == Transfers.Impl.DESTINATION_NON_TRANSFERMANAGER_TRANSFER) {
                String localPath = getString(getColumnIndex(COLUMN_LOCAL_FILENAME));
                if (localPath == null) {
                    return null;
                }
                return Uri.fromFile(new File(localPath)).toString();
            }

            // return content URI for cache transfer
            long transferId = getLong(getColumnIndex(Transfers.Impl._ID));
            return ContentUris.withAppendedId(mBaseUri, transferId).toString();
        }

        private long getReason(int status) {
            switch (translateStatus(status)) {
                case STATUS_FAILED:
                    return getErrorCode(status);

                case STATUS_PAUSED:
                    return getPausedReason(status);

                default:
                    return 0; // arbitrary value when status is not an error
            }
        }

        private long getPausedReason(int status) {
            switch (status) {
                case Transfers.Impl.STATUS_WAITING_TO_RETRY:
                    return PAUSED_WAITING_TO_RETRY;

                case Transfers.Impl.STATUS_WAITING_FOR_NETWORK:
                    return PAUSED_WAITING_FOR_NETWORK;

                case Transfers.Impl.STATUS_QUEUED_FOR_WIFI:
                    return PAUSED_QUEUED_FOR_WIFI;

                default:
                    return PAUSED_UNKNOWN;
            }
        }

        private long getErrorCode(int status) {
            if ((400 <= status && status < Transfers.Impl.MIN_ARTIFICIAL_ERROR_STATUS)
                    || (500 <= status && status < 600)) {
                // HTTP status code
                return status;
            }

            switch (status) {
                case Transfers.Impl.STATUS_FILE_ERROR:
                    return ERROR_FILE_ERROR;

                case Transfers.Impl.STATUS_UNHANDLED_HTTP_CODE:
                case Transfers.Impl.STATUS_UNHANDLED_REDIRECT:
                    return ERROR_UNHANDLED_HTTP_CODE;

                case Transfers.Impl.STATUS_HTTP_DATA_ERROR:
                    return ERROR_HTTP_DATA_ERROR;

                case Transfers.Impl.STATUS_TOO_MANY_REDIRECTS:
                    return ERROR_TOO_MANY_REDIRECTS;

                case Transfers.Impl.STATUS_INSUFFICIENT_SPACE_ERROR:
                    return ERROR_INSUFFICIENT_SPACE;

                case Transfers.Impl.STATUS_DEVICE_NOT_FOUND_ERROR:
                    return ERROR_DEVICE_NOT_FOUND;

                case Transfers.Impl.STATUS_CANNOT_RESUME:
                    return ERROR_CANNOT_RESUME;

                case Transfers.Impl.STATUS_FILE_ALREADY_EXISTS_ERROR:
                    return ERROR_FILE_ALREADY_EXISTS;

                default:
                    return ERROR_UNKNOWN;
            }
        }

        private int translateStatus(int status) {
            switch (status) {
                case Transfers.Impl.STATUS_PENDING:
                    return STATUS_PENDING;

                case Transfers.Impl.STATUS_RUNNING:
                    return STATUS_RUNNING;

                case Transfers.Impl.STATUS_PAUSED_BY_APP:
                case Transfers.Impl.STATUS_WAITING_TO_RETRY:
                case Transfers.Impl.STATUS_WAITING_FOR_NETWORK:
                case Transfers.Impl.STATUS_QUEUED_FOR_WIFI:
                    return STATUS_PAUSED;

                case Transfers.Impl.STATUS_SUCCESS:
                    return STATUS_SUCCESSFUL;

                default:
                    assert Transfers.Impl.isStatusError(status);
                    return STATUS_FAILED;
            }
        }
    }
}
