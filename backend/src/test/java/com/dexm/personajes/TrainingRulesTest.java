package com.dexm.personajes;

import com.dexm.personajes.domain.TrainingRules;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingRulesTest {
    @Test void formationUsesCumulativeTable(){ assertThat(TrainingRules.bonus("FORMATION",6).primary()).isEqualByComparingTo("5"); }
    @Test void convertedEinherjerLearnsTwiceAsFastAfterAwakening(){
        var a=new TrainingRules.Activity("FORMATION",18,24,0,"medicina",null,null,false);
        assertThat(TrainingRules.humanEquivalent(a,true,"converted",20)).isEqualTo(10d);
    }
    @Test void concurrentOccupationDividesEquivalentTimeByOnePointFive(){
        var a=new TrainingRules.Activity("OCCUPATION",20,26,0,"deporte",null,null,true);
        assertThat(TrainingRules.humanEquivalent(a,false,null,null)).isEqualTo(4d);
    }
    @Test void currentAgeSentinelIncludesTheCurrentYear(){
        var a=new TrainingRules.Activity("FORMATION",20,24,0,"medicina",null,null,false);
        assertThat(TrainingRules.humanEquivalent(a,false,null,null)).isEqualTo(4d);
    }
    @Test void courseSlotsIncludePartialPeriods(){ assertThat(TrainingRules.courseSlots(10,21)).isEqualTo(3); }
    @Test void courseSlotsUseTheCharacterStartingAge(){ assertThat(TrainingRules.courseSlots(13,23)).isEqualTo(3); }
    @Test void coincidenceHalvesEachPreviousSelection(){ assertThat(TrainingRules.coincidence(BigDecimal.valueOf(4),2)).isEqualByComparingTo("1.00000000"); }
    @Test void roundsCoincidenceOnlyAfterAddingAllActivityValues(){
        assertThat(TrainingRules.roundTotal(java.util.List.of(new BigDecimal("0.75"), new BigDecimal("0.75")))).isEqualTo(2);
    }
    @Test void invalidHumanProfileCannotHaveAwakening(){ assertThatThrownBy(() -> TrainingRules.validateProfile(0,10,20,false,null)).isInstanceOf(IllegalArgumentException.class); }
}
