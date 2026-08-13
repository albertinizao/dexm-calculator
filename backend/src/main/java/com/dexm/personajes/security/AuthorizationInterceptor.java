package com.dexm.personajes.security;

import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class AuthorizationInterceptor implements WebMvcConfigurer {
    private final AuthorizationService authorization;
    public AuthorizationInterceptor(AuthorizationService authorization){this.authorization=authorization;}
    @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new HandlerInterceptor(){
        @Override public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response,Object handler){
            String path=request.getRequestURI();
            if(path.startsWith("/api/") && !path.startsWith("/api/auth/"))
                authorization.requireApplicationAccess(SecurityContextHolder.getContext().getAuthentication());
            if(path.startsWith("/api/characters/")){
                String remainder=path.substring("/api/characters/".length()); String id=remainder.split("/",2)[0];
                if(!id.isBlank()) authorization.requireCharacter(SecurityContextHolder.getContext().getAuthentication(),id, !"GET".equalsIgnoreCase(request.getMethod()));
            }
            return true;
        }
    });}
}
