package com.dexm.personajes.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TrainingActivityRepository extends JpaRepository<TrainingActivityEntity,String>{
 List<TrainingActivityEntity> findByCharacterIdOrderByStartAgeAscPriorityAsc(String characterId);
 void deleteByCharacterId(String characterId);
}
