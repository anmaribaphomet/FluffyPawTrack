package com.example.fluffypawtrack.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluffypawtrack.ui.components.DesignFrame
import com.example.fluffypawtrack.ui.components.ProfileHeader
import com.example.fluffypawtrack.ui.components.ProfileOptionRow
import com.example.fluffypawtrack.ui.components.ProfileStatCard
import com.example.fluffypawtrack.ui.components.placeAt
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_person_outline
import fluffypawtrack.shared.generated.resources.ic_report
import fluffypawtrack.shared.generated.resources.ic_star
import fluffypawtrack.shared.generated.resources.ic_user
import org.jetbrains.compose.resources.painterResource

/** Datos que muestra la pantalla de perfil. */
data class ProfileUser(
    val name: String,
    val email: String,
    val reports: Int,
    val attended: Int,
    val favorites: Int,
) {
    companion object {
        val Placeholder = ProfileUser("Nombre de Usuario", "usuario@correo.com", reports = 1, attended = 0, favorites = 1)
    }
}

/** Account screen wired to the signed-in user's data. */
@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onPersonalInfo: () -> Unit,
    onMyReports: () -> Unit,
    onFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        ProfileViewModel(container.authRepository, container.reportRepository, container.favoritesRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        onBack = onBack,
        modifier = modifier,
        user = state.toProfileUser(),
        onPersonalInfo = onPersonalInfo,
        onMyReports = onMyReports,
        onFavorites = onFavorites,
    )
}

/** Pantalla GESTION DE CUENTA (Figma nodo 11:228). */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    user: ProfileUser = ProfileUser.Placeholder,
    onPersonalInfo: () -> Unit = {},
    onMyReports: () -> Unit = {},
    onFavorites: () -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ProfileBackdrop()

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 168.dp, bottom = 220.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileAvatar()
            Spacer(Modifier.height(9.dp))
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
            )
            Text(user.email, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.width(310.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                ProfileStatCard(user.reports.twoDigits(), "Reportes")
                ProfileStatCard(user.attended.twoDigits(), "Atendidos")
                ProfileStatCard(user.favorites.twoDigits(), "Favoritos")
            }
            Spacer(Modifier.height(17.dp))
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                ProfileOptionRow(
                    icon = painterResource(Res.drawable.ic_person_outline),
                    title = "Información Personal",
                    subtitle = "Nombre, Correo y Teléfono",
                    onClick = onPersonalInfo,
                    iconSize = 22.dp,
                )
                ProfileOptionRow(
                    icon = painterResource(Res.drawable.ic_report),
                    title = "Mis reportes",
                    subtitle = "Consulta el estado de tus casos",
                    onClick = onMyReports,
                )
                ProfileOptionRow(
                    icon = painterResource(Res.drawable.ic_star),
                    title = "Mis Favoritos",
                    subtitle = "Animales y publicaciones guardados",
                    onClick = onFavorites,
                    iconSize = 25.dp,
                )
            }
        }

        ProfileHeader(onBack = onBack, modifier = Modifier.align(Alignment.TopCenter))
    }
}

/** Fondo: hoja blanca central y ondas naranjas arriba y abajo. */
@Composable
private fun ProfileBackdrop() {
    Box(Modifier.fillMaxSize().padding(top = 114.dp, bottom = 72.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .requiredWidth(359.dp)
                .fillMaxHeight()
                .dropShadow(FluffyShapes.ProfileSheet, FluffyShadows.Drop)
                .background(FluffyColors.White, FluffyShapes.ProfileSheet),

        )
    }
    DesignFrame(height = 205.dp) {
        Box(
            Modifier
                .placeAt((-19).dp, (-37).dp, 440.dp, 242.dp)
                .dropShadow(FluffyShapes.TangerineDripTop, FluffyShadows.Drop)
                .background(FluffyColors.Orange500, FluffyShapes.TangerineDripTop)
                .border(1.dp, FluffyColors.Peach300, FluffyShapes.TangerineDripTop),
        )
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        DesignFrame(height = 165.dp) {
            Box(
                Modifier
                    .placeAt((-9).dp, 0.dp, 440.dp, 242.dp)
                    .background(FluffyColors.Orange500, FluffyShapes.TangerineDripBottom)
                    .innerShadow(FluffyShapes.TangerineDripBottom, FluffyShadows.Inner)
                    .border(1.dp, FluffyColors.Peach300, FluffyShapes.TangerineDripBottom),
            )
        }
    }
}

@Composable
private fun ProfileAvatar() {
    Box(
        Modifier
            .size(80.dp, 85.dp)
            .background(FluffyColors.Brown300, FluffyShapes.Ellipse)
            .innerShadow(FluffyShapes.Ellipse, FluffyShadows.Inner),
        contentAlignment = Alignment.TopStart,

    ) {
        // Posición tomada de Figma (el dibujo del PNG no está centrado en su lienzo).
        Image(
            painter = painterResource(Res.drawable.ic_user),
            contentDescription = "Foto de perfil",
            modifier = Modifier.offset(10.dp, 10.dp).size(60.dp),
        )
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')

@Preview
@Composable
private fun ProfileScreenPreview() {
    FluffyPawTrackTheme {
        ProfileScreen(onBack = {})
    }
}
