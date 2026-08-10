package com.dexm.personajes.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OtherInventoryItemRepository extends JpaRepository<OtherInventoryItemEntity, String> {
    List<OtherInventoryItemEntity> findByCharacterIdOrderByNameAsc(String characterId);
    Optional<OtherInventoryItemEntity> findByIdAndCharacterId(String id, String characterId);
    void deleteByCharacterId(String characterId);
}
