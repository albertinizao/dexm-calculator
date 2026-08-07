package com.dexm.personajes.domain;

import java.util.*;

/** Numeric effects for persistent, self-only abilities. */
public final class AutomaticAbilityRules {
    public record Effect(String key, int value) {}
    private static final Set<String> FIXED = Set.of(
            "Ascendencia Jotun", "Auténtico Alfar", "Auténtico Dvergr", "Auténtico Héroe",
            "Auténtico Norna", "Auténtico Risa", "Auténtico Valkiria", "Anillo de los nibelungos",
            "Dökkálfar", "Jotun perfecto", "Aguante Dvegr", "Facilidad de liderazgo",
            "Musculatura perfecta", "Porte seguro", "Puntería heroica", "Utilidad de la diplomacia",
            "Utilidad de la labia", "Utilidad del engaño", "Esquiva Einhejer", "Esquiva del vidente",
            "Fortaleza Valkiria", "Fortaleza Risa");

    private AutomaticAbilityRules() {}

    public static boolean supported(String name) { return FIXED.contains(name) || name.matches("Fortaleza Dvergr [1-9]|Fortaleza Dvergr 10"); }
    public static boolean grantsExtraGenetics(Set<String> abilities) { return abilities.contains("Evolución del norte"); }
    public static boolean grantsExtraEvolution(Set<String> abilities) { return abilities.contains("Evolución del norte mejorada"); }
    public static boolean reducesForceEvolutionCost(Set<String> abilities) { return abilities.contains("Batido de proteinas"); }

    public static List<Effect> effects(String name, Map<String,Integer> attributes, Map<String,Integer> genetics, int dvergrFortresses) {
        var out = new ArrayList<Effect>();
        switch (name) {
            case "Ascendencia Jotun" -> add(out, "fisico", 2, "agilidad", -1, "fuerza", 2, "resistencia", 2, "destreza", -2, "esquiva", -1, "vida", 25, "bifrost", 15);
            case "Auténtico Alfar" -> add(out, "agilidad", 5);
            case "Auténtico Dvergr" -> add(out, "fisico", 5);
            case "Auténtico Héroe" -> add(out, "percepcion", 5);
            case "Auténtico Norna" -> add(out, "carisma", 5);
            case "Auténtico Risa" -> add(out, "mente", 5);
            case "Auténtico Valkiria" -> add(out, "estudio", 5);
            case "Anillo de los nibelungos" -> add(out, "atractivo", 10, "cruzarbifrost", 2, "destreza", 3, "diplomacia", 5, "einherjer", 3, "engano", 2, "labia", 5, "sentiryggdrasil", 5);
            case "Dökkálfar" -> add(out, "bifrost", 50, "agilidad", 5, "fuerza", 2, "mente", 2, "percepcion", 2, "destreza", 5, "deporte", 5, "cruzarbifrost", 5, "einherjer", 10, "intimidar", 5);
            case "Jotun perfecto" -> add(out, "fuerza", 3, "resistencia", 3, "cruzarbifrost", 3, "deporte", 3, "fisico", 2, "mente", 2, "atractivo", -5, "esquiva", -3, "destreza", -3, "agilidad", -2);
            case "Aguante Dvegr" -> add(out, "vida", 40);
            case "Facilidad de liderazgo" -> add(out, "liderazgo", 5);
            case "Musculatura perfecta" -> add(out, "atractivo", attributes.getOrDefault("deporte", 0) / 3);
            case "Porte seguro" -> add(out, "atractivo", 5);
            case "Puntería heroica" -> add(out, "punteria", 3);
            case "Utilidad de la diplomacia" -> add(out, "engano", 4, "labia", 4);
            case "Utilidad de la labia" -> add(out, "engano", 4, "diplomacia", 4);
            case "Utilidad del engaño" -> add(out, "labia", 4, "diplomacia", 4);
            case "Esquiva Einhejer" -> add(out, "esquiva", attributes.getOrDefault("einherjer", 0) / 5);
            case "Esquiva del vidente" -> add(out, "esquiva", genetics.getOrDefault("norna", 0) / 10);
            case "Fortaleza Valkiria" -> add(out, "vida", dvergrFortresses * 5);
            case "Fortaleza Risa" -> add(out, "bifrost", dvergrFortresses * 5);
            default -> { if (name.startsWith("Fortaleza Dvergr ")) add(out, "vida", 15); }
        }
        return out;
    }

    private static void add(List<Effect> out, Object... values) { for (int i = 0; i < values.length; i += 2) out.add(new Effect((String) values[i], (Integer) values[i + 1])); }
}
