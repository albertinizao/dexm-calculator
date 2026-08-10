package com.dexm.personajes.adapter.out.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "training_activities")
public class TrainingActivityEntity {
    @Id private String id;
    @Column(name="character_id", nullable=false) private String characterId;
    @Column(nullable=false) private String type;
    @Column(nullable=false) private String name;
    @Column(name="start_age", nullable=false) private int startAge;
    @Column(name="end_age", nullable=false) private int endAge;
    @Column(nullable=false) private int priority;
    @Column(name="primary_attribute") private String primaryAttribute;
    @Column(name="secondary_attribute") private String secondaryAttribute;
    @Column(name="tertiary_attribute") private String tertiaryAttribute;
    @Column(nullable=false) private boolean concurrent;
    protected TrainingActivityEntity() { }
    public TrainingActivityEntity(String id,String characterId,String type,String name,int startAge,int endAge,int priority,String primary,String secondary,String tertiary,boolean concurrent){this.id=id;this.characterId=characterId;this.type=type;this.name=name;this.startAge=startAge;this.endAge=endAge;this.priority=priority;this.primaryAttribute=primary;this.secondaryAttribute=secondary;this.tertiaryAttribute=tertiary;this.concurrent=concurrent;}
    public String getId(){return id;} public String getCharacterId(){return characterId;} public String getType(){return type;} public void setType(String v){type=v;} public String getName(){return name;} public void setName(String v){name=v;} public int getStartAge(){return startAge;} public void setStartAge(int v){startAge=v;} public int getEndAge(){return endAge;} public void setEndAge(int v){endAge=v;} public int getPriority(){return priority;} public void setPriority(int v){priority=v;} public String getPrimaryAttribute(){return primaryAttribute;} public void setPrimaryAttribute(String v){primaryAttribute=v;} public String getSecondaryAttribute(){return secondaryAttribute;} public void setSecondaryAttribute(String v){secondaryAttribute=v;} public String getTertiaryAttribute(){return tertiaryAttribute;} public void setTertiaryAttribute(String v){tertiaryAttribute=v;} public boolean isConcurrent(){return concurrent;} public void setConcurrent(boolean v){concurrent=v;}
}
