package io.scal.secureshareui.soundcloud;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.impl.cookie.DateUtils;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Class representing a remote audio stream object, including metadata.
 */
public class Stream implements Serializable {
    public static final String AMZ_BITRATE  = "x-amz-meta-bitrate";
    public static final String AMZ_DURATION = "x-amz-meta-duration";
    static final String EXPIRES = "Expires";

    public static final long DEFAULT_URL_LIFETIME = 60 * 1000; // expire after 1 minute
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DateUtils.PATTERN_RFC1123, Locale.US);

    private static final long serialVersionUID = -2054788615389851590L;

    public final String url;
    public final String streamUrl;
    public final String eTag;
    public final long contentLength;
    public final long lastModified;
    public final int duration;
    public final int bitRate;
    public final long expires;

    public Stream(String url, String streamUrl, HttpResponse resp) throws CloudAPI.ResolverException {
        this(url, streamUrl, getHeaderValue(resp, "ETag"),
                getLongHeader(resp, "Content-Length"),
                getDateHeader(resp, "Last-Modified"),
                getIntHeader(resp, AMZ_DURATION),
                getIntHeader(resp, AMZ_BITRATE),
                getExpires(streamUrl));
    }

    public Stream(String url, String streamUrl, String eTag, long contentLength, long lastModified,
                  int duration, int bitRate, long expires) {
        this.url = url;
        this.streamUrl = streamUrl;
        this.eTag = eTag;
        this.contentLength = contentLength;
        this.lastModified = lastModified;
        this.duration = duration;
        this.bitRate = bitRate;
        this.expires = expires;
    }

    public Request streamUrl() {
        return Request.to(streamUrl);
    }

    public Request url() {
        return Request.to(url);
    }

    public Stream withNewStreamUrl(String newStreamUrl) {
        return new Stream(url, newStreamUrl, eTag, contentLength, lastModified, duration, bitRate, getExpires(newStreamUrl));
    }

    public static long getLongHeader(HttpResponse resp, String name) throws CloudAPI.ResolverException {
        try {
            return Long.parseLong(getHeaderValue(resp, name));
        } catch (NumberFormatException e) {
            throw new CloudAPI.ResolverException(e, resp);
        }
    }

    public static int getIntHeader(HttpResponse resp, String name) throws CloudAPI.ResolverException {
        try {
            return Integer.parseInt(getHeaderValue(resp, name));
        } catch (NumberFormatException e) {
            throw new CloudAPI.ResolverException(e, resp);
        }
    }

    public static long getDateHeader(HttpResponse resp, String name) throws CloudAPI.ResolverException {
        try {
            return DATE_FORMAT.parse(getHeaderValue(resp, name)).getTime();
        } catch (ParseException e) {
            throw new CloudAPI.ResolverException(e, resp);
        }
    }

    private static String getHeaderValue(HttpResponse resp, String name) throws CloudAPI.ResolverException {
        Header h = resp.getFirstHeader(name);
        if (h != null && h.getValue() != null) {
            return h.getValue();
        } else {
            throw new CloudAPI.ResolverException("header " + name + " not set", resp);
        }
    }

    private static long getExpires(String resource) {
        String query = resource.substring(Math.min(resource.length(), resource.indexOf("?")+1),
                resource.length());
        for (String s : query.split("&")) {
            String[] kv = s.split("=", 2);
            if (kv != null && kv.length == 2) {
                try {
                    String name = URLDecoder.decode(kv[0], Request.UTF_8);
                    if (EXPIRES.equalsIgnoreCase(name)) {
                        String value = URLDecoder.decode(kv[1], Request.UTF_8);
                        try {
                            return Long.parseLong(value) * 1000L;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                } catch (UnsupportedEncodingException ignored) {}
            }
        }
        return System.currentTimeMillis() + DEFAULT_URL_LIFETIME;
    }

    @Override
    public String toString() {
        return "Stream{" +
                "url='" + url + '\'' +
                ", streamUrl='" + streamUrl + '\'' +
                ", eTag='" + eTag + '\'' +
                ", contentLength=" + contentLength +
                ", lastModified=" + lastModified +
                ", duration=" + duration +
                ", bitRate=" + bitRate +
                ", expires=" + expires +
                '}';
    }
}
