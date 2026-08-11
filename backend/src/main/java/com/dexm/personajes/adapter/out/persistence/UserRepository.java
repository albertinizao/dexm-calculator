package com.dexm.personajes.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<UserEntity,String>{ Optional<UserEntity> findByGoogleSubject(String subject); }
