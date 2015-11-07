package io.scal.secureshareui.lib;

import timber.log.Timber;

import retrofit.client.Response;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;

/**
 * Created by mnbogner on 5/14/15.
 */

// /api/oauth for oauth logins

// was api/oauth2/access_token?

// how to use proxy?

public interface LoginInterface {

    @FormUrlEncoded
    @POST("/api/oauth2/access_token")
    Response getAccessToken(@Field("client_id") String clientId,
                            @Field("client_secret") String clientSecret,
                            @Field("grant_type") String grantType, // must be "password"
                            @Field("username") String username,
                            @Field("password") String password);

}
