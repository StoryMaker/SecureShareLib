package io.scal.secureshareui.soundcloud;

import org.apache.http.HttpHost;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * The environment to operate against.
 * Use SANDBOX for testing your app, and LIVE for production applications.
 */
public enum Env {
    /** The main production site, http://soundcloud.com */
    LIVE("api.soundcloud.com", "soundcloud.com"),
    /** For testing, http://sandbox-soundcloud.com */
    @Deprecated
    SANDBOX("api.sandbox-soundcloud.com", "sandbox-soundcloud.com");

    public final HttpHost resourceHost, sslResourceHost, authResourceHost, sslAuthResourceHost;

    /**
     * @param resourceHost          the resource host
     * @param authResourceHost      the authentication resource host
     */
    Env(String resourceHost, String authResourceHost) {
        this.resourceHost = new HttpHost(resourceHost, 80, "http");
        sslResourceHost = new HttpHost(resourceHost, 443, "https");

        this.authResourceHost = new HttpHost(authResourceHost, 80, "http");
        sslAuthResourceHost = new HttpHost(authResourceHost, 443, "https");
    }

    public HttpHost getResourceHost(boolean secure) {
        return secure ? sslResourceHost : resourceHost;
    }

    public HttpHost getAuthResourceHost(boolean secure) {
        return secure ? sslAuthResourceHost : authResourceHost;
    }

    public URI getResourceURI(boolean secure) {
        return hostToUri(getResourceHost(secure));
    }

    public URI getAuthResourceURI(boolean secure) {
        return hostToUri(getAuthResourceHost(secure));
    }

    public boolean isApiHost(HttpHost host) {
        return ("http".equals(host.getSchemeName()) ||
               "https".equals(host.getSchemeName())) &&
                resourceHost.getHostName().equals(host.getHostName());
    }

    private static URI hostToUri(HttpHost host) {
        try {
            return new URI(host.getSchemeName(), host.getHostName(), null, null);
        } catch (URISyntaxException ignored) {
            throw new RuntimeException();
        }
    }
}
