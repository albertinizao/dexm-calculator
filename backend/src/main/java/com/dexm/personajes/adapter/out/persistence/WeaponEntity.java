package com.dexm.personajes.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "weapon_inventory", uniqueConstraints = @UniqueConstraint(name = "uk_weapon_character_slot", columnNames = {"character_id", "slot"}))
public class WeaponEntity {
    @Id private String id;
    @Column(name = "character_id", nullable = false) private String characterId;
    @Column(nullable = false, length = 32) private String slot;
    @Column(nullable = false) private String name;
    @Column(name = "weapon_type", nullable = false) private String weaponType;
    @Column(nullable = false, length = 16) private String size;
    @Column(name = "weapon_range", nullable = false, precision = 12, scale = 3) private BigDecimal range;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal reload;
    @Column(nullable = false, length = 32) private String rate;
    @Column(name = "damage_vital", nullable = false, precision = 12, scale = 3) private BigDecimal damageVital;
    @Column(name = "damage_normal", nullable = false, precision = 12, scale = 3) private BigDecimal damageNormal;
    @Column(name = "damage_light", nullable = false, precision = 12, scale = 3) private BigDecimal damageLight;
    @Column(name = "damage_very_light", nullable = false, precision = 12, scale = 3) private BigDecimal damageVeryLight;
    @Column(precision = 12, scale = 3) private BigDecimal aim;
    @Column(name = "automatic_fire") private String automaticFire;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal capacity;
    @Column(name = "loaded_bullets", nullable = false, precision = 12, scale = 3) private BigDecimal loadedBullets;
    @Column(nullable = false) private String caliber;
    @Column(name = "extra_rule", columnDefinition = "text") private String extraRule;
    @Column(name = "catalog_weapon_id") private String catalogWeaponId;
    @Column(name = "image_url", columnDefinition = "longtext") private String imageUrl;

    protected WeaponEntity() { }
    public WeaponEntity(String id, String characterId, String slot, String name, String weaponType, String size,
                        BigDecimal range, BigDecimal reload, String rate, BigDecimal damageVital,
                        BigDecimal damageNormal, BigDecimal damageLight, BigDecimal damageVeryLight,
                        BigDecimal aim, String automaticFire, BigDecimal capacity, String caliber, String extraRule) {
        this.id=id; this.characterId=characterId; this.slot=slot; this.name=name; this.weaponType=weaponType; this.size=size;
        this.range=range; this.reload=reload; this.rate=rate; this.damageVital=damageVital; this.damageNormal=damageNormal;
        this.damageLight=damageLight; this.damageVeryLight=damageVeryLight; this.aim=aim; this.automaticFire=automaticFire;
        this.capacity=capacity; this.loadedBullets=BigDecimal.ZERO; this.caliber=caliber; this.extraRule=extraRule;
    }
    public WeaponEntity(String id, String characterId, String slot, String name, String weaponType, String size,
                        BigDecimal range, BigDecimal reload, String rate, BigDecimal damageVital,
                        BigDecimal damageNormal, BigDecimal damageLight, BigDecimal damageVeryLight,
                        BigDecimal aim, String automaticFire, BigDecimal capacity, BigDecimal loadedBullets,
                        String caliber, String extraRule) {
        this(id, characterId, slot, name, weaponType, size, range, reload, rate, damageVital, damageNormal,
                damageLight, damageVeryLight, aim, automaticFire, capacity, caliber, extraRule);
        this.loadedBullets = loadedBullets == null ? BigDecimal.ZERO : loadedBullets;
    }
    public WeaponEntity(String id, String characterId, String slot, String name, String weaponType, String size,
                        BigDecimal range, BigDecimal reload, String rate, BigDecimal damageVital,
                        BigDecimal damageNormal, BigDecimal damageLight, BigDecimal damageVeryLight,
                        BigDecimal aim, String automaticFire, BigDecimal capacity, String caliber, String extraRule,
                        String catalogWeaponId, String imageUrl) {
        this(id, characterId, slot, name, weaponType, size, range, reload, rate, damageVital, damageNormal, damageLight,
                damageVeryLight, aim, automaticFire, capacity, caliber, extraRule);
        this.catalogWeaponId=catalogWeaponId; this.imageUrl=imageUrl;
    }
    public WeaponEntity(String id, String characterId, String slot, String name, String weaponType, String size,
                        BigDecimal range, BigDecimal reload, String rate, BigDecimal damageVital,
                        BigDecimal damageNormal, BigDecimal damageLight, BigDecimal damageVeryLight,
                        BigDecimal aim, String automaticFire, BigDecimal capacity, BigDecimal loadedBullets,
                        String caliber, String extraRule, String catalogWeaponId, String imageUrl) {
        this(id, characterId, slot, name, weaponType, size, range, reload, rate, damageVital, damageNormal,
                damageLight, damageVeryLight, aim, automaticFire, capacity, loadedBullets, caliber, extraRule);
        this.catalogWeaponId=catalogWeaponId; this.imageUrl=imageUrl;
    }
    public String getId(){return id;} public String getCharacterId(){return characterId;} public String getSlot(){return slot;} public void setSlot(String v){slot=v;}
    public String getName(){return name;} public void setName(String v){name=v;} public String getWeaponType(){return weaponType;} public void setWeaponType(String v){weaponType=v;}
    public String getSize(){return size;} public void setSize(String v){size=v;} public BigDecimal getRange(){return range;} public void setRange(BigDecimal v){range=v;}
    public BigDecimal getReload(){return reload;} public void setReload(BigDecimal v){reload=v;} public String getRate(){return rate;} public void setRate(String v){rate=v;}
    public BigDecimal getDamageVital(){return damageVital;} public void setDamageVital(BigDecimal v){damageVital=v;} public BigDecimal getDamageNormal(){return damageNormal;} public void setDamageNormal(BigDecimal v){damageNormal=v;}
    public BigDecimal getDamageLight(){return damageLight;} public void setDamageLight(BigDecimal v){damageLight=v;} public BigDecimal getDamageVeryLight(){return damageVeryLight;} public void setDamageVeryLight(BigDecimal v){damageVeryLight=v;}
    public BigDecimal getAim(){return aim;} public void setAim(BigDecimal v){aim=v;} public String getAutomaticFire(){return automaticFire;} public void setAutomaticFire(String v){automaticFire=v;}
    public BigDecimal getCapacity(){return capacity;} public void setCapacity(BigDecimal v){capacity=v;} public String getCaliber(){return caliber;} public void setCaliber(String v){caliber=v;}
    public BigDecimal getLoadedBullets(){return loadedBullets;} public void setLoadedBullets(BigDecimal v){loadedBullets=v;}
    public String getExtraRule(){return extraRule;} public void setExtraRule(String v){extraRule=v;}
    public String getCatalogWeaponId(){return catalogWeaponId;} public void setCatalogWeaponId(String v){catalogWeaponId=v;} public String getImageUrl(){return imageUrl;}
}
