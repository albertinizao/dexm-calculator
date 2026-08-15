package com.dexm.personajes.adapter.out.persistence.firestore;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import com.dexm.personajes.adapter.out.persistence.*;

/**
 * The Google client resolves Application Default Credentials on Cloud Run and
 * honours FIRESTORE_EMULATOR_HOST automatically for local development. No key
 * file or GOOGLE_APPLICATION_CREDENTIALS value is configured by this project.
 */
@Configuration
@Profile({"firestore", "local-firestore", "test"})
public class FirestoreConfiguration {
    @Bean(destroyMethod = "close")
    Firestore firestore(@Value("${app.firestore.project-id:}") String projectId) {
        var options = projectId == null || projectId.isBlank()
                ? FirestoreOptions.getDefaultInstance()
                : FirestoreOptions.newBuilder().setProjectId(projectId).build();
        return options.getService();
    }

    @Bean AbilityRepository abilityRepository(FirestoreRepositoryFactory f){ return f.create(AbilityRepository.class, AbilityEntity.class, "abilities"); }
    @Bean AmmunitionRepository ammunitionRepository(FirestoreRepositoryFactory f){ return f.create(AmmunitionRepository.class, AmmunitionEntity.class, "ammunition"); }
    @Bean GrenadeCatalogRepository grenadeCatalogRepository(FirestoreRepositoryFactory f){ return f.create(GrenadeCatalogRepository.class, GrenadeCatalogEntity.class, "grenadeCatalog"); }
    @Bean ArmorCatalogRepository armorCatalogRepository(FirestoreRepositoryFactory f){ return f.create(ArmorCatalogRepository.class, ArmorCatalogEntity.class, "armorCatalog"); }
    @Bean ArmorRepository armorRepository(FirestoreRepositoryFactory f){ return f.create(ArmorRepository.class, ArmorEntity.class, "armors"); }
    @Bean CampaignRepository campaignRepository(FirestoreRepositoryFactory f){ return f.create(CampaignRepository.class, CampaignEntity.class, "campaigns"); }
    @Bean CampaignInvitationRepository campaignInvitationRepository(FirestoreRepositoryFactory f){ return f.create(CampaignInvitationRepository.class, CampaignInvitationEntity.class, "campaignInvitations"); }
    @Bean CharacterRepository characterRepository(FirestoreRepositoryFactory f){ return f.create(CharacterRepository.class, CharacterEntity.class, "characters"); }
    @Bean CharacterInventoryAggregateRepository characterInventoryAggregateRepository(FirestoreRepositoryFactory f){ return f.create(CharacterInventoryAggregateRepository.class, CharacterInventoryAggregateEntity.class, "characterInventories"); }
    @Bean CharacterActivityAggregateRepository characterActivityAggregateRepository(FirestoreRepositoryFactory f){ return f.create(CharacterActivityAggregateRepository.class, CharacterActivityAggregateEntity.class, "characterActivities"); }
    @Bean CharacterAbilityStateRepository characterAbilityStateRepository(FirestoreRepositoryFactory f){ return f.create(CharacterAbilityStateRepository.class, CharacterAbilityStateEntity.class, "characterAbilities"); }
    @Bean MilestoneInventorySnapshotRepository milestoneInventorySnapshotRepository(FirestoreRepositoryFactory f){ return f.create(MilestoneInventorySnapshotRepository.class, MilestoneInventorySnapshotEntity.class, "milestoneInventorySnapshots"); }
    @Bean MilestoneActivitySnapshotRepository milestoneActivitySnapshotRepository(FirestoreRepositoryFactory f){ return f.create(MilestoneActivitySnapshotRepository.class, MilestoneActivitySnapshotEntity.class, "milestoneActivitySnapshots"); }
    @Bean CharacterAttributeModifierRepository modifierRepository(FirestoreRepositoryFactory f){ return f.create(CharacterAttributeModifierRepository.class, CharacterAttributeModifierEntity.class, "attributeModifiers"); }
    @Bean CharacterMinorAttributeValueRepository minorValueRepository(FirestoreRepositoryFactory f){ return f.create(CharacterMinorAttributeValueRepository.class, CharacterMinorAttributeValueEntity.class, "minorAttributeValues"); }
    @Bean MilestoneRepository milestoneRepository(FirestoreRepositoryFactory f){ return f.create(MilestoneRepository.class, MilestoneEntity.class, "milestones"); }
    @Bean MinorAttributeDefinitionRepository minorDefinitionRepository(FirestoreRepositoryFactory f){ return f.create(MinorAttributeDefinitionRepository.class, MinorAttributeDefinitionEntity.class, "minorAttributeDefinitions"); }
    @Bean OtherInventoryItemRepository otherInventoryRepository(FirestoreRepositoryFactory f){ return f.create(OtherInventoryItemRepository.class, OtherInventoryItemEntity.class, "otherInventoryItems"); }
    @Bean PhysicalShieldRepository physicalShieldRepository(FirestoreRepositoryFactory f){ return f.create(PhysicalShieldRepository.class, PhysicalShieldEntity.class, "physicalShields"); }
    @Bean ShieldCatalogRepository shieldCatalogRepository(FirestoreRepositoryFactory f){ return f.create(ShieldCatalogRepository.class, ShieldCatalogEntity.class, "shieldCatalog"); }
    @Bean ShieldRepository shieldRepository(FirestoreRepositoryFactory f){ return f.create(ShieldRepository.class, ShieldEntity.class, "shields"); }
    @Bean TrainingActivityRepository trainingRepository(FirestoreRepositoryFactory f){ return f.create(TrainingActivityRepository.class, TrainingActivityEntity.class, "trainingActivities"); }
    @Bean UserRepository userRepository(FirestoreRepositoryFactory f){ return f.create(UserRepository.class, UserEntity.class, "users"); }
    @Bean WeaponCatalogRepository weaponCatalogRepository(FirestoreRepositoryFactory f){ return f.create(WeaponCatalogRepository.class, WeaponCatalogEntity.class, "weaponCatalog"); }
    @Bean WeaponRepository weaponRepository(FirestoreRepositoryFactory f){ return f.create(WeaponRepository.class, WeaponEntity.class, "weapons"); }
}
