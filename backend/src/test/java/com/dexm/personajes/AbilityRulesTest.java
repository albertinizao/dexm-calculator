package com.dexm.personajes;
import com.dexm.personajes.domain.AbilityRules; import com.fasterxml.jackson.databind.ObjectMapper; import org.junit.jupiter.api.Test; import static org.assertj.core.api.Assertions.assertThat; import java.util.*;
class AbilityRulesTest {
 @Test void acceptsAnyAlternativeRequirement(){try{var m=new ObjectMapper();var a=m.readTree("[{\"Fis\":10},{\"Agi\":10}]");assertThat(AbilityRules.eligible(List.of(a.get(0),a.get(1)),Map.of("agilidad",10),Map.of())).isTrue();}catch(Exception e){throw new AssertionError(e);}}
 @Test void requiresAllFieldsInsideAlternative(){try{var m=new ObjectMapper();var a=m.readTree("[{\"Fis\":10,\"Agi\":10}]");assertThat(AbilityRules.eligible(List.of(a.get(0)),Map.of("fisico",10,"agilidad",9),Map.of())).isFalse();}catch(Exception e){throw new AssertionError(e);}}
}

