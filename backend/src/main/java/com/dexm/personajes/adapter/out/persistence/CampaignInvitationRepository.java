package com.dexm.personajes.adapter.out.persistence;
import java.util.*;
public interface CampaignInvitationRepository extends FirestoreRepository<CampaignInvitationEntity>{ List<CampaignInvitationEntity> findByCampaignIdOrderByEmailAsc(String campaignId); Optional<CampaignInvitationEntity> findByCampaignIdAndEmail(String campaignId,String email); boolean existsByCampaignIdAndEmailAndRevokedAtIsNull(String campaignId,String email); boolean existsByEmailAndRevokedAtIsNull(String email); }
