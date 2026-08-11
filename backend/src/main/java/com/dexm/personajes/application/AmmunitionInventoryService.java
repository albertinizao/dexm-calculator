package com.dexm.personajes.application;

import com.dexm.personajes.adapter.in.web.CharacterController.AmmunitionRequest;
import com.dexm.personajes.adapter.out.persistence.AmmunitionEntity;
import com.dexm.personajes.adapter.out.persistence.AmmunitionRepository;
import com.dexm.personajes.adapter.out.persistence.CharacterRepository;
import com.dexm.personajes.adapter.out.persistence.WeaponCatalogRepository;
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

    public AmmunitionInventoryService(CharacterRepository characters, AmmunitionRepository ammunition,
                                      WeaponCatalogRepository weaponCatalog, WeaponRepository weapons) {
        this.characters = characters;
        this.ammunition = ammunition;
        this.weaponCatalog = weaponCatalog;
        this.weapons = weapons;
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
        weaponCatalog.findAll().stream()
                .filter(item -> item.isOfficial())
                .map(item -> item.getCaliber() == null ? "" : item.getCaliber().trim())
                .filter(value -> !value.isBlank())
                .forEach(result::add);
        weapons.findByCharacterIdOrderBySlotAsc(characterId).stream()
                .map(item -> item.getCaliber() == null ? "" : item.getCaliber().trim())
                .filter(value -> !value.isBlank())
                .forEach(result::add);
        return result.stream().toList();
    }

    @Transactional
    public Map<String, Object> create(String characterId, AmmunitionRequest request) {
        ensureCharacter(characterId);
        String caliber = normalizeCaliber(request);
        ensureAllowedCaliber(characterId, caliber);
        int quantity = positiveQuantity(request.quantity());
        var existing = ammunition.findByCharacterIdAndCaliberForUpdate(characterId, caliber);
        AmmunitionEntity entity = existing.orElseGet(() -> new AmmunitionEntity(UUID.randomUUID().toString(), characterId, caliber, 0));
        entity.setQuantity(Math.addExact(entity.getQuantity(), quantity));
        return view(ammunition.save(entity));
    }

    @Transactional
    public Map<String, Object> update(String characterId, String id, AmmunitionRequest request) {
        ensureCharacter(characterId);
        String caliber = normalizeCaliber(request);
        ensureAllowedCaliber(characterId, caliber);
        int quantity = positiveQuantity(request.quantity());
        var entity = ammunition.findByIdAndCharacterIdForUpdate(id, characterId)
                .orElseThrow(() -> new NoSuchElementException("Munición no encontrada"));
        ammunition.findByCharacterIdAndCaliberForUpdate(characterId, caliber)
                .filter(existing -> !existing.getId().equals(entity.getId()))
                .ifPresent(existing -> { throw new IllegalArgumentException("Ya existe munición de ese calibre"); });
        entity.setCaliber(caliber);
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
    public Map<String, Object> reload(String characterId, String weaponId) {
        ensureCharacter(characterId);
        var weapon = weapons.findByIdAndCharacterIdForUpdate(weaponId, characterId)
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

    private String normalizeCaliber(AmmunitionRequest request) {
        if (request == null || request.caliber() == null || request.caliber().isBlank()) {
            throw new IllegalArgumentException("Calibre obligatorio");
        }
        return request.caliber().trim();
    }

    private int positiveQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) throw new IllegalArgumentException("La cantidad debe ser positiva");
        return quantity;
    }

    private void ensureAllowedCaliber(String characterId, String caliber) {
        if (!calibers(characterId).contains(caliber)) {
            throw new IllegalArgumentException("Calibre no disponible para este personaje");
        }
    }

    private void ensureCharacter(String id) {
        if (!characters.existsById(id)) throw new NoSuchElementException("Personaje no encontrado");
    }

    private Map<String, Object> view(AmmunitionEntity item) {
        var result = new LinkedHashMap<String, Object>();
        result.put("id", item.getId());
        result.put("caliber", item.getCaliber());
        result.put("quantity", item.getQuantity());
        return result;
    }
}
