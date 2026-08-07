package com.dexm.personajes.adapter.out.persistence;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="campaigns") public class CampaignEntity {
 @Id private String id; @Column(nullable=false) private String name; private Instant createdAt;
 protected CampaignEntity(){} public CampaignEntity(String id,String name){this.id=id;this.name=name;this.createdAt=Instant.now();}
 public String getId(){return id;} public String getName(){return name;} public Instant getCreatedAt(){return createdAt;}
}
