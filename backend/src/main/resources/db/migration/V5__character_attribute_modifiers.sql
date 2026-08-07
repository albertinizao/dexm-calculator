create table if not exists character_attribute_modifiers (
    id varchar(36) primary key,
    character_id varchar(36) not null,
    attribute_key varchar(120) not null,
    name varchar(160) not null,
    score int not null,
    constraint fk_attribute_modifier_character foreign key(character_id) references characters(id),
    constraint uq_attribute_modifier unique(character_id, attribute_key, name)
);
create index if not exists ix_attribute_modifier_character on character_attribute_modifiers(character_id);
