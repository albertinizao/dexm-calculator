package com.dexm.personajes.security;

public class IapJwtVerificationException extends RuntimeException {
    public IapJwtVerificationException(String message) { super(message); }
    public IapJwtVerificationException(String message, Throwable cause) { super(message, cause); }
}
