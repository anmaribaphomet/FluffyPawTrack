package com.example.fluffypawtrack.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

/** Placeholder until the project has a real support inbox. */
const val ContactEmail = "contacto@fluffypawtrack.example"

private data class InfoEntry(val title: String, val body: String)

private fun entriesFor(topic: SettingsTopic): List<InfoEntry> = when (topic) {
    SettingsTopic.Accessibility -> listOf(
        InfoEntry(
            "Tamaño de letra",
            "La aplicación respeta el tamaño de letra de tu teléfono. Puedes cambiarlo en los ajustes de " +
                "pantalla del sistema.",
        ),
        InfoEntry(
            "Contraste",
            "Los textos usan colores con buen contraste sobre los fondos de la aplicación para que se lean con facilidad.",
        ),
        InfoEntry(
            "Lector de pantalla",
            "Los botones e imágenes principales tienen descripciones para usarse con TalkBack.",
        ),
    )
    SettingsTopic.Permissions -> listOf(
        InfoEntry("Ubicación", "Sirve para marcar en el mapa dónde está el animal que reportas."),
        InfoEntry("Cámara y galería", "Sirven para adjuntar una foto del animal a tu reporte."),
        InfoEntry("Notificaciones", "Te avisan cuando cambia el estado de tus reportes o solicitudes."),
        InfoEntry(
            "Cómo cambiarlos",
            "Puedes conceder o retirar estos permisos cuando quieras desde los ajustes de tu teléfono.",
        ),
    )
    SettingsTopic.Faq -> listOf(
        InfoEntry(
            "¿Cómo reporto un animal?",
            "Toca \"Nuevo\" en la barra inferior, elige la categoría, toma o elige una foto, confirma la ubicación " +
                "y agrega una descripción. Tu reporte aparecerá en el mapa.",
        ),
        InfoEntry(
            "¿Puedo reportar sin crear una cuenta?",
            "Sí. Puedes enviar un reporte de forma anónima, pero no podrás consultar su seguimiento después.",
        ),
        InfoEntry(
            "¿Qué significa el estado de mi reporte?",
            "${ReportStatus.Pending.label}: ningún refugio lo ha tomado aún. ${ReportStatus.OnTheWay.label}: " +
                "un refugio va por el animal. ${ReportStatus.Sheltered.label}: el animal ya está a salvo. " +
                "${ReportStatus.Closed.label}: el caso terminó.",
        ),
        InfoEntry(
            "¿Puedo editar o eliminar un reporte?",
            "Sí, desde Cuenta > Mis reportes. Solo puedes editar la descripción y la referencia mientras el " +
                "reporte siga pendiente de atención.",
        ),
    )
    SettingsTopic.Contact -> listOf(
        InfoEntry(
            "Escríbenos",
            "¿Tienes dudas, sugerencias o encontraste un error? Escríbenos a $ContactEmail y te responderemos " +
                "lo antes posible.",
        ),
    )
}

/** Accesibilidad, Permisos, FAQ and Contáctanos: static text opened from Ajustes. */
@Composable
fun SettingsInfoRoute(topic: SettingsTopic, onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsInfoScreen(topic = topic, onBack = onBack, modifier = modifier)
}

@Composable
fun SettingsInfoScreen(topic: SettingsTopic, onBack: () -> Unit, modifier: Modifier = Modifier) {
    AccountScaffold(title = topic.title, onBack = onBack, modifier = modifier) {
        entriesFor(topic).forEach { entry ->
            AccountCard(color = FluffyColors.Peach100) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FluffyColors.Brown900,
                    modifier = Modifier.semantics { heading() },
                )
                Text(entry.body, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
            }
        }
    }
}

@Preview
@Composable
private fun SettingsInfoScreenPreview() {
    FluffyPawTrackTheme {
        SettingsInfoScreen(topic = SettingsTopic.Faq, onBack = {})
    }
}
