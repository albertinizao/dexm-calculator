package com.dexm.personajes.application;

import com.dexm.personajes.adapter.in.web.AttributeDetailDto;
import com.dexm.personajes.adapter.in.web.CharacterController;
import com.dexm.personajes.adapter.out.persistence.*;
import com.dexm.personajes.domain.AbilityRules;
import com.dexm.personajes.domain.CharacterRules;
import com.dexm.personajes.domain.CharacterCreationRules;
import com.dexm.personajes.domain.AutomaticAbilityRules;
import com.dexm.personajes.domain.TrainingRules;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import com.dexm.personajes.security.SecurityIdentityService;
import com.dexm.personajes.security.AuthorizationService;

@Service
public class CharacterService {
    private static final String FLOW_SINGLE = "single";
    private static final String FLOW_SEQUENTIAL_ALL = "sequential-all";
    private static final int MAX_IMAGE_DATA_URL_LENGTH = 150 * 1024;
    private static final int MAX_EXTERNAL_IMAGE_URL_LENGTH = 2048;
    private static final List<String> LEGACY_GENETICS = List.of("heroe", "norna", "alfar", "valkiria", "dvergr", "risa");
    private static final Set<String> MAJOR_KEYS = Set.of("fisico", "agilidad", "percepcion", "mente", "estudio", "carisma");
    private static final Map<String, String> PREDEFINED_MINOR_FORMULAS = Map.ofEntries(
            Map.entry("astronavegar", "(agilidad+percepcion)/2"), Map.entry("atractivo", "carisma*2"),
            Map.entry("buscar", "percepcion"), Map.entry("conduccion", "(agilidad+percepcion)/2"),
            Map.entry("cruzarbifrost", "(mente+estudio)/2"), Map.entry("deporte", "fisico"),
            Map.entry("destreza", "(fisico+agilidad)/2"), Map.entry("diplomacia", "carisma"),
            Map.entry("einherjer", "(fisico+mente)/2"), Map.entry("engano", "(percepcion+mente)/2"),
            Map.entry("esconderse", "(agilidad+mente)/2"), Map.entry("evolcurva", "(fisico+agilidad+percepcion+mente+estudio+carisma)/6"), Map.entry("esquiva", "(fisico+agilidad)/2"),
            Map.entry("fisicaquimica", "estudio"), Map.entry("fuerza", "fisico"),
            Map.entry("informatica", "estudio"), Map.entry("intimidar", "(fisico+carisma)/2"),
            Map.entry("labia", "carisma"), Map.entry("liderazgo", "carisma"), Map.entry("medicina", "estudio"),
            Map.entry("provocar", "(mente+max(fisico,carisma))/2"), Map.entry("punteria", "percepcion"),
            Map.entry("resistencia", "fisico"), Map.entry("sentiryggdrasil", "(percepcion+mente)/2"));

    private final CharacterRepository characters;
    private final MilestoneRepository milestones;
    private final AbilityRepository abilities;
    private final OfficialCatalogService officialCatalog;
    private final ObjectMapper json;
    private final MinorAttributeService minorAttributes;
    private final CharacterMinorAttributeValueRepository minorValues;
    private final MinorAttributeDefinitionRepository minorDefs;
    private final CharacterAttributeModifierRepository modifiers;
    private final TrainingActivityRepository trainingActivities;
    @Autowired private SecurityIdentityService identities;
    @Autowired private AuthorizationService authorization;
    @Autowired(required = false) private CharacterAbilityStateRepository abilityStates;
    @Autowired(required = false) private CharacterInventoryAggregateRepository inventoryAggregates;
    @Autowired(required = false) private CharacterActivityAggregateRepository activityAggregates;
    @Autowired(required = false) private MilestoneInventorySnapshotRepository inventorySnapshots;
    @Autowired(required = false) private MilestoneActivitySnapshotRepository activitySnapshots;

