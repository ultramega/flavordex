package com.ultramegasoft.flavordex2.backend;

import android.content.Context;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.ultramegasoft.flavordex2.backend.model.RegistrationRecord;

/**
 * Represents the Registration endpoint.
 *
 * @author Steve Guidetti
 */
public class Registration extends Endpoint {
    public Registration(Context context) {
        super(context);
    }

    @Override
    protected String getName() {
        return "registration";
    }

    /**
     * Register the client with the backend.
     *
     * @return The RegistrationRecord or null if the client is not registered with FCM
     * @throws ApiException
     */
    public RegistrationRecord register() throws ApiException {
        final String fcmId = FirebaseInstanceId.getInstance().getToken();
        if(fcmId != null) {
            try {
                return new Gson().fromJson(post("register", fcmId), RegistrationRecord.class);
            } catch(JsonSyntaxException e) {
                throw new ParseException(e);
            }
        }
        return null;
    }

    /**
     * Unregister the client from the backend.
     *
     * @throws ApiException
     */
    public void unregister() throws ApiException {
        final long id = BackendUtils.getClientId(getContext());
        if(id > 0) {
            post("unregister", id);
        }
    }
}
