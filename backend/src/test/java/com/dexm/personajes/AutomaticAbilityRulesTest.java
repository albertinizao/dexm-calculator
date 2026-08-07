package com.dexm.personajes;

import com.dexm.personajes.domain.AutomaticAbilityRules;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutomaticAbilityRulesTest {
    @Test
    void dynamicAbilitiesUseCurrentSourceValues() {
        var effects = AutomaticAbilityRules.effects("Esquiva Einhejer", Map.of("einherjer", 14), Map.of(), 0);
        assertEquals(2, effects.getFirst().value());
    }

    @Test
    void fortressEffectsScaleWithNumberOfDvergrAbilities() {
        var effects = AutomaticAbilityRules.effects("Fortaleza Valkiria", Map.of(), Map.of(), 4);
        assertEquals(20, effects.getFirst().value());
    }

    @Test
    void unsupportedTemporaryOrExternalAbilitiesAreIgnored() {
        assertEquals(false, AutomaticAbilityRules.supported("Agilidad del Valhalla"));
        assertEquals(false, AutomaticAbilityRules.supported("Prepararse para el Ragnarok"));
    }
}
