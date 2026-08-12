package com.dexm.personajes.adapter.out.persistence;

public class AmmunitionEntity {
    private String id;
    private String characterId;
    private String caliber;
    private int quantity;

    protected AmmunitionEntity() { }

    public AmmunitionEntity(String id, String characterId, String caliber, int quantity) {
        this.id = id;
        this.characterId = characterId;
        this.caliber = caliber;
        this.quantity = quantity;
    }

    public String getId() { return id; }
    public String getCharacterId() { return characterId; }
    public String getCaliber() { return caliber; }
    public void setCaliber(String value) { caliber = value; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int value) { quantity = value; }
}
