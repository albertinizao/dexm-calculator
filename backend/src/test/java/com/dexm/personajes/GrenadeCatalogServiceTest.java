package com.dexm.personajes;

import com.dexm.personajes.adapter.in.web.GrenadeCatalogController.GrenadeCatalogRequest;
import com.dexm.personajes.adapter.out.persistence.GrenadeCatalogEntity;
import com.dexm.personajes.adapter.out.persistence.GrenadeCatalogRepository;
import com.dexm.personajes.application.GrenadeCatalogService;
import com.dexm.personajes.application.GrenadeCatalogSeedService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrenadeCatalogServiceTest {
    @Test
    void creates_grenade_with_damage_profile() {
        var repository = mock(GrenadeCatalogRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new GrenadeCatalogService(repository);

        var result = service.create(new GrenadeCatalogRequest(" Granada básica ", "Descripción", 400, 100, 20));

        assertThat(result).containsEntry("name", "Granada básica").containsEntry("centralDamage", 400)
                .containsEntry("adjacentDamage", 100).containsEntry("damageDecay", 20).containsEntry("handGrenade", true)
                .containsEntry("type", null).containsEntry("official", false);
    }

    @Test
    void persists_additional_effect_literal() {
        var repository = mock(GrenadeCatalogRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new GrenadeCatalogService(repository);

        var result = service.create(new GrenadeCatalogRequest("Aturdidora", null, 0, 0, 0, true, null,
                "Resistencia 19/17/15/13 a distancia 0/1/2/3."));

        assertThat(result).containsEntry("additionalEffect", "Resistencia 19/17/15/13 a distancia 0/1/2/3.");
    }

    @Test
    void preserves_free_type_for_non_hand_grenade() {
        var repository = mock(GrenadeCatalogRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new GrenadeCatalogService(repository);

        var result = service.create(new GrenadeCatalogRequest("Lanzable", "Descripción", 80, 20, 5, false, "lanzagranadas"));

        assertThat(result).containsEntry("handGrenade", false).containsEntry("type", "lanzagranadas");
    }

    @Test
    void does_not_delete_official_grenade() {
        var repository = mock(GrenadeCatalogRepository.class);
        when(repository.findById("basic-grenade")).thenReturn(Optional.of(new GrenadeCatalogEntity("basic-grenade", "Básica", null, 400, 100, 20, true)));
        var service = new GrenadeCatalogService(repository);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.delete("basic-grenade"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Las granadas oficiales no se pueden borrar");
    }

    @Test
    void seeds_and_reconciles_the_complete_official_catalog() {
        var repository = mock(GrenadeCatalogRepository.class);
        var saved = new ArrayList<GrenadeCatalogEntity>();
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(invocation -> {
            GrenadeCatalogEntity entity = invocation.getArgument(0);
            saved.add(entity);
            return entity;
        });

        new GrenadeCatalogSeedService(repository).seedIfMissing();

        assertThat(saved).hasSize(20);
        assertThat(saved).extracting(GrenadeCatalogEntity::getId).doesNotHaveDuplicates()
                .contains("fragmentacion-estandar", "iluminacion-pesada");
        assertThat(saved.stream().filter(GrenadeCatalogEntity::isHandGrenade)).hasSize(6);
        assertThat(saved.stream().filter(item -> "LG".equals(item.getType()))).hasSize(8);
        assertThat(saved.stream().filter(item -> "LP".equals(item.getType()))).hasSize(6);
        assertThat(saved.stream().filter(item -> item.getId().equals("humo-pesado")).findFirst().orElseThrow().getAdditionalEffect())
                .isEqualTo("Humo radio 5 durante 5 asaltos; línea Puntería -4; 3+ casillas sin visión directa.");
        verify(repository, org.mockito.Mockito.never()).delete(any());
    }

    @Test
    void migrates_the_previous_official_basic_id_to_the_stable_id() {
        var repository = mock(GrenadeCatalogRepository.class);
        var legacy = new GrenadeCatalogEntity("basic-grenade", "Granada básica", "legacy", 1, 2, 3, true);
        when(repository.findAll()).thenReturn(List.of(legacy));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new GrenadeCatalogSeedService(repository).seedIfMissing();

        verify(repository).delete(legacy);
        verify(repository).save(org.mockito.ArgumentMatchers.argThat(item -> "fragmentacion-estandar".equals(item.getId())));
    }

    @Test
    void reconciles_existing_official_records_instead_of_leaving_stale_values() {
        var repository = mock(GrenadeCatalogRepository.class);
        var stale = new GrenadeCatalogEntity("fragmentacion-estandar", "Antigua", "legacy", 1, 2, 3,
                "efecto antiguo", false, "BAD", false);
        when(repository.findAll()).thenReturn(List.of(stale));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new GrenadeCatalogSeedService(repository).seedIfMissing();

        assertThat(stale.getName()).isEqualTo("Fragmentación estándar");
        assertThat(stale.getCentralDamage()).isEqualTo(400);
        assertThat(stale.getAdditionalEffect()).isEqualTo("—");
        assertThat(stale.isHandGrenade()).isTrue();
        assertThat(stale.getType()).isNull();
        assertThat(stale.isOfficial()).isTrue();
        verify(repository).save(stale);
    }
}
