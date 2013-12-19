package android.provider;

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

        import android.app.TransferManager;
        import android.content.Context;
        import android.net.NetworkPolicyManager;
        import android.net.Uri;

/**
 * The Transfer Manager
 *
 * @pending
 */
public final class Transfers {
    private Transfers() {}

    /**
     * Implementation details
     *
     * Exposes constants used to interact with the transfer manager's
     * content provider.
     * The constants URI ... STATUS are the names of columns in the transfers table.
     *
     * @hide
     */
    public static final class Impl implements BaseColumns {
        private Impl() {}

        /**
         * The permission to access the 
 manager
         */
        public static final String PERMISSION_ACCESS = "android.permission.ACCESS_TRANSFER_MANAGER";

        /**
         * The permission to access the transfer manager's advanced functions
         */
        public static final String PERMISSION_ACCESS_ADVANCED =
                "android.permission.ACCESS_TRANSFER_MANAGER_ADVANCED";

        /**
         * The permission to access the all the transfers in the manager.
         */
        public static final String PERMISSION_ACCESS_ALL =
                "android.permission.ACCESS_ALL_TRANSFERS";

        /**
         * The permission to directly access the transfer manager's cache
         * directory
         */
        public static final String PERMISSION_CACHE = "android.permission.ACCESS_CACHE_FILESYSTEM";

        /**
         * The permission to send broadcasts on transfer completion
         */
        public static final String PERMISSION_SEND_INTENTS =
                "android.permission.SEND_TRANSFER_COMPLETED_INTENTS";

        /**
         * The permission to transfer files to the cache partition that won't be automatically
         * purged when space is needed.
         */
        public static final String PERMISSION_CACHE_NON_PURGEABLE =
                "android.permission.TRANSFER_CACHE_NON_PURGEABLE";

        /**
         * The permission to transfer files without any system notification being shown.
         */
        public static final String PERMISSION_NO_NOTIFICATION =
                "android.permission.TRANSFER_WITHOUT_NOTIFICATION";

        /**
         * The content:// URI to access transfers owned by the caller's UID.
         */
        public static final Uri CONTENT_URI =
                Uri.parse("content://transfers/my_transfers");

        /**
         * The content URI for accessing all transfers across all UIDs (requires the
         * ACCESS_ALL_TRANSFERS permission).
         */
        public static final Uri ALL_TRANSFERS_CONTENT_URI =
                Uri.parse("content://transfers/all_transfers");

        /** URI segment to access a publicly accessible transfered file */
        public static final String PUBLICLY_ACCESSIBLE_TRANSFERS_URI_SEGMENT = "public_transfers";

        /**
         * The content URI for accessing publicly accessible transfers (i.e., it requires no
         * permissions to access this transfered file)
         */
        public static final Uri PUBLICLY_ACCESSIBLE_TRANSFERS_URI =
                Uri.parse("content://transfers/" + PUBLICLY_ACCESSIBLE_TRANSFERS_URI_SEGMENT);

        /**
         * Broadcast Action: this is sent by the transfer manager to the app
         * that had initiated a transfer when that transfer completes. The
         * transfer's content: uri is specified in the intent's data.
         */
        public static final String ACTION_TRANSFER_COMPLETED =
                "android.intent.action.TRANSFER_COMPLETED";

        /**
         * Broadcast Action: this is sent by the transfer manager to the app
         * that had initiated a transfer when the user selects the notification
         * associated with that transfer. The transfer's content: uri is specified
         * in the intent's data if the click is associated with a single transfer,
         * or Transfers.CONTENT_URI if the notification is associated with
         * multiple transfers.
         * Note: this is not currently sent for transfers that have completed
         * successfully.
         */
        public static final String ACTION_NOTIFICATION_CLICKED =
                "android.intent.action.TRANSFER_NOTIFICATION_CLICKED";

        /**
         * The name of the column containing the URI of the data being transfered.
         * <P>Type: TEXT</P>
         * <P>Owner can Init/Read</P>
         */
        public static final String COLUMN_URI = "uri";

