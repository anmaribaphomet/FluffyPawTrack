package com.example.fluffypawtrack.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import com.example.fluffypawtrack.ui.theme.FluffySemanticColors
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.img_logo_pin
import fluffypawtrack.shared.generated.resources.img_logo_wordmark
import org.jetbrains.compose.resources.painterResource

/** Splash entry point: plays the loading animation, then reports whether a user is signed in. */
@Composable
fun SplashRoute(onFinished: (isSignedIn: Boolean) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { SplashViewModel(container.authRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val finished by rememberUpdatedState(onFinished)

    LaunchedEffect(state.isSignedIn) {
        state.isSignedIn?.let { finished(it) }
    }

    SplashScreen()
}

/** Loading screen (Figma "PAGINA DE CARGA CON LOGO" 1:4 and "EJEMPLO CARGAndo" frames). */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }
    Box(
        modifier
            .fillMaxSize()
            .background(FluffySemanticColors.BackgroundHome)
            .semantics(mergeDescendants = true) { contentDescription = "Fluffy PawTrack. Cargando" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = entrance.value
                    val scale = 0.85f + 0.15f * entrance.value
                    scaleX = scale
                    scaleY = scale
                },
            ) {
                val pin = painterResource(Res.drawable.img_logo_pin)
                Image(
                    painter = pin,
                    contentDescription = null,
                    modifier = Modifier.width(200.dp).aspectRatio(pin.intrinsicSize.aspectRatioOr(83f / 65f)),
                )
                Spacer(Modifier.height(8.dp))
                val wordmark = painterResource(Res.drawable.img_logo_wordmark)
                Image(
                    painter = wordmark,
                    contentDescription = null,
                    modifier = Modifier.width(300.dp).aspectRatio(wordmark.intrinsicSize.aspectRatioOr(185f / 42f)),
                )
            }
            Spacer(Modifier.height(48.dp))
            LoadingDots()
        }
    }
}

private fun Size.aspectRatioOr(fallback: Float): Float =
    if (width > 0f && height > 0f && width.isFinite() && height.isFinite()) width / height else fallback

/** Three dots that pulse in turn, like the "EJEMPLO CARGAndo" frames. */
@Composable
private fun LoadingDots() {
    val transition = rememberInfiniteTransition(label = "loading")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0.3f at 0 using LinearEasing
                        1f at 300 using LinearEasing
                        0.3f at 600 using LinearEasing
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * 200),
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .size(12.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(FluffyColors.Brown900, FluffyShapes.Ellipse),
            )
        }
    }
}

@Preview
@Composable
private fun SplashScreenPreview() {
    FluffyPawTrackTheme {
        SplashScreen()
    }
}
