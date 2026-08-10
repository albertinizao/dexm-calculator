create table weapon_inventory (
    id varchar(36) not null primary key,
    character_id varchar(255) not null,
    slot varchar(32) not null,
    name varchar(255) not null,
    weapon_type varchar(80) not null,
    size varchar(16) not null,
    weapon_range decimal(12,3) not null,
    reload decimal(12,3) not null,
    rate decimal(12,3) not null,
    damage_vital decimal(12,3) not null,
    damage_normal decimal(12,3) not null,
    damage_light decimal(12,3) not null,
    damage_very_light decimal(12,3) not null,
    aim decimal(12,3),
    automatic_fire varchar(255),
    capacity decimal(12,3) not null,
    caliber varchar(255) not null,
    extra_rule text,
    constraint uk_weapon_character_slot unique (character_id, slot),
    constraint fk_weapon_inventory_character foreign key (character_id) references characters(id) on delete cascade
);
create index ix_weapon_inventory_character on weapon_inventory(character_id);
