package com.dexm.personajes.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ArmorRepository extends JpaRepository<ArmorEntity,String>{ List<ArmorEntity> findByCharacterIdOrderByNameAsc(String characterId); Optional<ArmorEntity> findByIdAndCharacterId(String id,String characterId); }
