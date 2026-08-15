package com.dexm.personajes.adapter.out.persistence;

public class GrenadeCatalogEntity {
    private String id;
    private String name;
    private String description;
    private int centralDamage;
    private int adjacentDamage;
    private int damageDecay;
    private String additionalEffect;
    private boolean handGrenade;
    private String type;
    private boolean official;

    protected GrenadeCatalogEntity() { }

    public GrenadeCatalogEntity(String id, String name, String description,
                                 int centralDamage, int adjacentDamage, int damageDecay, boolean official) {
        this(id, name, description, centralDamage, adjacentDamage, damageDecay, null, true, null, official);
    }

    public GrenadeCatalogEntity(String id, String name, String description,
                                 int centralDamage, int adjacentDamage, int damageDecay,
                                 boolean handGrenade, String type, boolean official) {
        this(id, name, description, centralDamage, adjacentDamage, damageDecay, null, handGrenade, type, official);
    }

    public GrenadeCatalogEntity(String id, String name, String description,
                                 int centralDamage, int adjacentDamage, int damageDecay,
                                 String additionalEffect, boolean handGrenade, String type, boolean official) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.centralDamage = centralDamage;
        this.adjacentDamage = adjacentDamage;
        this.damageDecay = damageDecay;
        this.additionalEffect = additionalEffect;
        this.handGrenade = handGrenade;
        this.type = type;
        this.official = official;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public int getCentralDamage() { return centralDamage; }
    public void setCentralDamage(int value) { centralDamage = value; }
    public int getAdjacentDamage() { return adjacentDamage; }
    public void setAdjacentDamage(int value) { adjacentDamage = value; }
    public int getDamageDecay() { return damageDecay; }
    public void setDamageDecay(int value) { damageDecay = value; }
    public String getAdditionalEffect() { return additionalEffect; }
    public void setAdditionalEffect(String value) { additionalEffect = value; }
    public boolean isHandGrenade() { return handGrenade; }
    public void setHandGrenade(boolean value) { handGrenade = value; }
    public String getType() { return type; }
    public void setType(String value) { type = value; }
    public boolean isOfficial() { return official; }
    public void setOfficial(boolean value) { official = value; }
}
