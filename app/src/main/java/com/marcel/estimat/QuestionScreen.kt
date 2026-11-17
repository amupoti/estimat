package com.marcel.estimat

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marcel.estimat.model.Question
import com.marcel.estimat.model.QuestionBank
import com.marcel.estimat.model.QuestionLanguage
import com.marcel.estimat.model.formatAnswer
import com.marcel.estimat.ui.AppUiState

@Composable
fun QuestionScreen(
    state: AppUiState,
    onDrawQuestion: () -> Unit,
    onRevealAnswer: () -> Unit,
    onRestartDeck: () -> Unit,
    onFunnyOnlyChange: (Boolean) -> Unit,
    sessionActions: SessionActions,
) {
    val bank = state.bank
    Column(modifier = Modifier.fillMaxSize()) {
        if (bank != null) {
            SessionBar(
                state = state,
                actions = sessionActions,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
            )
            val funnyOnly = state.filter.tones == setOf(QuestionBank.TAG_FUNNY)
            FilterChip(
                selected = funnyOnly,
                onClick = { onFunnyOnlyChange(!funnyOnly) },
                label = { Text(stringResource(R.string.funny_only)) },
                leadingIcon = if (funnyOnly) {
                    { Icon(Icons.Rounded.Check, contentDescription = null) }
                } else {
                    null
                },
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
            )
        }
        QuestionContent(state, bank, onDrawQuestion, onRevealAnswer, onRestartDeck)
    }
}

@Composable
private fun QuestionContent(
    state: AppUiState,
    bank: QuestionBank?,
    onDrawQuestion: () -> Unit,
    onRevealAnswer: () -> Unit,
    onRestartDeck: () -> Unit,
) {
    when {
        state.loadFailed -> MessageCard(title = stringResource(R.string.questions_load_failed))
        bank == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.current != null -> QuestionCard(
            question = state.current,
            bank = bank,
            language = state.language,
            answerRevealed = state.answerRevealed,
            remainingCount = state.remainingCount,
            onRevealAnswer = onRevealAnswer,
            onNextQuestion = onDrawQuestion,
        )
        state.available.isEmpty() -> MessageCard(
            title = stringResource(R.string.no_matching_questions),
            description = stringResource(R.string.no_matching_questions_hint),
        )
        state.remainingCount == 0 -> MessageCard(
            title = stringResource(R.string.deck_finished_title),
            description = stringResource(R.string.deck_finished_description),
            actionLabel = stringResource(R.string.restart_deck),
            onAction = onRestartDeck,
        )
        else -> MessageCard(
            title = pluralStringResource(
                R.plurals.questions_ready,
                state.remainingCount,
                state.remainingCount,
            ),
            actionLabel = stringResource(R.string.draw_question),
            onAction = onDrawQuestion,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionCard(
    question: Question,
    bank: QuestionBank,
    language: QuestionLanguage,
    answerRevealed: Boolean,
    remainingCount: Int,
    onRevealAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val labels = listOf(QuestionBank.GROUP_SCOPE, QuestionBank.GROUP_DIFFICULTY)
        .flatMap { groupId -> bank.group(groupId)?.tags.orEmpty() }
        .filter { it.id in question.tags }
        .map { it.label[language] }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEach { label ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Text(
            text = question.text[language],
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.heightIn(min = 120.dp),
        )
        AnimatedVisibility(visible = answerRevealed) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.answer_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = listOf(formatAnswer(question.answer, language), question.unit[language])
                            .filter { it.isNotBlank() }
                            .joinToString(" "),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    question.explanation?.let { explanation ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = explanation[language],
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    question.referenceYear?.let { year ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.reference_year, year),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    TextButton(
                        onClick = { runCatching { uriHandler.openUri(question.source) } },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.source,
                                Uri.parse(question.source).host?.removePrefix("www.") ?: question.source,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        if (answerRevealed) {
            Button(onClick = onNextQuestion, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.next_question))
            }
        } else {
            Button(onClick = onRevealAnswer, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.reveal_answer))
            }
            OutlinedButton(onClick = onNextQuestion, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.next_question))
            }
        }
        Text(
            text = pluralStringResource(R.plurals.remaining_questions, remainingCount, remainingCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MessageCard(
    title: String,
    description: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                description?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(text = it, style = MaterialTheme.typography.bodyLarge)
                }
                actionLabel?.let {
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onAction, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text(it)
                    }
                }
            }
        }
    }
}
