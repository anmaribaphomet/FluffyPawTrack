# Fluffy PawTrack — Requisitos del sistema

> **Lema:** Cada patita importa
> **Fuente:** *Actividad 06 – Propuesta de Innovación* (Ingeniería de Software, UNISON, 5.º semestre), el diseño de Figma y el código actual.
> **Figma:** https://www.figma.com/design/wW2C6PzFMazd7DsElw6Q38/Fluffy-PawTrack
> **Design system:** `docs/design-system.md`

---

## 1. Contexto

En Hermosillo hay muchos animales en situación de calle y mascotas extraviadas. Hoy los reportes están dispersos en redes sociales y en distintas instituciones. Fluffy PawTrack es una app móvil con un **mapa colaborativo**: los ciudadanos reportan animales con foto y GPS, los **albergues afiliados** cercanos reciben un aviso y dan seguimiento al caso, y se centralizan las **adopciones** y los **lugares de apoyo** (albergues, veterinarias y puntos seguros).

## 2. Fechas y alcance

| Hito | Fecha | Qué debe estar listo |
|---|---|---|
| Avance | **Lunes 23 nov 2026** | Al menos el **70 % de la vista del cliente** (ciudadano), navegable en Android |
| Meta | Lo antes posible | **Todos los flujos del documento funcionando**, con los 3 roles |

**Plataforma de presentación:** solo **Android**, en emulador o en dispositivo. El código de iOS se queda en el proyecto, pero no se compila ni se prueba, porque el equipo trabaja en Windows.

## 3. Stack técnico

| Capa | Tecnología |
|---|---|
| App | **Kotlin Multiplatform + Compose Multiplatform** (proyecto existente; `shared/` y `androidApp/`) |
| Backend | **Supabase**: Auth, PostgreSQL, Storage para las fotos, Realtime para los cambios en vivo y **Edge Functions** para enviar las push |
| Notificaciones push | **Firebase Cloud Messaging (FCM)**, solo Android (`androidMain`) |
| Cliente Supabase | `supabase-kt` (cliente para KMP; verificar la versión actual con context7) |
| Mapa | Una opción **sin API key ni tarjeta**, basada en OpenStreetMap o MapLibre. Si solo existe para Android, se implementa en `androidMain` con `expect/actual` |
| Ubicación y cámara | APIs de Android (`androidMain`) expuestas al código común con `expect/actual` |
| Navegación | Librería de navegación para Compose Multiplatform. Hoy se navega con un `enum` y un `when`, lo cual no escala a 30+ pantallas |

**Proyecto Supabase (ya creado)**
- URL: `https://mcokzmjspucpjduxzcsz.supabase.co` (plan Free, región Americas)
- La **publishable key** (`sb_publishable_...`) está en `local.properties`. **No se escribe en ningún archivo versionado.**
- Configuración del proyecto: Data API activa, acceso de la API automático a las tablas nuevas y **RLS activado automáticamente** en las tablas nuevas. Por eso cada tabla necesita sus políticas RLS, o la app no podrá leerla.
- Las tablas se crean con **migraciones SQL guardadas en el repo** (`supabase/migrations/`) para que el esquema quede documentado. Se aplican desde el SQL Editor de Supabase.
- La **secret key** y la contraseña de la base de datos **nunca** se usan en la app.

**Firebase (para las push)**
- Se crea un proyecto de Firebase y se registra la app de Android con su `applicationId`.
- `google-services.json` va en `androidApp/` y se agrega al `.gitignore`.
- La **cuenta de servicio de Firebase** (archivo JSON con la llave privada) **nunca** va en la app ni en el repo. Se guarda como *secret* de la Edge Function en Supabase.

**Principios**
- Lo más **ligero y simple** posible. No agregar librerías si no hacen falta.
- Las **credenciales de Supabase** (URL y anon key) van en `local.properties`, que ya está en `.gitignore`. **Nunca se suben a Git.**
- **Repositorio intermedio:** primero se construye la UI con un repositorio *fake* (datos de ejemplo en memoria) y después se cambia por Supabase **sin tocar las pantallas**. Así la demo del lunes no depende del backend.

