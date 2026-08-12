package com.dexm.personajes.adapter.out.persistence;
import java.util.*;
public interface PhysicalShieldRepository extends FirestoreRepository<PhysicalShieldEntity> { List<PhysicalShieldEntity> findByCharacterIdOrderByNameAsc(String characterId); Optional<PhysicalShieldEntity> findByIdAndCharacterId(String id,String characterId); }
