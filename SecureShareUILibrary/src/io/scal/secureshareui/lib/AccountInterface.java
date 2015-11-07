package io.scal.secureshareui.lib;

import timber.log.Timber;

import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by mnbogner on 5/14/15.
 */

// POST to /api/1/accounts/ to create accounts

// this may be redundant vs. UserInterface

public interface AccountInterface {

    @POST("/api/{version}/accounts/")
    Object createUser(@Path("version") int version,
                      @Path("id") int id,
                      @Body Object patch); // need return type and input type

}
