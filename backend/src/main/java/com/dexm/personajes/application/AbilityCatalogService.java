package com.dexm.personajes.application;
import com.dexm.personajes.adapter.out.persistence.*; import com.fasterxml.jackson.databind.*; import com.fasterxml.jackson.databind.node.ArrayNode; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.*;
@Service public class AbilityCatalogService {
 private final AbilityRepository repo; private final ObjectMapper mapper; public AbilityCatalogService(AbilityRepository r,ObjectMapper m){repo=r;mapper=m;}
 public boolean isEmpty(){return repo.count()==0;}
 public List<AbilityEntity> list(){return repo.findAll();}
 @Transactional public int merge(JsonNode root){if(!root.isArray()) throw new IllegalArgumentException("JSON must be an array");int count=0; for(JsonNode n:root){String name=n.path("Nombre").asText("").trim();if(name.isEmpty())continue;try{var existing=repo.findByName(name);String alternative=mapper.writeValueAsString(n); if(existing.isPresent()){var e=existing.get();var arr=(ArrayNode)mapper.readTree(e.getAlternativesJson());boolean found=arr.toString().contains(alternative);if(!found)arr.add(n);e.setAlternativesJson(arr.toString());repo.save(e);} else {var arr=mapper.createArrayNode().add(n);repo.save(new AbilityEntity(UUID.randomUUID().toString(),name,n.path("Descripcion").asText(null),n.path("Lanzamiento").asText(null),n.has("Coste")?n.path("Coste").asInt():null,n.path("Unica").asText(null),arr.toString()));}count++;}catch(Exception ex){throw new IllegalArgumentException("Invalid ability: "+name,ex);}}return count;}
 public ArrayNode exportJson(){var out=mapper.createArrayNode();repo.findAll().forEach(e->{try{for(JsonNode n:mapper.readTree(e.getAlternativesJson()))out.add(n);}catch(Exception ignored){}});return out;}
}
