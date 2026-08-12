package com.dexm.personajes.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

public interface OtherInventoryItemRepository extends FirestoreRepository<OtherInventoryItemEntity> {
    List<OtherInventoryItemEntity> findByCharacterIdOrderByNameAsc(String characterId);
    Optional<OtherInventoryItemEntity> findByIdAndCharacterId(String id, String characterId);
    void deleteByCharacterId(String characterId);
}
