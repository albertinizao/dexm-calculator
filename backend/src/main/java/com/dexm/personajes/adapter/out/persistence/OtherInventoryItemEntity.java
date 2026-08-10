package com.dexm.personajes.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "other_inventory_items")
public class OtherInventoryItemEntity {
    @Id private String id;
    @Column(name = "character_id", nullable = false) private String characterId;
    @Column(nullable = false) private String name;
    @Column(columnDefinition = "text") private String description;
    @Column private String location;
    @Column(nullable = false) private int quantity;
    @Column(name = "unit_value", precision = 19, scale = 4) private BigDecimal unitValue;

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
