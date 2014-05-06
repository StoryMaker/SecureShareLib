package io.scal.secureshareui.soundcloud;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convenience class for constructing HTTP requests.
 *
 * Example:
 * <code>
 *   <pre>
 *  HttpRequest request = Request.to("/tracks")
 *     .with("track[user]", 1234)
 *     .withFile("track[asset_data]", new File("track.mp3")
 *     .buildRequest(HttpPost.class);
 *
 *  httpClient.execute(request);
 *   </pre>
 *  </code>
 */
public class Request implements Iterable<NameValuePair> {
    public static final String UTF_8 = "UTF-8";

    private List<NameValuePair> mParams = new ArrayList<NameValuePair>(); // XXX should probably be lazy
    private Map<String, Attachment> mFiles;

    private HttpEntity mEntity;

    private Token mToken;
    private String mResource;
    private TransferProgressListener listener;
    private String mIfNoneMatch;
    private long[] mRange;

    /** Empty request */
    public Request() {}

    /**
     * @param resource the base resource
     */
    public Request(String resource) {
        if (resource == null) throw new IllegalArgumentException("resource is null");

        // make sure paths start with a slash
        if (!(resource.startsWith("http:") || resource.startsWith("https:"))
             && !resource.startsWith("/")) {
            resource = "/"+resource;
        }

        if (resource.contains("?")) {
            String query = resource.substring(Math.min(resource.length(), resource.indexOf("?")+1),
                    resource.length());
            for (String s : query.split("&")) {
                String[] kv = s.split("=", 2);
                if (kv != null) {
                    try {
                        if (kv.length == 2) {
                            mParams.add(new BasicNameValuePair(
                                    URLDecoder.decode(kv[0], UTF_8),
                                    URLDecoder.decode(kv[1], UTF_8)));
                        } else if (kv.length == 1) {
                            mParams.add(new BasicNameValuePair(URLDecoder.decode(kv[0], UTF_8), null));
                        }
                    } catch (UnsupportedEncodingException ignored) {
                    }
                }
            }
            mResource = resource.substring(0, resource.indexOf("?"));
        } else {
            mResource = resource;
        }
    }

    /**
     * constructs a a request from URI. the hostname+scheme will be ignored
     * @param uri - the uri
     */
    public Request(URI uri) {
        this(uri.getPath() == null ? "/" : uri.getPath() +
            (uri.getQuery() == null ? "" : "?"+uri.getQuery()));
    }

    /**
     * @param request the request to be copied
     */
    public Request(Request request) {
        mResource = request.mResource;
        mToken = request.mToken;
        listener = request.listener;
        mParams = new ArrayList<NameValuePair>(request.mParams);
        mIfNoneMatch = request.mIfNoneMatch;
        mEntity = request.mEntity;
        if (request.mFiles != null) mFiles = new HashMap<String, Attachment>(request.mFiles);
    }

    /**
     * @param resource  the resource to request
     * @param args      optional string expansion arguments (passed to String#format(String, Object...)
     * @throws java.util.IllegalFormatException - If a format string contains an illegal syntax,
     * @return the request
     * @see String#format(String, Object...)
     */
    public static Request to(String resource, Object... args) {
        if (args != null &&
            args.length > 0) {
            resource = String.format(Locale.ENGLISH, resource, args);
        }
        return new Request(resource);
    }

    /**
     * Adds a key/value pair.
     * <pre>
     * Request r = new Request.to("/foo")
     *    .add("singleParam", "value")
     *    .add("multiParam", new String[] { "1", "2", "3" })
     *    .add("singleParamWithOutValue", null);
     * </pre>
     *
     * @param name  the name
     * @param value the value to set, will be obtained via {@link String#valueOf(boolean)}.
     *              If null, only the parameter is set.
     *              It can also be a collection or array, in which case all elements are added as query parameters
     * @return this
     */
    public Request add(String name, Object value) {
        if (value instanceof Iterable) {
            for (Object o : ((Iterable<?>)value)) {
                addParam(name, o);
            }
        } else if (value instanceof Object[]) {
            for (Object o : (Object[])value) {
                addParam(name, o);
            }
        } else {
            addParam(name, value);
        }
        return this;
    }

    private void addParam(String name, Object param) {
        mParams.add(new BasicNameValuePair(name, param == null ? null : String.valueOf(param)));
    }

    /**
     * Sets a new parameter, overwriting previous value.
     * @param name the name
     * @param value the value
     * @return this
     */
    public Request set(String name, Object value) {
        return clear(name).add(name, value);
    }

    /**
     * Clears a parameter
     * @param name name of the parameter
     * @return this
     */
    public Request clear(String name) {
        Iterator<NameValuePair> it = mParams.iterator();
        while (it.hasNext()) {
            if (it.next().getName().equals(name)) {
                it.remove();
            }
        }
        return this;
    }

    /**
     * @param args a list of arguments
     * @return this
     */
    public Request with(Object... args) {
       if (args != null) {
            if (args.length % 2 != 0) throw new IllegalArgumentException("need even number of arguments");
            for (int i = 0; i < args.length; i += 2) {
                add(args[i].toString(), args[i + 1]);
            }
       }
       return this;
    }

    /**
     * @param resource the new resource
     * @return a new request with identical parameters except for the specified resource.
     */
    public Request newResource(String resource) {
        Request newRequest = new Request(this);
        newRequest.mResource = resource;
        return newRequest;
    }

    /**
     * The request should be made with a specific token.
     * @param token the token
     * @return this
     */
    public Request usingToken(Token token) {
        mToken = token;
        return this;
    }

    /** @return the size of the parameters */
    public int size() {
        return mParams.size();
    }

    /**
     * @return a String that is suitable for use as an <code>application/x-www-form-urlencoded</code>
     * list of parameters in an HTTP PUT or HTTP POST.
     */
    public String queryString() {
        return format(mParams, UTF_8);
    }

    /**
     * @param  resource the resource
     * @return an URL with the query string parameters appended
     */
    public String toUrl(String resource) {
        return mParams.isEmpty() ? resource : resource + "?" + queryString();
    }

    public String toUrl() {
        return toUrl(mResource);
    }

    /**
     * Registers a file to be uploaded with a POST or PUT request.
     * @param name  the name of the parameter
     * @param file  the file to be submitted
     * @return this
     */
    public Request withFile(String name, File file) {
        return file != null ? withFile(name, file, file.getName()) : this;
    }

    /**
     * Registers a file to be uploaded with a POST or PUT request.
     * @param name  the name of the parameter
     * @param file  the file to be submitted
     * @param fileName  the name of the uploaded file (over rides file parameter)
     * @return this
     */
    public Request withFile(String name, File file, String fileName) {
        if (mFiles == null) mFiles = new HashMap<String,Attachment>();
        if (file != null)  mFiles.put(name, new Attachment(file, fileName));
        return this;
    }

    /**
     * Registers binary data to be uploaded with a POST or PUT request.
     * @param name  the name of the parameter
     * @param data  the data to be submitted
     * @deprecated use {@link #withFile(String, byte[], String)} instead
     * @return this
     */
    @Deprecated public Request withFile(String name, byte[] data) {
        return withFile(name, ByteBuffer.wrap(data));
    }

    /**
     * Registers binary data to be uploaded with a POST or PUT request.
     * @param name  the name of the parameter
     * @param data  the data to be submitted
     * @param fileName the name of the uploaded file
     * @return this
     */
    public Request withFile(String name, byte[] data, String fileName) {
        return withFile(name, ByteBuffer.wrap(data), fileName);
    }

    /**
     * Registers binary data to be uploaded with a POST or PUT request.
     * @param name  the name of the parameter
     * @param data  the data to be submitted
     * @return this
     * @deprecated use {@link #withFile(String, java.nio.ByteBuffer), String} instead
     */
    @Deprecated public Request withFile(String name, ByteBuffer data) {
        return withFile(name, data, "upload");
    }

    /**
     * Registers binary data to be uploaded with a POST or PUT request.
     * @param name  the name of the parameter
     * @param data  the data to be submitted
     * @param fileName the name of the uploaded file
     * @return this
     */
    public Request withFile(String name, ByteBuffer data, String fileName) {
        if (mFiles == null) mFiles = new HashMap<String, Attachment>();
        if (data != null) mFiles.put(name, new Attachment(data, fileName));
        return this;
    }

    /**
     * Adds an arbitrary entity to the request (used with POST/PUT)
     * @param entity the entity to POST/PUT
     * @return this
     */
    public Request withEntity(HttpEntity entity) {
        mEntity = entity;
        return this;
    }

    /**
     * Adds string content to the request (used with POST/PUT)
     * @param content the content to POST/PUT
     * @param contentType the content type
     * @return this
     */
    public Request withContent(String content, String contentType) {
        try {
            StringEntity stringEntity = new StringEntity(content, UTF_8);
            if (contentType != null) {
                stringEntity.setContentType(contentType);
            }
            return withEntity(stringEntity);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public Request range(long... ranges) {
        mRange = ranges;
        return this;
    }

    /**
     * @param listener a listener for receiving notifications about transfer progress
     * @return this
     */
    public Request setProgressListener(TransferProgressListener listener) {
        this.listener = listener;
        return this;
    }

    public boolean isMultipart() {
        return mFiles != null && !mFiles.isEmpty();
    }

    /**
     * Conditional GET
     * @param etag the etag to check for (If-None-Match: etag)
     * @return this
     */
    public Request ifNoneMatch(String etag) {
        mIfNoneMatch = etag;
        return this;
    }

    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<String,String>();
        for (NameValuePair p : mParams) {
            params.put(p.getName(), p.getValue());
        }
        return params;
    }

    /**
     * Builds a request with the given set of parameters and files.
     * @param method    the type of request to use
     * @param <T>       the type of request to use
     * @return HTTP request, prepared to be executed
     */
    public <T extends HttpRequestBase> T buildRequest(Class<T> method) {
        try {
            T request = method.newInstance();
            // POST/PUT ?
            if (request instanceof HttpEntityEnclosingRequestBase) {
                HttpEntityEnclosingRequestBase enclosingRequest =
                        (HttpEntityEnclosingRequestBase) request;

                final Charset charSet = java.nio.charset.Charset.forName(UTF_8);
                if (isMultipart()) {
                    MultipartEntity multiPart = new MultipartEntity(
                            HttpMultipartMode.BROWSER_COMPATIBLE,  // XXX change this to STRICT once rack on server is upgraded
                            null,
                            charSet);

                    if (mFiles != null) {
                        for (Map.Entry<String, Attachment> e : mFiles.entrySet()) {
                            multiPart.addPart(e.getKey(), e.getValue().toContentBody());
                        }
                    }

                    for (NameValuePair pair : mParams) {
                        multiPart.addPart(pair.getName(), new StringBody(pair.getValue(), "text/plain", charSet));
                    }

                    enclosingRequest.setEntity(listener == null ? multiPart :
                        new CountingMultipartEntity(multiPart, listener));

                    request.setURI(URI.create(mResource));

                // form-urlencoded?
                } else if (mEntity != null) {
                    request.setHeader(mEntity.getContentType());
                    enclosingRequest.setEntity(mEntity);
                    request.setURI(URI.create(toUrl())); // include the params

                } else {
                    if (!mParams.isEmpty()) {
                        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                        enclosingRequest.setEntity(new StringEntity(queryString()));
                    }
                    request.setURI(URI.create(mResource));
                }

            } else { // just plain GET/HEAD/DELETE/...
                if (mRange != null) {
                    request.addHeader("Range", formatRange(mRange));
                }

                if (mIfNoneMatch != null) {
                    request.addHeader("If-None-Match", mIfNoneMatch);
                }
                request.setURI(URI.create(toUrl()));
            }

            if (mToken != null) {
                request.addHeader(ApiWrapper.createOAuthHeader(mToken));
            }
            return request;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String formatRange(long... range) {
        switch (range.length) {
            case 0: return "bytes=0-";
            case 1:
                if (range[0] < 0) throw new IllegalArgumentException("negative range");
                return "bytes="+range[0]+"-";
            case 2:
                if (range[0] < 0) throw new IllegalArgumentException("negative range");
                if (range[0] > range[1]) throw new IllegalArgumentException(range[0] + ">" + range[1]);
                return "bytes="+range[0]+"-"+range[1];
            default: throw new IllegalArgumentException("invalid range specified");
        }
    }

    @Override public Iterator<NameValuePair> iterator() {
        return mParams.iterator();
    }

    @Override
    public String toString() {
        return "Request{" +
                 "mResource='" + mResource + '\'' +
                ", params=" + mParams +
                ", files=" + mFiles +
                ", entity=" + mEntity +
                ", mToken=" + mToken +
                ", listener=" + listener +
                '}';
    }

    /* package */ Token getToken() {
        return mToken;
    }

    /* package */ TransferProgressListener getListener() {
        return listener;
    }

    /**
     * Updates about the amount of bytes already transferred.
     */
    public static interface TransferProgressListener {
        /**
         * @param amount number of bytes already transferred.
         * @throws IOException if the transfer should be cancelled
         */
        public void transferred(long amount) throws IOException;
    }

    /* package */ static class ByteBufferBody extends AbstractContentBody {
        private ByteBuffer mBuffer;

        public ByteBufferBody(ByteBuffer buffer) {
            super("application/octet-stream");
            mBuffer = buffer;
        }

        @Override
        public String getFilename() {
            return null;
        }

        public String getTransferEncoding() {
            return MIME.ENC_BINARY;
        }

        public String getCharset() {
            return null;
        }

        @Override
        public long getContentLength() {
            return mBuffer.capacity();
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            if (mBuffer.hasArray()) {
                out.write(mBuffer.array());
            } else {
                byte[] dst = new byte[mBuffer.capacity()];
                mBuffer.get(dst);
                out.write(dst);
            }
        }
    }

    /* package */ static class Attachment {
        public final File file;
        public final ByteBuffer data;
        public final String fileName;

        /** @noinspection UnusedDeclaration*/
        Attachment(File file) {
            this(file, file.getName());
        }

        Attachment(File file, String fileName) {
            if (file == null) throw  new IllegalArgumentException("file cannot be null");
            this.fileName = fileName;
            this.file = file;
            this.data = null;
        }

        /** @noinspection UnusedDeclaration*/
        Attachment(ByteBuffer data) {
            this(data, null);
        }

        Attachment(ByteBuffer data, String fileName) {
            if (data == null) throw new IllegalArgumentException("data cannot be null");

            this.data = data;
            this.fileName = fileName;
            this.file = null;
        }

        public ContentBody toContentBody() {
            if (file != null) {
                return new FileBody(file) {
                    @Override
                    public String getFilename() {
                        return fileName;
                    }
                };
            } else if (data != null) {
                return new ByteBufferBody(data) {
                    @Override
                    public String getFilename() {
                        return fileName;
                    }
                };
            } else {
                // never happens
                throw new IllegalStateException("no upload data");
            }
        }
    }

    /**
     * Returns a String that is suitable for use as an <code>application/x-www-form-urlencoded</code>
     * list of parameters in an HTTP PUT or HTTP POST.
     *
     * @param parameters  The parameters to include.
     * @param encoding The encoding to use.
     */
    public static String format(
            final List<? extends NameValuePair> parameters,
            final String encoding) {
        final StringBuilder result = new StringBuilder();
        for (final NameValuePair parameter : parameters) {
            final String encodedName = encode(parameter.getName(), encoding);
            final String value = parameter.getValue();
            final String encodedValue = value != null ? encode(value, encoding) : "";
            if (result.length() > 0)
                result.append("&");
            result.append(encodedName);
            if (value != null) {
                result.append("=");
                result.append(encodedValue);
            }
        }
        return result.toString();
    }

    private static String encode(final String content, final String encoding) {
        try {
            return URLEncoder.encode(content, encoding != null ? encoding : HTTP.DEFAULT_CONTENT_CHARSET);
        } catch (UnsupportedEncodingException problem) {
            throw new IllegalArgumentException(problem);
        }
    }
}
