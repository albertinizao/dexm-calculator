package com.dexm.personajes;

import com.dexm.personajes.adapter.in.web.AuthController;
import com.dexm.personajes.security.AuthorizationService;
import com.dexm.personajes.security.SecurityIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock private SecurityIdentityService identities;
    @Mock private AuthorizationService authorization;
    @Mock private Authentication authentication;
    private AuthController controller;

    @BeforeEach void setUp() { controller = new AuthController(identities, authorization); }

    @Test void keepalivePreservesTheExistingNoContentContractWithoutOAuth() {
        var response = controller.keepalive(authentication);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authorization).requireApplicationAccess(authentication);
    }

    @Test void logoutIsAStatelessNoContentOperation() {
        assertThat(controller.logout().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
