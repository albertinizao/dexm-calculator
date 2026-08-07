package com.dexm.personajes.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/** Evaluates one logical ability as OR of alternative requirement records. */
public final class AbilityRules {
    private AbilityRules() {}
    public static boolean eligible(List<JsonNode> alternatives, Map<String,Integer> values, Map<String,Integer> genetics) {
        return alternatives.stream().anyMatch(a -> eligibleAlternative(a,values,genetics));
    }
    private static boolean eligibleAlternative(JsonNode node, Map<String,Integer> values, Map<String,Integer> genetics) {
        var it=node.fields(); while(it.hasNext()) { var f=it.next(); if (Set.of("Nombre","Descripcion","Lanzamiento","Coste","Prueba","Unica").contains(f.getKey())) continue; if(!f.getValue().isNumber()) continue;
            String key=canonical(normalize(f.getKey())); int actual=values.getOrDefault(key,genetics.getOrDefault(key,0)); if(actual < f.getValue().asInt()) return false;
        } return true;
    }
    public static String normalize(String key) { return key.toLowerCase(Locale.ROOT).replace("ñ","n").replaceAll("[^a-z0-9]",""); }
    private static String canonical(String key){return switch(key){case "fis"->"fisico";case "agi"->"agilidad";case "pcn"->"percepcion";case "mnt"->"mente";case "est"->"estudio";case "car"->"carisma";case "evoluccioncurva","evolutivocurva"->"evolcurva";case "cruzarbifrost"->"cruzarbifrost";default->key;};}
}