        /**
         * The name of the column containing application-specific data.
         * <P>Type: TEXT</P>
         * <P>Owner can Init/Read/Write</P>
         */
        public static final String COLUMN_APP_DATA = "entity";

        /**
         * The name of the column containing the flags that indicates whether
         * the initiating application is capable of verifying the integrity of
         * the transfered file. When this flag is set, the transfer manager
         * performs transfers and reports success even in some situations where
         * it can't guarantee that the transfer has completed (e.g. when doing
         * a byte-range request without an ETag, or when it can't determine
         * whether a transfer fully completed).
         * <P>Type: BOOLEAN</P>
         * <P>Owner can Init</P>
         */
        public static final String COLUMN_NO_INTEGRITY = "no_integrity";

        /**
         * The name of the column containing the filename that the initiating
         * application recommends. When possible, the transfer manager will attempt
         * to use this filename, or a variation, as the actual name for the file.
         * <P>Type: TEXT</P>
         * <P>Owner can Init</P>
         */
        public static final String COLUMN_FILE_NAME_HINT = "hint";

        /**
         * The name of the column containing the filename where the transfered data
         * was actually stored.
         * <P>Type: TEXT</P>
         * <P>Owner can Read</P>
         */
        public static final String _DATA = "_data";

        /**
         * The name of the column containing the MIME type of the transfered data.
         * <P>Type: TEXT</P>
         * <P>Owner can Init/Read</P>
         */
        public static final String COLUMN_MIME_TYPE = "mimetype";

        /**
         * The name of the column containing the flag that controls the destination
         * of the transfer. See the DESTINATION_* constants for a list of legal values.
         * <P>Type: INTEGER</P>
         * <P>Owner can Init</P>
         */
        public static final String COLUMN_DESTINATION = "destination";

        /**
         * The name of the column containing the flags that controls whether the
         * transfer is displayed by the UI. See the VISIBILITY_* constants for
         * a list of legal values.
         * <P>Type: INTEGER</P>
         * <P>Owner can Init/Read/Write</P>
         */
        public static final String COLUMN_VISIBILITY = "visibility";

        /**
         * The name of the column containing the current control state  of the transfer.
         * Applications can write to this to control (pause/resume) the transfer.
         * the CONTROL_* constants for a list of legal values.
         * <P>Type: INTEGER</P>
         * <P>Owner can Read</P>
         */
        public static final String COLUMN_CONTROL = "control";

        /**
         * The name of the column containing the current status of the transfer.
         * Applications can read this to follow the progress of each transfer. See
         * the STATUS_* constants for a list of legal values.
         * <P>Type: INTEGER</P>
         * <P>Owner can Read</P>
         */
        public static final String COLUMN_STATUS = "status";

        /**
         * The name of the column containing the date at which some interesting
         * status changed in the transfer. Stored as a System.currentTimeMillis()
         * value.
         * <P>Type: BIGINT</P>
         * <P>Owner can Read</P>
         */
        public static final String COLUMN_LAST_MODIFICATION = "lastmod";

        /**
         * The name of the column containing the package name of the application
         * that initiating the transfer. The transfer manager will send
         * notifications to a component in this package when the transfer completes.
         * <P>Type: TEXT</P>
         * <P>Owner can Init/Read</P>
         */
        public static final String COLUMN_NOTIFICATION_PACKAGE = "notificationpackage";

        /**
         * The name of the column containing the component name of the class that
         * will receive notifications associated with the transfer. The
         * package/class combination is passed to
         * Intent.setClassName(String,String).
         * <P>Type: TEXT</P>
         * <P>Owner can Init/Read</P>
         */
        public static final String COLUMN_NOTIFICATION_CLASS = "notificationclass";

        /**
         * If extras are specified when requesting a transfer they will be provided in the intent that
         * is sent to the specified class and package when a transfer has finished.
         * <P>Type: TEXT</P>
         * <P>Owner can Init</P>
         */
        public static final String COLUMN_NOTIFICATION_EXTRAS = "notificationextras";

