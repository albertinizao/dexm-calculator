package com.dexm.personajes.adapter.out.persistence;

import java.math.BigDecimal;

public class OtherInventoryItemEntity {
    private String id;
    private String characterId;
    private String name;
    private String description;
    private String location;
    private int quantity;
    private BigDecimal unitValue;

    protected OtherInventoryItemEntity() { }

    public OtherInventoryItemEntity(String id, String characterId, String name, String description,
                                    String location, int quantity, BigDecimal unitValue) {
        this.id = id; this.characterId = characterId; this.name = name; this.description = description;
        this.location = location; this.quantity = quantity; this.unitValue = unitValue;
    }
    public String getId() { return id; }
    public String getCharacterId() { return characterId; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getLocation() { return location; }
    public void setLocation(String value) { location = value; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int value) { quantity = value; }
    public BigDecimal getUnitValue() { return unitValue; }
    public void setUnitValue(BigDecimal value) { unitValue = value; }
}
