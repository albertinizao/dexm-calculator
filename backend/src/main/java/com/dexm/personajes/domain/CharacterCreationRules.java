package com.dexm.personajes.domain;

import java.util.*;

/** Rules for the one-time character creation configuration. */
public final class CharacterCreationRules {
    public static final String EMPTY_MODE = "empty";
    public static final String GUIDED_MODE = "guided";
    public static final String MIDGARD_HUMAN = "Humano de Midgard";
    public static final List<String> MAJOR_ATTRIBUTES = List.of("fisico", "agilidad", "percepcion", "mente", "estudio", "carisma");
    public static final List<String> WIZARD_STATES = List.of("empty", "started", "race", "majors", "einherjer", "complete");
    public static final List<String> EINHERJER_ORIGINS = List.of("converted", "born_human", "born_einherjer");

    private CharacterCreationRules() {}

    public record Configuration(String mode, String race, Boolean einherjer, Boolean awakened,
                                List<String> selectedMajorAttributes, String wizardState,
                                String einherjerOrigin, Integer startingAge, Integer awakeningAge, Integer sheetAge) {
        public Configuration(String mode, String race, Boolean einherjer, Boolean awakened, List<String> selectedMajorAttributes, String wizardState) {
            this(mode, race, einherjer, awakened, selectedMajorAttributes, wizardState, null, null, null, null);
        }
        public Configuration {
            selectedMajorAttributes = selectedMajorAttributes == null ? List.of() : List.copyOf(selectedMajorAttributes);
        }
    }

    public static void validate(Configuration configuration) {
        if (configuration == null || configuration.mode() == null) throw new IllegalArgumentException("Creation mode is required");
        var selected = configuration.selectedMajorAttributes();
        if (selected.stream().distinct().count() != selected.size()) throw new IllegalArgumentException("Selected major attributes cannot contain duplicates");
        if (selected.stream().anyMatch(key -> !MAJOR_ATTRIBUTES.contains(key))) throw new IllegalArgumentException("Unknown selected major attribute");
        if (!selected.isEmpty() && selected.size() != 2) throw new IllegalArgumentException("Exactly 2 major attributes must be selected");
        if (configuration.einherjer() != null && configuration.einherjer() != Boolean.TRUE) throw new IllegalArgumentException("Todos los personajes deben ser Einherjer");
        if (configuration.einherjerOrigin() != null && !EINHERJER_ORIGINS.contains(configuration.einherjerOrigin())) throw new IllegalArgumentException("Origen Einherjer no válido");
        if (configuration.awakened() == Boolean.TRUE && configuration.awakeningAge() == null) throw new IllegalArgumentException("La edad de despertar es obligatoria");
        if (configuration.awakened() != Boolean.TRUE && configuration.awakeningAge() != null) throw new IllegalArgumentException("La edad de despertar solo aplica a personajes despertados");
        if (configuration.sheetAge() != null && configuration.sheetAge() < 0) throw new IllegalArgumentException("La edad actual no puede ser negativa");
        if ("complete".equals(configuration.wizardState()) && configuration.startingAge() != null) {
            TrainingRules.validateProfile(configuration.startingAge(), configuration.awakeningAge(), configuration.sheetAge(),
                    Boolean.TRUE.equals(configuration.einherjer()), configuration.einherjerOrigin());
        }

        if (EMPTY_MODE.equals(configuration.mode())) {
            require("complete".equals(configuration.wizardState()) && configuration.einherjer() == Boolean.TRUE
                    && configuration.awakened() != null && configuration.einherjerOrigin() != null && configuration.sheetAge() != null,
                    "La creación vacía necesita completar los datos Einherjer");
            if (configuration.race() != null || !selected.isEmpty()) throw new IllegalArgumentException("Empty creation cannot contain guided answers");
            if (configuration.sheetAge() < 0) throw new IllegalArgumentException("La edad actual no puede ser negativa");
            return;
        }
        if (!GUIDED_MODE.equals(configuration.mode())) throw new IllegalArgumentException("Invalid creation mode");
        if (!WIZARD_STATES.contains(configuration.wizardState()) || EMPTY_MODE.equals(configuration.wizardState())) {
            throw new IllegalArgumentException("Invalid creation wizard state");
        }
        if (configuration.race() != null && !MIDGARD_HUMAN.equals(configuration.race())) {
            throw new IllegalArgumentException("Invalid race");
        }
        switch (configuration.wizardState()) {
            case "started" -> require(!hasRace(configuration) && selected.isEmpty() && configuration.einherjer() == null && configuration.awakened() == null,
                    "Guided creation has invalid started state");
            case "race" -> require(hasRace(configuration) && selected.isEmpty() && configuration.einherjer() == null && configuration.awakened() == null,
                    "Guided creation has invalid race state");
            case "majors" -> require(hasRace(configuration) && selected.size() == 2 && configuration.einherjer() == null && configuration.awakened() == null,
                    "Guided creation has invalid major attributes state");
            case "einherjer" -> require(hasRace(configuration) && selected.size() == 2 && configuration.einherjer() == null && configuration.awakened() == null,
                    "Guided creation has invalid Einherjer state");
            case "complete" -> require(hasRace(configuration) && selected.size() == 2 && configuration.einherjer() == Boolean.TRUE
                            && configuration.awakened() != null && configuration.einherjerOrigin() != null && configuration.sheetAge() != null,
                    "Guided creation has invalid completed state");
            default -> throw new IllegalArgumentException("Invalid creation wizard state");
        }
    }

    public static Map<String, Integer> attributesFor(Configuration configuration) {
        validate(configuration);
        var result = CharacterRules.zeroValues(CharacterRules.ATTRIBUTES);
        if (GUIDED_MODE.equals(configuration.mode())) {
            configuration.selectedMajorAttributes().forEach(key -> result.put(key, 1));
            if (configuration.awakened() == Boolean.TRUE) MAJOR_ATTRIBUTES.forEach(key -> result.put(key, result.get(key) + 3));
        }
        return result;
    }

    private static boolean hasRace(Configuration configuration) { return MIDGARD_HUMAN.equals(configuration.race()); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
}
