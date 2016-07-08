package com.ultramegasoft.flavordex2.backend;

/**
 * Exception thrown when there is a problem parsing a response from the API.
 *
 * @author Steve Guidetti
 */
public class ParseException extends ApiException {
    public ParseException() {
        super();
    }

    public ParseException(Throwable cause) {
        super(cause);
    }
}
