package com.example.Jark.exception;

/**
 * Exception thrown when repository ingestion (clone, validation) fails.
 */
public class RepositoryIngestionException extends RuntimeException {

    public RepositoryIngestionException(String message) {
        super(message);
    }

    public RepositoryIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
