package com.marcel.estimat

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.marcel.estimat.model.QuestionFilter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.marcel.estimat.model.Audience
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    onOpenSessions: () -> Unit,
    onOpenFilters: () -> Unit,
    onFunnyOnlyChange: (Boolean) -> Unit,
    onFilterChange: (QuestionFilter) -> Unit,
    onDeleteQuestion: (String) -> Unit,
) {
    val bank = state.bank
    // Opening the game always shows a question, without an extra "draw" tap.
    LaunchedEffect(bank != null, state.current == null, state.remainingCount, state.activeSessionId) {
        if (bank != null && state.current == null && state.remainingCount > 0) onDrawQuestion()
    }
    // One scroll for the whole tab, so the open filter panel never squeezes the question.
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (bank != null) {
            GameHeader(
                state = state,
                bank = bank,
                onOpenSessions = onOpenSessions,
                onOpenFilters = onOpenFilters,
                onFunnyOnlyChange = onFunnyOnlyChange,
                onFilterChange = onFilterChange,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp),
            )
        }
        QuestionContent(state, bank, onDrawQuestion, onRevealAnswer, onRestartDeck, onDeleteQuestion)
    }
}

/** Who is playing, how far along they are, and which filters are on. */
@Composable
private fun GameHeader(
    state: AppUiState,
    bank: QuestionBank,
    onOpenSessions: () -> Unit,
    onOpenFilters: () -> Unit,
    onFunnyOnlyChange: (Boolean) -> Unit,
    onFilterChange: (QuestionFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    val funnyOnly = state.filter.tones == setOf(QuestionBank.TAG_FUNNY)
    val total = state.available.size
    val played = total - state.remainingCount
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = stringResource(R.string.sessions_change),
                        onClick = onOpenSessions,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_sessions),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.activeSession?.displayName().orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.game_progress, played, total),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else played.toFloat() / total },
                    modifier = Modifier.fillMaxWidth(),
                    drawStopIndicator = {},
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = stringResource(
                            if (filtersExpanded) R.string.filters_hide else R.string.filters_show,
                        ),
                        onClick = { filtersExpanded = !filtersExpanded },
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_filters),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = filterSummary(state, bank),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (filtersExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            // All filters inline, so the group can switch question types between rounds.
            AnimatedVisibility(visible = filtersExpanded) {
                Column(
                    modifier = Modifier
                        .padding(bottom = 12.dp),
                ) {
                    QuickFilters(state = state, onFilterChange = onFilterChange)
                    TextButton(
                        onClick = onOpenFilters,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Text(stringResource(R.string.filters_open_tab))
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.background)
            // Quick switch between serious and funny rounds, without opening Filters.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = funnyOnly,
                        role = Role.Switch,
                        onValueChange = onFunnyOnlyChange,
                    )
                    .padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "😄",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.funny_only),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = funnyOnly, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun filterSummary(state: AppUiState, bank: QuestionBank): String {
    val filter = state.filter
    val tagLabels = listOf(
        QuestionBank.GROUP_SCOPE to filter.scopes,
        QuestionBank.GROUP_DIFFICULTY to filter.difficulties,
        QuestionBank.GROUP_TONE to filter.tones,
        QuestionBank.GROUP_THEME to filter.themes,
    ).flatMap { (groupId, selected) ->
        bank.group(groupId)?.tags.orEmpty().filter { it.id in selected }.map { it.label[state.language] }
    }
    val parts = buildList {
        add(state.language.displayName())
        if (filter.audience != Audience.GENERAL) add(stringResource(filter.audience.labelRes()))
        if (tagLabels.isEmpty()) add(stringResource(R.string.filters_summary_all)) else addAll(tagLabels)
        if (filter.verifiedOnly) add(stringResource(R.string.filters_summary_verified))
    }
    return parts.joinToString(" · ")
}

/** Language names are shown in their own language. */
fun QuestionLanguage.displayName(): String = when (this) {
    QuestionLanguage.CA -> "Català"
    QuestionLanguage.ES -> "Castellano"
}

fun Audience.labelRes(): Int = when (this) {
    Audience.GENERAL -> R.string.audience_general
    Audience.KIDS -> R.string.audience_kids
    Audience.ADULTS -> R.string.audience_adults
}

@Composable
private fun QuestionContent(
    state: AppUiState,
    bank: QuestionBank?,
    onDrawQuestion: () -> Unit,
    onRevealAnswer: () -> Unit,
    onRestartDeck: () -> Unit,
    onDeleteQuestion: (String) -> Unit,
) {
    when {
        state.loadFailed -> MessageCard(title = stringResource(R.string.questions_load_failed))
        bank == null -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        // Keyed by question so the answer of the next question never shows while
        // the previous answer box animates closed.
        state.current != null -> key(state.current.id) {
            QuestionCard(
                question = state.current,
                bank = bank,
                language = state.language,
                answerRevealed = state.answerRevealed,
                onRevealAnswer = onRevealAnswer,
                onNextQuestion = onDrawQuestion,
                onDeleteQuestion = { onDeleteQuestion(state.current.id) },
            )
        }
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
    onRevealAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onDeleteQuestion: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_question_title)) },
            text = { Text(stringResource(R.string.delete_question_description)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteQuestion()
                }) {
                    Text(stringResource(R.string.delete_question_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
    val labels = listOf(QuestionBank.GROUP_SCOPE, QuestionBank.GROUP_DIFFICULTY)
        .flatMap { groupId -> bank.group(groupId)?.tags.orEmpty() }
        .filter { it.id in question.tags }
        .map { it.label[language] }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
            TextButton(
                onClick = onNextQuestion,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(R.string.skip_question))
            }
        }
        TextButton(
            onClick = { confirmDelete = true },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = stringResource(R.string.delete_question),
                color = MaterialTheme.colorScheme.error,
            )
        }
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
