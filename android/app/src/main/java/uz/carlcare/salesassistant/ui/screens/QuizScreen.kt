package uz.carlcare.salesassistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.carlcare.salesassistant.R
import uz.carlcare.salesassistant.core.data.QuizQuestion
import uz.carlcare.salesassistant.core.design.BrandColors
import uz.carlcare.salesassistant.ui.QuizState
import uz.carlcare.salesassistant.ui.QuizViewModel

private val CorrectGreen = Color(0xFF2E7D32)
private val WrongRed = Color(0xFFC62828)
private val Accent = BrandColors.Tecno

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(viewModel: QuizViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val language = LocalConfiguration.current.locales.get(0)?.language ?: "en"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quiz)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.finished -> QuizResult(
                state = state,
                language = language,
                onRestart = viewModel::restart,
                modifier = Modifier.padding(padding),
            )
            state.questions.isEmpty() -> QuizIntro(
                onStart = viewModel::start,
                modifier = Modifier.padding(padding),
            )
            else -> QuizQuestionPage(
                state = state,
                language = language,
                onAnswer = viewModel::answer,
                onNext = viewModel::next,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun QuizIntro(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Accent.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Quiz, contentDescription = null, tint = Accent, modifier = Modifier.size(44.dp))
        }
        Text(
            text = stringResource(R.string.quiz),
            modifier = Modifier.padding(top = 24.dp),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.quiz_intro_text),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Surface(
            color = Accent.copy(alpha = 0.12f),
            shape = RoundedCornerShape(100.dp),
            modifier = Modifier.padding(top = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.quiz_intro_meta),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
            )
        }
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
        ) {
            Text(stringResource(R.string.quiz_start))
        }
    }
}

@Composable
private fun QuizQuestionPage(
    state: QuizState,
    language: String,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val question = state.current ?: return
    val content = question.content(language)
    val answered = state.selected != null
    val isLast = state.index + 1 >= state.questions.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.quiz_progress, state.index + 1, state.questions.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Surface(color = Accent.copy(alpha = 0.12f), shape = RoundedCornerShape(100.dp)) {
                Text(
                    text = categoryLabel(question.category),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                )
            }
        }
        LinearProgressIndicator(
            progress = { (state.index + 1f) / state.questions.size },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Accent,
            trackColor = Accent.copy(alpha = 0.15f),
        )
        Text(
            text = content.question,
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        content.options.forEachIndexed { index, option ->
            OptionCard(
                index = index,
                text = option,
                chosen = state.selected == index,
                correct = index == content.answer,
                answered = answered,
                onClick = { onAnswer(index) },
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (state.selected != null) {
            val isCorrect = state.selected == content.answer
            FeedbackCard(
                correct = isCorrect,
                explanation = content.explanation,
                modifier = Modifier.padding(top = 16.dp),
            )
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCorrect) CorrectGreen else Accent,
                ),
            ) {
                Text(stringResource(if (isLast) R.string.quiz_show_result else R.string.quiz_next))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OptionCard(
    index: Int,
    text: String,
    chosen: Boolean,
    correct: Boolean,
    answered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = when {
        answered && correct -> CorrectGreen.copy(alpha = 0.12f)
        answered && chosen -> WrongRed.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surface
    }
    val border = when {
        answered && correct -> CorrectGreen
        answered && chosen -> WrongRed
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }
    val textColor = when {
        answered && correct -> CorrectGreen
        answered && chosen -> WrongRed
        answered -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val letterColor = when {
        answered && correct -> CorrectGreen
        answered && chosen -> WrongRed
        else -> Accent
    }

    Card(
        onClick = onClick,
        enabled = !answered,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            disabledContainerColor = container,
        ),
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = letterColor.copy(alpha = 0.14f), shape = CircleShape) {
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = ('A' + index).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = letterColor,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
            if (answered && (correct || chosen)) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (correct) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                    contentDescription = null,
                    tint = if (correct) CorrectGreen else WrongRed,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun FeedbackCard(correct: Boolean, explanation: String, modifier: Modifier = Modifier) {
    val tint = if (correct) CorrectGreen else WrongRed
    Surface(color = tint.copy(alpha = 0.10f), shape = RoundedCornerShape(14.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (correct) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(if (correct) R.string.quiz_correct else R.string.quiz_wrong),
                    style = MaterialTheme.typography.labelLarge,
                    color = tint,
                )
            }
            Text(
                text = explanation,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun QuizResult(
    state: QuizState,
    language: String,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = state.questions.size
    val score = state.score
    val messageRes = when {
        score == total -> R.string.quiz_result_perfect
        score * 10 >= total * 8 -> R.string.quiz_result_good
        score * 10 >= total * 5 -> R.string.quiz_result_fair
        else -> R.string.quiz_result_low
    }
    val mistakes = state.questions.mapIndexedNotNull { index, question ->
        val chosen = state.answers.getOrNull(index) ?: -1
        if (chosen != question.en.answer) question to chosen else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.quiz_score, score, total),
                style = MaterialTheme.typography.displaySmall,
                color = Accent,
            )
            Surface(
                color = Accent.copy(alpha = 0.10f),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(
                    text = stringResource(messageRes),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (mistakes.isEmpty()) {
            Text(
                text = stringResource(R.string.quiz_no_mistakes),
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.quiz_review_title),
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            mistakes.forEach { (question, chosen) ->
                MistakeCard(
                    question = question,
                    chosen = chosen,
                    language = language,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
        ) {
            Text(stringResource(R.string.quiz_restart))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MistakeCard(question: QuizQuestion, chosen: Int, language: String, modifier: Modifier = Modifier) {
    val content = question.content(language)
    val wrongAnswer = content.options.getOrNull(chosen) ?: "—"
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Surface(color = Accent.copy(alpha = 0.12f), shape = RoundedCornerShape(100.dp)) {
                Text(
                    text = categoryLabel(question.category),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                )
            }
            Text(
                text = content.question,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${stringResource(R.string.quiz_your_answer)}: $wrongAnswer",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = WrongRed,
            )
            Text(
                text = "${stringResource(R.string.quiz_correct_answer)}: ${content.options[content.answer]}",
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = CorrectGreen,
            )
            Text(
                text = content.explanation,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun categoryLabel(category: String): String = stringResource(
    when (category) {
        "display" -> R.string.quiz_cat_display
        "platform" -> R.string.quiz_cat_platform
        "memory" -> R.string.quiz_cat_memory
        "camera" -> R.string.quiz_cat_camera
        "battery" -> R.string.quiz_cat_battery
        "connectivity" -> R.string.quiz_cat_connectivity
        "durability" -> R.string.quiz_cat_durability
        "service" -> R.string.quiz_cat_service
        else -> R.string.quiz_cat_catalog
    },
)
