package io.scal.secureshareui.lib;

import timber.log.Timber;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by mnbogner on 5/14/15.
 */

// GET /api/1/accounts/<ID> to read a user’s data, PATCH to the same URL to change user data

// rename to/replace AccountInterface?

public interface UserInterface {

    @POST("/api/{version}/accounts/")
    Object createUser(@Path("version") int version,
                      @Path("id") int id,
                      @Body Object userData); // need return type and input type

    @GET("/api/{version}/accounts/{id}")
    Object lookupUser(@Path("version") int version,
                      @Path("id") int id); // need return type

    @PATCH("/api/{version}/accounts/{id}")
    Object updateUser(@Path("version") int version,
                      @Path("id") int id,
                      @Body Object userData); // need return type and input type

}
