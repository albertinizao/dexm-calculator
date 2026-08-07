alter table character_milestones modify column snapshot_json longtext not null;
alter table character_milestones modify column new_bonuses_json longtext not null;
alter table character_milestones modify column new_abilities_json longtext not null;
