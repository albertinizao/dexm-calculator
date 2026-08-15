package com.dexm.personajes.adapter.in.web;
import com.dexm.personajes.application.ProtectiveEquipmentService; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController public class ProtectiveCatalogController { private final ProtectiveEquipmentService service; public ProtectiveCatalogController(ProtectiveEquipmentService s){service=s;}
 @GetMapping("/api/armor-catalog") public Object armors(){return service.armorCatalog();}
 @PostMapping("/api/armor-catalog/{id}/characters/{characterId}") public Object addArmor(@PathVariable String id,@PathVariable String characterId){return service.addArmorCatalog(id,characterId);}
 @GetMapping("/api/shield-catalog") public Object shields(){return service.shieldCatalog();}
 @PostMapping("/api/shield-catalog") public ResponseEntity<?> createShield(@Valid @RequestBody CharacterController.ShieldRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.createShieldCatalog(r));}
 @PostMapping("/api/shield-catalog/{id}/characters/{characterId}") public Object addShield(@PathVariable String id,@PathVariable String characterId){return service.addShieldCatalog(id,characterId);}
 @GetMapping("/api/physical-shield-catalog") public Object physicalShields(){return service.physicalShieldCatalog();}
 @PostMapping("/api/physical-shield-catalog/{id}/characters/{characterId}") public Object addPhysicalShield(@PathVariable String id,@PathVariable String characterId){return service.addPhysicalShieldCatalog(id,characterId);}
}
