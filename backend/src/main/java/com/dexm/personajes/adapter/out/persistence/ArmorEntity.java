package com.dexm.personajes.adapter.out.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "armor_inventory")
public class ArmorEntity {
    @Id private String id;
    @Column(name="character_id", nullable=false) private String characterId;
    @Column(nullable=false) private String name;
    @Column(columnDefinition="text") private String description;
    @Column(name="slots_json", nullable=false, columnDefinition="text") private String slotsJson;
    @Column(name="image_url", columnDefinition="longtext") private String imageUrl;
    protected ArmorEntity() {}
    public ArmorEntity(String id,String characterId,String name,String description,String slotsJson,String imageUrl){this.id=id;this.characterId=characterId;this.name=name;this.description=description;this.slotsJson=slotsJson;this.imageUrl=imageUrl;}
    public String getId(){return id;} public String getCharacterId(){return characterId;} public String getName(){return name;} public void setName(String v){name=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getSlotsJson(){return slotsJson;} public void setSlotsJson(String v){slotsJson=v;}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;}
}
