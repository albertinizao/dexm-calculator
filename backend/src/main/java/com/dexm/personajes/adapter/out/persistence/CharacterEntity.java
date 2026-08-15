package com.dexm.personajes.adapter.out.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class CharacterEntity {
    private String id;
    private String importSourceId;
    private String name;
    private String campaignId;
    private String ownerUserId;
    private String imageUrl;
    private int experience;
    private int level;
    private int evolutionPoints;
    private int geneticsPoints;
    private String uniqueAbilityDecisionsJson = "{}";
    private boolean closed;
    private String attributesJson;
    private String geneticsJson;
    private String creationMode = "empty";
    private String race;
    private boolean einherjer = true;
    private boolean awakened;
    private String einherjerOrigin;
    private Integer startingAge;
    private Integer awakeningAge;
    private Integer sheetAge;
    private String selectedMajorAttributesJson = "[]";
    private String creationWizardState = "empty";
    private List<String> editorEmails = new ArrayList<>();
    private boolean editorEmailsConfigured;
    private List<CharacterAttributeModifierEntity> modifiers = new ArrayList<>();
    private List<CharacterMinorAttributeValueEntity> minorAttributeValues = new ArrayList<>();
    private int aggregateVersion;
    private Instant createdAt;
    private Instant updatedAt;

    protected CharacterEntity() {}

    public CharacterEntity(String id, String name, int experience, String attributesJson, String geneticsJson) {
        this(id, null, name, null, experience, 1, attributesJson, geneticsJson);
    }

    public CharacterEntity(String id, String campaignId, String name, String imageUrl, int experience,
                           String attributesJson, String geneticsJson) {
        this(id, campaignId, name, imageUrl, experience, 1, attributesJson, geneticsJson);
    }

    public CharacterEntity(String id, String campaignId, String name, String imageUrl, int experience, int level,
                           String attributesJson, String geneticsJson) {
        this.id = id;
        this.campaignId = campaignId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.experience = experience;
        this.level = level;
        this.attributesJson = attributesJson;
        this.geneticsJson = geneticsJson;
        this.editorEmailsConfigured = true;
        this.aggregateVersion = 1;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getImportSourceId() { return importSourceId; }
    public void setImportSourceId(String value) { importSourceId = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getCampaignId() { return campaignId; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String value) { ownerUserId = value; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String value) { imageUrl = value; }
    public int getExperience() { return experience; }
    public void setExperience(int value) { experience = value; }
    public int getLevel() { return level; }
    public void setLevel(int value) { level = Math.max(1, value); }
    public int getEvolutionPoints() { return evolutionPoints; }
    public void setEvolutionPoints(int value) { evolutionPoints = Math.max(0, value); }
    public int getGeneticsPoints() { return geneticsPoints; }
    public void setGeneticsPoints(int value) { geneticsPoints = Math.max(0, value); }
    public String getUniqueAbilityDecisionsJson() { return uniqueAbilityDecisionsJson; }
    public void setUniqueAbilityDecisionsJson(String value) { uniqueAbilityDecisionsJson = value == null ? "{}" : value; }
    public boolean isClosed() { return closed; }
    public void setClosed(boolean value) { closed = value; }
    public String getAttributesJson() { return attributesJson; }
    public void setAttributesJson(String value) { attributesJson = value; }
    public String getGeneticsJson() { return geneticsJson; }
    public void setGeneticsJson(String value) { geneticsJson = value; }
    public String getCreationMode() { return creationMode; }
    public void setCreationMode(String value) { creationMode = value; }
    public String getRace() { return race; }
    public void setRace(String value) { race = value; }
    public boolean isEinherjer() { return einherjer; }
    public void setEinherjer(boolean value) { einherjer = value; }
    public boolean isAwakened() { return awakened; }
    public void setAwakened(boolean value) { awakened = value; }
    public String getSelectedMajorAttributesJson() { return selectedMajorAttributesJson; }
    public void setSelectedMajorAttributesJson(String value) { selectedMajorAttributesJson = value == null ? "[]" : value; }
    public String getCreationWizardState() { return creationWizardState; }
    public void setCreationWizardState(String value) { creationWizardState = value; }
    public String getEinherjerOrigin() { return einherjerOrigin; }
    public void setEinherjerOrigin(String value) { einherjerOrigin = value; }
    public Integer getStartingAge() { return startingAge; }
    public void setStartingAge(Integer value) { startingAge = value; }
    public Integer getAwakeningAge() { return awakeningAge; }
    public void setAwakeningAge(Integer value) { awakeningAge = value; }
    public Integer getSheetAge() { return sheetAge; }
    public void setSheetAge(Integer value) { sheetAge = value; }
    public List<String> getEditorEmails() { if (editorEmails == null) editorEmails = new ArrayList<>(); return editorEmails; }
    public void setEditorEmails(Collection<String> values) { editorEmails = new ArrayList<>(normalizeEmails(values)); editorEmailsConfigured = true; }
    public boolean isEditorEmailsConfigured() { return editorEmailsConfigured; }
    public void setEditorEmailsConfigured(boolean value) { editorEmailsConfigured = value; }
    public void addEditorEmail(String email) { editorEmailsConfigured = true; var normalized = normalizeEmail(email); if (!normalized.isBlank() && !getEditorEmails().contains(normalized)) editorEmails.add(normalized); }
    public void removeEditorEmail(String email) { editorEmailsConfigured = true; getEditorEmails().remove(normalizeEmail(email)); }
    public boolean hasEditorEmail(String email) { return getEditorEmails().contains(normalizeEmail(email)); }
    public List<CharacterAttributeModifierEntity> getModifiers() { if (modifiers == null) modifiers = new ArrayList<>(); return modifiers; }
    public void setModifiers(Collection<CharacterAttributeModifierEntity> values) { modifiers = values == null ? new ArrayList<>() : new ArrayList<>(values); }
    public List<CharacterMinorAttributeValueEntity> getMinorAttributeValues() { if (minorAttributeValues == null) minorAttributeValues = new ArrayList<>(); return minorAttributeValues; }
    public void setMinorAttributeValues(Collection<CharacterMinorAttributeValueEntity> values) { minorAttributeValues = values == null ? new ArrayList<>() : new ArrayList<>(values); }
    public int getAggregateVersion() { return aggregateVersion; }
    public void setAggregateVersion(int value) { aggregateVersion = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touch() { updatedAt = Instant.now(); }

    private static List<String> normalizeEmails(Collection<String> values) {
        var result = new LinkedHashSet<String>();
        if (values != null) for (var value : values) {
            var normalized = normalizeEmail(value);
            if (!normalized.isBlank()) result.add(normalized);
        }
        return new ArrayList<>(result);
    }

    private static String normalizeEmail(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
}
