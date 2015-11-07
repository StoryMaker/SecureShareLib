package io.scal.secureshareui.soundcloud;

import timber.log.Timber;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

abstract class DecompressingEntity extends HttpEntityWrapper {
    /**
     * Default buffer size.
     */
    private static final int BUFFER_SIZE = 1024 * 2;

    /**
     * {@link #getContent()} method must return the same {@link InputStream}
     * instance when DecompressingEntity is wrapping a streaming entity.
     */
    private InputStream content;

    /**
     * Creates a new {@link DecompressingEntity}.
     *
     * @param wrapped the non-null {@link HttpEntity} to be wrapped
     */
    public DecompressingEntity(final HttpEntity wrapped) {
        super(wrapped);
    }

    abstract InputStream decorate(final InputStream wrapped) throws IOException;

    private InputStream getDecompressingStream() throws IOException {
        final InputStream in = wrappedEntity.getContent();
        try {
            return decorate(in);
        } catch (final IOException ex) {
            in.close();
            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getContent() throws IOException {
        if (wrappedEntity.isStreaming()) {
            if (content == null) {
                content = getDecompressingStream();
            }
            return content;
        } else {
            return getDecompressingStream();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        final InputStream instream = getContent();
        try {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int l;
            while ((l = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, l);
            }
        } finally {
            instream.close();
        }
    }
}
