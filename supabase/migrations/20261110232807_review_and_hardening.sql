create function public.review_registration(p_user uuid,p_admin uuid,p_approve boolean) returns void
language plpgsql security definer set search_path=public as $$
begin
 if not exists(select 1 from profiles where id=p_admin and role='ADMIN' and status='ACTIVE' and not must_change_password) then raise exception 'Forbidden'; end if;
 perform 1 from registration_requests where user_id=p_user and status='PENDING' for update;
 if not found then raise exception 'Request already reviewed'; end if;
 update profiles set status=case when p_approve then 'ACTIVE' else 'REJECTED' end where id=p_user and role='USER' and status='PENDING';
 if not found then raise exception 'Invalid account state'; end if;
 update registration_requests set status=case when p_approve then 'APPROVED' else 'REJECTED' end,reviewed_by=p_admin,reviewed_at=now() where user_id=p_user;
end; $$;
revoke all on function public.review_registration(uuid,uuid,boolean) from public,anon,authenticated;
grant execute on function public.review_registration(uuid,uuid,boolean) to service_role;

alter table public.profiles add constraint admin_username check (role <> 'ADMIN' or username = 'sagedkhan746');
grant all on public.profiles, public.registration_requests, public.repositories, public.user_repository_access, public.audit_logs, public.rate_limits to service_role;
grant usage, select on sequence public.audit_logs_id_seq to service_role;
create function public.prune_rate_limits() returns bigint language plpgsql security definer set search_path=public as $$
declare n bigint;
begin
 delete from rate_limits where window_start < now() - interval '1 day';
 get diagnostics n = row_count;
 return n;
end; $$;
revoke all on function public.prune_rate_limits() from public,anon,authenticated;
grant execute on function public.prune_rate_limits() to service_role;
