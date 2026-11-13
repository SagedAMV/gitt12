create table public.profiles (
 id uuid primary key references auth.users(id) on delete cascade,
 username text not null unique check (username ~ '^[a-z0-9_]{3,32}$'),
 role text not null default 'USER' check (role in ('ADMIN','USER')),
 status text not null default 'PENDING' check (status in ('PENDING','ACTIVE','DISABLED','REJECTED')),
 must_change_password boolean not null default false,
 created_at timestamptz not null default now()
);
create unique index one_admin_only on public.profiles(role) where role = 'ADMIN';
create table public.registration_requests (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null unique references public.profiles(id) on delete cascade,
 status text not null default 'PENDING' check (status in ('PENDING','APPROVED','REJECTED')),
 reviewed_by uuid references public.profiles(id), reviewed_at timestamptz,
 created_at timestamptz not null default now()
);
create table public.repositories (
 id uuid primary key default gen_random_uuid(), github_id bigint not null unique,
 owner text not null, name text not null, default_branch text not null default 'main',
 active boolean not null default true, created_at timestamptz not null default now(),
 unique(owner,name)
);
create table public.user_repository_access (
 user_id uuid references public.profiles(id) on delete cascade,
 repository_id uuid references public.repositories(id) on delete cascade,
 primary key(user_id, repository_id)
);
create table public.audit_logs (
 id bigint generated always as identity primary key,
 actor_id uuid references public.profiles(id) on delete set null,
 action text not null, target text, outcome text not null,
 request_id uuid not null default gen_random_uuid(), created_at timestamptz not null default now()
);
create index audit_time on public.audit_logs(created_at desc);
create table public.rate_limits (key text primary key, window_start timestamptz not null, attempts integer not null);
alter table public.rate_limits enable row level security;
create function public.consume_attempt(p_key text) returns boolean
language plpgsql security definer set search_path = public as $$
declare n integer;
begin
 insert into rate_limits values(p_key, now(), 1)
 on conflict(key) do update set
 attempts = case when rate_limits.window_start < now() - interval '15 minutes' then 1 else rate_limits.attempts + 1 end,
 window_start = case when rate_limits.window_start < now() - interval '15 minutes' then now() else rate_limits.window_start end
 returning attempts into n;
 return n <= 10;
end; $$;
revoke all on function public.consume_attempt(text) from public, anon, authenticated;
grant execute on function public.consume_attempt(text) to service_role;
create function public.is_active() returns boolean language sql stable security definer set search_path = public as $$
 select exists(select 1 from profiles where id=auth.uid() and status='ACTIVE' and not must_change_password);
$$;
create function public.is_admin() returns boolean language sql stable security definer set search_path = public as $$
 select exists(select 1 from profiles where id=auth.uid() and role='ADMIN' and status='ACTIVE' and not must_change_password);
$$;
alter table public.profiles enable row level security;
alter table public.registration_requests enable row level security;
alter table public.repositories enable row level security;
alter table public.user_repository_access enable row level security;
alter table public.audit_logs enable row level security;
create policy profile_read on public.profiles for select to authenticated using (id=auth.uid() or public.is_admin());
create policy requests_read on public.registration_requests for select to authenticated using (public.is_admin());
create policy access_read on public.user_repository_access for select to authenticated using (public.is_admin() or (user_id=auth.uid() and public.is_active()));
create policy repositories_read on public.repositories for select to authenticated using (
 public.is_active() and active and (public.is_admin() or exists(select 1 from public.user_repository_access a where a.user_id=auth.uid() and a.repository_id=repositories.id))
);
create policy audit_read on public.audit_logs for select to authenticated using (public.is_admin());
-- No client write policies. All mutations run through authenticated Edge Functions.
revoke all on public.profiles, public.registration_requests, public.repositories, public.user_repository_access, public.audit_logs, public.rate_limits from anon, authenticated;
grant select on public.profiles, public.registration_requests, public.repositories, public.user_repository_access, public.audit_logs to authenticated;
