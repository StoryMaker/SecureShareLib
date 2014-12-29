
package io.scal.secureshareui.lib;

import java.security.SecureRandom;
import java.util.Random;

import info.guardianproject.onionkit.ui.OrbotHelper;

import android.app.Activity;
import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;

public class Util {

    // netcipher
    public static final String ORBOT_HOST = "127.0.0.1";
    public static final int ORBOT_HTTP_PORT = 8118;
    public static final int ORBOT_SOCKS_PORT = 9050;

    public static boolean isOrbotInstalledAndRunning(Context mContext) {
        OrbotHelper orbotHelper = new OrbotHelper(mContext);
        return (orbotHelper.isOrbotInstalled() && orbotHelper.isOrbotRunning());
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
