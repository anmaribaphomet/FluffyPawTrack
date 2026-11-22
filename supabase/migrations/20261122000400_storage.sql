-- Fluffy PawTrack: Storage buckets for photos and their access policies.
-- Apply after 20261122000300_functions.sql. Safe to re-run.
--
-- The three buckets are public: anyone can open a photo by its public URL, so no select
-- policy for reading is needed. Writes are limited by folder (the first path segment):
--   report-photos/<auth.uid()>/<file>      report photos and the optional "Es mi mascota" photo
--   adoption-photos/<organization_id>/<file>  listing photos, by affiliates of that center
--   place-photos/<auth.uid()>/<file>       photo or logo sent with an affiliation request
-- Photos are compressed in the app before upload (REQUIREMENTS section 8); the bucket limit
-- below only rejects oversized or non-image files.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
    ('report-photos', 'report-photos', true, 5242880, array['image/jpeg', 'image/png', 'image/webp']),
    ('adoption-photos', 'adoption-photos', true, 5242880, array['image/jpeg', 'image/png', 'image/webp']),
    ('place-photos', 'place-photos', true, 5242880, array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do update
    set public = excluded.public,
        file_size_limit = excluded.file_size_limit,
        allowed_mime_types = excluded.allowed_mime_types;

-- ---------------------------------------------------------------------------
-- report-photos: any session, anonymous included (RF-01), writes in its own folder
-- ---------------------------------------------------------------------------

drop policy if exists report_photos_select_own on storage.objects;
create policy report_photos_select_own on storage.objects
    for select to authenticated
    using (bucket_id = 'report-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists report_photos_insert_own on storage.objects;
create policy report_photos_insert_own on storage.objects
    for insert to authenticated
    with check (bucket_id = 'report-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists report_photos_update_own on storage.objects;
create policy report_photos_update_own on storage.objects
    for update to authenticated
    using (bucket_id = 'report-photos' and (storage.foldername(name))[1] = (select auth.uid())::text)
    with check (bucket_id = 'report-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists report_photos_delete_own on storage.objects;
create policy report_photos_delete_own on storage.objects
    for delete to authenticated
    using (bucket_id = 'report-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

-- ---------------------------------------------------------------------------
-- adoption-photos: affiliates of an approved organization write in its folder
-- ---------------------------------------------------------------------------

drop policy if exists adoption_photos_select_center on storage.objects;
create policy adoption_photos_select_center on storage.objects
    for select to authenticated
    using (bucket_id = 'adoption-photos' and (storage.foldername(name))[1] = (select public.my_organization_id())::text);

drop policy if exists adoption_photos_insert_center on storage.objects;
create policy adoption_photos_insert_center on storage.objects
    for insert to authenticated
    with check (bucket_id = 'adoption-photos' and (storage.foldername(name))[1] = (select public.my_organization_id())::text);

drop policy if exists adoption_photos_update_center on storage.objects;
create policy adoption_photos_update_center on storage.objects
    for update to authenticated
    using (bucket_id = 'adoption-photos' and (storage.foldername(name))[1] = (select public.my_organization_id())::text)
    with check (bucket_id = 'adoption-photos' and (storage.foldername(name))[1] = (select public.my_organization_id())::text);

drop policy if exists adoption_photos_delete_center on storage.objects;
create policy adoption_photos_delete_center on storage.objects
    for delete to authenticated
    using (bucket_id = 'adoption-photos' and (storage.foldername(name))[1] = (select public.my_organization_id())::text);

-- ---------------------------------------------------------------------------
-- place-photos: a signed-in, non-anonymous account uploads before its organization exists
-- ---------------------------------------------------------------------------

drop policy if exists place_photos_select_own on storage.objects;
create policy place_photos_select_own on storage.objects
    for select to authenticated
    using (bucket_id = 'place-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists place_photos_insert_own on storage.objects;
create policy place_photos_insert_own on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'place-photos'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and not (select public.is_anonymous())
    );

drop policy if exists place_photos_update_own on storage.objects;
create policy place_photos_update_own on storage.objects
    for update to authenticated
    using (bucket_id = 'place-photos' and (storage.foldername(name))[1] = (select auth.uid())::text)
    with check (bucket_id = 'place-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists place_photos_delete_own on storage.objects;
create policy place_photos_delete_own on storage.objects
    for delete to authenticated
    using (bucket_id = 'place-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

-- ---------------------------------------------------------------------------
-- Admin manages every photo of the three buckets (RF-07 supervision)
-- ---------------------------------------------------------------------------

drop policy if exists app_photos_admin on storage.objects;
create policy app_photos_admin on storage.objects
    for all to authenticated
    using (bucket_id in ('report-photos', 'adoption-photos', 'place-photos') and (select public.is_admin()))
    with check (bucket_id in ('report-photos', 'adoption-photos', 'place-photos') and (select public.is_admin()));
