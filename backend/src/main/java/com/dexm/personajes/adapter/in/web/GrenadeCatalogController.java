package com.dexm.personajes.adapter.in.web;

import com.dexm.personajes.application.GrenadeCatalogService;
import com.dexm.personajes.security.AuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grenade-catalog")
public class GrenadeCatalogController {
    public record GrenadeCatalogRequest(@NotBlank String name, String description,
                                        @NotNull @Min(0) Integer centralDamage,
                                        @NotNull @Min(0) Integer adjacentDamage,
                                        @NotNull @Min(0) Integer damageDecay,
                                        Boolean handGrenade, String type, String additionalEffect) {
        public GrenadeCatalogRequest(String name, String description, Integer centralDamage, Integer adjacentDamage, Integer damageDecay) {
            this(name, description, centralDamage, adjacentDamage, damageDecay, true, null, null);
        }

        public GrenadeCatalogRequest(String name, String description, Integer centralDamage, Integer adjacentDamage,
                                     Integer damageDecay, Boolean handGrenade, String type) {
            this(name, description, centralDamage, adjacentDamage, damageDecay, handGrenade, type, null);
        }
    }

    private final GrenadeCatalogService service;
    private final AuthorizationService authorization;

    public GrenadeCatalogController(GrenadeCatalogService service, AuthorizationService authorization) {
        this.service = service;
        this.authorization = authorization;
    }

    @GetMapping
    public Object list() { return service.list(); }
    @GetMapping("/custom")
    public Object custom() { return service.custom(); }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody GrenadeCatalogRequest request) {
        authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public Object update(@PathVariable String id, @Valid @RequestBody GrenadeCatalogRequest request) {
        authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication());
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication());
        service.delete(id);
    }
}
