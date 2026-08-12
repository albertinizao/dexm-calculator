package com.dexm.personajes.adapter.out.persistence;

import java.math.BigDecimal;

public class WeaponEntity {
    private String id;
    private String characterId;
    private String slot;
    private String name;
    private String weaponType;
    private String size;
    private BigDecimal range;
    private BigDecimal reload;
    private String rate;
    private BigDecimal damageVital;
    private BigDecimal damageNormal;
    private BigDecimal damageLight;
    private BigDecimal damageVeryLight;
    private BigDecimal aim;
    private String automaticFire;
    private BigDecimal capacity;
    private BigDecimal loadedBullets;
    private String caliber;
    private String extraRule;
    private String catalogWeaponId;
    private String imageUrl;

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
