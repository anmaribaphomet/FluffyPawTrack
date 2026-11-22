-- Fluffy PawTrack: RPC functions and triggers for the multi-row rules (RF-04, RF-05, RF-06,
-- RF-07). Apply after 20261122000200_rls.sql. Safe to re-run.
--
-- Every RPC is `security definer` with an empty search_path: it runs as the owner, checks
-- the caller (auth.uid(), profile role, anonymous flag) itself and fails with a Spanish
-- message (`raise exception`) that matches the fake repositories in
-- shared/.../data/fake. Internal helpers live in the `private` schema, which the Data API
-- does not expose and `anon`/`authenticated` cannot use.

create schema if not exists private;
revoke all on schema private from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- Internal helpers
-- ---------------------------------------------------------------------------

create or replace function private.notify(
    p_user_id uuid,
    p_type public.notification_type,
    p_title text,
    p_body text,
    p_report_id uuid default null
)
returns void
language sql
security definer
set search_path = ''
as $$
    insert into public.notifications (user_id, type, title, body, report_id)
    values (p_user_id, p_type, p_title, p_body, p_report_id);
$$;

-- Every affiliate account of an organization receives the notification.
create or replace function private.notify_organization(
    p_org_id uuid,
    p_type public.notification_type,
    p_title text,
    p_body text,
    p_report_id uuid default null
)
returns void
language sql
security definer
set search_path = ''
as $$
    insert into public.notifications (user_id, type, title, body, report_id)
    select p.id, p_type, p_title, p_body, p_report_id
    from public.profiles p
    where p.role = 'affiliate' and p.organization_id = p_org_id;
$$;

create or replace function private.report_status_label(p_status public.report_status)
returns text
language sql
immutable
set search_path = ''
as $$
    select case p_status
        when 'pending' then 'Pendiente de atención'
        when 'on_the_way' then 'En camino'
        when 'sheltered' then 'Resguardado'
        when 'closed' then 'Caso cerrado'
    end;
$$;

-- The reporter, or any affiliate account of the recipient organization (RF-05 "Receptor").
create or replace function private.is_contact_recipient(p_request public.contact_requests)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select case
        when p_request.recipient_user_id is not null then p_request.recipient_user_id = auth.uid()
        else public.is_affiliate_of(p_request.recipient_org_id)
    end;
$$;

