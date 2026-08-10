alter table characters add creation_mode varchar(20) not null default 'empty';
alter table characters add race varchar(80);
alter table characters add einherjer boolean not null default false;
alter table characters add awakened boolean not null default false;
alter table characters add selected_major_attributes_json text not null default '[]';
alter table characters add creation_wizard_state varchar(20) not null default 'empty';