        /**
         * The name of the column contain the values of the cookie to be used for
         * the transfer. This is used directly as the value for the Cookie: HTTP
         * header that gets sent with the request.
         * <P>Type: TEXT</P>
         * <P>Owner can Init</P>
         */
        public static final String COLUMN_COOKIE_DATA = "cookiedata";

        /**
         * The name of the column containing the user agent that the initiating
         * application wants the transfer manager to use for this transfer.
         * <P>Type: TEXT</P>
         * <P>Owner can Init</P>
         */
        public static final String COLUMN_USER_AGENT = "useragent";

        /**
         * The name of the column containing the referer (sic) that the initiating
         * application wants the transfer manager to use for this transfer.
         * <P>Type: TEXT</P>
         * <P>Owner can Init</P>
         */
        public static final String COLUMN_REFERER = "referer";

        /**
         * The name of the column containing the total size of the file being
         * transfered.
         * <P>Type: INTEGER</P>
         * <P>Owner can Read</P>
         */
        public static final String COLUMN_TOTAL_BYTES = "total_bytes";

        /**
         * The name of the column containing the size of the part of the file that
         * has been transfered so far.
         * <P>Type: INTEGER</P>
         * <P>Owner can Read</P>
         */
        public static final String COLUMN_CURRENT_BYTES = "current_bytes";

        /**
         * The name of the column where the initiating application can provide the
         * UID of another application that is allowed to access this transfer. If
         * multiple applications share the same UID, all those applications will be
         * allowed to access this transfer. This column can be updated after the
         * transfer is initiated. This requires the permission
         * android.permission.ACCESS_TRANSFER_MANAGER_ADVANCED.
         * <P>Type: INTEGER</P>
         * <P>Owner can Init</P>
         */
        public static final String COLUMN_OTHER_UID = "otheruid";

        /**
         * The name of the column where the initiating application can provided the
         * title of this transfer. The title will be displayed ito the user in the
         * list of transfers.
         * <P>Type: TEXT</P>
         * <P>Owner can Init/Read/Write</P>
         */
        public static final String COLUMN_TITLE = "title";

        /**
         * The name of the column where the initiating application can provide the
         * description of this transfer. The description will be displayed to the
         * user in the list of transfers.
         * <P>Type: TEXT</P>
         * <P>Owner can Init/Read/Write</P>
         */
        public static final String COLUMN_DESCRIPTION = "description";

        /**
         * The name of the column indicating whether the transfer was requesting through the public
         * API.  This controls some differences in behavior.
         * <P>Type: BOOLEAN</P>
         * <P>Owner can Init/Read</P>
         */
        public static final String COLUMN_IS_PUBLIC_API = "is_public_api";

        /**
         * The name of the column holding a bitmask of allowed network types.  This is only used for
         * public API transfers.
         * <P>Type: INTEGER</P>
         * <P>Owner can Init/Read</P>
         */
        public static final String COLUMN_ALLOWED_NETWORK_TYPES = "allowed_network_types";

        /**
         * The name of the column indicating whether roaming connections can be used.  This is only
         * used for public API transfers.
         * <P>Type: BOOLEAN</P>
         * <P>Owner can Init/Read</P>
         */
        public static final String COLUMN_ALLOW_ROAMING = "allow_roaming";

        /**
         * The name of the column indicating whether metered connections can be used.  This is only
         * used for public API transfers.
         * <P>Type: BOOLEAN</P>
         * <P>Owner can Init/Read</P>
         */
        public static final String COLUMN_ALLOW_METERED = "allow_metered";

        /**
         * Whether or not this transfer should be displayed in the system's Transfers UI.  Defaults
         * to true.
         * <P>Type: INTEGER</P>
         * <P>Owner can Init/Read</P>
         */
        public static final String COLUMN_IS_VISIBLE_IN_TRANSFERS_UI = "is_visible_in_transfers_ui";

