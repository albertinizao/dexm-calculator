package com.dexm.personajes.adapter.in.web;

import java.util.List;

/** Common response used by major, predefined minor and custom minor attributes. */
public record AttributeDetailDto(
        String key,
        String definitionId,
        String name,
        String type,
        int total,
        int ranks,
        Integer maxRanks,
        String formula,
        int calculatedValue,
        int plusOne,
        int plusD6,
        List<ModifierDto> modifiers,
        List<ProgressionDto> progressions,
        boolean deletable
) {
    public record ModifierDto(String name, int value) {}

    public record ProgressionDto(String kind, int number, int threshold, boolean obtained) {}
}
