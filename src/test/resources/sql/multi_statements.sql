select max(ts) max_ts from public.a;
delete from public.a where ts = (select min(ts) from public.a);
insert into public.a (msg) values ('Added a new record.');
select * from public.a where 1=0;
select * from public.a;
