package com.dexm.personajes.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface WeaponRepository extends JpaRepository<WeaponEntity,String> {
    List<WeaponEntity> findByCharacterIdOrderBySlotAsc(String characterId);
    Optional<WeaponEntity> findByIdAndCharacterId(String id, String characterId);
    Optional<WeaponEntity> findByCharacterIdAndSlot(String characterId, String slot);
}
