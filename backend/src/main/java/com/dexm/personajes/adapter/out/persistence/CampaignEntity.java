package com.dexm.personajes.adapter.out.persistence;
import java.time.Instant;
public class CampaignEntity {
 private String id; private String name; private Instant createdAt;
 protected CampaignEntity(){} public CampaignEntity(String id,String name){this.id=id;this.name=name;this.createdAt=Instant.now();}
 public String getId(){return id;} public String getName(){return name;} public void setName(String name){this.name=name==null?"":name.trim();} public Instant getCreatedAt(){return createdAt;}
}