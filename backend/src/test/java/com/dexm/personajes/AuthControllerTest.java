package com.dexm.personajes;

import com.dexm.personajes.adapter.in.web.AuthController;
import com.dexm.personajes.security.AuthorizationService;
import com.dexm.personajes.security.SecurityIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock
    private SecurityIdentityService identities;
    @Mock
    private AuthorizationService authorization;
    @Mock
    private OAuth2AuthorizedClientManager authorizedClients;
    @Mock
    private Authentication authentication;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private HttpServletResponse servletResponse;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(identities, authorization, authorizedClients);
    }

    @Test
    void keepaliveAuthorizesGoogleClientForTheAuthenticatedSession() throws NoSuchMethodException {
        when(authorizedClients.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(
                org.mockito.Mockito.mock(OAuth2AuthorizedClient.class));

        var response = controller.keepalive(authentication, servletRequest, servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authorization).requireApplicationAccess(authentication);
        var request = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        verify(authorizedClients).authorize(request.capture());
        assertThat(request.getValue().getClientRegistrationId()).isEqualTo("google");
        assertThat(request.getValue().getPrincipal()).isSameAs(authentication);
        assertThat((Object) request.getValue().getAttribute(HttpServletRequest.class.getName())).isSameAs(servletRequest);
        assertThat((Object) request.getValue().getAttribute(HttpServletResponse.class.getName())).isSameAs(servletResponse);
        assertThat(AuthController.class.getDeclaredMethod("keepalive", Authentication.class,
                HttpServletRequest.class, HttpServletResponse.class).getAnnotation(PostMapping.class).value())
                .containsExactly("/keepalive");
    }

    @Test
    void keepaliveKeepsTheApplicationSessionWhenAuthorizedClientCannotBeLoaded() {
        when(authorizedClients.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

        var response = controller.keepalive(authentication, servletRequest, servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void keepaliveDoesNotInvalidateTheApplicationSessionWhenRefreshFails() {
        when(authorizedClients.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenThrow(new OAuth2AuthorizationException(new OAuth2Error("invalid_grant")));

        var response = controller.keepalive(authentication, servletRequest, servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void keepaliveHidesUnexpectedRefreshFailuresAsServiceUnavailable() {
        when(authorizedClients.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenThrow(new IllegalStateException("refresh backend failed"));

        var response = controller.keepalive(authentication, servletRequest, servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
