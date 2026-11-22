-- Fluffy PawTrack: demo content (optional). Mirrors shared/.../data/fake/SampleData.kt:
-- organizations, adoption listings and reports, with the same bundled `res:sample_*` images.
-- Apply last, from the SQL Editor (it runs as the owner, so RLS does not block it).
-- Safe to re-run: rows with these fixed ids are skipped.
--
-- Fixed ids (SampleData id -> uuid):
--   org-imbpa     a0000000-0000-4000-8000-000000000001
--   org-petcare   a0000000-0000-4000-8000-000000000002
--   org-patitas   a0000000-0000-4000-8000-000000000003
--   org-huellitas a0000000-0000-4000-8000-000000000004
--   org-zaragoza  a0000000-0000-4000-8000-000000000005
--   org-norte     a0000000-0000-4000-8000-000000000006  (pending: not on the map)
--   org-vet-sur   a0000000-0000-4000-8000-000000000007  (inactive: not on the map)
--   listing-1..4  b0000000-0000-4000-8000-00000000000N
--   report-1..6   c0000000-0000-4000-8000-00000000000N  (reporter_id null: no auth users here)

insert into public.organizations
    (id, type, name, description, photo_url, lat, lng, address, schedule, phone, email, social, status, active)
values
    ('a0000000-0000-4000-8000-000000000001', 'shelter',
     'Instituto Municipal de Protección y Bienestar Animal',
     'Albergue municipal que recibe animales rescatados en la ciudad.',
     'res:sample_place_municipal_shelter', 29.1105, -110.9920,
     'Blvd. Solidaridad s/n, Hermosillo, Son.', 'Lunes a viernes, 8:00 a 15:00',
     '6620001000', 'contacto@imbpa.example', null, 'approved', true),
    ('a0000000-0000-4000-8000-000000000002', 'vet',
     'Pet Care Center',
     'Veterinaria aliada con consulta de urgencias.',
     'res:sample_place_vet_exam', 29.0905, -110.9455,
     'Blvd. Kino 820, Hermosillo, Son.', 'Todos los días, 9:00 a 21:00',
     '6620002000', 'hola@petcare.example', null, 'approved', true),
    ('a0000000-0000-4000-8000-000000000003', 'adoption_center',
     'Patitas Callejeras HMO',
     'Iniciativa de rescate animal que busca hogar para perros y gatos de la calle.',
     'res:sample_place_adoption_center', 29.0655, -110.9705,
     'Calle Rosales 45, Col. Centro, Hermosillo, Son.', 'Sábados y domingos, 10:00 a 14:00',
     '6620003000', 'adopciones@patitas.example', 'patitascallejerashmo', 'approved', true),
    ('a0000000-0000-4000-8000-000000000004', 'shelter',
     'Refugio Huellitas del Desierto',
     'Refugio para perros adultos y animales con lesiones.',
     'res:sample_place_dog_refuge', 29.1210, -110.9310,
     'Calle de los Yaquis 300, Hermosillo, Son.', 'Lunes a sábado, 9:00 a 17:00',
     '6620004000', 'contacto@huellitas.example', null, 'approved', true),
    ('a0000000-0000-4000-8000-000000000005', 'safe_point',
     'Punto Seguro Plaza Zaragoza',
     'Punto de resguardo temporal mientras llega un albergue.',
     'res:sample_place_plaza', 29.0729, -110.9559,
     'Plaza Zaragoza, Col. Centro, Hermosillo, Son.', 'Todos los días, 8:00 a 20:00',
     '6620005000', 'puntoseguro@zaragoza.example', null, 'approved', true),
    ('a0000000-0000-4000-8000-000000000006', 'shelter',
     'Rescate Animal Norte',
     'Solicitud de afiliación en revisión.',
     'res:sample_place_shelter_kennels', 29.1400, -110.9600,
     'Blvd. Colosio Norte 1200, Hermosillo, Son.', 'Por confirmar',
     '6620006000', 'rescate@norte.example', null, 'pending', true),
    ('a0000000-0000-4000-8000-000000000007', 'vet',
     'Veterinaria del Sur',
     'Veterinaria temporalmente fuera del directorio.',
     'res:sample_place_vet_clinic', 29.0300, -110.9500,
     'Blvd. Las Quintas 50, Hermosillo, Son.', 'Lunes a viernes, 10:00 a 18:00',
     '6620007000', 'citas@vetsur.example', null, 'approved', false)
on conflict (id) do nothing;

insert into public.adoption_listings
    (id, organization_id, name, species, breed, age, vaccination, description, photo_urls, status)
