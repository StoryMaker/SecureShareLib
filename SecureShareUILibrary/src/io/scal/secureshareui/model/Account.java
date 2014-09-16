
package io.scal.secureshareui.model;

import io.scal.secureshareui.controller.ArchiveSiteController;
import io.scal.secureshareui.controller.FacebookSiteController;
import io.scal.secureshareui.controller.FlickrSiteController;
import io.scal.secureshareui.controller.SSHSiteController;
import io.scal.secureshareui.controller.SoundCloudSiteController;
import io.scal.secureshareui.controller.YoutubeSiteController;

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
