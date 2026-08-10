create table other_inventory_items (
    id varchar(36) not null primary key,
    character_id varchar(255) not null,
    name varchar(255) not null,
    description text,
    location varchar(255),
    quantity integer not null,
    unit_value decimal(19,4),
    constraint fk_other_inventory_character foreign key (character_id) references characters(id) on delete cascade
);
create index ix_other_inventory_character on other_inventory_items(character_id);
