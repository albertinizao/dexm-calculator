package com.dexm.personajes.adapter.out.persistence;
import java.util.List;
public interface TrainingActivityRepository extends FirestoreRepository<TrainingActivityEntity>{
 List<TrainingActivityEntity> findByCharacterIdOrderByStartAgeAscPriorityAsc(String characterId);
 void deleteByCharacterId(String characterId);
}
