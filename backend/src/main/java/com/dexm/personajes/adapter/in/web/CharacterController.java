package com.dexm.personajes.adapter.in.web;

import com.dexm.personajes.application.CharacterService;
import com.dexm.personajes.application.OtherInventoryService;
import com.dexm.personajes.application.WeaponInventoryService;
import com.dexm.personajes.domain.CharacterCreationRules;
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
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {
    private final CharacterService service;
    private final OtherInventoryService otherInventory;
    private final WeaponInventoryService weaponInventory;

    public CharacterController(CharacterService service, OtherInventoryService otherInventory, WeaponInventoryService weaponInventory) { this.service = service; this.otherInventory = otherInventory; this.weaponInventory = weaponInventory; }

    public record CreateRequest(@NotBlank String name) {}

    public record CreationConfigurationRequest(@NotBlank String mode, String race, Boolean einherjer, Boolean awakened,
                                                String einherjerOrigin, Integer startingAge, Integer awakeningAge, Integer sheetAge,
                                                List<String> selectedMajorAttributes, @NotBlank String wizardState) {}

    public record TrainingActivityRequest(@NotBlank String type, @NotBlank String name, @NotNull @Min(0) Integer startAge,
                                          @NotNull @Min(1) Integer endAge, @Min(0) Integer priority, String primaryAttribute,
                                          String secondaryAttribute, String tertiaryAttribute, Boolean concurrent) {}
    public record TrainingPreviewRequest(@NotNull @Valid TrainingActivityRequest activity, String replacingActivityId) {}
    public record TrainingReorderRequest(@NotNull List<String> activityIds) {}

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
            @JsonProperty("final") Boolean finalStep,
            Integer evolutionPoints,
            Boolean einherjer,
            Boolean awakened,
            String einherjerOrigin,
            Integer startingAge,
            Integer awakeningAge,
            Integer sheetAge) {}

    public record LegacyRequest(@NotBlank String code) {}

    public record ModifierRequest(@NotBlank String name, @NotNull Integer value) {}

    public record PreviewRequest(@Min(0) int experience, Map<String, Integer> attributes, Map<String, Integer> genetics) {}
    public record ExperienceRequest(@NotNull @Min(1) Integer amount) {}
    public record UniqueAbilityDecisionRequest(@NotBlank String decision) {}
    public record OtherInventoryItemRequest(@NotBlank String name, String description, String location,
                                             @NotNull @Min(1) Integer quantity,
                                             @DecimalMin("0.0") BigDecimal unitValue) {}
    public record WeaponRequest(@NotBlank String slot, @NotBlank String name, @NotBlank String weaponType, @NotBlank String size,
                                @NotNull @DecimalMin("0.0") BigDecimal range, @NotNull @DecimalMin("0.0") BigDecimal reload,
                                @NotBlank String rate, @NotNull @DecimalMin("0.0") BigDecimal damageVital,
                                @NotNull @DecimalMin("0.0") BigDecimal damageNormal, @NotNull @DecimalMin("0.0") BigDecimal damageLight,
                                @NotNull @DecimalMin("0.0") BigDecimal damageVeryLight, BigDecimal aim,
                                String automaticFire, @NotNull @DecimalMin("0.0") BigDecimal capacity, @NotBlank String caliber,
                                String extraRule) {}
    public record WeaponCatalogCreateRequest(@NotBlank String name, @NotBlank String weaponType, @NotBlank String size,
                                @NotNull @DecimalMin("0.0") BigDecimal range, @NotNull @DecimalMin("0.0") BigDecimal reload,
                                @NotBlank String rate, @NotNull @DecimalMin("0.0") BigDecimal damageVital, @NotNull @DecimalMin("0.0") BigDecimal damageNormal,
                                @NotNull @DecimalMin("0.0") BigDecimal damageLight, @NotNull @DecimalMin("0.0") BigDecimal damageVeryLight,
                                BigDecimal aim, String automaticFire, @NotNull @DecimalMin("0.0") BigDecimal capacity, @NotBlank String caliber,
                                String extraRule, String imageUrl) {}
    public record WeaponCatalogCopyRequest(@NotBlank String slot) {}

    @GetMapping
    public List<?> list() { return service.list(); }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.name()));
    }

    @PostMapping("/{id}/creation")
    public Object configureCreation(@PathVariable String id, @Valid @RequestBody CreationConfigurationRequest request) {
        return service.configureCreation(id, new CharacterCreationRules.Configuration(request.mode(), request.race(),
                request.einherjer(), request.awakened(), request.selectedMajorAttributes(), request.wizardState(),
                request.einherjerOrigin(), request.startingAge(), request.awakeningAge(), request.sheetAge()));
    }

    @GetMapping("/{id}/training")
    public Object training(@PathVariable String id) { return service.training(id); }

    @GetMapping("/{id}/inventory/others")
    public Object otherInventory(@PathVariable String id) { return otherInventory.list(id); }

    @PostMapping("/{id}/inventory/others")
    public ResponseEntity<?> createOtherInventory(@PathVariable String id, @Valid @RequestBody OtherInventoryItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(otherInventory.create(id, request));
    }

    @GetMapping("/{id}/inventory/others/{itemId}")
    public Object getOtherInventory(@PathVariable String id, @PathVariable String itemId) { return otherInventory.get(id, itemId); }

    @PutMapping("/{id}/inventory/others/{itemId}")
    public Object updateOtherInventory(@PathVariable String id, @PathVariable String itemId,
                                       @Valid @RequestBody OtherInventoryItemRequest request) {
        return otherInventory.update(id, itemId, request);
    }

    @DeleteMapping("/{id}/inventory/others/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOtherInventory(@PathVariable String id, @PathVariable String itemId) { otherInventory.delete(id, itemId); }

    @GetMapping("/{id}/inventory/weapons")
    public Object weaponInventory(@PathVariable String id) { return weaponInventory.list(id); }
    @PostMapping("/{id}/inventory/weapons")
    public ResponseEntity<?> createWeapon(@PathVariable String id, @Valid @RequestBody WeaponRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(weaponInventory.create(id, request)); }
    @PutMapping("/{id}/inventory/weapons/{weaponId}")
    public Object updateWeapon(@PathVariable String id, @PathVariable String weaponId, @Valid @RequestBody WeaponRequest request) { return weaponInventory.update(id, weaponId, request); }
    @DeleteMapping("/{id}/inventory/weapons/{weaponId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWeapon(@PathVariable String id, @PathVariable String weaponId) { weaponInventory.delete(id, weaponId); }
    @PostMapping("/{id}/inventory/weapons/{weaponId}/move")
    public Object moveWeapon(@PathVariable String id, @PathVariable String weaponId, @RequestBody Map<String,String> request) { return weaponInventory.move(id, weaponId, request.get("slot")); }

    @PostMapping("/{id}/training/preview")
    public Object previewTraining(@PathVariable String id, @Valid @RequestBody TrainingPreviewRequest request) {
        return service.previewTraining(id, request.activity(), request.replacingActivityId());
    }

    @PostMapping("/{id}/training")
    public Object addTraining(@PathVariable String id, @Valid @RequestBody TrainingActivityRequest request) {
        return service.addTraining(id, request);
    }

    @PostMapping("/{id}/training/reorder")
    public Object reorderTraining(@PathVariable String id, @Valid @RequestBody TrainingReorderRequest request) {
        return service.reorderTraining(id, request.activityIds());
    }

    @PutMapping("/{id}/training/{activityId}")
    public Object updateTraining(@PathVariable String id, @PathVariable String activityId, @Valid @RequestBody TrainingActivityRequest request) {
        return service.updateTraining(id, activityId, request);
    }

    @DeleteMapping("/{id}/training/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTraining(@PathVariable String id, @PathVariable String activityId) { service.deleteTraining(id, activityId); }

    @GetMapping("/{id}")
    public Object get(@PathVariable String id) { return service.view(id); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) { service.delete(id); }

    @GetMapping("/{id}/unique-abilities/pending")
    public Object pendingUniqueAbilities(@PathVariable String id) { return service.pendingUniqueAbilities(id); }

    @PostMapping("/{id}/unique-abilities/{name}/decision")
    public Object decideUniqueAbility(@PathVariable String id, @PathVariable String name, @Valid @RequestBody UniqueAbilityDecisionRequest request) {
        return service.decideUniqueAbility(id, name, request.decision());
    }

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
                Boolean.TRUE.equals(request.visible()), Boolean.TRUE.equals(request.finalStep()), request.evolutionPoints(),
                request.einherjer(), request.awakened(), request.einherjerOrigin(), request.startingAge(), request.awakeningAge(), request.sheetAge());
    }

    @PostMapping("/{id}/legacy/import")
    public Object importLegacy(@PathVariable String id, @Valid @RequestBody LegacyRequest request) {
        service.get(id);
        return service.importLegacy(request.code());
    }

    @GetMapping("/{id}/legacy/export")
    public String exportLegacy(@PathVariable String id) { return service.exportLegacy(id); }

    @PostMapping("/{id}/preview")
    public Object preview(@PathVariable String id, @Valid @RequestBody PreviewRequest request) {
        service.get(id);
        return service.preview(request.experience(), values(request.attributes()), values(request.genetics()));
    }

    @GetMapping("/{id}/milestones")
    public Object milestones(@PathVariable String id) { return service.milestones(id); }

    @PostMapping("/{id}/cancel-changes")
    public Object cancelChanges(@PathVariable String id) { return service.cancelChanges(id); }

    @PostMapping("/{id}/history/{milestoneId}/recover")
    public Object recover(@PathVariable String id, @PathVariable String milestoneId) { return service.recover(id, milestoneId); }

    @GetMapping("/{id}/last-upgrade")
    public Object lastUpgrade(@PathVariable String id) { return service.lastUpgrade(id); }

    @GetMapping("/{id}/current-upgrade")
    public Object currentUpgrade(@PathVariable String id) { return service.currentUpgrade(id); }

    private static Map<String, Integer> values(Map<String, Integer> values) { return values == null ? Map.of() : values; }
}
