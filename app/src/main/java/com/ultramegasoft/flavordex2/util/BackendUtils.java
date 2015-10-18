package com.ultramegasoft.flavordex2.util;

import android.content.Context;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.backend.registration.Registration;

/**
 * Helpers for accessing the backend.
 *
 * @author Steve Guidetti
 */
public class BackendUtils {
    /**
     * Keys for the backend shared preferences
     */
    private static final String PREFS_KEY = "backend";
    private static final String PREF_CLIENT_ID = "pref_client_id";

    /**
     * The API project ID
     */
    private static final String PROJECT_ID = "flavordex";

    /**
     * The project Web client ID
     */
    private static final String WEB_CLIENT_ID = "1001621163874-su48pt09eaj7rd4g0mni19ag4vv2g7p7.apps.googleusercontent.com";

    /**
     * Get a GoogleAccountCredential to authenticate with the backend.
     *
     * @param context The Context
     * @return A GoogleAccountCredential
     */
    public static GoogleAccountCredential getCredential(Context context) {
        return GoogleAccountCredential.usingAudience(context, "server:client_id:" + WEB_CLIENT_ID);
    }

    /**
     * Get the client identifier.
     *
     * @param context The Context
     * @return The client identifier
     */
    public static long getClientId(Context context) {
        return context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
                .getLong(PREF_CLIENT_ID, 0);
    }

    /**
     * Set the client identifier.
     *
     * @param context  The Context
     * @param clientId The client identifier
     */
    public static void setClientId(Context context, long clientId) {
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
                .putLong(PREF_CLIENT_ID, clientId).apply();
    }

    /**
     * Get a Registration endpoint client.
     *
     * @param credential The credential to use for authentication
     * @return The Registration endpoint client
     */
    public static Registration getRegistration(GoogleAccountCredential credential) {
        final Registration.Builder builder = new Registration.Builder(new ApacheHttpTransport(),
                new AndroidJsonFactory(), credential);
        return (Registration)build(builder);
    }

    /**
     * Build an endpoint client from a Builder.
     *
     * @param builder The Builder
     * @return The built endpoint client
     */
    private static AbstractGoogleJsonClient build(AbstractGoogleJsonClient.Builder builder) {
        builder.setApplicationName(PROJECT_ID);
        if(FlavordexApp.DEVELOPER_MODE) {
            builder.setRootUrl("http://10.0.2.2:8080/_ah/api");
        }
        return builder.build();
    }
}
