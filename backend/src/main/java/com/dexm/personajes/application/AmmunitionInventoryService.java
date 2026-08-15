package com.dexm.personajes.application;

import com.dexm.personajes.adapter.in.web.CharacterController.AmmunitionRequest;
import com.dexm.personajes.adapter.out.persistence.AmmunitionEntity;
import com.dexm.personajes.adapter.out.persistence.AmmunitionRepository;
import com.dexm.personajes.adapter.out.persistence.CharacterRepository;
import com.dexm.personajes.adapter.out.persistence.GrenadeCatalogEntity;
import com.dexm.personajes.adapter.out.persistence.GrenadeCatalogRepository;
import com.dexm.personajes.adapter.out.persistence.WeaponCatalogRepository;
import com.dexm.personajes.adapter.out.persistence.WeaponEntity;
import com.dexm.personajes.adapter.out.persistence.WeaponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class AmmunitionInventoryService {
    private static final Set<Integer> DECREMENT_AMOUNTS = Set.of(-1, -5, -10);

    private final CharacterRepository characters;
    private final AmmunitionRepository ammunition;
    private final WeaponCatalogRepository weaponCatalog;
    private final WeaponRepository weapons;
    private final GrenadeCatalogRepository grenades;
    private final OfficialCatalogService officialCatalog;

    public AmmunitionInventoryService(CharacterRepository characters, AmmunitionRepository ammunition,
                                      WeaponCatalogRepository weaponCatalog, WeaponRepository weapons) {
        this(characters, ammunition, weaponCatalog, weapons, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AmmunitionInventoryService(CharacterRepository characters, AmmunitionRepository ammunition,
                                      WeaponCatalogRepository weaponCatalog, WeaponRepository weapons,
                            GrenadeCatalogRepository grenades) {
        this(characters, ammunition, weaponCatalog, weapons, grenades, null);
    }
    public AmmunitionInventoryService(CharacterRepository characters, AmmunitionRepository ammunition,
                                      WeaponCatalogRepository weaponCatalog, WeaponRepository weapons,
                                      GrenadeCatalogRepository grenades, OfficialCatalogService officialCatalog) {
        this.characters = characters;
        this.ammunition = ammunition;
        this.weaponCatalog = weaponCatalog;
        this.weapons = weapons;
        this.grenades = grenades;
        this.officialCatalog = officialCatalog;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String characterId) {
        ensureCharacter(characterId);
        return ammunition.findByCharacterIdOrderByCaliberAsc(characterId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<String> calibers(String characterId) {
        ensureCharacter(characterId);
        var result = new TreeSet<String>(Comparator.comparing(value -> value.toLowerCase(Locale.ROOT)));
        ammunition.findByCharacterIdOrderByCaliberAsc(characterId).stream()
                .map(item -> item.getCaliber() == null ? "" : item.getCaliber().trim())
                .filter(value -> !value.isBlank())
                .forEach(result::add);
        weapons.findByCharacterIdOrderBySlotAsc(characterId).stream()
                .map(item -> item.getCaliber() == null ? "" : item.getCaliber().trim())
                .filter(value -> !value.isBlank())
                .forEach(result::add);
        if (officialCatalog != null) {
            officialCatalog.weapons().stream()
                    .map(item -> item.getCaliber() == null ? "" : item.getCaliber().trim())
                    .filter(value -> !value.isBlank())
                    .forEach(result::add);
        }
        return result.stream().toList();
    }

    @Transactional
    public Map<String, Object> create(String characterId, AmmunitionRequest request) {
        ensureCharacter(characterId);
        String type = normalizeType(request);
        String caliber = normalizeCaliber(request, type);
        String grenadeCatalogId = normalizeGrenadeCatalogId(request, type);
        int quantity = positiveQuantity(request.quantity());
        ensureAllowed(characterId, type, caliber, grenadeCatalogId);
        var existing = findForUpdate(characterId, type, caliber, grenadeCatalogId);
        AmmunitionEntity entity = existing.orElseGet(() -> new AmmunitionEntity(UUID.randomUUID().toString(), characterId, type, caliber, grenadeCatalogId, 0));
        entity.setType(type);
        entity.setCaliber(caliber);
        entity.setGrenadeCatalogId(grenadeCatalogId);
        entity.setQuantity(Math.addExact(entity.getQuantity(), quantity));
        return view(ammunition.save(entity));
    }

    @Transactional
    public Map<String, Object> update(String characterId, String id, AmmunitionRequest request) {
        ensureCharacter(characterId);
        String type = normalizeType(request);
        String caliber = normalizeCaliber(request, type);
        String grenadeCatalogId = normalizeGrenadeCatalogId(request, type);
        ensureAllowed(characterId, type, caliber, grenadeCatalogId);
        int quantity = positiveQuantity(request.quantity());
        var entity = ammunition.findByIdAndCharacterIdForUpdate(id, characterId)
                .orElseThrow(() -> new NoSuchElementException("Munición no encontrada"));
        findForUpdate(characterId, type, caliber, grenadeCatalogId)
                .filter(existing -> !existing.getId().equals(entity.getId()))
                .ifPresent(existing -> { throw new IllegalArgumentException("Ya existe munición de ese calibre"); });
        entity.setType(type);
        entity.setCaliber(caliber);
        entity.setGrenadeCatalogId(grenadeCatalogId);
        entity.setQuantity(quantity);
        return view(ammunition.save(entity));
    }

    @Transactional
    public Map<String, Object> decrement(String characterId, String id, Integer amount) {
        ensureCharacter(characterId);
        if (amount == null || !DECREMENT_AMOUNTS.contains(amount)) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser -1, -5 o -10");
        }
        var entity = ammunition.findByIdAndCharacterIdForUpdate(id, characterId)
                .orElseThrow(() -> new NoSuchElementException("Munición no encontrada"));
        if (entity.getQuantity() < Math.abs(amount)) {
            throw new IllegalArgumentException("No hay suficiente munición");
        }
        int remaining = entity.getQuantity() + amount;
        if (remaining == 0) {
            ammunition.delete(entity);
            return null;
        }
        entity.setQuantity(remaining);
        return view(ammunition.save(entity));
    }

    @Transactional
    public Map<String, Object> consumeGrenade(String characterId, String ammunitionId) {
        ensureCharacter(characterId);
        var candidate = ammunition.findByIdAndCharacterIdForUpdate(ammunitionId, characterId)
                .orElseThrow(() -> new NoSuchElementException("Granada no encontrada"));
        if (!"GRENADE".equals(candidate.getType())) throw new IllegalArgumentException("La munición no es una granada");
        ensureHandGrenade(candidate.getGrenadeCatalogId());
        var consumedAtomically = ammunition.consumeOneGrenade(ammunitionId, characterId);
        if (consumedAtomically != null) return view(consumedAtomically);
        var entity = ammunition.findByIdAndCharacterIdForUpdate(ammunitionId, characterId)
                .orElseThrow(() -> new NoSuchElementException("Granada no encontrada"));
        if (!"GRENADE".equals(entity.getType())) throw new IllegalArgumentException("La munición no es una granada");
        ensureHandGrenade(entity.getGrenadeCatalogId());
        if (entity.getQuantity() < 1) throw new IllegalArgumentException("No quedan granadas disponibles");
        entity.setQuantity(entity.getQuantity() - 1);
        if (entity.getQuantity() == 0) {
            ammunition.delete(entity);
            return view(entity);
        }
        return view(ammunition.save(entity));
    }

    @Transactional
    public Map<String, Object> consumeGrenadeByCatalog(String characterId, String grenadeCatalogId) {
        ensureCharacter(characterId);
        ensureHandGrenade(grenadeCatalogId);
        var entity = ammunition.findByCharacterIdAndTypeAndGrenadeCatalogIdForUpdate(characterId, "GRENADE", grenadeCatalogId)
                .orElseThrow(() -> new NoSuchElementException("Granada no encontrada en el inventario"));
        return consumeGrenade(characterId, entity.getId());
    }

    @Transactional
    public Map<String, Object> reload(String characterId, String weaponId) {
        ensureCharacter(characterId);
        var weapon = weaponForCharacter(characterId, weaponId)
                .orElseThrow(() -> new NoSuchElementException("Arma no encontrada"));
        int capacity = integralPositiveCapacity(weapon.getCapacity());
        String caliber = normalizeWeaponCaliber(weapon.getCaliber());
        var entity = ammunition.findByCharacterIdAndCaliberForUpdate(characterId, caliber)
                .orElse(null);
        int available = entity == null ? 0 : Math.max(0, entity.getQuantity());
        int loaded = loadedBullets(weapon.getLoadedBullets(), capacity);
        int needed = capacity - loaded;
        int consumed = Math.min(needed, available);
        int remaining = available - consumed;
        int newLoaded = Math.min(capacity, loaded + consumed);
        int missing = capacity - newLoaded;
        if (entity != null) {
            if (remaining == 0) {
                ammunition.delete(entity);
            } else {
                entity.setQuantity(remaining);
                ammunition.save(entity);
            }
        }
        weapon.setLoadedBullets(BigDecimal.valueOf(newLoaded));
        weapons.save(weapon);
        return Map.of("weaponId", weapon.getId(), "caliber", caliber, "requested", capacity,
                "consumed", consumed, "remaining", remaining, "missing", missing, "complete", missing == 0,
                "loadedBullets", newLoaded);
    }

    private int loadedBullets(BigDecimal loaded, int capacity) {
        if (loaded == null) return 0;
        try {
            int value = loaded.toBigIntegerExact().intValueExact();
            if (value < 0 || value > capacity) throw new IllegalArgumentException("Las balas cargadas deben estar entre 0 y la capacidad del arma");
            return value;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Las balas cargadas deben ser un número entero entre 0 y la capacidad del arma", exception);
        }
    }

    private int integralPositiveCapacity(BigDecimal capacity) {
        if (capacity == null) throw new IllegalArgumentException("La capacidad del arma debe ser un entero positivo");
        try {
            int value = capacity.toBigIntegerExact().intValueExact();
            if (value < 1) throw new IllegalArgumentException("La capacidad del arma debe ser un entero positivo");
            return value;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("La capacidad del arma debe ser un entero positivo", exception);
        }
    }

    private String normalizeWeaponCaliber(String caliber) {
        if (caliber == null || caliber.isBlank()) {
            throw new IllegalArgumentException("El arma no tiene un calibre configurado");
        }
        return caliber.trim();
    }

    private String normalizeType(AmmunitionRequest request) {
        String value = request == null || request.type() == null || request.type().isBlank() ? "CALIBER" : request.type().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("CALIBER", "GRENADE").contains(value)) throw new IllegalArgumentException("Tipo de munición no válido");
        return value;
    }

    private String normalizeCaliber(AmmunitionRequest request, String type) {
        if ("GRENADE".equals(type)) return null;
        if (request == null || request.caliber() == null || request.caliber().isBlank()) throw new IllegalArgumentException("Calibre obligatorio");
        return request.caliber().trim();
    }

    private String normalizeGrenadeCatalogId(AmmunitionRequest request, String type) {
        if (!"GRENADE".equals(type)) return null;
        if (request == null || request.grenadeCatalogId() == null || request.grenadeCatalogId().isBlank()) throw new IllegalArgumentException("Granada de catálogo obligatoria");
        return request.grenadeCatalogId().trim();
    }

    private int positiveQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) throw new IllegalArgumentException("La cantidad debe ser positiva");
        return quantity;
    }

    private void ensureAllowed(String characterId, String type, String caliber, String grenadeCatalogId) {
        if ("GRENADE".equals(type)) {
            if (grenades == null) throw new IllegalStateException("El catálogo de granadas no está disponible");
            if (officialCatalog == null || officialCatalog.grenade(grenadeCatalogId).isEmpty()) {
                if (grenades == null) throw new NoSuchElementException("Granada de catálogo no encontrada");
                grenades.findById(grenadeCatalogId).orElseThrow(() -> new NoSuchElementException("Granada de catálogo no encontrada"));
            }
            return;
        }
        // Carrying ammunition is independent from owning a compatible weapon.
        // The character may keep any valid caliber in their inventory for
        // future use, trade, or another character; weapon compatibility is
        // only relevant when reloading or shooting.
    }

    private java.util.Optional<AmmunitionEntity> findForUpdate(String characterId, String type, String caliber, String grenadeCatalogId) {
        return "GRENADE".equals(type)
                ? ammunition.findByCharacterIdAndTypeAndGrenadeCatalogIdForUpdate(characterId, type, grenadeCatalogId)
                : ammunition.findByCharacterIdAndCaliberForUpdate(characterId, caliber);
    }

    private void ensureCharacter(String id) {
        if (!characters.existsById(id)) throw new NoSuchElementException("Personaje no encontrado");
    }

    private Map<String, Object> view(AmmunitionEntity item) {
        var result = new LinkedHashMap<String, Object>();
        result.put("id", item.getId());
        result.put("type", item.getType() == null ? "CALIBER" : item.getType());
        result.put("caliber", item.getCaliber());
        result.put("grenadeCatalogId", item.getGrenadeCatalogId());
        if (item.getGrenadeCatalogId() != null) {
            var official = officialCatalog == null ? Optional.<com.fasterxml.jackson.databind.JsonNode>empty() : officialCatalog.grenade(item.getGrenadeCatalogId());
            if (official.isPresent()) result.put("grenade", officialGrenadeView(official.get()));
            else if (grenades != null) grenades.findById(item.getGrenadeCatalogId()).ifPresent(grenade -> result.put("grenade", grenadeView(grenade)));
        }
        result.put("quantity", item.getQuantity());
        return result;
    }

    private Map<String, Object> grenadeView(GrenadeCatalogEntity item) {
        var result = new LinkedHashMap<String, Object>();
        result.put("id", item.getId());
        result.put("name", item.getName());
        result.put("description", item.getDescription() == null ? "" : item.getDescription());
        result.put("centralDamage", item.getCentralDamage());
        result.put("adjacentDamage", item.getAdjacentDamage());
        result.put("damageDecay", item.getDamageDecay());
        result.put("additionalEffect", item.getAdditionalEffect());
        result.put("handGrenade", item.isHandGrenade());
        result.put("type", item.getType());
        result.put("official", item.isOfficial());
        return result;
    }

    private void ensureHandGrenade(String grenadeCatalogId) {
        if (grenades == null) throw new IllegalStateException("El catálogo de granadas no está disponible");
        if (grenadeCatalogId == null || grenadeCatalogId.isBlank()) throw new IllegalArgumentException("La granada no tiene catálogo asociado");
        var official = officialCatalog == null ? Optional.<com.fasterxml.jackson.databind.JsonNode>empty() : officialCatalog.grenade(grenadeCatalogId);
        if (official.isPresent()) { if (!official.get().path("handGrenade").asBoolean()) throw new IllegalArgumentException("Esta granada necesita un arma y no admite lanzamiento directo"); return; }
        var grenade = grenades.findById(grenadeCatalogId).orElseThrow(() -> new NoSuchElementException("Granada de catálogo no encontrada"));
        if (!grenade.isHandGrenade()) throw new IllegalArgumentException("Esta granada necesita un arma y no admite lanzamiento directo");
    }

    /**
     * Inventory aggregates are filtered in memory after reading the single
     * character document. Keep the direct derived query as the fast path, but
     * fall back to the already-supported character inventory listing while
     * older aggregate documents are being migrated. This also covers weapons
     * whose aggregate representation predates the ForUpdate repository method.
     */
    private Optional<WeaponEntity> weaponForCharacter(String characterId, String weaponId) {
        var direct = weapons.findByIdAndCharacterIdForUpdate(weaponId, characterId);
        if (direct.isPresent()) return direct;
        return weapons.findByCharacterIdOrderBySlotAsc(characterId).stream()
                .filter(weapon -> weaponId.equals(weapon.getId()))
                .findFirst();
    }

    private Map<String,Object> officialGrenadeView(com.fasterxml.jackson.databind.JsonNode n) {
        var result = new LinkedHashMap<String,Object>(); result.put("id", n.path("id").asText()); result.put("name", n.path("name").asText());
        result.put("description", n.path("description").asText(null)); result.put("centralDamage", n.path("centralDamage").asInt());
        result.put("adjacentDamage", n.path("adjacentDamage").asInt()); result.put("damageDecay", n.path("damageDecay").asInt());
        result.put("additionalEffect", n.path("additionalEffect").asText(null)); result.put("handGrenade", n.path("handGrenade").asBoolean());
        result.put("type", n.path("type").asText(null)); result.put("official", true); return result;
    }
}
