package com.dexm.personajes.security;

import com.dexm.personajes.adapter.out.persistence.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AuthorizationService {
    private final CampaignRepository campaigns;
    private final CampaignInvitationRepository invitations;
    private final CharacterRepository characters;
    private final SecurityIdentityService identities;
    public AuthorizationService(CampaignRepository campaigns, CampaignInvitationRepository invitations, CharacterRepository characters, SecurityIdentityService identities){this.campaigns=campaigns;this.invitations=invitations;this.characters=characters;this.identities=identities;}
    public AuthIdentity identity(Authentication auth){return identities.current(auth);}
    public boolean isAdmin(Authentication auth){return identities.isAdmin(identity(auth));}
    public void requireAdmin(Authentication auth){if(!isAdmin(auth)) throw new AccessDeniedException("Administrator role required");}
    public void requireApplicationAccess(Authentication auth){
        var id=identity(auth);
        if(!identities.isAdmin(id) && !invitations.existsByEmailAndRevokedAtIsNull(id.email()))
            throw new AccessDeniedException("No active campaign invitation found");
    }
    public boolean canAccessCampaign(String campaignId, Authentication auth){var id=identity(auth); return identities.isAdmin(id)||invitations.existsByCampaignIdAndEmailAndRevokedAtIsNull(campaignId,id.email());}
    public void requireCampaign(Authentication auth,String campaignId){if(!canAccessCampaign(campaignId,auth)) throw new AccessDeniedException("Campaign access denied");}
    public void requireCharacter(Authentication auth,String characterId, boolean write){
        var character=characters.findById(characterId).orElseThrow(()->new NoSuchElementException("Character not found"));
        var id=identity(auth); if(identities.isAdmin(id)) return;
        var user=identities.requireCurrentUser(auth); if(!user.getId().equals(character.getOwnerUserId())) throw new AccessDeniedException("Character access denied");
    }
    public void requireCharacter(Authentication auth,String characterId){requireCharacter(auth,characterId,false);}
}
