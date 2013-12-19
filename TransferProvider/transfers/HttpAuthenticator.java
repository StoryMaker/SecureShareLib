package com.android.providers.transfers;

public class HttpAuthenticator implements IAuthenticator {
	
    public Authenticator() {
    }

    /* currently we only support publically visible downloads so this is always true */
    public boolean isAuthenticated() {
        return true;
    }
            
}
