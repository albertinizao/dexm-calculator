package com.dexm.personajes.adapter.out.persistence;
import java.util.ArrayList;
import java.util.List;
public class CharacterInventoryAggregateEntity {
 private String id; private String characterId;
 private List<WeaponEntity> weapons = new ArrayList<>();
 private List<AmmunitionEntity> ammunition = new ArrayList<>();
 private List<ArmorEntity> armors = new ArrayList<>();
 private List<ShieldEntity> shields = new ArrayList<>();
 private List<PhysicalShieldEntity> physicalShields = new ArrayList<>();
 private List<OtherInventoryItemEntity> otherInventoryItems = new ArrayList<>();
 protected CharacterInventoryAggregateEntity() {}
 public CharacterInventoryAggregateEntity(String characterId){this.id=characterId;this.characterId=characterId;}
 public String getId(){return id;} public String getCharacterId(){return characterId;}
 public void setId(String v){id=v;} public void setCharacterId(String v){characterId=v;}
 public List<WeaponEntity> getWeapons(){return weapons;} public void setWeapons(List<WeaponEntity> v){weapons=v==null?new ArrayList<>():v;}
 public List<AmmunitionEntity> getAmmunition(){return ammunition;} public void setAmmunition(List<AmmunitionEntity> v){ammunition=v==null?new ArrayList<>():v;}
 public List<ArmorEntity> getArmors(){return armors;} public void setArmors(List<ArmorEntity> v){armors=v==null?new ArrayList<>():v;}
 public List<ShieldEntity> getShields(){return shields;} public void setShields(List<ShieldEntity> v){shields=v==null?new ArrayList<>():v;}
 public List<PhysicalShieldEntity> getPhysicalShields(){return physicalShields;} public void setPhysicalShields(List<PhysicalShieldEntity> v){physicalShields=v==null?new ArrayList<>():v;}
 public List<OtherInventoryItemEntity> getOtherInventoryItems(){return otherInventoryItems;} public void setOtherInventoryItems(List<OtherInventoryItemEntity> v){otherInventoryItems=v==null?new ArrayList<>():v;}
}
