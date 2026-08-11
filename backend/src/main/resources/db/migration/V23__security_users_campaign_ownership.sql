create table users (
 id varchar(36) primary key,
 google_subject varchar(255) not null unique,
 email varchar(255) not null,
 display_name varchar(255),
 created_at timestamp not null,
 last_login_at timestamp not null
);
create table campaign_invitations (
 id varchar(36) primary key,
 campaign_id varchar(36) not null,
 email varchar(255) not null,
 created_at timestamp not null,
 revoked_at timestamp null,
 constraint fk_invitation_campaign foreign key(campaign_id) references campaigns(id) on delete cascade,
 constraint uq_campaign_invitation_email unique(campaign_id,email)
);
create index ix_invitation_campaign on campaign_invitations(campaign_id);
alter table characters add column owner_user_id varchar(36) null;
alter table characters add constraint fk_character_owner foreign key(owner_user_id) references users(id);
create index ix_character_owner on characters(owner_user_id);
