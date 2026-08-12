package com.dexm.personajes.adapter.out.persistence;

import java.util.List;

public interface CharacterAttributeModifierRepository extends FirestoreRepository<CharacterAttributeModifierEntity> {
    List<CharacterAttributeModifierEntity> findByCharacterId(String characterId);
    List<CharacterAttributeModifierEntity> findByCharacterIdAndAttributeKey(String characterId, String attributeKey);
    void deleteByCharacterId(String characterId);
    void deleteByCharacterIdAndAttributeKey(String characterId, String attributeKey);
}
