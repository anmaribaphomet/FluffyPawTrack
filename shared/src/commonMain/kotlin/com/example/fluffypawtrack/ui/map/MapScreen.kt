package com.example.fluffypawtrack.ui.map

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.platform.PawMap
import com.example.fluffypawtrack.ui.components.ProfileHeader
import com.example.fluffypawtrack.ui.components.ProfileHeaderHeight
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.map_ic_close
import fluffypawtrack.shared.generated.resources.map_ic_filter
import fluffypawtrack.shared.generated.resources.map_ic_search
import org.jetbrains.compose.resources.painterResource
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.Organization
import androidx.compose.foundation.layout.ColumnScope

/**
 * Map screen (Figma MAPA, node 11:277) wired to its ViewModel.
 *
 * @param initialFilter filter applied when the screen opens, e.g. [MapFilter.Adoptions]
 *   when it is opened from the adoption flow.
 * @param reportActions, placeActions extra buttons at the end of each detail sheet
 *   (e.g. "Es mi mascota", "Ver centro"), supplied by the navigation host. Adoption centers
 *   use the place sheet and [placeActions].
 */
@Composable
fun MapRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialFilter: MapFilter = MapFilter.All,
    reportActions: @Composable ColumnScope.(Report) -> Unit = {},
    placeActions: @Composable ColumnScope.(Organization) -> Unit = {},
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        MapViewModel(
            container.reportRepository,
            container.organizationRepository,
            container.adoptionRepository,
            initialFilter,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onFilterSelected = viewModel::onFilterSelected,
        onPinClick = viewModel::onPinClick,
        onDismissDetail = viewModel::onDismissDetail,
        modifier = modifier,
        reportActions = reportActions,
        placeActions = placeActions,
    )
}

@Composable
fun MapScreen(
    state: MapUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterSelected: (MapFilter) -> Unit,
    onPinClick: (String) -> Unit,
    onDismissDetail: () -> Unit,
    modifier: Modifier = Modifier,
    reportActions: @Composable ColumnScope.(Report) -> Unit = {},
    placeActions: @Composable ColumnScope.(Organization) -> Unit = {},
) {
    var filtersOpen by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PawMap(
            pins = state.pins,
            onPinClick = { id ->
                filtersOpen = false
                onPinClick(id)
            },
            onMapClick = {
                filtersOpen = false
                onDismissDetail()
            },
            modifier = Modifier.fillMaxSize(),
        )

        ProfileHeader(onBack = onBack, showLogoBadge = true, modifier = Modifier.align(Alignment.TopCenter))

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = ProfileHeaderHeight + 8.dp, start = 10.dp, end = 10.dp)
                .fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MapSearchField(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                FiltersButton(onClick = { filtersOpen = !filtersOpen })
            }
            if (filtersOpen) {
                Spacer(Modifier.height(8.dp))
                FilterPanel(
                    options = state.filterOptions,
                    selected = state.filter,
                    onSelect = {
                        onFilterSelected(it)
                        filtersOpen = false
                    },
                    modifier = Modifier.align(Alignment.End),
                )
            } else if (state.showNoResults) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No hay resultados en el mapa",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(FluffyColors.Cream50, FluffyShapes.Pill)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = FluffyColors.Brown900,
                )
            }
        }

        state.selected?.let { item ->
            MapDetailSheet(onClose = onDismissDetail, modifier = Modifier.align(Alignment.BottomCenter)) {
                when (item) {
                    is MapItem.Report -> ReportDetailSheetContent(item.report) { reportActions(item.report) }
                    is MapItem.Place -> PlaceDetailSheetContent(item.organization) { placeActions(item.organization) }
                    is MapItem.AdoptionCenter -> PlaceDetailSheetContent(item.organization, pinColor = item.color) {
                        placeActions(item.organization)
                    }
                }
            }
        }
    }
}

@Composable
private fun MapSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val textStyle = MaterialTheme.typography.bodyMedium.copy(color = FluffyColors.Brown900)
    Row(
        modifier = modifier
            .height(40.dp)
            .dropShadow(FluffyShapes.Pill, FluffyShadows.Drop)
            .background(FluffyColors.Cream50, FluffyShapes.Pill)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.map_ic_search),
            contentDescription = null,
            colorFilter = ColorFilter.tint(FluffyColors.Brown900),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("Buscar en el mapa", style = textStyle, color = FluffyColors.Brown600, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(FluffyColors.Brown900),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Image(
                painter = painterResource(Res.drawable.map_ic_close),
                contentDescription = "Borrar búsqueda",
                colorFilter = ColorFilter.tint(FluffyColors.Brown900),
                modifier = Modifier
                    .size(28.dp)
                    .clip(FluffyShapes.Ellipse)
                    .clickable(role = Role.Button) { onQueryChange("") }
                    .padding(5.dp),
            )
        }
    }
}

@Composable
private fun FiltersButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .dropShadow(FluffyShapes.Pill, FluffyShadows.Drop)
            .clip(FluffyShapes.Pill)
            .background(FluffyColors.Sage200)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.map_ic_filter),
            contentDescription = null,
            colorFilter = ColorFilter.tint(FluffyColors.Brown900),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Filtros", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
    }
}

/** Filter panel (Figma Group 27, node 301:684), extended to every RF-02 option. */
@Composable
private fun FilterPanel(
    options: List<MapFilter>,
    selected: MapFilter,
    onSelect: (MapFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(220.dp)
            .dropShadow(FluffyShapes.Card, FluffyShadows.Drop)
            .background(MaterialTheme.colorScheme.surface, FluffyShapes.Card)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FluffyShapes.Card)
                    .background(option.color?.copy(alpha = 0.3f) ?: FluffyColors.Cream100)
                    .then(if (isSelected) Modifier.border(1.5.dp, FluffyColors.Brown900, FluffyShapes.Card) else Modifier)
                    .clickable(role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = option.color
                if (color != null) {
                    Box(Modifier.size(12.dp).background(color, FluffyShapes.Ellipse))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = option.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = FluffyColors.Brown900,
                    textAlign = if (color == null) TextAlign.Center else TextAlign.Start,
                )
            }
        }
    }
}

private val SheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

/** Bottom panel that hosts the detail of the tapped pin. */
@Composable
private fun MapDetailSheet(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(SheetShape, FluffyShadows.Drop)
            .background(FluffyColors.Cream50, SheetShape),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        ) {
            content()
        }
        // 48 dp touch target around the same 36 dp circle (still 8 dp from the sheet's corner).
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(48.dp)
                .clip(FluffyShapes.Ellipse)
                .clickable(role = Role.Button, onClickLabel = "Cerrar", onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.map_ic_close),
                contentDescription = "Cerrar",
                colorFilter = ColorFilter.tint(FluffyColors.Brown900),
                modifier = Modifier
                    .size(36.dp)
                    .clip(FluffyShapes.Ellipse)
                    .background(FluffyColors.Cream100)
                    .padding(8.dp),
            )
        }
    }
}

@Preview
@Composable
private fun MapScreenFilterPreview() {
    FluffyPawTrackTheme {
        FilterPanel(MapFilter.options, selected = MapFilter.All, onSelect = {})
    }
}
