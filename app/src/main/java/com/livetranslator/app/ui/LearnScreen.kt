package com.livetranslator.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livetranslator.app.UserSettings
import com.livetranslator.app.learn.Course
import com.livetranslator.app.learn.Example
import com.livetranslator.app.learn.Exercise
import com.livetranslator.app.learn.LearnScreenState
import com.livetranslator.app.learn.LearnUiState
import com.livetranslator.app.learn.LearnViewModel
import com.livetranslator.app.learn.Lesson
import com.livetranslator.app.learn.Level
import com.livetranslator.app.learn.Speaker

@Composable
fun LearnScreen(modifier: Modifier = Modifier, vm: LearnViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    val speak: (String, String) -> Unit = { text, lang ->
        if (!speaker.speak(text, lang)) {
            Toast.makeText(context, "Voix non installée pour cette langue (Paramètres › Synthèse vocale)", Toast.LENGTH_LONG).show()
        }
    }

    BackHandler(enabled = state.screen != LearnScreenState.Courses) { vm.back() }

    when (val screen = state.screen) {
        LearnScreenState.Courses -> CoursesScreen(state, vm, modifier)
        is LearnScreenState.LessonView -> {
            val course = vm.course(screen.courseId)
            val original = vm.lesson(screen.courseId, screen.lessonId)
            if (course != null && original != null) LessonScreen(course, original, state, vm, speak, modifier)
        }
        is LearnScreenState.Exercises -> {
            val course = vm.course(screen.courseId)
            val original = vm.lesson(screen.courseId, screen.lessonId)
            if (course != null && original != null) {
                ExercisesScreen(screen, course, displayed(original, state), vm, speak, modifier)
            }
        }
    }
}

/** The lesson to display: with translated explanations when available, unless the original is asked. */
private fun displayed(original: Lesson, state: LearnUiState): Lesson {
    val translated = state.translation.lesson
    return if (translated != null && translated.id == original.id && !state.translation.showOriginal) translated else original
}

// ------------------------------------------------------------------ Courses list

