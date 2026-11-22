package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.ContactRequest
import com.example.fluffypawtrack.domain.model.ContactRequestStatus
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.UserProfile
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Demo data for Hermosillo, Sonora. Phone numbers, emails, and people are fictional;
 * place names follow the Figma design. Every report, place and listing has an image: `res:` refs
 * point to bundled sample photos (credits in `licenses/SAMPLE_IMAGES.md`).
 */
@OptIn(ExperimentalTime::class)
object SampleData {

    const val DEMO_PASSWORD = "demo1234"

    /** Plaza Zaragoza, used where a place has no exact location yet. */
    val HERMOSILLO_CENTER = GeoPoint(29.0729, -110.9559)

    val demoUser = UserProfile(
        id = "user-demo",
        email = "demo@correo.com",
        firstName = "María",
        lastName = "Valenzuela",
        phone = "6620000001",
        role = Role.User,
        organizationId = null,
        isAnonymous = false,
    )

    private val otherUser = UserProfile(
        id = "user-2",
        email = "carlos@correo.com",
        firstName = "Carlos",
        lastName = "Moreno",
        phone = "6620000002",
        role = Role.User,
        organizationId = null,
        isAnonymous = false,
    )

    val users: List<UserProfile> = listOf(demoUser, otherUser)

    val organizations: List<Organization> = listOf(
        Organization(
            id = "org-imbpa",
            type = OrganizationType.Shelter,
            name = "Instituto Municipal de Protección y Bienestar Animal",
            description = "Albergue municipal que recibe animales rescatados en la ciudad.",
            location = GeoPoint(29.1105, -110.9920),
            address = "Blvd. Solidaridad s/n, Hermosillo, Son.",
            schedule = "Lunes a viernes, 8:00 a 15:00",
            phone = "6620001000",
            email = "contacto@imbpa.example",
            social = null,
            photoUrl = "res:sample_place_municipal_shelter",
            status = OrganizationStatus.Approved,
            active = true,
        ),
        Organization(
            id = "org-petcare",
            type = OrganizationType.Vet,
            name = "Pet Care Center",
            description = "Veterinaria aliada con consulta de urgencias.",
            location = GeoPoint(29.0905, -110.9455),
            address = "Blvd. Kino 820, Hermosillo, Son.",
            schedule = "Todos los días, 9:00 a 21:00",
            phone = "6620002000",
            email = "hola@petcare.example",
            social = null,
            photoUrl = "res:sample_place_vet_exam",
            status = OrganizationStatus.Approved,
            active = true,
        ),
        Organization(
            id = "org-patitas",
            type = OrganizationType.AdoptionCenter,
            name = "Patitas Callejeras HMO",
            description = "Iniciativa de rescate animal que busca hogar para perros y gatos de la calle.",
            location = GeoPoint(29.0655, -110.9705),
            address = "Calle Rosales 45, Col. Centro, Hermosillo, Son.",
            schedule = "Sábados y domingos, 10:00 a 14:00",
            phone = "6620003000",
            email = "adopciones@patitas.example",
            social = "patitascallejerashmo",
            photoUrl = "res:sample_place_adoption_center",
            status = OrganizationStatus.Approved,
            active = true,
        ),
        Organization(
            id = "org-huellitas",
            type = OrganizationType.Shelter,
            name = "Refugio Huellitas del Desierto",
            description = "Refugio para perros adultos y animales con lesiones.",
            location = GeoPoint(29.1210, -110.9310),
            address = "Calle de los Yaquis 300, Hermosillo, Son.",
            schedule = "Lunes a sábado, 9:00 a 17:00",
            phone = "6620004000",
            email = "contacto@huellitas.example",
            social = null,
            photoUrl = "res:sample_place_dog_refuge",
            status = OrganizationStatus.Approved,
            active = true,
        ),
        Organization(
            id = "org-zaragoza",
            type = OrganizationType.SafePoint,
            name = "Punto Seguro Plaza Zaragoza",
            description = "Punto de resguardo temporal mientras llega un albergue.",
            location = GeoPoint(29.0729, -110.9559),
            address = "Plaza Zaragoza, Col. Centro, Hermosillo, Son.",
            schedule = "Todos los días, 8:00 a 20:00",
            phone = "6620005000",
            email = "puntoseguro@zaragoza.example",
            social = null,
            photoUrl = "res:sample_place_plaza",
            status = OrganizationStatus.Approved,
            active = true,
        ),
        // Not shown on the map: pending approval.
        Organization(
            id = "org-norte",
            type = OrganizationType.Shelter,
            name = "Rescate Animal Norte",
            description = "Solicitud de afiliación en revisión.",
            location = GeoPoint(29.1400, -110.9600),
            address = "Blvd. Colosio Norte 1200, Hermosillo, Son.",
            schedule = "Por confirmar",
            phone = "6620006000",
            email = "rescate@norte.example",
            social = null,
            photoUrl = "res:sample_place_shelter_kennels",
            status = OrganizationStatus.Pending,
            active = true,
        ),
        // Not shown on the map: deactivated by the admin.
        Organization(
            id = "org-vet-sur",
            type = OrganizationType.Vet,
            name = "Veterinaria del Sur",
            description = "Veterinaria temporalmente fuera del directorio.",
            location = GeoPoint(29.0300, -110.9500),
            address = "Blvd. Las Quintas 50, Hermosillo, Son.",
            schedule = "Lunes a viernes, 10:00 a 18:00",
            phone = "6620007000",
            email = "citas@vetsur.example",
            social = null,
            photoUrl = "res:sample_place_vet_clinic",
            status = OrganizationStatus.Approved,
            active = false,
        ),
    )

