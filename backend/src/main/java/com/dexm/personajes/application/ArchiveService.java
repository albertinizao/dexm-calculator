package com.dexm.personajes.application;

import com.dexm.personajes.adapter.out.persistence.*;
import com.dexm.personajes.security.AuthorizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Versioned, portable game-data archives. Images and access-control data never enter this boundary. */
@Service
public class ArchiveService {
    public static final int FORMAT_VERSION = 1;
    private final ObjectMapper json = new ObjectMapper();
    private final CampaignRepository campaigns;
    private final CharacterRepository characters;
    private final MinorAttributeDefinitionRepository definitions;
    private final CharacterMinorAttributeValueRepository minorValues;
    private final CharacterAttributeModifierRepository modifiers;
    private final TrainingActivityRepository training;
    private final OtherInventoryItemRepository others;
    private final WeaponRepository weapons;
    private final AmmunitionRepository ammunition;
    private final GrenadeCatalogRepository grenadeCatalog;
    private final ArmorRepository armor;
    private final ShieldRepository shields;
    private final PhysicalShieldRepository physicalShields;
    private final MilestoneRepository milestones;
    private final AuthorizationService authorization;

    public ArchiveService(CampaignRepository campaigns, CharacterRepository characters, MinorAttributeDefinitionRepository definitions,
                          CharacterMinorAttributeValueRepository minorValues, CharacterAttributeModifierRepository modifiers,
                          TrainingActivityRepository training, OtherInventoryItemRepository others, WeaponRepository weapons,
                          AmmunitionRepository ammunition, ArmorRepository armor, ShieldRepository shields,
                          PhysicalShieldRepository physicalShields, MilestoneRepository milestones, AuthorizationService authorization) {
        this(campaigns, characters, definitions, minorValues, modifiers, training, others, weapons, ammunition, null,
                armor, shields, physicalShields, milestones, authorization);
    }

    @Autowired
    public ArchiveService(CampaignRepository campaigns, CharacterRepository characters, MinorAttributeDefinitionRepository definitions,
                          CharacterMinorAttributeValueRepository minorValues, CharacterAttributeModifierRepository modifiers,
                          TrainingActivityRepository training, OtherInventoryItemRepository others, WeaponRepository weapons,
                          AmmunitionRepository ammunition, GrenadeCatalogRepository grenadeCatalog, ArmorRepository armor,
                          ShieldRepository shields, PhysicalShieldRepository physicalShields, MilestoneRepository milestones,
                          AuthorizationService authorization) {
        this.campaigns = campaigns;
        this.characters = characters;
        this.definitions = definitions;
        this.minorValues = minorValues;
        this.modifiers = modifiers;
        this.training = training;
        this.others = others;
        this.weapons = weapons;
        this.ammunition = ammunition;
        this.grenadeCatalog = grenadeCatalog;
        this.armor = armor;
        this.shields = shields;
        this.physicalShields = physicalShields;
        this.milestones = milestones;
        this.authorization = authorization;
    }

    public Archive exportCharacter(String id) {
        requireCharacterWrite(id);
        return new Archive(FORMAT_VERSION, "character", Instant.now().toString(), null,
                snapshot(characters.findById(id).orElseThrow()));
    }

    public Archive exportCampaign(String id) {
        requireAdmin();
        var campaign = campaigns.findById(id).orElseThrow();
        return new Archive(FORMAT_VERSION, "campaign", Instant.now().toString(),
                new CampaignData(campaign.getName(), definitions.findByCampaignIdOrderByNameAsc(id).stream().map(this::definition).toList(),
                        characters.findByCampaignIdOrderByNameAsc(id).stream().map(this::snapshot).toList()), null);
    }

    @Transactional
    public void importCharacter(String destinationId, String payload) {
        requireCharacterWrite(destinationId);
        var archive = parse(payload, "character");
        replaceCharacter(characters.findById(destinationId).orElseThrow(), archive.character());
    }

