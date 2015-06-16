package io.scal.secureshareui.lib;

import java.io.IOException;

/**
 * Created by mnbogner on 6/11/15.
 */
public class CaptchaException extends IOException {

    String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
