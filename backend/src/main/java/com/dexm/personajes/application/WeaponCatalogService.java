package com.dexm.personajes.application;

import com.dexm.personajes.adapter.in.web.CharacterController.WeaponCatalogCreateRequest;
import com.dexm.personajes.adapter.out.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.util.*;
import java.io.IOException;
import java.util.Base64;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

@Service
public class WeaponCatalogService {
    private final WeaponCatalogRepository catalog; private final WeaponRepository weapons; private final CharacterRepository characters;
    public WeaponCatalogService(WeaponCatalogRepository catalog) { this.catalog=catalog; this.weapons=null; this.characters=null; }
    @Autowired public WeaponCatalogService(WeaponCatalogRepository catalog, WeaponRepository weapons, CharacterRepository characters) { this.catalog=catalog; this.weapons=weapons; this.characters=characters; }

    @Transactional(readOnly = true) public List<Map<String,Object>> search(String slot, String name, String type) {
        String normalizedName=name==null?"":name.trim().toLowerCase(Locale.ROOT); String normalizedType=type==null?"":type.trim().toUpperCase(Locale.ROOT);
        return catalog.findAll().stream().filter(item -> normalizedName.isEmpty() || item.getName().toLowerCase(Locale.ROOT).contains(normalizedName))
            .filter(item -> normalizedType.isEmpty() || item.getWeaponType().equals(normalizedType))
            .filter(item -> slot==null || slot.isBlank() || compatible(item.getSize(), slot)).sorted(Comparator.comparing(WeaponCatalogEntity::getName)).map(this::view).toList();
    }
    @Transactional public Map<String,Object> createCustom(WeaponCatalogCreateRequest request) {
        validate(request); var entity=new WeaponCatalogEntity(UUID.randomUUID().toString(), request.name().trim(), request.weaponType(), request.size(), request.range(), request.reload(), request.rate().trim(), request.damageVital(), request.damageNormal(), request.damageLight(), request.damageVeryLight(), request.aim(), clean(request.automaticFire()), (isMelee(request.weaponType()) ? BigDecimal.ONE : request.capacity()), (isMelee(request.weaponType()) ? null : clean(request.caliber())), clean(request.extraRule()), imageData(request.imageUrl()), false);
        return view(catalog.save(entity));
    }
    @Transactional(readOnly = true) public ImageData image(String catalogId) {
        var item=catalog.findById(catalogId).orElseThrow(()->new NoSuchElementException("Arma de catálogo no encontrada"));
        String value=item.getImageUrl(); if(value==null || value.isBlank()) throw new NoSuchElementException("El arma no tiene imagen");
        try {
            if(value.startsWith("data:image/")) {
                int comma=value.indexOf(','); String header=value.substring(0,comma); String mime=header.substring(5,header.indexOf(';'));
                return new ImageData(MediaType.parseMediaType(mime), Base64.getDecoder().decode(value.substring(comma+1)));
            }
            var resource=new ClassPathResource("static"+value); if(!resource.exists()) throw new NoSuchElementException("Imagen de arma no encontrada");
            String mime=MediaTypeFactory.getMediaType(resource.getFilename()).orElse(MediaType.APPLICATION_OCTET_STREAM).toString();
            return new ImageData(MediaType.parseMediaType(mime), resource.getInputStream().readAllBytes());
        } catch(IOException | IllegalArgumentException error) { throw new IllegalStateException("No se pudo leer la imagen del arma", error); }
    }
    public record ImageData(MediaType mediaType, byte[] bytes) { }
    @Transactional public WeaponEntity copyToCharacter(String catalogId, String characterId, String slot) {
        if(weapons==null || characters==null) throw new IllegalStateException("Servicio de inventario no disponible");
        if(!characters.existsById(characterId)) throw new NoSuchElementException("Personaje no encontrado");
        var item=catalog.findById(catalogId).orElseThrow(()->new NoSuchElementException("Arma de catálogo no encontrada"));
        if(!compatible(item.getSize(), slot)) throw new IllegalArgumentException("El tamaño del arma no cabe en ese hueco");
        var existing=weapons.findByCharacterIdAndSlot(characterId, slot); existing.ifPresent(weapons::delete);
        return weapons.save(new WeaponEntity(UUID.randomUUID().toString(), characterId, slot, item.getName(), item.getWeaponType(), item.getSize(), item.getRange(), item.getReload(), item.getRate(), item.getDamageVital(), item.getDamageNormal(), item.getDamageLight(), item.getDamageVeryLight(), item.getAim(), item.getAutomaticFire(), item.getCapacity(), BigDecimal.ZERO, item.getCaliber(), item.getExtraRule(), item.getId(), item.getImageUrl()==null?null:"/api/weapon-catalog/"+item.getId()+"/image"));
    }
    private boolean compatible(String size, String slot) { try { WeaponInventoryService.ensureCompatible(size, slot.trim().toUpperCase(Locale.ROOT)); return true; } catch (IllegalArgumentException error) { return false; } }
    private void validate(WeaponCatalogCreateRequest r) { if(r==null || !WeaponInventoryService.TYPES.contains(r.weaponType()) || !WeaponInventoryService.SIZES.contains(r.size())) throw new IllegalArgumentException("Tipo o tamaño de arma no válido"); if(r.rate()==null || r.rate().isBlank()) throw new IllegalArgumentException("Cadencia obligatoria"); if(!isMelee(r.weaponType()) && (r.caliber()==null || r.caliber().isBlank())) throw new IllegalArgumentException("El calibre es obligatorio para armas de fuego"); }
    private boolean isMelee(String type) { return type != null && type.startsWith("CUERPO_"); }
    private String clean(String value) { return value==null || value.isBlank()?null:value.trim(); }
    private String imageData(String value) { if(value==null || value.isBlank()) return null; if(value.length()>7_000_000 || !value.startsWith("data:image/")) throw new IllegalArgumentException("Imagen de arma no válida o demasiado grande"); return value; }
    private Map<String,Object> view(WeaponCatalogEntity e) { var m=new LinkedHashMap<String,Object>(); m.put("id",e.getId());m.put("name",e.getName());m.put("weaponType",e.getWeaponType());m.put("size",e.getSize());m.put("range",e.getRange());m.put("reload",e.getReload());m.put("rate",e.getRate());m.put("damageVital",e.getDamageVital());m.put("damageNormal",e.getDamageNormal());m.put("damageLight",e.getDamageLight());m.put("damageVeryLight",e.getDamageVeryLight());m.put("aim",e.getAim());m.put("automaticFire",e.getAutomaticFire());m.put("capacity",e.getCapacity());m.put("loadedBullets",BigDecimal.ZERO);m.put("caliber",e.getCaliber());m.put("extraRule",e.getExtraRule());m.put("imageUrl",e.getImageUrl()==null?null:"/api/weapon-catalog/"+e.getId()+"/image");m.put("official",e.isOfficial());return m; }
}
