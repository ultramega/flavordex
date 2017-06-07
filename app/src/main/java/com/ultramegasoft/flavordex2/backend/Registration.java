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
     */
    public RegistrationRecord register() throws ApiException {
        final String fcmId = FirebaseInstanceId.getInstance().getToken();
        if(fcmId != null) {
            try {
                final RegistrationRecord record =
                        new Gson().fromJson(post("register", fcmId), RegistrationRecord.class);
                if(record.clientId <= 0) {
                    throw new ApiException("Received invalid client ID: " + record.clientId);
                }
                return record;
            } catch(JsonSyntaxException e) {
                throw new ParseException(e);
            }
        }
        return null;
    }

    /**
     * Unregister the client from the backend.
     */
    public void unregister() throws ApiException {
        final long id = BackendUtils.getClientId(getContext());
        if(id > 0) {
            post("unregister", id);
        }
    }
}
