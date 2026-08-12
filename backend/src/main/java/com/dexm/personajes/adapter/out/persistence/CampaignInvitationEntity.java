package com.dexm.personajes.adapter.out.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignInvitationEntity {
    private String id;
    private String campaignId;
    private String email;
    private Instant createdAt;
    private Instant revokedAt;
    protected CampaignInvitationEntity(){}
    public CampaignInvitationEntity(String id,String campaignId,String email){this.id=id;this.campaignId=campaignId;this.email=email;this.createdAt=Instant.now();}
    public String getId(){return id;} public String getCampaignId(){return campaignId;} public String getEmail(){return email;} public Instant getCreatedAt(){return createdAt;} public Instant getRevokedAt(){return revokedAt;} @JsonIgnore public boolean isActive(){return revokedAt==null;} public void revoke(){revokedAt=Instant.now();} public void reactivate(){revokedAt=null;}
}
