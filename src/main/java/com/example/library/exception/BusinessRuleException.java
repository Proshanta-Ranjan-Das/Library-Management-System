package com.example.library.exception;

/** Thrown when a request is valid but violates a library rule (no copies, unpaid fines, ...). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
