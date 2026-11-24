-- Fluffy PawTrack: RF-08 "Información personal editable" (profile photo bucket).
-- Apply after 20261122001000_report_rate_limit_hardening.sql. Safe to re-run.
--
-- Profile editing already relies on 20261122000200_rls.sql: `profiles_update_own` limits
-- updates to the caller's own row and the column grant allows only first_name, last_name,
-- phone and avatar_url, so role and organization_id cannot change through the API. The
-- grant is restated here so this file alone documents (and restores) the rule.

revoke update on public.profiles from anon, authenticated;
grant update (first_name, last_name, phone, avatar_url) on public.profiles to authenticated;

-- ---------------------------------------------------------------------------
-- avatar-photos: public read; each signed-in, non-anonymous account writes in
-- avatar-photos/<auth.uid()>/<file>. Photos are compressed in the app before upload.
-- ---------------------------------------------------------------------------

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatar-photos', 'avatar-photos', true, 5242880, array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do update
    set public = excluded.public,
        file_size_limit = excluded.file_size_limit,
        allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists avatar_photos_select_own on storage.objects;
create policy avatar_photos_select_own on storage.objects
    for select to authenticated
    using (bucket_id = 'avatar-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists avatar_photos_insert_own on storage.objects;
create policy avatar_photos_insert_own on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'avatar-photos'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and not (select public.is_anonymous())
    );

drop policy if exists avatar_photos_update_own on storage.objects;
create policy avatar_photos_update_own on storage.objects
    for update to authenticated
    using (bucket_id = 'avatar-photos' and (storage.foldername(name))[1] = (select auth.uid())::text)
    with check (bucket_id = 'avatar-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists avatar_photos_delete_own on storage.objects;
create policy avatar_photos_delete_own on storage.objects
    for delete to authenticated
    using (bucket_id = 'avatar-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);
