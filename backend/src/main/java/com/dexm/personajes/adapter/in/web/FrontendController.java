package com.dexm.personajes.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;

/** Serves the Vue entry point for browser history routes in the production SPA. */
@Controller
public class FrontendController {
    @GetMapping("/characters/**")
    public String characterRoute(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Never turn a missing asset (for example /characters/foo.js) into HTML.
        if (path.substring("/characters/".length()).contains(".")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }
}
