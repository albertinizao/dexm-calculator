package com.dexm.personajes.adapter.in.web;
import com.dexm.personajes.application.CampaignService; import com.dexm.personajes.application.MinorAttributeService; import jakarta.validation.Valid; import jakarta.validation.constraints.NotBlank; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/campaigns") public class CampaignController {
 private final CampaignService service; private final MinorAttributeService minorAttributes; public CampaignController(CampaignService service,MinorAttributeService minorAttributes){this.service=service;this.minorAttributes=minorAttributes;}
 public record CreateRequest(@NotBlank String name){} public record CharacterRequest(@NotBlank String name,String imageUrl){} public record MemberRequest(@NotBlank String email){}
 @GetMapping public List<?> list(){return service.list();}
 @PostMapping public ResponseEntity<?> create(@Valid @RequestBody CreateRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.name()));}
 @GetMapping("/{id}") public Object get(@PathVariable String id){return service.get(id);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id){service.delete(id);}
 @GetMapping("/{id}/characters") public List<?> characters(@PathVariable String id){return service.characters(id);}
 @PostMapping("/{id}/characters") public ResponseEntity<?> createCharacter(@PathVariable String id,@Valid @RequestBody CharacterRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.createCharacter(id,request.name(),request.imageUrl()));} public record MinorAttributeRequest(String key,@NotBlank String name,@NotBlank String maxFormula,String bonusSource,String type){} @GetMapping("/{id}/minor-attributes") public Object minorAttributes(@PathVariable String id){service.get(id); return minorAttributes.list(id);} @PostMapping("/{id}/minor-attributes") public ResponseEntity<?> createMinorAttribute(@PathVariable String id,@Valid @RequestBody MinorAttributeRequest r){service.get(id); return ResponseEntity.status(HttpStatus.CREATED).body(minorAttributes.create(id,r.key(),r.name(),r.maxFormula(),r.bonusSource(),r.type()));}
 @GetMapping("/{id}/members") public Object members(@PathVariable String id){return service.members(id);}
 @PostMapping("/{id}/members") public ResponseEntity<?> invite(@PathVariable String id,@Valid @RequestBody MemberRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.invite(id,request.email()));}
 @DeleteMapping("/{id}/members/{email}") @ResponseStatus(HttpStatus.NO_CONTENT) public void revoke(@PathVariable String id,@PathVariable String email){service.revoke(id,email);}
}
