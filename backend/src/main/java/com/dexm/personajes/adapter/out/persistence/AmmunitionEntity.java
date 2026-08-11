package com.dexm.personajes.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ammunition_inventory",
        uniqueConstraints = @UniqueConstraint(name = "uk_ammunition_character_caliber", columnNames = {"character_id", "caliber"}))
public class AmmunitionEntity {
    @Id
    private String id;
    @Column(name = "character_id", nullable = false)
    private String characterId;
    @Column(nullable = false, length = 255)
    private String caliber;
    @Column(nullable = false)
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
