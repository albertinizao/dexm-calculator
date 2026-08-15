package com.dexm.personajes.adapter.out.persistence;
import java.util.ArrayList; import java.util.List;
public class CharacterAbilityStateEntity {
 private String id; private String characterId; private List<String> obtained = new ArrayList<>(); private List<String> pendingUnique = new ArrayList<>(); private String sourceMilestoneId;
 protected CharacterAbilityStateEntity() {}
 public CharacterAbilityStateEntity(String characterId){this.id=characterId;this.characterId=characterId;}
 public String getId(){return id;} public String getCharacterId(){return characterId;}
 public List<String> getObtained(){return obtained;} public void setObtained(List<String> v){obtained=v==null?new ArrayList<>():v;}
 public List<String> getPendingUnique(){return pendingUnique;} public void setPendingUnique(List<String> v){pendingUnique=v==null?new ArrayList<>():v;}
 public String getSourceMilestoneId(){return sourceMilestoneId;} public void setSourceMilestoneId(String v){sourceMilestoneId=v;}
}
