alter table character_attribute_modifiers add column exact_score decimal(12,8) not null default 0;
update character_attribute_modifiers set exact_score = score;