## 4. Roles y acceso

Al iniciar sesión, el **rol** asociado a la cuenta decide qué pantallas y acciones se ven.

| Rol | Quién es | Cómo se obtiene |
|---|---|---|
| `user` | Ciudadano, dueño de mascota o persona que quiere adoptar | Registro libre en la app |
| `affiliate` | Albergue, rescatista o centro de adopción | Solicita la afiliación desde la app y **el admin la aprueba** |
| `admin` | Administrador de la plataforma | Se asigna manualmente en la base de datos |

- **Reporte anónimo:** se permite reportar sin cuenta (Supabase *anonymous sign-in*). Para dar seguimiento, enviar solicitudes de contacto o tener favoritos sí hace falta cuenta.
  - En el login, "Reportar sin cuenta" abre directamente el flujo de reporte (categoría → foto → ubicación → descripción y prioridad → resumen), **sin** botón "Regresar" en el encabezado.
  - El retroceso es entre pasos. En el **primer paso**, el gesto o botón de atrás muestra el diálogo *"¿Descartar reporte? Se perderá la información que ingresaste."* con **[Seguir reportando]** y **[Descartar]**. Descartar regresa al login y cierra la sesión anónima.
  - Al publicar: modal *"¡Reporte publicado!"* con, abajo, el texto pequeño *"Para ver el mapa y el seguimiento del reporte, inicia sesión."* y el botón **"Volver al inicio"**, que regresa al login y cierra la sesión anónima.
  - El usuario anónimo no tiene barra de navegación inferior ni acceso a otras pantallas.
- **Navegación:** en ninguna pantalla la interfaz puede quedar bloqueada; el gesto de atrás siempre regresa a la pantalla anterior o, en la primera, sale de la app.
- La seguridad se aplica con **Row Level Security (RLS)** en Supabase, no solo ocultando botones en la app.

## 5. Requisitos funcionales

### RF-01 Reporte de animales (foto + GPS)
1. **Categoría** (una de las siguientes):
   - Situación de calle
   - Posible mascota extraviada
   - Maltratado
   - Herido o lesionado
   - **Estado crítico** (siempre con prioridad **urgente**). En toda la app se muestra solo como "Estado crítico".
   - Abandono
2. **Foto (obligatoria):** tomarla con la cámara o subirla de la galería. No se puede avanzar del paso de foto sin ella.
3. **Ubicación:** GPS automático (con diálogo de permiso) y un campo de **referencia adicional** opcional. Se guardan las **coordenadas** y, si se puede obtener con el Geocoder de Android, la **dirección en texto**. Si no se obtiene, se muestran solo las coordenadas. Las coordenadas son las que se usan para el enlace de Google Maps.
4. **Descripción** breve.
5. **Prioridad:** baja, media, alta o urgente. El sistema sugiere una según la categoría y el usuario puede ajustarla, salvo en "estado crítico".
6. **Resumen** y confirmación → modal "¡Reporte publicado!" → botón **Ver mapa**.
- Diseño: pantallas 10 a 16 de Figma, con su indicador de pasos.

### RF-02 Mapa interactivo y filtros
- Muestra los reportes activos, los centros de adopción, los albergues afiliados, las veterinarias aliadas y los puntos seguros. **No hay pines individuales de animales en adopción:** cada centro de adopción tiene **un solo pin**.
- **Un pin de color distinto por categoría.**
- **Filtros:** Todos, cada categoría de reporte, Adopciones y Lugares de apoyo. Hay que ampliar el filtro de Figma, que solo trae 4 opciones.
- Búsqueda en el mapa.
- Al tocar un pin se abre una **hoja de detalle** con foto, descripción, categoría, prioridad, estado y fecha.
- El buscador y el botón **Filtros** quedan completamente **debajo de la onda del encabezado**, sin encimarse con el logo ni con "Regresar".

