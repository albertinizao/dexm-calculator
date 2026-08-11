create table ammunition_inventory (
    id varchar(36) not null primary key,
    character_id varchar(255) not null,
    caliber varchar(255) not null,
    quantity integer not null,
    constraint uk_ammunition_character_caliber unique (character_id, caliber),
    constraint fk_ammunition_inventory_character foreign key (character_id) references characters(id) on delete cascade,
    constraint ck_ammunition_inventory_quantity_positive check (quantity > 0)
);
create index ix_ammunition_inventory_character on ammunition_inventory(character_id);

-- Correct only evident catalog typos or text contamination; caliber variants are intentionally not merged.
update weapon_catalog set caliber = '4.6 x 30mm'
 where id = 'h-k-mp7' and official = true and caliber = '4m6 x 30mm';
update weapon_catalog set caliber = '.338 Lapua Magnum'
 where id = 'accuracy-international-awm' and official = true and caliber = '.338 Laupa Magnum';
update weapon_catalog set caliber = '.12'
 where id = 'franchi-spas-12' and official = true
   and caliber = '.12Alternativamente puede funcionar con Cadencia 1x4 a elección de cada acción de disparo.';
update weapon_catalog set caliber = '.12'
 where id = 'benelli-m-3' and official = true
   and caliber = '.12 Alternativamente puede funcionar con Cadencia 1x4 a elección de cada acción de disparo.';
