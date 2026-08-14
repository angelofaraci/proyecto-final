package com.example.proyectofinal.ui.teacher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import com.example.proyectofinal.ui.primitives.MProgressIndicator
import com.example.proyectofinal.ui.primitives.MTextField
import org.jetbrains.compose.resources.stringResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.teacher_action_create
import proyectofinal.composeapp.generated.resources.teacher_action_logout
import proyectofinal.composeapp.generated.resources.teacher_courses_empty
import proyectofinal.composeapp.generated.resources.teacher_courses_title
import proyectofinal.composeapp.generated.resources.teacher_create_description
import proyectofinal.composeapp.generated.resources.teacher_create_title
import proyectofinal.composeapp.generated.resources.teacher_dashboard_title
import proyectofinal.composeapp.generated.resources.teacher_progress_empty
import proyectofinal.composeapp.generated.resources.teacher_error
import proyectofinal.composeapp.generated.resources.teacher_action_retry
import proyectofinal.composeapp.generated.resources.teacher_student_metrics

@Composable
fun TeacherDashboardScreen(
    viewModel: TeacherDashboardViewModel,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.teacher_dashboard_title), style = MaterialTheme.typography.headlineMedium)
                MButton(onClick = onLogout, style = MButtonStyle.Outline) {
                    Text(stringResource(Res.string.teacher_action_logout))
                }
            }
        }
        item {
            MTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(Res.string.teacher_create_title)) },
                singleLine = true
            )
        }
        item {
            MTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(Res.string.teacher_create_description)) }
            )
        }
        item {
            MButton(
                onClick = {
                    viewModel.createCourse(title, description)
                    title = ""
                    description = ""
                },
                enabled = title.isNotBlank() && description.isNotBlank() && !state.isCreating,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(Res.string.teacher_action_create)) }
        }
        item { Text(stringResource(Res.string.teacher_courses_title), style = MaterialTheme.typography.titleLarge) }
        if (state.isLoading) item { MProgressIndicator() }
        state.errorMessage?.let {
            item {
                Column {
                    Text(stringResource(Res.string.teacher_error), color = MaterialTheme.colorScheme.error)
                    MButton(onClick = viewModel::refresh, style = MButtonStyle.Outline) {
                        Text(stringResource(Res.string.teacher_action_retry))
                    }
                }
            }
        }
        if (!state.isLoading && state.courses.isEmpty()) {
            item { Text(stringResource(Res.string.teacher_courses_empty)) }
        }
        items(state.courses, key = { it.id }) { course ->
            Card(Modifier.fillMaxWidth().clickable { viewModel.selectCourse(course.id) }) {
                Column(Modifier.padding(16.dp)) {
                    Text(course.title, style = MaterialTheme.typography.titleMedium)
                    Text(course.description)
                }
            }
        }
        state.selectedCourseProgress?.let { roster ->
            item { Text(roster.courseTitle, style = MaterialTheme.typography.titleLarge) }
            if (roster.students.isEmpty()) item { Text(stringResource(Res.string.teacher_progress_empty)) }
            items(roster.students, key = { it.studentId }) { student ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(student.studentName, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(
                            Res.string.teacher_student_metrics,
                            student.completedLessons,
                            student.completedExercises,
                            student.progressPercentage
                        ))
                    }
                }
            }
        }
    }
}
