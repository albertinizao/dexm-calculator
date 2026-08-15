alter table armor_inventory add column defense_penalty decimal(4,2) not null default 0;
alter table armor_inventory add column melee_defense_penalty decimal(4,2) not null default 0;
alter table armor_inventory add column ranged_defense_penalty decimal(4,2) not null default 0;
alter table armor_inventory add column movement_penalty decimal(4,2) not null default 0;