    @Transactional
    public void importCampaign(String destinationId, String payload) {
        requireAdmin();
        var archive = parse(payload, "campaign");
        var target = campaigns.findById(destinationId).orElseThrow();
        target.setName(archive.campaign().name());
        campaigns.save(target);

        var existingBySource = new HashMap<String, CharacterEntity>();
        for (var candidate : characters.findByCampaignIdOrderByNameAsc(destinationId)) {
            existingBySource.put(candidate.getImportSourceId() == null ? candidate.getId() : candidate.getImportSourceId(), candidate);
        }
        var importedCharacters = new LinkedHashMap<String, CharacterEntity>();
        for (CharacterData source : archive.campaign().characters()) {
            var targetCharacter = existingBySource.get(source.sourceId());
            if (targetCharacter == null) {
                targetCharacter = new CharacterEntity(UUID.randomUUID().toString(), destinationId, source.name(), null,
                        source.experience(), source.level(), source.attributesJson(), source.geneticsJson());
                targetCharacter.setImportSourceId(source.sourceId());
                characters.save(targetCharacter);
            }
            importedCharacters.put(source.sourceId(), targetCharacter);
        }
        var definitionIds = new HashMap<String, String>();
        for (DefinitionData source : archive.campaign().definitions()) {
            var existing = definitions.findByCampaignIdAndKey(destinationId, source.key()).orElse(null);
            var targetId = existing == null ? UUID.randomUUID().toString() : existing.getId();
            definitionIds.put(source.id(), targetId);
            if (existing == null) {
                var owner = source.ownerCharacterId() == null ? null
                        : Optional.ofNullable(importedCharacters.get(source.ownerCharacterId())).map(CharacterEntity::getId).orElse(null);
                definitions.save(new MinorAttributeDefinitionEntity(targetId, destinationId, owner, source.key(), source.name(),
                        source.maxFormula(), source.bonusSource(), source.type()));
            }
        }
        for (CharacterData source : archive.campaign().characters()) {
            replaceCharacter(importedCharacters.get(source.sourceId()), source, definitionIds);
        }
    }

    private Archive parse(String payload, String type) {
        try {
            var archive = json.readValue(payload, Archive.class);
            if (archive.version() != FORMAT_VERSION) throw new IllegalArgumentException("La versión del fichero no es compatible");
            if (!type.equals(archive.type())) throw new IllegalArgumentException("El fichero no corresponde a un/a "
                    + ("campaign".equals(type) ? "campaña" : "personaje"));
            if (("character".equals(type) && archive.character() == null) || ("campaign".equals(type) && archive.campaign() == null))
                throw new IllegalArgumentException("El fichero está incompleto");
            return archive;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("El fichero no es un archivo DEXM válido");
        }
    }

