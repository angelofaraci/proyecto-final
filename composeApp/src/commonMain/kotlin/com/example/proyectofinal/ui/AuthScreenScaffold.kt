package com.example.proyectofinal.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.proyectofinal.ui.theme.AppThemeDefaults
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.auth_scaffold_logo_description
import proyectofinal.composeapp.generated.resources.mathimapp_logo

@Composable
internal fun AuthScreenScaffold(
    formTitle: String,
    formSubtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 0.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.Start
            ) {
                AuthBrand()
                Spacer(modifier = Modifier.height(16.dp))
                // Jul 16 handoff: screen title is 27/800 (headlineMedium). The 32/800 in
                // design/tasks predates the handoff update and is intentionally not used.
                Text(
                    text = formTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                content()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AuthBrand() {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    // Brand mark card: 16dp per the Jul 16 handoff, decoupled from the 18dp card token.
                    shape = RoundedCornerShape(AppThemeDefaults.shapes.button)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.mathimapp_logo),
                contentDescription = stringResource(Res.string.auth_scaffold_logo_description),
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.width(11.dp))
        // Handoff wordmark: 22sp/800 with -0.02em tracking, "Mathim" ink + "App" muted/500.
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) { append("Mathim") }
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                ) { append("App") }
            },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 22.sp,
                letterSpacing = (-0.02).em
            ),
            fontWeight = FontWeight.ExtraBold
        )
    }
}
