package io.scal.secureshareui.soundcloud;

import timber.log.Timber;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.net.URI;

/**
 * Interface with SoundCloud, using OAuth2.
 *
 * This is the interface, for the implementation see ApiWrapper.
 * @see ApiWrapper
 */
public interface CloudAPI {
    // OAuth2 parameters
    String GRANT_TYPE       = "grant_type";
    String CLIENT_ID        = "client_id";
    String CLIENT_SECRET    = "client_secret";
    String USERNAME         = "username";
    String REDIRECT_URI     = "redirect_uri";
    String CODE             = "code";
    String RESPONSE_TYPE    = "response_type";
    String SCOPE            = "scope";
    String DISPLAY          = "display";
    String STATE            = "state";

    // standard oauth2 grant types
    String PASSWORD           = "password";
    String AUTHORIZATION_CODE = "authorization_code";
    String REFRESH_TOKEN      = "refresh_token";
    String CLIENT_CREDENTIALS = "client_credentials";

    // custom
    String OAUTH1_TOKEN_GRANT_TYPE  = "oauth1_token"; // soundcloud
    String FACEBOOK_GRANT_TYPE      = "urn:soundcloud:oauth2:grant-type:facebook&access_token="; // oauth2 extension
    String GOOGLE_PLUS_GRANT_TYPE   = "urn:soundcloud:oauth2:grant-type:google_plus&access_token=";

    // other constants
    String REALM              = "SoundCloud";
    String OAUTH_SCHEME       = "oauth";
    String VERSION            = "1.3.1";
    String USER_AGENT         = "SoundCloud Java Wrapper ("+VERSION+")";
    String POPUP              = "popup";

    /**
     * Request a token using <a href="http://tools.ietf.org/html/draft-ietf-oauth-v2-10#section-4.1.2">
     * Resource Owner Password Credentials</a>.
     *
     * @param username SoundCloud username
     * @param password SoundCloud password
     * @param scopes   the desired scope(s), or empty for default scope
     * @return a valid token
     * @throws com.soundcloud.api.CloudAPI.InvalidTokenException
     *                     invalid token
     * @throws IOException In case of network/server errors
     */
    Token login(String username, String password, String... scopes) throws IOException;


    /**
     * Request a token using <a href="http://tools.ietf.org/html/draft-ietf-oauth-v2-10#section-4.1.1">
     * Authorization Code</a>, requesting a default scope.
     *
     * @param code the authorization code
     * @param scopes   the desired scope(s), or empty for default scope
     * @return a valid token
     * @throws com.soundcloud.api.CloudAPI.InvalidTokenException invalid token
     * @throws IOException In case of network/server errors
     */
    Token authorizationCode(String code, String... scopes) throws IOException;


    /**
     * Request a "signup" token using <a href="http://tools.ietf.org/html/draft-ietf-oauth-v2-15#section-4.4">
     * Client Credentials</a>.
     *
     * Note that this token is <b>not</b> set as the current token in the wrapper - it should only be used
     * for one request (typically the signup / user creation request).
     * Also note that not all apps are allowed to request this token type (the wrapper throws
     * InvalidTokenException in this case).
     *
     * @param scopes   the desired scope(s), or empty for default scope
     * @return a valid token
     * @throws IOException IO/Error
     * @throws com.soundcloud.api.CloudAPI.InvalidTokenException if requested scope is not available
     */
    Token clientCredentials(String... scopes) throws IOException;


    /**
     * Request a token using an <a href="http://tools.ietf.org/html/draft-ietf-oauth-v2-22#section-4.5">
     * extension grant type</a>.
     * @param grantType
     * @param scopes
     * @return
     * @throws IOException
     */
    Token extensionGrantType(String grantType, String... scopes) throws IOException;

    /**
     * Tries to refresh the currently used access token with the refresh token.
     * If successful the API wrapper will have the new token set already.
     * @return a valid token
     * @throws IOException in case of network problems
     * @throws com.soundcloud.api.CloudAPI.InvalidTokenException invalid token
     * @throws IllegalStateException if no refresh token present
     */
    Token refreshToken() throws IOException;

    /**
     * This method should be called when the token was found to be invalid.
     * Also replaces the current token, if there is one available.
     * @return an alternative token, or null if none available
     *         (which indicates that a refresh could be tried)
     */
    Token invalidateToken();

    /**
       * @param request resource to HEAD
       * @return the HTTP response
       * @throws IOException IO/Error
       */
    HttpResponse head(Request request) throws IOException;

    /**
     * @param request resource to GET
     * @return the HTTP response
     * @throws IOException IO/Error
     */
    HttpResponse get(Request request) throws IOException;

    /**
     * @param request resource to POST
     * @return the HTTP response
     * @throws IOException IO/Error
     */
    HttpResponse post(Request request) throws IOException;

    /**
     * @param request resource to PUT
     * @return the HTTP response
     * @throws IOException IO/Error
     */
    HttpResponse put(Request request) throws IOException;

