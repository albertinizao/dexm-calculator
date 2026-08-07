package com.dexm.personajes.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterAttributeModifierRepository extends JpaRepository<CharacterAttributeModifierEntity, String> {
    List<CharacterAttributeModifierEntity> findByCharacterId(String characterId);
    List<CharacterAttributeModifierEntity> findByCharacterIdAndAttributeKey(String characterId, String attributeKey);
    void deleteByCharacterId(String characterId);
    void deleteByCharacterIdAndAttributeKey(String characterId, String attributeKey);
}