### RF-03 Notificaciones a albergues cercanos
- Cada reporte nuevo notifica a los **afiliados dentro de un radio configurable**, por ejemplo 5 km por defecto.
- **Preferencias:** cada afiliado y cada usuario elige qué prioridades quiere recibir y cuáles omitir.
- Las notificaciones se guardan en la tabla `notifications` y se muestran en la pantalla de Notificaciones. Mientras la app está abierta llegan **en vivo con Supabase Realtime**.
- **Las notificaciones push son obligatorias.** Llegan al celular **aunque la app esté cerrada**, mediante **Firebase Cloud Messaging (FCM)**:
  1. La app registra el **token FCM** del dispositivo en la tabla `device_tokens` al iniciar sesión, y lo borra al cerrar sesión.
  2. Al crearse un reporte o cambiar su estado, una **Supabase Edge Function** (llamada por un trigger o webhook de la base de datos) busca a los destinatarios y les envía la push con la API HTTP v1 de FCM. Los destinatarios son: los afiliados dentro del radio que aceptan esa prioridad, o el autor del reporte cuando cambia el estado.
  3. Al tocar la notificación, la app abre el **detalle del reporte** correspondiente.
- Las push también se usan para avisar al usuario de una **solicitud de contacto** recibida o aceptada, y al afiliado cuando el admin **aprueba** su afiliación.
- En Android 13 o más reciente se pide el permiso **POST_NOTIFICATIONS**, con una pantalla que explica para qué se usa.
- Se respetan las **preferencias** (`notification_prefs`): si alguien desactivó una prioridad, no recibe ni la push ni la notificación en la app.
- La pantalla de Notificaciones incluye **"Marcar todas como leídas"**.

### RF-04 Seguimiento del rescate
- Estados: **Pendiente de atención → En camino → Resguardado → Caso cerrado**.
- Solo un afiliado puede **tomar** el caso. Una vez tomado, los demás lo ven como "En camino (Albergue X)" y **no pueden tomarlo**, lo que evita duplicar esfuerzos.
- Cada cambio de estado **notifica a quien hizo el reporte**.
- Se guarda un **historial de estados** con fecha y responsable.

### RF-05 Mascotas extraviadas y recuperación
**Objetivo:** que el dueño de una mascota perdida se comunique con quien la encontró, o con el albergue que la resguarda, **sin que ninguno vea el teléfono ni el correo del otro hasta que el receptor acepte**.

#### Términos
- **Solicitante:** el usuario que cree que el animal del reporte es su mascota.
- **Receptor:** quien decide la solicitud. Se determina así, en este orden:
  1. Si el reporte tiene un **albergue asignado** (estado "En camino" o "Resguardado"), el receptor es **ese albergue**. Cualquier cuenta afiliada de esa organización puede responder.
  2. Si no, el receptor es **el usuario que hizo el reporte**.
  3. Si el reporte se hizo **en modo anónimo** y no tiene albergue asignado, **no hay receptor**: el botón aparece deshabilitado con el texto *"Este reporte es anónimo. Podrás enviar una solicitud cuando un albergue lo tome."*

#### Flujo
1. El solicitante abre el detalle de un reporte de categoría **"Posible mascota extraviada"** y toca **"Es mi mascota"**.
2. Llena el formulario:
   - **Mensaje** (obligatorio, de 20 a 500 caracteres) con señas que prueben que es suyo (nombre, marcas, collar…).
   - **Foto de su mascota** (opcional, máximo 1) como prueba.
3. Se crea la solicitud con estado **Pendiente** y el receptor recibe una **push**: *"Alguien cree que este animal es su mascota"*.
4. El receptor abre la solicitud y ve: **solo el nombre de pila** del solicitante, el mensaje, la foto y la fecha. **No** ve su teléfono, su correo ni su apellido.
5. El receptor elige:
   - **Aceptar** → estado **Aceptada**. A partir de ese momento **los dos ven el nombre completo, el teléfono y el correo del otro** en el detalle de la solicitud. Si el receptor es un albergue, se muestran el **teléfono y el correo de la organización**, no los de la persona que respondió. El solicitante recibe la push *"Tu solicitud fue aceptada"*.
   - **Rechazar** → estado **Rechazada**. No se comparte ningún dato y el solicitante recibe la push *"Tu solicitud fue rechazada"*.
