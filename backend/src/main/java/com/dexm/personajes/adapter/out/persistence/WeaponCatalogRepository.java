package com.dexm.personajes.adapter.out.persistence;

import java.util.List;

public interface WeaponCatalogRepository extends FirestoreRepository<WeaponCatalogEntity> {
    List<WeaponCatalogEntity> findBySize(String size);
}