    public CharacterService(CharacterRepository characters, MilestoneRepository milestones, AbilityRepository abilities,
                             ObjectMapper json, MinorAttributeService minorAttributes,
                             CharacterMinorAttributeValueRepository minorValues,
                             MinorAttributeDefinitionRepository minorDefs,
                             CharacterAttributeModifierRepository modifiers) {
        this(characters, milestones, abilities, json, minorAttributes, minorValues, minorDefs, modifiers, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CharacterService(CharacterRepository characters, MilestoneRepository milestones, AbilityRepository abilities,
                             ObjectMapper json, MinorAttributeService minorAttributes,
                             CharacterMinorAttributeValueRepository minorValues,
                             MinorAttributeDefinitionRepository minorDefs,
                             CharacterAttributeModifierRepository modifiers, TrainingActivityRepository trainingActivities,
                             OfficialCatalogService officialCatalog) {
        this.characters = characters;
        this.officialCatalog = officialCatalog;
        this.milestones = milestones;
        this.abilities = abilities;
        this.json = json;
        this.minorAttributes = minorAttributes;
        this.minorValues = minorValues;
        this.minorDefs = minorDefs;
        this.modifiers = modifiers;
        this.trainingActivities = trainingActivities;
    }

    public List<CharacterEntity> list() {
        var all=characters.findAll();
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (identities==null || auth==null || !auth.isAuthenticated() || identities.isAdmin(identities.current(auth))) return all;
        var owner=identities.requireCurrentUser(auth).getId();
        return all.stream().filter(c->owner.equals(c.getOwnerUserId())).toList();
    }
    public List<CharacterEntity> listByCampaign(String campaignId) { return characters.findByCampaignIdOrderByNameAsc(campaignId); }

    @Transactional
    public void deleteByCampaign(String campaignId) {
        for (var character : listByCampaign(campaignId)) {
            if (trainingActivities != null) trainingActivities.deleteByCharacterId(character.getId());
            modifiers.deleteByCharacterId(character.getId());
            minorValues.deleteAll(minorValues.findByCharacterId(character.getId()));
            milestones.deleteByCharacterId(character.getId());
            characters.delete(character);
        }
        minorDefs.deleteAll(minorDefs.findByCampaignIdOrderByNameAsc(campaignId));
    }

    @Transactional
    public void delete(String id) {
        var character = get(id);
        if (trainingActivities != null) trainingActivities.deleteByCharacterId(id);
        modifiers.deleteByCharacterId(id);
        minorValues.deleteAll(minorValues.findByCharacterId(id));
        minorDefs.findByCampaignIdOrderByNameAsc(character.getCampaignId()).stream()
                .filter(definition -> id.equals(definition.getOwnerCharacterId())).forEach(minorDefs::delete);
        milestones.deleteByCharacterId(id);
        characters.delete(character);
    }

    @Transactional
    public CharacterEntity create(String name) {
        try {
            var attrs = CharacterRules.zeroValues(CharacterRules.ATTRIBUTES);
            var gen = CharacterRules.zeroValues(CharacterRules.GENETICS);
            var c = new CharacterEntity(UUID.randomUUID().toString(), name, 0, json.writeValueAsString(attrs), json.writeValueAsString(gen));
            assignCurrentEditor(c);
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
            var c = new CharacterEntity(UUID.randomUUID().toString(), campaignId, name, validateImageUrl(imageUrl), 0,
                    json.writeValueAsString(attrs), json.writeValueAsString(gen));
            assignCurrentEditor(c);
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
    public List<String> editors(String id) { return new ArrayList<>(get(id).getEditorEmails()); }

    @Transactional
    public List<String> addEditor(String id, String email) {
        var normalized = com.dexm.personajes.security.AuthIdentity.normalizeEmail(email);
        if (normalized.isBlank() || !normalized.contains("@")) throw new IllegalArgumentException("Valid email is required");
        var character = get(id);
        character.addEditorEmail(normalized);
        characters.save(character);
        return editors(id);
    }

    @Transactional
    public List<String> removeEditor(String id, String email) {
        var character = get(id);
        character.removeEditorEmail(email);
        characters.save(character);
        return editors(id);
    }

    private void assignCurrentEditor(CharacterEntity character) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (identities == null || authentication == null || !authentication.isAuthenticated()) return;
        var identity = identities.current(authentication);
        character.addEditorEmail(identity.email());
        character.setOwnerUserId(identities.requireCurrentUser(authentication).getId());
    }

    private boolean canEditCurrent(CharacterEntity character) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authorization != null && authentication != null && authentication.isAuthenticated()
                && authorization.canEditCharacter(authentication, character);
    }

    @Transactional
    public Map<String, Object> beginEdit(String id) {
        var character = get(id);
        character.setClosed(false);
        character.touch();
        characters.save(character);
        return Map.of("closed", false);
    }

    public Map<String, Object> view(String id) {
        try {
            var c = get(id);
            return view(c, modifiers.findByCharacterId(id));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<AbilityEntity> abilityCatalog() {
        return officialCatalog != null ? officialCatalog.abilities() : abilities.findAll();
    }

    private Map<String, Object> view(CharacterEntity c, List<CharacterAttributeModifierEntity> preloadedModifierRows) {
        return view(c, preloadedModifierRows, null);
    }

    private Map<String, Object> view(CharacterEntity c, List<CharacterAttributeModifierEntity> preloadedModifierRows,
                                      Set<String> preloadedActiveAbilities) {
        try {
            var id = c.getId();
            var attrs = parse(c.getAttributesJson());
            var gen = parse(c.getGeneticsJson());
            var modifierRows = preloadedModifierRows;
            var modifierTotals = modifierTotals(modifierRows);
            var minorView = minorAttributes.view(c, attrs, gen, modifierRows);
            var customMinorRanks = minorView.stream().collect(Collectors.toMap(
                    item -> String.valueOf(item.get("key")), item -> ((Number) item.get("value")).intValue(),
                    (left, right) -> left, LinkedHashMap::new));
            var totals = new LinkedHashMap<String, Integer>();
            attrs.forEach((key, value) -> totals.put(key, value + modifierTotals.getOrDefault(key, 0)));
            gen.forEach((key, value) -> totals.put(key, value + modifierTotals.getOrDefault(key, 0)));
            minorView.forEach(attribute -> totals.put(String.valueOf(attribute.get("key")), ((Number) attribute.get("total")).intValue()));
            var derivedStats = CharacterRules.derivedStats(attrs, modifierTotals);

            var result = new LinkedHashMap<String, Object>();
            result.put("id", c.getId());
            result.put("campaignId", c.getCampaignId());
            result.put("name", c.getName());
            result.put("imageUrl", c.getImageUrl());
            result.put("creationMode", c.getCreationMode());
            result.put("race", c.getRace());
            result.put("einherjer", c.isEinherjer());
            result.put("awakened", c.isAwakened());
            result.put("einherjerOrigin", c.getEinherjerOrigin());
            result.put("startingAge", c.getStartingAge());
            result.put("awakeningAge", c.getAwakeningAge());
            result.put("sheetAge", c.getSheetAge());
            result.put("selectedMajorAttributes", parseList(c.getSelectedMajorAttributesJson()));
            result.put("creationWizardState", c.getCreationWizardState());
            result.put("canEdit", canEditCurrent(c));
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && authorization != null && authorization.isAdmin(authentication))
                result.put("editorEmails", c.getEditorEmails());
            result.put("experience", c.getExperience());
            result.put("level", c.getLevel());
            result.put("attributes", attrs);
            result.put("attributeTotals", totals);
            var modifierView = modifierView(modifierRows);
            result.put("attributeModifiers", modifierView);
            result.put("derivedStats", derivedStatsView(derivedStats, modifierView));
            result.put("genetics", gen);
            result.put("minorAttributes", minorView);
            // Ability eligibility and milestone history are loaded only on the abilities/history routes.
            result.put("abilities", List.of());
            result.put("pendingUniqueAbilities", List.of());
            var allocation = CharacterRules.allocationBudget(c.getEvolutionPoints(), c.getGeneticsPoints(), attrs, gen, customMinorRanks);
            result.put("allocation", allocationView(c, allocation, withModifiers(attrs, modifierTotals),
                    preloadedActiveAbilities == null ? currentActiveAbilities(c) : preloadedActiveAbilities));
            result.put("closed", c.isClosed());
            result.put("createdAt", c.getCreatedAt());
            result.put("updatedAt", c.getUpdatedAt());
            result.put("lastClosedAt", c.getCreatedAt());
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
    public Map<String, Object> save(String id, String name, Integer level, int xp, Map<String, Integer> attrs,
                                     Map<String, Integer> gen, Map<String, Integer> minor, boolean visible,
                                     boolean finalStep, Integer legacyEvolutionPoints) {
        return persistAllocation(id, name, level, xp, attrs, gen, minor, visible, finalStep, "save", legacyEvolutionPoints);
    }

    @Transactional
    public Map<String, Object> save(String id, String name, Integer level, int xp, Map<String, Integer> attrs,
                                     Map<String, Integer> gen, Map<String, Integer> minor, boolean visible,
                                     boolean finalStep, Integer legacyEvolutionPoints, Boolean einherjer,
                                     Boolean awakened, String origin, Integer startingAge, Integer awakeningAge,
                                     Integer sheetAge) {
        return persistAllocation(id, name, level, xp, attrs, gen, minor, visible, finalStep, "save", legacyEvolutionPoints,
                einherjer, awakened, origin, startingAge, awakeningAge, sheetAge);
    }

    @Transactional
    public Map<String, Object> save(String id, String name, Integer level, int xp, Map<String, Integer> attrs,
                                     Map<String, Integer> gen, Map<String, Integer> minor, boolean visible,
                                     boolean finalStep, Integer legacyEvolutionPoints, Boolean einherjer,
                                     Boolean awakened, String origin, Integer startingAge, Integer awakeningAge,
                                     Integer sheetAge, boolean imageProvided, String imageUrl) {
        return persistAllocation(id, name, level, xp, attrs, gen, minor, visible, finalStep, "save",
                legacyEvolutionPoints, einherjer, awakened, origin, startingAge, awakeningAge, sheetAge,
                imageProvided, imageUrl);
    }

    /** Parses the static HTML backup format without changing the character. */
    public Map<String, Object> importLegacy(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Legacy code is required");
        var allowed = new LinkedHashSet<String>();
        allowed.add("nivel"); allowed.add("experiencia"); allowed.add("evolGuardado");
        allowed.addAll(LEGACY_GENETICS);
        for (var key : CharacterRules.ATTRIBUTES) { allowed.add(key); allowed.add(key + "Extra"); }
        var values = new LinkedHashMap<String, Integer>();
        for (var token : code.trim().split("&&")) {
            if (token.isBlank()) continue;
            var separator = token.indexOf(':');
            if (separator <= 0 || separator != token.lastIndexOf(':')) throw new IllegalArgumentException("Invalid legacy token");
            var key = token.substring(0, separator).trim();
            if (!allowed.contains(key) || values.containsKey(key)) throw new IllegalArgumentException("Unknown or repeated legacy key: " + key);
            try {
                var value = Integer.parseInt(token.substring(separator + 1).trim());
                if (value < 0) throw new NumberFormatException();
                values.put(key, value);
            } catch (NumberFormatException e) { throw new IllegalArgumentException("Legacy values must be non-negative integers"); }
        }
        var required = new ArrayList<String>(List.of("nivel", "experiencia", "evolGuardado"));
        required.addAll(LEGACY_GENETICS); required.addAll(CharacterRules.ATTRIBUTES);
        CharacterRules.ATTRIBUTES.forEach(key -> required.add(key + "Extra"));
        for (var key : required) if (!values.containsKey(key)) throw new IllegalArgumentException("Missing legacy key: " + key);
        var attrs = new LinkedHashMap<String, Integer>();
        CharacterRules.ATTRIBUTES.forEach(key -> attrs.put(key, values.get(key)));
        var gen = new LinkedHashMap<String, Integer>();
        LEGACY_GENETICS.forEach(key -> gen.put(key, values.get(key)));
        var extras = new LinkedHashMap<String, Integer>();
        CharacterRules.ATTRIBUTES.forEach(key -> { var value = values.getOrDefault(key + "Extra", 0); if (value != 0) extras.put(key, value); });
        return Map.of("level", values.get("nivel"), "experience", values.get("experiencia"),
                "evolutionPoints", values.get("evolGuardado"), "attributes", attrs, "genetics", gen, "extras", extras);
    }

    /** Emits the canonical order consumed by index.html's Cargar Backup handler. */
    public String exportLegacy(String id) {
        var c = get(id); if (!c.isClosed()) throw new IllegalStateException("Only closed characters can export legacy backups");
        var attrs = parse(c.getAttributesJson()); var gen = parse(c.getGeneticsJson()); var totals = modifierTotals(id);
        var out = new StringBuilder();
        addLegacy(out, "nivel", c.getLevel()); addLegacy(out, "experiencia", c.getExperience()); addLegacy(out, "evolGuardado", c.getEvolutionPoints());
        LEGACY_GENETICS.forEach(key -> addLegacy(out, key, gen.getOrDefault(key, 0)));
        CharacterRules.ATTRIBUTES.forEach(key -> { addLegacy(out, key, attrs.getOrDefault(key, 0)); addLegacy(out, key + "Extra", totals.getOrDefault(key, 0)); });
        return out.toString();
    }

    private static void addLegacy(StringBuilder out, String key, int value) { out.append(key).append(':').append(value).append("&&"); }

    @Transactional
    public Map<String, Object> saveAttributeModifiers(String id,
                                                       Map<String, List<com.dexm.personajes.adapter.in.web.CharacterController.ModifierRequest>> requested) {
        var character = get(id);
        var allowed = new LinkedHashSet<String>(CharacterRules.ATTRIBUTES);
        allowed.addAll(CharacterRules.GENETICS);
        allowed.addAll(Set.of("vida", "bifrost", "defensaCuerpo", "defensaDistancia"));
        allowed.addAll(PREDEFINED_MINOR_FORMULAS.keySet());
        allowed.addAll(minorDefs.findByCampaignIdOrderByNameAsc(character.getCampaignId()).stream()
                .filter(definition -> definition.getOwnerCharacterId() == null || id.equals(definition.getOwnerCharacterId()))
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
                .filter(existing -> existing.getSource() == null || !existing.getSource().startsWith("TRAINING:"))
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
                        requestedModifier.name(), requestedModifier.value(), "MANUAL"));
            }
        });
        var updatedModifierRows = modifiers.findByCharacterId(id);
        // The modifier repository updates the embedded aggregate on every
        // save/delete. Do not persist the stale CharacterEntity loaded at the
        // beginning of this method afterwards, otherwise it overwrites the
        // newly saved manual modifiers and currentUpgrade reads old state.
        character.setModifiers(updatedModifierRows);
        character.setAggregateVersion(Math.max(1, character.getAggregateVersion()));
        character.setClosed(false);
        character.touch();
        characters.save(character);
        var activeAbilities = currentActiveAbilities(character);
        return Map.of("character", view(character, updatedModifierRows, activeAbilities), "visible", false, "final", false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> training(String id) {
        var character = get(id);
        var activities = trainingActivities == null ? List.<TrainingActivityEntity>of() : trainingActivities.findByCharacterIdOrderByStartAgeAscPriorityAsc(id);
        var trainingModifiers = activities.isEmpty() ? List.<CharacterAttributeModifierEntity>of() : modifiers.findByCharacterId(id);
        return training(character, activities, trainingModifiers);
    }

    private Map<String, Object> training(CharacterEntity c, List<TrainingActivityEntity> activities, List<CharacterAttributeModifierEntity> trainingModifiers) {
        var rows = activities.stream().map(a -> {
            var row = new LinkedHashMap<String,Object>(); row.put("id",a.getId()); row.put("type",a.getType()); row.put("name",a.getName());
            row.put("startAge",a.getStartAge()); row.put("endAge",a.getEndAge()); row.put("priority",a.getPriority()); row.put("concurrent",a.isConcurrent());
            row.put("primaryAttribute",a.getPrimaryAttribute()); row.put("secondaryAttribute",a.getSecondaryAttribute()); row.put("tertiaryAttribute",a.getTertiaryAttribute());
            row.put("modifiers", trainingModifiers.stream().filter(m -> ("TRAINING:"+a.getId()).equals(m.getSource())).map(m -> Map.of("attributeKey",m.getAttributeKey(),"name",m.getName(),"value",m.getExactValue())).toList());
            return row;
        }).toList();
        return Map.of("enabled", c.getStartingAge()!=null && c.getSheetAge()!=null, "startingAge", c.getStartingAge()==null?0:c.getStartingAge(), "sheetAge", c.getSheetAge()==null?0:c.getSheetAge(), "activities", rows);
    }

    @Transactional
    public Map<String,Object> addTraining(String id, CharacterController.TrainingActivityRequest request) {
        var character = ensureTrainingAvailable(id);
        var activities = new ArrayList<>(trainingActivities.findByCharacterIdOrderByStartAgeAscPriorityAsc(id));
        validateTrainingRequest(character, request, null, activities);
        var ages = normalizedTrainingAges(character, request);
        var entity = new TrainingActivityEntity(UUID.randomUUID().toString(), id, request.type().toUpperCase(Locale.ROOT), request.name().trim(), ages[0], ages[1],
                request.priority()==null?0:request.priority(), request.primaryAttribute(), request.secondaryAttribute(), request.tertiaryAttribute(), Boolean.TRUE.equals(request.concurrent()));
        trainingActivities.save(entity);
        activities.add(entity);
        var trainingModifiers = recalculateTraining(character, activities);
        return training(character, activities, trainingModifiers);
    }

    @Transactional
    public Map<String, Object> reorderTraining(String id, List<String> activityIds) {
        var character = ensureTrainingAvailable(id);
        if (activityIds == null || activityIds.isEmpty() || new HashSet<>(activityIds).size() != activityIds.size()) {
            throw new IllegalArgumentException("El orden de actividades no es válido");
        }
        var all = trainingActivities.findByCharacterIdOrderByStartAgeAscPriorityAsc(id);
        var selected = all.stream().filter(activity -> activityIds.contains(activity.getId())).toList();
        if (selected.size() != activityIds.size()) {
            throw new IllegalArgumentException("Solo se pueden reordenar actividades simultáneas del personaje");
        }
        var startAge = selected.get(0).getStartAge();
        if (selected.stream().anyMatch(activity -> activity.getStartAge() != startAge)) {
            throw new IllegalArgumentException("Las actividades deben comenzar a la misma edad");
        }
        var group = all.stream().filter(activity -> activity.getStartAge() == startAge).toList();
        if (group.size() != activityIds.size() || !new HashSet<>(activityIds).equals(group.stream().map(TrainingActivityEntity::getId).collect(Collectors.toSet()))) {
            throw new IllegalArgumentException("Debe indicarse el grupo simultáneo completo");
        }
        var byId = group.stream().collect(Collectors.toMap(TrainingActivityEntity::getId, activity -> activity));
        for (int index = 0; index < activityIds.size(); index++) byId.get(activityIds.get(index)).setPriority(index);
        trainingActivities.saveAll(group);
        var trainingModifiers = recalculateTraining(character, all);
        return training(character, all, trainingModifiers);
    }

    @Transactional
    public Map<String,Object> updateTraining(String id, String activityId, CharacterController.TrainingActivityRequest request) {
        var character = ensureTrainingAvailable(id);
        var all = new ArrayList<>(trainingActivities.findByCharacterIdOrderByStartAgeAscPriorityAsc(id));
        var entity = all.stream().filter(activity -> activityId.equals(activity.getId())).findFirst().orElseThrow(() -> new NoSuchElementException("Training activity not found"));
        validateTrainingRequest(character, request, activityId, all);
        var ages = normalizedTrainingAges(character, request);
        entity.setType(request.type().toUpperCase(Locale.ROOT)); entity.setName(request.name().trim()); entity.setStartAge(ages[0]); entity.setEndAge(ages[1]); entity.setPriority(request.priority()==null?0:request.priority()); entity.setPrimaryAttribute(request.primaryAttribute()); entity.setSecondaryAttribute(request.secondaryAttribute()); entity.setTertiaryAttribute(request.tertiaryAttribute()); entity.setConcurrent(Boolean.TRUE.equals(request.concurrent()));
        trainingActivities.save(entity);
        var trainingModifiers = recalculateTraining(character, all);
        return training(character, all, trainingModifiers);
    }

    @Transactional
    public void deleteTraining(String id, String activityId) { var character = ensureTrainingAvailable(id); var all = new ArrayList<>(trainingActivities.findByCharacterIdOrderByStartAgeAscPriorityAsc(id)); var entity=all.stream().filter(activity -> activityId.equals(activity.getId())).findFirst().orElseThrow(() -> new NoSuchElementException("Training activity not found")); trainingActivities.delete(entity); all.remove(entity); recalculateTraining(character, all); }

    private CharacterEntity ensureTrainingAvailable(String id) { if (trainingActivities==null) throw new IllegalStateException("Training is unavailable in this context"); var c=get(id); if(c.getStartingAge()==null||c.getSheetAge()==null||!"guided".equals(c.getCreationMode())) throw new IllegalStateException("Training is only available for guided characters"); if(c.isClosed()) throw new IllegalStateException("La ficha está cerrada; ábrela para modificar la trayectoria"); return c; }
    private void validateTrainingRequest(CharacterEntity c, CharacterController.TrainingActivityRequest r, String ignoredId, List<TrainingActivityEntity> all) {
        var type=r.type().toUpperCase(Locale.ROOT); if(!Set.of("FORMATION","PROFESSION","OCCUPATION","COURSE").contains(type)) throw new IllegalArgumentException("Tipo de actividad no válido");
        if(!"COURSE".equals(type) && (r.endAge()<=r.startAge()||r.startAge()<c.getStartingAge()||r.endAge()>c.getSheetAge()+1)) throw new IllegalArgumentException("El intervalo queda fuera de la vida de la ficha");
        var attrs=java.util.stream.Stream.of(r.primaryAttribute(),r.secondaryAttribute(),r.tertiaryAttribute()).filter(Objects::nonNull).filter(s->!s.isBlank()).toList(); if(attrs.size()!=new HashSet<>(attrs).size()) throw new IllegalArgumentException("Los atributos de una actividad deben ser distintos");
        var allowedMinorAttributes = trainingMinorAttributeKeys(c);
        if (attrs.stream().anyMatch(attribute -> !allowedMinorAttributes.contains(attribute))) throw new IllegalArgumentException("El atributo no es un atributo menor de la ficha");
        if("COURSE".equals(type) && (r.secondaryAttribute()!=null || r.tertiaryAttribute()!=null)) throw new IllegalArgumentException("Un curso solo puede tener un atributo");
        if((!"COURSE".equals(type)) && attrs.isEmpty()) throw new IllegalArgumentException("La actividad necesita al menos un atributo");
        for(var a:all){if(a.getId().equals(ignoredId)||"COURSE".equals(a.getType())||"COURSE".equals(type))continue; boolean overlap=r.startAge()<a.getEndAge()&&a.getStartAge()<r.endAge(); if(overlap && !("OCCUPATION".equals(type)&&r.concurrent()) && !("OCCUPATION".equals(a.getType())&&a.isConcurrent())) throw new IllegalArgumentException("Las actividades principales no pueden solaparse");}
        if("COURSE".equals(type) && all.stream().filter(a->"COURSE".equals(a.getType()) && !a.getId().equals(ignoredId)).count()>=TrainingRules.courseSlots(c.getStartingAge(), c.getSheetAge())) throw new IllegalArgumentException("No quedan Cursos disponibles para esta ficha");
        if("COURSE".equals(type) && r.primaryAttribute()!=null && !r.primaryAttribute().isBlank()) {
            var currentTotals = trainingModifierTotals(c, ignoredId, all);
            if (currentTotals.getOrDefault(r.primaryAttribute(), BigDecimal.ZERO).compareTo(BigDecimal.valueOf(5)) >= 0) throw new IllegalArgumentException("No se puede seleccionar un atributo con +5 o más de trayectoria");
        }
    }

    private int[] normalizedTrainingAges(CharacterEntity character, CharacterController.TrainingActivityRequest request) {
        return "COURSE".equalsIgnoreCase(request.type()) ? new int[]{character.getStartingAge(), character.getSheetAge()} : new int[]{request.startAge(), request.endAge()};
    }

    private Map<String, BigDecimal> trainingModifierTotals(CharacterEntity character, String ignoredId, List<TrainingActivityEntity> all) {
        var activities = all.stream().filter(a -> !a.getId().equals(ignoredId)).toList();
        var totals = new LinkedHashMap<String, BigDecimal>();
        calculateTraining(character, activities).values().forEach(calculation -> calculation.modifiers().forEach(modifier -> totals.merge(modifier.attributeKey(), modifier.value(), BigDecimal::add)));
        return totals;
    }

    private Set<String> trainingMinorAttributeKeys(CharacterEntity character) {
        var keys = new LinkedHashSet<>(PREDEFINED_MINOR_FORMULAS.keySet());
        keys.remove("astronavegar");
        keys.addAll(minorDefs.findByCampaignIdOrderByNameAsc(character.getCampaignId()).stream()
                .filter(definition -> definition.getOwnerCharacterId() == null || character.getId().equals(definition.getOwnerCharacterId()))
                .map(MinorAttributeDefinitionEntity::getKey).toList());
        return keys;
    }

    @Transactional
    List<CharacterAttributeModifierEntity> recalculateTraining(CharacterEntity c, List<TrainingActivityEntity> activities) {
        var id = c.getId();
        var existingModifiers = modifiers.findByCharacterId(id);
        existingModifiers.stream()
                .filter(m -> m.getSource() != null && m.getSource().startsWith("TRAINING:"))
                .forEach(modifiers::delete);
        // Force the deletes before inserting the recalculated rows. The database
        // uniqueness constraint is character + attribute + name, so Hibernate's
        // deferred flush would otherwise see the old training rows still present.
        modifiers.flush();
        var generated = new ArrayList<CharacterAttributeModifierEntity>();
        var calculations = calculateTraining(c, activities);
        calculations.forEach((activityId, calculation) -> calculation.modifiers().forEach(modifier -> {
            if (modifier.value().signum() != 0) {
                var row = new CharacterAttributeModifierEntity(UUID.randomUUID().toString(), id, modifier.attributeKey(),
                        modifier.type() + ": " + modifier.activityName(), modifier.value(), "TRAINING:" + activityId);
                modifiers.save(row);
                generated.add(row);
            }
        }));
        return generated;
    }

    private TrainingActivityEntity trainingActivity(String activityId, String characterId, CharacterController.TrainingActivityRequest request) {
        var character = get(characterId);
        var ages = normalizedTrainingAges(character, request);
        return new TrainingActivityEntity(activityId, characterId, request.type().toUpperCase(Locale.ROOT), request.name().trim(),
                ages[0], ages[1], request.priority() == null ? 0 : request.priority(), request.primaryAttribute(),
                request.secondaryAttribute(), request.tertiaryAttribute(), Boolean.TRUE.equals(request.concurrent()));
    }

    private Map<String, TrainingCalculation> calculateTraining(CharacterEntity character, List<TrainingActivityEntity> activities) {
        var orderedActivities = activities.stream()
                .sorted(Comparator.comparingInt((TrainingActivityEntity a) -> "COURSE".equals(a.getType()) ? 1 : 0)
                        .thenComparingInt(TrainingActivityEntity::getStartAge)
                        .thenComparingInt(TrainingActivityEntity::getPriority)).toList();
        var selections = new HashMap<String, Integer>();
        var totals = new LinkedHashMap<String, BigDecimal>();
        var attrs = parse(character.getAttributesJson());
        var calculations = new LinkedHashMap<String, TrainingCalculation>();
        for (var activity : orderedActivities) {
            var type = activity.getType();
            var humanYears = TrainingRules.humanEquivalent(new TrainingRules.Activity(type, activity.getStartAge(), activity.getEndAge(), activity.getPriority(), activity.getPrimaryAttribute(), activity.getSecondaryAttribute(), activity.getTertiaryAttribute(), activity.isConcurrent()), character.isEinherjer(), character.getEinherjerOrigin(), character.getAwakeningAge());
            var bonus = TrainingRules.bonus(type, humanYears);
            var values = List.of(bonus.primary(), bonus.secondary(), bonus.tertiary());
            var keys = java.util.stream.Stream.of(activity.getPrimaryAttribute(), activity.getSecondaryAttribute(), activity.getTertiaryAttribute()).toList();
            var activityModifiers = new ArrayList<TrainingModifier>();
            for (int index = 0; index < 3; index++) {
                var key = keys.get(index); if (key == null || key.isBlank()) continue;
                BigDecimal baseValue; int previousSelections; BigDecimal value;
                if ("COURSE".equals(type)) {
                    var current = totals.getOrDefault(key, BigDecimal.ZERO);
                    previousSelections = 0; baseValue = current.compareTo(BigDecimal.valueOf(2)) < 0 ? BigDecimal.valueOf(2) : current.compareTo(BigDecimal.valueOf(5)) < 0 ? BigDecimal.ONE : BigDecimal.ZERO;
                    if (attrs.getOrDefault(key, 0) > 0) baseValue = BigDecimal.ZERO;
                    value = baseValue;
                } else {
                    baseValue = values.get(index); previousSelections = selections.getOrDefault(key, 0); value = TrainingRules.coincidence(baseValue, previousSelections); selections.put(key, previousSelections + 1);
                }
                totals.merge(key, value, BigDecimal::add); activityModifiers.add(new TrainingModifier(key, type, activity.getName(), baseValue, previousSelections, value));
            }
            calculations.put(activity.getId(), new TrainingCalculation(humanYears, activityModifiers));
        }
        return calculations;
    }

    private record TrainingCalculation(double humanYears, List<TrainingModifier> modifiers) {}
    private record TrainingModifier(String attributeKey, String type, String activityName, BigDecimal baseValue, int previousSelections, BigDecimal value) {}

    @Transactional(readOnly = true)
    public AttributeDetailDto attributeDetail(String characterId, String attributeKey) {
        var character = get(characterId);
        var attrs = parse(character.getAttributesJson());
        var genetics = parse(character.getGeneticsJson());
        var modifierRows = modifiers.findByCharacterIdAndAttributeKey(characterId, attributeKey);
        var modifierDtos = modifierRows.stream().map(m -> new AttributeDetailDto.ModifierDto(m.getName(), m.getExactValue(), m.getSource())).toList();
        var modifierTotal = TrainingRules.roundTotal(modifierRows.stream().map(CharacterAttributeModifierEntity::getExactValue).toList());
        var ranks = CharacterRules.GENETICS.contains(attributeKey) ? genetics.getOrDefault(attributeKey, 0) : attrs.getOrDefault(attributeKey, 0);
        var total = ranks + modifierTotal;
        var derived = CharacterRules.derivedStats(attrs, modifierTotals(characterId)).get(attributeKey);
        if (derived != null) return new AttributeDetailDto(attributeKey, null, derived.name(), "DERIVED", derived.total(), 0, null, derived.formula(), derived.baseValue(), 0, 0, modifierDtos, List.of(), false);
        if (MAJOR_KEYS.contains(attributeKey)) {
            Integer max = ranks >= 5 ? MAJOR_KEYS.stream().filter(k -> !k.equals(attributeKey)).mapToInt(k -> attrs.getOrDefault(k, 0)).max().orElse(0) * 2 : null;
            var formula = ranks >= 5 ? "2 × el rango mayor más alto de los otros atributos" : "No aplica por debajo de 5 rangos";
            return new AttributeDetailDto(attributeKey, null, label(attributeKey), "MAJOR", total, ranks, max, formula, max == null ? 0 : max, bonus(total, attributeKey, true, false), bonus(total, attributeKey, false, false), modifierDtos, List.of(), false);
        }
        if (CharacterRules.GENETICS.contains(attributeKey)) return new AttributeDetailDto(attributeKey, null, label(attributeKey), "GENETIC", total, ranks, null, "Sin máximo calculado", 0, 0, 0, modifierDtos, List.of(), false);
        var definition = minorDefs.findByCampaignIdAndOwnerCharacterIdAndKey(character.getCampaignId(), characterId, attributeKey)
                .or(() -> minorDefs.findByCampaignIdAndOwnerCharacterIdIsNullAndKey(character.getCampaignId(), attributeKey)).orElse(null);
        if (definition == null && CharacterRules.ATTRIBUTES.contains(attributeKey)) {
            var formula = PREDEFINED_MINOR_FORMULAS.get(attributeKey);
            var calculated = formula == null ? 0 : MinorAttributeService.evaluate(formula, attrs, genetics, minorAttributes.values(characterId), character.getLevel());
            return new AttributeDetailDto(attributeKey, null, label(attributeKey), "PREDEFINED", total, ranks, formula == null ? null : calculated, formula == null ? "Máximo especial" : formula, calculated, bonus(total, attributeKey, true, false), bonus(total, attributeKey, false, false), modifierDtos, List.of(), false);
        }
        if (definition == null) throw new NoSuchElementException("Attribute not found");
        ranks = minorValues.findByCharacterIdAndDefinitionId(characterId, definition.getId()).map(CharacterMinorAttributeValueEntity::getValue).orElse(0);
        total = ranks + modifierTotal;
        var calculated = MinorAttributeService.evaluate(definition.getMaxFormula(), attrs, genetics, minorAttributes.values(characterId), character.getLevel());
        var source = definition.getBonusSource() == null ? attributeKey : definition.getBonusSource();
        var customBonus = "GALDR".equals(definition.getType()) ? new CharacterRules.Bonus(total / 5, total / 3) : sourceBonus(characterId, source, attrs, genetics);
        return new AttributeDetailDto(attributeKey, definition.getId(), definition.getName(), definition.getType(), total, ranks, calculated, definition.getMaxFormula(), calculated, customBonus.plusOne(), customBonus.plusD6(), modifierDtos, List.of(), "CUSTOM".equals(definition.getType()));
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
        return persistAllocation(id, requestedName, requestedLevel, requestedXp, requestedAttributes, requestedGenetics,
                requestedCustomMinors, visible, finalStep, flow, null);
    }

    private Map<String, Object> persistAllocation(String id, String requestedName, Integer requestedLevel, int requestedXp,
                                                   Map<String, Integer> requestedAttributes, Map<String, Integer> requestedGenetics,
                                                   Map<String, Integer> requestedCustomMinors, boolean visible, boolean finalStep,
                                                   String flow, Integer legacyEvolutionPoints) {
        return persistAllocation(id, requestedName, requestedLevel, requestedXp, requestedAttributes, requestedGenetics,
                requestedCustomMinors, visible, finalStep, flow, legacyEvolutionPoints, null, null, null, null, null, null);
    }

    private Map<String, Object> persistAllocation(String id, String requestedName, Integer requestedLevel, int requestedXp,
                                                   Map<String, Integer> requestedAttributes, Map<String, Integer> requestedGenetics,
                                                   Map<String, Integer> requestedCustomMinors, boolean visible, boolean finalStep,
                                                   String flow, Integer legacyEvolutionPoints, Boolean requestedEinherjer,
                                                   Boolean requestedAwakened, String requestedOrigin, Integer requestedStartingAge,
                                                   Integer requestedAwakeningAge, Integer requestedSheetAge) {
        return persistAllocation(id, requestedName, requestedLevel, requestedXp, requestedAttributes, requestedGenetics,
                requestedCustomMinors, visible, finalStep, flow, legacyEvolutionPoints, requestedEinherjer,
                requestedAwakened, requestedOrigin, requestedStartingAge, requestedAwakeningAge, requestedSheetAge,
                false, null);
    }

    private Map<String, Object> persistAllocation(String id, String requestedName, Integer requestedLevel, int requestedXp,
                                                   Map<String, Integer> requestedAttributes, Map<String, Integer> requestedGenetics,
                                                   Map<String, Integer> requestedCustomMinors, boolean visible, boolean finalStep,
                                                   String flow, Integer legacyEvolutionPoints, Boolean requestedEinherjer,
                                                   Boolean requestedAwakened, String requestedOrigin, Integer requestedStartingAge,
                                                   Integer requestedAwakeningAge, Integer requestedSheetAge,
                                                   boolean imageProvided, String imageUrl) {
        try {
            var character = get(id);
            var modifierRows = modifiers.findByCharacterId(id);
            var modifierTotals = modifierTotals(modifierRows);
            var currentCustomMinors = minorAttributes.values(character.getId());
            var abilityCatalog = abilityCatalog();
            if (imageProvided) character.setImageUrl(validateImageUrl(imageUrl));
            updateProfile(character, requestedEinherjer, requestedAwakened, requestedOrigin, requestedStartingAge,
                    requestedAwakeningAge, requestedSheetAge);
            var abilitiesBeforeChange = currentActiveAbilities(character, modifierTotals, currentCustomMinors, abilityCatalog);
            int targetLevel = requestedLevel == null ? character.getLevel() : requestedLevel;
            validateLevelAndExperience(character, targetLevel, requestedXp, flow, visible, finalStep, legacyEvolutionPoints != null);

            var attributes = normalizeRanks("attributes", requestedAttributes, parse(character.getAttributesJson()), CharacterRules.ATTRIBUTES, true);
            var genetics = normalizeRanks("genetics", requestedGenetics, parse(character.getGeneticsJson()), CharacterRules.GENETICS, true);
            var currentAttributes = parse(character.getAttributesJson());
            var currentAttributeTotals = withModifiers(currentAttributes, modifierTotals);
            var currentGenetics = parse(character.getGeneticsJson());
            var customMinors = normalizeCustomMinorRanks(character, requestedCustomMinors);
            if (legacyEvolutionPoints == null) {
                rejectRankReductions(currentAttributes, attributes, "attributes");
                rejectRankReductions(currentGenetics, genetics, "genetics");
                rejectRankReductions(currentCustomMinors, customMinors, "minor attributes");
            }
            if (FLOW_SINGLE.equals(flow) || FLOW_SEQUENTIAL_ALL.equals(flow)) {
                rejectGeneticLevelOverflow(currentGenetics, genetics);
                int geneticReward = CharacterRules.GENETICS_POINTS_PER_LEVEL + (AutomaticAbilityRules.grantsExtraGenetics(abilitiesBeforeChange) ? 1 : 0);
                if (CharacterRules.geneticDelta(currentGenetics, genetics) != geneticReward) {
                    throw new IllegalArgumentException("Exactly " + geneticReward + " genetic points must be assigned per level");
                }
            }
            if (legacyEvolutionPoints == null) clampMinorRanks(character, targetLevel, attributes, genetics, customMinors);

            int evolutionReward = flow.equals(FLOW_SINGLE) || flow.equals(FLOW_SEQUENTIAL_ALL)
                    ? CharacterRules.EVOLUTION_POINTS_PER_LEVEL + currentAttributeTotals.getOrDefault("evolcurva", 0)
                    + (AutomaticAbilityRules.grantsExtraEvolution(abilitiesBeforeChange) ? 5 : 0)
                    : 0;
            int geneticsReward = flow.equals(FLOW_SINGLE) || flow.equals(FLOW_SEQUENTIAL_ALL)
                    ? CharacterRules.GENETICS_POINTS_PER_LEVEL + (AutomaticAbilityRules.grantsExtraGenetics(abilitiesBeforeChange) ? 1 : 0) : 0;
            int evolutionAvailable = character.getEvolutionPoints() + evolutionReward;
            int geneticsAvailable = FLOW_SINGLE.equals(flow) || FLOW_SEQUENTIAL_ALL.equals(flow)
                    ? geneticsReward : character.getGeneticsPoints() + geneticsReward;
            var budget = CharacterRules.allocationBudget(evolutionAvailable, geneticsAvailable,
                    currentAttributes, currentGenetics, currentCustomMinors,
                    attributes, genetics, customMinors,
                    AutomaticAbilityRules.reducesForceEvolutionCost(abilitiesBeforeChange));
            if (legacyEvolutionPoints == null && budget.evolutionRemaining() < 0) throw new IllegalArgumentException("Evolution points budget exceeded");
            if (legacyEvolutionPoints == null && budget.geneticsRemaining() < 0) throw new IllegalArgumentException("Genetics points budget exceeded");

            String name = requestedName == null ? character.getName() : requestedName;
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Character name is required");
            var previous = milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc(id).stream().findFirst();
            var projection = CharacterRules.projectAtLevel(targetLevel, requestedXp, attributes, genetics, modifierTotals);

            character.setName(name);
            character.setExperience(requestedXp);
            character.setLevel(targetLevel);
            character.setAttributesJson(json.writeValueAsString(attributes));
            character.setGeneticsJson(json.writeValueAsString(genetics));
            character.setEvolutionPoints(legacyEvolutionPoints == null ? budget.evolutionRemaining() : legacyEvolutionPoints);
            character.setGeneticsPoints(FLOW_SINGLE.equals(flow) || FLOW_SEQUENTIAL_ALL.equals(flow)
                    ? 0 : budget.geneticsRemaining());
            character.setClosed(visible && finalStep);
            character.touch();
            characters.save(character);
            persistCustomMinorRanks(character, customMinors);

            var effectiveAttributes = withModifiers(attributes, modifierTotals);
            var effectiveCustomMinors = withModifiers(customMinors, modifierTotals);
            var awards = eligibleAbilities(effectiveAttributes, withModifiers(genetics, modifierTotals), effectiveCustomMinors, abilityCatalog);
            var all = awards.obtained();
            var before = previous.map(this::snapshotAbilities).orElse(Set.of());
            var newly = new LinkedHashSet<>(all);
            newly.removeAll(before);
            syncAutomaticModifiers(character, modifierRows, all, newly);
            ensureGaldr(id, all.contains("Lenguaje Galdr"), newly.contains("Lenguaje Galdr"));
            modifierRows = modifiers.findByCharacterId(id);
            modifierTotals = modifierTotals(modifierRows);
            customMinors = minorAttributes.values(id);
            var activeAbilitiesAfterChange = currentActiveAbilities(character, modifierTotals, customMinors, abilityCatalog);
            projection = CharacterRules.projectAtLevel(targetLevel, requestedXp, attributes, genetics, modifierTotals);
            var snapshot = new LinkedHashMap<String, Object>();
            snapshot.put("name", name);
            snapshot.put("experience", requestedXp);
            snapshot.put("level", targetLevel);
            snapshot.put("attributes", attributes);
            snapshot.put("genetics", genetics);
            snapshot.put("minorAttributes", customMinors);
            snapshot.put("modifiers", modifierSnapshot(modifierRows));
            snapshot.put("evolutionPoints", character.getEvolutionPoints());
            snapshot.put("geneticsPoints", character.getGeneticsPoints());
            snapshot.put("imageUrl", character.getImageUrl());
            snapshot.put("uniqueAbilityDecisions", uniqueAbilityDecisions(character));
            snapshot.put("abilities", all);
            snapshot.put("pendingUniqueAbilities", awards.pendingUnique());
            snapshot.put("visible", visible);
            var newBonuses = Map.of("level", projection.level(), "bonuses", projection.bonuses(), "allocation", budget);
            var milestone = new MilestoneEntity(UUID.randomUUID().toString(), id, targetLevel, requestedXp,
                    json.writeValueAsString(snapshot), json.writeValueAsString(newBonuses), json.writeValueAsString(newly), visible);
            milestones.save(milestone);
            saveHeavyHistoryParts(milestone, id);
            if (abilityStates != null) {
                var state = abilityStates.findById(id).orElse(new CharacterAbilityStateEntity(id));
                state.setObtained(new ArrayList<>(all));
                state.setPendingUnique(new ArrayList<>(awards.pendingUnique()));
                state.setSourceMilestoneId(milestone.getId());
                abilityStates.save(state);
            }

            var response = new LinkedHashMap<String, Object>();
            response.put("character", view(character, modifierRows, activeAbilitiesAfterChange));
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
                                             boolean visible, boolean finalStep, boolean legacyImport) {
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
        if (!legacyImport && targetLevel != character.getLevel()) throw new IllegalStateException("Regular saves cannot change level");
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
        return modifierTotals(modifiers.findByCharacterId(characterId));
    }

    private Map<String, Integer> modifierTotals(List<CharacterAttributeModifierEntity> rows) {
        return rows.stream().collect(Collectors.groupingBy(
                CharacterAttributeModifierEntity::getAttributeKey, LinkedHashMap::new,
                Collectors.mapping(CharacterAttributeModifierEntity::getExactValue, Collectors.toList())))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> TrainingRules.roundTotal(entry.getValue()),
                 (first, ignored) -> first, LinkedHashMap::new));
    }

    private void updateProfile(CharacterEntity character, Boolean requestedEinherjer, Boolean requestedAwakened,
                                String requestedOrigin, Integer requestedStartingAge, Integer requestedAwakeningAge,
                                Integer requestedSheetAge) {
        if (requestedEinherjer == null && requestedAwakened == null && requestedOrigin == null
                && requestedStartingAge == null && requestedAwakeningAge == null && requestedSheetAge == null) return;
        boolean einherjer = requestedEinherjer == null || requestedEinherjer;
        boolean awakened = requestedAwakened == null ? character.isAwakened() : requestedAwakened;
        String origin = requestedOrigin == null ? character.getEinherjerOrigin() : requestedOrigin;
        Integer startingAge = requestedStartingAge == null ? character.getStartingAge() : requestedStartingAge;
        Integer sheetAge = requestedSheetAge == null ? character.getSheetAge() : requestedSheetAge;
        Integer awakeningAge = requestedAwakened != null && !requestedAwakened ? null
                : (requestedAwakeningAge == null ? character.getAwakeningAge() : requestedAwakeningAge);
        if (!einherjer) throw new IllegalArgumentException("Todos los personajes deben ser Einherjer");
        if (sheetAge != null && sheetAge < 0) throw new IllegalArgumentException("La edad actual no puede ser negativa");
        if (awakened && awakeningAge == null) throw new IllegalArgumentException("La edad de despertar es obligatoria");
        if (!awakened) awakeningAge = null;
        if (origin != null && !CharacterCreationRules.EINHERJER_ORIGINS.contains(origin)) throw new IllegalArgumentException("Origen Einherjer no válido");
        if (awakeningAge != null && startingAge != null && (awakeningAge < startingAge || (sheetAge != null && awakeningAge > sheetAge))) {
            throw new IllegalArgumentException("La edad de despertar no es válida");
        }
        if (startingAge != null && sheetAge != null) {
            if (origin != null) TrainingRules.validateProfile(startingAge, awakeningAge, sheetAge, true, origin);
            if (trainingActivities != null && trainingActivities.findByCharacterIdOrderByStartAgeAscPriorityAsc(character.getId()).stream()
                    .anyMatch(activity -> activity.getEndAge() > sheetAge + 1)) {
                throw new IllegalArgumentException("La edad actual no puede ser inferior al final de una trayectoria existente");
            }
        }
        character.setEinherjer(true);
        character.setAwakened(awakened);
        character.setEinherjerOrigin(origin);
        character.setStartingAge(startingAge);
        character.setAwakeningAge(awakeningAge);
        character.setSheetAge(sheetAge);
    }

    @Transactional
    public Map<String, Object> configureCreation(String id, CharacterCreationRules.Configuration configuration) {
        CharacterCreationRules.validate(configuration);
        if ("complete".equals(configuration.wizardState()) && "guided".equals(configuration.mode()) && configuration.startingAge() == null) {
            throw new IllegalArgumentException("La creación guiada necesita las edades de la trayectoria");
        }
        var character = get(id);
        if (character.getLevel() != 1 || character.getExperience() != 0) {
            throw new IllegalStateException("Initial creation is only available before character progression");
        }
        if ("complete".equals(character.getCreationWizardState())) {
            throw new IllegalStateException("Character creation is already complete");
        }
        if ("guided".equals(character.getCreationMode()) && !"guided".equals(configuration.mode())) {
            throw new IllegalStateException("Guided creation cannot be changed to empty creation");
        }
        try {
            character.setCreationMode(configuration.mode());
            if (configuration.race() != null) character.setRace(configuration.race());
            if (configuration.einherjer() != null) character.setEinherjer(configuration.einherjer());
            if (configuration.awakened() != null) character.setAwakened(configuration.awakened());
            if (configuration.einherjerOrigin() != null) character.setEinherjerOrigin(configuration.einherjerOrigin());
            if (configuration.startingAge() != null) character.setStartingAge(configuration.startingAge());
            if (configuration.awakeningAge() != null || Boolean.FALSE.equals(configuration.awakened())) character.setAwakeningAge(configuration.awakeningAge());
            if (configuration.sheetAge() != null) character.setSheetAge(configuration.sheetAge());
            character.setSelectedMajorAttributesJson(json.writeValueAsString(configuration.selectedMajorAttributes()));
            character.setCreationWizardState(configuration.wizardState());
            character.setAttributesJson(json.writeValueAsString(CharacterCreationRules.attributesFor(configuration)));
            character.setClosed(true);
            character.touch();
            characters.save(character);
            return view(id);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not persist creation configuration", e);
        }
    }

    private Map<String, Object> allocationView(CharacterEntity character, CharacterRules.AllocationBudget budget, Map<String,Integer> attrs) {
        return allocationView(character, budget, attrs, currentActiveAbilities(character));
    }

    private Map<String, Object> allocationView(CharacterEntity character, CharacterRules.AllocationBudget budget,
                                                Map<String,Integer> attrs, Set<String> active) {
        var result = new LinkedHashMap<String,Object>();
        result.put("evolutionAvailable", budget.evolutionAvailable()); result.put("evolutionSpent", budget.evolutionSpent()); result.put("evolutionRemaining", budget.evolutionRemaining());
        result.put("geneticsAvailable", budget.geneticsAvailable()); result.put("geneticsSpent", budget.geneticsSpent()); result.put("geneticsRemaining", budget.geneticsRemaining());
        result.put("nextEvolutionReward", CharacterRules.EVOLUTION_POINTS_PER_LEVEL + attrs.getOrDefault("evolcurva", 0) + (AutomaticAbilityRules.grantsExtraEvolution(active) ? 5 : 0));
        result.put("nextGeneticsReward", CharacterRules.GENETICS_POINTS_PER_LEVEL + (AutomaticAbilityRules.grantsExtraGenetics(active) ? 1 : 0));
        result.put("minorEvolutionCost", AutomaticAbilityRules.reducesForceEvolutionCost(active) ? 4 : 5);
        return result;
    }

    private void syncAutomaticModifiers(String characterId, Set<String> activeAbilities, Set<String> newlyObtained) {
        var rows = modifiers.findByCharacterId(characterId);
        var character = get(characterId);
        syncAutomaticModifiers(character, rows, activeAbilities, newlyObtained);
    }

    private void syncAutomaticModifiers(CharacterEntity character, List<CharacterAttributeModifierEntity> rows,
                                        Set<String> activeAbilities, Set<String> newlyObtained) {
        var characterId = character.getId();
        var autoRows = rows.stream().filter(row -> "AUTOMATIC".equals(row.getSource())).toList();
        var activeSupported = activeAbilities.stream().filter(AutomaticAbilityRules::supported).collect(Collectors.toSet());
        autoRows.stream().filter(row -> !activeSupported.contains(row.getName())).forEach(modifiers::delete);

        var baseAttributes = parse(character.getAttributesJson());
        var baseGenetics = parse(character.getGeneticsJson());
        var totals = rows.stream().collect(Collectors.groupingBy(CharacterAttributeModifierEntity::getAttributeKey,
                LinkedHashMap::new, Collectors.summingInt(CharacterAttributeModifierEntity::getValue)));
        var effectiveAttributes = new LinkedHashMap<>(baseAttributes);
        totals.forEach((key, value) -> { if (CharacterRules.ATTRIBUTES.contains(key)) effectiveAttributes.put(key, baseAttributes.getOrDefault(key, 0) + value); });
        var effectiveGenetics = new LinkedHashMap<>(baseGenetics);
        totals.forEach((key, value) -> { if (CharacterRules.GENETICS.contains(key)) effectiveGenetics.put(key, baseGenetics.getOrDefault(key, 0) + value); });
        int dvergr = (int) activeAbilities.stream().filter(name -> name.matches("Fortaleza Dvergr [1-9]|Fortaleza Dvergr 10")).count();

        for (var ability : activeSupported) {
            boolean existing = autoRows.stream().anyMatch(row -> row.getName().equals(ability));
            if (!existing && !newlyObtained.contains(ability)) continue;
            var effects = AutomaticAbilityRules.effects(ability, effectiveAttributes, effectiveGenetics, dvergr);
            for (var effect : effects) upsertAutomatic(characterId, effect.key(), ability, effect.value());
        }

    }

    private void removeAutomaticAbility(String characterId, String ability) {
        modifiers.findByCharacterId(characterId).stream()
                .filter(row -> "AUTOMATIC".equals(row.getSource()) && row.getName().equals(ability))
                .forEach(modifiers::delete);
    }

    private void upsertAutomatic(String characterId, String key, String name, int value) {
        var existing = modifiers.findByCharacterIdAndAttributeKey(characterId, key).stream()
                .filter(row -> "AUTOMATIC".equals(row.getSource()) && row.getName().equals(name)).findFirst();
        if (existing.isPresent()) { existing.get().setValue(value); modifiers.save(existing.get()); }
        else modifiers.save(new CharacterAttributeModifierEntity(UUID.randomUUID().toString(), characterId, key, name, value, "AUTOMATIC"));
    }

    private void ensureGaldr(String characterId, boolean active, boolean newlyObtained) {
        var character = get(characterId);
        if (!active) return;
        if (!newlyObtained && minorDefs.findByCampaignIdAndOwnerCharacterIdAndKey(character.getCampaignId(), characterId, "galdr").isPresent()) return;
        var existing = minorDefs.findByCampaignIdAndOwnerCharacterIdAndKey(character.getCampaignId(), characterId, "galdr");
        if (existing.isPresent()) return;
        var definition = minorDefs.save(new MinorAttributeDefinitionEntity(UUID.randomUUID().toString(), character.getCampaignId(), characterId,
                "galdr", "Galdr", "min(cruzarbifrost,einherjer,sentiryggdrasil)", null, "GALDR"));
        minorValues.save(new CharacterMinorAttributeValueEntity(UUID.randomUUID().toString(), characterId, definition.getId(), 0));
    }

    private static String modifierKey(String attributeKey, String name) {
        return attributeKey + '\u0000' + name;
    }

    private Map<String, List<Map<String, Object>>> modifierView(String characterId) {
        return modifierView(modifiers.findByCharacterId(characterId));
    }

    private Map<String, List<Map<String, Object>>> modifierView(List<CharacterAttributeModifierEntity> rows) {
        var result = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (var modifier : rows) {
            result.computeIfAbsent(modifier.getAttributeKey(), ignored -> new ArrayList<>())
                    .add(Map.of("name", modifier.getName(), "value", modifier.getExactValue(), "source", modifier.getSource()));
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
        return eligibleAbilities(attrs, gen, customMinors, Map.of());
    }

    private AbilityAwards eligibleAbilities(Map<String, Integer> attrs, Map<String, Integer> gen,
                                             Map<String, Integer> customMinors,
                                             List<AbilityEntity> abilityCatalog) {
        return eligibleAbilities(attrs, gen, customMinors, Map.of(), abilityCatalog);
    }

    private AbilityAwards eligibleAbilities(Map<String, Integer> attrs, Map<String, Integer> gen,
                                             Map<String, Integer> customMinors, Map<String, String> decisions) {
        return eligibleAbilities(attrs, gen, customMinors, decisions, abilityCatalog());
    }

    private AbilityAwards eligibleAbilities(Map<String, Integer> attrs, Map<String, Integer> gen,
                                             Map<String, Integer> customMinors, Map<String, String> decisions,
                                             List<AbilityEntity> abilityCatalog) {
        Set<String> obtained = new LinkedHashSet<>();
        Set<String> pendingUnique = new LinkedHashSet<>();
        var values = new LinkedHashMap<String, Integer>(attrs);
        customMinors.forEach(values::putIfAbsent);
        abilityCatalog.forEach(a -> {
            try {
                var alternatives = new ArrayList<JsonNode>();
                json.readTree(a.getAlternativesJson()).forEach(alternatives::add);
                if (AbilityRules.eligible(alternatives, values, gen)) {
                    if (isUnique(a.getUniqueFlag())) {
                        var decision = decisions.get(a.getName());
                        if ("accepted".equals(decision)) obtained.add(a.getName());
                        else if (!"rejected".equals(decision)) pendingUnique.add(a.getName());
                    } else obtained.add(a.getName());
                }
            } catch (Exception e) {
                throw new IllegalStateException("Invalid ability catalog entry: " + a.getName(), e);
            }
        });
        return new AbilityAwards(obtained, pendingUnique);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> abilityState(String id) {
        var character = get(id);
        var decisions = uniqueAbilityDecisions(character);
        Set<String> obtained = abilityStates == null
                ? latestSnapshotValues(id, "abilities")
                : abilityStates.findById(id)
                    .map(state -> new LinkedHashSet<>(state.getObtained()))
                    .orElseGet(() -> new LinkedHashSet<>(latestSnapshotValues(id, "abilities")));
        decisions.forEach((name, decision) -> {
            if ("accepted".equals(decision)) obtained.add(name);
            if ("rejected".equals(decision)) obtained.remove(name);
        });
        var result = new LinkedHashMap<String, Object>();
        result.put("catalog", abilityCatalog());
        result.put("abilities", obtained);
        // Eligibility is a presentation concern now. The director review endpoint
        // remains authoritative and performs the complete server-side calculation.
        result.put("pendingUniqueAbilities", List.of());
        return result;
    }

    private void saveHeavyHistoryParts(MilestoneEntity milestone, String characterId) {
        try {
            if (inventorySnapshots != null && inventoryAggregates != null) {
                var aggregate = inventoryAggregates.findById(characterId)
                        .orElse(new CharacterInventoryAggregateEntity(characterId));
                inventorySnapshots.save(new MilestoneInventorySnapshotEntity(
                        milestone.getId(), characterId, json.writeValueAsString(aggregate)));
            }
            if (activitySnapshots != null && activityAggregates != null) {
                var aggregate = activityAggregates.findById(characterId)
                        .orElse(new CharacterActivityAggregateEntity(characterId));
                activitySnapshots.save(new MilestoneActivitySnapshotEntity(
                        milestone.getId(), characterId, json.writeValueAsString(aggregate)));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not persist complete history snapshot", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> pendingUniqueAbilities(String id) {
        var character = get(id);
        var attrs = parse(character.getAttributesJson());
        var genetics = parse(character.getGeneticsJson());
        var modifierTotals = modifierTotals(id);
        var awards = eligibleAbilities(withModifiers(attrs, modifierTotals), withModifiers(genetics, modifierTotals),
                withModifiers(minorAttributes.values(id), modifierTotals), uniqueAbilityDecisions(character));
        return abilityCatalog().stream().filter(ability -> awards.pendingUnique().contains(ability.getName()))
                .map(this::uniqueAbilityView).toList();
    }

    @Transactional
    public Map<String, Object> decideUniqueAbility(String id, String name, String decision) {
        if (!Set.of("accepted", "rejected").contains(decision)) throw new IllegalArgumentException("Decision must be accepted or rejected");
        var character = get(id);
        if (pendingUniqueAbilities(id).stream().noneMatch(ability -> name.equals(ability.get("name")))) {
            throw new IllegalArgumentException("Unique ability is not pending review: " + name);
        }
        var decisions = new LinkedHashMap<>(uniqueAbilityDecisions(character));
        decisions.put(name, decision);
        try { character.setUniqueAbilityDecisionsJson(json.writeValueAsString(decisions)); }
        catch (Exception e) { throw new IllegalStateException("Could not save unique ability decision", e); }
        characters.save(character);
        if ("accepted".equals(decision)) {
            syncAutomaticModifiers(id, currentActiveAbilities(character), Set.of(name));
            ensureGaldr(id, "Lenguaje Galdr".equals(name), "Lenguaje Galdr".equals(name));
        } else {
            removeAutomaticAbility(id, name);
        }
        return view(id);
    }

    private Set<String> currentActiveAbilities(CharacterEntity character) {
        var attrs = parse(character.getAttributesJson());
        var gen = parse(character.getGeneticsJson());
        var totals = modifierTotals(character.getId());
        var customMinors = minorAttributes.values(character.getId());
        return currentActiveAbilities(character, totals, customMinors, abilityCatalog());
    }

    private Set<String> currentActiveAbilities(CharacterEntity character, Map<String, Integer> totals,
                                                Map<String, Integer> customMinors,
                                                List<AbilityEntity> abilityCatalog) {
        var attrs = parse(character.getAttributesJson());
        var gen = parse(character.getGeneticsJson());
        var awards = eligibleAbilities(withModifiers(attrs, totals), withModifiers(gen, totals),
                withModifiers(customMinors, totals), uniqueAbilityDecisions(character), abilityCatalog);
        var result = new LinkedHashSet<>(awards.obtained());
        uniqueAbilityDecisions(character).forEach((ability, value) -> { if ("accepted".equals(value)) result.add(ability); });
        return result;
    }

    private Map<String, Object> uniqueAbilityView(AbilityEntity ability) {
        try {
            var result = new LinkedHashMap<String, Object>();
            result.put("name", ability.getName()); result.put("description", Objects.toString(ability.getDescription(), ""));
            result.put("launchType", Objects.toString(ability.getLaunchType(), "")); result.put("cost", ability.getCost());
            var alternatives = json.readTree(ability.getAlternativesJson());
            result.put("test", alternatives.isArray() && alternatives.size() > 0 ? alternatives.get(0).path("Prueba").asText("") : "");
            result.put("requirements", json.convertValue(alternatives, Object.class));
            return result;
        } catch (Exception e) { throw new IllegalStateException("Unique ability requirements are invalid", e); }
    }

    private Map<String, String> uniqueAbilityDecisions(CharacterEntity character) {
        try {
            var result = new LinkedHashMap<String, String>();
            json.readTree(Objects.toString(character.getUniqueAbilityDecisionsJson(), "{}")).fields()
                    .forEachRemaining(entry -> result.put(entry.getKey(), entry.getValue().asText()));
            return result;
        } catch (Exception e) { return Map.of(); }
    }

    private Map<String, Integer> withModifiers(Map<String, Integer> values, Map<String, Integer> modifierTotals) {
        var result = new LinkedHashMap<>(values);
        result.replaceAll((key, value) -> value + modifierTotals.getOrDefault(key, 0));
        modifierTotals.forEach((key, value) -> result.putIfAbsent(key, value));
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

    public List<Map<String, Object>> milestones(String id) {
        get(id);
        return milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc(id).stream().map(this::milestoneView).toList();
    }

    @Transactional
    public Map<String, Object> cancelChanges(String id) {
        var character = get(id);
        var latest = milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc(id).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No closed version exists to restore"));
        restoreSnapshot(character, latest);
        character.setClosed(true);
        character.touch();
        characters.save(character);
        return Map.of("character", view(id));
    }

    @Transactional
    public Map<String, Object> recover(String id, String milestoneId) {
        var character = get(id);
        var target = milestones.findById(milestoneId)
                .filter(m -> m.isVisible() && id.equals(m.getCharacterId()))
                .orElseThrow(() -> new NoSuchElementException("Closed version not found"));
        restoreSnapshot(character, target);
        character.setClosed(true);
        character.touch();
        characters.save(character);
        var snapshot = snapshotFromCurrent(character, id, snapshot(target));
        var recovered = new MilestoneEntity(UUID.randomUUID().toString(), id, character.getLevel(), character.getExperience(),
                writeJson(snapshot), target.getNewBonusesJson(), target.getNewAbilitiesJson(), true);
        milestones.save(recovered);
        saveHeavyHistoryParts(recovered, id);
        return Map.of("character", view(id), "milestone", milestoneView(recovered));
    }

    private Map<String, Object> milestoneView(MilestoneEntity milestone) {
        var result = new LinkedHashMap<String, Object>();
        result.put("id", milestone.getId());
        result.put("level", milestone.getLevel());
        result.put("experience", milestone.getExperience());
        result.put("createdAt", milestone.getCreatedAt());
        result.put("snapshot", snapshot(milestone));
        return result;
    }

    private Map<String, Map<String, Object>> derivedStatsView(Map<String, CharacterRules.DerivedStat> stats,
                                                               Map<String, List<Map<String, Object>>> modifierView) {
        var result = new LinkedHashMap<String, Map<String, Object>>();
        stats.forEach((key, stat) -> {
            var modifiers = modifierView.getOrDefault(key, List.of());
            result.put(key, Map.of("key", stat.key(), "name", stat.name(), "formula", stat.formula(),
                    "baseValue", stat.baseValue(), "total", stat.total(), "modifiers", modifiers));
        });
        return result;
    }

    private void restoreSnapshot(CharacterEntity character, MilestoneEntity milestone) {
        var node = snapshot(milestone);
        if (node.has("name")) character.setName(node.path("name").asText(character.getName()));
        if (node.has("level")) character.setLevel(node.path("level").asInt(character.getLevel()));
        if (node.has("experience")) character.setExperience(node.path("experience").asInt(character.getExperience()));
        if (node.has("attributes")) character.setAttributesJson(writeJson(json.convertValue(node.get("attributes"), Map.class)));
        if (node.has("genetics")) character.setGeneticsJson(writeJson(json.convertValue(node.get("genetics"), Map.class)));
        if (node.has("evolutionPoints")) character.setEvolutionPoints(node.path("evolutionPoints").asInt(character.getEvolutionPoints()));
        if (node.has("geneticsPoints")) character.setGeneticsPoints(node.path("geneticsPoints").asInt(character.getGeneticsPoints()));
        if (node.has("imageUrl")) character.setImageUrl(node.get("imageUrl").isNull() ? null : node.get("imageUrl").asText());
        if (node.has("uniqueAbilityDecisions")) character.setUniqueAbilityDecisionsJson(writeJson(json.convertValue(node.get("uniqueAbilityDecisions"), Map.class)));
        if (node.has("minorAttributes")) {
            var custom = json.convertValue(node.get("minorAttributes"), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>() {});
            persistCustomMinorRanks(character, custom);
        }
        if (node.has("modifiers")) {
            restoreModifiers(character.getId(), node.path("modifiers"));
        }
        if (abilityStates != null && node.has("abilities")) {
            var state = abilityStates.findById(character.getId()).orElse(new CharacterAbilityStateEntity(character.getId()));
            state.setObtained(json.convertValue(node.get("abilities"), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
            state.setPendingUnique(node.has("pendingUniqueAbilities")
                    ? json.convertValue(node.get("pendingUniqueAbilities"), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
                    : List.of());
            state.setSourceMilestoneId(milestone.getId());
            abilityStates.save(state);
        }
        restoreHeavyHistoryParts(milestone, character.getId());
    }

    private void restoreHeavyHistoryParts(MilestoneEntity milestone, String characterId) {
        try {
            if (inventorySnapshots != null && inventoryAggregates != null) {
                inventorySnapshots.findById(milestone.getId()).ifPresent(snapshot -> {
                    try {
                        var aggregate = json.readValue(snapshot.getSnapshotJson(), CharacterInventoryAggregateEntity.class);
                        aggregate.setId(characterId);
                        aggregate.setCharacterId(characterId);
                        inventoryAggregates.save(aggregate);
                    } catch (Exception e) { throw new IllegalStateException("Invalid inventory history snapshot", e); }
                });
            }
            if (activitySnapshots != null && activityAggregates != null) {
                activitySnapshots.findById(milestone.getId()).ifPresent(snapshot -> {
                    try {
                        var aggregate = json.readValue(snapshot.getSnapshotJson(), CharacterActivityAggregateEntity.class);
                        aggregate.setId(characterId);
                        aggregate.setCharacterId(characterId);
                        activityAggregates.save(aggregate);
                    } catch (Exception e) { throw new IllegalStateException("Invalid activity history snapshot", e); }
                });
            }
        } catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Could not restore complete history snapshot", e); }
    }

    private void restoreModifiers(String characterId, JsonNode requested) {
        var desired = new LinkedHashMap<String, RequestedModifier>();
        requested.fields().forEachRemaining(attribute -> attribute.getValue().forEach(modifier -> {
            var name = modifier.path("name").asText("").trim();
            if (!name.isBlank()) desired.put(attribute.getKey() + '\u0000' + name,
                    new RequestedModifier(attribute.getKey(), modifier.path("value").asInt(), modifier.path("source").asText("MANUAL")));
        }));
        var existing = modifiers.findByCharacterId(characterId).stream().collect(Collectors.toMap(
                modifier -> modifier.getAttributeKey() + '\u0000' + modifier.getName(), modifier -> modifier,
                (first, ignored) -> first, LinkedHashMap::new));
        existing.forEach((key, modifier) -> {
            var requestedModifier = desired.remove(key);
            if (requestedModifier == null) modifiers.delete(modifier);
            else if (modifier.getValue() != requestedModifier.value()) {
                modifier.setValue(requestedModifier.value());
                modifiers.save(modifier);
            } else if (!Objects.equals(modifier.getSource(), requestedModifier.source())) {
                modifier.setSource(requestedModifier.source());
                modifiers.save(modifier);
            }
        });
        desired.forEach((key, requestedModifier) -> {
            var separator = key.indexOf('\u0000');
            modifiers.save(new CharacterAttributeModifierEntity(UUID.randomUUID().toString(), characterId,
                    requestedModifier.key(), key.substring(separator + 1), requestedModifier.value(), requestedModifier.source()));
        });
    }

    private record RequestedModifier(String key, int value, String source) {}

    private Map<String, Object> snapshotFromCurrent(CharacterEntity character, String id, JsonNode source) {
        var result = new LinkedHashMap<String, Object>();
        result.put("name", character.getName()); result.put("experience", character.getExperience()); result.put("level", character.getLevel());
        result.put("attributes", parse(character.getAttributesJson())); result.put("genetics", parse(character.getGeneticsJson()));
        result.put("minorAttributes", minorAttributes.values(id)); result.put("modifiers", modifierSnapshot(id));
        result.put("evolutionPoints", character.getEvolutionPoints()); result.put("geneticsPoints", character.getGeneticsPoints());
        result.put("imageUrl", character.getImageUrl()); result.put("uniqueAbilityDecisions", uniqueAbilityDecisions(character));
        result.put("abilities", snapshotSet(source, "abilities")); result.put("pendingUniqueAbilities", snapshotSet(source, "pendingUniqueAbilities")); result.put("visible", true);
        return result;
    }

    private Set<String> snapshotSet(JsonNode source, String field) {
        var result = new LinkedHashSet<String>();
        source.path(field).forEach(value -> result.add(value.asText()));
        return result;
    }

    private String writeJson(Object value) { try { return json.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("Could not serialize character snapshot", e); } }
    @Transactional(readOnly = true)
    public Map<String, Object> lastUpgrade(String id) {
        var character = get(id);
        var closedVersions = milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc(id);
        if (closedVersions.size() < 2) return Map.of("available", false);
        var current = closedVersions.getFirst(); var previous = closedVersions.get(1);
        var currentAbilities = snapshotAbilities(current);
        uniqueAbilityDecisions(character).forEach((name, decision) -> { if ("accepted".equals(decision)) currentAbilities.add(name); });
        return compareUpgrade(snapshot(current), snapshot(previous), current.getLevel(), current.getExperience(), currentAbilities, current.getCreatedAt(), previous.getLevel(), previous.getExperience(), previous.getCreatedAt(), snapshotAbilities(previous));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> currentUpgrade(String id) {
        var character = get(id);
        var closedVersions = milestones.findByCharacterIdAndVisibleTrueOrderByCreatedAtDesc(id);
        if (closedVersions.isEmpty()) return Map.of("available", false);
        var previous = closedVersions.getFirst();
            var currentAttributes = parse(character.getAttributesJson());
            var currentModifierRows = currentModifierRows(character, id);
            var currentModifierTotals = modifierTotals(currentModifierRows);
            var currentAttributeTotals = withModifiers(currentAttributes, currentModifierTotals);
            var currentGenetics = parse(character.getGeneticsJson());
        var currentMinorAttributes = minorAttributes.values(id);
        var abilityCatalog = abilityCatalog();
        var currentAwards = eligibleAbilities(withModifiers(currentAttributes, currentModifierTotals), withModifiers(currentGenetics, currentModifierTotals),
                withModifiers(currentMinorAttributes, currentModifierTotals), Map.of(), abilityCatalog);
        var currentAbilities = new LinkedHashSet<String>(currentAwards.obtained());
        currentAbilities.addAll(currentAwards.pendingUnique());
        var currentSnapshot = json.valueToTree(Map.of(
                "attributes", currentAttributes,
                "genetics", currentGenetics,
                "minorAttributes", currentMinorAttributes,
                "modifiers", modifierSnapshot(currentModifierRows),
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
        return Map.of("available", true, "current", Map.of("level", currentLevel, "closedAt", currentClosedAt), "previous", Map.of("level", previousLevel, "closedAt", previousClosedAt), "scores", scores, "bonuses", bonuses, "modifiers", modifierChanges(currentSnapshot, previousSnapshot), "abilities", abilities);
    }

    private JsonNode snapshot(MilestoneEntity milestone) { try { return json.readTree(milestone.getSnapshotJson()); } catch (Exception e) { throw new IllegalStateException("Closed version snapshot is invalid", e); } }
    private Map<String, List<Map<String, Object>>> modifierSnapshot(String characterId) {
        return modifierSnapshot(modifiers.findByCharacterId(characterId));
    }

    private List<CharacterAttributeModifierEntity> currentModifierRows(CharacterEntity character, String characterId) {
        if (character.getAggregateVersion() > 0 && character.getModifiers() != null) {
            return new ArrayList<>(character.getModifiers());
        }
        return modifiers.findByCharacterId(characterId);
    }

    private Map<String, List<Map<String, Object>>> modifierSnapshot(List<CharacterAttributeModifierEntity> rows) {
        var result = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (var modifier : rows) {
            result.computeIfAbsent(modifier.getAttributeKey(), ignored -> new ArrayList<>())
                    .add(Map.of("name", modifier.getName(), "value", modifier.getExactValue(), "source", modifier.getSource()));
        }
        return result;
    }
    private Map<String, Integer> snapshotModifierValues(JsonNode snapshot) {
        var result = new LinkedHashMap<String, Integer>();
        snapshot.path("modifiers").fields().forEachRemaining(attribute -> attribute.getValue().forEach(modifier -> {
            var name = modifier.path("name").asText("");
            if (!name.isBlank()) result.put(attribute.getKey() + '\u0000' + name, modifier.path("value").asInt());
        }));
        return result;
    }
    private List<Map<String, Object>> modifierChanges(JsonNode currentSnapshot, JsonNode previousSnapshot) {
        var current = snapshotModifierValues(currentSnapshot); var previous = snapshotModifierValues(previousSnapshot);
        var keys = new LinkedHashSet<String>(); keys.addAll(current.keySet()); keys.addAll(previous.keySet());
        var changes = new ArrayList<Map<String, Object>>();
        for (var composite : keys) {
            if (Objects.equals(current.get(composite), previous.get(composite))) continue;
            var separator = composite.indexOf('\u0000');
            var change = new LinkedHashMap<String, Object>();
            change.put("key", composite.substring(0, separator)); change.put("name", composite.substring(separator + 1));
            change.put("before", previous.get(composite)); change.put("after", current.get(composite));
            changes.add(change);
        }
        return changes;
    }
    private Map<String, Integer> snapshotRanks(JsonNode snapshot, String field) { var result = new LinkedHashMap<String, Integer>(); snapshot.path(field).fields().forEachRemaining(entry -> result.put(entry.getKey(), entry.getValue().asInt())); return result; }
    private void appendScoreChanges(List<Map<String, Object>> changes, String type, Map<String, Integer> current, Map<String, Integer> previous) { var keys = new LinkedHashSet<String>(); keys.addAll(current.keySet()); keys.addAll(previous.keySet()); for (var key : keys) { int before = previous.getOrDefault(key, 0), after = current.getOrDefault(key, 0), increase = after - before; if (increase > 0) changes.add(Map.of("key", key, "type", type, "before", before, "after", after, "increase", increase)); } }

    private static String validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        if (imageUrl.startsWith("data:")) {
            if (imageUrl.length() > MAX_IMAGE_DATA_URL_LENGTH)
                throw new IllegalArgumentException("El retrato supera el límite seguro de 150 KB");
            if (!imageUrl.matches("data:image/(jpeg|webp);base64,[A-Za-z0-9+/=]+"))
                throw new IllegalArgumentException("El retrato debe ser un JPEG o WebP procesado");
        } else {
            if (imageUrl.length() > MAX_EXTERNAL_IMAGE_URL_LENGTH)
                throw new IllegalArgumentException("La URL del retrato supera los 2048 caracteres");
            final var uri = tryParseUri(imageUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null)
                throw new IllegalArgumentException("La URL externa del retrato debe usar HTTPS");
        }
        return imageUrl;
    }

    private static URI tryParseUri(String imageUrl) {
        try { return URI.create(imageUrl); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("La URL externa del retrato no es válida"); }
    }

    private static Map<String, Integer> parse(String value) {
        try { return new ObjectMapper().readValue(value, Map.class); }
        catch (Exception e) { return new LinkedHashMap<>(); }
    }

    private List<String> parseList(String value) {
        try { return json.readValue(value, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}); }
        catch (Exception e) { return List.of(); }
    }
}
