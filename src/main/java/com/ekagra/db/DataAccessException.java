package com.ekagra.db;

/** Unchecked wrapper for SQL errors, so services don't need try/catch for every query. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