values
    ('b0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000003',
     'Canela', 'Perro', 'Mestiza', '2 años', 'Esquema completo', 'Juguetona y buena con niños.',
     array['res:sample_listing_canela_1', 'res:sample_listing_canela_2'], 'available'),
    ('b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000003',
     'Michi', 'Gato', 'Doméstico de pelo corto', '8 meses', 'Primera dosis', 'Tranquilo, ideal para departamento.',
     array['res:sample_listing_michi_1', 'res:sample_listing_michi_2'], 'available'),
    ('b0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000003',
     'Toby', 'Perro', 'Labrador mestizo', '4 años', 'Esquema completo', 'Le encanta caminar; necesita patio.',
     array['res:sample_listing_toby'], 'available'),
    ('b0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000003',
     'Luna', 'Gato', 'Siamés mestizo', '1 año', 'Esquema completo', 'Ya encontró un hogar.',
     array['res:sample_listing_luna'], 'adopted')
on conflict (id) do nothing;

-- Sample reports have no reporter (no auth users are created in SQL), so they behave like
-- anonymous reports: "Es mi mascota" is enabled only once an organization takes them.
insert into public.reports
    (id, reporter_id, category, priority, description, photo_url, lat, lng, address, reference,
     status, assigned_org_id, created_at)
values
    ('c0000000-0000-4000-8000-000000000001', null, 'street', 'low',
     'Perro mediano color café, se ve tranquilo pero delgado.',
     'res:sample_report_street_dog', 29.0815, -110.9790,
     'Av. Juan Romero 148, Col. Jesús García, 83140', null,
     'pending', null, '2026-11-20T16:30:00Z'),
    ('c0000000-0000-4000-8000-000000000002', null, 'possibly_lost', 'medium',
     'Gata blanca con collar rojo, muy cariñosa. Parece tener dueño.',
     'res:sample_report_white_cat', 29.0950, -110.9520,
     'Calle Pino Suárez 210, Col. Centenario', 'Frente a la tienda de la esquina',
     'pending', null, '2026-11-21T01:10:00Z'),
    ('c0000000-0000-4000-8000-000000000003', null, 'injured', 'high',
     'Perro con una pata trasera lastimada, cojea al caminar.',
     'res:sample_report_injured_dog', 29.1020, -110.9680,
     'Blvd. Luis Encinas y Reforma', 'Cerca de la parada del camión',
     'on_the_way', 'a0000000-0000-4000-8000-000000000004', '2026-11-21T14:45:00Z'),
    ('c0000000-0000-4000-8000-000000000004', null, 'critical', 'urgent',
     'Cachorro muy débil, no se levanta. Necesita atención inmediata.',
     'res:sample_report_weak_puppy', 29.0600, -110.9400,
     'Calle Garmendia 80, Col. San Benito', null,
     'sheltered', 'a0000000-0000-4000-8000-000000000001', '2026-11-19T20:05:00Z'),
    ('c0000000-0000-4000-8000-000000000005', null, 'possibly_lost', 'medium',
     'Perro labrador amarillo con placa sin nombre.',
     'res:sample_report_labrador', 29.0880, -110.9300,
     'Blvd. Navarrete 350, Col. Valle Escondido', 'Afuera del parque',
     'pending', null, '2026-11-21T18:20:00Z'),
    ('c0000000-0000-4000-8000-000000000006', null, 'abandoned', 'medium',
     'Dos gatitos dejados en una caja.',
     'res:sample_report_kittens_box', 29.0700, -111.0000,
     'Calle Dr. Paliza 15, Col. Centenario', null,
     'closed', 'a0000000-0000-4000-8000-000000000003', '2026-11-15T12:00:00Z')
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- Demo accounts (run by hand AFTER registering them in the app with the same emails)
-- ---------------------------------------------------------------------------
-- Auth users are not created in SQL. Register each account in the app (Registro), then run
-- the matching statement here in the SQL Editor. Profiles are created as `user` on sign-up.
--
-- Affiliate of Patitas Callejeras HMO:
--   update public.profiles
--   set role = 'affiliate', organization_id = 'a0000000-0000-4000-8000-000000000003'
--   where id = (select id from auth.users where email = 'patitas@correo.com');
--
-- Affiliate of Refugio Huellitas del Desierto (to test taking cases and contact requests):
--   update public.profiles
--   set role = 'affiliate', organization_id = 'a0000000-0000-4000-8000-000000000004'
--   where id = (select id from auth.users where email = 'huellitas@correo.com');
--
-- Administrator:
--   update public.profiles
--   set role = 'admin', organization_id = null
--   where id = (select id from auth.users where email = 'admin@correo.com');
--
-- Optional: give a sample report to a registered user so its contact requests have a recipient:
--   update public.reports
--   set reporter_id = (select id from auth.users where email = 'carlos@correo.com')
--   where id = 'c0000000-0000-4000-8000-000000000002';
