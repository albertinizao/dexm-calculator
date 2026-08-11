package com.dexm.personajes.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ShieldRepository extends JpaRepository<ShieldEntity,String>{ List<ShieldEntity> findByCharacterIdOrderByNameAsc(String c); Optional<ShieldEntity> findByIdAndCharacterId(String id,String c); }
