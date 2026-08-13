package com.dexm.personajes.application;

import com.dexm.personajes.adapter.out.persistence.WeaponCatalogEntity;
import com.dexm.personajes.adapter.out.persistence.WeaponCatalogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WeaponCatalogSeedService {
    private static final String RESOURCE = "catalog/official-weapon-catalog.json";

    private final WeaponCatalogRepository catalog;
    private final ObjectMapper mapper;

    public WeaponCatalogSeedService(WeaponCatalogRepository catalog, ObjectMapper mapper) {
        this.catalog = catalog;
        this.mapper = mapper;
    }

    public void seedIfEmpty() {
        try (var input = new ClassPathResource(RESOURCE).getInputStream()) {
            var seeds = mapper.readValue(input, new TypeReference<List<WeaponSeed>>() { });
            Set<String> existingIds = catalog.findAll().stream()
                    .map(WeaponCatalogEntity::getId)
                    .collect(Collectors.toSet());
            var missing = seeds.stream()
                    .filter(seed -> !existingIds.contains(seed.id()))
                    .map(WeaponSeed::toEntity)
                    .toList();
            if (!missing.isEmpty()) catalog.saveAll(missing);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo cargar el catálogo oficial de armas", exception);
        }
    }

    private record WeaponSeed(
            String id,
            String name,
            String weaponType,
            String size,
            BigDecimal range,
            BigDecimal reload,
            String rate,
            BigDecimal damageVital,
            BigDecimal damageNormal,
            BigDecimal damageLight,
            BigDecimal damageVeryLight,
            BigDecimal aim,
            String automaticFire,
            BigDecimal capacity,
            String caliber,
            String extraRule,
            String imageUrl,
            boolean official
    ) {
        private WeaponCatalogEntity toEntity() {
            return new WeaponCatalogEntity(id, name, weaponType, size, range, reload, rate, damageVital,
                    damageNormal, damageLight, damageVeryLight, aim, automaticFire, capacity, caliber,
                    extraRule, imageUrl, official);
        }
    }
}
