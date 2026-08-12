package com.dexm.personajes.security;

public record IapJwtClaims(String subject, String email, String name) { }
