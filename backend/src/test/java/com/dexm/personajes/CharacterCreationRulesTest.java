package com.dexm.personajes;

import com.dexm.personajes.domain.CharacterCreationRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CharacterCreationRulesTest {
    @Test
    void guidedHumanAwakenedAddsFourToSelectedAndThreeToOthers() {
        var configuration = new CharacterCreationRules.Configuration("guided", "Humano de Midgard", true, true,
                List.of("fisico", "carisma"), "complete", "born_human", 10, 16, 18);

        var attributes = CharacterCreationRules.attributesFor(configuration);

        assertThat(attributes).containsEntry("fisico", 4).containsEntry("carisma", 4)
                .containsEntry("mente", 3).containsEntry("estudio", 3).containsEntry("agilidad", 3).containsEntry("percepcion", 3);
    }

    @Test
    void rejectsInvalidRaceWrongSelectionCountDuplicatesAndAwakenedWithoutEinherjer() {
        assertThatThrownBy(() -> validate("Androide", List.of("fisico", "mente"), true, true))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("race");
        assertThatThrownBy(() -> validate("Humano de Midgard", List.of("fisico"), false, false))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Exactly 2");
        assertThatThrownBy(() -> validate("Humano de Midgard", List.of("fisico", "fisico"), false, false))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicates");
        assertThatThrownBy(() -> validate("Humano de Midgard", List.of("vida", "mente"), false, false))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown");
        assertThatThrownBy(() -> validate("Humano de Midgard", List.of("fisico", "mente"), false, true))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Einherjer");

        assertThatThrownBy(() -> CharacterCreationRules.validate(new CharacterCreationRules.Configuration("guided", "Humano de Midgard", true, true,
                List.of("fisico", "mente"), "complete", "born_human", 10, null, 18)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("despertar");
    }

    private void validate(String race, List<String> selected, boolean einherjer, boolean awakened) {
        CharacterCreationRules.validate(new CharacterCreationRules.Configuration("guided", race, einherjer, awakened, selected, "complete", "born_human", 10, awakened ? 16 : null, 18));
    }
}
