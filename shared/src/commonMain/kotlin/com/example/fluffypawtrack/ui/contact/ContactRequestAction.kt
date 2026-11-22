package com.example.fluffypawtrack.ui.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.ui.theme.FluffyColors

/**
 * "Es mi mascota" button for the map's report sheet. Shown only on possibly-lost reports
 * of someone else; disabled with the RF-05 notice when the report has no recipient.
 */
@Composable
fun ContactRequestAction(
    report: Report,
    onRequest: (reportId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val user by container.authRepository.currentUser.collectAsStateWithLifecycle()

    when (val state = contactActionState(report, user)) {
        ContactActionState.Hidden -> Unit
        ContactActionState.Enabled ->
            CardActionButton("Es mi mascota", onClick = { onRequest(report.id) }, modifier = modifier.fillMaxWidth())
        is ContactActionState.Disabled -> Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CardActionButton("Es mi mascota", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            Text(state.reason, style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
        }
    }
}