-- Pending contact requests of a report become rejected with [p_note]; requesters are notified.
create or replace function private.reject_pending_contact_requests(p_report_id uuid, p_note text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    rejected record;
begin
    for rejected in
        update public.contact_requests
        set status = 'rejected', status_note = p_note, responded_at = now()
        where report_id = p_report_id and status = 'pending'
        returning requester_id
    loop
        perform private.notify(rejected.requester_id, 'report', 'Solicitud de contacto',
                               'Tu solicitud fue rechazada: ' || p_note, p_report_id);
    end loop;
end;
$$;

-- Rejects one adoption request and sends the RF-06 rejection notification.
create or replace function private.reject_adoption_request(p_request_id uuid, p_reason text)
returns public.adoption_requests
language plpgsql
security definer
set search_path = ''
as $$
declare
    rejected public.adoption_requests;
    animal_name text;
begin
    update public.adoption_requests
    set status = 'rejected', rejection_reason = p_reason, responded_at = now()
    where id = p_request_id
    returning * into rejected;

    select l.name into animal_name from public.adoption_listings l where l.id = rejected.listing_id;

    perform private.notify(
        rejected.requester_id,
        'adoption',
        'Solicitud de adopción no aprobada',
        'Tu solicitud para adoptar a ' || coalesce(animal_name, 'este animal')
            || ' no fue aprobada esta vez. ¡Hay muchos más esperando un hogar!' || chr(10)
            || 'Motivo: ' || p_reason
    );
    return rejected;
end;
$$;

-- ---------------------------------------------------------------------------
-- Triggers: status history, report notifications, cascading rejections
-- ---------------------------------------------------------------------------

-- RF-04: every status (including the initial one) is stored with its date and author, and
-- each change notifies the reporter (not for anonymous reports or the reporter's own change).
create or replace function private.reports_track_status()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if tg_op = 'INSERT' or new.status is distinct from old.status then
        insert into public.report_status_history (report_id, status, changed_by)
        values (new.id, new.status, auth.uid());
    end if;

    if tg_op = 'UPDATE' and new.status is distinct from old.status
       and new.reporter_id is not null and not new.reporter_is_anonymous
       and new.reporter_id is distinct from auth.uid() then
        perform private.notify(
            new.reporter_id,
            'report',
            'Actualización de tu reporte',
            'Tu reporte ahora está en "' || private.report_status_label(new.status) || '".',
            new.id
        );
    end if;
    return null;
end;
$$;

drop trigger if exists reports_track_status on public.reports;
create trigger reports_track_status
    after insert or update of status on public.reports
    for each row execute function private.reports_track_status();

-- RF-05 "Reglas técnicas": hiding or deleting a report rejects its pending contact requests.
create or replace function private.reports_release_contact_requests()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if tg_op = 'DELETE' then
        perform private.reject_pending_contact_requests(old.id, 'El reporte ya no está disponible');
        return old;
    end if;
    if new.hidden and not old.hidden then
        perform private.reject_pending_contact_requests(new.id, 'El reporte ya no está disponible');
    end if;
    return new;
end;
$$;

drop trigger if exists reports_release_on_hide on public.reports;
create trigger reports_release_on_hide
    after update of hidden on public.reports
    for each row execute function private.reports_release_contact_requests();

drop trigger if exists reports_release_on_delete on public.reports;
create trigger reports_release_on_delete
    before delete on public.reports
    for each row execute function private.reports_release_contact_requests();

-- RF-06: when an animal becomes adopted (through the RPC or a direct update), its pending
-- requests are rejected with "El animal ya fue adoptado" and the requesters are notified.
create or replace function private.adoption_listings_on_adopted()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    pending_id uuid;
begin
    if new.status = 'adopted' and old.status <> 'adopted' then
        for pending_id in
            select r.id from public.adoption_requests r
            where r.listing_id = new.id and r.status = 'pending'
            for update
        loop
            perform private.reject_adoption_request(pending_id, 'El animal ya fue adoptado');
        end loop;
    end if;
    return null;
end;
$$;

drop trigger if exists adoption_listings_on_adopted on public.adoption_listings;
create trigger adoption_listings_on_adopted
    after update of status on public.adoption_listings
    for each row execute function private.adoption_listings_on_adopted();

-- ---------------------------------------------------------------------------
-- RF-05 contact requests
-- ---------------------------------------------------------------------------

-- The other party's contact data, only when the request is accepted and the caller is one
-- of its two parties; otherwise no row. For an organization recipient it returns the
-- organization's name, phone and email, not those of the affiliate who answered.
create or replace function public.get_contact_details(request_id uuid)
returns table (full_name text, phone text, email text)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    req public.contact_requests;
begin
    select * into req from public.contact_requests cr
    where cr.id = get_contact_details.request_id and cr.status = 'accepted';
    if not found or auth.uid() is null then
        return;
    end if;

    if req.requester_id = auth.uid() then
        if req.recipient_org_id is not null then
            return query
                select o.name, o.phone, o.email from public.organizations o where o.id = req.recipient_org_id;
        else
            return query
                select btrim(p.first_name || ' ' || p.last_name), coalesce(p.phone, ''), coalesce(u.email, '')::text
                from public.profiles p join auth.users u on u.id = p.id
                where p.id = req.recipient_user_id;
        end if;
    elsif private.is_contact_recipient(req) then
        return query
            select btrim(p.first_name || ' ' || p.last_name), coalesce(p.phone, ''), coalesce(u.email, '')::text
            from public.profiles p join auth.users u on u.id = p.id
            where p.id = req.requester_id;
    end if;
end;
$$;

create or replace function public.send_contact_request(
    p_report_id uuid,
    p_message text,
    p_photo_url text default null
)
returns public.contact_requests
language plpgsql
security definer
set search_path = ''
as $$
declare
    me public.profiles;
    rep public.reports;
    created public.contact_requests;
    recipient_user uuid;
    recipient_org uuid;
begin
    select * into me from public.profiles p where p.id = auth.uid();
    if me.id is null or public.is_anonymous() then
        raise exception 'Necesitas una cuenta para enviar solicitudes de contacto';
    end if;
    if me.role <> 'user' then
        raise exception 'Solo los usuarios pueden enviar solicitudes de contacto';
    end if;

    select * into rep from public.reports r where r.id = p_report_id for share;
    if rep.id is null or rep.hidden then
        raise exception 'El reporte ya no está disponible';
    end if;
    if rep.category <> 'possibly_lost' then
        raise exception 'Solo puedes enviar solicitudes en reportes de "Posible mascota extraviada"';
    end if;
    if rep.status = 'closed' then
        raise exception 'Este caso ya está cerrado';
    end if;
    if rep.reporter_id = me.id then
        raise exception 'No puedes enviar una solicitud a tu propio reporte';
    end if;

    if rep.assigned_org_id is not null then
        recipient_org := rep.assigned_org_id;
    elsif rep.reporter_id is not null and not rep.reporter_is_anonymous then
        recipient_user := rep.reporter_id;
    else
        raise exception 'Este reporte es anónimo. Podrás enviar una solicitud cuando un albergue lo tome.';
    end if;

    if exists (select 1 from public.contact_requests cr
               where cr.report_id = rep.id and cr.requester_id = me.id and cr.status in ('pending', 'accepted')) then
        raise exception 'Ya tienes una solicitud activa en este reporte';
    end if;
    if exists (select 1 from public.contact_requests cr
               where cr.report_id = rep.id and cr.requester_id = me.id and cr.status = 'rejected') then
        raise exception 'Tu solicitud en este reporte ya fue rechazada';
    end if;
    if btrim(coalesce(me.phone, '')) = '' then
        raise exception 'Agrega un teléfono a tu perfil antes de continuar';
    end if;
    if char_length(btrim(coalesce(p_message, ''))) not between 20 and 500 then
        raise exception 'El mensaje debe tener entre 20 y 500 caracteres';
    end if;

    begin
        insert into public.contact_requests (
            report_id, requester_id, requester_first_name, recipient_user_id, recipient_org_id, message, photo_url
        )
        values (rep.id, me.id, me.first_name, recipient_user, recipient_org, btrim(p_message),
                nullif(btrim(coalesce(p_photo_url, '')), ''))
        returning * into created;
    exception when unique_violation then
        raise exception 'Ya tienes una solicitud activa en este reporte';
    end;

    if recipient_user is not null then
        perform private.notify(recipient_user, 'report', 'Solicitud de contacto',
                               'Alguien cree que este animal es su mascota', rep.id);
    else
        perform private.notify_organization(recipient_org, 'report', 'Solicitud de contacto',
                                            'Alguien cree que este animal es su mascota', rep.id);
    end if;
    return created;
end;
$$;

-- The recipient accepts (p_accept = true) or rejects a pending request.
create or replace function public.respond_contact_request(p_request_id uuid, p_accept boolean)
returns public.contact_requests
language plpgsql
security definer
set search_path = ''
as $$
declare
    req public.contact_requests;
begin
    select * into req from public.contact_requests cr where cr.id = p_request_id for update;
    if req.id is null then
        raise exception 'La solicitud no existe';
    end if;
    if not private.is_contact_recipient(req) then
        raise exception 'Solo el receptor puede responder esta solicitud';
    end if;
    if req.status <> 'pending' then
        raise exception 'Esta solicitud ya fue respondida';
    end if;
    if p_accept and btrim(coalesce((select p.phone from public.profiles p where p.id = auth.uid()), '')) = '' then
        raise exception 'Agrega un teléfono a tu perfil antes de continuar';
    end if;

    update public.contact_requests
    set status = case when p_accept then 'accepted'::public.contact_request_status
                      else 'rejected'::public.contact_request_status end,
        responded_at = now()
    where id = req.id
    returning * into req;

    perform private.notify(req.requester_id, 'report', 'Solicitud de contacto',
                           case when p_accept then 'Tu solicitud fue aceptada' else 'Tu solicitud fue rechazada' end,
                           req.report_id);
    return req;
end;
$$;

create or replace function public.cancel_contact_request(p_request_id uuid)
returns public.contact_requests
language plpgsql
security definer
set search_path = ''
as $$
declare
    req public.contact_requests;
begin
    select * into req from public.contact_requests cr where cr.id = p_request_id for update;
    if req.id is null then
        raise exception 'La solicitud no existe';
    end if;
    if req.requester_id is distinct from auth.uid() then
        raise exception 'Solo quien envió la solicitud puede cancelarla';
    end if;
    if req.status <> 'pending' then
        raise exception 'Solo puedes cancelar solicitudes pendientes';
    end if;

    update public.contact_requests
    set status = 'cancelled', responded_at = now()
    where id = req.id
    returning * into req;
    return req;
end;
$$;

-- The recipient confirms the delivery: the report closes and its other pending requests
-- are rejected with "El caso se cerró".
create or replace function public.mark_pet_delivered(p_request_id uuid)
returns public.contact_requests
language plpgsql
security definer
set search_path = ''
as $$
declare
    req public.contact_requests;
    rep public.reports;
begin
    select * into req from public.contact_requests cr where cr.id = p_request_id for update;
    if req.id is null then
        raise exception 'La solicitud no existe';
    end if;
    if not private.is_contact_recipient(req) then
        raise exception 'Solo el receptor puede responder esta solicitud';
    end if;
    if req.status <> 'accepted' then
        raise exception 'Solo puedes marcar como entregada una solicitud aceptada';
    end if;

    select * into rep from public.reports r where r.id = req.report_id for update;
    if rep.id is null then
        raise exception 'El reporte ya no está disponible';
    end if;
    if rep.status = 'closed' then
        raise exception 'Este caso ya está cerrado';
    end if;

    update public.reports set status = 'closed' where id = rep.id;
    perform private.reject_pending_contact_requests(rep.id, 'El caso se cerró');
    return req;
end;
$$;

-- ---------------------------------------------------------------------------
-- RF-06 adoption requests
-- ---------------------------------------------------------------------------

create or replace function public.submit_adoption_request(
    p_listing_id uuid,
    p_full_name text,
    p_phone text,
    p_email text,
    p_housing_type public.housing_type,
    p_housing_tenure public.housing_tenure,
    p_has_other_pets boolean,
    p_other_pets text,
    p_reason text
)
returns public.adoption_requests
language plpgsql
security definer
set search_path = ''
as $$
declare
    listing public.adoption_listings;
    created public.adoption_requests;
begin
    if auth.uid() is null or public.is_anonymous() then
        raise exception 'Necesitas una cuenta para enviar una solicitud de adopción';
    end if;
    if not public.is_regular_user() then
        raise exception 'Solo los usuarios pueden enviar solicitudes de adopción';
    end if;

    select * into listing from public.adoption_listings l where l.id = p_listing_id for share;
    if listing.id is null or listing.status <> 'available' then
        raise exception 'Este animal ya no está disponible para adopción';
    end if;
    if exists (select 1 from public.adoption_requests r
               where r.listing_id = listing.id and r.requester_id = auth.uid()
                   and r.status in ('pending', 'approved')) then
        raise exception 'Ya tienes una solicitud activa para este animal';
    end if;
    if btrim(coalesce(p_full_name, '')) = '' or btrim(coalesce(p_phone, '')) = ''
       or btrim(coalesce(p_email, '')) = '' then
        raise exception 'Completa tu nombre, teléfono y correo';
    end if;
    if p_has_other_pets and btrim(coalesce(p_other_pets, '')) = '' then
        raise exception 'Indica cuáles otras mascotas tienes';
    end if;
    if btrim(coalesce(p_reason, '')) = '' then
        raise exception 'Cuéntanos por qué quieres adoptarlo';
    end if;

    begin
        insert into public.adoption_requests (
            listing_id, organization_id, requester_id, full_name, phone, email,
            housing_type, housing_tenure, has_other_pets, other_pets, reason
        )
        values (
            listing.id, listing.organization_id, auth.uid(), btrim(p_full_name), btrim(p_phone), btrim(p_email),
            p_housing_type, p_housing_tenure, coalesce(p_has_other_pets, false),
            case when p_has_other_pets then btrim(p_other_pets) end, btrim(p_reason)
        )
        returning * into created;
    exception when unique_violation then
        raise exception 'Ya tienes una solicitud activa para este animal';
    end;
    return created;
end;
$$;

-- An affiliate of the center approves (p_approve = true) or rejects with a required reason.
create or replace function public.respond_adoption_request(
    p_request_id uuid,
    p_approve boolean,
    p_reason text default null
)
returns public.adoption_requests
language plpgsql
security definer
set search_path = ''
as $$
declare
    req public.adoption_requests;
    animal_name text;
begin
    select * into req from public.adoption_requests r where r.id = p_request_id for update;
    if req.id is null then
        raise exception 'La solicitud no existe';
    end if;
    if not public.is_affiliate_of(req.organization_id) then
        raise exception 'Solo el centro de este animal puede responder la solicitud';
    end if;
    if req.status <> 'pending' then
        raise exception 'Esta solicitud ya fue respondida';
    end if;

    if not p_approve then
        if btrim(coalesce(p_reason, '')) = '' then
            raise exception 'Escribe el motivo del rechazo';
        end if;
        return private.reject_adoption_request(req.id, btrim(p_reason));
    end if;

    update public.adoption_requests
    set status = 'approved', responded_at = now()
    where id = req.id
    returning * into req;

    select l.name into animal_name from public.adoption_listings l where l.id = req.listing_id;
    perform private.notify(
        req.requester_id,
        'adoption',
        'Solicitud de adopción aprobada',
        '¡Buenas noticias! El centro aprobó tu solicitud para adoptar a ' || coalesce(animal_name, 'este animal')
            || '. Pronto se pondrán en contacto contigo.'
    );
    return req;
end;
$$;

create or replace function public.cancel_adoption_request(p_request_id uuid)
returns public.adoption_requests
language plpgsql
security definer
set search_path = ''
as $$
declare
    req public.adoption_requests;
begin
    select * into req from public.adoption_requests r where r.id = p_request_id for update;
    if req.id is null then
        raise exception 'La solicitud no existe';
    end if;
    if req.requester_id is distinct from auth.uid() then
        raise exception 'Solo quien envió la solicitud puede cancelarla';
    end if;
    if req.status <> 'pending' then
        raise exception 'Solo puedes cancelar solicitudes pendientes';
    end if;

    update public.adoption_requests
    set status = 'cancelled', responded_at = now()
    where id = req.id
    returning * into req;
    return req;
end;
$$;

-- The pending requests are rejected by the adoption_listings_on_adopted trigger.
create or replace function public.mark_listing_adopted(p_listing_id uuid)
returns public.adoption_listings
language plpgsql
security definer
set search_path = ''
as $$
declare
    listing public.adoption_listings;
begin
    select * into listing from public.adoption_listings l where l.id = p_listing_id for update;
    if listing.id is null then
        raise exception 'El animal no existe';
    end if;
    if not public.is_affiliate_of(listing.organization_id) then
        raise exception 'Solo el centro de este animal puede marcarlo como adoptado';
    end if;
    if listing.status = 'adopted' then
        raise exception 'Este animal ya fue marcado como adoptado';
    end if;

    update public.adoption_listings set status = 'adopted' where id = listing.id returning * into listing;
    return listing;
end;
$$;

-- ---------------------------------------------------------------------------
-- RF-04 rescue follow-up and RF-08 "Mis reportes"
-- ---------------------------------------------------------------------------

-- The reporter edits the text of a report while it is pending.
create or replace function public.update_report(p_report_id uuid, p_description text, p_reference text)
returns public.reports
language plpgsql
security definer
set search_path = ''
as $$
declare
    rep public.reports;
begin
    select * into rep from public.reports r where r.id = p_report_id for update;
    if rep.id is null or (rep.hidden and rep.reporter_id is distinct from auth.uid()) then
        raise exception 'El reporte no existe';
    end if;
    if rep.reporter_id is distinct from auth.uid() then
        raise exception 'Solo quien hizo el reporte puede editarlo';
    end if;
    if rep.status <> 'pending' then
        raise exception 'Solo puedes editar reportes pendientes de atención';
    end if;

    update public.reports
    set description = coalesce(btrim(p_description), ''), reference = nullif(btrim(coalesce(p_reference, '')), '')
    where id = rep.id
    returning * into rep;
    return rep;
end;
$$;

-- RF-04: only one organization takes a case; the first affiliate wins atomically.
create or replace function public.take_report(p_report_id uuid)
returns public.reports
language plpgsql
security definer
set search_path = ''
as $$
declare
    org_id uuid := public.my_organization_id();
    rep public.reports;
begin
    if org_id is null then
        raise exception 'Solo los albergues afiliados pueden tomar casos';
    end if;

    update public.reports
    set assigned_org_id = org_id, status = 'on_the_way'
    where id = p_report_id and not hidden and status = 'pending' and assigned_org_id is null
    returning * into rep;

    if rep.id is null then
        select * into rep from public.reports r where r.id = p_report_id;
        if rep.id is null or rep.hidden then
            raise exception 'El reporte no existe';
        elsif rep.status = 'closed' then
            raise exception 'Este caso ya está cerrado';
        else
            raise exception 'Este caso ya fue tomado por otro albergue';
        end if;
    end if;
    return rep;
end;
$$;

-- The assigned organization moves the case forward; the admin may set any status (RF-07).
create or replace function public.set_report_status(p_report_id uuid, p_status public.report_status)
returns public.reports
language plpgsql
security definer
set search_path = ''
as $$
declare
    rep public.reports;
begin
    select * into rep from public.reports r where r.id = p_report_id for update;
    if rep.id is null then
        raise exception 'El reporte no existe';
    end if;
    if not public.is_admin() then
        if not public.is_affiliate_of(rep.assigned_org_id) then
            raise exception 'Solo el albergue asignado puede cambiar el estado del reporte';
        end if;
        if p_status <= rep.status then
            raise exception 'El estado del reporte solo puede avanzar';
        end if;
    end if;

    update public.reports set status = p_status where id = rep.id returning * into rep;
    return rep;
end;
$$;

-- RF-07: the admin hides false or inappropriate reports (and can show them again).
create or replace function public.set_report_hidden(p_report_id uuid, p_hidden boolean)
returns public.reports
language plpgsql
security definer
set search_path = ''
as $$
declare
    rep public.reports;
begin
    if not public.is_admin() then
        raise exception 'Solo el administrador puede ocultar reportes';
    end if;
    update public.reports set hidden = p_hidden where id = p_report_id returning * into rep;
    if rep.id is null then
        raise exception 'El reporte no existe';
    end if;
    return rep;
end;
$$;

-- ---------------------------------------------------------------------------
-- RF-07 affiliations
-- ---------------------------------------------------------------------------

-- A shelter, rescuer or adoption center asks to join; it stays off the map while pending.
create or replace function public.request_affiliation(
    p_type public.organization_type,
    p_name text,
    p_address text,
    p_phone text,
    p_email text,
    p_description text,
    p_photo_url text
)
returns public.organizations
language plpgsql
security definer
set search_path = ''
as $$
declare
    created public.organizations;
begin
    if auth.uid() is null or public.is_anonymous() then
        raise exception 'Necesitas una cuenta para solicitar la afiliación';
    end if;
    if btrim(coalesce(p_name, '')) = '' or btrim(coalesce(p_address, '')) = '' or btrim(coalesce(p_phone, '')) = ''
       or btrim(coalesce(p_email, '')) = '' or btrim(coalesce(p_description, '')) = '' then
        raise exception 'Completa todos los datos de la organización';
    end if;
    if btrim(coalesce(p_photo_url, '')) = '' then
        raise exception 'Agrega una foto del lugar o su logo';
    end if;

    -- The form has no map picker: Plaza Zaragoza until the admin sets the exact spot.
    insert into public.organizations (
        type, name, description, photo_url, lat, lng, address, schedule, phone, email, status, active, requested_by
    )
    values (
        p_type, btrim(p_name), btrim(p_description), btrim(p_photo_url), 29.0729, -110.9559, btrim(p_address),
        'Por confirmar', btrim(p_phone), lower(btrim(p_email)), 'pending', true, auth.uid()
    )
    returning * into created;
    return created;
end;
$$;

-- The admin approves (the requester becomes an affiliate of it) or rejects an affiliation.
create or replace function public.review_affiliation(p_organization_id uuid, p_approve boolean)
returns public.organizations
language plpgsql
security definer
set search_path = ''
as $$
declare
    org public.organizations;
begin
    if not public.is_admin() then
        raise exception 'Solo el administrador puede revisar afiliaciones';
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
                      else 'rejected'::public.organization_status end
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
                               'Tu solicitud de afiliación para ' || org.name || ' no fue aprobada.');
    end if;
    return org;
end;
$$;

-- ---------------------------------------------------------------------------
-- Execute grants: RPCs need a session; internal helpers stay private.
-- ---------------------------------------------------------------------------

revoke all on all functions in schema private from public, anon, authenticated;

revoke execute on function
    public.send_contact_request(uuid, text, text),
    public.respond_contact_request(uuid, boolean),
    public.cancel_contact_request(uuid),
    public.mark_pet_delivered(uuid),
    public.get_contact_details(uuid),
    public.submit_adoption_request(uuid, text, text, text, public.housing_type, public.housing_tenure, boolean, text, text),
    public.respond_adoption_request(uuid, boolean, text),
    public.cancel_adoption_request(uuid),
    public.mark_listing_adopted(uuid),
    public.update_report(uuid, text, text),
    public.take_report(uuid),
    public.set_report_status(uuid, public.report_status),
    public.set_report_hidden(uuid, boolean),
    public.request_affiliation(public.organization_type, text, text, text, text, text, text),
    public.review_affiliation(uuid, boolean)
    from public, anon;

grant execute on function
    public.send_contact_request(uuid, text, text),
    public.respond_contact_request(uuid, boolean),
    public.cancel_contact_request(uuid),
    public.mark_pet_delivered(uuid),
    public.get_contact_details(uuid),
    public.submit_adoption_request(uuid, text, text, text, public.housing_type, public.housing_tenure, boolean, text, text),
    public.respond_adoption_request(uuid, boolean, text),
    public.cancel_adoption_request(uuid),
    public.mark_listing_adopted(uuid),
    public.update_report(uuid, text, text),
    public.take_report(uuid),
    public.set_report_status(uuid, public.report_status),
    public.set_report_hidden(uuid, boolean),
    public.request_affiliation(public.organization_type, text, text, text, text, text, text),
    public.review_affiliation(uuid, boolean)
    to authenticated;
