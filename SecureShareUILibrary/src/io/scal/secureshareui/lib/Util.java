
package io.scal.secureshareui.lib;

import java.security.SecureRandom;
import java.util.Random;

import info.guardianproject.onionkit.ui.OrbotHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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

        if(!getOrbotHelper(context).isOrbotInstalled()) {
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
}
