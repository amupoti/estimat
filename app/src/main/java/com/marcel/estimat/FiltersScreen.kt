package com.marcel.estimat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marcel.estimat.model.Audience
import com.marcel.estimat.model.QuestionBank
import com.marcel.estimat.model.QuestionFilter
import com.marcel.estimat.ui.AppUiState

@Composable
fun FiltersScreen(
    state: AppUiState,
    onFilterChange: (QuestionFilter) -> Unit,
) {
    val filter = state.filter
    val bank = state.bank
    Column(modifier = Modifier.fillMaxSize()) {
        // Stays visible while scrolling so every change shows its effect.
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
            Text(
                text = pluralStringResource(
                    R.plurals.matching_questions,
                    state.available.size,
                    state.available.size,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.filter_any_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 20.dp),
        ) {
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
                                label = { Text(stringResource(audience.labelRes())) },
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.verified_only),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.verified_only_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = filter.verifiedOnly,
                            onCheckedChange = { onFilterChange(filter.copy(verifiedOnly = it)) },
                        )
                    }
                }
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
    FilterCard(title = title) {
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