6. Ya con la solicitud aceptada, las partes se coordinan **fuera de la app** (llamada, WhatsApp o correo). La app **no** tiene chat.
7. Cuando el animal se entrega, el receptor toca **"Mascota entregada"** en la solicitud aceptada. Con eso:
   - El reporte pasa a **Caso cerrado**.
   - Todas las demás solicitudes **pendientes** de ese reporte pasan a **Rechazada**, con el aviso *"El caso se cerró"*.
   - Los solicitantes afectados reciben una push.

#### Estados de una solicitud
`pending` (Pendiente) → `accepted` (Aceptada) **o** `rejected` (Rechazada) **o** `cancelled` (Cancelada por el solicitante, solo mientras está pendiente).
Las solicitudes aceptadas, rechazadas o canceladas **ya no cambian de estado**.

#### Qué SÍ se puede hacer
- Enviar solicitudes a cualquier reporte de "Posible mascota extraviada" que **no esté cerrado ni oculto**.
- Cancelar una solicitud propia mientras esté **Pendiente**.
- El receptor puede **aceptar más de una** solicitud del mismo reporte, por ejemplo si dos personas dicen ser los dueños y quiere hablar con ambas.
- Ver el historial completo de solicitudes en la pantalla **Solicitudes de contacto**.

#### Qué NO se puede hacer
- Enviar solicitudes **sin cuenta**: los usuarios anónimos no pueden.
- Enviar solicitudes a un reporte **propio**.
- Que un **afiliado o un administrador** envíe solicitudes. Solo el rol `user` puede enviar.
- Tener **más de una solicitud activa** (pendiente o aceptada) del mismo usuario en el mismo reporte.
- **Reenviar** una solicitud a un reporte donde ya fue **rechazada**.
- Enviar o aceptar solicitudes **sin teléfono registrado en el perfil**. Si falta, la app pide completarlo antes de continuar. El correo siempre existe porque es el de la cuenta.
- Ver el teléfono, el correo o el apellido del otro **antes de que la solicitud sea aceptada**.
- Enviar solicitudes en reportes de otras categorías (calle, maltratado, herido, crítico, abandono).

#### Reglas técnicas
- Los datos de contacto **nunca** viajan en la consulta normal de solicitudes. Se obtienen con una **función RPC de Supabase** (`get_contact_details(request_id)`) que verifica que la solicitud esté **aceptada** y que quien pregunta sea **una de las dos partes**. Si no se cumple, no devuelve nada.
- RLS: el solicitante solo ve sus solicitudes enviadas; el receptor solo ve las de sus reportes o las de su organización.
- Si un reporte se **elimina** o el admin lo **oculta**, sus solicitudes pendientes pasan a `rejected`, con el aviso *"El reporte ya no está disponible"*.

#### Pantalla "Solicitudes de contacto" (acceso desde Cuenta)
- **Pestaña Enviadas:** tarjeta con la miniatura del reporte, la fecha, el estado (con color) y el botón **Cancelar** si está pendiente. Si fue aceptada, se muestra el nombre completo, el teléfono y el correo del receptor, con botones para **Llamar** y **Enviar correo**.
- **Pestaña Recibidas:** tarjeta con la miniatura del reporte, el nombre de pila del solicitante, el mensaje, la foto y los botones **Aceptar** y **Rechazar** si está pendiente. Si fue aceptada, se muestran los datos completos del solicitante y el botón **Mascota entregada**.
- Los afiliados ven también la pestaña **Recibidas**, con las solicitudes de los reportes de su organización.

### RF-06 Adopciones
- Los afiliados de tipo centro de adopción crean **fichas de animales** con campos **obligatorios**: fotos (**al menos 1**), tipo de animal, raza, edad, vacunación y descripción.
- Cada ficha queda ligada al centro, con su ubicación y sus métodos de contacto. En el mapa aparece **el centro**, no cada animal.
- El afiliado puede marcar el animal como **adoptado**.