@Composable
private fun CoursesScreen(state: LearnUiState, vm: LearnViewModel, modifier: Modifier) {
    val native by UserSettings.nativeLanguage.collectAsState()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Cours de langues", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Une règle, des exemples à écouter, puis des exercices pour s'entraîner.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.levelFilter == null,
                        onClick = { vm.setLevelFilter(null) },
                        label = { Text("Tous niveaux") },
                    )
                }
                items(Level.entries) { level ->
                    FilterChip(
                        selected = state.levelFilter == level,
                        onClick = { vm.setLevelFilter(level) },
                        label = { Text(level.label) },
                    )
                }
            }
        }
        if (state.courses.isEmpty()) item { CircularProgressIndicator() }
        // No point in offering a course of the user's own native language.
        state.courses.filter { it.language != native }.forEach { course ->
            val lessons = course.lessons.filter { state.levelFilter == null || it.level == state.levelFilter }
            if (lessons.isNotEmpty()) {
                item(key = course.id) {
                    CourseCard(course, lessons, state.progress) { lesson -> vm.openLesson(course.id, lesson.id) }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course, lessons: List<Lesson>, progress: Map<String, Int>, onOpen: (Lesson) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(course.name, style = MaterialTheme.typography.titleLarge)
            }
            lessons.forEach { lesson ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(lesson) }.padding(vertical = 10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(lesson.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(lesson.level.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    progress[lesson.id]?.let { best ->
                        val perfect = best == lesson.exercises.size
                        Icon(
                            if (perfect) Icons.Default.EmojiEvents else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (perfect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("$best/${lesson.exercises.size}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Lesson

@Composable
private fun BackRow(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TranslationBanner(state: LearnUiState, onToggle: () -> Unit) {
    val t = state.translation
    when {
        t.translating -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Traduction des explications dans votre langue…", style = MaterialTheme.typography.bodySmall)
        }
        t.failed -> Text(
            "Traduction indisponible (connexion nécessaire pour télécharger le modèle la première fois).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        t.lesson != null -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (t.showOriginal) "Explications en version originale (français)" else "Explications traduites dans votre langue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggle) { Text(if (t.showOriginal) "Traduire" else "Voir l'original") }
        }
    }
}

@Composable
private fun SpeakButton(text: String, language: String, speak: (String, String) -> Unit) {
    IconButton(onClick = { speak(text, language) }) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Écouter", tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ExampleRow(example: Example, language: String, speak: (String, String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(example.text, style = MaterialTheme.typography.titleMedium)
            example.pronunciation?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (example.translation.isNotBlank()) {
                Text(
                    example.translation,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SpeakButton(example.text, language, speak)
    }
}

@Composable
private fun LessonScreen(
    course: Course,
    original: Lesson,
    state: LearnUiState,
    vm: LearnViewModel,
    speak: (String, String) -> Unit,
    modifier: Modifier,
) {
    val lesson = displayed(original, state)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackRow("${course.name} · ${original.level.label}") { vm.back() }
        TranslationBanner(state, vm::toggleOriginal)
        Text(lesson.title, style = MaterialTheme.typography.headlineSmall)
        if (lesson.summary.isNotBlank()) {
            Text(lesson.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        lesson.sections.forEach { section ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(section.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    if (section.body.isNotBlank()) Text(section.body, style = MaterialTheme.typography.bodyLarge)
                    if (section.examples.isNotEmpty()) {
                        HorizontalDivider()
                        section.examples.forEach { ExampleRow(it, course.language, speak) }
                    }
                }
            }
        }
        Button(onClick = { vm.startExercises(course.id, original.id) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("S'entraîner (${original.exercises.size} exercices)")
        }
    }
}

// ------------------------------------------------------------------ Exercises

@Composable
private fun ExercisesScreen(
    ex: LearnScreenState.Exercises,
    course: Course,
    lesson: Lesson,
    vm: LearnViewModel,
    speak: (String, String) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackRow("Exercices · ${lesson.title}") { vm.back() }
        val total = lesson.exercises.size
        if (ex.finished) {
            ExercisesResult(ex.score, total, onRestart = vm::restartExercises, onBack = { vm.back() })
        } else {
            ExerciseContent(ex, course, lesson.exercises[ex.index], total, vm, speak)
        }
    }
}

@Composable
private fun ExerciseContent(
    ex: LearnScreenState.Exercises,
    course: Course,
    exercise: Exercise,
    total: Int,
    vm: LearnViewModel,
    speak: (String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LinearProgressIndicator(
            progress = { (ex.index + if (ex.correct != null) 1 else 0) / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Exercice ${ex.index + 1} / $total", style = MaterialTheme.typography.labelLarge)
        Text(exercise.prompt, style = MaterialTheme.typography.titleMedium)
        if (exercise.text.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.text, style = MaterialTheme.typography.headlineSmall)
                    exercise.pronunciation?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                // Read the sentence without the blank marker.
                SpeakButton(exercise.text.replace("___", "…"), course.language, speak)
            }
        }

        when (exercise) {
            is Exercise.Choice -> ChoiceOptions(ex, exercise, vm)
            is Exercise.Input -> InputAnswer(ex, exercise, vm)
        }

        if (ex.correct != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (ex.correct) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (ex.correct) "Bonne réponse !" else "Pas tout à fait…", style = MaterialTheme.typography.titleMedium)
                    if (!ex.correct && exercise is Exercise.Input) {
                        Text("Réponse attendue : ${exercise.answers.first()}", fontWeight = FontWeight.SemiBold)
                    }
                    if (exercise.explanation.isNotBlank()) Text(exercise.explanation, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(onClick = vm::next, modifier = Modifier.fillMaxWidth()) {
                Text(if (ex.index + 1 < total) "Exercice suivant" else "Voir mon score")
            }
        }
    }
}

@Composable
private fun ChoiceOptions(ex: LearnScreenState.Exercises, exercise: Exercise.Choice, vm: LearnViewModel) {
    exercise.options.forEachIndexed { i, option ->
        val answered = ex.correct != null
        val isCorrect = i == exercise.answer
        val isChosen = i == ex.selected
        val colors = when {
            answered && isCorrect -> CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            answered && isChosen -> CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            else -> CardDefaults.outlinedCardColors()
        }
        // Stays enabled so the answer colors show; the ViewModel ignores a second answer.
        OutlinedCard(
            onClick = { vm.selectOption(i) },
            colors = colors,
            border = if (answered && (isCorrect || isChosen)) {
                BorderStroke(2.dp, if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            } else {
                CardDefaults.outlinedCardBorder(enabled = true)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(option, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (answered && isCorrect) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Bonne réponse", tint = MaterialTheme.colorScheme.primary)
                } else if (answered && isChosen) {
                    Icon(Icons.Default.Cancel, contentDescription = "Mauvaise réponse", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun InputAnswer(ex: LearnScreenState.Exercises, exercise: Exercise.Input, vm: LearnViewModel) {
    var typed by rememberSaveable(ex.lessonId, ex.index) { mutableStateOf("") }
    val answered = ex.correct != null
    OutlinedTextField(
        value = if (answered) ex.typed.orEmpty() else typed,
        onValueChange = { if (!answered) typed = it },
        readOnly = answered,
        isError = ex.correct == false,
        label = { Text("Votre réponse") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { vm.submitAnswer(typed) }),
        modifier = Modifier.fillMaxWidth(),
    )
    if (!answered) {
        Button(onClick = { vm.submitAnswer(typed) }, enabled = typed.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Valider")
        }
    }
}

@Composable
private fun ExercisesResult(score: Int, total: Int, onRestart: () -> Unit, onBack: () -> Unit) {
    val ratio = score / total.toFloat()
    val message = when {
        ratio == 1f -> "Parfait ! Tu maîtrises cette leçon."
        ratio >= 0.6f -> "Bien joué ! Relis les points manqués pour viser le sans-faute."
        else -> "Relis la leçon et écoute les exemples, puis réessaie !"
    }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("$score / $total", style = MaterialTheme.typography.displayMedium)
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Recommencer") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Revoir la leçon") }
    }
}
