package com.dexm.personajes.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

/** Persistence port used by application services; implementations are profile-specific. */
public interface FirestoreRepository<T> {
    <S extends T> S save(S entity);
    <S extends T> List<S> saveAll(Iterable<S> entities);
    Optional<T> findById(String id);
    boolean existsById(String id);
    List<T> findAll();
    long count();
    default <S extends T> S saveAndFlush(S entity) { return save(entity); }
    default void flush() { }
    void delete(T entity);
    void deleteById(String id);
    void deleteAll(Iterable<? extends T> entities);
    void deleteAll();
}
