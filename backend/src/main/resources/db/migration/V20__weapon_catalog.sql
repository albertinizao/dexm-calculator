create table weapon_catalog (
    id varchar(80) not null primary key,
    name varchar(255) not null,
    weapon_type varchar(80) not null,
    size varchar(16) not null,
    weapon_range decimal(12,3) not null,
    reload decimal(12,3) not null,
    rate varchar(32) not null,
    damage_vital decimal(12,3) not null,
    damage_normal decimal(12,3) not null,
    damage_light decimal(12,3) not null,
    damage_very_light decimal(12,3) not null,
    aim decimal(12,3),
    automatic_fire varchar(255),
    capacity decimal(12,3) not null,
    caliber varchar(255) not null,
    extra_rule text,
    image_url longtext,
    official boolean not null
);
alter table weapon_inventory modify column rate varchar(32) not null;
alter table weapon_inventory add column catalog_weapon_id varchar(80);
alter table weapon_inventory add column image_url longtext;
create index ix_weapon_catalog_type on weapon_catalog(weapon_type);
