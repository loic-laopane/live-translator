package com.livetranslator.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livetranslator.app.learn.LearnScreenState
import com.livetranslator.app.learn.LearnUiState
import com.livetranslator.app.learn.LearnViewModel
import com.livetranslator.app.learn.Lesson
import com.livetranslator.app.learn.Level
import com.livetranslator.app.learn.Subject

@Composable
fun LearnScreen(modifier: Modifier = Modifier, vm: LearnViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.screen != LearnScreenState.Subjects) { vm.back() }

    when (val screen = state.screen) {
        LearnScreenState.Subjects -> SubjectsScreen(state, vm, modifier)
        is LearnScreenState.LessonView -> {
            val subject = vm.subject(screen.subjectId)
            val original = vm.lesson(screen.subjectId, screen.lessonId)
            if (subject != null && original != null) {
                LessonScreen(subject, original, state, vm, modifier)
            }
        }
        is LearnScreenState.Quiz -> {
            val original = vm.lesson(screen.subjectId, screen.lessonId)
            if (original != null) QuizScreen(screen, displayed(original, state), vm, modifier)
        }
    }
}

/** The lesson to display: translated when available, unless the user asked for the original. */
private fun displayed(original: Lesson, state: LearnUiState): Lesson {
    val translated = state.translation.lesson
    return if (translated != null && translated.id == original.id && !state.translation.showOriginal) translated else original
}

private fun subjectIcon(id: String): ImageVector = when (id) {
    "francais" -> Icons.AutoMirrored.Filled.MenuBook
    "maths" -> Icons.Default.Calculate
    "anglais" -> Icons.Default.Language
    "physique-chimie" -> Icons.Default.Science
    "svt" -> Icons.Default.Biotech
    "histoire-geo" -> Icons.Default.Public
    "philosophie" -> Icons.Default.Psychology
    else -> Icons.Default.School
}

// ------------------------------------------------------------------ Subjects & lessons list

@Composable
private fun SubjectsScreen(state: LearnUiState, vm: LearnViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Mini-cours et quiz", style = MaterialTheme.typography.headlineSmall)
            Text(
                "De la seconde à la terminale. Lisez le cours puis testez-vous !",
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
        if (state.subjects.isEmpty()) {
            item { CircularProgressIndicator() }
        }
        state.subjects.forEach { subject ->
            val lessons = subject.lessons.filter { state.levelFilter == null || it.level == state.levelFilter }
            if (lessons.isNotEmpty()) {
                item(key = subject.id) {
                    SubjectCard(subject, lessons, state.progress) { lesson -> vm.openLesson(subject.id, lesson.id) }
                }
            }
        }
    }
}

@Composable
private fun SubjectCard(
    subject: Subject,
    lessons: List<Lesson>,
    progress: Map<String, Int>,
    onOpen: (Lesson) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(subjectIcon(subject.id), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(subject.name, style = MaterialTheme.typography.titleLarge)
            }
            lessons.forEach { lesson ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(lesson) }
                        .padding(vertical = 10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(lesson.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            lesson.level.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    progress[lesson.id]?.let { best ->
                        ScoreBadge(best, lesson.quiz.size)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBadge(score: Int, total: Int) {
    val perfect = score == total
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (perfect) Icons.Default.EmojiEvents else Icons.Default.Quiz,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (perfect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text("$score/$total", style = MaterialTheme.typography.labelMedium)
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
            Text("Traduction du cours dans votre langue…", style = MaterialTheme.typography.bodySmall)
        }
        t.failed -> Text(
            "Traduction indisponible (connexion nécessaire pour télécharger le modèle la première fois).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        t.lesson != null -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (t.showOriginal) "Version originale (français)" else "Traduit automatiquement dans votre langue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggle) { Text(if (t.showOriginal) "Voir la traduction" else "Voir l'original") }
        }
    }
}

@Composable
private fun LessonScreen(subject: Subject, original: Lesson, state: LearnUiState, vm: LearnViewModel, modifier: Modifier) {
    val lesson = displayed(original, state)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackRow("${subject.name} · ${original.level.label}") { vm.back() }
        TranslationBanner(state, vm::toggleOriginal)
        Text(lesson.title, style = MaterialTheme.typography.headlineSmall)
        if (lesson.summary.isNotBlank()) {
            Text(lesson.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        lesson.sections.forEach { section ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(section.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(section.body, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Button(
            onClick = { vm.startQuiz(subject.id, original.id) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Quiz, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Faire le quiz (${original.quiz.size} questions)")
        }
    }
}

// ------------------------------------------------------------------ Quiz

@Composable
private fun QuizScreen(quiz: LearnScreenState.Quiz, lesson: Lesson, vm: LearnViewModel, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackRow("Quiz · ${lesson.title}") { vm.back() }
        val total = lesson.quiz.size

        if (quiz.finished) {
            QuizResult(quiz.score, total, onRestart = vm::restartQuiz, onBack = { vm.back() })
        } else {
            QuizQuestionContent(quiz, lesson, total, vm)
        }
    }
}

@Composable
private fun QuizQuestionContent(quiz: LearnScreenState.Quiz, lesson: Lesson, total: Int, vm: LearnViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val question = lesson.quiz[quiz.index]
        LinearProgressIndicator(
            progress = { (quiz.index + if (quiz.selected != null) 1 else 0) / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Question ${quiz.index + 1} / $total", style = MaterialTheme.typography.labelLarge)
        Text(question.question, style = MaterialTheme.typography.titleLarge)

        question.options.forEachIndexed { i, option ->
            val answered = quiz.selected != null
            val isCorrect = i == question.answer
            val isChosen = i == quiz.selected
            val colors = when {
                answered && isCorrect -> CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                answered && isChosen -> CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                else -> CardDefaults.outlinedCardColors()
            }
            OutlinedCard(
                // Stays enabled so the answer colors show; the ViewModel ignores a second answer.
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

        if (quiz.selected != null) {
            val right = quiz.selected == question.answer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (right) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (right) "Bonne réponse !" else "Pas tout à fait…", style = MaterialTheme.typography.titleMedium)
                    if (question.explanation.isNotBlank()) Text(question.explanation, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(onClick = vm::nextQuestion, modifier = Modifier.fillMaxWidth()) {
                Text(if (quiz.index + 1 < total) "Question suivante" else "Voir mon score")
            }
        }
    }
}

@Composable
private fun QuizResult(score: Int, total: Int, onRestart: () -> Unit, onBack: () -> Unit) {
    val ratio = score / total.toFloat()
    val message = when {
        ratio == 1f -> "Parfait ! Tu maîtrises cette leçon."
        ratio >= 0.6f -> "Bien joué ! Relis les points manqués pour viser le sans-faute."
        else -> "Relis le cours, puis retente ta chance !"
    }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text("$score / $total", style = MaterialTheme.typography.displayMedium)
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Recommencer le quiz") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Revoir le cours") }
    }
}
