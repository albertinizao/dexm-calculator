package com.dexm.personajes.adapter.out.persistence;

import java.math.BigDecimal;

public class ArmorEntity {
    private String id;
    private String characterId;
    private String name;
    private String description;
    private String slotsJson;
    private String imageUrl;
    private String effect;
    private String zone;
    private BigDecimal defensePenalty;
    private BigDecimal meleeDefensePenalty;
    private BigDecimal rangedDefensePenalty;
    private BigDecimal movementPenalty;
    protected ArmorEntity() {}
    public ArmorEntity(String id,String characterId,String name,String description,String slotsJson,String imageUrl){this(id,characterId,name,description,slotsJson,imageUrl,null,null,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);}
    public ArmorEntity(String id,String characterId,String name,String description,String slotsJson,String imageUrl,String effect,String zone){this(id,characterId,name,description,slotsJson,imageUrl,effect,zone,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);}
    public ArmorEntity(String id,String characterId,String name,String description,String slotsJson,String imageUrl,String effect,String zone,BigDecimal defensePenalty,BigDecimal meleeDefensePenalty,BigDecimal rangedDefensePenalty,BigDecimal movementPenalty){this.id=id;this.characterId=characterId;this.name=name;this.description=description;this.slotsJson=slotsJson;this.imageUrl=imageUrl;this.effect=effect;this.zone=zone;this.defensePenalty=defensePenalty;this.meleeDefensePenalty=meleeDefensePenalty;this.rangedDefensePenalty=rangedDefensePenalty;this.movementPenalty=movementPenalty;}
    public String getId(){return id;} public String getCharacterId(){return characterId;} public String getName(){return name;} public void setName(String v){name=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getSlotsJson(){return slotsJson;} public void setSlotsJson(String v){slotsJson=v;}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;}
    public String getEffect(){return effect;} public void setEffect(String v){effect=v;}
    public String getZone(){return zone;} public void setZone(String v){zone=v;}
    public BigDecimal getDefensePenalty(){return defensePenalty == null ? BigDecimal.ZERO : defensePenalty;} public void setDefensePenalty(BigDecimal v){defensePenalty=v;}
    public BigDecimal getMeleeDefensePenalty(){return meleeDefensePenalty == null ? BigDecimal.ZERO : meleeDefensePenalty;} public void setMeleeDefensePenalty(BigDecimal v){meleeDefensePenalty=v;}
    public BigDecimal getRangedDefensePenalty(){return rangedDefensePenalty == null ? BigDecimal.ZERO : rangedDefensePenalty;} public void setRangedDefensePenalty(BigDecimal v){rangedDefensePenalty=v;}
    public BigDecimal getMovementPenalty(){return movementPenalty == null ? BigDecimal.ZERO : movementPenalty;} public void setMovementPenalty(BigDecimal v){movementPenalty=v;}
}
