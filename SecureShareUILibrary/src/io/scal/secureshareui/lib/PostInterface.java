package io.scal.secureshareui.lib;

import timber.log.Timber;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.mime.TypedOutput;

/**
 * Created by mnbogner on 5/14/15.
 */

// api/story/

// how to use proxy?

public interface PostInterface {

    @POST("/api/story/")
    Response postContent(@Header("Authorization") String authorization, // must be "Bearer <token>"
                      // @Header("Content-type") String contentType, // must be "application/json"
                      // @Body String jsonString); // will retrofit attempt to convert this?
                         @Body TypedOutput body);

}
