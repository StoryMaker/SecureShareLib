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

// how to use proxy?

public interface IndexInterface {

    @GET("/api/{version}/assignment/available/")
    Response getIndex(@Path("version") int version, // currently 1
                      @Header("Authorization") String authorization); // must be "Bearer <token>"
}
