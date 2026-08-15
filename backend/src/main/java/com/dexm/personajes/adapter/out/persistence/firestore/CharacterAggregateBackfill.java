package com.dexm.personajes.adapter.out.persistence.firestore;

import com.dexm.personajes.adapter.out.persistence.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CharacterAggregateBackfill {
    private final Firestore firestore;
    private final ObjectMapper mapper;
    private final CharacterRepository characters;

    public CharacterAggregateBackfill(Firestore firestore, ObjectMapper mapper, CharacterRepository characters) {
        this.firestore = firestore; this.mapper = mapper; this.characters = characters;
    }

    public Result run() throws Exception {
        int charactersMigrated = 0, inventoryMigrated = 0, activitiesMigrated = 0, abilitiesMigrated = 0;
        for (CharacterEntity character : characters.findAll()) {
            var modifiers = readLegacy("attributeModifiers", CharacterAttributeModifierEntity.class, character.getId());
            var minors = readLegacy("minorAttributeValues", CharacterMinorAttributeValueEntity.class, character.getId());
            character.setModifiers(modifiers);
            character.setMinorAttributeValues(minors);
            character.setAggregateVersion(1);
            characters.save(character);

            var inventory = new CharacterInventoryAggregateEntity(character.getId());
            inventory.setWeapons(readLegacy("weapons", WeaponEntity.class, character.getId()));
            inventory.setAmmunition(readLegacy("ammunition", AmmunitionEntity.class, character.getId()));
            inventory.setArmors(readLegacy("armors", ArmorEntity.class, character.getId()));
            inventory.setShields(readLegacy("shields", ShieldEntity.class, character.getId()));
            inventory.setPhysicalShields(readLegacy("physicalShields", PhysicalShieldEntity.class, character.getId()));
            inventory.setOtherInventoryItems(readLegacy("otherInventoryItems", OtherInventoryItemEntity.class, character.getId()));
            save("characterInventories", character.getId(), inventory);
            inventoryMigrated++;

            var activities = new CharacterActivityAggregateEntity(character.getId());
            activities.setActivities(readLegacy("trainingActivities", TrainingActivityEntity.class, character.getId()));
            save("characterActivities", character.getId(), activities);
            activitiesMigrated++;

            var abilities = new CharacterAbilityStateEntity(character.getId());
            var latest = latestMilestone(character.getId());
            if (latest != null) {
                abilities.setSourceMilestoneId(latest.getId());
                JsonNode snapshot = mapper.readTree(latest.getSnapshotJson());
                abilities.setObtained(readTextArray(snapshot.path("abilities")));
                abilities.setPendingUnique(readTextArray(snapshot.path("pendingUniqueAbilities")));
            }
            save("characterAbilities", character.getId(), abilities);
            abilitiesMigrated++;
            charactersMigrated++;
        }
        return new Result(charactersMigrated, inventoryMigrated, activitiesMigrated, abilitiesMigrated);
    }

    private MilestoneEntity latestMilestone(String characterId) throws Exception {
        var docs = firestore.collection("milestones").whereEqualTo("characterId", characterId)
                .orderBy("createdAt").get().get().getDocuments();
        MilestoneEntity latest = null;
        for (DocumentSnapshot doc : docs) {
            if (!doc.exists()) continue;
            var candidate = mapper.readValue(doc.getString("json"), MilestoneEntity.class);
            if (candidate.isVisible() && (latest == null || candidate.getCreatedAt().isAfter(latest.getCreatedAt()))) latest = candidate;
        }
        return latest;
    }

    private List<String> readTextArray(JsonNode node) {
        if (node == null || !node.isArray()) return new ArrayList<>();
        var result = new ArrayList<String>(); node.forEach(value -> { if (value.isTextual()) result.add(value.asText()); }); return result;
    }

    private <T> List<T> readLegacy(String collection, Class<T> type, String characterId) throws Exception {
        var result = new ArrayList<T>();
        for (DocumentSnapshot doc : firestore.collection(collection).whereEqualTo("characterId", characterId).get().get().getDocuments()) {
            if (doc.exists()) result.add(mapper.readValue(doc.getString("json"), type));
        }
        return result;
    }

    private void save(String collection, String id, Object value) throws Exception {
        var document = new HashMap<String,Object>();
        document.put("id", id); document.put("characterId", id);
        document.putAll(mapper.convertValue(value, Map.class));
        document.put("json", mapper.writeValueAsString(value));
        firestore.collection(collection).document(id).set(document).get();
    }

    public record Result(int characters, int inventories, int activities, int abilities) {}
}

