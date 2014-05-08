package io.scal.secureshareui.lib;

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
	
}
