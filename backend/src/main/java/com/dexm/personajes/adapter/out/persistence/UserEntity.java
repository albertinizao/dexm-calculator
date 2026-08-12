package com.dexm.personajes.adapter.out.persistence;

import java.time.Instant;

public class UserEntity {
    private String id;
    private String googleSubject;
    private String email;
    private String displayName;
    private Instant createdAt;
    private Instant lastLoginAt;
    protected UserEntity() {}
    public UserEntity(String id, String googleSubject, String email, String displayName) { this.id=id; this.googleSubject=googleSubject; this.email=email; this.displayName=displayName; this.createdAt=Instant.now(); this.lastLoginAt=this.createdAt; }
    public String getId(){return id;} public String getGoogleSubject(){return googleSubject;} public String getEmail(){return email;} public String getDisplayName(){return displayName;} public Instant getCreatedAt(){return createdAt;} public Instant getLastLoginAt(){return lastLoginAt;}
    public void update(String email, String displayName){this.email=email; this.displayName=displayName; this.lastLoginAt=Instant.now();}
}
