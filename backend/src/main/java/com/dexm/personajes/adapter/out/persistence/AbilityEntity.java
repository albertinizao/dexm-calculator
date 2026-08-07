package com.dexm.personajes.adapter.out.persistence;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="abilities") public class AbilityEntity {
 @Id private String id; private String name; @Column(columnDefinition="text") private String description; private String launchType; private Integer cost; private String uniqueFlag; @Column(columnDefinition="text") private String alternativesJson; private Instant updatedAt;
 protected AbilityEntity(){} public AbilityEntity(String id,String name,String description,String launchType,Integer cost,String uniqueFlag,String alternativesJson){this.id=id;this.name=name;this.description=description;this.launchType=launchType;this.cost=cost;this.uniqueFlag=uniqueFlag;this.alternativesJson=alternativesJson;this.updatedAt=Instant.now();}
 public String getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public String getLaunchType(){return launchType;} public Integer getCost(){return cost;} public String getUniqueFlag(){return uniqueFlag;} public String getAlternativesJson(){return alternativesJson;} public void setAlternativesJson(String v){alternativesJson=v;} public Instant getUpdatedAt(){return updatedAt;}
}
