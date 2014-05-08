package io.scal.secureshareui.soundcloud;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class CountingMultipartEntity implements HttpEntity {
    private HttpEntity mDelegate;
    private Request.TransferProgressListener mListener;

    public CountingMultipartEntity(HttpEntity delegate,
                                   Request.TransferProgressListener listener) {
        super();
        mDelegate = delegate;
        mListener = listener;
    }

    public void consumeContent() throws IOException {
        mDelegate.consumeContent();
    }

    public InputStream getContent() throws IOException, IllegalStateException {
        return mDelegate.getContent();
    }

    public Header getContentEncoding() {
        return mDelegate.getContentEncoding();
    }

    public long getContentLength() {
        return mDelegate.getContentLength();
    }

    public Header getContentType() {
        return mDelegate.getContentType();
    }

    public boolean isChunked() {
        return mDelegate.isChunked();
    }

    public boolean isRepeatable() {
        return mDelegate.isRepeatable();
    }

    public boolean isStreaming() {
        return mDelegate.isStreaming();
    }

    public void writeTo(OutputStream outstream) throws IOException {
        mDelegate.writeTo(new CountingOutputStream(outstream, mListener));
    }

    private static class CountingOutputStream extends FilterOutputStream {
        private final Request.TransferProgressListener mListener;
        private long mTransferred = 0;

        public CountingOutputStream(final OutputStream out, final Request.TransferProgressListener listener) {
            super(out);
            mListener = listener;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            mTransferred += len;
            if (mListener != null) mListener.transferred(mTransferred);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            mTransferred++;
            if (mListener != null) mListener.transferred(mTransferred);
        }
    }
}
