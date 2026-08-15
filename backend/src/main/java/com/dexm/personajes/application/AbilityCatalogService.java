package com.dexm.personajes.application;
import com.dexm.personajes.adapter.out.persistence.*; import com.fasterxml.jackson.databind.*; import com.fasterxml.jackson.databind.node.ArrayNode; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.*;
@Service public class AbilityCatalogService {
 private final AbilityRepository repo; private final ObjectMapper mapper; private final OfficialCatalogService official;
 public AbilityCatalogService(AbilityRepository r,ObjectMapper m){this(r,m,null);}
 @org.springframework.beans.factory.annotation.Autowired public AbilityCatalogService(AbilityRepository r,ObjectMapper m,OfficialCatalogService official){repo=r;mapper=m;this.official=official;}
 public boolean isEmpty(){return official != null ? official.abilities().isEmpty() : repo.count()==0;}
 public List<AbilityEntity> list(){return official != null ? official.abilities() : repo.findAll();}
 @Transactional public int merge(JsonNode root){
  if(!root.isArray()) throw new IllegalArgumentException("JSON must be an array");
  // Load the existing catalog once. The manual sync is allowed to repair partial data,
  // but it must not issue one findByName query for every catalog entry.
  var existingByName=new LinkedHashMap<String,AbilityEntity>();
  repo.findAll().forEach(entity -> existingByName.put(entity.getName(),entity));
  int count=0;
  for(JsonNode n:root){
   String name=n.path("Nombre").asText("").trim();
   if(name.isEmpty())continue;
   try{
    var existing=existingByName.get(name);
    String alternative=mapper.writeValueAsString(n);
    if(existing!=null){
     var arr=(ArrayNode)mapper.readTree(existing.getAlternativesJson());
     boolean found=arr.toString().contains(alternative);
     if(!found){arr.add(n);existing.setAlternativesJson(arr.toString());repo.save(existing);}
    } else {
     var arr=mapper.createArrayNode().add(n);
     var created=new AbilityEntity(UUID.randomUUID().toString(),name,n.path("Descripcion").asText(null),n.path("Lanzamiento").asText(null),n.has("Coste")?n.path("Coste").asInt():null,n.path("Unica").asText(null),arr.toString());
     repo.save(created);existingByName.put(name,created);
    }
    count++;
   }catch(Exception ex){throw new IllegalArgumentException("Invalid ability: "+name,ex);}
  }
  return count;
 }
 public ArrayNode exportJson(){var out=mapper.createArrayNode(); list().forEach(e->{try{for(JsonNode n:mapper.readTree(e.getAlternativesJson()))out.add(n);}catch(Exception ignored){}});return out;}
}
