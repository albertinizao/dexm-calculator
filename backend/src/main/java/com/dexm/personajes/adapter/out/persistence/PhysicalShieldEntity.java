package com.dexm.personajes.adapter.out.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "physical_shield_inventory", uniqueConstraints = @UniqueConstraint(name = "uk_physical_shield_character", columnNames = "character_id"))
public class PhysicalShieldEntity {
    @Id private String id;
    @Column(name = "character_id", nullable = false) private String characterId;
    @Column(nullable = false) private String name;
    @Column(columnDefinition = "text") private String description;
    @Column(nullable = false) private int rd;
    @Column(nullable = false) private int armor;
    @Column(nullable = false) private int defense;
    @Column(name = "other_effects", columnDefinition = "text") private String otherEffects;
    @Column(name = "image_url", columnDefinition = "longtext") private String imageUrl;
    protected PhysicalShieldEntity() {}
    public PhysicalShieldEntity(String id, String c, String n, String d, int rd, int armor, int defense, String effects, String image) { this.id=id; characterId=c; name=n; description=d; this.rd=rd; this.armor=armor; this.defense=defense; otherEffects=effects; imageUrl=image; }
    public String getId(){return id;} public String getCharacterId(){return characterId;} public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public int getRd(){return rd;} public void setRd(int v){rd=v;} public int getArmor(){return armor;} public void setArmor(int v){armor=v;} public int getDefense(){return defense;} public void setDefense(int v){defense=v;} public String getOtherEffects(){return otherEffects;} public void setOtherEffects(String v){otherEffects=v;} public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;}
}
