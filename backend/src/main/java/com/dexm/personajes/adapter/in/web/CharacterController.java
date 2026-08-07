package com.dexm.personajes.adapter.in.web;

import com.dexm.personajes.application.CharacterService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {
    private final CharacterService service;

    public CharacterController(CharacterService service) { this.service = service; }

    public record CreateRequest(@NotBlank String name) {}

    public record AllocationRequest(
            @NotNull @Min(1) Integer level,
            @NotNull @Min(0) Integer experience,
            Map<String, Integer> attributes,
            Map<String, Integer> genetics,
            Map<String, Integer> minorAttributes,
            Boolean visible,
            @JsonProperty("final") Boolean finalStep) {}

    public record SaveRequest(
            @NotBlank String name,
            @Min(1) Integer level,
            @Min(0) int experience,
            Map<String, Integer> attributes,
            Map<String, Integer> genetics,
            Map<String, Integer> minorAttributes,
            Boolean visible,
            @JsonProperty("final") Boolean finalStep) {}

    public record ModifierRequest(@NotBlank String name, @NotNull Integer value) {}

    public record PreviewRequest(@Min(0) int experience, Map<String, Integer> attributes, Map<String, Integer> genetics) {}
    public record ExperienceRequest(@NotNull @Min(1) Integer amount) {}

    @GetMapping
    public List<?> list() { return service.list(); }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.name()));
    }

    @GetMapping("/{id}")
    public Object get(@PathVariable String id) { return service.view(id); }

    @PostMapping("/{id}/edit")
    public Object edit(@PathVariable String id) { return service.beginEdit(id); }

    @PostMapping("/{id}/experience")
    public Object addExperience(@PathVariable String id, @Valid @RequestBody ExperienceRequest request) {
        return service.addExperience(id, request.amount());
    }

    @PostMapping("/{id}/level-up")
    public Object levelUp(@PathVariable String id, @Valid @RequestBody AllocationRequest request) {
        return service.levelUp(id, request.level(), request.experience(), values(request.attributes()),
                values(request.genetics()), values(request.minorAttributes()),
                Boolean.TRUE.equals(request.visible()), Boolean.TRUE.equals(request.finalStep()));
    }

    @PostMapping("/{id}/level-up-all")
    public Object levelUpAll(@PathVariable String id, @Valid @RequestBody AllocationRequest request) {
        return service.levelUpAll(id, request.level(), request.experience(), values(request.attributes()),
                values(request.genetics()), values(request.minorAttributes()),
                Boolean.TRUE.equals(request.visible()), Boolean.TRUE.equals(request.finalStep()));
    }

    @GetMapping("/{id}/attributes/{key}")
    public AttributeDetailDto attributeDetail(@PathVariable String id, @PathVariable String key) {
        return service.attributeDetail(id, key);
    }

    @PutMapping("/{id}/attribute-modifiers")
    public Object saveAttributeModifiers(@PathVariable String id,
                                          @RequestBody Map<String, List<ModifierRequest>> request) {
        return service.saveAttributeModifiers(id, request);
    }

    @DeleteMapping("/{id}/minor-attributes/{definitionId}")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMinorAttribute(@PathVariable String id, @PathVariable String definitionId) {
        service.deleteCustomMinorAttribute(id, definitionId);
    }

    @PutMapping("/{id}")
    public Object save(@PathVariable String id, @Valid @RequestBody SaveRequest request) {
        return service.save(id, request.name(), request.level(), request.experience(), values(request.attributes()),
                values(request.genetics()), values(request.minorAttributes()),
                Boolean.TRUE.equals(request.visible()), Boolean.TRUE.equals(request.finalStep()));
    }

    @PostMapping("/{id}/preview")
    public Object preview(@PathVariable String id, @Valid @RequestBody PreviewRequest request) {
        service.get(id);
        return service.preview(request.experience(), values(request.attributes()), values(request.genetics()));
    }

    @GetMapping("/{id}/milestones")
    public Object milestones(@PathVariable String id) { return service.milestones(id); }

    @GetMapping("/{id}/last-upgrade")
    public Object lastUpgrade(@PathVariable String id) { return service.lastUpgrade(id); }

    @GetMapping("/{id}/current-upgrade")
    public Object currentUpgrade(@PathVariable String id) { return service.currentUpgrade(id); }

    private static Map<String, Integer> values(Map<String, Integer> values) { return values == null ? Map.of() : values; }
}
