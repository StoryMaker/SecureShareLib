package io.scal.secureshareui.soundcloud;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRequestDirector;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;

/**
 * Interface with SoundCloud, using OAuth2.
 * This API wrapper makes a few assumptions - namely:
 * <ul>
 * <li>Server responses are always requested in JSON format</li>
 * <li>Refresh-token handling is transparent to the client application (you should not need to
 *     call <code>refreshToken()</code> manually)
 * </li>
 * <li>You use <a href="http://hc.apache.org/httpcomponents-client-ga/">Apache HttpClient</a></li>
 * </ul>
 * Example usage:
 * <code>
 *     <pre>
 * ApiWrapper wrapper = new ApiWrapper("client_id", "client_secret", null, null, Env.SANDBOX);
 * wrapper.login("login", "password");
 * HttpResponse response = wrapper.get(Request.to("/tracks"));
 *      </pre>
 * </code>
 * @see <a href="http://developers.soundcloud.com/docs">Using the SoundCloud API</a>
 */
public class ApiWrapper implements CloudAPI, Serializable {
    public static final String DEFAULT_CONTENT_TYPE = "application/json";

    private static final long serialVersionUID = 3662083416905771921L;
    private static final Token EMPTY_TOKEN = new Token(null, null);

    /** The current environment, only live possible for now */
    public final Env env = Env.LIVE;

    private Token mToken;
    private final String mClientId, mClientSecret;
    private final URI mRedirectUri;
    transient private HttpClient httpClient;
    transient private TokenListener listener;

    private String mDefaultContentType;
    private String mDefaultAcceptEncoding;

    public static final int BUFFER_SIZE = 8192;
    /** Connection timeout */
    public static final int TIMEOUT = 20 * 1000;
    /** Keepalive timeout */
    public static final long KEEPALIVE_TIMEOUT = 20 * 1000;
    /* maximum number of connections allowed */
    public static final int MAX_TOTAL_CONNECTIONS = 10;
    /* spam response code from API */
    public static final int STATUS_CODE_SPAM_WARNING = 429;

    /** debug request details to stderr */
    public boolean debugRequests;


    /**
     * Constructs a new ApiWrapper instance.
     *
     * @param clientId     the application client id
     * @param clientSecret the application client secret
     * @param redirectUri  the registered redirect url, or null
     * @param token        an valid token, or null if not known
     * @see <a href="http://developers.soundcloud.com/docs#authentication">API authentication documentation</a>
     */
    public ApiWrapper(String clientId,
                      String clientSecret,
                      URI redirectUri,
                      Token token) {
        mClientId = clientId;
        mClientSecret = clientSecret;
        mRedirectUri = redirectUri;
        mToken = token == null ? EMPTY_TOKEN : token;
    }

    @Override public Token login(String username, String password, String... scopes) throws IOException {
        if (username == null || password == null) {
            throw new IllegalArgumentException("username or password is null");
        }
        final Request request = addScope(Request.to(Endpoints.TOKEN).with(
                GRANT_TYPE, PASSWORD,
                CLIENT_ID, mClientId,
                CLIENT_SECRET, mClientSecret,
                USERNAME, username,
                PASSWORD, password), scopes);
        mToken = requestToken(request);
        return mToken;
    }



    @Override public Token authorizationCode(String code, String... scopes) throws IOException {
        if (code == null) {
            throw new IllegalArgumentException("code is null");
        }
        final Request request = addScope(Request.to(Endpoints.TOKEN).with(
                GRANT_TYPE, AUTHORIZATION_CODE,
                CLIENT_ID, mClientId,
                CLIENT_SECRET, mClientSecret,
                REDIRECT_URI, mRedirectUri,
                CODE, code), scopes);
        mToken = requestToken(request);
        return mToken;
    }


