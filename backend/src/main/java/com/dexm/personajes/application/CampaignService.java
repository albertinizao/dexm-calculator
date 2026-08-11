package com.dexm.personajes.application;
import com.dexm.personajes.adapter.out.persistence.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.*;
import org.springframework.beans.factory.annotation.Autowired; import org.springframework.security.core.context.SecurityContextHolder;
import com.dexm.personajes.security.AuthorizationService; import com.dexm.personajes.security.SecurityIdentityService;
@Service public class CampaignService {
 private final CampaignRepository campaigns; private final CharacterService characters;
 @Autowired private AuthorizationService authorization; @Autowired private CampaignInvitationRepository invitations;
 @Autowired private SecurityIdentityService identities;
 public CampaignService(CampaignRepository campaigns,CharacterService characters){this.campaigns=campaigns;this.characters=characters;}
 public List<CampaignEntity> list(){var all=campaigns.findAll(); if(!authenticated()||authorization.isAdmin(SecurityContextHolder.getContext().getAuthentication())) return all; var auth=SecurityContextHolder.getContext().getAuthentication(); return all.stream().filter(c->authorization.canAccessCampaign(c.getId(),auth)).toList();}
 @Transactional public CampaignEntity create(String name){if(authenticated()) authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication()); return campaigns.save(new CampaignEntity(UUID.randomUUID().toString(),name.trim()));}
 public CampaignEntity get(String id){var campaign=campaigns.findById(id).orElseThrow(()->new NoSuchElementException("Campaign not found")); if(authenticated()) authorization.requireCampaign(SecurityContextHolder.getContext().getAuthentication(),id); return campaign;}
 @Transactional public void delete(String id){if(authenticated()) authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication()); getUnsecured(id); characters.deleteByCampaign(id); campaigns.deleteById(id);}
 public List<?> characters(String id){get(id); var result=characters.listByCampaign(id); var auth=SecurityContextHolder.getContext().getAuthentication(); if(!authenticated() || authorization.isAdmin(auth)) return result; var owner=identities.requireCurrentUser(auth).getId(); return result.stream().filter(c->owner.equals(c.getOwnerUserId())).toList();}
 @Transactional public Object createCharacter(String id,String name,String imageUrl){get(id);return characters.create(id,name,imageUrl);}
 public List<?> members(String id){authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication()); getUnsecured(id); return invitations.findByCampaignIdOrderByEmailAsc(id).stream().map(i->Map.of("id",i.getId(),"email",i.getEmail(),"active",i.isActive(),"createdAt",i.getCreatedAt(),"revokedAt",Objects.toString(i.getRevokedAt(),""))).toList();}
 @Transactional public Map<String,Object> invite(String id,String email){authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication()); getUnsecured(id); String normalized=com.dexm.personajes.security.AuthIdentity.normalizeEmail(email); if(normalized.isBlank()) throw new IllegalArgumentException("Email is required"); var existing=invitations.findByCampaignIdAndEmail(id,normalized); var invitation=existing.orElseGet(()->new CampaignInvitationEntity(UUID.randomUUID().toString(),id,normalized)); invitation.reactivate(); var saved=invitations.save(invitation); return Map.of("id",saved.getId(),"email",saved.getEmail(),"active",saved.isActive());}
 @Transactional public void revoke(String id,String email){authorization.requireAdmin(SecurityContextHolder.getContext().getAuthentication()); getUnsecured(id); invitations.findByCampaignIdAndEmail(id,com.dexm.personajes.security.AuthIdentity.normalizeEmail(email)).ifPresent(i->{i.revoke();invitations.save(i);});}
 private CampaignEntity getUnsecured(String id){return campaigns.findById(id).orElseThrow(()->new NoSuchElementException("Campaign not found"));}
 private boolean authenticated(){var a=SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.isAuthenticated()&&!"anonymousUser".equals(a.getPrincipal());}
}
