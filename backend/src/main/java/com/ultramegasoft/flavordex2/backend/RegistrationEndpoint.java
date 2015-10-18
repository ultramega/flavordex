package com.ultramegasoft.flavordex2.backend;

import com.google.api.server.spi.Constant;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;

import javax.inject.Named;

/**
 * The client registration endpoint to register devices with Google Cloud Messaging.
 *
 * @author Steve Guidetti
 */
@Api(
        name = "registration",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "backend.flavordex2.ultramegasoft.com",
                ownerName = "backend.flavordex2.ultramegasoft.com",
                packagePath = ""
        ),
        scopes = {
                BackendConstants.EMAIL_SCOPE
        },
        clientIds = {
                BackendConstants.WEB_CLIENT_ID,
                BackendConstants.ANDROID_CLIENT_ID,
                BackendConstants.ANDROID_CLIENT_ID_DEBUG,
                Constant.API_EXPLORER_CLIENT_ID
        },
        audiences = {
                BackendConstants.ANDROID_AUDIENCE
        }
)
public class RegistrationEndpoint {

    /**
     * Register a client device with the backend.
     *
     * @param user  The User
     * @param gcmId The GCM registration ID
     * @return The RegistrationRecord containing a unique ID for the client to store
     * @throws UnauthorizedException
     */
    @ApiMethod(name = "register")
    public RegistrationRecord register(User user, @Named("gcmId") String gcmId)
            throws UnauthorizedException {
        if(user == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return DatabaseHelper.registerClient(user.getEmail(), gcmId);
    }

    /**
     * Unregister a client device with the backend.
     *
     * @param user     The User
     * @param clientId The GCM registration ID
     * @throws UnauthorizedException
     */
    @ApiMethod(name = "unregister")
    public void unregister(User user, @Named("clientId") long clientId)
            throws UnauthorizedException {
        if(user == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        DatabaseHelper.unregisterClient(clientId, user.getEmail());
    }
}
