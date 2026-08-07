package com.dexm.personajes;
import com.dexm.personajes.domain.CharacterRules; import org.junit.jupiter.api.Test; import static org.assertj.core.api.Assertions.assertThat; import java.util.*;
class CharacterRulesTest {
 @Test void convertsExperienceIntoLevelAndRemainder(){var p=CharacterRules.project(250,Map.of("fisico",0),Map.of());assertThat(p.level()).isEqualTo(3);assertThat(p.experienceRemainder()).isEqualTo(50);assertThat(p.evolutionAvailable()).isEqualTo(35);}
 @Test void supportsPersistedLevelWithRemainingExperience(){var p=CharacterRules.projectAtLevel(4,25,Map.of("fisico",0),Map.of(),Map.of());assertThat(p.level()).isEqualTo(4);assertThat(p.experienceRemainder()).isEqualTo(25);assertThat(p.evolutionAvailable()).isEqualTo(35);}
 @Test void chargesMajorTenAndMinorFivePoints(){var b=CharacterRules.allocationBudget(100,9,Map.of(),Map.of(),Map.of(),Map.of("fisico",4,"fuerza",2),Map.of("heroe",2),Map.of("custom",3));assertThat(b.evolutionSpent()).isEqualTo(65);assertThat(b.evolutionRemaining()).isEqualTo(35);assertThat(b.geneticsSpent()).isEqualTo(2);assertThat(b.geneticsRemaining()).isEqualTo(7);}
 @Test void rewardIsIndependentOfCharacterLevel(){var first=CharacterRules.allocationBudget(35+8,3,Map.of(),Map.of(),Map.of());var later=CharacterRules.allocationBudget(35+8,3,Map.of(),Map.of(),Map.of());assertThat(first.evolutionAvailable()).isEqualTo(later.evolutionAvailable());}
 @Test void levelUpProjectionDoesNotRewriteExperienceRemainder(){var p=CharacterRules.projectAtLevel(2,150,Map.of(),Map.of(),Map.of());assertThat(p.level()).isEqualTo(2);assertThat(p.experienceRemainder()).isEqualTo(150);}
 @Test void calculatesBenefitsFromAttributeValue(){var p=CharacterRules.project(0,Map.of("fisico",25),Map.of());assertThat(p.bonuses().get("fisico").plusOne()).isEqualTo(4);assertThat(p.bonuses().get("fisico").plusD6()).isEqualTo(4);}
 @Test void usesAttributeSpecificBonusTables(){
  var p=CharacterRules.project(0,Map.of("atractivo",7,"conduccion",5,"destreza",56,"sentiryggdrasil",13),Map.of());
  assertThat(p.bonuses().get("atractivo")).isEqualTo(new CharacterRules.Bonus(2,2));
  assertThat(p.bonuses().get("conduccion")).isEqualTo(new CharacterRules.Bonus(1,3));
  // Destreza deliberately preserves the source table's duplicated 56 threshold.
  assertThat(p.bonuses().get("destreza")).isEqualTo(new CharacterRules.Bonus(11,9));
  assertThat(p.bonuses().get("sentiryggdrasil")).isEqualTo(new CharacterRules.Bonus(2,2));
 }
 @Test void calculatesDerivedStatsAndAppliesSignedFinalModifiers(){
  var stats=CharacterRules.derivedStats(
    Map.of("fisico",4,"mente",3,"esquiva",9,"destreza",9),
    Map.of("vida",-5,"bifrost",2,"defensaCuerpo",-1,"defensaDistancia",4));
  assertThat(stats.get("vida").baseValue()).isEqualTo(90);
  assertThat(stats.get("vida").total()).isEqualTo(85);
  assertThat(stats.get("bifrost").total()).isEqualTo(32);
  assertThat(stats.get("defensaCuerpo").baseValue()).isEqualTo(15);
  assertThat(stats.get("defensaCuerpo").total()).isEqualTo(14);
  assertThat(stats.get("defensaDistancia").baseValue()).isEqualTo(18);
  assertThat(stats.get("defensaDistancia").total()).isEqualTo(22);
 }
 @Test void attributeModifiersAffectDerivedBaseBeforeFinalModifier(){
  var stats=CharacterRules.derivedStats(Map.of("fisico",4,"mente",3), Map.of("fisico",2));
  assertThat(stats.get("vida").baseValue()).isEqualTo(100);
 }
 @Test void appliesReducedGrowthAfterFifteenOnlyForVida(){
  assertThat(CharacterRules.derivedStats(Map.of("fisico",15), Map.of()).get("vida").baseValue()).isEqualTo(145);
  assertThat(CharacterRules.derivedStats(Map.of("fisico",16), Map.of()).get("vida").baseValue()).isEqualTo(147);
  assertThat(CharacterRules.derivedStats(Map.of("fisico",17), Map.of()).get("vida").baseValue()).isEqualTo(150);
  assertThat(CharacterRules.derivedStats(Map.of("mente",15), Map.of()).get("bifrost").baseValue()).isEqualTo(150);
  assertThat(CharacterRules.derivedStats(Map.of("mente",16), Map.of()).get("bifrost").baseValue()).isEqualTo(160);
  assertThat(CharacterRules.derivedStats(Map.of("mente",17), Map.of()).get("bifrost").baseValue()).isEqualTo(170);
 }
}

