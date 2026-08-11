package com.dexm.personajes.adapter.out.persistence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface WeaponRepository extends JpaRepository<WeaponEntity,String> {
    List<WeaponEntity> findByCharacterIdOrderBySlotAsc(String characterId);
    Optional<WeaponEntity> findByIdAndCharacterId(String id, String characterId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select weapon from WeaponEntity weapon where weapon.id = :id and weapon.characterId = :characterId")
    Optional<WeaponEntity> findByIdAndCharacterIdForUpdate(@Param("id") String id, @Param("characterId") String characterId);
    Optional<WeaponEntity> findByCharacterIdAndSlot(String characterId, String slot);
}
