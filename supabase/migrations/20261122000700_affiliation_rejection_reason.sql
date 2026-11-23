-- The admin must give a reason when rejecting an affiliation request; the requester sees it.
-- Apply after 20261122000600_review_fixes.sql.

alter table public.organizations
    add column if not exists rejection_reason text;

alter table public.organizations
    drop constraint if exists organizations_rejection_reason_length;
alter table public.organizations
    add constraint organizations_rejection_reason_length
    check (rejection_reason is null or char_length(rejection_reason) <= 500);

-- Replace the two-argument version with one that takes the reason.
drop function if exists public.review_affiliation(uuid, boolean);

create or replace function public.review_affiliation(
    p_organization_id uuid,
    p_approve boolean,
    p_reason text default null
)
returns public.organizations
language plpgsql
security definer
set search_path = ''
as $$
declare
    org public.organizations;
    reason text := nullif(btrim(coalesce(p_reason, '')), '');
begin
    if not public.is_admin() then
        raise exception 'Solo el administrador puede revisar afiliaciones';
    end if;
    if not p_approve and reason is null then
        raise exception 'Escribe el motivo del rechazo';
    end if;
    if reason is not null and char_length(reason) > 500 then
        raise exception 'El motivo debe tener como máximo 500 caracteres';
    end if;

    select * into org from public.organizations o where o.id = p_organization_id for update;
    if org.id is null then
        raise exception 'La organización no existe';
    end if;
    if org.status <> 'pending' then
        raise exception 'Esta solicitud de afiliación ya fue revisada';
    end if;

    update public.organizations
    set status = case when p_approve then 'approved'::public.organization_status
                      else 'rejected'::public.organization_status end,
        rejection_reason = case when p_approve then null else reason end
    where id = org.id
    returning * into org;

    if p_approve and org.requested_by is not null then
        update public.profiles
        set role = 'affiliate', organization_id = org.id
        where id = org.requested_by and role = 'user';
        perform private.notify(org.requested_by, 'general', 'Afiliación aprobada',
                               'Tu solicitud de afiliación para ' || org.name || ' fue aprobada.');
    elsif org.requested_by is not null then
        perform private.notify(org.requested_by, 'general', 'Afiliación no aprobada',
                               'Tu solicitud de afiliación para ' || org.name || ' no fue aprobada.'
                               || E'\nMotivo: ' || reason);
    end if;
    return org;
end;
$$;

revoke all on function public.review_affiliation(uuid, boolean, text) from public, anon;
grant execute on function public.review_affiliation(uuid, boolean, text) to authenticated;