#### Flujo del usuario
1. Tocar el pin del centro → hoja con la información preliminar.
2. **"Ver centro"** → ficha completa del centro y, debajo, la sección **"Animales en adopción"**.
3. Cada animal es una **tarjeta plegada** con foto, nombre, tipo, raza y edad, más una flecha.
4. Tocar la flecha → **ficha completa del animal** (todas sus fotos, vacunación, descripción, etc.). Desde ahí se puede guardar en **favoritos**.
5. Al final de la ficha del animal → botón **"Quiero adoptarlo"**.
- La ficha del centro **no** tiene botón de formulario: el formulario **solo** se abre desde "Quiero adoptarlo" en un animal específico.

#### Solicitud de adopción
- **Formulario:**
  - Campo **"Interesado en:"**, autocompletado y **bloqueado** (no editable), con el formato `Nombre · Tipo · Raza` (ejemplo: "Canela · Perro · Mestiza").
  - Nombre completo, teléfono y correo, **precargados del perfil y editables**.
  - Tipo de vivienda (**casa** o **departamento**) y si es **propia** o **rentada**.
  - **¿Tienes otras mascotas?** (sí o no, y cuáles).
  - **¿Por qué quieres adoptarlo?** (texto, obligatorio).
  - Texto arriba del botón de enviar: *"Una vez aprobada tu solicitud, el centro se pondrá en contacto contigo mediante los datos de contacto que proporcionaste."*
- **Reglas:**
  - Requiere cuenta. Solo el rol `user` puede enviar.
  - Máximo **una solicitud activa** (pendiente o aprobada) por usuario por animal.
  - El usuario puede **cancelar** mientras esté pendiente.
- **Estados:** `pending` (Pendiente) → `approved` (Aprobada) **o** `rejected` (Rechazada) **o** `cancelled` (Cancelada por el usuario, solo mientras está pendiente).
- **Centro (afiliado):** sección **"Solicitudes de adopción"** con la lista agrupada por animal. Ve los datos de cada solicitud y puede **Aprobar** o **Rechazar**.
- **Al aprobar:** push y notificación para el usuario: *"¡Buenas noticias! El centro aprobó tu solicitud para adoptar a [Nombre]. Pronto se pondrán en contacto contigo."*
- **Al rechazar:** push y notificación: *"Tu solicitud para adoptar a [Nombre] no fue aprobada esta vez. ¡Hay muchos más esperando un hogar!"*, y abajo el **motivo de rechazo** que escribe el centro.
- Cuando el centro marca al animal como **adoptado**, sus solicitudes pendientes pasan a **rechazadas** con ese mismo aviso.
- El usuario ve sus solicitudes y su estado en **Cuenta → "Mis solicitudes de adopción"**.

### RF-07 Directorio y administración
- El admin **da de alta, valida o rechaza** albergues, centros de adopción, veterinarias aliadas y puntos seguros: nombre, tipo, ubicación, horario, capacidad y contacto.
- Mientras un lugar esté **activo**, aparece permanentemente en el mapa.
- El admin **supervisa los reportes**: puede verlos todos, ocultar los falsos o inapropiados y cerrarlos.

### RF-08 Cuenta y ajustes
- Registro e inicio de sesión con correo y contraseña. *No hay pantalla en Figma: se diseña con el mismo estilo.*
- Si un dato es inválido, el login y el registro muestran **solo** el mensaje *"Datos inválidos, intenta de nuevo."* Nunca se indica si el correo no existe o si la contraseña está mal.
- En el login, el enlace de afiliación se resalta: en *"¿Eres un albergue o centro de adopción? Solicita tu afiliación"*, la parte *"Solicita tu afiliación"* va en un tono rojizo y subrayada, para que se entienda que lleva a un registro.
- **Solicitud de afiliación:** requiere una cuenta (la persona que la envía se vuelve afiliada al aprobarse). Si no hay sesión, primero se pide crear la cuenta o iniciar sesión y después se abre el formulario. Se guarda con estado `pending` y aparece en el panel del administrador.
- **Información personal editable** (usuario, afiliado y administrador): nombre, apellido, teléfono y foto de perfil se editan con **"Editar"** → **"Guardar cambios"** / **"Cancelar"**; el correo se muestra pero no se edita. Validaciones: nombre obligatorio y teléfono de 10 dígitos. Al guardar se muestra una confirmación. Cada usuario solo puede actualizar su propio perfil y nunca su rol.
- Gestión de cuenta: información personal, **Mis reportes** (historial con Editar y Eliminar, pantallas 17–18), Favoritos, y contadores de Reportes, Atendidos y Favoritos.
- Ajustes: idioma (ES), modo oscuro, notificaciones, accesibilidad, permisos, FAQ y Contáctanos.

