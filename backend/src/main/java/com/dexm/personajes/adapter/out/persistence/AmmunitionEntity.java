package com.dexm.personajes.adapter.out.persistence;

public class AmmunitionEntity {
    private String id;
    private String characterId;
    private String type = "CALIBER";
    private String caliber;
    private String grenadeCatalogId;
    private int quantity;

    protected AmmunitionEntity() { }

    public AmmunitionEntity(String id, String characterId, String caliber, int quantity) {
        this(id, characterId, "CALIBER", caliber, null, quantity);
    }

    public AmmunitionEntity(String id, String characterId, String type, String caliber, String grenadeCatalogId, int quantity) {
        this.id = id;
        this.characterId = characterId;
        this.type = type == null || type.isBlank() ? "CALIBER" : type;
        this.caliber = caliber;
        this.grenadeCatalogId = grenadeCatalogId;
        this.quantity = quantity;
    }

    public String getId() { return id; }
    public String getCharacterId() { return characterId; }
    public String getType() { return type; }
    public void setType(String value) { type = value; }
    public String getCaliber() { return caliber; }
    public void setCaliber(String value) { caliber = value; }
    public String getGrenadeCatalogId() { return grenadeCatalogId; }
    public void setGrenadeCatalogId(String value) { grenadeCatalogId = value; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int value) { quantity = value; }
}
