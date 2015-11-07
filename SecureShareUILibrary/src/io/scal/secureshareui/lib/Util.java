
package io.scal.secureshareui.lib;

import timber.log.Timber;

import java.security.SecureRandom;
import java.util.Random;

import info.guardianproject.onionkit.ui.OrbotHelper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;

public class Util {

    // netcipher
    public static final String ORBOT_HOST = "127.0.0.1";
    public static final int ORBOT_HTTP_PORT = 8118;
    public static final int ORBOT_SOCKS_PORT = 9050;

    private static OrbotHelper getOrbotHelper(Context mContext) {
        OrbotHelper orbotHelper = new OrbotHelper(mContext);
        return orbotHelper;
    }

    public static boolean isOrbotInstalled(Context mContext) {
        return getOrbotHelper(mContext).isOrbotInstalled();
    }

    public static boolean isOrbotRunning(Context mContext) {
        return getOrbotHelper(mContext).isOrbotRunning();
    }

    public static boolean isOrbotInstalledAndRunning(Context mContext) {
        return isOrbotInstalled(mContext) && isOrbotRunning(mContext);
    }

    //TODO move strings to resources and send to tor app
    public static boolean checkIsTorRunningAlert(final Context context) {
        boolean wasTorRunning = false;

        if(getOrbotHelper(context).isOrbotRunning()) {
            wasTorRunning = true;
        } else {
            new AlertDialog.Builder(context)
                    .setTitle("Orbot Not Running")
                    .setMessage("Using Tor requires Orbot be actively running. Would you like to start Tor now?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        return wasTorRunning;
    }

    //TODO move strings to resources
    public static boolean checkIsTorInstalledDialog(final Context context) {
        boolean wasTorInstalled = false;

        if(isOrbotInstalled(context)) {
            wasTorInstalled = true;
        } else {
            new AlertDialog.Builder(context)
                    .setTitle("Install Orbot")
                    .setCancelable(true)
                    .setMessage("Using Tor requires a separate installation of Orbot. Would you like to install this now?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("market://details" + "?id=org.torproject.android"));
                            context.startActivity(intent);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        return wasTorInstalled;
    }

    // TODO audit code for security since we use the to generate random strings for url slugs
    public static final class RandomString
    {

      /* Assign a string that contains the set of characters you allow. */
      private static final String symbols = "abcdefghijklmnopqrstuvwxyz0123456789";

      private final Random random = new SecureRandom();

      private final char[] buf;

      public RandomString(int length)
      {
        if (length < 1)
          throw new IllegalArgumentException("length < 1: " + length);
        buf = new char[length];
      }

      public String nextString()
      {
        for (int idx = 0; idx < buf.length; ++idx) 
          buf[idx] = symbols.charAt(random.nextInt(symbols.length()));
        return new String(buf);
      }

    }
    
	public static String getMediaType(String mediaPath) {
		String result = null;
		String fileExtension = MimeTypeMap.getFileExtensionFromUrl(mediaPath);		
		result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
		
		if (result == null) {
			if (mediaPath.endsWith("wav")) {
				result = "audio/wav";
			}
			else if (mediaPath.endsWith("mp3")) {
				result = "audio/mpeg";
			}
			else if (mediaPath.endsWith("3gp")) {
				result = "audio/3gpp";
			}
			else if (mediaPath.endsWith("mp4")) {
				result = "video/mp4";
			}
			else if (mediaPath.endsWith("jpg")) {
				result = "image/jpeg";
			}
			else if (mediaPath.endsWith("png")) {
				result = "image/png";
			}
		}
		
		if (result.contains("audio")) {
			return "audio";
		} else if(result.contains("image")) {
			return "image";
		} else if(result.contains("video")) {
			return "movies";
		}
		
		return null;
	}
	
	public static void clearWebviewAndCookies(WebView webview, Activity activity) {
		CookieSyncManager.createInstance(activity);
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();	
		
		if(webview != null) {
			webview.clearHistory();
			webview.clearCache(true);
			webview.clearFormData();
			webview.loadUrl("about:blank");			
			webview.destroy();
		}
	}

    public static boolean isEmpty(String string) {
        return string == null || string.trim().isEmpty();
    }

    // https://stackoverflow.com/questions/19985286/convert-content-uri-to-actual-path-in-android-4-4

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}