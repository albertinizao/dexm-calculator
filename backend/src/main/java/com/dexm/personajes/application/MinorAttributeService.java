package com.dexm.personajes.application;

import com.dexm.personajes.adapter.out.persistence.*;
import com.dexm.personajes.domain.CharacterRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MinorAttributeService {
    private final MinorAttributeDefinitionRepository defs;
    private final CharacterMinorAttributeValueRepository vals;
    private final CharacterRepository chars;
    private final CharacterAttributeModifierRepository modifiers;

    public MinorAttributeService(MinorAttributeDefinitionRepository defs, CharacterMinorAttributeValueRepository vals,
                                 CharacterRepository chars, CharacterAttributeModifierRepository modifiers) {
        this.defs = defs;
        this.vals = vals;
        this.chars = chars;
        this.modifiers = modifiers;
    }

    public List<MinorAttributeDefinitionEntity> list(String campaign) { return defs.findByCampaignIdOrderByNameAsc(campaign).stream().filter(d -> d.getOwnerCharacterId() == null).toList(); }

    @Transactional
    public MinorAttributeDefinitionEntity create(String campaign, String key, String name, String formula, String source, String type) {
        String k = key == null || key.isBlank() ? slug(name) : key;
        if (defs.findByCampaignIdAndKey(campaign, k).isPresent()) throw new IllegalArgumentException("El atributo ya existe");
        validateFormulaForCampaign(formula, campaign);
        Set<String> deps = new HashSet<>();
        for (String token : formula.split("[^A-Za-z0-9_]+")) {
            if (!token.isBlank() && !token.matches("\\d+") && !allowedVariables().containsKey(token)
                    && defs.findByCampaignIdAndKey(campaign, token).isPresent() == false
                    && !token.equals("min") && !token.equals("max")) {
                throw new IllegalArgumentException("Variable no permitida: " + token);
            }
            if (defs.findByCampaignIdAndKey(campaign, token).isPresent()) deps.add(token);
        }
        if (deps.contains(k)) throw new IllegalArgumentException("Referencia circular");
        for (String dep : deps) if (dependsOn(campaign, dep, k, new HashSet<>())) throw new IllegalArgumentException("Referencia circular");
        if (source != null && !source.isBlank() && defs.findByCampaignIdAndKey(campaign, source).isEmpty()
                && !CharacterRules.ATTRIBUTES.contains(source)) throw new IllegalArgumentException("Fuente de bonos inválida");
        var entity = defs.save(new MinorAttributeDefinitionEntity(UUID.randomUUID().toString(), campaign, k, name, formula, source,
                type == null ? "CUSTOM" : type));
        for (var character : chars.findByCampaignIdOrderByNameAsc(campaign)) {
            vals.save(new CharacterMinorAttributeValueEntity(UUID.randomUUID().toString(), character.getId(), entity.getId(), 0));
        }
        return entity;
    }

    public Map<String, Integer> values(String characterId) {
        var out = new LinkedHashMap<String, Integer>();
        vals.findByCharacterId(characterId).forEach(v -> defs.findById(v.getDefinitionId()).ifPresent(d -> out.put(d.getKey(), v.getValue())));
        return out;
    }

    public List<Map<String, Object>> view(String characterId) {
        var character = chars.findById(characterId).orElseThrow();
        var attrs = parse(character.getAttributesJson());
        var genetics = parse(character.getGeneticsJson());
        var values = values(characterId);
        var modifierTotals = modifiers.findByCharacterId(characterId).stream().collect(java.util.stream.Collectors.groupingBy(
                CharacterAttributeModifierEntity::getAttributeKey, LinkedHashMap::new,
                java.util.stream.Collectors.summingInt(CharacterAttributeModifierEntity::getScore)));
        var out = new ArrayList<Map<String, Object>>();
        defs.findByCampaignIdOrderByNameAsc(character.getCampaignId()).stream()
                .filter(definition -> definition.getOwnerCharacterId() == null || characterId.equals(definition.getOwnerCharacterId()))
                .forEach(definition -> {
            int ranks = vals.findByCharacterIdAndDefinitionId(characterId, definition.getId())
                    .map(CharacterMinorAttributeValueEntity::getValue).orElse(0);
            var modifierRows = modifiers.findByCharacterIdAndAttributeKey(characterId, definition.getKey());
            int total = ranks + modifierRows.stream().mapToInt(CharacterAttributeModifierEntity::getValue).sum();
            int max = evaluate(definition.getMaxFormula(), attrs, genetics, values, character.getLevel());
            String source = definition.getBonusSource();
            int plus = 0, d6 = 0;
            if ("GALDR".equals(definition.getType())) {
                d6 = total / 3;
                plus = total / 5;
            } else {
                String sourceKey = source == null ? definition.getKey() : source;
                int sourceValue = attrs.getOrDefault(sourceKey,
                        genetics.getOrDefault(sourceKey, values.getOrDefault(sourceKey, total)))
                        + modifierTotals.getOrDefault(sourceKey, 0);
                var bonus = CharacterRules.project(0, Map.of(sourceKey, sourceValue), Map.of()).bonuses().get(sourceKey);
                if (bonus != null) { plus = bonus.plusOne(); d6 = bonus.plusD6(); }
            }
            var modifierView = modifierRows.stream().map(m -> Map.of("name", m.getName(), "value", m.getValue())).toList();
            var row = new LinkedHashMap<String, Object>();
            row.put("id", definition.getId()); row.put("key", definition.getKey()); row.put("name", definition.getName());
            row.put("value", ranks); row.put("ranks", ranks); row.put("total", total); row.put("max", max);
            row.put("maxFormula", definition.getMaxFormula());
            row.put("bonusSource", source == null ? definition.getKey() : source); row.put("plusOne", plus); row.put("plusD6", d6);
            row.put("type", definition.getType()); row.put("modifiers", modifierView); out.add(row);
        });
        return out;
    }

    @Transactional
    public void deleteCustom(String characterId, String definitionId) {
        var character = chars.findById(characterId).orElseThrow(() -> new NoSuchElementException("Character not found"));
        var definition = defs.findById(definitionId).orElseThrow(() -> new NoSuchElementException("Minor attribute not found"));
        if (!Objects.equals(character.getCampaignId(), definition.getCampaignId())) throw new NoSuchElementException("Minor attribute not found");
        if (!"CUSTOM".equals(definition.getType())) throw new IllegalArgumentException("Solo se pueden eliminar atributos personalizados");
        var value = vals.findByCharacterIdAndDefinitionId(characterId, definitionId)
                .orElseThrow(() -> new NoSuchElementException("Minor attribute not found"));
        vals.delete(value);
        if (vals.findByDefinitionId(definitionId).isEmpty()) defs.delete(definition);
    }

    private boolean dependsOn(String campaign, String current, String target, Set<String> seen) {
        if (!seen.add(current)) return false;
        var definition = defs.findByCampaignIdAndKey(campaign, current).orElse(null);
        if (definition == null) return false;
        for (String token : definition.getMaxFormula().split("[^A-Za-z0-9_]+")) {
            if (token.equals(target)) return true;
            if (defs.findByCampaignIdAndKey(campaign, token).isPresent() && dependsOn(campaign, token, target, seen)) return true;
        }
        return false;
    }

    private static String slug(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", ""); }

    private static Map<String, Integer> parse(String value) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().readValue(value, Map.class); }
        catch (Exception e) { return new HashMap<>(); }
    }

    private void validateFormulaForCampaign(String formula, String campaign) {
        if (formula == null || formula.isBlank() || !formula.matches("[A-Za-z0-9_+*/()., -]+")) throw new IllegalArgumentException("Formula invalida");
        Map<String, Integer> context = allowedVariables();
        defs.findByCampaignIdOrderByNameAsc(campaign).forEach(d -> context.put(d.getKey(), 0));
        new Parser(formula, context).parse();
    }

    public static void validateFormula(String formula) {
        if (formula == null || formula.isBlank() || !formula.matches("[A-Za-z0-9_+*/()., -]+")) throw new IllegalArgumentException("Fórmula inválida");
        new Parser(formula, allowedVariables()).parse();
    }

    public static int evaluate(String formula, Map<String, Integer> attributes, Map<String, Integer> genetics,
                               Map<String, Integer> custom, int level) {
        Map<String, Integer> context = new HashMap<>(); context.putAll(attributes); context.putAll(genetics); context.putAll(custom); context.put("nivel", level);
        return Math.max(0, new Parser(formula, context).parse());
    }

    private static Map<String, Integer> allowedVariables() {
        Map<String, Integer> result = new HashMap<>();
        for (String key : CharacterRules.ATTRIBUTES) result.put(key, 0);
        for (String key : CharacterRules.GENETICS) result.put(key, 0);
        result.put("nivel", 0); return result;
    }

    private static final class Parser {
        private final String source; private final Map<String, Integer> context; private int position;
        private Parser(String source, Map<String, Integer> context) { this.source = source; this.context = context; }
        private int parse() { int value = expression(); skip(); if (position != source.length()) throw new IllegalArgumentException("Fórmula inválida"); return value; }
        private int expression() { int value = term(); while (true) { skip(); if (match('+')) value += term(); else if (match('-')) value -= term(); else return value; } }
        private int term() { int value = factor(); while (true) { skip(); if (match('*')) value *= factor(); else if (match('/')) { int divisor = factor(); if (divisor == 0) throw new IllegalArgumentException("División por cero"); value = Math.floorDiv(value, divisor); } else return value; } }
        private int factor() { skip(); if (match('(')) { int value = expression(); if (!match(')')) throw new IllegalArgumentException("Paréntesis"); return value; } if (position < source.length() && Character.isDigit(source.charAt(position))) { int number = 0; while (position < source.length() && Character.isDigit(source.charAt(position))) number = number * 10 + (source.charAt(position++) - '0'); return number; } int start = position; while (position < source.length() && (Character.isLetterOrDigit(source.charAt(position)) || source.charAt(position) == '_')) position++; if (start == position) throw new IllegalArgumentException("Token inválido"); String name = source.substring(start, position); skip(); if (match('(')) { int first = expression(); if (match(',')) { int second = expression(); int result = "min".equals(name) ? Math.min(first, second) : "max".equals(name) ? Math.max(first, second) : unsupported(); while (match(',')) { int next = expression(); result = "min".equals(name) ? Math.min(result, next) : Math.max(result, next); } if (!match(')')) throw new IllegalArgumentException("Paréntesis"); return result; } if (!match(')')) throw new IllegalArgumentException("Paréntesis"); return unsupported(); } if (!context.containsKey(name)) throw new IllegalArgumentException("Variable no permitida: " + name); return context.get(name); }
        private boolean match(char expected) { skip(); if (position < source.length() && source.charAt(position) == expected) { position++; return true; } return false; }
        private void skip() { while (position < source.length() && Character.isWhitespace(source.charAt(position))) position++; }
        private int unsupported() { throw new IllegalArgumentException("Función no permitida"); }
    }
}
