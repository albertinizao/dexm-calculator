package com.dexm.personajes.adapter.in.web;

import com.dexm.personajes.application.WeaponCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.dexm.personajes.security.AuthorizationService;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/weapon-catalog")
public class WeaponCatalogController {
    private final WeaponCatalogService catalog; private final AuthorizationService authorization;
    public WeaponCatalogController(WeaponCatalogService catalog, AuthorizationService authorization) { this.catalog=catalog; this.authorization=authorization; }
    @GetMapping public Object search(@RequestParam(required=false) String slot, @RequestParam(required=false) String name, @RequestParam(required=false) String type) { return catalog.search(slot, name, type); }
    @GetMapping("/custom") public Object custom() { return catalog.custom(); }
    @PostMapping public ResponseEntity<?> createCustom(@Valid @RequestBody CharacterController.WeaponCatalogCreateRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(catalog.createCustom(request)); }
    @PostMapping("/{catalogId}/characters/{characterId}") public Object copyToCharacter(@PathVariable String catalogId, @PathVariable String characterId, @RequestBody CharacterController.WeaponCatalogCopyRequest request) { authorization.requireCharacter(SecurityContextHolder.getContext().getAuthentication(), characterId, true); return catalog.copyToCharacter(catalogId, characterId, request.slot()); }
}