        /**
         * If true, the user has confirmed that this transfer can proceed over the mobile network
         * even though it exceeds the recommended maximum size.
         * <P>Type: BOOLEAN</P>
         */
        public static final String COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT =
                "bypass_recommended_size_limit";

        /**
         * Set to true if this transfer is deleted. It is completely removed from the database
         * when MediaProvider database also deletes the metadata asociated with this transfered file.
         * <P>Type: BOOLEAN</P>
         * <P>Owner can Read</P>
         */
        public static final String COLUMN_DELETED = "deleted";

        /**
         * The URI to the corresponding entry in MediaProvider for this transfered entry. It is
         * used to delete the entries from MediaProvider database when it is deleted from the
         * transfered list.
         * <P>Type: TEXT</P>
         * <P>Owner can Read</P>
         */
        public static final String COLUMN_MEDIAPROVIDER_URI = "mediaprovider_uri";

        /**
         * The column that is used to remember whether the media scanner was invoked.
         * It can take the values: null or 0(not scanned), 1(scanned), 2 (not scannable).
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_MEDIA_SCANNED = "scanned";

        /**
         * The column with errorMsg for a failed transfered.
         * Used only for debugging purposes.
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_ERROR_MSG = "errorMsg";

        /**
         *  This column stores the source of the last update to this row.
         *  This column is only for internal use.
         *  Valid values are indicated by LAST_UPDATESRC_* constants.
         * <P>Type: INT</P>
         */
        public static final String COLUMN_LAST_UPDATESRC = "lastUpdateSrc";

        /** The column that is used to count retries */
        public static final String COLUMN_FAILED_CONNECTIONS = "numfailed";

        public static final String COLUMN_ALLOW_WRITE = "allow_write";

        /** The column that is used to tell the Transfer Type */
        public static final String COLUMN_IS_TYPE_UPLOAD = "isTypeUpload";//ml

        /** The column that is used to tell if the Transfer is using Tor for security */
        public static final String COLUMN_IS_USING_TOR = "isUsingTor";

        /**
         * default value for {@link #COLUMN_LAST_UPDATESRC}.
         * This value is used when this column's value is not relevant.
         */
        public static final int LAST_UPDATESRC_NOT_RELEVANT = 0;

        /**
         * One of the values taken by {@link #COLUMN_LAST_UPDATESRC}.
         * This value is used when the update is NOT to be relayed to the TransferService
         * (and thus spare TransferService from scanning the database when this change occurs)
         */
        public static final int LAST_UPDATESRC_DONT_NOTIFY_TRANSFERSVC = 1;

        /*
         * Lists the destinations that an application can specify for a transfer.
         */

        /**
         * This transfer will be saved to the external storage. This is the
         * default behavior, and should be used for any file that the user
         * can freely access, copy, delete. Even with that destination,
         * unencrypted DRM files are saved in secure internal storage.
         * Transfers to the external destination only write files for which
         * there is a registered handler. The resulting files are accessible
         * by filename to all applications.
         */
        public static final int DESTINATION_EXTERNAL = 0;

        /**
         * This transfer will be saved to the transfer manager's private
         * partition. This is the behavior used by applications that want to
         * transfer private files that are used and deleted soon after they
         * get transfered. All file types are allowed, and only the initiating
         * application can access the file (indirectly through a content
         * provider). This requires the
         * android.permission.ACCESS_TRANSFER_MANAGER_ADVANCED permission.
         */
        public static final int DESTINATION_CACHE_PARTITION = 1;

        /**
         * This transfer will be saved to the transfer manager's private
         * partition and will be purged as necessary to make space. This is
         * for private files (similar to CACHE_PARTITION) that aren't deleted
         * immediately after they are used, and are kept around by the transfer
         * manager as long as space is available.
         */
        public static final int DESTINATION_CACHE_PARTITION_PURGEABLE = 2;

