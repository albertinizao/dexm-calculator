package com.dexm.personajes.application;

import com.dexm.personajes.adapter.out.persistence.AbilityEntity;
import com.dexm.personajes.adapter.out.persistence.WeaponCatalogEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/** Immutable official catalog loaded once from the application resources. */
@Service
public class OfficialCatalogService {
    private final ObjectMapper mapper;
    private final List<AbilityEntity> abilities;
    private final List<WeaponCatalogEntity> weapons;
    private final Map<String, JsonNode> grenades;
    private final Map<String, JsonNode> armors;
    private final Map<String, JsonNode> physicalShields;

    public OfficialCatalogService(ObjectMapper mapper) {
        this.mapper = mapper;
        try {
            this.abilities = loadAbilities();
            JsonNode objects = mapper.readTree(new ClassPathResource("catalog/objects.v1.json").getInputStream());
            this.weapons = loadWeapons(objects.path("weapons"));
            this.grenades = new LinkedHashMap<>();
            objects.path("grenades").forEach(node -> grenades.put(node.path("id").asText(), node));
            this.armors = indexById(objects.path("armors"));
            this.physicalShields = indexById(objects.path("physicalShields"));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load official static catalogs", e);
        }
    }

    public List<AbilityEntity> abilities() { return abilities; }
    public Optional<AbilityEntity> ability(String name) { return abilities.stream().filter(a -> a.getName().equals(name)).findFirst(); }
    public List<WeaponCatalogEntity> weapons() { return weapons; }
    public Optional<WeaponCatalogEntity> weapon(String id) { return weapons.stream().filter(w -> w.getId().equals(id)).findFirst(); }
    public Optional<JsonNode> grenade(String id) { return Optional.ofNullable(grenades.get(id)); }
    public List<JsonNode> grenades() { return List.copyOf(grenades.values()); }
    public List<JsonNode> armors() { return List.copyOf(armors.values()); }
    public Optional<JsonNode> armor(String id) { return Optional.ofNullable(armors.get(id)); }
    public List<JsonNode> physicalShields() { return List.copyOf(physicalShields.values()); }
    public Optional<JsonNode> physicalShield(String id) { return Optional.ofNullable(physicalShields.get(id)); }

    private Map<String, JsonNode> indexById(JsonNode root) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        root.forEach(node -> result.put(node.path("id").asText(), node));
        return result;
    }

    private List<AbilityEntity> loadAbilities() throws IOException {
        JsonNode root = mapper.readTree(new ClassPathResource("catalog/abilities.v1.json").getInputStream());
        List<AbilityEntity> result = new ArrayList<>();
        for (JsonNode node : root) {
            String name = node.path("Nombre").asText("").trim();
            if (name.isEmpty()) continue;
            result.add(new AbilityEntity("official-" + Integer.toHexString(name.hashCode()), name,
                    node.path("Descripcion").asText(null), node.path("Lanzamiento").asText(null),
                    node.has("Coste") ? node.path("Coste").asInt() : null,
                    node.path("Unica").asText(null), mapper.createArrayNode().add(node).toString()));
        }
        return List.copyOf(result);
    }

    private List<WeaponCatalogEntity> loadWeapons(JsonNode root) {
        List<WeaponCatalogEntity> result = new ArrayList<>();
        root.forEach(n -> result.add(new WeaponCatalogEntity(n.path("id").asText(), n.path("name").asText(),
                n.path("weaponType").asText(), n.path("size").asText(), decimal(n,"range"), decimal(n,"reload"),
                n.path("rate").asText(), decimal(n,"damageVital"), decimal(n,"damageNormal"), decimal(n,"damageLight"),
                decimal(n,"damageVeryLight"), decimalOrNull(n,"aim"), n.path("automaticFire").asText(null), decimal(n,"capacity"),
                n.path("caliber").asText(null), n.path("extraRule").asText(null), n.path("imageUrl").asText(null), true)));
        return List.copyOf(result);
    }
    private BigDecimal decimal(JsonNode n, String key) { return n.path(key).isNumber() ? n.path(key).decimalValue() : BigDecimal.ZERO; }
    private BigDecimal decimalOrNull(JsonNode n, String key) { return n.path(key).isNumber() ? n.path(key).decimalValue() : null; }
}
