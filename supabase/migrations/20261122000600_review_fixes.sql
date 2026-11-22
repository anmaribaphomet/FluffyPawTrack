-- Fixes from the independent security review of Phase 2.
-- Apply after 20261122000500_seed.sql (or after 0400 if the seed is skipped).

-- 1. RF-05: closing a case by any path (admin, assigned shelter, set_report_status)
--    rejects its pending contact requests, and a closed case can no longer accept one.

create or replace function private.reports_reject_on_close()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.status = 'closed' and old.status is distinct from 'closed' then
        perform private.reject_pending_contact_requests(new.id, 'El caso se cerró');
    end if;
    return new;
end;
$$;

drop trigger if exists reports_reject_on_close on public.reports;
create trigger reports_reject_on_close
    after update of status on public.reports
    for each row execute function private.reports_reject_on_close();

create or replace function private.contact_requests_block_closed_accept()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.status = 'accepted' and old.status = 'pending' and exists (
        select 1 from public.reports r where r.id = new.report_id and r.status = 'closed'
    ) then
        raise exception 'El caso se cerró';
    end if;
    return new;
end;
$$;

drop trigger if exists contact_requests_block_closed_accept on public.contact_requests;
create trigger contact_requests_block_closed_accept
    before update of status on public.contact_requests
    for each row execute function private.contact_requests_block_closed_accept();

-- 2. Deleting an organization that has affiliates used to violate
--    profiles_affiliate_has_org (organization_id is set to null while role stays
--    'affiliate'). Its affiliates go back to the 'user' role first.

create or replace function private.organizations_release_affiliates()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    update public.profiles
       set role = 'user', organization_id = null
     where organization_id = old.id;
    return old;
end;
$$;

drop trigger if exists organizations_release_affiliates on public.organizations;
create trigger organizations_release_affiliates
    before delete on public.organizations
    for each row execute function private.organizations_release_affiliates();

revoke all on function private.reports_reject_on_close() from public;
revoke all on function private.contact_requests_block_closed_accept() from public;
revoke all on function private.organizations_release_affiliates() from public;
