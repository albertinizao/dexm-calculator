package com.dexm.personajes.adapter.out.persistence;

import java.math.BigDecimal;

public class CharacterAttributeModifierEntity {
    private String id;

    private String characterId;

    private String attributeKey;

    private String name;

    private int score;

    private BigDecimal exactScore = BigDecimal.ZERO;

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
