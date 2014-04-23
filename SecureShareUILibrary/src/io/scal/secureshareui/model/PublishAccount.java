package io.scal.secureshareui.model;

public class PublishAccount {
	
	private String id;
	private String name;
    private String site;
    private String userName;
    private String credentials;
    private boolean isConnected;
    private boolean areCredentialsValid;
	
	public PublishAccount(String id, String name, String site, String userName, String credentials, boolean isConnected, boolean areCredentialsValid){
		this.id = id;
		this.setName(name);
		this.setSite(site);
		this.setUserName(userName);
		this.setCredentials(credentials);
		this.setIsConnected(false);
		this.setAreCredentialsValid(false);
	}

	public String getId() {
		return id;
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
