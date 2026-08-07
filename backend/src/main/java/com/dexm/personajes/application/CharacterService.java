package com.dexm.personajes.application;

import com.dexm.personajes.adapter.in.web.AttributeDetailDto;
import com.dexm.personajes.adapter.in.web.CharacterController;
import com.dexm.personajes.adapter.out.persistence.*;
import com.dexm.personajes.domain.AbilityRules;
import com.dexm.personajes.domain.CharacterRules;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CharacterService {
    private static final String FLOW_SINGLE = "single";
    private static final String FLOW_SEQUENTIAL_ALL = "sequential-all";
    private static final Set<String> MAJOR_KEYS = Set.of("fisico", "agilidad", "percepcion", "mente", "estudio", "carisma");
    private static final Map<String, String> PREDEFINED_MINOR_FORMULAS = Map.ofEntries(
            Map.entry("astronavegar", "(agilidad+percepcion)/2"), Map.entry("atractivo", "carisma*2"),
            Map.entry("buscar", "percepcion"), Map.entry("conduccion", "(agilidad+percepcion)/2"),
            Map.entry("cruzarbifrost", "(mente+estudio)/2"), Map.entry("deporte", "fisico"),
            Map.entry("destreza", "(fisico+agilidad)/2"), Map.entry("diplomacia", "carisma"),
            Map.entry("einherjer", "(fisico+mente)/2"), Map.entry("engano", "(percepcion+mente)/2"),
            Map.entry("esconderse", "(agilidad+mente)/2"), Map.entry("esquiva", "(fisico+agilidad)/2"),
            Map.entry("fisicaquimica", "estudio"), Map.entry("fuerza", "fisico"),
            Map.entry("informatica", "estudio"), Map.entry("intimidar", "(fisico+carisma)/2"),
            Map.entry("labia", "carisma"), Map.entry("liderazgo", "carisma"), Map.entry("medicina", "estudio"),
            Map.entry("provocar", "(mente+max(fisico,carisma))/2"), Map.entry("punteria", "percepcion"),
            Map.entry("resistencia", "fisico"), Map.entry("sentiryggdrasil", "(percepcion+mente)/2"));

    private final CharacterRepository characters;
    private final MilestoneRepository milestones;
    private final AbilityRepository abilities;
    private final ObjectMapper json;
    private final MinorAttributeService minorAttributes;
    private final CharacterMinorAttributeValueRepository minorValues;
    private final MinorAttributeDefinitionRepository minorDefs;
    private final CharacterAttributeModifierRepository modifiers;

    public CharacterService(CharacterRepository characters, MilestoneRepository milestones, AbilityRepository abilities,
                             ObjectMapper json, MinorAttributeService minorAttributes,
                             CharacterMinorAttributeValueRepository minorValues,
                             MinorAttributeDefinitionRepository minorDefs,
                             CharacterAttributeModifierRepository modifiers) {
        this.characters = characters;
        this.milestones = milestones;
        this.abilities = abilities;
        this.json = json;
        this.minorAttributes = minorAttributes;
        this.minorValues = minorValues;
        this.minorDefs = minorDefs;
        this.modifiers = modifiers;
    }

    public List<CharacterEntity> list() { return characters.findAll(); }
    public List<CharacterEntity> listByCampaign(String campaignId) { return characters.findByCampaignIdOrderByNameAsc(campaignId); }

    @Transactional
    public void deleteByCampaign(String campaignId) {
        for (var character : listByCampaign(campaignId)) {
            modifiers.deleteByCharacterId(character.getId());
            minorValues.deleteAll(minorValues.findByCharacterId(character.getId()));
            milestones.deleteByCharacterId(character.getId());
            characters.delete(character);
        }
        minorDefs.deleteAll(minorDefs.findByCampaignIdOrderByNameAsc(campaignId));
    }

    @Transactional
    public CharacterEntity create(String name) {
        try {
            var attrs = CharacterRules.zeroValues(CharacterRules.ATTRIBUTES);
            var gen = CharacterRules.zeroValues(CharacterRules.GENETICS);
            var c = new CharacterEntity(UUID.randomUUID().toString(), name, 0, json.writeValueAsString(attrs), json.writeValueAsString(gen));
            c.setClosed(true);
            return characters.save(c);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public CharacterEntity create(String campaignId, String name, String imageUrl) {
        try {
            var attrs = CharacterRules.zeroValues(CharacterRules.ATTRIBUTES);
            var gen = CharacterRules.zeroValues(CharacterRules.GENETICS);
            var c = new CharacterEntity(UUID.randomUUID().toString(), campaignId, name, imageUrl, 0,
                    json.writeValueAsString(attrs), json.writeValueAsString(gen));
            c.setClosed(true);
            return characters.save(c);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public CharacterEntity get(String id) {
        return characters.findById(id).orElseThrow(() -> new NoSuchElementException("Character not found"));
    }

    @Transactional
    public Map<String, Object> beginEdit(String id) {
        var character = get(id);
        character.setClosed(false);
        character.touch();
        characters.save(character);
        return view(id);
    }

    public Map<String, Object> view(String id) {
        try {
            var c = get(id);
            var attrs = parse(c.getAttributesJson());
            var gen = parse(c.getGeneticsJson());
            var customMinorRanks = minorAttributes.values(id);
            var modifierTotals = modifierTotals(id);
            var totals = new LinkedHashMap<String, Integer>();
            attrs.forEach((key, value) -> totals.put(key, value + modifierTotals.getOrDefault(key, 0)));
            var minorView = minorAttributes.view(id);
            minorView.forEach(attribute -> totals.put(String.valueOf(attribute.get("key")), ((Number) attribute.get("total")).intValue()));

            var result = new LinkedHashMap<String, Object>();
            result.put("id", c.getId());
            result.put("campaignId", c.getCampaignId());
            result.put("name", c.getName());
            result.put("imageUrl", c.getImageUrl());
            result.put("experience", c.getExperience());
            result.put("level", c.getLevel());
            result.put("attributes", attrs);
            result.put("attributeTotals", totals);
            result.put("attributeModifiers", modifierView(id));
            result.put("genetics", gen);
            result.put("minorAttributes", minorView);
            result.put("abilities", latestSnapshotValues(id, "abilities"));
            result.put("allocation", CharacterRules.allocationBudget(c.getEvolutionPoints(), c.getGeneticsPoints(), attrs, gen, customMinorRanks));
            result.put("closed", c.isClosed());
            result.put("createdAt", c.getCreatedAt());
            result.put("updatedAt", c.getUpdatedAt());
            result.put("lastClosedAt", milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc(id).stream()
                    .findFirst().map(MilestoneEntity::getCreatedAt).orElse(c.getCreatedAt()));
            return result;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public Map<String, Object> save(String id, String name, int xp, Map<String, Integer> attrs,
                                    Map<String, Integer> gen, Map<String, Integer> minor) {
        return save(id, name, null, xp, attrs, gen, minor, false, false);
    }

    @Transactional
    public Map<String, Object> save(String id, String name, Integer level, int xp, Map<String, Integer> attrs,
                                    Map<String, Integer> gen, Map<String, Integer> minor, boolean visible,
                                    boolean finalStep) {
        return persistAllocation(id, name, level, xp, attrs, gen, minor, visible, finalStep, "save");
    }

    @Transactional
    public Map<String, Object> saveAttributeModifiers(String id,
                                                       Map<String, List<com.dexm.personajes.adapter.in.web.CharacterController.ModifierRequest>> requested) {
        var character = get(id);
        var allowed = new LinkedHashSet<String>(CharacterRules.ATTRIBUTES);
        allowed.addAll(PREDEFINED_MINOR_FORMULAS.keySet());
        allowed.addAll(minorDefs.findByCampaignIdOrderByNameAsc(character.getCampaignId()).stream()
                .map(MinorAttributeDefinitionEntity::getKey).toList());
        if (requested == null) requested = Map.of();
        for (var entry : requested.entrySet()) {
            if (!allowed.contains(entry.getKey())) throw new IllegalArgumentException("Unknown attribute: " + entry.getKey());
            var names = new HashSet<String>();
            for (var modifier : Optional.ofNullable(entry.getValue()).orElse(List.of())) {
                if (modifier == null || modifier.name() == null || modifier.name().isBlank()) {
                    throw new IllegalArgumentException("Modifier name is required");
                }
                if (!names.add(modifier.name().trim())) throw new IllegalArgumentException("Duplicate modifier name: " + modifier.name());
                if (modifier.value() == null) throw new IllegalArgumentException("Modifier value is required");
            }
        }
        var requestedByKey = new LinkedHashMap<String, CharacterController.ModifierRequest>();
        requested.forEach((attributeKey, values) -> Optional.ofNullable(values).orElse(List.of()).forEach(modifier -> {
            var name = modifier.name().trim();
            requestedByKey.put(modifierKey(attributeKey, name),
                    new CharacterController.ModifierRequest(name, modifier.value()));
        }));

        var existingByKey = modifiers.findByCharacterId(id).stream()
                .collect(Collectors.toMap(modifier -> modifierKey(modifier.getAttributeKey(), modifier.getName()),
                        modifier -> modifier, (first, ignored) -> first, LinkedHashMap::new));
        existingByKey.forEach((key, existing) -> {
            var requestedModifier = requestedByKey.get(key);
            if (requestedModifier == null) {
                modifiers.delete(existing);
            } else if (existing.getValue() != requestedModifier.value()) {
                existing.setValue(requestedModifier.value());
                modifiers.save(existing);
            }
        });
        requestedByKey.forEach((key, requestedModifier) -> {
            if (!existingByKey.containsKey(key)) {
                var attributeKey = key.substring(0, key.indexOf('\u0000'));
                modifiers.save(new CharacterAttributeModifierEntity(UUID.randomUUID().toString(), id, attributeKey,
                        requestedModifier.name(), requestedModifier.value()));
            }
        });
        var attrs = parse(character.getAttributesJson());
        var genetics = parse(character.getGeneticsJson());
        character.setClosed(false);
        character.touch();
        characters.save(character);
        return Map.of("character", view(id), "visible", false, "final", false);
    }

    @Transactional(readOnly = true)
    public AttributeDetailDto attributeDetail(String characterId, String attributeKey) {
        var character = get(characterId);
        var attrs = parse(character.getAttributesJson());
        var genetics = parse(character.getGeneticsJson());
        var modifierRows = modifiers.findByCharacterIdAndAttributeKey(characterId, attributeKey);
        var modifierDtos = modifierRows.stream().map(m -> new AttributeDetailDto.ModifierDto(m.getName(), m.getValue())).toList();
        var modifierTotal = modifierRows.stream().mapToInt(CharacterAttributeModifierEntity::getValue).sum();
        var ranks = attrs.getOrDefault(attributeKey, 0);
        var total = ranks + modifierTotal;
        var level = character.getLevel();

        if (MAJOR_KEYS.contains(attributeKey)) {
            Integer max = ranks >= 5 ? MAJOR_KEYS.stream().filter(k -> !k.equals(attributeKey)).mapToInt(k -> attrs.getOrDefault(k, 0)).max().orElse(0) * 2 : null;
            var formula = ranks >= 5 ? "2 × el rango mayor más alto de los otros atributos" : "No aplica por debajo de 5 rangos";
            return new AttributeDetailDto(attributeKey, null, label(attributeKey), "MAJOR", total, ranks, max, formula,
                    max == null ? 0 : max, bonus(total, attributeKey, true, false), bonus(total, attributeKey, false, false), modifierDtos,
                    progressions(attributeKey, total), false);
        }

        var definition = minorDefs.findByCampaignIdAndKey(character.getCampaignId(), attributeKey).orElse(null);
        if (definition == null && CharacterRules.ATTRIBUTES.contains(attributeKey)) {
            var formula = PREDEFINED_MINOR_FORMULAS.get(attributeKey);
            var calculated = formula == null ? 0 : MinorAttributeService.evaluate(formula, attrs, genetics, minorAttributes.values(characterId), level);
            total = calculated + modifierTotal;
            return new AttributeDetailDto(attributeKey, null, label(attributeKey), "PREDEFINED", total, ranks, formula == null ? null : calculated,
                    formula == null ? "Máximo especial" : formula, calculated, bonus(total, attributeKey, true, false), bonus(total, attributeKey, false, false),
                    modifierDtos, progressions(attributeKey, total), false);
        }
        if (definition == null) throw new NoSuchElementException("Attribute not found");
        if (!Objects.equals(definition.getCampaignId(), character.getCampaignId())) throw new NoSuchElementException("Attribute not found");

        ranks = minorValues.findByCharacterIdAndDefinitionId(characterId, definition.getId()).map(CharacterMinorAttributeValueEntity::getValue).orElse(0);
        total = ranks + modifierTotal;
        var calculated = MinorAttributeService.evaluate(definition.getMaxFormula(), attrs, genetics, minorAttributes.values(characterId), level);
        var source = definition.getBonusSource() == null ? attributeKey : definition.getBonusSource();
        var customBonus = "GALDR".equals(definition.getType())
                ? new CharacterRules.Bonus(total / 5, total / 3)
                : sourceBonus(characterId, source, attrs, genetics);
        return new AttributeDetailDto(attributeKey, definition.getId(), definition.getName(), definition.getType(), total, ranks,
                calculated, definition.getMaxFormula(), calculated, customBonus.plusOne(), customBonus.plusD6(), modifierDtos,
                progressions(attributeKey, total), "CUSTOM".equals(definition.getType()));
    }

    @Transactional
    public Map<String, Object> addExperience(String id, int amount) {
        if (amount < 1) throw new IllegalArgumentException("Experience amount must be positive");
        var c = get(id);
        c.setExperience(Math.addExact(c.getExperience(), amount));
        c.setClosed(false);
        c.touch();
        characters.save(c);
        return view(id);
    }

    @Transactional
    public Map<String, Object> levelUp(String id, int level, int xp, Map<String, Integer> attrs,
                                       Map<String, Integer> gen, Map<String, Integer> minor) {
        return levelUp(id, level, xp, attrs, gen, minor, true, true);
    }

    @Transactional
    public Map<String, Object> levelUp(String id, int level, int xp, Map<String, Integer> attrs,
                                       Map<String, Integer> gen, Map<String, Integer> minor,
                                       boolean visible, boolean finalStep) {
        return persistAllocation(id, null, level, xp, attrs, gen, minor, visible, finalStep, FLOW_SINGLE);
    }

    @Transactional
    public Map<String, Object> levelUpAll(String id, int level, int xp, Map<String, Integer> attrs,
                                          Map<String, Integer> gen, Map<String, Integer> minor,
                                          boolean visible, boolean finalStep) {
        return persistAllocation(id, null, level, xp, attrs, gen, minor, visible, finalStep, FLOW_SEQUENTIAL_ALL);
    }

    private Map<String, Object> persistAllocation(String id, String requestedName, Integer requestedLevel, int requestedXp,
                                                  Map<String, Integer> requestedAttributes, Map<String, Integer> requestedGenetics,
                                                  Map<String, Integer> requestedCustomMinors, boolean visible, boolean finalStep,
                                                  String flow) {
        try {
            var character = get(id);
            int targetLevel = requestedLevel == null ? character.getLevel() : requestedLevel;
            validateLevelAndExperience(character, targetLevel, requestedXp, flow, visible, finalStep);

            var attributes = normalizeRanks("attributes", requestedAttributes, parse(character.getAttributesJson()), CharacterRules.ATTRIBUTES, true);
            var genetics = normalizeRanks("genetics", requestedGenetics, parse(character.getGeneticsJson()), CharacterRules.GENETICS, true);
            var currentAttributes = parse(character.getAttributesJson());
            var currentGenetics = parse(character.getGeneticsJson());
            var currentCustomMinors = minorAttributes.values(character.getId());
            var customMinors = normalizeCustomMinorRanks(character, requestedCustomMinors);
            rejectRankReductions(currentAttributes, attributes, "attributes");
            rejectRankReductions(currentGenetics, genetics, "genetics");
            rejectRankReductions(currentCustomMinors, customMinors, "minor attributes");
            if (FLOW_SINGLE.equals(flow) || FLOW_SEQUENTIAL_ALL.equals(flow)) {
                rejectGeneticLevelOverflow(currentGenetics, genetics);
                if (CharacterRules.geneticDelta(currentGenetics, genetics) != CharacterRules.GENETICS_POINTS_PER_LEVEL) {
                    throw new IllegalArgumentException("Exactly 3 genetic points must be assigned per level");
                }
            }
            clampMinorRanks(character, targetLevel, attributes, genetics, customMinors);

            int evolutionReward = flow.equals(FLOW_SINGLE) || flow.equals(FLOW_SEQUENTIAL_ALL)
                    ? CharacterRules.EVOLUTION_POINTS_PER_LEVEL + parse(character.getAttributesJson()).getOrDefault("evolcurva", 0)
                    : 0;
            int geneticsReward = flow.equals(FLOW_SINGLE) || flow.equals(FLOW_SEQUENTIAL_ALL)
                    ? CharacterRules.GENETICS_POINTS_PER_LEVEL : 0;
            int evolutionAvailable = character.getEvolutionPoints() + evolutionReward;
            int geneticsAvailable = FLOW_SINGLE.equals(flow) || FLOW_SEQUENTIAL_ALL.equals(flow)
                    ? geneticsReward : character.getGeneticsPoints() + geneticsReward;
            var budget = CharacterRules.allocationBudget(evolutionAvailable, geneticsAvailable,
                    currentAttributes, currentGenetics, currentCustomMinors,
                    attributes, genetics, customMinors);
            if (budget.evolutionRemaining() < 0) throw new IllegalArgumentException("Evolution points budget exceeded");
            if (budget.geneticsRemaining() < 0) throw new IllegalArgumentException("Genetics points budget exceeded");

            String name = requestedName == null ? character.getName() : requestedName;
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Character name is required");
            var previous = milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc(id).stream().findFirst();
            var projection = CharacterRules.projectAtLevel(targetLevel, requestedXp, attributes, genetics, modifierTotals(id));

            character.setName(name);
            character.setExperience(requestedXp);
            character.setLevel(targetLevel);
            character.setAttributesJson(json.writeValueAsString(attributes));
            character.setGeneticsJson(json.writeValueAsString(genetics));
            character.setEvolutionPoints(budget.evolutionRemaining());
            character.setGeneticsPoints(FLOW_SINGLE.equals(flow) || FLOW_SEQUENTIAL_ALL.equals(flow)
                    ? 0 : budget.geneticsRemaining());
            character.setClosed(visible && finalStep);
            character.touch();
            characters.save(character);
            persistCustomMinorRanks(character, customMinors);

            var effectiveAttributes = withModifiers(attributes, modifierTotals(id));
            var effectiveCustomMinors = withModifiers(customMinors, modifierTotals(id));
            var awards = eligibleAbilities(effectiveAttributes, genetics, effectiveCustomMinors);
            var all = awards.obtained();
            var before = previous.map(this::snapshotAbilities).orElse(Set.of());
            var newly = new LinkedHashSet<>(all);
            newly.removeAll(before);
            var snapshot = new LinkedHashMap<String, Object>();
            snapshot.put("name", name);
            snapshot.put("experience", requestedXp);
            snapshot.put("level", targetLevel);
            snapshot.put("attributes", attributes);
            snapshot.put("genetics", genetics);
            snapshot.put("minorAttributes", customMinors);
            snapshot.put("abilities", all);
            snapshot.put("pendingUniqueAbilities", awards.pendingUnique());
            snapshot.put("visible", visible);
            var newBonuses = Map.of("level", projection.level(), "bonuses", projection.bonuses(), "allocation", budget);
            var milestone = new MilestoneEntity(UUID.randomUUID().toString(), id, targetLevel, requestedXp,
                    json.writeValueAsString(snapshot), json.writeValueAsString(newBonuses), json.writeValueAsString(newly), visible);
            milestones.save(milestone);

            var response = new LinkedHashMap<String, Object>();
            response.put("character", view(id));
            response.put("projection", projection);
            response.put("allocation", budget);
            response.put("milestone", milestone);
            response.put("flow", flow);
            response.put("visible", visible);
            response.put("final", finalStep);
            response.put("nextLevel", flow.equals(FLOW_SEQUENTIAL_ALL) && !finalStep ? targetLevel + 1 : null);
            response.put("remainingLevels", Math.max(0, requestedXp / 100));
            response.put("canContinue", flow.equals(FLOW_SEQUENTIAL_ALL) && !finalStep && requestedXp >= 100);
            response.put("nextAction", flow.equals(FLOW_SEQUENTIAL_ALL) && !finalStep ? "Siguiente nivel" : "Guardado");
            previous.ifPresent(p -> response.put("previousMilestone", p));
            return response;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void validateLevelAndExperience(CharacterEntity character, int targetLevel, int targetXp, String flow,
                                             boolean visible, boolean finalStep) {
        if (targetLevel < 1) throw new IllegalArgumentException("Level must be positive");
        if (targetXp < 0) throw new IllegalArgumentException("Experience cannot be negative");
        if (visible != finalStep) throw new IllegalArgumentException("Visible and final flags must agree");
        if (FLOW_SINGLE.equals(flow)) {
            if (visible != finalStep) throw new IllegalArgumentException("Visible and final flags must agree");
            if (targetLevel != character.getLevel() + 1 || character.getExperience() < 100
                    || targetXp != character.getExperience() - 100) {
                throw new IllegalStateException("Level-up must increase one level and spend 100 experience");
            }
            return;
        }
        if (FLOW_SEQUENTIAL_ALL.equals(flow)) {
            if (targetLevel != character.getLevel() + 1 || character.getExperience() < 100
                    || targetXp != character.getExperience() - 100) {
                throw new IllegalStateException("Sequential level-up must increase one level and spend 100 experience");
            }
            if (visible && finalStep != targetXp < 100) throw new IllegalStateException("Sequential final flag does not match remaining levels");
            return;
        }
        if (targetLevel != character.getLevel()) throw new IllegalStateException("Regular saves cannot change level");
    }

    private Map<String, Integer> normalizeRanks(String label, Map<String, Integer> requested, Map<String, Integer> current,
                                                 Collection<String> allowed, boolean preserveMissing) {
        var result = new LinkedHashMap<String, Integer>();
        allowed.forEach(key -> result.put(key, Math.max(0, current.getOrDefault(key, 0))));
        if (requested == null) return result;
        var allowedSet = new HashSet<>(allowed);
        requested.forEach((key, value) -> {
            if (!allowedSet.contains(key)) throw new IllegalArgumentException("Unknown " + label + " attribute: " + key);
            if (value == null || value < 0) throw new IllegalArgumentException("Ranks cannot be negative");
            result.put(key, value);
        });
        return result;
    }

    private Map<String, Integer> normalizeCustomMinorRanks(CharacterEntity character, Map<String, Integer> requested) {
        var result = new LinkedHashMap<>(minorAttributes.values(character.getId()));
        var definitions = minorDefs.findByCampaignIdOrderByNameAsc(character.getCampaignId());
        var allowed = definitions.stream().map(MinorAttributeDefinitionEntity::getKey).collect(Collectors.toSet());
        if (requested == null) return result;
        requested.forEach((key, value) -> {
            if (!allowed.contains(key)) throw new IllegalArgumentException("Unknown custom minor attribute: " + key);
            if (value == null || value < 0) throw new IllegalArgumentException("Ranks cannot be negative");
            result.put(key, value);
        });
        definitions.forEach(definition -> result.putIfAbsent(definition.getKey(), 0));
        return result;
    }

    private void rejectRankReductions(Map<String, Integer> current, Map<String, Integer> requested, String label) {
        current.forEach((key, value) -> {
            if (requested.getOrDefault(key, 0) < value) {
                throw new IllegalArgumentException("Existing " + label + " ranks cannot be reduced: " + key);
            }
        });
    }

    private void rejectGeneticLevelOverflow(Map<String, Integer> current, Map<String, Integer> requested) {
        current.forEach((key, value) -> {
            if (requested.getOrDefault(key, 0) - value > 2) {
                throw new IllegalArgumentException("A genetic attribute can increase by at most 2 ranks per level: " + key);
            }
        });
    }

    private void clampMinorRanks(CharacterEntity character, int level, Map<String, Integer> attributes,
                                 Map<String, Integer> genetics, Map<String, Integer> customMinors) {
        var definitions = minorDefs.findByCampaignIdOrderByNameAsc(character.getCampaignId());
        for (int pass = 0; pass <= definitions.size(); pass++) {
            boolean changed = false;
            for (var entry : PREDEFINED_MINOR_FORMULAS.entrySet()) {
                if (!attributes.containsKey(entry.getKey())) continue;
                int cap = MinorAttributeService.evaluate(entry.getValue(), attributes, genetics, customMinors, level);
                int clamped = Math.min(attributes.getOrDefault(entry.getKey(), 0), cap);
                if (clamped != attributes.getOrDefault(entry.getKey(), 0)) { attributes.put(entry.getKey(), clamped); changed = true; }
            }
            for (var definition : definitions) {
                int current = customMinors.getOrDefault(definition.getKey(), 0);
                int cap = MinorAttributeService.evaluate(definition.getMaxFormula(), attributes, genetics, customMinors, level);
                int clamped = Math.min(current, cap);
                if (clamped != current) { customMinors.put(definition.getKey(), clamped); changed = true; }
            }
            if (!changed) return;
        }
    }

    private void persistCustomMinorRanks(CharacterEntity character, Map<String, Integer> customMinors) {
        minorDefs.findByCampaignIdOrderByNameAsc(character.getCampaignId()).forEach(definition -> {
            var value = minorValues.findByCharacterIdAndDefinitionId(character.getId(), definition.getId())
                    .orElse(new CharacterMinorAttributeValueEntity(UUID.randomUUID().toString(), character.getId(), definition.getId(), 0));
            value.setValue(customMinors.getOrDefault(definition.getKey(), 0));
            minorValues.save(value);
        });
    }

    @Transactional
    public void deleteCustomMinorAttribute(String characterId, String definitionId) {
        minorAttributes.deleteCustom(characterId, definitionId);
    }

    private List<AttributeDetailDto.ProgressionDto> progressions(String key, int value) {
        var result = new ArrayList<AttributeDetailDto.ProgressionDto>();
        var one = CharacterRules.plusOneThresholds(key);
        for (int i = 0; i < one.length; i++) result.add(new AttributeDetailDto.ProgressionDto("+1", i + 1, one[i], value >= one[i]));
        var d6 = CharacterRules.plusD6Thresholds(key);
        for (int i = 0; i < d6.length; i++) result.add(new AttributeDetailDto.ProgressionDto("+D6", i + 1, d6[i], value >= d6[i]));
        return result;
    }

    private int bonus(int value, String key, boolean plusOne, boolean ignored) {
        var thresholds = plusOne ? CharacterRules.plusOneThresholds(key) : CharacterRules.plusD6Thresholds(key);
        int count = 0;
        for (var threshold : thresholds) if (value >= threshold) count++;
        return count;
    }

    private CharacterRules.Bonus sourceBonus(String characterId, String source, Map<String, Integer> attrs, Map<String, Integer> genetics) {
        var context = effectiveContext(characterId, attrs, genetics);
        var value = context.attributes().getOrDefault(source,
                context.genetics().getOrDefault(source, context.minors().getOrDefault(source, 0)));
        return CharacterRules.project(0, Map.of(source, value), Map.of()).bonuses().getOrDefault(source, new CharacterRules.Bonus(0, 0));
    }

    private EffectiveContext effectiveContext(String characterId, Map<String, Integer> attrs, Map<String, Integer> genetics) {
        var minors = minorAttributes.values(characterId);
        var totals = modifierTotals(characterId);
        attrs = new LinkedHashMap<>(attrs);
        attrs.replaceAll((key, value) -> value + totals.getOrDefault(key, 0));
        minors.replaceAll((key, value) -> value + totals.getOrDefault(key, 0));
        return new EffectiveContext(attrs, genetics, minors);
    }

    private record EffectiveContext(Map<String, Integer> attributes, Map<String, Integer> genetics, Map<String, Integer> minors) {}

    private Map<String, Integer> modifierTotals(String characterId) {
        return modifiers.findByCharacterId(characterId).stream().collect(Collectors.groupingBy(
                CharacterAttributeModifierEntity::getAttributeKey, LinkedHashMap::new,
                Collectors.summingInt(CharacterAttributeModifierEntity::getValue)));
    }

    private static String modifierKey(String attributeKey, String name) {
        return attributeKey + '\u0000' + name;
    }

    private Map<String, List<Map<String, Object>>> modifierView(String characterId) {
        var result = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (var modifier : modifiers.findByCharacterId(characterId)) {
            result.computeIfAbsent(modifier.getAttributeKey(), ignored -> new ArrayList<>())
                    .add(Map.of("name", modifier.getName(), "value", modifier.getValue()));
        }
        return result;
    }

    private String label(String key) {
        return switch (key) {
            case "fisico" -> "Físico"; case "agilidad" -> "Agilidad"; case "percepcion" -> "Percepción";
            case "mente" -> "Mente"; case "estudio" -> "Estudio"; case "carisma" -> "Carisma";
            default -> key;
        };
    }

    private AbilityAwards eligibleAbilities(Map<String, Integer> attrs, Map<String, Integer> gen,
                                             Map<String, Integer> customMinors) {
        Set<String> obtained = new LinkedHashSet<>();
        Set<String> pendingUnique = new LinkedHashSet<>();
        var values = new LinkedHashMap<String, Integer>(attrs);
        values.putAll(customMinors);
        abilities.findAll().forEach(a -> {
            try {
                var alternatives = new ArrayList<JsonNode>();
                json.readTree(a.getAlternativesJson()).forEach(alternatives::add);
                if (AbilityRules.eligible(alternatives, values, gen)) {
                    if (isUnique(a.getUniqueFlag())) pendingUnique.add(a.getName());
                    else obtained.add(a.getName());
                }
            } catch (Exception ignored) {}
        });
        return new AbilityAwards(obtained, pendingUnique);
    }

    private Map<String, Integer> withModifiers(Map<String, Integer> values, Map<String, Integer> modifierTotals) {
        var result = new LinkedHashMap<>(values);
        result.replaceAll((key, value) -> value + modifierTotals.getOrDefault(key, 0));
        return result;
    }

    private boolean isUnique(String value) {
        return value != null && ("sí".equalsIgnoreCase(value) || "si".equalsIgnoreCase(value));
    }

    private Set<String> latestSnapshotValues(String characterId, String field) {
        return milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc(characterId).stream()
                .findFirst().map(milestone -> snapshotValues(milestone, field)).orElseGet(LinkedHashSet::new);
    }

    private Set<String> snapshotValues(MilestoneEntity milestone, String field) {
        try {
            Set<String> result = new LinkedHashSet<>();
            json.readTree(milestone.getSnapshotJson()).path(field).forEach(x -> result.add(x.asText()));
            return result;
        } catch (Exception e) { return new LinkedHashSet<>(); }
    }

    private record AbilityAwards(Set<String> obtained, Set<String> pendingUnique) {}

    private Set<String> snapshotAbilities(MilestoneEntity milestone) {
        try {
            var n = json.readTree(milestone.getSnapshotJson()).path("abilities");
            Set<String> result = new LinkedHashSet<>();
            n.forEach(x -> result.add(x.asText()));
            return result;
        } catch (Exception e) { return new LinkedHashSet<>(); }
    }

    public List<MilestoneEntity> milestones(String id) { get(id); return milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc(id); }
    @Transactional(readOnly = true)
    public Map<String, Object> lastUpgrade(String id) {
        get(id);
        var closedVersions = milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc(id);
        if (closedVersions.size() < 2) return Map.of("available", false);
        var current = closedVersions.getFirst(); var previous = closedVersions.get(1);
        return compareUpgrade(snapshot(current), snapshot(previous), current.getLevel(), current.getExperience(), snapshotAbilities(current), current.getCreatedAt(), previous.getLevel(), previous.getExperience(), previous.getCreatedAt(), snapshotAbilities(previous));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> currentUpgrade(String id) {
        var character = get(id);
        var closedVersions = milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc(id);
        if (closedVersions.isEmpty()) return Map.of("available", false);
        var previous = closedVersions.getFirst();
        var currentAttributes = parse(character.getAttributesJson());
        var currentGenetics = parse(character.getGeneticsJson());
        var currentMinorAttributes = minorAttributes.values(id);
        var currentModifierTotals = modifierTotals(id);
        var currentAwards = eligibleAbilities(withModifiers(currentAttributes, currentModifierTotals), currentGenetics,
                withModifiers(currentMinorAttributes, currentModifierTotals));
        var currentAbilities = new LinkedHashSet<String>(currentAwards.obtained());
        currentAbilities.addAll(currentAwards.pendingUnique());
        var currentSnapshot = json.valueToTree(Map.of(
                "attributes", currentAttributes,
                "genetics", currentGenetics,
                "minorAttributes", currentMinorAttributes,
                "abilities", currentAbilities));
        var previousAbilities = snapshotAbilities(previous);
        previousAbilities.addAll(snapshotValues(previous, "pendingUniqueAbilities"));
        return compareUpgrade(currentSnapshot, snapshot(previous), character.getLevel(), character.getExperience(), currentAbilities,
                character.getUpdatedAt(), previous.getLevel(), previous.getExperience(), previous.getCreatedAt(), previousAbilities);
    }

    private Map<String, Object> compareUpgrade(JsonNode currentSnapshot, JsonNode previousSnapshot,
                                               int currentLevel, int currentExperience, Set<String> currentAbilities,
                                               Object currentClosedAt, int previousLevel, int previousExperience, Object previousClosedAt,
                                               Set<String> previousAbilities) {
        var currentAttributes = snapshotRanks(currentSnapshot, "attributes"); var previousAttributes = snapshotRanks(previousSnapshot, "attributes");
        var currentGenetics = snapshotRanks(currentSnapshot, "genetics"); var previousGenetics = snapshotRanks(previousSnapshot, "genetics");
        var currentMinors = snapshotRanks(currentSnapshot, "minorAttributes"); var previousMinors = snapshotRanks(previousSnapshot, "minorAttributes");
        var scores = new ArrayList<Map<String, Object>>();
        appendScoreChanges(scores, "attribute", currentAttributes, previousAttributes);
        appendScoreChanges(scores, "genetic", currentGenetics, previousGenetics);
        appendScoreChanges(scores, "minorAttribute", currentMinors, previousMinors);
        var currentBonuses = CharacterRules.projectAtLevel(currentLevel, currentExperience, currentAttributes, currentGenetics, Map.of()).bonuses();
        var previousBonuses = CharacterRules.projectAtLevel(previousLevel, previousExperience, previousAttributes, previousGenetics, Map.of()).bonuses();
        var bonuses = new ArrayList<Map<String, Object>>(); var keys = new LinkedHashSet<String>(); keys.addAll(currentBonuses.keySet()); keys.addAll(previousBonuses.keySet());
        for (var key : keys) { var after = currentBonuses.getOrDefault(key, new CharacterRules.Bonus(0, 0)); var before = previousBonuses.getOrDefault(key, new CharacterRules.Bonus(0, 0)); int plusOne = after.plusOne() - before.plusOne(), plusD6 = after.plusD6() - before.plusD6(); if (plusOne > 0 || plusD6 > 0) bonuses.add(Map.of("key", key, "plusOne", plusOne, "plusD6", plusD6)); }
        var abilities = new LinkedHashSet<>(currentAbilities); abilities.removeAll(previousAbilities);
        return Map.of("available", true, "current", Map.of("level", currentLevel, "closedAt", currentClosedAt), "previous", Map.of("level", previousLevel, "closedAt", previousClosedAt), "scores", scores, "bonuses", bonuses, "abilities", abilities);
    }
    public Map<String, Object> preview(int xp, Map<String, Integer> attrs, Map<String, Integer> gen) { return Map.of("projection", CharacterRules.project(xp, attrs, gen)); }

    private JsonNode snapshot(MilestoneEntity milestone) { try { return json.readTree(milestone.getSnapshotJson()); } catch (Exception e) { throw new IllegalStateException("Closed version snapshot is invalid", e); } }
    private Map<String, Integer> snapshotRanks(JsonNode snapshot, String field) { var result = new LinkedHashMap<String, Integer>(); snapshot.path(field).fields().forEachRemaining(entry -> result.put(entry.getKey(), entry.getValue().asInt())); return result; }
    private void appendScoreChanges(List<Map<String, Object>> changes, String type, Map<String, Integer> current, Map<String, Integer> previous) { var keys = new LinkedHashSet<String>(); keys.addAll(current.keySet()); keys.addAll(previous.keySet()); for (var key : keys) { int before = previous.getOrDefault(key, 0), after = current.getOrDefault(key, 0), increase = after - before; if (increase > 0) changes.add(Map.of("key", key, "type", type, "before", before, "after", after, "increase", increase)); } }

    private static Map<String, Integer> parse(String value) {
        try { return new ObjectMapper().readValue(value, Map.class); }
        catch (Exception e) { return new LinkedHashMap<>(); }
    }
}
