package com.dexm.personajes.adapter.out.persistence;

/** Heavy inventory part of a history snapshot, kept outside the milestone document. */
public class MilestoneInventorySnapshotEntity {
    private String id;
    private String milestoneId;
    private String characterId;
    private String snapshotJson;
    protected MilestoneInventorySnapshotEntity() {}
    public MilestoneInventorySnapshotEntity(String milestoneId, String characterId, String snapshotJson) {
        this.id = milestoneId; this.milestoneId = milestoneId; this.characterId = characterId; this.snapshotJson = snapshotJson;
    }
    public String getId() { return id; }
    public String getMilestoneId() { return milestoneId; }
    public String getCharacterId() { return characterId; }
    public String getSnapshotJson() { return snapshotJson; }
}
