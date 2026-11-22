package com.example.fluffypawtrack.ui.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.ContactDetails
import com.example.fluffypawtrack.platform.PhotoPreview
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.account.LabeledValue
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.common.formatDateTime
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import kotlin.time.ExperimentalTime

/** "Solicitudes de contacto" (RF-05), opened from Cuenta. */
@Composable
fun ContactRequestsRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        ContactRequestsViewModel(container.authRepository, container.contactRequestRepository, container.reportRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ContactRequestsScreen(
        state = state,
        onBack = onBack,
        onSelectTab = viewModel::selectTab,
        onCancel = viewModel::cancel,
        onAccept = viewModel::accept,
        onReject = viewModel::reject,
        onMarkDelivered = viewModel::markDelivered,
        modifier = modifier,
    )
}

@Composable
fun ContactRequestsScreen(
    state: ContactRequestsUiState,
    onBack: () -> Unit,
    onSelectTab: (ContactRequestsTab) -> Unit,
    onCancel: (String) -> Unit,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onMarkDelivered: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Solicitudes de contacto", onBack = onBack, modifier = modifier) {
        TabSelector(selected = state.tab, onSelect = onSelectTab)

        state.message?.let { AuthErrorMessage(it) }

        if (state.loaded && state.visible.isEmpty()) {
            Text(
                text = if (state.tab == ContactRequestsTab.Sent) {
                    "Aún no has enviado solicitudes."
                } else {
                    "Aún no has recibido solicitudes."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.visible.forEach { item ->
            if (state.tab == ContactRequestsTab.Sent) {
                SentRequestCard(item, onCancel = { onCancel(item.request.id) })
            } else {
                ReceivedRequestCard(
                    item,
                    onAccept = { onAccept(item.request.id) },
                    onReject = { onReject(item.request.id) },
                    onMarkDelivered = { onMarkDelivered(item.request.id) },
                )
            }
        }
    }
}

@Composable
private fun TabSelector(selected: ContactRequestsTab, onSelect: (ContactRequestsTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(FluffyColors.Cream100, FluffyShapes.Pill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ContactRequestsTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Text(
                text = tab.label,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(FluffyShapes.Pill)
                    .background(if (isSelected) FluffyColors.Orange500 else FluffyColors.Cream100)
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .semantics { this.selected = isSelected }
                    .padding(vertical = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun CardHeader(item: ContactRequestItem, title: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ReportThumbnail(item.report)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
            Text(formatDateTime(item.request.createdAt), style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
        }
        StatusChip(item.request.status)
    }
    item.request.statusNote?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
    }
}

@Composable
private fun SentRequestCard(item: ContactRequestItem, onCancel: () -> Unit) {
    AccountCard {
        CardHeader(item, title = item.report?.address ?: "Reporte no disponible")
        item.contact?.let { ContactBlock(it, title = "Datos de contacto") }
        if (item.isDelivered) DeliveredNote()
        if (item.canCancel) {
            CardActionButton("Cancelar", onClick = onCancel, color = FluffyColors.Cream100, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ReceivedRequestCard(
    item: ContactRequestItem,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onMarkDelivered: () -> Unit,
) {
    AccountCard {
        // Before acceptance only the first name is known (RF-05).
        CardHeader(item, title = item.request.requesterFirstName)
        Text(item.request.message, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
        item.request.photoUrl?.let { photo ->
            PhotoPreview(
                photo,
                contentDescription = "Foto enviada por ${item.request.requesterFirstName}",
                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
            )
        }
        if (item.canRespond) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardActionButton("Aceptar", onClick = onAccept, color = FluffyColors.Sage400, modifier = Modifier.weight(1f))
                CardActionButton("Rechazar", onClick = onReject, color = FluffyColors.Pink400, modifier = Modifier.weight(1f))
            }
        }
        item.contact?.let { ContactBlock(it, title = "Datos del solicitante") }
        if (item.canMarkDelivered) {
            CardActionButton("Mascota entregada", onClick = onMarkDelivered, modifier = Modifier.fillMaxWidth())
        }
        if (item.isDelivered) DeliveredNote()
    }
}

@Composable
private fun DeliveredNote() {
    Text("Mascota entregada · Caso cerrado", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
}

/** Full name, phone and email, shown only after acceptance, with Llamar / Enviar correo. */
@Composable
private fun ContactBlock(contact: ContactDetails, title: String) {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(FluffyColors.Peach100, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        LabeledValue("Nombre", contact.fullName)
        LabeledValue("Teléfono", contact.phone)
        LabeledValue("Correo", contact.email)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardActionButton(
                "Llamar",
                onClick = { runCatching { uriHandler.openUri("tel:${contact.phone}") } },
                enabled = contact.phone.isNotBlank(),
                modifier = Modifier.weight(1f),
            )
            CardActionButton(
                "Enviar correo",
                onClick = { runCatching { uriHandler.openUri("mailto:${contact.email}") } },
                enabled = contact.email.isNotBlank(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
