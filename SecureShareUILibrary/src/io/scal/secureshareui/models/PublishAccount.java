package io.scal.secureshareui.models;

public class PublishAccount{
	
	private String Name;
	private String Site;
	private String IconUrl;
	private boolean IsConnected;
	
	public PublishAccount(String name, String site, String iconUrl, boolean isConnected){
		this.Name = name;
		this.Site = site;
		this.IconUrl = iconUrl;
		this.IsConnected = isConnected;
	}
	
	public String getName(){
	     return this.Name;
	}
	public void setName(String name){
	     this.Name = name;
	}
	
	public String getSite(){
	     return this.Site;
	}
	public void setSite(String site){
	     this.Site = site;
	}
	
	public String getIconUrl(){
	     return this.IconUrl;
	}
	public void setId(String iconUrl){
	     this.IconUrl = iconUrl;
	}
	
	public boolean getIsConnected(){
	     return this.IsConnected;
   	}
   	public void setIsConnected(boolean isConnected){
   	     this.IsConnected = isConnected;
   	} 	
}
