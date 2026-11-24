package com.example.fluffypawtrack.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.components.ProfileHeader
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffySemanticColors

/** Registration entry point. [onRegistered] runs once the account exists and the session is open. */
@Composable
fun RegisterRoute(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    onHaveAccount: () -> Unit = onBack,
    banner: String? = null,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { RegisterViewModel(container.authRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val registered by rememberUpdatedState(onRegistered)

    LaunchedEffect(state.registered) {
        if (state.registered) {
            registered()
            viewModel.onRegisteredHandled()
        }
    }

    RegisterScreen(
        state = state,
        actions = RegisterActions(
            onFirstNameChange = viewModel::onFirstNameChange,
            onLastNameChange = viewModel::onLastNameChange,
            onEmailChange = viewModel::onEmailChange,
            onPhoneChange = viewModel::onPhoneChange,
            onPasswordChange = viewModel::onPasswordChange,
            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
            onSubmit = viewModel::onSubmit,
            onBack = onBack,
            onHaveAccount = onHaveAccount,
        ),
        banner = banner,
    )
}

/** Events the registration screen sends up. */
class RegisterActions(
    val onFirstNameChange: (String) -> Unit,
    val onLastNameChange: (String) -> Unit,
    val onEmailChange: (String) -> Unit,
    val onPhoneChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onConfirmPasswordChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onBack: () -> Unit,
    /** "Ya tengo cuenta"; defaults to going back to the login. */
    val onHaveAccount: () -> Unit = onBack,
)

/** Registration screen (no Figma frame: built with the app's wave header and cream inputs). */
@Composable
fun RegisterScreen(
    state: RegisterUiState,
    actions: RegisterActions,
    modifier: Modifier = Modifier,
    /** Why an account is needed, e.g. [AFFILIATION_ACCOUNT_BANNER]. */
    banner: String? = null,
) {
    val focusManager = LocalFocusManager.current
    val next = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val enabled = !state.isLoading
    Column(
        modifier
            .fillMaxSize()
            .background(FluffySemanticColors.BackgroundHome)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding(),
    ) {
        ProfileHeader(onBack = actions.onBack)
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Crear cuenta",
                modifier = Modifier.fillMaxWidth().semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                color = FluffyColors.Brown900,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Con una cuenta puedes dar seguimiento a tus reportes, guardar favoritos y solicitar una adopción.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
            )
            if (banner != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = banner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FluffyColors.Cream50, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FluffyColors.Brown900,
                )
            }
            Spacer(Modifier.height(16.dp))
            AuthCard {
                AuthTextField(
                    value = state.firstName,
                    onValueChange = actions.onFirstNameChange,
                    label = "Nombre",
                    capitalization = KeyboardCapitalization.Words,
                    keyboardActions = next,
                    enabled = enabled,
                )
                AuthTextField(
                    value = state.lastName,
                    onValueChange = actions.onLastNameChange,
                    label = "Apellidos",
                    capitalization = KeyboardCapitalization.Words,
                    keyboardActions = next,
                    enabled = enabled,
                )
                AuthTextField(
                    value = state.email,
                    onValueChange = actions.onEmailChange,
                    label = "Correo electrónico",
                    keyboardType = KeyboardType.Email,
                    keyboardActions = next,
                    enabled = enabled,
                )
                AuthTextField(
                    value = state.phone,
                    onValueChange = actions.onPhoneChange,
                    label = "Teléfono (opcional)",
                    helperText = "Lo necesitarás más adelante para enviar o recibir solicitudes de contacto.",
                    keyboardType = KeyboardType.Phone,
                    keyboardActions = next,
                    enabled = enabled,
                )
                AuthTextField(
                    value = state.password,
                    onValueChange = actions.onPasswordChange,
                    label = "Contraseña",
                    helperText = "Mínimo $MIN_PASSWORD_LENGTH caracteres",
                    isPassword = true,
                    keyboardActions = next,
                    enabled = enabled,
                )
                AuthTextField(
                    value = state.confirmPassword,
                    onValueChange = actions.onConfirmPasswordChange,
                    label = "Confirmar contraseña",
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        actions.onSubmit()
                    }),
                    enabled = enabled,
                )
                state.errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    AuthErrorMessage(it)
                }
                Spacer(Modifier.height(16.dp))
                AuthPrimaryButton(text = "Crear cuenta", onClick = actions.onSubmit, isLoading = state.isLoading)
                TextButton(
                    onClick = actions.onHaveAccount,
                    enabled = enabled,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                ) {
                    Text("Ya tengo cuenta", style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
                }
            }
        }
    }
}

@Preview
@Composable
private fun RegisterScreenPreview() {
    FluffyPawTrackTheme {
        RegisterScreen(
            state = RegisterUiState(
                firstName = "Ana",
                email = "ana@correo",
                errorMessage = INVALID_DATA_MESSAGE,
            ),
            actions = RegisterActions({}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