        /**
         * This transfer will be saved to the transfer manager's private
         * partition, as with DESTINATION_CACHE_PARTITION, but the transfer
         * will not proceed if the user is on a roaming data connection.
         */
        public static final int DESTINATION_CACHE_PARTITION_NOROAMING = 3;

        /**
         * This transfer will be saved to the location given by the file URI in
         * {@link #COLUMN_FILE_NAME_HINT}.
         */
        public static final int DESTINATION_FILE_URI = 4;

        /**
         * This transfer will be saved to the system cache ("/cache")
         * partition. This option is only used by system apps and so it requires
         * android.permission.ACCESS_CACHE_FILESYSTEM permission.
         */
        public static final int DESTINATION_SYSTEMCACHE_PARTITION = 5;

        /**
         * This transfer was completed by the caller (i.e., NOT transfermanager)
         * and caller wants to have this transfer displayed in Transfers App.
         */
        public static final int DESTINATION_NON_TRANSFERMANAGER_TRANSFER = 6;

        /**
         * This transfer is allowed to run.
         */
        public static final int CONTROL_RUN = 0;

        /**
         * This transfer must pause at the first opportunity.
         */
        public static final int CONTROL_PAUSED = 1;

        /*
         * Lists the states that the transfer manager can set on a transfer
         * to notify applications of the transfer progress.
         * The codes follow the HTTP families:<br>
         * 1xx: informational<br>
         * 2xx: success<br>
         * 3xx: redirects (not used by the transfer manager)<br>
         * 4xx: client errors<br>
         * 5xx: server errors
         */

        /**
         * Returns whether the status is informational (i.e. 1xx).
         */
        public static boolean isStatusInformational(int status) {
            return (status >= 100 && status < 200);
        }

        /**
         * Returns whether the status is a success (i.e. 2xx).
         */
        public static boolean isStatusSuccess(int status) {
            return (status >= 200 && status < 300);
        }

        /**
         * Returns whether the status is an error (i.e. 4xx or 5xx).
         */
        public static boolean isStatusError(int status) {
            return (status >= 400 && status < 600);
        }

        /**
         * Returns whether the status is a client error (i.e. 4xx).
         */
        public static boolean isStatusClientError(int status) {
            return (status >= 400 && status < 500);
        }

        /**
         * Returns whether the status is a server error (i.e. 5xx).
         */
        public static boolean isStatusServerError(int status) {
            return (status >= 500 && status < 600);
        }

        /**
         * this method determines if a notification should be displayed for a
         * given {@link #COLUMN_VISIBILITY} value
         * @param visibility the value of {@link #COLUMN_VISIBILITY}.
         * @return true if the notification should be displayed. false otherwise.
         */
        public static boolean isNotificationToBeDisplayed(int visibility) {
            return visibility == TransferManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED ||
                    visibility == TransferManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION;
        }

        /**
         * Returns whether the transfer has completed (either with success or
         * error).
         */
        public static boolean isStatusCompleted(int status) {
            return (status >= 200 && status < 300) || (status >= 400 && status < 600);
        }

        /**
         * This transfer hasn't stated yet
         */
        public static final int STATUS_PENDING = 190;

        /**
         * This transfer has started
         */
        public static final int STATUS_RUNNING = 192;

        /**
         * This transfer has been paused by the owning app.
         */
        public static final int STATUS_PAUSED_BY_APP = 193;

        /**
         * This transfer encountered some network error and is waiting before retrying the request.
         */
        public static final int STATUS_WAITING_TO_RETRY = 194;

        /**
         * This transfer is waiting for network connectivity to proceed.
         */
        public static final int STATUS_WAITING_FOR_NETWORK = 195;

        /**
         * This transfer exceeded a size limit for mobile networks and is waiting for a Wi-Fi
         * connection to proceed.
         */
        public static final int STATUS_QUEUED_FOR_WIFI = 196;

        /**
         * This transfer couldn't be completed due to insufficient storage
         * space.  Typically, this is because the SD card is full.
         */
        public static final int STATUS_INSUFFICIENT_SPACE_ERROR = 198;

