package com.dexm.personajes.adapter.out.persistence;
import java.util.Optional;
public interface UserRepository extends FirestoreRepository<UserEntity>{ Optional<UserEntity> findByGoogleSubject(String subject); }
