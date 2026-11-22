package com.example.fluffypawtrack.ui.adoptions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.account.LabeledValue
import com.example.fluffypawtrack.ui.auth.AuthPrimaryButton
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

/** "Centro de Adopción": a center's info and its available animals (Figma 309:770). */
@Composable
fun CenterDetailRoute(
    organizationId: String,
    onBack: () -> Unit,
    onListingClick: (String) -> Unit,
    onAdoptionForm: (organizationId: String, listingId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "center-$organizationId") {
        CenterDetailViewModel(organizationId, container.organizationRepository, container.adoptionRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CenterDetailScreen(
        state = state,
        onBack = onBack,
        onListingClick = onListingClick,
        onAdoptionForm = { onAdoptionForm(organizationId, null) },
        modifier = modifier,
    )
}

@Composable
fun CenterDetailScreen(
    state: CenterDetailUiState,
    onBack: () -> Unit,
    onListingClick: (String) -> Unit,
    onAdoptionForm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val organization = state.organization
    AccountScaffold(title = organization?.name ?: "Centro de Adopción", onBack = onBack, modifier = modifier) {
        if (organization == null) {
            if (state.loaded) {
                Text(
                    "No encontramos este centro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FluffyColors.Brown900,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            return@AccountScaffold
        }
        Text(
            organization.type.label,
            style = MaterialTheme.typography.labelLarge,
            color = FluffyColors.Brown900,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        AccountCard(color = FluffyColors.Peach200) {
            Text(
                "Sobre este lugar",
                style = MaterialTheme.typography.titleMedium,
                color = FluffyColors.Brown900,
                modifier = Modifier.semantics { heading() },
            )
            Text(organization.description, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
        }
        AccountCard {
            LabeledValue("Dirección", organization.address)
            LabeledValue("Horario", organization.schedule)
            LabeledValue("Teléfono", organization.phone)
            LabeledValue("Correo", organization.email)
            LabeledValue("Redes sociales", organization.social)
        }
        AuthPrimaryButton(text = "Formulario de adopción", onClick = onAdoptionForm)
        Text(
            "Animales en adopción",
            style = MaterialTheme.typography.titleMedium,
            color = FluffyColors.Brown900,
            modifier = Modifier.semantics { heading() },
        )
        if (state.listings.isEmpty()) {
            Text(
                "Por ahora no hay animales disponibles en este lugar.",
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
            )
        }
        state.listings.forEach { listing ->
            ListingRow(listing, onClick = { onListingClick(listing.id) })
        }
    }
}

@Preview
@Composable
private fun CenterDetailScreenPreview() {
    FluffyPawTrackTheme {
        CenterDetailScreen(
            state = CenterDetailUiState(
                organization = SampleData.organizations.first { it.id == "org-patitas" },
                listings = SampleData.adoptionListings.filter { it.status == ListingStatus.Available },
                loaded = true,
            ),
            onBack = {},
            onListingClick = {},
            onAdoptionForm = {},
        )
    }
}
