package com.marcel.estimat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marcel.estimat.model.Audience
import com.marcel.estimat.model.QuestionBank
import com.marcel.estimat.model.QuestionFilter
import com.marcel.estimat.model.QuestionLanguage
import com.marcel.estimat.ui.AppUiState

@Composable
fun FiltersScreen(
    state: AppUiState,
    onLanguageChange: (QuestionLanguage) -> Unit,
    onFilterChange: (QuestionFilter) -> Unit,
) {
    val filter = state.filter
    val bank = state.bank
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            Text(
                text = pluralStringResource(
                    R.plurals.matching_questions,
                    state.available.size,
                    state.available.size,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            FilterCard(title = stringResource(R.string.question_language)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    QuestionLanguage.entries.forEachIndexed { index, language ->
                        SegmentedButton(
                            selected = state.language == language,
                            onClick = { onLanguageChange(language) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = QuestionLanguage.entries.size,
                            ),
                            // Language names are shown in their own language.
                            label = {
                                Text(
                                    when (language) {
                                        QuestionLanguage.CA -> "Català"
                                        QuestionLanguage.ES -> "Castellano"
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
        item {
            FilterCard(
                title = stringResource(R.string.audience),
                description = stringResource(R.string.audience_description),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Audience.entries.forEachIndexed { index, audience ->
                        SegmentedButton(
                            selected = filter.audience == audience,
                            onClick = { onFilterChange(filter.copy(audience = audience)) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = Audience.entries.size,
                            ),
                            label = {
                                Text(
                                    stringResource(
                                        when (audience) {
                                            Audience.GENERAL -> R.string.audience_general
                                            Audience.KIDS -> R.string.audience_kids
                                            Audience.ADULTS -> R.string.audience_adults
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
        if (bank != null) {
            val groups = listOf(
                Triple(QuestionBank.GROUP_SCOPE, R.string.filter_scope, filter.scopes),
                Triple(QuestionBank.GROUP_DIFFICULTY, R.string.filter_difficulty, filter.difficulties),
                Triple(QuestionBank.GROUP_TONE, R.string.filter_tone, filter.tones),
                Triple(QuestionBank.GROUP_THEME, R.string.filter_theme, filter.themes),
            )
            groups.forEach { (groupId, titleRes, selected) ->
                val group = bank.group(groupId) ?: return@forEach
                item(key = groupId) {
                    TagFilterCard(
                        title = stringResource(titleRes),
                        tags = group.tags.map { it.id to it.label[state.language] },
                        selected = selected,
                        onSelectedChange = { newSelection ->
                            onFilterChange(filter.withGroupSelection(groupId, newSelection))
                        },
                    )
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { onFilterChange(QuestionFilter(verifiedOnly = filter.verifiedOnly)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.clear_filters))
            }
        }
    }
}

private fun QuestionFilter.withGroupSelection(groupId: String, selection: Set<String>): QuestionFilter =
    when (groupId) {
        QuestionBank.GROUP_SCOPE -> copy(scopes = selection)
        QuestionBank.GROUP_DIFFICULTY -> copy(difficulties = selection)
        QuestionBank.GROUP_TONE -> copy(tones = selection)
        QuestionBank.GROUP_THEME -> copy(themes = selection)
        else -> this
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFilterCard(
    title: String,
    tags: List<Pair<String, String>>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
) {
    FilterCard(title = title, description = stringResource(R.string.filter_any_hint)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { (id, label) ->
                val isSelected = id in selected
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSelectedChange(if (isSelected) selected - id else selected + id)
                    },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun FilterCard(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            description?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
