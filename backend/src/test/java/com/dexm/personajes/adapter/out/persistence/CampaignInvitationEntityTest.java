package com.dexm.personajes.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class CampaignInvitationEntityTest {
    @Test
    void ignoresLegacyDerivedActivePropertyWhenReadingFirestoreJson() {
        var json = """
                {"id":"invitation-1","campaignId":"campaign-1","email":"player@example.com","createdAt":"2026-08-12T20:00:00Z","revokedAt":null,"active":true}
                """;

        assertThatCode(() -> new ObjectMapper().findAndRegisterModules()
                .readValue(json, CampaignInvitationEntity.class))
                .doesNotThrowAnyException();
    }
}
