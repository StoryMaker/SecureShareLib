package io.scal.secureshareui.lib;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.mime.TypedOutput;

/**
 * Created by mnbogner on 5/14/15.
 */

// api/story/

// how to use proxy?

public interface IndexInterface {

    @GET("/api/{version}/index/{id}") // need real endpoint
    Response getIndex(@Path("version") int version,
                      @Path("id") int id); // need return type (json string?)
}