    val reports: List<Report> = listOf(
        Report(
            id = "report-1",
            reporterId = demoUser.id,
            category = ReportCategory.Street,
            priority = Priority.Low,
            description = "Perro mediano color café, se ve tranquilo pero delgado.",
            photoUrl = "res:sample_report_street_dog",
            location = GeoPoint(29.0815, -110.9790),
            address = "Av. Juan Romero 148, Col. Jesús García, 83140",
            reference = null,
            status = ReportStatus.Pending,
            assignedOrganizationId = null,
            createdAt = Instant.parse("2026-11-20T16:30:00Z"),
        ),
        Report(
            id = "report-2",
            reporterId = otherUser.id,
            category = ReportCategory.PossiblyLost,
            priority = Priority.Medium,
            description = "Gata blanca con collar rojo, muy cariñosa. Parece tener dueño.",
            photoUrl = "res:sample_report_white_cat",
            location = GeoPoint(29.0950, -110.9520),
            address = "Calle Pino Suárez 210, Col. Centenario",
            reference = "Frente a la tienda de la esquina",
            status = ReportStatus.Pending,
            assignedOrganizationId = null,
            createdAt = Instant.parse("2026-11-21T01:10:00Z"),
        ),
        Report(
            id = "report-3",
            reporterId = null,
            category = ReportCategory.Injured,
            priority = Priority.High,
            description = "Perro con una pata trasera lastimada, cojea al caminar.",
            photoUrl = "res:sample_report_injured_dog",
            location = GeoPoint(29.1020, -110.9680),
            address = "Blvd. Luis Encinas y Reforma",
            reference = "Cerca de la parada del camión",
            status = ReportStatus.OnTheWay,
            assignedOrganizationId = "org-huellitas",
            createdAt = Instant.parse("2026-11-21T14:45:00Z"),
        ),
        Report(
            id = "report-4",
            reporterId = otherUser.id,
            category = ReportCategory.Critical,
            priority = Priority.Urgent,
            description = "Cachorro muy débil, no se levanta. Necesita atención inmediata.",
            photoUrl = "res:sample_report_weak_puppy",
            location = GeoPoint(29.0600, -110.9400),
            address = "Calle Garmendia 80, Col. San Benito",
            reference = null,
            status = ReportStatus.Sheltered,
            assignedOrganizationId = "org-imbpa",
            createdAt = Instant.parse("2026-11-19T20:05:00Z"),
        ),
        Report(
            id = "report-5",
            reporterId = demoUser.id,
            category = ReportCategory.PossiblyLost,
            priority = Priority.Medium,
            description = "Perro labrador amarillo con placa sin nombre.",
            photoUrl = "res:sample_report_labrador",
            location = GeoPoint(29.0880, -110.9300),
            address = "Blvd. Navarrete 350, Col. Valle Escondido",
            reference = "Afuera del parque",
            status = ReportStatus.Pending,
            assignedOrganizationId = null,
            createdAt = Instant.parse("2026-11-21T18:20:00Z"),
        ),
        Report(
            id = "report-6",
            reporterId = otherUser.id,
            category = ReportCategory.Abandoned,
            priority = Priority.Medium,
            description = "Dos gatitos dejados en una caja.",
            photoUrl = "res:sample_report_kittens_box",
            location = GeoPoint(29.0700, -111.0000),
            address = "Calle Dr. Paliza 15, Col. Centenario",
            reference = null,
            status = ReportStatus.Closed,
            assignedOrganizationId = "org-patitas",
            createdAt = Instant.parse("2026-11-15T12:00:00Z"),
        ),
    )

