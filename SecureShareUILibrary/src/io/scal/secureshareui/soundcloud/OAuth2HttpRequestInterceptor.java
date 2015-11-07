package io.scal.secureshareui.soundcloud;

import timber.log.Timber;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

class OAuth2HttpRequestInterceptor implements HttpRequestInterceptor {
    @Override public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        if (request == null) throw new IllegalArgumentException("HTTP request may not be null");
        if (context == null) throw new IllegalArgumentException("HTTP context may not be null");

        if (!request.getRequestLine().getMethod().equalsIgnoreCase("CONNECT")) {
            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
            if (authState != null) {
                AuthScheme authScheme = authState.getAuthScheme();
                if (authScheme != null && !authScheme.isConnectionBased()) {
                    try {
                        request.setHeader(authScheme.authenticate(null, request));
                    } catch (AuthenticationException ignored) {
                        // ignored
                    }
                }
            }
        }
    }
}
