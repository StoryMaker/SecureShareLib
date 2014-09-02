
package io.scal.secureshareui.lib;

import java.security.SecureRandom;
import java.util.Random;

import info.guardianproject.onionkit.ui.OrbotHelper;

import android.content.Context;

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
      private static final String symbols = "ABCDEFGJKLMNPRSTUVWXYZ0123456789"; 

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
}
