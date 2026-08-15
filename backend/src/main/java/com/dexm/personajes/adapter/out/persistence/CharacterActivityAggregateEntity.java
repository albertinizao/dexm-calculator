package com.dexm.personajes.adapter.out.persistence;
import java.util.ArrayList; import java.util.List;
public class CharacterActivityAggregateEntity {
 private String id; private String characterId; private List<TrainingActivityEntity> activities = new ArrayList<>();
 protected CharacterActivityAggregateEntity() {}
 public CharacterActivityAggregateEntity(String characterId){this.id=characterId;this.characterId=characterId;}
 public String getId(){return id;} public String getCharacterId(){return characterId;}
 public void setId(String v){id=v;} public void setCharacterId(String v){characterId=v;}
 public List<TrainingActivityEntity> getActivities(){return activities;} public void setActivities(List<TrainingActivityEntity> v){activities=v==null?new ArrayList<>():v;}
}
