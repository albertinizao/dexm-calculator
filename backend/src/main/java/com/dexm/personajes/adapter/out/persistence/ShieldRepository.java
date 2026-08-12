package com.dexm.personajes.adapter.out.persistence;
import java.util.*;
public interface ShieldRepository extends FirestoreRepository<ShieldEntity>{ List<ShieldEntity> findByCharacterIdOrderByNameAsc(String c); Optional<ShieldEntity> findByIdAndCharacterId(String id,String c); }