    val adoptionListings: List<AdoptionListing> = listOf(
        AdoptionListing(
            id = "listing-1",
            organizationId = "org-patitas",
            name = "Canela",
            species = "Perro",
            breed = "Mestiza",
            age = "2 años",
            vaccination = "Esquema completo",
            description = "Juguetona y buena con niños.",
            photoUrls = listOf("res:sample_listing_canela_1", "res:sample_listing_canela_2"),
            status = ListingStatus.Available,
        ),
        AdoptionListing(
            id = "listing-2",
            organizationId = "org-patitas",
            name = "Michi",
            species = "Gato",
            breed = "Doméstico de pelo corto",
            age = "8 meses",
            vaccination = "Primera dosis",
            description = "Tranquilo, ideal para departamento.",
            photoUrls = listOf("res:sample_listing_michi_1", "res:sample_listing_michi_2"),
            status = ListingStatus.Available,
        ),
        AdoptionListing(
            id = "listing-3",
            organizationId = "org-patitas",
            name = "Toby",
            species = "Perro",
            breed = "Labrador mestizo",
            age = "4 años",
            vaccination = "Esquema completo",
            description = "Le encanta caminar; necesita patio.",
            photoUrls = listOf("res:sample_listing_toby"),
            status = ListingStatus.Available,
        ),
        AdoptionListing(
            id = "listing-4",
            organizationId = "org-patitas",
            name = "Luna",
            species = "Gato",
            breed = "Siamés mestizo",
            age = "1 año",
            vaccination = "Esquema completo",
            description = "Ya encontró un hogar.",
            photoUrls = listOf("res:sample_listing_luna"),
            status = ListingStatus.Adopted,
        ),
    )

    val notifications: Map<String, List<AppNotification>> = mapOf(
        demoUser.id to listOf(
            AppNotification(
                id = "notification-1",
                title = "Nuevo reporte",
                message = "Gracias por tu aportación, te avisaremos apenas se atienda el caso.",
                receivedAt = Instant.parse("2026-11-20T16:31:00Z"),
                type = NotificationType.Report,
                isRead = true,
            ),
            AppNotification(
                id = "notification-2",
                title = "Solicitud de contacto",
                message = "Alguien cree que este animal es su mascota.",
                receivedAt = Instant.parse("2026-11-21T19:00:00Z"),
                type = NotificationType.Report,
            ),
            AppNotification(
                id = "notification-3",
                title = "Nuevo en adopción",
                message = "Toby busca un hogar en Patitas Callejeras HMO.",
                receivedAt = Instant.parse("2026-11-21T20:15:00Z"),
                type = NotificationType.Adoption,
            ),
        ),
    )

    val contactRequests: List<ContactRequest> = listOf(
        // Sent by the demo user on someone else's possibly-lost report.
        ContactRequest(
            id = "request-1",
            reportId = "report-2",
            requesterId = demoUser.id,
            requesterFirstName = demoUser.firstName,
            recipientUserId = otherUser.id,
            recipientOrganizationId = null,
            message = "Mi gata Nieve se perdió en la Centenario; tiene un collar rojo con cascabel.",
            photoUrl = null,
            status = ContactRequestStatus.Pending,
            statusNote = null,
            createdAt = Instant.parse("2026-11-21T03:00:00Z"),
        ),
        // Received on the demo user's possibly-lost report.
        ContactRequest(
            id = "request-2",
            reportId = "report-5",
            requesterId = otherUser.id,
            requesterFirstName = otherUser.firstName,
            recipientUserId = demoUser.id,
            recipientOrganizationId = null,
            message = "Creo que es mi perro Max; tiene una mancha blanca en el pecho.",
            photoUrl = null,
            status = ContactRequestStatus.Pending,
            statusNote = null,
            createdAt = Instant.parse("2026-11-21T19:00:00Z"),
        ),
    )
}
