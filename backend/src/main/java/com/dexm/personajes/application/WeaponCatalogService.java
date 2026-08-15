package com.dexm.personajes.application;

import com.dexm.personajes.adapter.in.web.CharacterController.WeaponCatalogCreateRequest;
import com.dexm.personajes.adapter.out.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.util.*;

@Service
public class WeaponCatalogService {
    private final WeaponCatalogRepository catalog; private final WeaponRepository weapons; private final CharacterRepository characters; private final OfficialCatalogService official;
    public WeaponCatalogService(WeaponCatalogRepository catalog) { this(catalog, null, null, null); }
    public WeaponCatalogService(WeaponCatalogRepository catalog, WeaponRepository weapons, CharacterRepository characters) { this(catalog, weapons, characters, null); }
    @Autowired public WeaponCatalogService(WeaponCatalogRepository catalog, WeaponRepository weapons, CharacterRepository characters, OfficialCatalogService official) { this.catalog=catalog; this.weapons=weapons; this.characters=characters; this.official=official; }

    @Transactional(readOnly = true) public List<Map<String,Object>> search(String slot, String name, String type) {
        String normalizedName=name==null?"":name.trim().toLowerCase(Locale.ROOT); String normalizedType=type==null?"":type.trim().toUpperCase(Locale.ROOT);
        List<WeaponCatalogEntity> candidates = new ArrayList<>();
        if (official != null) {
            candidates.addAll(official.weapons());
            candidates.addAll(catalog.findAll().stream().filter(item -> !item.isOfficial()).toList());
        } else {
            candidates.addAll(slot == null || slot.isBlank() ? catalog.findAll() : compatibleSizes(slot).stream().flatMap(size -> catalog.findBySize(size).stream()).distinct().toList());
        }
        return candidates.stream().filter(item -> normalizedName.isEmpty() || item.getName().toLowerCase(Locale.ROOT).contains(normalizedName))
            .filter(item -> normalizedType.isEmpty() || item.getWeaponType().equals(normalizedType))
            .filter(item -> slot==null || slot.isBlank() || compatible(item.getSize(), slot)).sorted(Comparator.comparing(WeaponCatalogEntity::getName)).map(this::view).toList();
    }
    @Transactional(readOnly = true) public List<Map<String,Object>> custom() { return catalog.findAll().stream().filter(item -> !item.isOfficial()).sorted(Comparator.comparing(WeaponCatalogEntity::getName)).map(this::view).toList(); }
    @Transactional public Map<String,Object> createCustom(WeaponCatalogCreateRequest request) {
        validate(request); var entity=new WeaponCatalogEntity(UUID.randomUUID().toString(), request.name().trim(), request.weaponType(), request.size(), request.range(), request.reload(), request.rate().trim(), request.damageVital(), request.damageNormal(), request.damageLight(), request.damageVeryLight(), request.aim(), clean(request.automaticFire()), (isMelee(request.weaponType()) ? BigDecimal.ONE : request.capacity()), (isMelee(request.weaponType()) ? null : clean(request.caliber())), clean(request.extraRule()), imageData(request.imageUrl()), false);
        return view(catalog.save(entity));
    }
    @Transactional public WeaponEntity copyToCharacter(String catalogId, String characterId, String slot) {
        if(weapons==null || characters==null) throw new IllegalStateException("Servicio de inventario no disponible");
        if(!characters.existsById(characterId)) throw new NoSuchElementException("Personaje no encontrado");
        var item=(official == null ? Optional.<WeaponCatalogEntity>empty() : official.weapon(catalogId))
                .or(() -> catalog.findById(catalogId)).orElseThrow(()->new NoSuchElementException("Arma de catálogo no encontrada"));
        if(!compatible(item.getSize(), slot)) throw new IllegalArgumentException("El tamaño del arma no cabe en ese hueco");
        var existing=weapons.findByCharacterIdAndSlot(characterId, slot); existing.ifPresent(weapons::delete);
        return weapons.save(new WeaponEntity(UUID.randomUUID().toString(), characterId, slot, item.getName(), item.getWeaponType(), item.getSize(), item.getRange(), item.getReload(), item.getRate(), item.getDamageVital(), item.getDamageNormal(), item.getDamageLight(), item.getDamageVeryLight(), item.getAim(), item.getAutomaticFire(), item.getCapacity(), BigDecimal.ZERO, item.getCaliber(), item.getExtraRule(), item.getId(), item.getImageUrl()));
    }
    private boolean compatible(String size, String slot) { try { WeaponInventoryService.ensureCompatible(size, slot.trim().toUpperCase(Locale.ROOT)); return true; } catch (IllegalArgumentException error) { return false; } }
    private List<String> compatibleSizes(String slot) { String normalized = slot.trim().toUpperCase(Locale.ROOT); return switch (normalized) { case "SMALL_1", "SMALL_2", "SMALL_3" -> List.of("PEQUENA"); case "MEDIUM_1", "MEDIUM_2" -> List.of("MEDIANA"); case "ANY" -> List.of("GRANDE", "ENORME"); default -> throw new IllegalArgumentException("Hueco no válido"); }; }
    private void validate(WeaponCatalogCreateRequest r) { if(r==null || !WeaponInventoryService.TYPES.contains(r.weaponType()) || !WeaponInventoryService.SIZES.contains(r.size())) throw new IllegalArgumentException("Tipo o tamaño de arma no válido"); if(r.rate()==null || r.rate().isBlank()) throw new IllegalArgumentException("Cadencia obligatoria"); if(!isMelee(r.weaponType()) && (r.caliber()==null || r.caliber().isBlank())) throw new IllegalArgumentException("El calibre es obligatorio para armas de fuego"); }
    private boolean isMelee(String type) { return type != null && type.startsWith("CUERPO_"); }
    private String clean(String value) { return value==null || value.isBlank()?null:value.trim(); }
    private String imageData(String value) { if(value==null || value.isBlank()) return null; if(value.length()>7_000_000 || !value.startsWith("data:image/")) throw new IllegalArgumentException("Imagen de arma no válida o demasiado grande"); return value; }
    private Map<String,Object> view(WeaponCatalogEntity e) { var m=new LinkedHashMap<String,Object>(); m.put("id",e.getId());m.put("name",e.getName());m.put("weaponType",e.getWeaponType());m.put("size",e.getSize());m.put("range",e.getRange());m.put("reload",e.getReload());m.put("rate",e.getRate());m.put("damageVital",e.getDamageVital());m.put("damageNormal",e.getDamageNormal());m.put("damageLight",e.getDamageLight());m.put("damageVeryLight",e.getDamageVeryLight());m.put("aim",e.getAim());m.put("automaticFire",e.getAutomaticFire());m.put("capacity",e.getCapacity());m.put("loadedBullets",BigDecimal.ZERO);m.put("caliber",e.getCaliber());m.put("extraRule",e.getExtraRule());m.put("imageUrl",e.getImageUrl());m.put("official",e.isOfficial());return m; }
}
