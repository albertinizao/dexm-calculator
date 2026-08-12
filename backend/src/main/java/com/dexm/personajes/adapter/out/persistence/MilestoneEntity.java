package com.dexm.personajes.adapter.out.persistence;
import java.time.Instant;
public class MilestoneEntity {
 private String id; private String characterId; private int level; private int experience; private String snapshotJson; private String newBonusesJson; private String newAbilitiesJson; private boolean visible; private Instant createdAt;
 protected MilestoneEntity(){}
 public MilestoneEntity(String id,String characterId,int level,int experience,String snapshotJson,String bonuses,String abilities){this(id,characterId,level,experience,snapshotJson,bonuses,abilities,true);}
 public MilestoneEntity(String id,String characterId,int level,int experience,String snapshotJson,String bonuses,String abilities,boolean visible){this.id=id;this.characterId=characterId;this.level=level;this.experience=experience;this.snapshotJson=snapshotJson;this.newBonusesJson=bonuses;this.newAbilitiesJson=abilities;this.visible=visible;this.createdAt=Instant.now();}
 public String getId(){return id;} public String getCharacterId(){return characterId;} public int getLevel(){return level;} public int getExperience(){return experience;} public String getSnapshotJson(){return snapshotJson;} public String getNewBonusesJson(){return newBonusesJson;} public String getNewAbilitiesJson(){return newAbilitiesJson;} public boolean isVisible(){return visible;} public Instant getCreatedAt(){return createdAt;}
}
