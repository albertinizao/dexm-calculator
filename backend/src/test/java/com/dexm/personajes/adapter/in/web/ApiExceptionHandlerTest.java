package com.dexm.personajes.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {
    @Test
    void mapsNotFoundToHttpStatusAndSafeContract() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/characters/1");
        request.setAttribute(RequestCorrelationFilter.ATTRIBUTE, "test-request");
        var response = new ApiExceptionHandler().notFound(new NoSuchElementException("secret-email@example.com"), request);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("test-request", response.getBody().requestId());
        assertEquals("Recurso no encontrado.", response.getBody().message());
        assertFalse(response.getBody().message().contains("secret-email"));
    }

    @Test
    void usesValidatedRequestIdInsteadOfRawHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.addHeader(RequestCorrelationFilter.HEADER, "bad id with spaces");
        request.setAttribute(RequestCorrelationFilter.ATTRIBUTE, "validated-id");
        var response = new ApiExceptionHandler().unexpected(new IllegalStateException("Firestore token"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("validated-id", response.getBody().requestId());
        assertEquals("Se ha producido un error interno.", response.getBody().message());
    }
}
