create table armor_inventory (id varchar(255) not null, character_id varchar(255) not null, name varchar(255) not null, description text, slots_json text not null, image_url longtext, primary key (id));
create index ix_armor_character on armor_inventory(character_id);
create table shield_inventory (id varchar(255) not null, character_id varchar(255) not null, name varchar(255) not null, description text, hit_points int not null, image_url longtext, primary key (id), constraint uk_shield_character unique(character_id));
create table armor_catalog (id varchar(255) not null, name varchar(255) not null, description text, slots_json text not null, image_url longtext, official boolean not null, primary key(id));
create table shield_catalog (id varchar(255) not null, name varchar(255) not null, description text, hit_points int not null, image_url longtext, official boolean not null, primary key(id));
