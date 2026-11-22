package com.example.fluffypawtrack.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.ui.components.DesignFrame
import com.example.fluffypawtrack.ui.components.placeAt
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.img_logo_pin
import fluffypawtrack.shared.generated.resources.img_logo_wordmark
import org.jetbrains.compose.resources.painterResource

private val AuthCardShape = RoundedCornerShape(24.dp)

/** Wave header with the logo and no actions, for screens shown before signing in. */
@Composable
internal fun AuthHeader(modifier: Modifier = Modifier) {
    // Same geometry as HomeHeader, without the profile button.
    DesignFrame(height = 155.dp, modifier = modifier) {
        Box(
            Modifier
                .placeAt((-32).dp, 34.dp, 436.dp, 103.dp)
                .dropShadow(FluffyShapes.HeaderFront, FluffyShadows.Drop)
                .background(FluffyColors.Cream50, FluffyShapes.HeaderFront),
        )
        Box(
            Modifier
                .placeAt((-18.36).dp, 42.38.dp, 445.76.dp, 103.dp)
                .graphicsLayer { rotationZ = -5f }
                .dropShadow(FluffyShapes.HeaderBack, FluffyShadows.Drop)
                .background(FluffyColors.Cream50, FluffyShapes.HeaderBack),
        )
        Box(
            Modifier
                .placeAt(22.dp, 54.dp, 60.dp, 57.dp)
                .dropShadow(FluffyShapes.Ellipse, FluffyShadows.Drop)
                .background(FluffyColors.Orange500, FluffyShapes.Ellipse),
        )
        Image(
            painter = painterResource(Res.drawable.img_logo_pin),
            contentDescription = null,
            modifier = Modifier.placeAt(12.dp, 50.dp, 83.dp, 65.dp),
        )
        Image(
            painter = painterResource(Res.drawable.img_logo_wordmark),
            contentDescription = "Fluffy PawTrack",
            modifier = Modifier.placeAt(69.dp, 73.dp, 185.dp, 42.dp),
        )
    }
}

/** White card that groups a form on the peach background. */
@Composable
internal fun AuthCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .dropShadow(AuthCardShape, FluffyShadows.Drop)
            .background(MaterialTheme.colorScheme.surface, AuthCardShape)
            .padding(20.dp),
        content = content,
    )
}

/**
 * Labeled text field in the cream input style. Password fields get a "Mostrar / Ocultar"
 * toggle; [error] replaces [helperText] and is announced by screen readers.
 */
@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    helperText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isPassword: Boolean = false,
    enabled: Boolean = true,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val supporting = error ?: helperText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (error != null) error(error) },
        enabled = enabled,
        singleLine = true,
        isError = error != null,
        textStyle = MaterialTheme.typography.bodyMedium,
        supportingText = supporting?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "Ocultar" else "Mostrar",
                        style = MaterialTheme.typography.labelLarge,
                        color = FluffyColors.Brown900,
                    )
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            capitalization = capitalization,
            autoCorrectEnabled = !isPassword && keyboardType == KeyboardType.Text,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = FluffyColors.Brown900,
            unfocusedTextColor = FluffyColors.Brown900,
            errorTextColor = FluffyColors.Brown900,
            focusedContainerColor = FluffyColors.Cream100,
            unfocusedContainerColor = FluffyColors.Cream100,
            errorContainerColor = FluffyColors.Cream100,
            disabledContainerColor = FluffyColors.Cream100,
            cursorColor = FluffyColors.Brown900,
            errorCursorColor = FluffyColors.Brown900,
            focusedBorderColor = FluffyColors.Orange700,
            unfocusedBorderColor = FluffyColors.Sage700,
            errorBorderColor = FluffyColors.Orange700,
            focusedLabelColor = FluffyColors.Brown900,
            unfocusedLabelColor = FluffyColors.Brown900,
            errorLabelColor = FluffyColors.Brown900,
            focusedSupportingTextColor = FluffyColors.Brown900,
            unfocusedSupportingTextColor = FluffyColors.Brown900,
            errorSupportingTextColor = FluffyColors.Brown900,
        ),
    )
}

/** Form-level error (for example, wrong credentials), announced when it appears. */
@Composable
internal fun AuthErrorMessage(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier
            .fillMaxWidth()
            .background(FluffyColors.Peach300, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium,
        color = FluffyColors.Brown900,
    )
}

/** Orange pill button for the main action of a form. */
@Composable
internal fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = FluffyShapes.Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = FluffyColors.Orange500,
            contentColor = FluffyColors.Brown900,
            disabledContainerColor = FluffyColors.Orange500,
            disabledContentColor = FluffyColors.Brown900,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = FluffyColors.Brown900,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
