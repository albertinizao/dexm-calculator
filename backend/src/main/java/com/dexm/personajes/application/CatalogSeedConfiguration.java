package com.dexm.personajes.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
@Profile("!test")
public class CatalogSeedConfiguration {
    @Bean
    CommandLineRunner seedCatalog(AbilityCatalogService abilityCatalog, WeaponCatalogSeedService weaponCatalog,
                                  ObjectMapper mapper) {
        return args -> {
            seedAbilitiesIfEmpty(abilityCatalog, mapper);
            weaponCatalog.seedIfEmpty();
        };
    }

    private void seedAbilitiesIfEmpty(AbilityCatalogService service, ObjectMapper mapper) {
        if (!service.isEmpty() || !new ClassPathResource("CompendioHabilidadesExport.json").exists()) return;
        try {
            var resource = new ClassPathResource("CompendioHabilidadesExport.json");
            service.merge(mapper.readTree(resource.getInputStream()));
        } catch (IOException ignored) {
            // Preserve the existing ability-seed behavior when the optional resource cannot be read.
        }
    }
}
