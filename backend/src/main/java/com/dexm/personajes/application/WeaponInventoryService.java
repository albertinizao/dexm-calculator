package com.dexm.personajes.application;

import com.dexm.personajes.adapter.in.web.CharacterController.WeaponRequest;
import com.dexm.personajes.adapter.out.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class WeaponInventoryService {
    public static final List<String> SLOTS = List.of("SMALL_1", "SMALL_2", "SMALL_3", "MEDIUM_1", "MEDIUM_2", "ANY");
    public static final Set<String> TYPES = Set.of("PISTOLA", "SUBFUSIL", "FUSIL", "RIFLE_CAZA", "FUSIL_FRANCOTIRADOR", "AMETRALLADORA_LIGERA", "ESCOPETA", "CUERPO_PEQUENA", "CUERPO_MEDIANA", "CUERPO_PESADA");
    public static final Set<String> SIZES = Set.of("PEQUENA", "MEDIANA", "GRANDE", "ENORME");
    private final CharacterRepository characters; private final WeaponRepository weapons;
    public WeaponInventoryService(CharacterRepository characters, WeaponRepository weapons){this.characters=characters;this.weapons=weapons;}

    public record ShootCommand(String mode, Integer shots) { }

    @Transactional(readOnly=true) public List<Map<String,Object>> list(String characterId){ensureCharacter(characterId); return weapons.findByCharacterIdOrderBySlotAsc(characterId).stream().map(this::view).toList();}
    @Transactional public Map<String,Object> create(String characterId, WeaponRequest request){
        ensureValidRequest(request); ensureCharacter(characterId); String slot=normalizeSlot(request.slot());
        var existing=weapons.findByCharacterIdAndSlot(characterId,slot); existing.ifPresent(weapons::delete);
        return view(weapons.save(from(UUID.randomUUID().toString(),characterId,slot,request)));
    }
    @Transactional public Map<String,Object> update(String characterId,String id,WeaponRequest request){
        var entity=find(characterId,id); ensureValidRequest(request); ensureCompatible(request.size(), entity.getSlot());
        if(entity.getCatalogWeaponId()!=null && !entity.getWeaponType().equals(request.weaponType())) throw new IllegalArgumentException("El tipo de un arma del listado no se puede modificar");
        String slot=entity.getSlot(); apply(entity,request); entity.setSlot(slot); return view(weapons.save(entity));
    }
    @Transactional public void delete(String characterId,String id){weapons.delete(find(characterId,id));}

    @Transactional
    public Map<String,Object> shoot(String characterId, String weaponId, ShootCommand command) {
        ensureCharacter(characterId);
        if (command == null) throw new IllegalArgumentException("El disparo es obligatorio");
        var weapon = weapons.findByIdAndCharacterIdForUpdate(weaponId, characterId)
                .orElseThrow(() -> new NoSuchElementException("Arma no encontrada"));
        String mode = normalizeShootMode(command.mode());
        int cadence = cadence(weapon.getRate());
        Integer automaticFire = optionalAutomaticFire(weapon.getAutomaticFire());
        int requested;
        if (mode.equals("automatic")) {
            if (automaticFire == null) throw new IllegalArgumentException("El arma no tiene fuego automático");
            try {
                requested = Math.multiplyExact(cadence, automaticFire);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("La cantidad de disparos automáticos no es válida", exception);
            }
        } else {
            requested = positiveShots(command.shots());
            if (requested > cadence) throw new IllegalArgumentException("No se pueden realizar más disparos que la cadencia del arma");
        }
        if (requested < 1) throw new IllegalArgumentException("La cantidad de disparos debe ser positiva");

        int loaded = loadedBullets(weapon.getLoadedBullets(), weapon.getCapacity()).intValueExact();
        if (requested > loaded) throw new IllegalArgumentException("No hay suficientes balas cargadas: faltan " + (requested - loaded));
        int remaining = loaded - requested;
        weapon.setLoadedBullets(BigDecimal.valueOf(remaining));
        weapons.save(weapon);

        var result = new LinkedHashMap<String,Object>();
        result.put("weaponId", weapon.getId());
        result.put("mode", mode);
        result.put("requested", requested);
        result.put("consumed", requested);
        result.put("remaining", remaining);
        result.put("cadence", cadence);
        result.put("automaticFire", automaticFire);
        result.put("loadedBullets", remaining);
        return result;
    }

    private String normalizeShootMode(String mode) {
        if (mode == null || mode.isBlank()) return "normal";
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("normal") || normalized.equals("single") || normalized.equals("manual")) return "normal";
        if (normalized.equals("automatic") || normalized.equals("auto")) return "automatic";
        throw new IllegalArgumentException("Modo de disparo no válido");
    }

    private int positiveShots(Integer shots) {
        if (shots == null || shots < 1) throw new IllegalArgumentException("La cantidad de disparos debe ser positiva");
        return shots;
    }

    private int cadence(String rate) {
        if (rate == null || rate.isBlank()) throw new IllegalArgumentException("La cadencia del arma no es válida");
        String value = rate.trim();
        int separator = value.toLowerCase(Locale.ROOT).indexOf('x');
        String cadence = separator < 0 ? value : value.substring(0, separator).trim();
        try {
            int parsed = Integer.parseInt(cadence);
            if (parsed < 1) throw new IllegalArgumentException("La cadencia del arma debe ser positiva");
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("La cadencia del arma no es válida", exception);
        }
    }

    private Integer optionalAutomaticFire(String automaticFire) {
        if (automaticFire == null || automaticFire.isBlank()) return null;
        try {
            int parsed = Integer.parseInt(automaticFire.trim());
            if (parsed < 1) throw new IllegalArgumentException("El fuego automático debe ser positivo");
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El fuego automático no es válido", exception);
        }
    }
    @Transactional public Map<String,Object> move(String characterId,String id,String targetSlot){
        var source=find(characterId,id); targetSlot=normalizeSlot(targetSlot); ensureCompatible(source.getSize(),targetSlot);
        if(source.getSlot().equals(targetSlot)) return view(source);
        var target=weapons.findByCharacterIdAndSlot(characterId,targetSlot).orElse(null);
        if(target!=null){ ensureCompatible(target.getSize(),source.getSlot()); String sourceSlot=source.getSlot(); source.setSlot("TMP"); weapons.saveAndFlush(source); target.setSlot(sourceSlot); weapons.saveAndFlush(target); source.setSlot(targetSlot); }
        source.setSlot(targetSlot); return view(weapons.save(source));
    }
    private WeaponEntity from(String id,String characterId,String slot,WeaponRequest r){return new WeaponEntity(id,characterId,slot,r.name().trim(),r.weaponType(),r.size(),r.range(),r.reload(),r.rate(),r.damageVital(),r.damageNormal(),r.damageLight(),r.damageVeryLight(),r.aim(),clean(r.automaticFire()),r.capacity(),loadedBullets(r.loadedBullets(),r.capacity()),r.caliber().trim(),clean(r.extraRule()));}
    private void apply(WeaponEntity e,WeaponRequest r){e.setName(r.name().trim());e.setWeaponType(r.weaponType());e.setSize(r.size());e.setRange(r.range());e.setReload(r.reload());e.setRate(r.rate());e.setDamageVital(r.damageVital());e.setDamageNormal(r.damageNormal());e.setDamageLight(r.damageLight());e.setDamageVeryLight(r.damageVeryLight());e.setAim(r.aim());e.setAutomaticFire(clean(r.automaticFire()));e.setCapacity(r.capacity());e.setLoadedBullets(loadedBullets(r.loadedBullets()==null?e.getLoadedBullets():r.loadedBullets(),r.capacity()));e.setCaliber(r.caliber().trim());e.setExtraRule(clean(r.extraRule()));}
    private WeaponEntity find(String c,String id){ensureCharacter(c);return weapons.findByIdAndCharacterId(id,c).orElseThrow(()->new NoSuchElementException("Arma no encontrada"));}
    private void ensureCharacter(String id){if(!characters.existsById(id))throw new NoSuchElementException("Personaje no encontrado");}
    private String normalizeSlot(String slot){if(slot==null)throw new IllegalArgumentException("Hueco obligatorio"); String s=slot.trim().toUpperCase(Locale.ROOT); if(!SLOTS.contains(s))throw new IllegalArgumentException("Hueco no válido");return s;}
    private void ensureValidRequest(WeaponRequest r){if(r==null||!TYPES.contains(r.weaponType())||!SIZES.contains(r.size()))throw new IllegalArgumentException("Tipo o tamaño de arma no válido"); ensureCompatible(r.size(),normalizeSlot(r.slot())); integralPositiveCapacity(r.capacity()); loadedBullets(r.loadedBullets(),r.capacity());}
    public static void ensureCompatible(String size,String slot){if(!SIZES.contains(size)||!SLOTS.contains(slot))throw new IllegalArgumentException("Tamaño o hueco no válido"); if(size.equals("PEQUENA"))return; if(size.equals("MEDIANA")&&(slot.startsWith("MEDIUM_")||slot.equals("ANY")))return; if((size.equals("GRANDE")||size.equals("ENORME"))&&slot.equals("ANY"))return; throw new IllegalArgumentException("El tamaño del arma no cabe en ese hueco");}
    private String clean(String v){return v==null||v.isBlank()?null:v.trim();}
    private int integralPositiveCapacity(BigDecimal capacity){if(capacity==null)throw new IllegalArgumentException("La capacidad del arma debe ser un entero positivo");try{int value=capacity.toBigIntegerExact().intValueExact();if(value<1)throw new IllegalArgumentException("La capacidad del arma debe ser un entero positivo");return value;}catch(ArithmeticException exception){throw new IllegalArgumentException("La capacidad del arma debe ser un entero positivo",exception);}}
    private BigDecimal loadedBullets(BigDecimal loaded, BigDecimal capacity){int maximum=integralPositiveCapacity(capacity);BigDecimal value=loaded==null?BigDecimal.ZERO:loaded;try{int integerValue=value.toBigIntegerExact().intValueExact();if(integerValue<0||integerValue>maximum)throw new IllegalArgumentException("Las balas cargadas deben estar entre 0 y la capacidad del arma");return BigDecimal.valueOf(integerValue);}catch(ArithmeticException exception){throw new IllegalArgumentException("Las balas cargadas deben ser un número entero entre 0 y la capacidad del arma",exception);}}
    private Map<String,Object> view(WeaponEntity e){var m=new LinkedHashMap<String,Object>();m.put("id",e.getId());m.put("slot",e.getSlot());m.put("name",e.getName());m.put("weaponType",e.getWeaponType());m.put("size",e.getSize());m.put("range",e.getRange());m.put("reload",e.getReload());m.put("rate",e.getRate());m.put("damageVital",e.getDamageVital());m.put("damageNormal",e.getDamageNormal());m.put("damageLight",e.getDamageLight());m.put("damageVeryLight",e.getDamageVeryLight());m.put("aim",e.getAim());m.put("automaticFire",e.getAutomaticFire());m.put("capacity",e.getCapacity());m.put("loadedBullets",e.getLoadedBullets()==null?BigDecimal.ZERO:e.getLoadedBullets());m.put("caliber",e.getCaliber());m.put("extraRule",e.getExtraRule());m.put("catalogWeaponId",e.getCatalogWeaponId());m.put("imageUrl",e.getImageUrl());return m;}
}
