package com.example.fluffypawtrack.ui.report

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.platform.rememberLocationRequester
import com.example.fluffypawtrack.platform.rememberPhotoLaunchers
import com.example.fluffypawtrack.ui.components.PillActionButton
import com.example.fluffypawtrack.ui.components.ProfileHeader
import com.example.fluffypawtrack.ui.components.ProfileHeaderHeight
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffySemanticColors
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_map
import fluffypawtrack.shared.generated.resources.img_cat_phone
import org.jetbrains.compose.resources.painterResource

/**
 * New-report flow (RF-01, Figma screens 10–16): category → photo → location → description and
 * priority → summary → "¡Reporte publicado!". System back moves to the previous step and leaves
 * the flow through [onBack] on the first one.
 */
@Composable
fun NewReportRoute(onBack: () -> Unit, onSeeMap: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { NewReportViewModel(container.reportRepository, container.authRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val requestLocation = rememberLocationRequester(viewModel::onLocationResult)
    val photos = rememberPhotoLaunchers(viewModel::onPhotoSelected)
    val currentOnBack by rememberUpdatedState(onBack)
    val goBack = remember(viewModel) { { if (!viewModel.back()) currentOnBack() } }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.publishedReport == null,
        onBackCompleted = goBack,
    )

    // Ask for the GPS position the first time the location step opens.
    LaunchedEffect(state.step) {
        if (state.step == ReportStep.Location && state.locationStatus == LocationStatus.Idle) {
            viewModel.onLocating()
            requestLocation()
        }
    }

    val actions = remember(viewModel, photos, requestLocation) {
        NewReportActions(
            onBack = goBack,
            onNext = viewModel::next,
            onSelectCategory = viewModel::selectCategory,
            onSelectPriority = viewModel::selectPriority,
            onTakePhoto = photos.takePhoto,
            onPickPhoto = photos.pickFromGallery,
            onRequestLocation = {
                viewModel.onLocating()
                requestLocation()
            },
            onAddressChange = viewModel::onAddressChange,
            onReferenceChange = viewModel::onReferenceChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onSubmit = viewModel::submit,
        )
    }
    NewReportScreen(state = state, actions = actions, onSeeMap = onSeeMap, modifier = modifier)
}

/** Events of the flow, grouped so the stateless screen keeps a short signature. */
internal class NewReportActions(
    val onBack: () -> Unit,
    val onNext: () -> Unit,
    val onSelectCategory: (ReportCategory) -> Unit,
    val onSelectPriority: (Priority) -> Unit,
    val onTakePhoto: () -> Unit,
    val onPickPhoto: () -> Unit,
    val onRequestLocation: () -> Unit,
    val onAddressChange: (String) -> Unit,
    val onReferenceChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onSubmit: () -> Unit,
)

@Composable
internal fun NewReportScreen(
    state: NewReportUiState,
    actions: NewReportActions,
    onSeeMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = ProfileHeaderHeight, start = 20.dp, end = 20.dp, bottom = 16.dp),
            ) {
                StepTitle(state.step.title)
                Spacer(Modifier.height(20.dp))
                when (state.step) {
                    ReportStep.Category -> CategoryStep(state.category, actions.onSelectCategory)
                    ReportStep.Photo -> PhotoStep(state.photoRef, actions.onTakePhoto, actions.onPickPhoto)
                    ReportStep.Location -> LocationStep(state, actions)
                    ReportStep.Details -> DetailsStep(state, actions)
                    ReportStep.Summary -> SummaryStep(state)
                }
            }
            BottomActionRow(state, actions)
            StepIndicator(current = state.stepNumber, total = ReportStep.entries.size)
        }
        ProfileHeader(onBack = actions.onBack, showLogoBadge = false, modifier = Modifier.align(Alignment.TopCenter))
    }

    if (state.publishedReport != null) {
        PublishedDialog(onSeeMap = onSeeMap)
    }
}

private val ReportStep.title: String
    get() = when (this) {
        ReportStep.Category -> "Selecciona la categoría que corresponde"
        ReportStep.Photo -> "Toma una foto del animalito"
        ReportStep.Location -> "¿En qué lugar se encuentra?"
        ReportStep.Details -> "Cuéntanos qué pasa"
        ReportStep.Summary -> "Revisa tu reporte"
    }

/** Mascot badge plus the step question (Figma: circle with the mascot next to the title). */
@Composable
private fun StepTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(88.dp)
                .dropShadow(FluffyShapes.Ellipse, FluffyShadows.Drop)
                .clip(FluffyShapes.Ellipse)
                .background(FluffyColors.Sage200)
                .border(4.dp, FluffyColors.Cream50, FluffyShapes.Ellipse),
        ) {
            Image(
                painter = painterResource(Res.drawable.img_cat_phone),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = FluffyColors.Brown900,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BottomActionRow(state: NewReportUiState, actions: NewReportActions) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FluffyColors.Peach300, FluffyShapes.Card)
                    .padding(12.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
        if (state.step == ReportStep.Summary) {
            PrimaryButton(
                text = "Subir Reporte",
                enabled = state.canContinue,
                loading = state.isSubmitting,
                onClick = actions.onSubmit,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PrimaryButton(
                text = "Siguiente",
                enabled = state.canContinue,
                onClick = actions.onNext,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

/** Orange call-to-action ("Siguiente", "Subir Reporte"); text stays brown-900 on the color. */
@Composable
internal fun PrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .dropShadow(FluffyShapes.Card, FluffyShadows.Drop)
            .clip(FluffyShapes.Card)
            .background(if (enabled) FluffyColors.Orange500 else FluffySemanticColors.Disabled)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = FluffyColors.Brown900, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
        }
    }
}

/** Bottom band with one dot per step (Figma: pill on the orange wave; past and current dots filled). */
@Composable
private fun StepIndicator(current: Int, total: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(FluffyColors.Orange500, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .dropShadow(FluffyShapes.Pill, FluffyShadows.Drop)
                .background(FluffyColors.Peach100, FluffyShapes.Pill)
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .semantics { contentDescription = "Paso $current de $total" },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(total) { index ->
                val reached = index < current
                val size = if (index == current - 1) 30.dp else 22.dp
                Box(
                    Modifier
                        .size(size)
                        .dropShadow(FluffyShapes.Ellipse, FluffyShadows.Drop)
                        .background(if (reached) FluffyColors.Peach300 else FluffyColors.Cream50, FluffyShapes.Ellipse),
                )
            }
        }
    }
}

/** Modal "¡Reporte publicado!" (Figma "Final", 183:378). */
@Composable
private fun PublishedDialog(onSeeMap: () -> Unit) {
    Dialog(
        onDismissRequest = onSeeMap,
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dropShadow(FluffyShapes.ProfileSheet, FluffyShadows.Drop)
                .background(FluffyColors.Cream50, FluffyShapes.ProfileSheet)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.img_cat_phone),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "¡Reporte publicado!",
                style = MaterialTheme.typography.headlineSmall,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Gracias por ayudar. Los albergues cercanos recibirán tu reporte.",
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Box(Modifier.background(FluffyColors.Orange500, FluffyShapes.Pill).padding(12.dp)) {
                PillActionButton(text = "Ver mapa", icon = painterResource(Res.drawable.ic_map), onClick = onSeeMap)
            }
        }
    }
}

@Preview
@Composable
private fun NewReportScreenPreview() {
    FluffyPawTrackTheme {
        NewReportScreen(
            state = NewReportUiState(category = ReportCategory.Injured, priority = Priority.High),
            actions = NewReportActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}),
            onSeeMap = {},
        )
    }
}
