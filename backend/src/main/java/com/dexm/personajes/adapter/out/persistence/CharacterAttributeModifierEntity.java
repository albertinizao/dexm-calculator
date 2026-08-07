package com.dexm.personajes.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "character_attribute_modifiers", uniqueConstraints = @UniqueConstraint(columnNames = {"character_id", "attribute_key", "name"}))
public class CharacterAttributeModifierEntity {
    @Id
    private String id;

    @Column(name = "character_id", nullable = false)
    private String characterId;

    @Column(name = "attribute_key", nullable = false)
    private String attributeKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "source", nullable = false)
    private String source = "MANUAL";

    protected CharacterAttributeModifierEntity() {
    }

    public CharacterAttributeModifierEntity(String id, String characterId, String attributeKey, String name, int value) {
        this.id = id;
        this.characterId = characterId;
        this.attributeKey = attributeKey;
        this.name = name;
        this.score = value;
    }

    public CharacterAttributeModifierEntity(String id, String characterId, String attributeKey, String name, int value, String source) {
        this(id, characterId, attributeKey, name, value);
        this.source = source == null ? "MANUAL" : source;
    }

    public String getId() { return id; }
    public String getCharacterId() { return characterId; }
    public String getAttributeKey() { return attributeKey; }
    public String getName() { return name; }
    public int getScore() { return score; }
    public int getValue() { return score; }
    public void setScore(int score) { this.score = score; }
    public void setValue(int value) { this.score = value; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source == null ? "MANUAL" : source; }
}
