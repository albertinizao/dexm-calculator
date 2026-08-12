package com.dexm.personajes.adapter.out.persistence;
import java.time.Instant;
public class MinorAttributeDefinitionEntity {
 private String id; private String campaignId; private String ownerCharacterId; private String key; private String name; private String maxFormula; private String bonusSource; private String type; private Instant createdAt;
 protected MinorAttributeDefinitionEntity(){} public MinorAttributeDefinitionEntity(String id,String campaignId,String key,String name,String maxFormula,String bonusSource,String type){this(id,campaignId,null,key,name,maxFormula,bonusSource,type);} public MinorAttributeDefinitionEntity(String id,String campaignId,String ownerCharacterId,String key,String name,String maxFormula,String bonusSource,String type){this.id=id;this.campaignId=campaignId;this.ownerCharacterId=ownerCharacterId;this.key=key;this.name=name;this.maxFormula=maxFormula;this.bonusSource=bonusSource;this.type=type;this.createdAt=Instant.now();}
 public String getId(){return id;} public String getCampaignId(){return campaignId;} public String getOwnerCharacterId(){return ownerCharacterId;} public String getKey(){return key;} public String getName(){return name;} public String getMaxFormula(){return maxFormula;} public String getBonusSource(){return bonusSource;} public String getType(){return type;} public Instant getCreatedAt(){return createdAt;}
}
