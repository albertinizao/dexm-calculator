package com.dexm.personajes.domain;

import java.util.*;

/** Pure rules ported from the legacy calculator. */
public final class CharacterRules {
    public static final int EVOLUTION_POINTS_PER_LEVEL = 35;
    public static final int EVOLUTION_POINTS_PER_MAJOR_RANK = 10;
    public static final int EVOLUTION_POINTS_PER_MINOR_RANK = 5;
    public static final int GENETICS_POINTS_PER_LEVEL = 3;
    public static final List<String> ATTRIBUTES = List.of("fisico","agilidad","percepcion","mente","estudio","carisma","astronavegar","atractivo","buscar","conduccion","cruzarbifrost","deporte","destreza","diplomacia","einherjer","engano","esconderse","evolcurva","esquiva","fisicaquimica","fuerza","informatica","intimidar","labia","liderazgo","medicina","provocar","punteria","resistencia","sentiryggdrasil");
    public static final List<String> GENETICS = List.of("heroe","norna","alfar","valkiria","risa","dvergr");
    private CharacterRules() {}

    public static Map<String,Integer> zeroValues(Collection<String> names) { var result=new LinkedHashMap<String,Integer>(); names.forEach(n->result.put(n,0)); return result; }

    public static Projection project(int experience, Map<String,Integer> attributes, Map<String,Integer> genetics) {
        return project(experience, attributes, genetics, Map.of());
    }

    /**
     * Projects the legacy rank maps while applying persisted modifiers only to
     * derived bonuses. Rank budgets remain based on the original rank fields.
     */
    public static Projection project(int experience, Map<String,Integer> attributes, Map<String,Integer> genetics,
                                     Map<String,Integer> modifierTotals) {
        int safeXp=Math.max(0,experience), level=1+safeXp/100, remainder=safeXp%100;
        return projectAtLevel(level, safeXp % 100, attributes, genetics, modifierTotals);
    }

    public static Projection projectAtLevel(int level, int experience, Map<String,Integer> attributes, Map<String,Integer> genetics,
                                            Map<String,Integer> modifierTotals) {
        int remainder=Math.max(0,experience), safeLevel=Math.max(1,level);
        int evolutionBudget=EVOLUTION_POINTS_PER_LEVEL + Math.max(0, attributes.getOrDefault("evolcurva", 0)), evolutionSpent=majorSpent(attributes) * EVOLUTION_POINTS_PER_MAJOR_RANK + minorSpent(attributes) * EVOLUTION_POINTS_PER_MINOR_RANK;
        int geneticsBudget=GENETICS_POINTS_PER_LEVEL, geneticsSpent=spent(genetics);
        Map<String,Bonus> bonuses=new LinkedHashMap<>();
        for (var e:attributes.entrySet()) {
            int value=Math.max(0,e.getValue()+modifierTotals.getOrDefault(e.getKey(),0));
            bonuses.put(e.getKey(),new Bonus(count(value,bonoThreshold(e.getKey())),count(value,d6Threshold(e.getKey()))));
        }
        return new Projection(safeLevel,remainder,evolutionBudget,evolutionSpent,geneticsBudget,geneticsSpent,bonuses);
    }

    public static int spent(Map<String, Integer> values) {
        return values.values().stream().mapToInt(value -> Math.max(0, value == null ? 0 : value)).sum();
    }

    public static AllocationBudget allocationBudget(int evolutionAvailable, int geneticsAvailable,
                                                     Map<String, Integer> currentAttributes,
                                                     Map<String, Integer> currentGenetics,
                                                     Map<String, Integer> currentCustomMinorAttributes,
                                                     Map<String, Integer> requestedAttributes,
                                                     Map<String, Integer> requestedGenetics,
                                                     Map<String, Integer> requestedCustomMinorAttributes) {
        int evolutionSpent = deltaMajor(currentAttributes, requestedAttributes) * EVOLUTION_POINTS_PER_MAJOR_RANK
                + deltaMinor(currentAttributes, requestedAttributes) * EVOLUTION_POINTS_PER_MINOR_RANK
                + delta(currentCustomMinorAttributes, requestedCustomMinorAttributes) * EVOLUTION_POINTS_PER_MINOR_RANK;
        int geneticsSpent = delta(currentGenetics, requestedGenetics);
        return new AllocationBudget(Math.max(0, evolutionAvailable), evolutionSpent,
                evolutionAvailable - evolutionSpent, Math.max(0, geneticsAvailable), geneticsSpent,
                geneticsAvailable - geneticsSpent);
    }

    public static AllocationBudget allocationBudget(int evolutionAvailable, int geneticsAvailable,
                                                     Map<String, Integer> attributes,
                                                     Map<String, Integer> genetics,
                                                     Map<String, Integer> customMinorAttributes) {
        return allocationBudget(evolutionAvailable, geneticsAvailable, attributes, genetics, customMinorAttributes,
                attributes, genetics, customMinorAttributes);
    }

    public static int delta(Map<String, Integer> current, Map<String, Integer> requested) {
        return requested.keySet().stream().mapToInt(key -> Math.max(0, requested.getOrDefault(key, 0) - current.getOrDefault(key, 0))).sum();
    }

    public static int geneticDelta(Map<String, Integer> current, Map<String, Integer> requested) {
        return delta(current, requested);
    }

