package com.ultramegasoft.flavordex2.backend;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.ultramegasoft.flavordex2.BuildConfig;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.backend.model.Model;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Represents an endpoint of the API.
 *
 * @author Steve Guidetti
 */
public abstract class Endpoint {
    /**
     * The Context
     */
    private final Context mContext;

    /**
     * The base for all API URLs
     */
    private final Uri mBaseUrl;

    /**
     * The user agent string
     */
    private final String mUserAgent;

    /**
     * Constructor.
     *
     * @param context The Context to use
     */
    public Endpoint(Context context) {
        mContext = context;
        //noinspection ConstantConditions
        final Uri apiUri = Uri.parse(context.getString(
                FlavordexApp.DEVELOPER_MODE ? R.string.api_url_debug : R.string.api_url));
        mBaseUrl = Uri.withAppendedPath(apiUri, getName());
        mUserAgent = context.getString(R.string.user_agent, BuildConfig.VERSION_NAME);
    }

    /**
     * Get the name of the endpoint that this implementation represents.
     *
     * @return The name of the endpoint
     */
    protected abstract String getName();

    /**
     * Get the Context.
     *
     * @return The Context
     */
    protected Context getContext() {
        return mContext;
    }

    /**
     * Perform a GET request on the API.
     *
     * @param method The method to access
     * @return The response from the API.
     * @throws ApiException
     */
    protected String get(String method) throws ApiException {
        return get(method, new Object[0]);
    }

    /**
     * Perform a GET request on the API.
     *
     * @param method The method to access
     * @param params The parameters for the method
     * @return The response from the API.
     * @throws ApiException
     */
    protected String get(String method, Object... params) throws ApiException {
        try {
            final HttpURLConnection conn = openConnection(method, params);
            conn.setRequestMethod("GET");
            return readResponse(conn);
        } catch(IOException e) {
            throw new ApiException("Request failed", e);
        }
    }

    /**
     * Perform a POST request on the API.
     *
     * @param method The method to access
     * @return The response from the API.
     * @throws ApiException
     */
    protected String post(String method) throws ApiException {
        return post(method, null);
    }

    /**
     * Perform a POST request on the API.
     *
     * @param method The method to access
     * @param data   The data to send
     * @return The response from the API.
     * @throws ApiException
     */
    protected String post(String method, Object data) throws ApiException {
        return post(method, data, new Object[0]);
    }

    /**
     * Perform a POST request on the API.
     *
     * @param method The method to access
     * @param data   The data to send
     * @param params The parameters for the method
     * @return The response from the API.
     * @throws ApiException
     */
    protected String post(String method, Object data, Object... params) throws ApiException {
        try {
            final HttpURLConnection conn = openConnection(method, params);
            conn.setRequestMethod("POST");
            if(data != null) {
                conn.setDoOutput(true);
                conn.setChunkedStreamingMode(0);

                final String dataString;
                if(data instanceof Model) {
                    conn.setRequestProperty("Content-Type", "application/json");
                    dataString = ((Model)data).toJson();
                } else {
                    conn.setRequestProperty("Content-Type", "text/plain");
                    dataString = data.toString();
                }

                final DataOutputStream dos =
                        new DataOutputStream(new BufferedOutputStream(conn.getOutputStream()));
                try {
                    dos.writeBytes(dataString);
                } finally {
                    dos.flush();
                    dos.close();
                }
            }

            return readResponse(conn);
        } catch(IOException e) {
            throw new ApiException("Request failed", e);
        }
    }

    /**
     * Read a response from the server.
     *
     * @param conn The HTTP connection
     * @return The response body as a string
     * @throws IOException
     * @throws ApiException
     */
    private String readResponse(HttpURLConnection conn) throws IOException, ApiException {
        try {
            final int code = conn.getResponseCode();
            if(code != HttpURLConnection.HTTP_OK) {
                if(code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    throw new UnauthorizedException();
                } else {
                    throw new ApiException(conn.getResponseMessage());
                }
            }
        } catch(IOException e) {
            if(e.getMessage().equals("Received authentication challenge is null")) {
                throw new UnauthorizedException();
            }
            throw e;
        }

        final InputStream inputStream = conn.getInputStream();
        final ByteArrayOutputStream content = new ByteArrayOutputStream();
        try {
            final byte[] buffer = new byte[8192];
            int readBytes;
            while((readBytes = inputStream.read(buffer)) != -1) {
                content.write(buffer, 0, readBytes);
            }
            return new String(content.toByteArray());
        } finally {
            inputStream.close();
            content.close();
        }

    }

    /**
     * Open a connection to the backend server.
     *
     * @param method The method to access
     * @param params The parameters for the method
     * @return The HTTP connection
     * @throws IOException
     */
    private HttpURLConnection openConnection(String method, Object... params) throws IOException {
        final Uri uri = mBaseUrl.buildUpon()
                .appendEncodedPath(method)
                .appendEncodedPath(TextUtils.join("/", params))
                .build();
        return openConnection(new URL(uri.toString()));
    }

    /**
     * Open an HTTP connection.
     *
     * @param url The URL to connect to
     * @return The HTTP connection
     * @throws IOException
     */
    private HttpURLConnection openConnection(URL url) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(30000);
        conn.setRequestProperty("User-Agent", mUserAgent);

        final FirebaseUser auth = FirebaseAuth.getInstance().getCurrentUser();
        if(auth != null) {
            final Task<GetTokenResult> tokenTask = auth.getToken(true);
            while(!tokenTask.isComplete()) {
            }
            final String token = tokenTask.getResult().getToken();
            conn.setRequestProperty("Auth-Token", token);
        }
        return conn;
    }

}
