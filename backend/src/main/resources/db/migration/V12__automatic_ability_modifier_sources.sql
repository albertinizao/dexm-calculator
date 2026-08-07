alter table character_attribute_modifiers add column source varchar(20) not null default 'MANUAL';
alter table minor_attribute_definitions add column owner_character_id varchar(36) null;
alter table minor_attribute_definitions add constraint fk_minor_attr_owner foreign key(owner_character_id) references characters(id);
create index ix_minor_attr_owner on minor_attribute_definitions(owner_character_id);
