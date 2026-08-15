package com.dexm.personajes.adapter.out.persistence;

public class PhysicalShieldEntity {
    private String id;
    private String characterId;
    private String name;
    private String description;
    private int rd;
    private int armor;
    private int defense;
    private int movement;
    private String otherEffects;
    private String imageUrl;
    private String size;
    protected PhysicalShieldEntity() {}
    public PhysicalShieldEntity(String id, String c, String n, String d, int rd, int armor, int defense, String effects, String image) { this(id,c,n,d,rd,armor,defense,0,effects,image,null); }
    public PhysicalShieldEntity(String id, String c, String n, String d, int rd, int armor, int defense, int movement, String effects, String image, String size) { this.id=id; characterId=c; name=n; description=d; this.rd=rd; this.armor=armor; this.defense=defense; this.movement=movement; otherEffects=effects; imageUrl=image; this.size=size; }
    public String getId(){return id;} public String getCharacterId(){return characterId;} public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public int getRd(){return rd;} public void setRd(int v){rd=v;} public int getArmor(){return armor;} public void setArmor(int v){armor=v;} public int getDefense(){return defense;} public void setDefense(int v){defense=v;} public int getMovement(){return movement;} public void setMovement(int v){movement=v;} public String getOtherEffects(){return otherEffects;} public void setOtherEffects(String v){otherEffects=v;} public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;} public String getSize(){return size;} public void setSize(String v){size=v;}
}
