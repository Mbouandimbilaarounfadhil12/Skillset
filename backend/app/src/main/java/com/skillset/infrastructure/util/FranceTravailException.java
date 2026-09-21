package com.skillset.infrastructure.util;

public class FranceTravailException extends RuntimeException {
    private final int httpStatus;

    public FranceTravailException(String message) {
        super(message);
        this.httpStatus = 500;
    }

    public FranceTravailException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = 500;
    }

    public FranceTravailException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public FranceTravailException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
