package com.dexm.personajes.adapter.in.web;
import com.dexm.personajes.application.AbilityCatalogService; import com.fasterxml.jackson.databind.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/abilities") public class AbilityController { private final AbilityCatalogService service; public AbilityController(AbilityCatalogService s){service=s;}
 @PostMapping(value="/import",consumes=MediaType.APPLICATION_JSON_VALUE) public Object importJson(@RequestBody JsonNode json){return java.util.Map.of("imported",service.merge(json));}
 @GetMapping public Object list(){return service.list();}
 @GetMapping(value="/export",produces=MediaType.APPLICATION_JSON_VALUE) public ResponseEntity<JsonNode> export(){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=CompendioHabilidadesExport.json").body(service.exportJson());}
}
