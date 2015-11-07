
package io.scal.secureshareui.model;

import timber.log.Timber;

import android.content.Context;
import android.content.SharedPreferences;

import io.scal.secureshareui.controller.ArchiveSiteController;
import io.scal.secureshareui.controller.FacebookSiteController;
import io.scal.secureshareui.controller.FlickrSiteController;
import io.scal.secureshareui.controller.SSHSiteController;
import io.scal.secureshareui.controller.SoundCloudSiteController;
import io.scal.secureshareui.controller.YoutubeSiteController;
import io.scal.secureshareui.controller.ZTSiteController;
import io.scal.secureshareui.lib.Util;

public class Account {

    private int id;
    private String name;
    private String site;
    private String userName;
    private String credentials;
    private String data;
    private boolean isConnected;
    private boolean areCredentialsValid;
    public static final String[] CONTROLLER_SITE_NAMES = {
            ArchiveSiteController.SITE_NAME,
            FacebookSiteController.SITE_NAME,
            YoutubeSiteController.SITE_NAME,
            SoundCloudSiteController.SITE_NAME,
            FlickrSiteController.SITE_NAME,
            SSHSiteController.SITE_NAME
    };
    public static final String[] CONTROLLER_SITE_KEYS = {
	        ArchiveSiteController.SITE_KEY,
	        FacebookSiteController.SITE_KEY,
            YoutubeSiteController.SITE_KEY,
            SoundCloudSiteController.SITE_KEY,
            FlickrSiteController.SITE_KEY,
            SSHSiteController.SITE_KEY
    };

    public Account(int id, String name, String site, String userName, String credentials, String data, boolean isConnected, boolean areCredentialsValid) {
        this.id = id;
        this.setName(name);
        this.setSite(site);
        this.setUserName(userName);
        this.setCredentials(credentials);
        this.setData(data);
        this.setIsConnected(isConnected);
        this.setAreCredentialsValid(areCredentialsValid);
    }

    // FIXME move this into a clearly named static method, this is just confusing
    public Account(Context context, String prefsName) {
        if (Util.isEmpty(prefsName)) {
            prefsName = "secureshare_auth";
        }

        SharedPreferences settings = context.getSharedPreferences(prefsName, 0);

        id = settings.getInt("id", 0);
        name = settings.getString("name", null);
        credentials = settings.getString("credentials", null);
        isConnected = settings.getBoolean("is_connected", false);
        data = settings.getString("data", null);
        userName = settings.getString("user_name", null);
    }

    public void saveToSharedPrefs(Context context, String prefsName) {
        if (Util.isEmpty(prefsName)) {
            prefsName = "secureshare_auth";
        }
        SharedPreferences settings = context.getSharedPreferences(prefsName, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("id", id);
        editor.putString("name", name);
        editor.putString("credentials", credentials);
        editor.putBoolean("is_connected", isConnected);
        editor.putString("data", data);
        editor.putString("user_name", userName);
        editor.commit();
    }

    public static void clearSharedPreferences(Context context, String prefsName) {
        if (Util.isEmpty(prefsName)) {
            prefsName = "secureshare_auth";
        }

        SharedPreferences settings = context.getSharedPreferences(prefsName, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean getIsConnected() {
        return isConnected;
    }

    public void setIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public boolean getAreCredentialsValid() {
        return areCredentialsValid;
    }

    public void setAreCredentialsValid(boolean areCredentialsValid) {
        this.areCredentialsValid = areCredentialsValid;
    }
}