## 6. Pantallas por rol

**Común:** carga o splash con el logo, inicio de sesión, registro y solicitud de afiliación.

**Usuario** (barra inferior: Inicio, Mapa, Nuevo, Notificaciones, Ajustes)
- Inicio ✅ *ya implementado*. Tercera tarjeta (casita, debajo de las existentes): *"¿Necesitas apoyo cerca?"*, *"Encuentra veterinarias, albergues y puntos seguros."*, botón **"Ver lugares de apoyo"** que abre el mapa con el filtro "Lugares de apoyo". Las tarjetas existentes no cambian.
- Mapa con filtros y hoja de detalle del pin
- Flujo de nuevo reporte: categoría → foto → ubicación → descripción y prioridad → resumen → publicado
- Adopciones: mapa de centros → ficha del centro → ficha del animal → formulario
- Notificaciones ✅ *ya implementado (sin datos reales)*
- Cuenta ✅ *ya implementado*, más Mis reportes, Favoritos y Mis solicitudes de adopción
- Solicitudes de contacto: pestañas Enviadas y Recibidas, más el formulario "Es mi mascota" (ver RF-05)
- Ajustes

**Afiliado** (mismo estilo, con menú propio)
- Bandeja de reportes cercanos, con filtro por prioridad
- Solicitudes de contacto recibidas en los reportes de su organización (ver RF-05)
- Detalle del reporte → Tomar caso → cambiar estado
- Mis casos en curso
- Mis fichas de adopción (crear, editar, marcar adoptado)
- Solicitudes de adopción de sus animales (aprobar o rechazar con motivo, ver RF-06)
- Perfil de la organización y preferencias de notificación (radio y prioridades)

**Administrador**
- Solicitudes de afiliación pendientes (aprobar o rechazar)
- Directorio de lugares (CRUD: albergues, centros, veterinarias, puntos seguros)
- Supervisión de reportes (ver, ocultar, cerrar)

> Las pantallas que no están en Figma se diseñan con el **design system** y los componentes existentes (encabezado en forma de onda, botones píldora, tarjetas, filas de ajustes).

## 7. Modelo de datos (borrador para Supabase)

| Tabla | Campos principales |
|---|---|
| `profiles` | id (= auth.users), first_name, last_name, phone (obligatorio para las solicitudes de contacto), avatar_url, **role** (`user` / `affiliate` / `admin`), organization_id |
| `organizations` | id, **type** (`shelter` / `adoption_center` / `vet` / `safe_point`; en la app el tipo `vet` se muestra como "Veterinaria"), name, description, **photo_url (obligatoria: foto del lugar o logo)**, lat, lng, address, schedule, capacity, contact, social, **status** (`pending` / `approved` / `rejected`), active |
| `reports` | id, reporter_id (nullable si es anónimo), **category**, **priority**, description, **photo_url (obligatoria)**, lat, lng, address (nullable: dirección del Geocoder), reference, **status**, assigned_org_id, hidden, created_at |
| `report_status_history` | id, report_id, status, changed_by, created_at |
| `adoption_listings` | id, organization_id, name, species, breed, age, vaccination, description, **photos[] (al menos 1)**, status (`available` / `adopted`) |
| `adoption_requests` | id, listing_id, organization_id, requester_id, full_name, phone, email, housing_type (`house` / `apartment`), housing_tenure (`owned` / `rented`), has_other_pets, other_pets, reason, **status** (`pending` / `approved` / `rejected` / `cancelled`), rejection_reason, created_at, responded_at. Restricción: máximo una solicitud activa por (listing_id, requester_id). **RLS:** el usuario ve solo las suyas; los afiliados del centro ven solo las de sus animales |
| `contact_requests` | id, report_id, requester_id, recipient_user_id (nullable), recipient_org_id (nullable), message, photo_url (nullable), status (`pending` / `accepted` / `rejected` / `cancelled`), status_note, created_at, responded_at. Restricción: máximo una solicitud activa por (report_id, requester_id) |
| `notifications` | id, user_id, type, title, body, report_id, read, created_at |
| `notification_prefs` | user_id, priorities[], radius_km, push_enabled |
| `device_tokens` | id, user_id, fcm_token, platform (`android`), updated_at |
| `favorites` | user_id, listing_id |

