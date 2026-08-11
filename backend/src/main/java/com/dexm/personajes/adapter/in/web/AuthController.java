package com.dexm.personajes.adapter.in.web;

import com.dexm.personajes.security.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final SecurityIdentityService identities;
    private final AuthorizationService authorization;
    private final OAuth2AuthorizedClientManager authorizedClients;

    public AuthController(SecurityIdentityService identities, AuthorizationService authorization,
                          OAuth2AuthorizedClientManager authorizedClients) {
        this.identities = identities;
        this.authorization = authorization;
        this.authorizedClients = authorizedClients;
    }

    @GetMapping("/me")
    public Map<String,Object> me(Authentication authentication){
        var identity=identities.current(authentication);
        authorization.requireApplicationAccess(authentication);
        var user=identities.provision(authentication);
        return Map.of("id",user.getId(),"email",identity.email(),"name",identity.name(),"admin",identities.isAdmin(identity));
    }

    @PostMapping("/keepalive")
    public ResponseEntity<Void> keepalive(Authentication authentication,
                                          HttpServletRequest servletRequest,
                                          HttpServletResponse response) {
        authorization.requireApplicationAccess(authentication);
        try {
            var authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("google")
                    .principal(authentication)
                    .attribute(HttpServletRequest.class.getName(), servletRequest)
                    .attribute(HttpServletResponse.class.getName(), response)
                    .build();
            authorizedClients.authorize(authorizeRequest);
            return ResponseEntity.noContent().build();
        } catch (OAuth2AuthorizationException exception) {
            return ResponseEntity.noContent().build();
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}
