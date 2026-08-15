package com.dexm.personajes;

import com.dexm.personajes.adapter.in.web.CharacterController.AmmunitionRequest;
import com.dexm.personajes.adapter.out.persistence.AmmunitionEntity;
import com.dexm.personajes.adapter.out.persistence.AmmunitionRepository;
import com.dexm.personajes.adapter.out.persistence.CharacterRepository;
import com.dexm.personajes.adapter.out.persistence.WeaponCatalogEntity;
import com.dexm.personajes.adapter.out.persistence.WeaponCatalogRepository;
import com.dexm.personajes.adapter.out.persistence.WeaponEntity;
import com.dexm.personajes.adapter.out.persistence.WeaponRepository;
import com.dexm.personajes.adapter.out.persistence.GrenadeCatalogEntity;
import com.dexm.personajes.adapter.out.persistence.GrenadeCatalogRepository;
import com.dexm.personajes.application.AmmunitionInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmmunitionInventoryServiceTest {
    @Mock CharacterRepository characters;
    @Mock AmmunitionRepository ammunition;
    @Mock WeaponCatalogRepository weaponCatalog;
    @Mock WeaponRepository weapons;
    @Mock GrenadeCatalogRepository grenades;

    private AmmunitionInventoryService service;

    @BeforeEach
    void setUp() {
        service = new AmmunitionInventoryService(characters, ammunition, weaponCatalog, weapons, grenades);
        when(characters.existsById("character-1")).thenReturn(true);
    }

    @Test
    void create_should_accumulate_existing_caliber() {
        stubAllowedCaliber();
        when(ammunition.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var existing = new AmmunitionEntity("ammo-1", "character-1", ".45 ACP", 4);
        when(ammunition.findByCharacterIdAndCaliberForUpdate("character-1", ".45 ACP")).thenReturn(Optional.of(existing));

        var result = service.create("character-1", new AmmunitionRequest(" .45 ACP ", 6));

        assertThat(result).containsEntry("id", "ammo-1").containsEntry("caliber", ".45 ACP").containsEntry("quantity", 10);
    }

    @Test
    void decrement_should_delete_stack_when_last_round_is_consumed() {
        var existing = new AmmunitionEntity("ammo-1", "character-1", ".45 ACP", 5);
        when(ammunition.findByIdAndCharacterIdForUpdate("ammo-1", "character-1")).thenReturn(Optional.of(existing));

        assertThat(service.decrement("character-1", "ammo-1", -5)).isNull();

        verify(ammunition).delete(existing);
        verify(ammunition, never()).save(existing);
    }

    @Test
    void decrement_should_reject_when_stack_is_too_small_or_amount_is_not_supported() {
        var existing = new AmmunitionEntity("ammo-1", "character-1", ".45 ACP", 4);
        when(ammunition.findByIdAndCharacterIdForUpdate("ammo-1", "character-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.decrement("character-1", "ammo-1", -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No hay suficiente munición");
        assertThatThrownBy(() -> service.decrement("character-1", "ammo-1", -2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La cantidad a descontar debe ser -1, -5 o -10");
    }

    @Test
    void create_should_reject_non_positive_quantity() {

        assertThatThrownBy(() -> service.create("character-1", new AmmunitionRequest(".45 ACP", 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La cantidad debe ser positiva");
    }

    @Test
    void decrement_should_remove_one_grenade_from_stack() {
        var existing = new AmmunitionEntity("ammo-1", "character-1", "GRENADE", null, "basic-grenade", 4);
        when(ammunition.findByIdAndCharacterIdForUpdate("ammo-1", "character-1")).thenReturn(Optional.of(existing));
        when(ammunition.save(existing)).thenReturn(existing);

        var result = service.decrement("character-1", "ammo-1", -1);

        assertThat(result).containsEntry("quantity", 3);
        verify(ammunition).save(existing);
        verify(ammunition, never()).delete(existing);
    }

    @Test
    void create_should_accumulate_grenade_stack_by_catalog() {
        var grenade = new GrenadeCatalogEntity("basic-grenade", "Granada básica", "", 400, 100, 20, true);
        when(grenades.findById("basic-grenade")).thenReturn(Optional.of(grenade));
        when(ammunition.findByCharacterIdAndTypeAndGrenadeCatalogIdForUpdate("character-1", "GRENADE", "basic-grenade"))
                .thenReturn(Optional.of(new AmmunitionEntity("ammo-1", "character-1", "GRENADE", null, "basic-grenade", 2)));
        when(ammunition.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create("character-1", new AmmunitionRequest(null, 3, "GRENADE", "basic-grenade"));

        assertThat(result).containsEntry("type", "GRENADE").containsEntry("grenadeCatalogId", "basic-grenade").containsEntry("quantity", 5);
    }

    @Test
    void consume_grenade_should_delete_last_unit_from_stack() {
        var existing = new AmmunitionEntity("ammo-1", "character-1", "GRENADE", null, "basic-grenade", 1);
        when(ammunition.findByIdAndCharacterIdForUpdate("ammo-1", "character-1")).thenReturn(Optional.of(existing));
        when(grenades.findById("basic-grenade")).thenReturn(Optional.of(new GrenadeCatalogEntity("basic-grenade", "Básica", null, 400, 100, 20, true, null, true)));

        var result = service.consumeGrenade("character-1", "ammo-1");

        assertThat(result).containsEntry("type", "GRENADE").containsEntry("quantity", 0);
        verify(ammunition).delete(existing);
        verify(ammunition, never()).save(existing);
    }

    @Test
    void reload_should_consume_exact_weapon_capacity() {
        var weapon = weaponWithCapacityAndCaliber(new BigDecimal("10.0"), ".45 ACP");
        when(weapon.getId()).thenReturn("weapon-1");
        var existing = new AmmunitionEntity("ammo-1", "character-1", ".45 ACP", 10);
        when(weapons.findByIdAndCharacterIdForUpdate("weapon-1", "character-1")).thenReturn(Optional.of(weapon));
        when(ammunition.findByCharacterIdAndCaliberForUpdate("character-1", ".45 ACP")).thenReturn(Optional.of(existing));

        var result = service.reload("character-1", "weapon-1");

        assertThat(existing.getQuantity()).isEqualTo(10);
        assertThat(result).containsEntry("weaponId", "weapon-1").containsEntry("caliber", ".45 ACP")
                .containsEntry("requested", 10).containsEntry("consumed", 10).containsEntry("remaining", 0)
                .containsEntry("missing", 0).containsEntry("complete", true).containsEntry("loadedBullets", 10);
        verify(weapon).setLoadedBullets(BigDecimal.TEN);
        verify(weapons).save(weapon);
        verify(ammunition).delete(existing);
        verify(ammunition, never()).save(existing);
    }

    @Test
    void reload_should_reject_weapon_without_caliber() {
        var weapon = weaponWithCapacityAndCaliber(BigDecimal.TEN, " ");
        when(weapons.findByIdAndCharacterIdForUpdate("weapon-1", "character-1")).thenReturn(Optional.of(weapon));

        assertThatThrownBy(() -> service.reload("character-1", "weapon-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El arma no tiene un calibre configurado");
        verify(ammunition, never()).findByCharacterIdAndCaliberForUpdate(any(), any());
    }

    @Test
    void reload_should_report_all_capacity_missing_when_caliber_ammunition_is_missing() {
        var weapon = weaponWithCapacityAndCaliber(BigDecimal.TEN, ".45 ACP");
        when(weapon.getId()).thenReturn("weapon-1");
        when(weapons.findByIdAndCharacterIdForUpdate("weapon-1", "character-1")).thenReturn(Optional.of(weapon));
        when(ammunition.findByCharacterIdAndCaliberForUpdate("character-1", ".45 ACP")).thenReturn(Optional.empty());

        var result = service.reload("character-1", "weapon-1");

        assertThat(result).containsEntry("weaponId", "weapon-1").containsEntry("caliber", ".45 ACP")
                .containsEntry("requested", 10).containsEntry("consumed", 0).containsEntry("remaining", 0)
                .containsEntry("missing", 10).containsEntry("complete", false);
    }

    @Test
    void reload_should_consume_available_ammunition_when_it_is_insufficient() {
        var weapon = weaponWithCapacityAndCaliber(BigDecimal.TEN, ".45 ACP");
        when(weapon.getId()).thenReturn("weapon-1");
        var existing = new AmmunitionEntity("ammo-1", "character-1", ".45 ACP", 9);
        when(weapons.findByIdAndCharacterIdForUpdate("weapon-1", "character-1")).thenReturn(Optional.of(weapon));
        when(ammunition.findByCharacterIdAndCaliberForUpdate("character-1", ".45 ACP")).thenReturn(Optional.of(existing));

        var result = service.reload("character-1", "weapon-1");

        assertThat(existing.getQuantity()).isEqualTo(9);
        assertThat(result).containsEntry("weaponId", "weapon-1").containsEntry("caliber", ".45 ACP")
                .containsEntry("requested", 10).containsEntry("consumed", 9).containsEntry("remaining", 0)
                .containsEntry("missing", 1).containsEntry("complete", false).containsEntry("loadedBullets", 9);
        verify(weapon).setLoadedBullets(BigDecimal.valueOf(9));
        verify(weapons).save(weapon);
        verify(ammunition).delete(existing);
        verify(ammunition, never()).save(existing);
    }

    @Test
    void reload_should_increment_loaded_bullets_by_consumed_without_exceeding_capacity() {
        var weapon = weaponWithCapacityAndCaliber(BigDecimal.TEN, ".45 ACP");
        when(weapon.getLoadedBullets()).thenReturn(BigDecimal.valueOf(8));
        when(weapon.getId()).thenReturn("weapon-1");
        var existing = new AmmunitionEntity("ammo-1", "character-1", ".45 ACP", 5);
        when(weapons.findByIdAndCharacterIdForUpdate("weapon-1", "character-1")).thenReturn(Optional.of(weapon));
        when(ammunition.findByCharacterIdAndCaliberForUpdate("character-1", ".45 ACP")).thenReturn(Optional.of(existing));

        var result = service.reload("character-1", "weapon-1");

        assertThat(existing.getQuantity()).isEqualTo(3);
        assertThat(result).containsEntry("consumed", 2).containsEntry("remaining", 3)
                .containsEntry("missing", 0).containsEntry("complete", true).containsEntry("loadedBullets", 10);
        verify(weapon).setLoadedBullets(BigDecimal.TEN);
        verify(weapons).save(weapon);
        verify(ammunition).save(existing);
    }

    @Test
    void reload_should_reject_non_integral_capacity_without_rounding() {
        var weapon = weaponWithCapacity(new BigDecimal("10.5"));
        when(weapons.findByIdAndCharacterIdForUpdate("weapon-1", "character-1")).thenReturn(Optional.of(weapon));

        assertThatThrownBy(() -> service.reload("character-1", "weapon-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La capacidad del arma debe ser un entero positivo");
        verify(ammunition, never()).findByCharacterIdAndCaliberForUpdate(any(), any());
    }

    @Test
    void reload_should_reject_weapon_owned_by_another_character() {
        when(weapons.findByIdAndCharacterIdForUpdate("weapon-1", "character-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reload("character-1", "weapon-1"))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessage("Arma no encontrada");
        verify(ammunition, never()).findByCharacterIdAndCaliberForUpdate(any(), any());
    }

    @Test
    void reload_should_find_weapon_in_character_inventory_when_derived_for_update_query_misses() {
        var weapon = weaponWithCapacityAndCaliber(BigDecimal.TEN, ".45 ACP");
        when(weapon.getId()).thenReturn("weapon-1");
        when(weapons.findByIdAndCharacterIdForUpdate("weapon-1", "character-1")).thenReturn(Optional.empty());
        when(weapons.findByCharacterIdOrderBySlotAsc("character-1")).thenReturn(List.of(weapon));
        when(ammunition.findByCharacterIdAndCaliberForUpdate("character-1", ".45 ACP")).thenReturn(Optional.empty());

        var result = service.reload("character-1", "weapon-1");

        assertThat(result).containsEntry("weaponId", "weapon-1")
                .containsEntry("requested", 10).containsEntry("consumed", 0)
                .containsEntry("missing", 10).containsEntry("complete", false);
    }

    private WeaponEntity weaponWithCapacity(BigDecimal capacity) {
        var weapon = org.mockito.Mockito.mock(WeaponEntity.class);
        when(weapon.getCapacity()).thenReturn(capacity);
        return weapon;
    }

    private WeaponEntity weaponWithCapacityAndCaliber(BigDecimal capacity, String caliber) {
        var weapon = weaponWithCapacity(capacity);
        when(weapon.getCaliber()).thenReturn(caliber);
        return weapon;
    }

    private WeaponCatalogEntity catalogCaliber(String caliber) {
        var item = org.mockito.Mockito.mock(WeaponCatalogEntity.class);
        when(item.isOfficial()).thenReturn(true);
        when(item.getCaliber()).thenReturn(caliber);
        return item;
    }

    private void stubAllowedCaliber() {
        var weapon = org.mockito.Mockito.mock(WeaponEntity.class);
        when(weapon.getCaliber()).thenReturn(".45 ACP");
        when(weapons.findByCharacterIdOrderBySlotAsc("character-1")).thenReturn(List.of(weapon));
        when(ammunition.findByCharacterIdOrderByCaliberAsc("character-1")).thenReturn(List.of());
    }
}