    private void requireAdmin() { authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication()); }
    private void requireCharacterWrite(String id) { authorization.requireCharacter(SecurityContextHolder.getContext().getAuthentication(), id, true); }

    private CharacterData snapshot(CharacterEntity character) {
        return new CharacterData(character.getImportSourceId() == null ? character.getId() : character.getImportSourceId(), character.getName(),
                character.getExperience(), character.getLevel(), character.getEvolutionPoints(), character.getGeneticsPoints(),
                character.getUniqueAbilityDecisionsJson(), character.isClosed(), character.getAttributesJson(), character.getGeneticsJson(),
                character.getCreationMode(), character.getRace(), character.isEinherjer(), character.isAwakened(), character.getEinherjerOrigin(),
                character.getStartingAge(), character.getAwakeningAge(), character.getSheetAge(), character.getSelectedMajorAttributesJson(),
                character.getCreationWizardState(),
                minorValues.findByCharacterId(character.getId()).stream().map(v -> new MinorValueData(v.getDefinitionId(), v.getValue())).toList(),
                modifiers.findByCharacterId(character.getId()).stream().map(m -> new ModifierData(m.getAttributeKey(), m.getName(), m.getExactValue(), m.getSource())).toList(),
                training.findByCharacterIdOrderByStartAgeAscPriorityAsc(character.getId()).stream().map(t -> new TrainingData(t.getType(), t.getName(), t.getStartAge(), t.getEndAge(), t.getPriority(), t.getPrimaryAttribute(), t.getSecondaryAttribute(), t.getTertiaryAttribute(), t.isConcurrent())).toList(),
                others.findByCharacterIdOrderByNameAsc(character.getId()).stream().map(o -> new OtherData(o.getName(), o.getDescription(), o.getLocation(), o.getQuantity(), o.getUnitValue())).toList(),
                weapons.findByCharacterIdOrderBySlotAsc(character.getId()).stream().map(w -> new WeaponData(w.getSlot(), w.getName(), w.getWeaponType(), w.getSize(), w.getRange(), w.getReload(), w.getRate(), w.getDamageVital(), w.getDamageNormal(), w.getDamageLight(), w.getDamageVeryLight(), w.getAim(), w.getAutomaticFire(), w.getCapacity(), w.getLoadedBullets(), w.getCaliber(), w.getExtraRule(), w.getCatalogWeaponId())).toList(),
                ammunition.findByCharacterIdOrderByCaliberAsc(character.getId()).stream().map(this::ammo).toList(),
                armor.findByCharacterIdOrderByNameAsc(character.getId()).stream().map(a -> new ArmorData(a.getName(), a.getDescription(), a.getSlotsJson())).toList(),
                shields.findByCharacterIdOrderByNameAsc(character.getId()).stream().map(s -> new ShieldData(s.getName(), s.getDescription(), s.getHitPoints())).toList(),
                physicalShields.findByCharacterIdOrderByNameAsc(character.getId()).stream().map(s -> new PhysicalShieldData(s.getName(), s.getDescription(), s.getRd(), s.getArmor(), s.getDefense(), s.getOtherEffects())).toList(),
                // Export needs every milestone; sorting in memory avoids requiring
                // the characterId + createdAt composite Firestore index.
                milestones.findByCharacterId(character.getId()).stream()
                        .sorted(Comparator.comparing(MilestoneEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(m -> new MilestoneData(m.getLevel(), m.getExperience(), m.getSnapshotJson(), m.getNewBonusesJson(), m.getNewAbilitiesJson(), m.isVisible())).toList());
    }

    private AmmoData ammo(AmmunitionEntity item) {
        var type = item.getType() == null ? "CALIBER" : item.getType();
        var grenade = "GRENADE".equals(type) && grenadeCatalog != null && item.getGrenadeCatalogId() != null
                ? grenadeCatalog.findById(item.getGrenadeCatalogId()).map(this::grenade).orElse(null) : null;
        return new AmmoData(type, item.getCaliber(), item.getGrenadeCatalogId(), item.getQuantity(), grenade);
    }

    private GrenadeData grenade(GrenadeCatalogEntity item) {
        return new GrenadeData(item.getId(), item.getName(), item.getDescription(), item.getCentralDamage(), item.getAdjacentDamage(),
                item.getDamageDecay(), item.isHandGrenade(), item.getType(), item.isOfficial());
    }

    private DefinitionData definition(MinorAttributeDefinitionEntity definition) {
        return new DefinitionData(definition.getId(), definition.getOwnerCharacterId(), definition.getKey(), definition.getName(),
                definition.getMaxFormula(), definition.getBonusSource(), definition.getType());
    }

    private void replaceCharacter(CharacterEntity target, CharacterData source) { replaceCharacter(target, source, Map.of()); }

    private void replaceCharacter(CharacterEntity target, CharacterData source, Map<String, String> definitionIds) {
        target.setName(source.name()); target.setExperience(source.experience()); target.setLevel(source.level());
        target.setEvolutionPoints(source.evolutionPoints()); target.setGeneticsPoints(source.geneticsPoints());
        target.setUniqueAbilityDecisionsJson(source.uniqueAbilityDecisionsJson()); target.setClosed(source.closed());
        target.setAttributesJson(source.attributesJson()); target.setGeneticsJson(source.geneticsJson()); target.setCreationMode(source.creationMode());
        target.setRace(source.race()); target.setEinherjer(source.einherjer()); target.setAwakened(source.awakened());
        target.setEinherjerOrigin(source.einherjerOrigin()); target.setStartingAge(source.startingAge()); target.setAwakeningAge(source.awakeningAge());
        target.setSheetAge(source.sheetAge()); target.setSelectedMajorAttributesJson(source.selectedMajorAttributesJson());
        target.setCreationWizardState(source.creationWizardState()); target.setImportSourceId(source.sourceId()); target.touch(); characters.save(target);
        String id = target.getId();
        minorValues.deleteAll(minorValues.findByCharacterId(id)); modifiers.deleteByCharacterId(id); training.deleteByCharacterId(id);
        others.deleteByCharacterId(id); weapons.deleteAll(weapons.findByCharacterIdOrderBySlotAsc(id));
        ammunition.deleteAll(ammunition.findByCharacterIdOrderByCaliberAsc(id)); armor.deleteAll(armor.findByCharacterIdOrderByNameAsc(id));
        shields.deleteAll(shields.findByCharacterIdOrderByNameAsc(id)); physicalShields.deleteAll(physicalShields.findByCharacterIdOrderByNameAsc(id));
        milestones.deleteByCharacterId(id);
        for (var value : source.minorValues()) minorValues.save(new CharacterMinorAttributeValueEntity(UUID.randomUUID().toString(), id, definitionIds.getOrDefault(value.definitionId(), value.definitionId()), value.value()));
        for (var modifier : source.modifiers()) modifiers.save(new CharacterAttributeModifierEntity(UUID.randomUUID().toString(), id, modifier.attributeKey(), modifier.name(), modifier.value(), modifier.source()));
        for (var activity : source.training()) training.save(new TrainingActivityEntity(UUID.randomUUID().toString(), id, activity.type(), activity.name(), activity.startAge(), activity.endAge(), activity.priority(), activity.primary(), activity.secondary(), activity.tertiary(), activity.concurrent()));
        for (var other : source.others()) others.save(new OtherInventoryItemEntity(UUID.randomUUID().toString(), id, other.name(), other.description(), other.location(), other.quantity(), other.unitValue()));
        for (var weapon : source.weapons()) weapons.save(new WeaponEntity(UUID.randomUUID().toString(), id, weapon.slot(), weapon.name(), weapon.weaponType(), weapon.size(), weapon.range(), weapon.reload(), weapon.rate(), weapon.damageVital(), weapon.damageNormal(), weapon.damageLight(), weapon.damageVeryLight(), weapon.aim(), weapon.automaticFire(), weapon.capacity(), weapon.loadedBullets(), weapon.caliber(), weapon.extraRule(), weapon.catalogWeaponId(), null));
        for (var ammo : source.ammunition()) importAmmunition(id, ammo);
        for (var item : source.armor()) armor.save(new ArmorEntity(UUID.randomUUID().toString(), id, item.name(), item.description(), item.slotsJson(), null));
        for (var shield : source.shields()) shields.save(new ShieldEntity(UUID.randomUUID().toString(), id, shield.name(), shield.description(), shield.hitPoints(), null));
        for (var shield : source.physicalShields()) physicalShields.save(new PhysicalShieldEntity(UUID.randomUUID().toString(), id, shield.name(), shield.description(), shield.rd(), shield.armor(), shield.defense(), shield.otherEffects(), null));
        for (var milestone : source.milestones()) milestones.save(new MilestoneEntity(UUID.randomUUID().toString(), id, milestone.level(), milestone.experience(), milestone.snapshotJson(), milestone.newBonusesJson(), milestone.newAbilitiesJson(), milestone.visible()));
    }

    private void importAmmunition(String characterId, AmmoData source) {
        var type = "GRENADE".equalsIgnoreCase(source.type()) ? "GRENADE" : "CALIBER";
        if ("GRENADE".equals(type)) {
            var catalogId = clean(source.grenadeCatalogId());
            if (catalogId == null) throw new IllegalArgumentException("La granada archivada no tiene catálogo asociado");
            ensureGrenadeCatalog(source.grenade(), catalogId);
            ammunition.save(new AmmunitionEntity(UUID.randomUUID().toString(), characterId, type, null, catalogId, source.quantity()));
            return;
        }
        ammunition.save(new AmmunitionEntity(UUID.randomUUID().toString(), characterId, type, clean(source.caliber()), null, source.quantity()));
    }

    private void ensureGrenadeCatalog(GrenadeData data, String catalogId) {
        if (grenadeCatalog == null) throw new IllegalStateException("El catálogo de granadas no está disponible");
        if (grenadeCatalog.findById(catalogId).isPresent()) return;
        if (data == null) throw new IllegalArgumentException("El catálogo de la granada archivada no está disponible");
        grenadeCatalog.save(new GrenadeCatalogEntity(catalogId, data.name(), data.description(), data.centralDamage(), data.adjacentDamage(),
                data.damageDecay(), data.handGrenade(), data.type(), data.official()));
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record Archive(int version, String type, String exportedAt, CampaignData campaign, CharacterData character) {}
    public record CampaignData(String name, List<DefinitionData> definitions, List<CharacterData> characters) {}
    public record DefinitionData(String id, String ownerCharacterId, String key, String name, String maxFormula, String bonusSource, String type) {}
    public record CharacterData(String sourceId, String name, int experience, int level, int evolutionPoints, int geneticsPoints, String uniqueAbilityDecisionsJson, boolean closed, String attributesJson, String geneticsJson, String creationMode, String race, boolean einherjer, boolean awakened, String einherjerOrigin, Integer startingAge, Integer awakeningAge, Integer sheetAge, String selectedMajorAttributesJson, String creationWizardState, List<MinorValueData> minorValues, List<ModifierData> modifiers, List<TrainingData> training, List<OtherData> others, List<WeaponData> weapons, List<AmmoData> ammunition, List<ArmorData> armor, List<ShieldData> shields, List<PhysicalShieldData> physicalShields, List<MilestoneData> milestones) {}
    public record MinorValueData(String definitionId, int value) {}
    public record ModifierData(String attributeKey, String name, BigDecimal value, String source) {}
    public record TrainingData(String type, String name, int startAge, int endAge, int priority, String primary, String secondary, String tertiary, boolean concurrent) {}
    public record OtherData(String name, String description, String location, int quantity, BigDecimal unitValue) {}
    public record WeaponData(String slot, String name, String weaponType, String size, BigDecimal range, BigDecimal reload, String rate, BigDecimal damageVital, BigDecimal damageNormal, BigDecimal damageLight, BigDecimal damageVeryLight, BigDecimal aim, String automaticFire, BigDecimal capacity, BigDecimal loadedBullets, String caliber, String extraRule, String catalogWeaponId) {}
    public record AmmoData(String type, String caliber, String grenadeCatalogId, int quantity, GrenadeData grenade) {}
    public record GrenadeData(String id, String name, String description, int centralDamage, int adjacentDamage, int damageDecay, boolean handGrenade, String type, boolean official) {}
    public record ArmorData(String name, String description, String slotsJson) {}
    public record ShieldData(String name, String description, int hitPoints) {}
    public record PhysicalShieldData(String name, String description, int rd, int armor, int defense, String otherEffects) {}
    public record MilestoneData(int level, int experience, String snapshotJson, String newBonusesJson, String newAbilitiesJson, boolean visible) {}
}
