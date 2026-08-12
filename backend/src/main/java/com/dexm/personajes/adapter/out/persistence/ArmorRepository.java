package com.dexm.personajes.adapter.out.persistence;
import java.util.*;
public interface ArmorRepository extends FirestoreRepository<ArmorEntity>{ List<ArmorEntity> findByCharacterIdOrderByNameAsc(String characterId); Optional<ArmorEntity> findByIdAndCharacterId(String id,String characterId); }
