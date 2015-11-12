package com.ultramegasoft.flavordex2.backend;

import com.google.api.server.spi.Constant;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(RegistrationEndpoint.class.getName());

    /**
     * Register a client device with the backend.
     *
     * @param gcmId The GCM registration ID
     * @param user  The User
     * @return The RegistrationRecord containing a unique ID for the client to store
     * @throws InternalServerErrorException
     * @throws UnauthorizedException
     */
    @ApiMethod(name = "register")
    public RegistrationRecord register(@Named("gcmId") String gcmId, User user)
            throws InternalServerErrorException, UnauthorizedException {
        if(user == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        final DatabaseHelper helper = new DatabaseHelper();
        try {
            helper.open();
            helper.setUser(user.getEmail());
            return helper.registerClient(gcmId);
        } catch(SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to register the client", e);
            throw new InternalServerErrorException("Failed to register the client");
        } finally {
            helper.close();
        }
    }

    /**
     * Unregister a client device with the backend.
     *
     * @param clientId The GCM registration ID
     * @param user     The User
     * @throws InternalServerErrorException
     * @throws UnauthorizedException
     */
    @ApiMethod(name = "unregister")
    public void unregister(@Named("clientId") long clientId, User user)
            throws InternalServerErrorException, UnauthorizedException {
        if(user == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        final DatabaseHelper helper = new DatabaseHelper();
        try {
            helper.open();
            helper.setUser(user.getEmail());
            helper.unregisterClient(clientId);
        } catch(SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to unregister the client", e);
            throw new InternalServerErrorException("Failed to unregister the client");
        } finally {
            helper.close();
        }
    }
}
