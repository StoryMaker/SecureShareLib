package io.scal.secureshareui.lib;

import timber.log.Timber;

import java.io.IOException;
import java.io.OutputStream;

import ch.boye.httpclientandroidlib.HttpEntity;
import retrofit.mime.TypedOutput;

/**
 * Created by mnbogner on 5/15/15.
 */
public class EntityWrapper implements TypedOutput {

    private HttpEntity entity;

    public EntityWrapper (HttpEntity entity) {
        this.entity = entity;
    }

    @Override
    public String fileName() {
        return null;
    }

    @Override
    public String mimeType() {
        return entity.getContentType().getValue();
    }

    @Override
    public long length() {
        return entity.getContentLength();
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        entity.writeTo(out);
    }
}
