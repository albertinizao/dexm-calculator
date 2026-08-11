package com.dexm.personajes.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AmmunitionRepository extends JpaRepository<AmmunitionEntity, String> {
    List<AmmunitionEntity> findByCharacterIdOrderByCaliberAsc(String characterId);
    Optional<AmmunitionEntity> findByIdAndCharacterId(String id, String characterId);
    Optional<AmmunitionEntity> findByCharacterIdAndCaliber(String characterId, String caliber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from AmmunitionEntity item where item.id = :id and item.characterId = :characterId")
    Optional<AmmunitionEntity> findByIdAndCharacterIdForUpdate(@Param("id") String id, @Param("characterId") String characterId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from AmmunitionEntity item where item.characterId = :characterId and item.caliber = :caliber")
    Optional<AmmunitionEntity> findByCharacterIdAndCaliberForUpdate(@Param("characterId") String characterId, @Param("caliber") String caliber);
}