    /**
     * @param request resource to DELETE
     * @return the HTTP response
     * @throws IOException IO/Error
     */
    HttpResponse delete(Request request) throws IOException;

    /**
     * @return the used httpclient
     */
    HttpClient getHttpClient();

    /**
     * Generic execute method, with added workarounds for various HTTPClient bugs.
     *
     * @param target the target host (can be null)
     * @param request the request
     * @return the HTTP response
     * @throws IOException network errors
     * @throws BrokenHttpClientException in case of HTTPClient framework bugs
     */
    HttpResponse safeExecute(HttpHost target, HttpUriRequest request) throws IOException;

    /**
     * Resolve the given SoundCloud URI
     *
     * @param uri SoundCloud model URI, e.g. http://soundcloud.com/bob
     * @return the id
     * @throws IOException network errors
     * @throws ResolverException if object could not be resolved
     */
    long resolve(String uri) throws IOException;

    /**
     * Resolve the given SoundCloud stream URI
     *
     * @param uri SoundCloud stream URI, e.g. https://api.soundcloud.com/tracks/25272620/stream
     * @param skipLogging skip logging the play of this track (client needs
     *        {@link com.soundcloud.api.Token#SCOPE_PLAYCOUNT})
     * @return the resolved stream
     * @throws IOException network errors
     * @throws com.soundcloud.api.CloudAPI.ResolverException resolver error (invalid status etc)
     */
    Stream resolveStreamUrl(String uri, boolean skipLogging) throws IOException;

    /** @return the current token */
    Token getToken();

    /** @param token the token to be used */
    void setToken(Token token);

    /**
     * Registers a listener. The listener will be informed when an access token was found
     * to be invalid, and when the token had to be refreshed.
     * @param listener token listener
     */
    void setTokenListener(TokenListener listener);

    /**
     * Request login via authorization code
     * After login, control will go to the redirect URI (wrapper specific), with
     * one of the following query parameters appended:
     * <ul>
     * <li><code>code</code> in case of success, this will contain the code used for the
     *     <code>authorizationCode</code> call to obtain the access token.
     * <li><code>error</code> in case of failure, this contains an error code (most likely
     * <code>access_denied</code>).
     * </ul>
     * @param  options auth endpoint to use (leave out for default), requested scope (leave out for default), display ('popup' for mobile optimized screen) and state.
     * @return the URI to open in a browser/WebView etc.
     * @see CloudAPI#authorizationCode(String, String...)
     */
    URI authorizationCodeUrl(String... options);

    /**
     * Changes the default content type sent in the "Accept" header.
     * If you don't set this it defaults to "application/json".
     *
     * @param contentType the request mime type.
     */
    void setDefaultContentType(String contentType);
    void setDefaultAcceptEncoding(String encoding);

    /**
     * Interested in changes to the current token.
     */
    interface TokenListener {
        /**
         * Called when token was found to be invalid
         * @param token the invalid token
         * @return a cached token if available, or null
         */
        Token onTokenInvalid(Token token);

        /**
         * Called when the token got successfully refreshed
         * @param token      the refreshed token
         */
        void onTokenRefreshed(Token token);
    }

    /**
     * Thrown when token is not valid.
     */
    class InvalidTokenException extends IOException {
        private static final long serialVersionUID = 1954919760451539868L;

        /**
         * @param code the HTTP error code
         * @param status the HTTP status, or other error message
         */
        public InvalidTokenException(int code, String status) {
            super("HTTP error:" + code + " (" + status + ")");
        }
    }

    /**
     * Thrown if resolving the audio stream of a SoundCloud sound fails.
     */
    class ResolverException extends ApiResponseException {

        public ResolverException(String s, HttpResponse resp) {
            super(resp, s);
        }

        public ResolverException(Throwable throwable, HttpResponse response) {
            super(throwable, response);
        }
    }

    /**
     * Thrown if the service API responds in error. The HTTP status code can be obtained via {@link #getStatusCode()}.
     */
    class ApiResponseException extends IOException {
        private static final long serialVersionUID = -2990651725862868387L;

        public final HttpResponse response;

        public ApiResponseException(HttpResponse resp, String error) {
            super(resp.getStatusLine().getStatusCode() + ": [" + resp.getStatusLine().getReasonPhrase() + "] "
                    + (error != null ? error : ""));
            this.response = resp;
        }

        public ApiResponseException(Throwable throwable, HttpResponse response) {
            super(throwable == null ? null : throwable.toString());
            initCause(throwable);
            this.response = response;
        }

        public int getStatusCode() {
            return response.getStatusLine().getStatusCode();
        }

        @Override
        public String getMessage() {
            return super.getMessage()+" "+(response != null ? response.getStatusLine() : "");
        }
    }

    class BrokenHttpClientException extends IOException {
        private static final long serialVersionUID = -4764332412926419313L;

        BrokenHttpClientException(Throwable throwable) {
            super(throwable == null ? null : throwable.toString());
            initCause(throwable);
        }
    }
}
