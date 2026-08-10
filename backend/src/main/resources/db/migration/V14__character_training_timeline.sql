alter table characters add starting_age int null;
alter table characters add awakening_age int null;
alter table characters add sheet_age int null;
alter table characters add einherjer_origin varchar(20) null;
create table training_activities (
 id varchar(36) primary key,
 character_id varchar(36) not null,
 type varchar(20) not null,
 name varchar(160) not null,
 start_age int not null,
 end_age int not null,
 priority int not null,
 primary_attribute varchar(120),
 secondary_attribute varchar(120),
 tertiary_attribute varchar(120),
 concurrent boolean not null default false,
 constraint fk_training_character foreign key(character_id) references characters(id)
);
create index ix_training_character_time on training_activities(character_id,start_age,priority);
