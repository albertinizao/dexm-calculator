package com.dexm.personajes.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

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

    @Column(name = "exact_score", nullable = false, precision = 12, scale = 8)
    private BigDecimal exactScore = BigDecimal.ZERO;

    @Column(name = "source", nullable = false, length = 120)
    private String source = "MANUAL";

    protected CharacterAttributeModifierEntity() {
    }

    public CharacterAttributeModifierEntity(String id, String characterId, String attributeKey, String name, int value) {
        this.id = id;
        this.characterId = characterId;
        this.attributeKey = attributeKey;
        this.name = name;
        this.score = value;
        this.exactScore = BigDecimal.valueOf(value);
    }

    public CharacterAttributeModifierEntity(String id, String characterId, String attributeKey, String name, int value, String source) {
        this(id, characterId, attributeKey, name, value);
        this.source = source == null ? "MANUAL" : source;
    }

    public CharacterAttributeModifierEntity(String id, String characterId, String attributeKey, String name, BigDecimal exactValue, String source) {
        this(id, characterId, attributeKey, name, exactValue.setScale(0, java.math.RoundingMode.HALF_UP).intValue(), source);
        this.exactScore = exactValue;
    }

    public String getId() { return id; }
    public String getCharacterId() { return characterId; }
    public String getAttributeKey() { return attributeKey; }
    public String getName() { return name; }
    public int getScore() { return score; }
    public int getValue() { return score; }
    public BigDecimal getExactValue() { return exactScore == null ? BigDecimal.valueOf(score) : exactScore; }
    public void setScore(int score) { this.score = score; this.exactScore = BigDecimal.valueOf(score); }
    public void setValue(int value) { this.score = value; this.exactScore = BigDecimal.valueOf(value); }
    public void setExactValue(BigDecimal value) { this.exactScore = value; this.score = value.setScale(0, java.math.RoundingMode.HALF_UP).intValue(); }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source == null ? "MANUAL" : source; }
}
