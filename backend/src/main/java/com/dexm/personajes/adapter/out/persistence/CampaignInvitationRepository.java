package com.dexm.personajes.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CampaignInvitationRepository extends JpaRepository<CampaignInvitationEntity,String>{ List<CampaignInvitationEntity> findByCampaignIdOrderByEmailAsc(String campaignId); Optional<CampaignInvitationEntity> findByCampaignIdAndEmail(String campaignId,String email); boolean existsByCampaignIdAndEmailAndRevokedAtIsNull(String campaignId,String email); boolean existsByEmailAndRevokedAtIsNull(String email); }
