alter table weapon_inventory add column loaded_bullets decimal(12,3) not null default 0;
alter table weapon_inventory add constraint ck_weapon_loaded_bullets
    check (loaded_bullets >= 0 and loaded_bullets <= capacity and loaded_bullets = floor(loaded_bullets));
