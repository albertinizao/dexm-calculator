package com.dexm.personajes.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="campaign_invitations", uniqueConstraints=@UniqueConstraint(name="uq_campaign_invitation_email", columnNames={"campaign_id","email"}))
public class CampaignInvitationEntity {
    @Id private String id;
    @Column(name="campaign_id", nullable=false) private String campaignId;
    @Column(nullable=false) private String email;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="revoked_at") private Instant revokedAt;
    protected CampaignInvitationEntity(){}
    public CampaignInvitationEntity(String id,String campaignId,String email){this.id=id;this.campaignId=campaignId;this.email=email;this.createdAt=Instant.now();}
    public String getId(){return id;} public String getCampaignId(){return campaignId;} public String getEmail(){return email;} public Instant getCreatedAt(){return createdAt;} public Instant getRevokedAt(){return revokedAt;} public boolean isActive(){return revokedAt==null;} public void revoke(){revokedAt=Instant.now();} public void reactivate(){revokedAt=null;}
}
