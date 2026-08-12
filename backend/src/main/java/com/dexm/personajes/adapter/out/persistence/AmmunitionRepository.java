package com.dexm.personajes.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

public interface AmmunitionRepository extends FirestoreRepository<AmmunitionEntity> {
    List<AmmunitionEntity> findByCharacterIdOrderByCaliberAsc(String characterId);
    Optional<AmmunitionEntity> findByIdAndCharacterId(String id, String characterId);
    Optional<AmmunitionEntity> findByCharacterIdAndCaliber(String characterId, String caliber);

    Optional<AmmunitionEntity> findByIdAndCharacterIdForUpdate(String id, String characterId);

    Optional<AmmunitionEntity> findByCharacterIdAndCaliberForUpdate(String characterId, String caliber);
}
