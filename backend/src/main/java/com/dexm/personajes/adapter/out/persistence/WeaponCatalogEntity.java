package com.dexm.personajes.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "weapon_catalog")
public class WeaponCatalogEntity {
    @Id private String id;
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
    @Column(nullable = false) private String caliber;
    @Column(name = "extra_rule", columnDefinition = "text") private String extraRule;
    @Column(name = "image_url", columnDefinition = "longtext") private String imageUrl;
    @Column(nullable = false) private boolean official;

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
