package com.example.proyectofinal.ui.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.ui.primitives.MCard
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import org.jetbrains.compose.resources.stringResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.theory_sheet_label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheorySheet(
    lesson: Lesson,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        TheorySheetContent(lesson)
    }
}

@Composable
internal fun TheorySheetContent(lesson: Lesson, modifier: Modifier = Modifier) {
    Column(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.theory_sheet_label),
                modifier = Modifier.testTag("theorySheetChapter"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = lesson.title,
                modifier = Modifier.testTag("theorySheetTitle"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TheorySection("Concepto clave", "theorySheetConcept", lesson.theoryContent)
            TheorySteps()
            TheorySection(
                "Ejemplo práctico",
                "theorySheetExample",
                "Usa este ejemplo como guía antes de continuar con la actividad."
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MButton(
                    onClick = {},
                    enabled = false,
                    style = MButtonStyle.Outline,
                    modifier = Modifier.weight(1f).testTag("theorySheetPrevious")
                ) { Text("← Anterior") }
                MButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f).testTag("theorySheetNext")
                ) { Text("Siguiente →") }
            }
    }
}

@Composable
private fun TheorySection(title: String, tag: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        MCard(modifier = Modifier.fillMaxWidth().testTag(tag)) {
            Text(
                text = content,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TheorySteps() {
    Column(
        modifier = Modifier.testTag("theorySheetSteps"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Cómo resolverlo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        listOf(
            "Encuentra los datos importantes.",
            "Aplica el concepto paso a paso.",
            "Comprueba el resultado."
        ).forEachIndexed { index, text ->
            Row(
                modifier = Modifier.testTag("theorySheetStep${index + 1}"),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
                }
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
