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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.ultramegasoft.flavordex2.BuildConfig;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.backend.model.Model;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Represents an endpoint of the API.
 *
 * @author Steve Guidetti
 */
abstract class Endpoint {
    private static final String TAG = "Endpoint";

    /**
     * The Context
     */
    @NonNull
    private final Context mContext;

    /**
     * The base for all API URLs
     */
    @NonNull
    private final HttpUrl mBaseUrl;

    /**
     * The user agent string
     */
    @NonNull
    private final String mUserAgent;

    /**
     * The user's authorization token
     */
    @Nullable
    private String mAuthToken;

    /**
     * The HTTP client
     */
    @NonNull
    private final OkHttpClient mClient;

    /**
     * Constructor.
     *
     * @param context The Context to use
     */
    Endpoint(@NonNull Context context) {
        try {
            ProviderInstaller.installIfNeeded(context);
        } catch(GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        mContext = context;

        final OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS);
        configureSSL(builder);
        mClient = builder.build();

        final String apiUri =
                context.getString(BuildConfig.DEBUG ? R.string.api_url_debug : R.string.api_url);
        mBaseUrl = HttpUrl.parse(apiUri).newBuilder()
                .addEncodedPathSegment(getName())
                .build();
        mUserAgent = context.getString(R.string.user_agent, BuildConfig.VERSION_NAME);

        loadAuthToken();
    }

    /**
     * Set up the SSL environment for HTTPS connections.
     *
     * @param builder The HTTP client builder
     */
    private void configureSSL(OkHttpClient.Builder builder) {
        if(BuildConfig.DEBUG) {
            Log.d(TAG, "Disabling SSL verification for debug mode...");

            @SuppressLint("TrustAllX509TrustManager") final X509TrustManager trustManager = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }
            };

            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());
                builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
            } catch(KeyManagementException | NoSuchAlgorithmException e) {
                Log.e(TAG, "Unable to disable SSL certificate checking for debugging", e);
            }

            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                @SuppressLint("BadHostnameVerifier")
                public boolean verify(String hostname, SSLSession session) {
                    return true;
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
            try {
                final GetTokenResult result = Tasks.await(auth.getIdToken(true));
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
    @NonNull
    protected abstract String getName();

    /**
     * Get the Context.
     *
     * @return The Context
     */
    @NonNull
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
    @NonNull
    String get(@NonNull String method, @NonNull Object... params) throws FlavordexApiException {
        try {
            final Request request = getRequestBuilder(method, params)
                    .get()
                    .build();
            final Call call = mClient.newCall(request);
            return readResponse(call.execute());
        } catch(IOException e) {
            throw new FlavordexApiException("Request failed", e);
        }
    }

    /**
     * Perform a POST request on the API.
     *
     * @param method The method to access
     * @param data   The data to send
     * @return The response from the API.
     */
    @NonNull
    String post(@NonNull String method, @Nullable Object data) throws FlavordexApiException {
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
    @NonNull
    String post(@NonNull String method, @Nullable Object data, @NonNull Object... params)
            throws FlavordexApiException {
        try {
            final Request.Builder builder = getRequestBuilder(method, params);
            if(data != null) {
                if(data instanceof Model) {
                    final MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                    final RequestBody body = RequestBody.create(mediaType, ((Model)data).toJson());
                    builder.post(body);
                } else {
                    final MediaType mediaType = MediaType.parse("text/plain; charset=utf-8");
                    final RequestBody body = RequestBody.create(mediaType, data.toString());
                    builder.post(body);
                }
            } else {
                builder.post(RequestBody.create(null, new byte[0]));
            }

            final Request request = builder.build();
            final Response response = mClient.newCall(request).execute();

            return readResponse(response);
        } catch(IOException e) {
            throw new FlavordexApiException("Request failed", e);
        }
    }

    /**
     * Read a response from the server.
     *
     * @param response The HTTP response
     * @return The response body as a string
     */
    @NonNull
    private String readResponse(@NonNull Response response) throws IOException, FlavordexApiException {
        if(!response.isSuccessful()) {
            if(response.code() == 401) {
                throw new UnauthorizedException();
            } else {
                throw new FlavordexApiException(response.message());
            }
        }

        final ResponseBody body = response.body();
        try {
            return body.string();
        } finally {
            body.close();
        }
    }

    /**
     * Initialize a request to the backend server.
     *
     * @param method The method to access
     * @param params The parameters for the method
     * @return The HTTP request builder
     */
    @NonNull
    private Request.Builder getRequestBuilder(@NonNull String method, @NonNull Object... params) {
        final HttpUrl url = mBaseUrl.newBuilder()
                .addEncodedPathSegment(method)
                .addEncodedPathSegments(TextUtils.join("/", params))
                .build();
        return getRequestBuilder(url);
    }

    /**
     * Initialize an HTTP request.
     *
     * @param url The URL to connect to
     * @return The HTTP request builder
     */
    @NonNull
    private Request.Builder getRequestBuilder(@NonNull HttpUrl url) {
        final Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", mUserAgent)
                .cacheControl(new CacheControl.Builder().noCache().build());

        if(mAuthToken != null) {
            builder.addHeader("Auth-Token", mAuthToken);
        }

        return builder;
    }

}
