package com.dexm.personajes.adapter.out.persistence;
import java.util.*;
public interface WeaponRepository extends FirestoreRepository<WeaponEntity> {
    List<WeaponEntity> findByCharacterIdOrderBySlotAsc(String characterId);
    Optional<WeaponEntity> findByIdAndCharacterId(String id, String characterId);
    Optional<WeaponEntity> findByIdAndCharacterIdForUpdate(String id, String characterId);
    Optional<WeaponEntity> findByCharacterIdAndSlot(String characterId, String slot);
}