- Las fotos van en **Supabase Storage**, en los buckets `report-photos` y `adoption-photos`.
- Para buscar "afiliados dentro de X km" se usa una función SQL (o PostGIS) que calcula la distancia.
- **RLS:** cada quien edita solo lo suyo. Solo los afiliados aprobados cambian el estado de un reporte y solo el admin gestiona organizaciones.

## 8. Requisitos no funcionales
- **Privacidad:** no se expone la ubicación ni los datos de quien reporta. Se guarda solo la ubicación del animal.
- **Accesibilidad:** contraste WCAG AA (ver la tabla del design system) y textos legibles para todas las edades.
- **Rendimiento:** las fotos se comprimen antes de subirlas.
- **Idioma de la UI:** español. El código, los nombres y los commits van en inglés.
- **Metodología:** Scrum con iteraciones cortas. Se hace commit al terminar cada unidad de trabajo.
- **Imágenes obligatorias:** toda ficha y toda tarjeta muestra su imagen (reportes, animales en adopción y lugares). Los datos de ejemplo también la tienen.
- **Enlaces externos:**
  - Toda dirección o ubicación (reportes, centros, albergues, veterinarias, puntos seguros) tiene un botón **"Cómo llegar"** que abre Google Maps con la ruta: `https://www.google.com/maps/dir/?api=1&destination=LAT,LNG`.
  - Las redes sociales de los afiliados son enlaces que abren la app o el navegador.
  - Teléfono → abre el marcador. Correo → abre la app de correo.

## 9. Decisiones ya tomadas
- **Tarjetas de Inicio:** se mantiene el café `brown-300`. El texto que va directo sobre la tarjeta usa `title-card` (Inter 20 Bold) o un tamaño mayor para cumplir el contraste, y los botones van sobre píldoras blancas. **Excepción aceptada:** las descripciones de las tarjetas se quedan en tamaño `body`, como en Figma, y no cumplen el contraste AA (4.13:1). El diseño no cambia. Ver `docs/design-system.md`.
- **Persona e idioma:** la UI y las respuestas al equipo van en **español neutro, sin voseo**.
- **Git:** por ahora se trabaja en `master` y se hace push al terminar cada fase. `.atl/` está en `.gitignore`.
- **Reporte anónimo:** sí, con Supabase anonymous sign-in.
- **Foto del reporte:** obligatoria.
- **Ubicación del reporte:** coordenadas siempre, más la dirección en texto del Geocoder de Android cuando se pueda obtener.
- **Solicitudes de adopción:** se guardan en la tabla `adoption_requests` (ver RF-06).
- **Notificaciones:** **push obligatorias con FCM** (con la app cerrada) más Realtime dentro de la app.

## 10. Correcciones pendientes del Figma
- Textos: "Patitas Calljeras" → **Callejeras**, "dispositvo" → **dispositivo**, "Guardas" → **Guardados**, "atentido" → **atendido**.
- "Marcar todas como leídas" quedó fuera del frame.
- Fuentes: unificar en **Inter** (ver el design system).