    @Override public Token clientCredentials(String... scopes) throws IOException {
        final Request req = addScope(Request.to(Endpoints.TOKEN).with(
                GRANT_TYPE, CLIENT_CREDENTIALS,
                CLIENT_ID,  mClientId,
                CLIENT_SECRET, mClientSecret), scopes);

        final Token token = requestToken(req);
        if (scopes != null) {
            for (String scope : scopes) {
                if (!token.scoped(scope)) {
                    throw new InvalidTokenException(-1, "Could not obtain requested scope '"+scope+"' (got: '" +
                    token.scope + "')");
                }
            }
        }
        return token;
    }

    @Override
    public Token extensionGrantType(String grantType, String... scopes) throws IOException {
        final Request req = addScope(Request.to(Endpoints.TOKEN).with(
                GRANT_TYPE, grantType,
                CLIENT_ID,  mClientId,
                CLIENT_SECRET, mClientSecret), scopes);

        mToken = requestToken(req);
        return mToken;
    }

    @Override public Token refreshToken() throws IOException {
        if (mToken == null || mToken.refresh == null) throw new IllegalStateException("no refresh token available");
        mToken = requestToken(Request.to(Endpoints.TOKEN).with(
                GRANT_TYPE, REFRESH_TOKEN,
                CLIENT_ID, mClientId,
                CLIENT_SECRET, mClientSecret,
                REFRESH_TOKEN, mToken.refresh));
        return mToken;
    }

    @Override public Token invalidateToken() {
        if (mToken != null) {
            Token alternative = listener == null ? null : listener.onTokenInvalid(mToken);
            mToken.invalidate();
            if (alternative != null) {
                mToken = alternative;
                return mToken;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override public URI authorizationCodeUrl(String... options) {
        final Request req = Request.to(options.length == 0 ? Endpoints.CONNECT : options[0]).with(
                REDIRECT_URI, mRedirectUri,
                CLIENT_ID, mClientId,
                RESPONSE_TYPE, CODE);
        if (options.length > 1) req.add(SCOPE, options[1]);
        if (options.length > 2) req.add(DISPLAY, options[2]);
        if (options.length > 3) req.add(STATE, options[3]);
        return getURI(req, false, true);
    }

    /**
     * Constructs URI path for a given resource.
     * @param request      the resource to access
     * @param api          api or web
     * @param secure       whether to use SSL or not
     * @return a valid URI
     */
    public URI getURI(Request request, boolean api, boolean secure) {
        final URI uri = api ? env.getResourceURI(secure) : env.getAuthResourceURI(secure);
        return uri.resolve(request.toUrl());
    }

    /**
     * User-Agent to identify ourselves with - defaults to USER_AGENT
     * @return the agent to use
     * @see CloudAPI#USER_AGENT
     */
    public String getUserAgent() {
        return USER_AGENT;
    }

    /**
     * Request an OAuth2 token from SoundCloud
     * @param  request the token request
     * @return the token
     * @throws java.io.IOException network error
     * @throws com.soundcloud.api.CloudAPI.InvalidTokenException unauthorized
     * @throws com.soundcloud.api.CloudAPI.ApiResponseException http error
     */
    protected Token requestToken(Request request) throws IOException {
        HttpResponse response = safeExecute(env.sslResourceHost, request.buildRequest(HttpPost.class));
        final int status = response.getStatusLine().getStatusCode();

        String error;
        try {
            if (status == HttpStatus.SC_OK) {
                final Token token = new Token(Http.getJSON(response));
                if (listener != null) listener.onTokenRefreshed(token);
                return token;
            } else {
                error = Http.getJSON(response).getString("error");
            }
        } catch (IOException ignored) {
            error = ignored.getMessage();
        } catch (JSONException ignored) {
            error = ignored.getMessage();
        }
        throw status == HttpStatus.SC_UNAUTHORIZED ?
                new InvalidTokenException(status, error) :
                new ApiResponseException(response, error);
    }

    /**
     * @return the default HttpParams
     * @see <a href="http://developer.android.com/reference/android/net/http/AndroidHttpClient.html#newInstance(java.lang.String, android.content.Context)">
     *      android.net.http.AndroidHttpClient#newInstance(String, Context)</a>
     */
    protected HttpParams getParams() {
        final HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpConnectionParams.setSocketBufferSize(params, BUFFER_SIZE);
        ConnManagerParams.setMaxTotalConnections(params, MAX_TOTAL_CONNECTIONS);

        // Turn off stale checking.  Our connections break all the time anyway,
        // and it's not worth it to pay the penalty of checking every time.
        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        // fix contributed by Bjorn Roche XXX check if still needed
        params.setBooleanParameter("http.protocol.expect-continue", false);
        params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRoute() {
            @Override
            public int getMaxForRoute(HttpRoute httpRoute) {
                if (env.isApiHost(httpRoute.getTargetHost())) {
                    // there will be a lot of concurrent request to the API host
                    return MAX_TOTAL_CONNECTIONS;
                } else {
                    return ConnPerRouteBean.DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
                }
            }
        });
        // apply system proxy settings
        final String proxyHost = System.getProperty("http.proxyHost");
        final String proxyPort = System.getProperty("http.proxyPort");
        if (proxyHost != null) {
            int port = 80;
            try {
                port = Integer.parseInt(proxyPort);
            } catch (NumberFormatException ignored) {
            }
            params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxyHost, port));
        }
        return params;
    }

