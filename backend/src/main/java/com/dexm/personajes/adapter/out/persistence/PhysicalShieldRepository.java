package com.dexm.personajes.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PhysicalShieldRepository extends JpaRepository<PhysicalShieldEntity,String> { List<PhysicalShieldEntity> findByCharacterIdOrderByNameAsc(String characterId); Optional<PhysicalShieldEntity> findByIdAndCharacterId(String id,String characterId); }
