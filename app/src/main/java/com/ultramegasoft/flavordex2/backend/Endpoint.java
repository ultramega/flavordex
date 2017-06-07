/*
 * The MIT License (MIT)
 * Copyright © 2016 Steve Guidetti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.ultramegasoft.flavordex2.backend;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.ultramegasoft.flavordex2.BuildConfig;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.backend.model.Model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Represents an endpoint of the API.
 *
 * @author Steve Guidetti
 */
abstract class Endpoint {
    private static final String TAG = "Endpoint";

    static {
        configureSSL();
    }

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
     * The user's authorization token
     */
    private String mAuthToken;

    /**
     * Constructor.
     *
     * @param context The Context to use
     */
    Endpoint(Context context) {
        mContext = context;
        //noinspection ConstantConditions
        final Uri apiUri = Uri.parse(context.getString(
                BuildConfig.DEBUG ? R.string.api_url_debug : R.string.api_url));
        mBaseUrl = Uri.withAppendedPath(apiUri, getName());
        mUserAgent = context.getString(R.string.user_agent, BuildConfig.VERSION_NAME);

        loadAuthToken();
    }

    /**
     * Set up the SSL environment for HTTPS connections.
     */
    private static void configureSSL() {
        if(BuildConfig.DEBUG) {
            @SuppressLint("TrustAllX509TrustManager")
            final TrustManager trustManager = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }
            };

            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            } catch(KeyManagementException | NoSuchAlgorithmException e) {
                Log.e(TAG, "Unable to disable SSL certificate checking for debugging", e);
            }

            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return BuildConfig.DEBUG;
                }
            });
        }
    }

    /**
     * Load the user's auth token if it is available.
     */
    private void loadAuthToken() {
        final FirebaseUser auth = FirebaseAuth.getInstance().getCurrentUser();
        if(auth != null) {
            final Task<GetTokenResult> tokenTask = auth.getToken(true);
            try {
                final GetTokenResult result = Tasks.await(tokenTask);
                mAuthToken = result.getToken();
            } catch(ExecutionException | InterruptedException e) {
                Log.e(TAG, "Failed to obtain authorization token", e);
            }
        }
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
    Context getContext() {
        return mContext;
    }

    /**
     * Perform a GET request on the API.
     *
     * @param method The method to access
     * @param params The parameters for the method
     * @return The response from the API.
     */
    String get(String method, Object... params) throws ApiException {
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
     * @param data   The data to send
     * @return The response from the API.
     */
    String post(String method, Object data) throws ApiException {
        return post(method, data, new Object[0]);
    }

    /**
     * Perform a POST request on the API.
     *
     * @param method The method to access
     * @param data   The data to send
     * @param params The parameters for the method
     * @return The response from the API.
     */
    String post(String method, Object data, Object... params) throws ApiException {
        try {
            final HttpURLConnection conn = openConnection(method, params);
            conn.setRequestMethod("POST");
            if(data != null) {
                conn.setDoOutput(true);
                conn.setChunkedStreamingMode(0);

                final String dataString;
                if(data instanceof Model) {
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    dataString = ((Model)data).toJson();
                } else {
                    conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
                    dataString = data.toString();
                }

                conn.getOutputStream().write(dataString.getBytes());
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
     */
    private HttpURLConnection openConnection(URL url) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(30000);
        conn.setRequestProperty("User-Agent", mUserAgent);

        if(mAuthToken != null) {
            conn.setRequestProperty("Auth-Token", mAuthToken);
        }
        return conn;
    }

}