        /**
         * This transfer couldn't be completed because no external storage
         * device was found.  Typically, this is because the SD card is not
         * mounted.
         */
        public static final int STATUS_DEVICE_NOT_FOUND_ERROR = 199;

        /**
         * This transfer has successfully completed.
         * Warning: there might be other status values that indicate success
         * in the future.
         * Use isSucccess() to capture the entire category.
         */
        public static final int STATUS_SUCCESS = 200;

        /**
         * This request couldn't be parsed. This is also used when processing
         * requests with unknown/unsupported URI schemes.
         */
        public static final int STATUS_BAD_REQUEST = 400;

        /**
         * This transfer can't be performed because the content type cannot be
         * handled.
         */
        public static final int STATUS_NOT_ACCEPTABLE = 406;

        /**
         * This transfer cannot be performed because the length cannot be
         * determined accurately. This is the code for the HTTP error "Length
         * Required", which is typically used when making requests that require
         * a content length but don't have one, and it is also used in the
         * client when a response is received whose length cannot be determined
         * accurately (therefore making it impossible to know when a transfer
         * completes).
         */
        public static final int STATUS_LENGTH_REQUIRED = 411;

        /**
         * This transfer was interrupted and cannot be resumed.
         * This is the code for the HTTP error "Precondition Failed", and it is
         * also used in situations where the client doesn't have an ETag at all.
         */
        public static final int STATUS_PRECONDITION_FAILED = 412;

        /**
         * The lowest-valued error status that is not an actual HTTP status code.
         */
        public static final int MIN_ARTIFICIAL_ERROR_STATUS = 488;

        /**
         * The requested destination file already exists.
         */
        public static final int STATUS_FILE_ALREADY_EXISTS_ERROR = 488;

        /**
         * Some possibly transient error occurred, but we can't resume the transfer.
         */
        public static final int STATUS_CANNOT_RESUME = 489;

        /**
         * This transfer was canceled
         */
        public static final int STATUS_CANCELED = 490;

        /**
         * This transfer has completed with an error.
         * Warning: there will be other status values that indicate errors in
         * the future. Use isStatusError() to capture the entire category.
         */
        public static final int STATUS_UNKNOWN_ERROR = 491;

        /**
         * This transfer couldn't be completed because of a storage issue.
         * Typically, that's because the filesystem is missing or full.
         * Use the more specific {@link #STATUS_INSUFFICIENT_SPACE_ERROR}
         * and {@link #STATUS_DEVICE_NOT_FOUND_ERROR} when appropriate.
         */
        public static final int STATUS_FILE_ERROR = 492;

        /**
         * This transfer couldn't be completed because of an HTTP
         * redirect response that the transfer manager couldn't
         * handle.
         */
        public static final int STATUS_UNHANDLED_REDIRECT = 493;

        /**
         * This transfer couldn't be completed because of an
         * unspecified unhandled HTTP code.
         */
        public static final int STATUS_UNHANDLED_HTTP_CODE = 494;

        /**
         * This transfer couldn't be completed because of an
         * error receiving or processing data at the HTTP level.
         */
        public static final int STATUS_HTTP_DATA_ERROR = 495;

        /**
         * This transfer couldn't be completed because of an
         * HttpException while setting up the request.
         */
        public static final int STATUS_HTTP_EXCEPTION = 496;

        /**
         * This transfer couldn't be completed because there were
         * too many redirects.
         */
        public static final int STATUS_TOO_MANY_REDIRECTS = 497;

        /**
         * This transfer has failed because requesting application has been
         * blocked by {@link NetworkPolicyManager}.
         *
         * @hide
         * @deprecated since behavior now uses
         *             {@link #STATUS_WAITING_FOR_NETWORK}
         */
        @Deprecated
        public static final int STATUS_BLOCKED = 498;

