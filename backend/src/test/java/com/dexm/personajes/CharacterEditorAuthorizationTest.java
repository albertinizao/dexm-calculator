package com.dexm.personajes;

import com.dexm.personajes.adapter.out.persistence.*;
import com.dexm.personajes.application.CharacterService;
import com.dexm.personajes.security.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterEditorAuthorizationTest {
    @Mock CampaignRepository campaigns;
    @Mock CampaignInvitationRepository invitations;
    @Mock CharacterRepository characters;
    @Mock SecurityIdentityService identities;
    @Mock UserRepository users;
    @Mock MilestoneRepository milestones;
    @Mock AbilityRepository abilities;
    @Mock CharacterMinorAttributeValueRepository minorValues;
    @Mock MinorAttributeDefinitionRepository minorDefs;
    @Mock CharacterAttributeModifierRepository modifiers;

    @Test
    void campaignMemberMayReadCharacterButOnlyAssignedEditorMayWrite() {
        var authentication = authentication("member@example.com");
        var member = new UserEntity("user-1", "subject-1", "member@example.com", "Member");
        var character = new CharacterEntity("character-1", "campaign-1", "Astrid", null, 0, "{}", "{}");
        character.setOwnerUserId("different-user");
        character.setEditorEmails(List.of("member@example.com"));
        when(characters.findById("character-1")).thenReturn(Optional.of(character));
        when(identities.current(authentication)).thenReturn(new AuthIdentity("subject-1", "member@example.com", "Member"));
        when(identities.isAdmin(any())).thenReturn(false);
        when(invitations.existsByCampaignIdAndEmailAndRevokedAtIsNull("campaign-1", "member@example.com")).thenReturn(true);

        var authorization = new AuthorizationService(campaigns, invitations, characters, identities);

        authorization.requireCharacter(authentication, "character-1", false);
        authorization.requireCharacter(authentication, "character-1", true);

        verify(invitations, atLeastOnce()).existsByCampaignIdAndEmailAndRevokedAtIsNull("campaign-1", "member@example.com");
    }

    @Test
    void nonEditorCannotWriteButCanStillReadCampaignCharacter() {
        var authentication = authentication("viewer@example.com");
        var viewer = new UserEntity("user-2", "subject-2", "viewer@example.com", "Viewer");
        var character = new CharacterEntity("character-1", "campaign-1", "Astrid", null, 0, "{}", "{}");
        character.setOwnerUserId("different-user");
        character.setEditorEmails(List.of("editor@example.com"));
        when(characters.findById("character-1")).thenReturn(Optional.of(character));
        when(identities.current(authentication)).thenReturn(new AuthIdentity("subject-2", "viewer@example.com", "Viewer"));
        when(identities.isAdmin(any())).thenReturn(false);
        when(invitations.existsByCampaignIdAndEmailAndRevokedAtIsNull("campaign-1", "viewer@example.com")).thenReturn(true);

        var authorization = new AuthorizationService(campaigns, invitations, characters, identities);

        authorization.requireCharacter(authentication, "character-1", false);
        assertThatThrownBy(() -> authorization.requireCharacter(authentication, "character-1", true))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void adminBypassesCharacterEditorAssignment() {
        var authentication = authentication("albertinizao@gmail.com");
        var character = new CharacterEntity("character-1", "campaign-1", "Astrid", null, 0, "{}", "{}");
        when(characters.findById("character-1")).thenReturn(Optional.of(character));
        when(identities.current(authentication)).thenReturn(new AuthIdentity("admin", "albertinizao@gmail.com", "Admin"));
        when(identities.isAdmin(any())).thenReturn(true);

        var authorization = new AuthorizationService(campaigns, invitations, characters, identities);

        authorization.requireCharacter(authentication, "character-1", true);
        verifyNoInteractions(invitations);
    }

    @Test
    void campaignCreationAssignsNormalizedAuthenticatedEmailAsEditor() throws Exception {
        var authentication = authentication("Creator@Example.COM");
        var identity = new AuthIdentity("subject-3", "creator@example.com", "Creator");
        when(identities.current(authentication)).thenReturn(identity);
        when(identities.requireCurrentUser(authentication)).thenReturn(new UserEntity("creator", "subject-3", identity.email(), "Creator"));
        when(characters.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new CharacterService(characters, milestones, abilities, new ObjectMapper(),
                new com.dexm.personajes.application.MinorAttributeService(minorDefs, minorValues, characters, modifiers),
                minorValues, minorDefs, modifiers);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "identities", identities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            var created = service.create("campaign-1", "Astrid", null);
            assertThat(created.getEditorEmails()).containsExactly("creator@example.com");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void editorEmailsNormalizeAndIgnoreDuplicates() {
        var character = new CharacterEntity("character-1", "Astrid", 0, "{}", "{}");

        character.addEditorEmail(" Editor@Example.COM ");
        character.addEditorEmail("editor@example.com");

        assertThat(character.getEditorEmails()).containsExactly("editor@example.com");
    }

    @Test
    void legacyOwnerKeepsWriteAccessButExplicitEmptyListDoesNot() {
        var authentication = authentication("owner@example.com");
        var owner = new UserEntity("owner-1", "subject-owner@example.com", "owner@example.com", "Owner");
        var character = new CharacterEntity("character-1", "campaign-1", "Astrid", null, 0, "{}", "{}");
        character.setOwnerUserId(owner.getId());
        character.setEditorEmailsConfigured(false);
        when(characters.findById("character-1")).thenReturn(Optional.of(character));
        when(identities.current(authentication)).thenReturn(new AuthIdentity("subject-owner@example.com", "owner@example.com", "Owner"));
        when(identities.isAdmin(any())).thenReturn(false);
        when(identities.requireCurrentUser(authentication)).thenReturn(owner);
        when(invitations.existsByCampaignIdAndEmailAndRevokedAtIsNull("campaign-1", "owner@example.com")).thenReturn(true);

        var authorization = new AuthorizationService(campaigns, invitations, characters, identities);
        authorization.requireCharacter(authentication, character.getId(), true);

        character.setEditorEmails(List.of());
        assertThat(character.isEditorEmailsConfigured()).isTrue();
        assertThatThrownBy(() -> authorization.requireCharacter(authentication, character.getId(), true))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void editorAssignmentRequiresActiveCampaignMembership() {
        var authentication = authentication("albertinizao@gmail.com");
        var character = new CharacterEntity("character-1", "campaign-1", "Astrid", null, 0, "{}", "{}");
        when(characters.findById(character.getId())).thenReturn(Optional.of(character));
        when(identities.current(authentication)).thenReturn(new AuthIdentity("admin", "albertinizao@gmail.com", "Admin"));
        when(identities.isAdmin(any())).thenReturn(true);
        when(invitations.existsByCampaignIdAndEmailAndRevokedAtIsNull("campaign-1", "uninvited@example.com")).thenReturn(false);

        var authorization = new AuthorizationService(campaigns, invitations, characters, identities);
        assertThatThrownBy(() -> authorization.requireCharacterEditorEmail(authentication, character.getId(), "uninvited@example.com"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void editorAddAndRemoveAreNormalizedAndDuplicateSafe() {
        var character = new CharacterEntity("character-1", "Astrid", 0, "{}", "{}");
        when(characters.findById("character-1")).thenReturn(Optional.of(character));
        when(characters.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new CharacterService(characters, milestones, abilities, new ObjectMapper(),
                new com.dexm.personajes.application.MinorAttributeService(minorDefs, minorValues, characters, modifiers),
                minorValues, minorDefs, modifiers);

        service.addEditor("character-1", "Editor@Example.COM");
        service.addEditor("character-1", "editor@example.com");
        service.removeEditor("character-1", "EDITOR@example.com");

        assertThat(character.getEditorEmails()).isEmpty();
        verify(characters, times(3)).save(character);
    }

    private static TestingAuthenticationToken authentication(String email) {
        return new TestingAuthenticationToken(new AuthenticatedPrincipal("subject-" + email, email, email), null, "ROLE_USER");
    }
}
