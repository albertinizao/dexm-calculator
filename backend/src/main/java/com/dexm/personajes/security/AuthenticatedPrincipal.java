package com.dexm.personajes.security;

/** Identity accepted only after local-mode injection or IAP JWT verification. */
public record AuthenticatedPrincipal(String subject, String email, String name) { }
