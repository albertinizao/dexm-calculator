create table campaigns (id varchar(36) primary key, name varchar(160) not null, created_at timestamp not null);
alter table characters add column campaign_id varchar(36) null;
alter table characters add column image_url text null;
create index ix_character_campaign on characters(campaign_id);
alter table characters add constraint fk_character_campaign foreign key(campaign_id) references campaigns(id);
