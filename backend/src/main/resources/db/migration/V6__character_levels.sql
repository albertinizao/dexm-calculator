alter table characters add column level int not null default 1;
update characters set level = 1 + floor(greatest(experience, 0) / 100);
