package com.example.fluffypawtrack.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.components.BottomNavBar
import com.example.fluffypawtrack.ui.components.BottomNavBarHeight
import com.example.fluffypawtrack.ui.components.BottomNavBarSpaceBelowBar
import com.example.fluffypawtrack.ui.components.CardCallToAction
import com.example.fluffypawtrack.ui.components.CardIllustration
import com.example.fluffypawtrack.ui.components.HomeDestination
import com.example.fluffypawtrack.ui.components.HomeHeader
import com.example.fluffypawtrack.ui.components.HomeHeaderHeight
import com.example.fluffypawtrack.ui.components.HouseCard
import com.example.fluffypawtrack.ui.components.HouseCardEars
import com.example.fluffypawtrack.ui.theme.FluffySemanticColors
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_camera
import fluffypawtrack.shared.generated.resources.ic_heart
import fluffypawtrack.shared.generated.resources.img_cat_phone
import fluffypawtrack.shared.generated.resources.img_dog_adopt
import org.jetbrains.compose.resources.painterResource

/** Home wired to [HomeViewModel] (fakes or Supabase, per AppContainer) for the unread badge. */
@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
    onReportAnimal: () -> Unit = {},
    onSeeAdoptions: () -> Unit = {},
    onNavigate: (HomeDestination) -> Unit = {},
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { HomeViewModel(container.notificationRepository, container.authRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        modifier = modifier,
        onProfileClick = onProfileClick,
        onReportAnimal = onReportAnimal,
        onSeeAdoptions = onSeeAdoptions,
        onNavigate = onNavigate,
        unreadNotifications = state.unreadNotifications,
    )
}

/** Pantalla INICIO (Figma nodo 11:69). */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
    onReportAnimal: () -> Unit = {},
    onSeeAdoptions: () -> Unit = {},
    onNavigate: (HomeDestination) -> Unit = {},
    unreadNotifications: Int = 0,
) {
    var selected by remember { mutableStateOf(HomeDestination.Inicio) }
    // The grass block may slide down into its own bottom margin, never below the system navigation bar.
    val systemNavBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navDrop = (BottomNavBarSpaceBelowBar - systemNavBar - NavBarMinClearance).coerceIn(0.dp, MaxNavDrop)

    Box(modifier.fillMaxSize().background(FluffySemanticColors.BackgroundHome)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = HomeHeaderHeight - GreetingLift, bottom = BottomNavBarHeight + 15.dp - navDrop),
        ) {
            Text(
                text = "¡Hola, amante de los\nanimales!",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displaySmall,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
            )
            ReportAnimalCard(onReportAnimal)
            Spacer(Modifier.height(CardGap))
            AdoptionCard(onSeeAdoptions)
        }

        HomeHeader(onProfileClick = onProfileClick, modifier = Modifier.align(Alignment.TopCenter))

        BottomNavBar(
            selected = selected,
            unreadNotifications = unreadNotifications,
            onSelect = {
                selected = it
                onNavigate(it)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = navDrop)
                .height(BottomNavBarHeight),
        )
    }
}

// Space savings so both cards and their buttons sit above the grass on a ~900 dp tall phone.
/** The greeting starts this much higher, still below the header wave (which ends at ~145 dp). */
private val GreetingLift = 18.dp

/** Gap between the two cards (Figma uses 20 dp; the second card's ears already add 47 dp). */
private val CardGap = 8.dp

/** Minimum space kept between the glass bar and the system navigation bar. */
private val NavBarMinClearance = 8.dp

/** How far the grass block may slide down at most. */
private val MaxNavDrop = 24.dp

@Composable
private fun ReportAnimalCard(onClick: () -> Unit) {
    HouseCard(
        ears = HouseCardEars.Soft,
        illustration = CardIllustration(painterResource(Res.drawable.img_cat_phone), 6.dp, 125.dp, 116.dp),
        minBodyHeight = 157.dp,
    ) {
        CardCallToAction(
            title = "¿Encontraste un animalito?",
            description = "Ayúdanos a encontrarle un hogar o un refugio donde pueda estar a salvo",
            actionText = "Reportar un animal",
            actionIcon = painterResource(Res.drawable.ic_camera),
            onAction = onClick,
        )
    }
}

@Composable
private fun AdoptionCard(onClick: () -> Unit) {
    HouseCard(
        ears = HouseCardEars.Pointy,
        illustration = CardIllustration(painterResource(Res.drawable.img_dog_adopt), (-22).dp, 198.dp, 107.dp),
        minBodyHeight = 163.dp,
    ) {
        CardCallToAction(
            title = "¡Dales una\noportunidad!",
            description = "Checa en el mapa los centros de adopción y conoce a los animalitos que esperan una nueva familia",
            actionText = "Ver adopciones",
            actionIcon = painterResource(Res.drawable.ic_heart),
            actionIconSize = 17.dp,
            onAction = onClick,
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    FluffyPawTrackTheme {
        HomeScreen(unreadNotifications = 3)
    }
}
