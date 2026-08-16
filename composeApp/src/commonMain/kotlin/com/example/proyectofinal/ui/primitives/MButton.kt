package com.example.proyectofinal.ui.primitives

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.ui.theme.AppThemeDefaults
import com.example.proyectofinal.ui.theme.BrandCoralShadow

enum class MButtonStyle {
    Filled,
    Outline,
    Social
}

@Composable
fun MButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: MButtonStyle = MButtonStyle.Filled,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable RowScope.() -> Unit
) {
    val buttonModifier = modifier
        .heightIn(min = 56.dp)
        .alpha(if (enabled) 1f else 0.5f)
    when (style) {
        MButtonStyle.Filled -> Button(
            onClick = onClick,
            modifier = buttonModifier.shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = BrandCoralShadow,
                spotColor = BrandCoralShadow
            ),
            enabled = enabled,
            shape = shape,
            contentPadding = contentPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            content = content
        )

        MButtonStyle.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            contentPadding = contentPadding,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.primary
            ),
            content = content
        )

        MButtonStyle.Social -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            // Redesign handoff: social buttons use the 14dp token, not the 16dp button radius.
            shape = RoundedCornerShape(AppThemeDefaults.shapes.socialButton),
            contentPadding = contentPadding,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface
            ),
            content = content
        )
    }
}
