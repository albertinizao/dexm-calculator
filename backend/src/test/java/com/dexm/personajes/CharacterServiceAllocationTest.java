package com.dexm.personajes;

import com.dexm.personajes.adapter.out.persistence.*;
import com.dexm.personajes.adapter.in.web.CharacterController;
import com.dexm.personajes.application.CharacterService;
import com.dexm.personajes.domain.CharacterRules;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CharacterServiceAllocationTest {
    @Mock CharacterRepository characters;
    @Mock MilestoneRepository milestones;
    @Mock AbilityRepository abilities;
    @Mock CharacterMinorAttributeValueRepository minorValues;
    @Mock MinorAttributeDefinitionRepository minorDefs;
    @Mock CharacterAttributeModifierRepository modifiers;

    private CharacterService service;
    private CharacterEntity character;
    private Map<String, Integer> attributes;
    private Map<String, Integer> genetics;

    @BeforeEach
    void setUp() throws Exception {
        attributes = new LinkedHashMap<>(CharacterRules.zeroValues(CharacterRules.ATTRIBUTES));
        genetics = new LinkedHashMap<>(CharacterRules.zeroValues(CharacterRules.GENETICS));
        character = new CharacterEntity("c1", "campaign", "Astrid", null, 250, attributesJson(), geneticsJson());
        when(characters.findById("c1")).thenReturn(Optional.of(character));
        when(characters.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(milestones.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(minorValues.findByCharacterId("c1")).thenReturn(List.of());
        when(minorDefs.findByCampaignIdOrderByNameAsc("campaign")).thenReturn(List.of());
        when(modifiers.findByCharacterId("c1")).thenReturn(List.of());
        when(abilities.findAll()).thenReturn(List.of());
        service = new CharacterService(characters, milestones, abilities, new ObjectMapper(),
                new com.dexm.personajes.application.MinorAttributeService(minorDefs, minorValues, characters, modifiers),
                minorValues, minorDefs, modifiers);
    }

    @Test
    void levelUpSubtractsOneHundredExperienceAndCreatesVisibleFinalMilestone() {
        var response = service.levelUp("c1", 2, 150, attributes, geneticsWithThree(), Map.of());

        assertThat(character.getLevel()).isEqualTo(2);
        assertThat(character.getExperience()).isEqualTo(150);
        assertThat(response).containsEntry("flow", "single").containsEntry("visible", true).containsEntry("final", true);
        var milestone = captureMilestone();
        assertThat(milestone.isVisible()).isTrue();
    }

    @Test
    void sequentialLevelsPersistHiddenThenVisibleSnapshots() {
        var hidden = service.levelUpAll("c1", 2, 150, attributes, geneticsWithThree(), Map.of(), false, false);
        assertThat(hidden).containsEntry("flow", "sequential-all").containsEntry("canContinue", true);
        assertThat(captureMilestone().isVisible()).isFalse();

        var visibleGenetics = new LinkedHashMap<>(geneticsWithThree());
        visibleGenetics.put("alfar", 2);
        visibleGenetics.put("valkiria", 1);
        var visible = service.levelUpAll("c1", 3, 50, attributes, visibleGenetics, Map.of(), true, true);
        assertThat(visible).containsEntry("nextAction", "Guardado").containsEntry("visible", true);
        assertThat(captureMilestone().isVisible()).isTrue();
        assertThat(character.getLevel()).isEqualTo(3);
        assertThat(character.getExperience()).isEqualTo(50);
    }

    @Test
    void rejectsBudgetOverflowAndInconsistentLevelExperience() {
        attributes.put("fisico", 71);
        assertThatThrownBy(() -> service.levelUp("c1", 2, 150, attributes, geneticsWithThree(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("budget");

        assertThatThrownBy(() -> service.levelUp("c1", 2, 149, CharacterRules.zeroValues(CharacterRules.ATTRIBUTES), geneticsWithThree(), Map.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("100 experience");
    }

    @Test
    void levelRewardIsThirtyFivePlusCurrentEvolutionCurveAndNotLevelBased() throws Exception {
        attributes.put("evolcurva", 4);
        character.setAttributesJson(attributesJson());
        service.levelUp("c1", 2, 150, attributes, geneticsWithThree(), Map.of());

        assertThat(character.getEvolutionPoints()).isEqualTo(39);
        assertThat(character.getGeneticsPoints()).isEqualTo(0);
    }

    @Test
    void requiresAllThreeGeneticPointsBeforeAcceptingLevel() {
        var requested = new LinkedHashMap<>(genetics);
        requested.put("heroe", 2);

        assertThatThrownBy(() -> service.levelUp("c1", 2, 150, attributes, requested, Map.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Exactly 3 genetic points");
    }

    @Test
    void savesNamedModifiersWithoutClosingVersion() {
        var requested = Map.of("fisico", List.of(new CharacterController.ModifierRequest("Armadura", 2)));

        var response = service.saveAttributeModifiers("c1", requested);

        verify(modifiers, never()).deleteByCharacterId("c1");
        verify(modifiers).save(any(CharacterAttributeModifierEntity.class));
        assertThat(((Map<?, ?>) response.get("character")).get("closed")).isEqualTo(false);
        assertThat(character.isClosed()).isFalse();
        verify(milestones, never()).save(any(MilestoneEntity.class));
    }

    @Test
    void synchronizesModifiersWithoutReplacingUnchangedRows() {
        var existing = new CharacterAttributeModifierEntity("m1", "c1", "fisico", "Armadura", 1);
        var removed = new CharacterAttributeModifierEntity("m2", "c1", "fisico", "Antiguo", 4);
        when(modifiers.findByCharacterId("c1")).thenReturn(List.of(existing, removed));
        var requested = Map.of("fisico", List.of(
                new CharacterController.ModifierRequest(" Armadura ", 3),
                new CharacterController.ModifierRequest("Nuevo", 2)));

        service.saveAttributeModifiers("c1", requested);

        verify(modifiers).delete(removed);
        verify(modifiers).save(existing);
        verify(modifiers).save(argThat(modifier -> "Nuevo".equals(modifier.getName()) && modifier.getValue() == 2));
        assertThat(existing.getValue()).isEqualTo(3);
    }

    @Test
    void predefinedMinorAttributeTotalIncludesModifier() {
        when(modifiers.findByCharacterIdAndAttributeKey("c1", "atractivo"))
                .thenReturn(List.of(new CharacterAttributeModifierEntity("m1", "c1", "atractivo", "Guapura extrema", 20)));

        var detail = service.attributeDetail("c1", "atractivo");

        assertThat(detail.total()).isEqualTo(20);
        assertThat(detail.plusOne()).isEqualTo(5);
        assertThat(detail.plusD6()).isEqualTo(4);
    }

    @Test
    void rejectsModifierWithBlankName() {
        var requested = Map.of("fisico", List.of(new CharacterController.ModifierRequest(" ", -1)));

        assertThatThrownBy(() -> service.saveAttributeModifiers("c1", requested))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
        verify(modifiers, never()).deleteByCharacterId("c1");
    }

    @Test
    void limitsOneGeneticToTwoNewRanksPerLevel() {
        var requested = new LinkedHashMap<>(genetics);
        requested.put("heroe", 3);

        assertThatThrownBy(() -> service.levelUp("c1", 2, 150, attributes, requested, Map.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at most 2");
    }

    @Test
    void limitsTotalGeneticAllocationToThreePointsPerLevel() {
        var requested = new LinkedHashMap<>(genetics);
        requested.put("heroe", 2);
        requested.put("norna", 2);

        assertThatThrownBy(() -> service.levelUp("c1", 2, 150, attributes, requested, Map.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Exactly 3 genetic points");
    }

    @Test
    void existingRanksAreFreeAndCannotBeReduced() throws Exception {
        attributes.put("fisico", 4);
        attributes.put("fuerza", 2);
        character.setAttributesJson(attributesJson());

        var requested = new LinkedHashMap<>(attributes);
        requested.put("fisico", 3);
        assertThatThrownBy(() -> service.levelUp("c1", 2, 150, requested, genetics, Map.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cannot be reduced");

        service.levelUp("c1", 2, 150, attributes, geneticsWithThree(), Map.of());
        assertThat(character.getEvolutionPoints()).isEqualTo(35);
    }

    @Test
    @SuppressWarnings("unchecked")
    void lastUpgradeComparesTheTwoLatestClosedVersions() throws Exception {
        var previousAttributes = new LinkedHashMap<>(attributes); previousAttributes.put("fisico", 4);
        var currentAttributes = new LinkedHashMap<>(previousAttributes); currentAttributes.put("fisico", 5);
        var mapper = new ObjectMapper();
        var previousSnapshot = mapper.writeValueAsString(Map.of("attributes", previousAttributes, "genetics", genetics, "minorAttributes", Map.of(), "abilities", List.of("Vigía")));
        var currentSnapshot = mapper.writeValueAsString(Map.of("attributes", currentAttributes, "genetics", genetics, "minorAttributes", Map.of(), "abilities", List.of("Vigía", "Reflejos")));
        var current = new MilestoneEntity("m2", "c1", 2, 50, currentSnapshot, "{}", "[\"Reflejos\"]");
        var previous = new MilestoneEntity("m1", "c1", 1, 50, previousSnapshot, "{}", "[\"Vigía\"]");
        when(milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc("c1")).thenReturn(List.of(current, previous));

        var result = service.lastUpgrade("c1");

        assertThat(result).containsEntry("available", true);
        assertThat((List<Map<String, Object>>) result.get("scores")).anyMatch(change -> change.get("key").equals("fisico") && change.get("before").equals(4) && change.get("after").equals(5));
        assertThat((List<Map<String, Object>>) result.get("bonuses")).anyMatch(change -> change.get("key").equals("fisico") && change.get("plusOne").equals(1));
        assertThat((Set<String>) result.get("abilities")).containsExactly("Reflejos");
    }

    private MilestoneEntity captureMilestone() {
        var captor = ArgumentCaptor.forClass(MilestoneEntity.class);
        verify(milestones, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().get(captor.getAllValues().size() - 1);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> attributesFromCharacter() {
        try { return new ObjectMapper().readValue(character.getAttributesJson(), Map.class); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    private String attributesJson() throws Exception { return new ObjectMapper().writeValueAsString(attributes); }
    private String geneticsJson() throws Exception { return new ObjectMapper().writeValueAsString(genetics); }

    private Map<String, Integer> geneticsWithThree() {
        var result = new LinkedHashMap<>(genetics);
        result.put("heroe", 2);
        result.put("norna", 1);
        return result;
    }
}
