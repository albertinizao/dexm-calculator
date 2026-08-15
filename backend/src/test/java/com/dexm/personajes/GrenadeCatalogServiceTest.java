package com.dexm.personajes;

import com.dexm.personajes.adapter.in.web.GrenadeCatalogController.GrenadeCatalogRequest;
import com.dexm.personajes.adapter.out.persistence.GrenadeCatalogEntity;
import com.dexm.personajes.adapter.out.persistence.GrenadeCatalogRepository;
import com.dexm.personajes.application.GrenadeCatalogService;
import org.junit.jupiter.api.Test;

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

}
