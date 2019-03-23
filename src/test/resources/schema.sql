drop all objects; -- H2 special statement to drop all objects

create table public.a (
	id serial primary key,
	ts timestamp default current_timestamp,
	msg varchar(100)
);
