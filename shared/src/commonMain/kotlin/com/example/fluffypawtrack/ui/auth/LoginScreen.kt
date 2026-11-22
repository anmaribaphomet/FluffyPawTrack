package com.example.fluffypawtrack.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.components.PillActionButton
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffySemanticColors
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_camera
import org.jetbrains.compose.resources.painterResource

/** Login entry point. The parent wires each callback to a navigation action. */
@Composable
fun LoginRoute(
    onSignedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onReportAnonymously: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LoginViewModel(container.authRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val signedIn by rememberUpdatedState(onSignedIn)
    val anonymous by rememberUpdatedState(onReportAnonymously)

    LaunchedEffect(state.result) {
        when (state.result) {
            LoginResult.SignedIn -> signedIn()
            LoginResult.Anonymous -> anonymous()
            null -> return@LaunchedEffect
        }
        viewModel.onResultHandled()
    }

    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
        onCreateAccount = onCreateAccount,
        onReportAnonymously = viewModel::onReportAnonymously,
    )
}

/** Login screen (no Figma frame: built with the app's wave header, cream inputs and pill buttons). */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCreateAccount: () -> Unit,
    onReportAnonymously: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier
            .fillMaxSize()
            .background(FluffySemanticColors.BackgroundHome)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding(),
    ) {
        AuthHeader()
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Iniciar sesión",
                modifier = Modifier.fillMaxWidth().semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                color = FluffyColors.Brown900,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Entra para dar seguimiento a tus reportes y guardar favoritos.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
            )
            Spacer(Modifier.height(16.dp))
            AuthCard {
                AuthTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = "Correo electrónico",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    enabled = !state.isLoading,
                )
                Spacer(Modifier.height(8.dp))
                AuthTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = "Contraseña",
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onSubmit()
                    }),
                    enabled = !state.isLoading,
                )
                state.errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    AuthErrorMessage(it)
                }
                Spacer(Modifier.height(16.dp))
                AuthPrimaryButton(text = "Iniciar sesión", onClick = onSubmit, isLoading = state.isLoading)
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("¿No tienes cuenta?", style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
                    TextButton(onClick = onCreateAccount, enabled = !state.isLoading) {
                        Text("Crear cuenta", style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "¿Viste un animal que necesita ayuda? Puedes reportarlo sin crear una cuenta.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            PillActionButton(
                text = "Reportar sin cuenta",
                icon = painterResource(Res.drawable.ic_camera),
                onClick = { if (!state.isLoading) onReportAnonymously() },
            )
            Spacer(Modifier.height(24.dp))
            // Demo build only: the fake repository accepts this account.
            Text(
                text = "Cuenta de prueba: demo@correo.com / demo1234",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    FluffyPawTrackTheme {
        LoginScreen(
            state = LoginUiState(email = "demo@correo.com", errorMessage = "Correo o contraseña incorrectos"),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onCreateAccount = {},
            onReportAnonymously = {},
        )
    }
}
