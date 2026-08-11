package com.dexm.personajes.adapter.in.web;

import com.dexm.personajes.security.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final SecurityIdentityService identities;
    private final AuthorizationService authorization;
    public AuthController(SecurityIdentityService identities, AuthorizationService authorization){this.identities=identities;this.authorization=authorization;}
    @GetMapping("/me")
    public Map<String,Object> me(Authentication authentication){
        var identity=identities.current(authentication);
        authorization.requireApplicationAccess(authentication);
        var user=identities.provision(authentication);
        return Map.of("id",user.getId(),"email",identity.email(),"name",identity.name(),"admin",identities.isAdmin(identity));
    }
}
