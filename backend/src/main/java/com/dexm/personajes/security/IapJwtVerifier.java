package com.dexm.personajes.security;

public interface IapJwtVerifier {
    IapJwtClaims verify(String assertion);
}
