package com.dexm.personajes.adapter.out.persistence;

import java.math.BigDecimal;

public class WeaponCatalogEntity {
    private String id;
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
    private String caliber;
    private String extraRule;
    private String imageUrl;
    private boolean official;

    protected WeaponCatalogEntity() { }
    public WeaponCatalogEntity(String id, String name, String weaponType, String size, BigDecimal range, BigDecimal reload, String rate,
                               BigDecimal damageVital, BigDecimal damageNormal, BigDecimal damageLight, BigDecimal damageVeryLight,
                               BigDecimal aim, String automaticFire, BigDecimal capacity, String caliber, String extraRule, String imageUrl, boolean official) {
        this.id=id; this.name=name; this.weaponType=weaponType; this.size=size; this.range=range; this.reload=reload; this.rate=rate;
        this.damageVital=damageVital; this.damageNormal=damageNormal; this.damageLight=damageLight; this.damageVeryLight=damageVeryLight;
        this.aim=aim; this.automaticFire=automaticFire; this.capacity=capacity; this.caliber=caliber; this.extraRule=extraRule; this.imageUrl=imageUrl; this.official=official;
    }
    public String getId(){return id;} public String getName(){return name;} public String getWeaponType(){return weaponType;} public String getSize(){return size;}
    public BigDecimal getRange(){return range;} public BigDecimal getReload(){return reload;} public String getRate(){return rate;} public BigDecimal getDamageVital(){return damageVital;}
    public BigDecimal getDamageNormal(){return damageNormal;} public BigDecimal getDamageLight(){return damageLight;} public BigDecimal getDamageVeryLight(){return damageVeryLight;}
    public BigDecimal getAim(){return aim;} public String getAutomaticFire(){return automaticFire;} public BigDecimal getCapacity(){return capacity;} public String getCaliber(){return caliber;}
    public String getExtraRule(){return extraRule;} public String getImageUrl(){return imageUrl;} public boolean isOfficial(){return official;}
}
