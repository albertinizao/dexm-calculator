package com.dexm.personajes.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "users")
public class UserEntity {
    @Id private String id;
    @Column(name = "google_subject", nullable = false, unique = true) private String googleSubject;
    @Column(nullable = false) private String email;
    @Column(name = "display_name") private String displayName;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "last_login_at", nullable = false) private Instant lastLoginAt;
    protected UserEntity() {}
    public UserEntity(String id, String googleSubject, String email, String displayName) { this.id=id; this.googleSubject=googleSubject; this.email=email; this.displayName=displayName; this.createdAt=Instant.now(); this.lastLoginAt=this.createdAt; }
    public String getId(){return id;} public String getGoogleSubject(){return googleSubject;} public String getEmail(){return email;} public String getDisplayName(){return displayName;} public Instant getCreatedAt(){return createdAt;} public Instant getLastLoginAt(){return lastLoginAt;}
    public void update(String email, String displayName){this.email=email; this.displayName=displayName; this.lastLoginAt=Instant.now();}
}