    /**
     * @param proxy the proxy to use for the wrapper, or null to clear the current one.
     */
    public void setProxy(URI proxy) {
        final HttpHost host;
        if (proxy != null) {
            Scheme scheme = getHttpClient()
                .getConnectionManager()
                .getSchemeRegistry()
                .getScheme(proxy.getScheme());

            host = new HttpHost(proxy.getHost(), scheme.resolvePort(proxy.getPort()), scheme.getName());
        } else {
            host = null;
        }
        getHttpClient().getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, host);
    }


    public URI getProxy() {
        Object proxy = getHttpClient().getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY);
        if (proxy instanceof HttpHost) {
            return URI.create(((HttpHost)proxy).toURI());
        } else {
            return null;
        }
    }

    public boolean isProxySet() {
        return getProxy() != null;
    }

    /**
     * @return SocketFactory used by the underlying HttpClient
     */
    protected SocketFactory getSocketFactory() {
        return PlainSocketFactory.getSocketFactory();
    }

    /**
     * @return SSL SocketFactory used by the underlying HttpClient
     */
    protected SSLSocketFactory getSSLSocketFactory() {
        return SSLSocketFactory.getSocketFactory();
    }


    /** @return The HttpClient instance used to make the calls */
    public HttpClient getHttpClient() {
        if (httpClient == null) {
            final HttpParams params = getParams();
            HttpClientParams.setRedirecting(params, false);
            HttpProtocolParams.setUserAgent(params, getUserAgent());

            final SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", getSocketFactory(), 80));
            final SSLSocketFactory sslFactory = getSSLSocketFactory();
            registry.register(new Scheme("https", sslFactory, 443));
            httpClient = new DefaultHttpClient(
                    new ThreadSafeClientConnManager(params, registry),
                    params) {
                {
                    setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
                        @Override
                        public long getKeepAliveDuration(HttpResponse httpResponse, HttpContext httpContext) {
                            return KEEPALIVE_TIMEOUT;
                        }
                    });

                    getCredentialsProvider().setCredentials(
                        new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, CloudAPI.REALM, OAUTH_SCHEME),
                        OAuth2Scheme.EmptyCredentials.INSTANCE);

                    getAuthSchemes().register(CloudAPI.OAUTH_SCHEME, new OAuth2Scheme.Factory(ApiWrapper.this));

                    addResponseInterceptor(new HttpResponseInterceptor() {
                        @Override
                        public void process(HttpResponse response, HttpContext context)
                                throws HttpException, IOException {
                            if (response == null || response.getEntity() == null) return;

                            HttpEntity entity = response.getEntity();
                            Header header = entity.getContentEncoding();
                            if (header != null) {
                                for (HeaderElement codec : header.getElements()) {
                                    if (codec.getName().equalsIgnoreCase("gzip")) {
                                        response.setEntity(new GzipDecompressingEntity(entity));
                                        break;
                                    }
                                }
                            }
                        }
                    });
                }

                @Override protected HttpContext createHttpContext() {
                    HttpContext ctxt = super.createHttpContext();
                    ctxt.setAttribute(ClientContext.AUTH_SCHEME_PREF,
                            Arrays.asList(CloudAPI.OAUTH_SCHEME, "digest", "basic"));
                    return ctxt;
                }

                @Override protected BasicHttpProcessor createHttpProcessor() {
                    BasicHttpProcessor processor = super.createHttpProcessor();
                    processor.addInterceptor(new OAuth2HttpRequestInterceptor());
                    return processor;
                }

                // for testability only
                @Override protected RequestDirector createClientRequestDirector(HttpRequestExecutor requestExec,
                                                                      ClientConnectionManager conman,
                                                                      ConnectionReuseStrategy reustrat,
                                                                      ConnectionKeepAliveStrategy kastrat,
                                                                      HttpRoutePlanner rouplan,
                                                                      HttpProcessor httpProcessor,
                                                                      HttpRequestRetryHandler retryHandler,
                                                                      RedirectHandler redirectHandler,
                                                                      AuthenticationHandler targetAuthHandler,
                                                                      AuthenticationHandler proxyAuthHandler,
                                                                      UserTokenHandler stateHandler,
                                                                      HttpParams params) {
                    return getRequestDirector(requestExec, conman, reustrat, kastrat, rouplan, httpProcessor, retryHandler,
                            redirectHandler, targetAuthHandler, proxyAuthHandler, stateHandler, params);
                }
            };
        }
        return httpClient;
    }

    @Override
    public long resolve(String url) throws IOException {
        HttpResponse resp = get(Request.to(Endpoints.RESOLVE).with("url", url));
        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
            Header location = resp.getFirstHeader("Location");
            if (location != null && location.getValue() != null) {
                final String path = URI.create(location.getValue()).getPath();
                if (path != null && path.contains("/")) {
                    try {
                      final String id = path.substring(path.lastIndexOf("/") + 1);
                      return Integer.parseInt(id);
                    } catch (NumberFormatException e) {
                        throw new ResolverException(e, resp);
                    }
                } else {
                    throw new ResolverException("Invalid string:"+path, resp);
                }
            } else {
                throw new ResolverException("No location header", resp);
            }
        } else {
            throw new ResolverException("Invalid status code", resp);
        }
    }

    @Override
    public Stream resolveStreamUrl(final String url, boolean skipLogging) throws IOException {
        HttpResponse resp = safeExecute(null, addHeaders(Request.to(url).buildRequest(HttpHead.class)));
        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
            Header location = resp.getFirstHeader("Location");
            if (location != null && location.getValue() != null) {
                final String headRedirect = location.getValue();
                resp = safeExecute(null, new HttpHead(headRedirect));
                if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    Stream stream = new Stream(url, headRedirect, resp);
                    // need to do another GET request to have a URL ready for client usage
                    Request req = Request.to(url);
                    if (skipLogging) {
                        // skip logging
                        req.with("skip_logging", "1");
                    }
                    resp = safeExecute(null, addHeaders(Request.to(url).buildRequest(HttpGet.class)));
                    if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                        return stream.withNewStreamUrl(resp.getFirstHeader("Location").getValue());
                    } else {
                        throw new ResolverException("Unexpected response code", resp);
                    }
                } else {
                    throw new ResolverException("Unexpected response code", resp);
                }
            } else {
                throw new ResolverException("Location header not set", resp);
            }
        } else {
            throw new ResolverException("Unexpected response code", resp);
        }
    }

    @Override
    public HttpResponse head(Request request) throws IOException {
        return execute(request, HttpHead.class);
    }

    @Override public HttpResponse get(Request request) throws IOException {
        return execute(request, HttpGet.class);
    }

    @Override public HttpResponse put(Request request) throws IOException {
        return execute(request, HttpPut.class);
    }

    @Override public HttpResponse post(Request request) throws IOException {
        return execute(request, HttpPost.class);
    }

    @Override public HttpResponse delete(Request request) throws IOException {
        return execute(request, HttpDelete.class);
    }

    @Override public Token getToken() {
        return mToken;
    }

    @Override public void setToken(Token newToken) {
        mToken = newToken == null ? EMPTY_TOKEN : newToken;
    }

    @Override
    public synchronized void setTokenListener(TokenListener listener) {
        this.listener = listener;
    }

    /**
     * Execute an API request, adds the necessary headers.
     * @param request the HTTP request
     * @return the HTTP response
     * @throws java.io.IOException network error etc.
     */
    public HttpResponse execute(HttpUriRequest request) throws IOException {
        return safeExecute(env.sslResourceHost, addHeaders(request));
    }

    public HttpResponse safeExecute(HttpHost target, HttpUriRequest request) throws IOException {
        if (target == null) {
            target = determineTarget(request);
        }

        try {
            return getHttpClient().execute(target, request);
        } catch (NullPointerException e) {
            // this is a workaround for a broken httpclient version,
            // cf. http://code.google.com/p/android/issues/detail?id=5255
            // NPE in DefaultRequestDirector.java:456
            if (!request.isAborted() && request.getParams().isParameterFalse("npe-retried")) {
                request.getParams().setBooleanParameter("npe-retried", true);
                return safeExecute(target, request);
            } else {
                request.abort();
                throw new BrokenHttpClientException(e);
            }
        } catch (IllegalArgumentException e) {
            // more brokenness
            // cf. http://code.google.com/p/android/issues/detail?id=2690
            request.abort();
            throw new BrokenHttpClientException(e);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Caused by: java.lang.ArrayIndexOutOfBoundsException: length=7; index=-9
            // org.apache.harmony.security.asn1.DerInputStream.readBitString(DerInputStream.java:72))
            // org.apache.harmony.security.asn1.ASN1BitString.decode(ASN1BitString.java:64)
            // ...
            // org.apache.http.conn.ssl.SSLSocketFactory.createSocket(SSLSocketFactory.java:375)
            request.abort();
            throw new BrokenHttpClientException(e);
        }
    }

    protected HttpResponse execute(Request req, Class<? extends HttpRequestBase> reqType) throws IOException {
        Request defaults = ApiWrapper.defaultParams.get();
        if (defaults != null && !defaults.getParams().isEmpty()) {
            // copy + merge in default parameters
            for (NameValuePair nvp : defaults) {
                req = new Request(req);
                req.add(nvp.getName(), nvp.getValue());
            }
        }
        logRequest(reqType, req);
        return execute(addClientIdIfNecessary(req).buildRequest(reqType));
    }

    protected Request addClientIdIfNecessary(Request req) {
        return req.getParams().containsKey(CLIENT_ID) ? req : new Request(req).add(CLIENT_ID, mClientId);
    }

    protected void logRequest( Class<? extends HttpRequestBase> reqType, Request request) {
        if (debugRequests) System.err.println(reqType.getSimpleName()+" "+request);
    }

    protected HttpHost determineTarget(HttpUriRequest request) {
        // A null target may be acceptable if there is a default target.
        // Otherwise, the null target is detected in the director.
        URI requestURI = request.getURI();
        if (requestURI.isAbsolute()) {
            return new HttpHost(
                    requestURI.getHost(),
                    requestURI.getPort(),
                    requestURI.getScheme());
        } else {
            return null;
        }
    }

    /**
     * serialize the wrapper to a File
     * @param f target
     * @throws java.io.IOException IO problems
     */
    public void toFile(File f) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
        oos.writeObject(this);
        oos.close();
    }


    public String getDefaultContentType() {
        return (mDefaultContentType == null) ? DEFAULT_CONTENT_TYPE : mDefaultContentType;
    }

    public void setDefaultContentType(String contentType) {
        mDefaultContentType = contentType;
    }

    public String getDefaultAcceptEncoding() {
        return mDefaultAcceptEncoding;
    }

    public void setDefaultAcceptEncoding(String encoding) {
        mDefaultAcceptEncoding = encoding;
    }


    /* package */ static Request addScope(Request request, String[] scopes) {
        if (scopes != null && scopes.length > 0) {
            StringBuilder scope = new StringBuilder();
            for (int i=0; i<scopes.length; i++) {
                scope.append(scopes[i]);
                if (i < scopes.length-1) scope.append(" ");
            }
            request.add(SCOPE, scope.toString());
        }
        return request;
    }

    /**
     * Read wrapper from a file
     * @param f  the file
     * @return   the wrapper
     * @throws IOException IO problems
     * @throws ClassNotFoundException class not found
     */
    public static ApiWrapper fromFile(File f) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
        try {
            return (ApiWrapper) ois.readObject();
        } finally {
            ois.close();
        }
    }

    /** Creates an OAuth2 header for the given token */
    public static Header createOAuthHeader(Token token) {
        return new BasicHeader(AUTH.WWW_AUTH_RESP, "OAuth " +
                (token == null || !token.valid() ? "invalidated" : token.access));
    }

    /** Adds an OAuth2 header to a given request */
    protected HttpUriRequest addAuthHeader(HttpUriRequest request) {
        if (!request.containsHeader(AUTH.WWW_AUTH_RESP)) {
            if (mToken != EMPTY_TOKEN) {
                request.addHeader(createOAuthHeader(mToken));
            }
        }
        return request;
    }

    /** Forces JSON */
    protected HttpUriRequest addAcceptHeader(HttpUriRequest request) {
        if (!request.containsHeader("Accept")) {
            request.addHeader("Accept", getDefaultContentType());
        }
        return request;
    }

    /** Adds all required headers to the request */
    protected HttpUriRequest addHeaders(HttpUriRequest req) {
        return addAcceptHeader(addAuthHeader(addEncodingHeader(req)));
    }

    protected HttpUriRequest addEncodingHeader(HttpUriRequest req) {
        if (getDefaultAcceptEncoding() != null) {
            req.addHeader("Accept-Encoding", getDefaultAcceptEncoding());
        }
        return req;
    }

    /** This method mainly exists to make the wrapper more testable. oh, apache's insanity. */
    protected RequestDirector getRequestDirector(HttpRequestExecutor requestExec,
                                                 ClientConnectionManager conman,
                                                 ConnectionReuseStrategy reustrat,
                                                 ConnectionKeepAliveStrategy kastrat,
                                                 HttpRoutePlanner rouplan,
                                                 HttpProcessor httpProcessor,
                                                 HttpRequestRetryHandler retryHandler,
                                                 RedirectHandler redirectHandler,
                                                 AuthenticationHandler targetAuthHandler,
                                                 AuthenticationHandler proxyAuthHandler,
                                                 UserTokenHandler stateHandler,
                                                 HttpParams params
    ) {
        return new DefaultRequestDirector(requestExec, conman, reustrat, kastrat, rouplan,
                httpProcessor, retryHandler, redirectHandler, targetAuthHandler, proxyAuthHandler,
                stateHandler, params);
    }

    private static final ThreadLocal<Request> defaultParams = new ThreadLocal<Request>() {
        @Override protected Request initialValue() {
            return new Request();
        }
    };

    /**
     * Adds a default parameter which will get added to all requests in this thread.
     * Use this method carefully since it might lead to unexpected side-effects.
     * @param name the name of the parameter
     * @param value the value of the parameter.
     */
    public static void setDefaultParameter(String name, String value) {
        defaultParams.get().set(name, value);
    }

    /**
     * Clears the default parameters.
     */
    public static void clearDefaultParameters() {
        defaultParams.remove();
    }
}
