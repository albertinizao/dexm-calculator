package com.dexm.personajes.application;
import com.fasterxml.jackson.databind.*; import org.springframework.context.annotation.*; import org.springframework.core.io.*; import org.springframework.boot.CommandLineRunner; import java.io.*;
@Configuration public class CatalogSeedConfiguration {
 @Bean CommandLineRunner seedCatalog(AbilityCatalogService service, ObjectMapper mapper){return args->{if(service.isEmpty() && new ClassPathResource("CompendioHabilidadesExport.json").exists()){try{var r=new ClassPathResource("CompendioHabilidadesExport.json");service.merge(mapper.readTree(r.getInputStream()));}catch(IOException ignored){}}};}
}
