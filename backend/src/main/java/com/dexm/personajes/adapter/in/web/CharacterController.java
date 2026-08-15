package com.dexm.personajes.adapter.in.web;

import com.dexm.personajes.application.CharacterService;
import com.dexm.personajes.application.ArchiveService;
import com.dexm.personajes.application.OtherInventoryService;
import com.dexm.personajes.application.WeaponInventoryService;
import com.dexm.personajes.application.ProtectiveEquipmentService;
import com.dexm.personajes.application.AmmunitionInventoryService;
import com.dexm.personajes.domain.CharacterCreationRules;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import com.dexm.personajes.security.AuthorizationService;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {
    private final CharacterService service;
    private final ArchiveService archives;
    private final OtherInventoryService otherInventory;
    private final WeaponInventoryService weaponInventory; private final ProtectiveEquipmentService protective; private final AmmunitionInventoryService ammunition; private final AuthorizationService authorization;

    public CharacterController(CharacterService service, ArchiveService archives, OtherInventoryService otherInventory, WeaponInventoryService weaponInventory, ProtectiveEquipmentService protective, AmmunitionInventoryService ammunition, AuthorizationService authorization) { this.service = service; this.archives = archives; this.otherInventory = otherInventory; this.weaponInventory = weaponInventory; this.protective = protective; this.ammunition = ammunition; this.authorization = authorization; }

    public record CreateRequest(@NotBlank String name) {}

    public record CreationConfigurationRequest(@NotBlank String mode, String race, Boolean einherjer, Boolean awakened,
                                                String einherjerOrigin, Integer startingAge, Integer awakeningAge, Integer sheetAge,
                                                List<String> selectedMajorAttributes, @NotBlank String wizardState) {}

    public record TrainingActivityRequest(@NotBlank String type, @NotBlank String name, @NotNull @Min(0) Integer startAge,
                                          @NotNull @Min(1) Integer endAge, @Min(0) Integer priority, String primaryAttribute,
                                          String secondaryAttribute, String tertiaryAttribute, Boolean concurrent) {}
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
            Integer sheetAge,
            JsonNode imageUrl) {}

    public record LegacyRequest(@NotBlank String code) {}
    public record EditorRequest(@NotBlank @Email String email) {}

    public record ModifierRequest(@NotBlank String name, @NotNull Integer value) {}

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
                                String automaticFire, @NotNull @DecimalMin("0.0") BigDecimal capacity,
                                @Digits(integer = 12, fraction = 0) @DecimalMin("0.0") BigDecimal loadedBullets,
                                String caliber,
                                String extraRule) {}
    public record WeaponCatalogCreateRequest(@NotBlank String name, @NotBlank String weaponType, @NotBlank String size,
                                @NotNull @DecimalMin("0.0") BigDecimal range, @NotNull @DecimalMin("0.0") BigDecimal reload,
                                @NotBlank String rate, @NotNull @DecimalMin("0.0") BigDecimal damageVital, @NotNull @DecimalMin("0.0") BigDecimal damageNormal,
                                @NotNull @DecimalMin("0.0") BigDecimal damageLight, @NotNull @DecimalMin("0.0") BigDecimal damageVeryLight,
                                BigDecimal aim, String automaticFire, @NotNull @DecimalMin("0.0") BigDecimal capacity, String caliber,
                                String extraRule, String imageUrl) {}
    public record WeaponCatalogCopyRequest(@NotBlank String slot) {}
    public record ArmorRequest(@NotBlank String name, String description, @NotNull Map<String, Map<String, Integer>> slots, String imageUrl) {}
    public record ShieldRequest(@NotBlank String name, String description, @NotNull @Min(0) Integer hitPoints, String imageUrl) {}
    public record PhysicalShieldRequest(@NotBlank String name, String description, @NotNull @Min(0) Integer rd, @NotNull @Min(0) Integer armor, @NotNull Integer defense, String otherEffects, String imageUrl) {}
    public record AmmunitionRequest(String caliber, @NotNull @Min(1) Integer quantity, String type, String grenadeCatalogId) {
        public AmmunitionRequest(String caliber, Integer quantity) { this(caliber, quantity, "CALIBER", null); }
    }
    public record AmmunitionDecrementRequest(Integer amount) {}

    @GetMapping
    public List<?> list() { return service.list(); }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest request) {
        authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication());
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

    @GetMapping("/{id}/inventory")
    public Object inventory(@PathVariable String id) {
        var result = new LinkedHashMap<String, Object>();
        result.put("others", otherInventory.list(id));
        result.put("weapons", weaponInventory.list(id));
        result.put("ammunition", ammunition.list(id));
        result.put("armors", protective.listArmors(id));
        result.put("shields", protective.listShields(id));
        result.put("physicalShields", protective.listPhysicalShields(id));
        return result;
    }
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
    @PostMapping("/{id}/inventory/weapons/{weaponId}/reload")
    public Object reloadWeapon(@PathVariable String id, @PathVariable String weaponId) { return ammunition.reload(id, weaponId); }
    @PostMapping("/{id}/inventory/weapons/{weaponId}/shoot")
    public Object shootWeapon(@PathVariable String id, @PathVariable String weaponId, @RequestBody Map<String, Object> request) {
        return weaponInventory.shoot(id, weaponId, shootCommand(request));
    }

    private WeaponInventoryService.ShootCommand shootCommand(Map<String, Object> request) {
        if (request == null) throw new IllegalArgumentException("El disparo es obligatorio");
        Object modeValue = request.get("mode");
        String mode = modeValue == null ? null : String.valueOf(modeValue);
        Object shotsNode = request.containsKey("shots") ? request.get("shots")
                : request.containsKey("count") ? request.get("count")
                : request.get("requested");
        Integer shots = null;
        if (shotsNode != null) {
            if (!(shotsNode instanceof Number number) || number.doubleValue() % 1 != 0) {
                throw new IllegalArgumentException("La cantidad de disparos debe ser un entero");
            }
            shots = number.intValue();
        }
        if (mode == null && shots != null) mode = "normal";
        return new WeaponInventoryService.ShootCommand(mode, shots);
    }

    @GetMapping("/{id}/inventory/ammunition")
    public Object ammunition(@PathVariable String id) { return ammunition.list(id); }
    @GetMapping("/{id}/inventory/ammunition/calibers")
    public Object ammunitionCalibers(@PathVariable String id) { return ammunition.calibers(id); }
    @PostMapping("/{id}/inventory/ammunition")
    public ResponseEntity<?> createAmmunition(@PathVariable String id, @Valid @RequestBody AmmunitionRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ammunition.create(id, request)); }
    @PutMapping("/{id}/inventory/ammunition/{ammunitionId}")
    public Object updateAmmunition(@PathVariable String id, @PathVariable String ammunitionId, @Valid @RequestBody AmmunitionRequest request) { return ammunition.update(id, ammunitionId, request); }
    @PostMapping("/{id}/inventory/ammunition/{ammunitionId}/decrement")
    public ResponseEntity<?> decrementAmmunition(@PathVariable String id, @PathVariable String ammunitionId, @RequestBody AmmunitionDecrementRequest request) {
        var result = ammunition.decrement(id, ammunitionId, request == null ? null : request.amount());
        return result == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
    }
    @PostMapping("/{id}/inventory/ammunition/{ammunitionId}/consume")
    public Object consumeGrenade(@PathVariable String id, @PathVariable String ammunitionId) { return ammunition.consumeGrenade(id, ammunitionId); }
    @PostMapping("/{id}/inventory/grenades/{grenadeCatalogId}/launch")
    public Object launchGrenade(@PathVariable String id, @PathVariable String grenadeCatalogId) { return ammunition.consumeGrenadeByCatalog(id, grenadeCatalogId); }

    @GetMapping("/{id}/inventory/armors") public Object armors(@PathVariable String id){return protective.listArmors(id);}
    @PostMapping("/{id}/inventory/armors") public ResponseEntity<?> createArmor(@PathVariable String id,@Valid @RequestBody ArmorRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(protective.createArmor(id,r));}
    @PutMapping("/{id}/inventory/armors/{armorId}") public Object updateArmor(@PathVariable String id,@PathVariable String armorId,@Valid @RequestBody ArmorRequest r){return protective.updateArmor(id,armorId,r);}
    @DeleteMapping("/{id}/inventory/armors/{armorId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteArmor(@PathVariable String id,@PathVariable String armorId){protective.deleteArmor(id,armorId);}
    @GetMapping("/{id}/inventory/shields") public Object shields(@PathVariable String id){return protective.listShields(id);}
    @PostMapping("/{id}/inventory/shields") public ResponseEntity<?> createShield(@PathVariable String id,@Valid @RequestBody ShieldRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(protective.createShield(id,r));}
    @PutMapping("/{id}/inventory/shields/{shieldId}") public Object updateShield(@PathVariable String id,@PathVariable String shieldId,@Valid @RequestBody ShieldRequest r){return protective.updateShield(id,shieldId,r);}
    @DeleteMapping("/{id}/inventory/shields/{shieldId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteShield(@PathVariable String id,@PathVariable String shieldId){protective.deleteShield(id,shieldId);}
    @GetMapping("/{id}/inventory/physical-shields") public Object physicalShields(@PathVariable String id){return protective.listPhysicalShields(id);}
    @PostMapping("/{id}/inventory/physical-shields") public ResponseEntity<?> createPhysicalShield(@PathVariable String id,@Valid @RequestBody PhysicalShieldRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(protective.createPhysicalShield(id,r));}
    @PutMapping("/{id}/inventory/physical-shields/{shieldId}") public Object updatePhysicalShield(@PathVariable String id,@PathVariable String shieldId,@Valid @RequestBody PhysicalShieldRequest r){return protective.updatePhysicalShield(id,shieldId,r);}
    @DeleteMapping("/{id}/inventory/physical-shields/{shieldId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deletePhysicalShield(@PathVariable String id,@PathVariable String shieldId){protective.deletePhysicalShield(id,shieldId);}

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

    @GetMapping("/{id}/abilities")
    public Object abilities(@PathVariable String id) { return service.abilityState(id); }
    @GetMapping("/{id}/unique-abilities/pending")
    public Object pendingUniqueAbilities(@PathVariable String id) {
        authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication());
        return service.pendingUniqueAbilities(id);
    }

    @PostMapping("/{id}/unique-abilities/{name}/decision")
    public Object decideUniqueAbility(@PathVariable String id, @PathVariable String name, @Valid @RequestBody UniqueAbilityDecisionRequest request) {
        authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication());
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
                request.einherjer(), request.awakened(), request.einherjerOrigin(), request.startingAge(), request.awakeningAge(), request.sheetAge(),
                request.imageUrl() != null,
                request.imageUrl() == null || request.imageUrl().isNull() ? null : request.imageUrl().asText());
    }

    @GetMapping("/{id}/archive")
    public Object exportArchive(@PathVariable String id) { return archives.exportCharacter(id); }

    @PostMapping("/{id}/archive/import")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void importArchive(@PathVariable String id, @RequestBody String payload) { archives.importCharacter(id, payload); }

    @PostMapping("/{id}/legacy/import")
    public Object importLegacy(@PathVariable String id, @Valid @RequestBody LegacyRequest request) {
        service.get(id);
        return service.importLegacy(request.code());
    }

    @GetMapping("/{id}/legacy/export")
    public String exportLegacy(@PathVariable String id) { return service.exportLegacy(id); }

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

    @GetMapping("/{id}/editors")
    public List<String> editors(@PathVariable String id) {
        authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication());
        return service.editors(id);
    }

    @PostMapping("/{id}/editors")
    public List<String> addEditor(@PathVariable String id, @Valid @RequestBody EditorRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        authorization.requireCharacterEditorEmail(auth, id, request.email());
        return service.addEditor(id, request.email());
    }

    @DeleteMapping("/{id}/editors/{email}")
    public List<String> removeEditor(@PathVariable String id, @PathVariable String email) {
        authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication());
        return service.removeEditor(id, email);
    }

    private static Map<String, Integer> values(Map<String, Integer> values) { return values == null ? Map.of() : values; }
}