        /** {@hide} */
        public static String statusToString(int status) {
            switch (status) {
                case STATUS_PENDING: return "PENDING";
                case STATUS_RUNNING: return "RUNNING";
                case STATUS_PAUSED_BY_APP: return "PAUSED_BY_APP";
                case STATUS_WAITING_TO_RETRY: return "WAITING_TO_RETRY";
                case STATUS_WAITING_FOR_NETWORK: return "WAITING_FOR_NETWORK";
                case STATUS_QUEUED_FOR_WIFI: return "QUEUED_FOR_WIFI";
                case STATUS_INSUFFICIENT_SPACE_ERROR: return "INSUFFICIENT_SPACE_ERROR";
                case STATUS_DEVICE_NOT_FOUND_ERROR: return "DEVICE_NOT_FOUND_ERROR";
                case STATUS_SUCCESS: return "SUCCESS";
                case STATUS_BAD_REQUEST: return "BAD_REQUEST";
                case STATUS_NOT_ACCEPTABLE: return "NOT_ACCEPTABLE";
                case STATUS_LENGTH_REQUIRED: return "LENGTH_REQUIRED";
                case STATUS_PRECONDITION_FAILED: return "PRECONDITION_FAILED";
                case STATUS_FILE_ALREADY_EXISTS_ERROR: return "FILE_ALREADY_EXISTS_ERROR";
                case STATUS_CANNOT_RESUME: return "CANNOT_RESUME";
                case STATUS_CANCELED: return "CANCELED";
                case STATUS_UNKNOWN_ERROR: return "UNKNOWN_ERROR";
                case STATUS_FILE_ERROR: return "FILE_ERROR";
                case STATUS_UNHANDLED_REDIRECT: return "UNHANDLED_REDIRECT";
                case STATUS_UNHANDLED_HTTP_CODE: return "UNHANDLED_HTTP_CODE";
                case STATUS_HTTP_DATA_ERROR: return "HTTP_DATA_ERROR";
                case STATUS_HTTP_EXCEPTION: return "HTTP_EXCEPTION";
                case STATUS_TOO_MANY_REDIRECTS: return "TOO_MANY_REDIRECTS";
                case STATUS_BLOCKED: return "BLOCKED";
                default: return Integer.toString(status);
            }
        }

        /**
         * This transfer is visible but only shows in the notifications
         * while it's in progress.
         */
        public static final int VISIBILITY_VISIBLE = TransferManager.Request.VISIBILITY_VISIBLE;

        /**
         * This transfer is visible and shows in the notifications while
         * in progress and after completion.
         */
        public static final int VISIBILITY_VISIBLE_NOTIFY_COMPLETED =
                TransferManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;

        /**
         * This transfer doesn't show in the UI or in the notifications.
         */
        public static final int VISIBILITY_HIDDEN = TransferManager.Request.VISIBILITY_HIDDEN;

        /**
         * Constants related to HTTP request headers associated with each transfer.
         */
        public static class RequestHeaders {
            public static final String HEADERS_DB_TABLE = "request_headers";
            public static final String COLUMN_TRANSFER_ID = "transfer_id";
            public static final String COLUMN_HEADER = "header";
            public static final String COLUMN_VALUE = "value";

            /**
             * Path segment to add to a transfer URI to retrieve request headers
             */
            public static final String URI_SEGMENT = "headers";

            /**
             * Prefix for ContentValues keys that contain HTTP header lines, to be passed to
             * TransferProvider.insert().
             */
            public static final String INSERT_KEY_PREFIX = "http_header_";
        }
    }

    /**
     * Query where clause for general querying.
     */
    private static final String QUERY_WHERE_CLAUSE = Impl.COLUMN_NOTIFICATION_PACKAGE + "=? AND "
            + Impl.COLUMN_NOTIFICATION_CLASS + "=?";

    /**
     * Delete all the transfers for a package/class pair.
     */
    public static final void removeAllTransfersByPackage(
            Context context, String notification_package, String notification_class) {
        context.getContentResolver().delete(Impl.CONTENT_URI, QUERY_WHERE_CLAUSE,
                new String[] { notification_package, notification_class });
    }
}
