package com.dexm.personajes;

import com.dexm.personajes.adapter.out.persistence.WeaponCatalogEntity;
import com.dexm.personajes.adapter.out.persistence.WeaponCatalogRepository;
import com.dexm.personajes.application.WeaponCatalogService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeaponCatalogServiceTest {
    @Test void returnsOnlyWeaponsCompatibleWithRequestedSlotAndMatchingFilters() {
        var repository = mock(WeaponCatalogRepository.class);
        var service = new WeaponCatalogService(repository);
        var pistol = new WeaponCatalogEntity("pistol", "Beretta 92", "PISTOLA", "PEQUENA", bd(15), bd(1), "2", bd(85), bd(40), bd(25), bd(20), bd(4), null, bd(15), ".9mm", null, "/weapons/beretta.jpg", true);
        var rifle = new WeaponCatalogEntity("rifle", "Colt M-16", "FUSIL", "GRANDE", bd(50), bd(1), "2", bd(105), bd(50), bd(35), bd(25), bd(3), "3", bd(30), "5.56", null, "/weapons/m16.jpg", true);
        when(repository.findBySize("PEQUENA")).thenReturn(List.of(pistol));

        var result = service.search("SMALL_1", "beretta", "PISTOLA");

        assertThat(result).extracting(row -> row.get("id")).containsExactly("pistol");
        verify(repository, never()).findAll();
    }

    @Test void returnsGrenadeLaunchersWithTheirGrenadeAmmunitionAndNoWeaponDamage() {
        var repository = mock(WeaponCatalogRepository.class);
        var launcher = new WeaponCatalogEntity("m203", "M203", "LANZAGRANADAS", "PEQUENA", bd(25), bd(1), "1",
                bd(0), bd(0), bd(0), bd(0), bd(1), null, bd(1), "LG ligero",
                "Solo puede utilizarse acoplado a un fusil.", "/weapons/grenade-launchers/m203.png", true);
        when(repository.findBySize("PEQUENA")).thenReturn(List.of(launcher));

        var result = new WeaponCatalogService(repository).search("SMALL_1", "", "LANZAGRANADAS");

        assertThat(result).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("id", "m203")
                    .containsEntry("caliber", "LG ligero")
                    .containsEntry("range", bd(25))
                    .containsEntry("damageVital", bd(0))
                    .containsEntry("damageNormal", bd(0))
                    .containsEntry("damageLight", bd(0))
                    .containsEntry("damageVeryLight", bd(0));
        });
    }

    private BigDecimal bd(int value) { return BigDecimal.valueOf(value); }
}
