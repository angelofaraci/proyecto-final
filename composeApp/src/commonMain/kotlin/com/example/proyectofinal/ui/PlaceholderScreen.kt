package com.example.proyectofinal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MCard
import org.jetbrains.compose.resources.stringResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.placeholder_default_message

enum class PlaceholderState { Empty, Loading }

@Composable
fun PlaceholderScreen(
    title: String,
    message: String? = null,
    state: PlaceholderState = PlaceholderState.Empty,
    onExploreActivities: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val resolvedMessage = message ?: stringResource(Res.string.placeholder_default_message)
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (state == PlaceholderState.Loading) {
            ActivityLoadingSurface()
        } else {
            ActivityEmptySurface(title, resolvedMessage, onExploreActivities)
        }
    }
}

@Composable
private fun ActivityLoadingSurface() {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("×", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Cargando actividad...",
                modifier = Modifier.padding(start = 18.dp).weight(1f).testTag("activityLoadingHeader"),
                style = MaterialTheme.typography.titleMedium
            )
            Text("•••", color = MaterialTheme.colorScheme.primary)
        }
        MCard(modifier = Modifier.fillMaxWidth().height(180.dp).testTag("placeholderLoadingSkeleton")) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBlock(140.dp, 18.dp, "placeholderLoading")
                SkeletonBlock(220.dp, 12.dp)
                SkeletonBlock(200.dp, 12.dp)
                SkeletonBlock(180.dp, 12.dp)
                SkeletonBlock(220.dp, 72.dp, "placeholderIllustration")
            }
        }
        SkeletonBlock(140.dp, 12.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                Surface(
                    modifier = Modifier.weight(1f).height(96.dp).testTag("activityLoadingOption"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface
                ) {}
            }
        }
        Box(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(52.dp).testTag("placeholderLoadingAction")) {
            Surface(
                modifier = Modifier.fillMaxSize().testTag("activityLoadingConfirm"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
            ) {}
        }
    }
}

@Composable
private fun ActivityEmptySurface(title: String, message: String, onExploreActivities: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("CONTINUAR APRENDIENDO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MCard(modifier = Modifier.fillMaxWidth().testTag("activityEmptyCard")) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(72.dp).testTag("placeholderIllustration"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(modifier = Modifier.testTag("placeholderEmptyArtwork"), contentAlignment = Alignment.Center) {
                        Text("✦", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(title, modifier = Modifier.testTag("activityEmptyTitle"), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                MButton(
                    onClick = onExploreActivities ?: {},
                    enabled = onExploreActivities != null,
                    modifier = Modifier.testTag("placeholderEmptyAction")
                ) { Text("Explorar actividades") }
            }
        }
    }
}

@Composable
private fun SkeletonBlock(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, tag: String? = null) {
    Surface(
        modifier = Modifier.size(width, height).then(if (tag == null) Modifier else Modifier.testTag(tag)),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {}
}