    public static int deltaMajor(Map<String, Integer> current, Map<String, Integer> requested) {
        return requested.keySet().stream().filter(key -> Set.of("fisico", "agilidad", "percepcion", "mente", "estudio", "carisma").contains(key))
                .mapToInt(key -> Math.max(0, requested.getOrDefault(key, 0) - current.getOrDefault(key, 0))).sum();
    }

    public static int deltaMinor(Map<String, Integer> current, Map<String, Integer> requested) {
        return requested.keySet().stream().filter(key -> !Set.of("fisico", "agilidad", "percepcion", "mente", "estudio", "carisma").contains(key))
                .mapToInt(key -> Math.max(0, requested.getOrDefault(key, 0) - current.getOrDefault(key, 0))).sum();
    }

    public static int majorSpent(Map<String, Integer> attributes) {
        return attributes.entrySet().stream().filter(e -> Set.of("fisico", "agilidad", "percepcion", "mente", "estudio", "carisma").contains(e.getKey()))
                .mapToInt(e -> Math.max(0, e.getValue() == null ? 0 : e.getValue())).sum();
    }

    public static int minorSpent(Map<String, Integer> attributes) {
        return attributes.entrySet().stream().filter(e -> !Set.of("fisico", "agilidad", "percepcion", "mente", "estudio", "carisma").contains(e.getKey()))
                .mapToInt(e -> Math.max(0, e.getValue() == null ? 0 : e.getValue())).sum();
    }

    public record Bonus(int plusOne,int plusD6) {}
    public record AllocationBudget(int evolutionAvailable, int evolutionSpent, int evolutionRemaining,
                                   int geneticsAvailable, int geneticsSpent, int geneticsRemaining) {}
    private static int count(int value,int[] thresholds){int n=0;for(int t:thresholds)if(value>=t)n++;return n;}
    private static final int[] PRINCIPAL_PLUS_ONE = {5,11,17,23,29,35,41,47,53,59,65,71};
    private static final int[] PRINCIPAL_D6 = {2,8,14,20,26,32,38,44,50,56,62,68};

    /** Thresholds are kept in the literal order supplied by the game tables. */
    private static int[] bonoThreshold(String key){
        if (Set.of("fisico","agilidad","percepcion","mente","estudio","carisma").contains(key)) return PRINCIPAL_PLUS_ONE;
        return switch (key) {
            case "astronavegar", "conduccion" -> new int[]{3,12,19,29,37,46,53,61,73,80,93,110};
            case "atractivo", "diplomacia", "engano", "labia", "liderazgo" -> new int[]{3,5,10,15,20,25,30,35,40,45,50,55};
            case "buscar" -> new int[]{5,10,15,20,25,30,35,40,45,50,55,60};
            case "cruzarbifrost", "einherjer", "sentiryggdrasil" -> new int[]{6,13,21,30,40,51,63,76,90,105,121,138};
            case "deporte", "fuerza" -> new int[]{6,12,18,24,30,36,42,48,54,60,66,72};
            case "destreza" -> new int[]{3,8,13,18,23,28,33,38,43,48,53,58};
            case "esconderse", "punteria", "resistencia" -> new int[]{9,16,23,30,37,44,51,58,65,72,79,86};
            case "evolcurva" -> new int[]{5,15,25,35,45,55,65,75,85,95,105,115};
            case "esquiva" -> new int[]{2,5,8,11,15,19,23,28,33,38,44,50};
            case "fisicaquimica", "informatica", "medicina" -> new int[]{4,10,17,24,31,38,45,52,59,66,73,80};
            case "intimidar", "provocar" -> new int[]{7,13,19,25,31,37,43,49,55,61,67,73};
            default -> new int[]{3,5,10,15,20,25,30,35,40,45,50,55};
        };
    }
    private static int[] d6Threshold(String key){
        if (Set.of("fisico","agilidad","percepcion","mente","estudio","carisma").contains(key)) return PRINCIPAL_D6;
        return switch (key) {
            case "astronavegar", "conduccion" -> new int[]{1,5,5,10,17,25,33,40,50,57,65,78};
            case "atractivo", "buscar" -> new int[]{2,7,12,17,22,27,32,37,42,47,52,57};
            case "cruzarbifrost", "einherjer", "sentiryggdrasil" -> new int[]{3,8,14,21,29,38,48,59,71,83,97,110};
            case "deporte", "fuerza", "punteria", "resistencia" -> new int[]{2,9,15,21,27,33,39,45,51,57,63,69};
            case "destreza" -> new int[]{3,10,17,24,31,38,45,52,59,56,73,80};
            case "diplomacia", "engano", "intimidar", "labia", "liderazgo", "provocar" -> new int[]{2,8,14,20,26,32,38,44,50,56,62,68};
            case "esconderse" -> new int[]{3,10,19,26,33,40,47,54,61,68,75,82};
            case "evolcurva" -> new int[]{10,20,30,40,50,60,70,80,90,100,110,120};
            case "esquiva" -> new int[]{7,15,23,31,39,47,55,63,71,79,87,95};
            case "fisicaquimica", "informatica", "medicina" -> new int[]{2,6,13,20,27,34,41,48,55,62,69,76};
            default -> PRINCIPAL_D6;
        };
    }

    public static int[] plusOneThresholds(String key) {
        return bonoThreshold(key).clone();
    }

    public static int[] plusD6Thresholds(String key) {
        return d6Threshold(key).clone();
    }

    public record Projection(int level,int experienceRemainder,int evolutionAvailable,int evolutionSpent,int geneticsAvailable,int geneticsSpent,Map<String,Bonus> bonuses) {}
}
