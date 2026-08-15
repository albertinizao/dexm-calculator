package com.dexm.personajes;

import com.dexm.personajes.adapter.out.persistence.*;
import com.dexm.personajes.adapter.in.web.CharacterController;
import com.dexm.personajes.application.CharacterService;
import com.dexm.personajes.application.OfficialCatalogService;
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
    @Mock OfficialCatalogService officialCatalog;

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
        when(officialCatalog.abilities()).thenReturn(List.of());
        service = new CharacterService(characters, milestones, abilities, new ObjectMapper(),
                new com.dexm.personajes.application.MinorAttributeService(minorDefs, minorValues, characters, modifiers),
                minorValues, minorDefs, modifiers, null, officialCatalog);
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
    void levelRewardIncludesEvolutionCurveModifiers() {
        when(modifiers.findByCharacterId("c1")).thenReturn(List.of(
                new CharacterAttributeModifierEntity("m1", "c1", "evolcurva", "Bendición", 15)));

        service.levelUp("c1", 2, 150, attributes, geneticsWithThree(), Map.of());

        assertThat(character.getEvolutionPoints()).isEqualTo(50);
    }
    @Test
    void capsEvolutionCurveAtAverageMajorAttributeRank() throws Exception {
        attributes.put("fisico", 1);
        attributes.put("agilidad", 1);
        attributes.put("percepcion", 1);
        attributes.put("mente", 1);
        attributes.put("estudio", 3);
        attributes.put("evolcurva", 5);
        character.setEvolutionPoints(100);

        service.levelUp("c1", 2, 150, attributes, geneticsWithThree(), Map.of());

        assertThat(attributesFromCharacter()).containsEntry("evolcurva", 1);
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
    void beginningEditOnlyReturnsTheChangedEditState() {
        character.setClosed(true);

        var response = service.beginEdit("c1");

        assertThat(response).containsExactlyEntriesOf(Map.of("closed", false));
        assertThat(character.isClosed()).isFalse();
        verify(characters).save(character);
        verify(modifiers, never()).findByCharacterId("c1");
        verify(minorValues, never()).findByCharacterId("c1");
    }

    @Test
    void savesSignedModifiersForDerivedStats() {
        var requested = Map.of(
                "vida", List.of(new CharacterController.ModifierRequest("Herida", -7)),
                "defensaCuerpo", List.of(new CharacterController.ModifierRequest("Escudo", 2)));

        service.saveAttributeModifiers("c1", requested);

        verify(modifiers).save(argThat(modifier -> "vida".equals(modifier.getAttributeKey())
                && modifier.getValue() == -7));
        verify(modifiers).save(argThat(modifier -> "defensaCuerpo".equals(modifier.getAttributeKey())
                && modifier.getValue() == 2));
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
    void importsLegacyCodeAtomicallyAndOmitsZeroExtras() {
        var code = new StringBuilder("nivel:10&&experiencia:7&&evolGuardado:2&&");
        code.append("heroe:1&&norna:2&&alfar:0&&valkiria:0&&dvergr:0&&risa:0&&");
        CharacterRules.ATTRIBUTES.forEach(key -> code.append(key).append(":3&&").append(key).append("Extra:")
                .append(key.equals("fisico") ? "4" : "0").append("&&"));

        var imported = service.importLegacy(code.toString());

        assertThat(imported).containsEntry("level", 10).containsEntry("experience", 7).containsEntry("evolutionPoints", 2);
        @SuppressWarnings("unchecked") var extras = (Map<String, Integer>) imported.get("extras");
        assertThat(extras).containsEntry("fisico", 4).doesNotContainKey("agilidad");
    }

    @Test
    void exportsClosedLegacyCodeWithModifierTotals() {
        character.setClosed(true);
        var attrs = attributesFromCharacter();
        attrs.put("fisico", 5);
        try { character.setAttributesJson(new ObjectMapper().writeValueAsString(attrs)); } catch (Exception e) { throw new AssertionError(e); }
        when(modifiers.findByCharacterId("c1")).thenReturn(List.of(
                new CharacterAttributeModifierEntity("m1", "c1", "fisico", "A", 2),
                new CharacterAttributeModifierEntity("m2", "c1", "fisico", "B", 3)));

        var code = service.exportLegacy("c1");

        assertThat(code).contains("nivel:1&&").contains("experiencia:250&&").contains("fisico:5&&fisicoExtra:5&&");
    }

    @Test
    void savesLegacyEvolutionPointsWithoutApplyingCurrentAllocationBudget() {
        var response = service.save("c1", "Astrid", 1, 7, attributes, genetics, Map.of(), true, true, 2);

        assertThat(character.getEvolutionPoints()).isEqualTo(2);
        assertThat(character.isClosed()).isTrue();
        assertThat(response).containsKey("character");
    }

    @Test
    void savesPortraitOnTheSameEntityUsedByAllocation() throws Exception {
        var reloaded = new CharacterEntity("c1", "campaign", "Astrid", null, 250, attributesJson(), geneticsJson());
        when(characters.findById("c1")).thenReturn(Optional.of(character), Optional.of(reloaded));
        var portrait = "data:image/jpeg;base64,AA==";

        service.save("c1", "Astrid", 1, 250, attributes, genetics, Map.of(), true, true, 2,
                null, null, null, null, null, null, true, portrait);

        var saved = ArgumentCaptor.forClass(CharacterEntity.class);
        verify(characters, atLeastOnce()).save(saved.capture());
        assertThat(saved.getAllValues()).anyMatch(candidate -> candidate == character
                && portrait.equals(candidate.getImageUrl()));
    }

    @Test
    void omittedPortraitKeepsExistingImage() {
        var existing = "https://example.com/portrait.jpg";
        character.setImageUrl(existing);

        service.save("c1", "Astrid", 1, 250, attributes, genetics, Map.of(), true, true, 2);

        assertThat(character.getImageUrl()).isEqualTo(existing);
    }

    @Test
    void explicitNullPortraitRemovesExistingImage() {
        character.setImageUrl("https://example.com/portrait.jpg");

        service.save("c1", "Astrid", 1, 250, attributes, genetics, Map.of(), true, true, 2,
                null, null, null, null, null, null, true, null);

        assertThat(character.getImageUrl()).isNull();
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

    @Test
    @SuppressWarnings("unchecked")
    void lastUpgradeReportsAddedRemovedAndChangedModifiers() throws Exception {
        var mapper = new ObjectMapper();
        var previousModifiers = Map.of("fisico", List.of(
                Map.of("name", "Armadura", "value", 1), Map.of("name", "Antiguo", "value", 4)));
        var currentModifiers = Map.of("fisico", List.of(
                Map.of("name", "Armadura", "value", 3), Map.of("name", "Nuevo", "value", 2)));
        var previousSnapshot = mapper.writeValueAsString(Map.of("attributes", attributes, "genetics", genetics,
                "minorAttributes", Map.of(), "modifiers", previousModifiers, "abilities", List.of()));
        var currentSnapshot = mapper.writeValueAsString(Map.of("attributes", attributes, "genetics", genetics,
                "minorAttributes", Map.of(), "modifiers", currentModifiers, "abilities", List.of()));
        when(milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc("c1")).thenReturn(List.of(
                new MilestoneEntity("m2", "c1", 2, 50, currentSnapshot, "{}", "[]"),
                new MilestoneEntity("m1", "c1", 1, 50, previousSnapshot, "{}", "[]")));

        var result = service.lastUpgrade("c1");
        var changes = (List<Map<String, Object>>) result.get("modifiers");

        assertThat(changes).hasSize(3);
        assertThat(changes).anyMatch(change -> change.get("name").equals("Armadura") && change.get("before").equals(1) && change.get("after").equals(3));
        assertThat(changes).anyMatch(change -> change.get("name").equals("Antiguo") && change.get("before").equals(4) && change.get("after") == null);
        assertThat(changes).anyMatch(change -> change.get("name").equals("Nuevo") && change.get("before") == null && change.get("after").equals(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    void currentUpgradeReportsManualModifierAddedAfterLatestClosedVersion() throws Exception {
        var mapper = new ObjectMapper();
        var previousSnapshot = mapper.writeValueAsString(Map.of(
                "attributes", attributes,
                "genetics", genetics,
                "minorAttributes", Map.of(),
                "modifiers", Map.of(),
                "abilities", List.of()));
        when(milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc("c1"))
                .thenReturn(List.of(new MilestoneEntity("m1", "c1", 1, 0, previousSnapshot, "{}", "[]")));

        character.setModifiers(List.of(new CharacterAttributeModifierEntity(
                "manual-1", "c1", "enganno", "Pruebitas", 45)));

        var result = service.currentUpgrade("c1");
        var changes = (List<Map<String, Object>>) result.get("modifiers");

        assertThat(changes).anyMatch(change -> change.get("key").equals("enganno")
                && change.get("name").equals("Pruebitas")
                && change.get("before") == null
                && change.get("after").equals(45));
        verify(modifiers, never()).findByCharacterId("c1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void currentUpgradeIncludesAbilityUnlockedByEmbeddedModifier() throws Exception {
        var mapper = new ObjectMapper();
        var previousSnapshot = mapper.writeValueAsString(Map.of(
                "attributes", attributes,
                "genetics", genetics,
                "minorAttributes", Map.of(),
                "modifiers", Map.of(),
                "abilities", List.of()));
        when(milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc("c1"))
                .thenReturn(List.of(new MilestoneEntity("m1", "c1", 1, 0, previousSnapshot, "{}", "[]")));
        when(officialCatalog.abilities()).thenReturn(List.of(
                new AbilityEntity("a1", "Concentración Agilidad", "", "", 10, "No", "[{\"Agi\":5}]"),
                new AbilityEntity("a2", "Concentración Físico", "", "", 10, "No", "[{\"Fis\":5}]")));

        var currentAttributes = new LinkedHashMap<>(attributes);
        currentAttributes.put("agilidad", 4);
        currentAttributes.put("fisico", 4);
        character.setAttributesJson(mapper.writeValueAsString(currentAttributes));
        character.setModifiers(List.of(
                new CharacterAttributeModifierEntity("manual-1", "c1", "agilidad", "dddd", 1),
                new CharacterAttributeModifierEntity("manual-2", "c1", "fisico", "físico", 1)));
        var result = service.currentUpgrade("c1");
        assertThat((Set<String>) result.get("abilities"))
                .contains("Concentración Agilidad", "Concentración Físico");
    }

    @Test
    void cancelChangesRestoresLatestClosedSnapshot() throws Exception {
        var mapper = new ObjectMapper();
        var savedAttributes = new LinkedHashMap<>(attributes); savedAttributes.put("fisico", 4);
        var snapshot = mapper.writeValueAsString(Map.of("name", "Astrid cerrada", "level", 2, "experience", 80,
                "attributes", savedAttributes, "genetics", genetics, "minorAttributes", Map.of(),
                "modifiers", Map.of("fisico", List.of(Map.of("name", "Armadura", "value", 3))),
                "evolutionPoints", 7, "geneticsPoints", 2, "visible", true));
        var closed = new MilestoneEntity("m1", "c1", 2, 80, snapshot, "{}", "[]");
        when(milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc("c1")).thenReturn(List.of(closed));
        character.setClosed(false); character.setName("Borrador"); character.setExperience(10);

        service.cancelChanges("c1");

        assertThat(character.isClosed()).isTrue();
        assertThat(character.getName()).isEqualTo("Astrid cerrada");
        assertThat(character.getLevel()).isEqualTo(2);
        assertThat(character.getExperience()).isEqualTo(80);
        assertThat(attributesFromCharacter()).containsEntry("fisico", 4);
        verify(modifiers).save(any(CharacterAttributeModifierEntity.class));
    }

    @Test
    void recoverCreatesNewClosedVersionFromHistoricalSnapshot() throws Exception {
        var mapper = new ObjectMapper();
        var snapshot = mapper.writeValueAsString(Map.of("name", "Histórica", "level", 1, "experience", 20,
                "attributes", attributes, "genetics", genetics, "minorAttributes", Map.of(), "abilities", List.of()));
        var target = new MilestoneEntity("old", "c1", 1, 20, snapshot, "{\"level\":1}", "[]");
        when(milestones.findById("old")).thenReturn(Optional.of(target));
        when(milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc("c1")).thenReturn(List.of(target));

        var result = service.recover("c1", "old");

        assertThat(character.isClosed()).isTrue();
        assertThat(character.getName()).isEqualTo("Histórica");
        assertThat(character.getLevel()).isEqualTo(1);
        assertThat(result).containsKey("milestone");
        verify(milestones).save(any(MilestoneEntity.class));
    }

    @Test
    void recoverUpdatesExistingModifierInsteadOfInsertingDuplicate() throws Exception {
        var existing = new CharacterAttributeModifierEntity("mod-1", "c1", "diplomacia", "wssd", 1);
        when(modifiers.findByCharacterId("c1")).thenReturn(List.of(existing));
        var snapshot = new ObjectMapper().writeValueAsString(Map.of("attributes", attributes, "genetics", genetics,
                "minorAttributes", Map.of(), "modifiers", Map.of("diplomacia", List.of(Map.of("name", "wssd", "value", 4))),
                "abilities", List.of()));
        var target = new MilestoneEntity("old", "c1", 1, 20, snapshot, "{}", "[]");
        when(milestones.findById("old")).thenReturn(Optional.of(target));
        when(milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc("c1")).thenReturn(List.of(target));

        service.recover("c1", "old");

        assertThat(existing.getValue()).isEqualTo(4);
        verify(modifiers).save(existing);
        verify(modifiers, never()).save(argThat(value -> value != existing));
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
