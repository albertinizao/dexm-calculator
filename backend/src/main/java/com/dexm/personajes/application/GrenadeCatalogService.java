package com.dexm.personajes.application;

import com.dexm.personajes.adapter.out.persistence.GrenadeCatalogEntity;
import com.dexm.personajes.adapter.out.persistence.GrenadeCatalogRepository;
import com.dexm.personajes.adapter.in.web.GrenadeCatalogController.GrenadeCatalogRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class GrenadeCatalogService {
    private final GrenadeCatalogRepository catalog; private final OfficialCatalogService official;

    public GrenadeCatalogService(GrenadeCatalogRepository catalog) { this(catalog, null); }
    @Autowired public GrenadeCatalogService(GrenadeCatalogRepository catalog, OfficialCatalogService official) { this.catalog = catalog; this.official = official; }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        List<Map<String,Object>> result = new java.util.ArrayList<>();
        if (official != null) official.grenades().stream().map(this::view).forEach(result::add);
        var stored = catalog.findAll().stream().filter(item -> official == null || !item.isOfficial()).sorted(Comparator.comparing(GrenadeCatalogEntity::getName, String.CASE_INSENSITIVE_ORDER)).map(this::view).toList();
        result.addAll(stored);
        return result;
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> custom() { return catalog.findAll().stream().filter(item -> !item.isOfficial()).sorted(Comparator.comparing(GrenadeCatalogEntity::getName, String.CASE_INSENSITIVE_ORDER)).map(this::view).toList(); }

    @Transactional
    public Map<String, Object> create(GrenadeCatalogRequest request) {
        validate(request);
        boolean handGrenade = handGrenade(request);
        return view(catalog.save(new GrenadeCatalogEntity(UUID.randomUUID().toString(), request.name().trim(), clean(request.description()),
                request.centralDamage(), request.adjacentDamage(), request.damageDecay(), clean(request.additionalEffect()), handGrenade,
                handGrenade ? null : clean(request.type()), false)));
    }

    @Transactional
    public Map<String, Object> update(String id, GrenadeCatalogRequest request) {
        validate(request);
        var entity = catalog.findById(id).orElseThrow(() -> new NoSuchElementException("Granada de catálogo no encontrada"));
        entity.setName(request.name().trim());
        entity.setDescription(clean(request.description()));
        entity.setCentralDamage(request.centralDamage());
        entity.setAdjacentDamage(request.adjacentDamage());
        entity.setDamageDecay(request.damageDecay());
        entity.setAdditionalEffect(clean(request.additionalEffect()));
        boolean handGrenade = handGrenade(request);
        entity.setHandGrenade(handGrenade);
        entity.setType(handGrenade ? null : clean(request.type()));
        return view(catalog.save(entity));
    }

    @Transactional
    public void delete(String id) {
        var entity = catalog.findById(id).orElseThrow(() -> new NoSuchElementException("Granada de catálogo no encontrada"));
        if (entity.isOfficial()) throw new IllegalArgumentException("Las granadas oficiales no se pueden borrar");
        catalog.delete(entity);
    }

    public Map<String, Object> view(GrenadeCatalogEntity entity) {
        var result = new LinkedHashMap<String, Object>();
        result.put("id", entity.getId());
        result.put("name", entity.getName());
        result.put("description", entity.getDescription());
        result.put("centralDamage", entity.getCentralDamage());
        result.put("adjacentDamage", entity.getAdjacentDamage());
        result.put("damageDecay", entity.getDamageDecay());
        result.put("additionalEffect", entity.getAdditionalEffect());
        result.put("handGrenade", entity.isHandGrenade());
        result.put("type", entity.getType());
        result.put("official", entity.isOfficial());
        return result;
    }

    private Map<String,Object> view(JsonNode n) {
        var result = new LinkedHashMap<String,Object>();
        result.put("id", n.path("id").asText()); result.put("name", n.path("name").asText());
        result.put("description", n.path("description").isNull() ? null : n.path("description").asText(null));
        result.put("centralDamage", n.path("centralDamage").asInt()); result.put("adjacentDamage", n.path("adjacentDamage").asInt());
        result.put("damageDecay", n.path("damageDecay").asInt()); result.put("additionalEffect", n.path("additionalEffect").asText(null));
        result.put("handGrenade", n.path("handGrenade").asBoolean()); result.put("type", n.path("type").asText(null)); result.put("official", true);
        return result;
    }

    private void validate(GrenadeCatalogRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) throw new IllegalArgumentException("El nombre de la granada es obligatorio");
        if (request.centralDamage() == null || request.adjacentDamage() == null || request.damageDecay() == null
                || request.centralDamage() < 0 || request.adjacentDamage() < 0 || request.damageDecay() < 0)
            throw new IllegalArgumentException("El daño de la granada no puede ser negativo");
    }

    private boolean handGrenade(GrenadeCatalogRequest request) {
        // Old clients omitted the field while every existing grenade was throwable by hand.
        return request.handGrenade() == null || request.handGrenade();
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
