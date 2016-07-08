package com.ultramegasoft.flavordex2.backend;

/**
 * Exception thrown when there is a problem communicating with the API.
 *
 * @author Steve Guidetti
 */
public class ApiException extends Exception {
    public ApiException() {
        super();
    }

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiException(Throwable cause) {
        super(cause);
    }
}
