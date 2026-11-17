package com.marcel.estimat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marcel.estimat.model.GameSession
import com.marcel.estimat.ui.AppUiState

/** What the session UI can ask the view model to do. */
data class SessionActions(
    val onCreate: (String) -> Unit,
    val onSelect: (String) -> Unit,
    val onRename: (String, String) -> Unit,
    val onDelete: (String) -> Unit,
)

@Composable
private fun GameSession.displayName(): String =
    name.ifBlank { stringResource(R.string.session_unnamed) }

/** Shows the active session and opens the session manager when tapped. */
@Composable
fun SessionBar(state: AppUiState, actions: SessionActions, modifier: Modifier = Modifier) {
    val active = state.activeSession ?: return
    var showManager by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showManager = true },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.session_label, active.displayName()),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.session_played_count,
                        active.playedIds.size,
                        active.playedIds.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.sessions_change),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    if (showManager) {
        SessionManagerDialog(
            state = state,
            actions = actions,
            onDismiss = { showManager = false },
        )
    }
}

private sealed interface SessionDialog {
    data object List : SessionDialog
    data object Create : SessionDialog
    data class Rename(val session: GameSession) : SessionDialog
    data class Delete(val session: GameSession) : SessionDialog
}

@Composable
private fun SessionManagerDialog(
    state: AppUiState,
    actions: SessionActions,
    onDismiss: () -> Unit,
) {
    var dialog by remember { mutableStateOf<SessionDialog>(SessionDialog.List) }
    val backToList = { dialog = SessionDialog.List }

    when (val current = dialog) {
        SessionDialog.List -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.sessions_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sessions_description),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(state.sessions.asReversed(), key = { it.id }) { session ->
                            SessionRow(
                                session = session,
                                selected = session.id == state.activeSessionId,
                                onSelect = {
                                    actions.onSelect(session.id)
                                    onDismiss()
                                },
                                onRename = { dialog = SessionDialog.Rename(session) },
                                onDelete = { dialog = SessionDialog.Delete(session) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialog = SessionDialog.Create }) {
                    Text(stringResource(R.string.session_new))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            },
        )
        SessionDialog.Create -> SessionNameDialog(
            title = stringResource(R.string.session_new),
            initialName = stringResource(R.string.session_default_name, state.sessions.size + 1),
            confirmLabel = stringResource(R.string.session_create),
            onConfirm = { name ->
                actions.onCreate(name)
                onDismiss()
            },
            onDismiss = backToList,
        )
        is SessionDialog.Rename -> SessionNameDialog(
            title = stringResource(R.string.session_rename),
            initialName = current.session.name,
            confirmLabel = stringResource(R.string.save),
            onConfirm = { name ->
                actions.onRename(current.session.id, name)
                backToList()
            },
            onDismiss = backToList,
        )
        is SessionDialog.Delete -> AlertDialog(
            onDismissRequest = backToList,
            title = { Text(stringResource(R.string.session_delete_title)) },
            text = {
                Text(stringResource(R.string.session_delete_description, current.session.displayName()))
            },
            confirmButton = {
                TextButton(onClick = {
                    actions.onDelete(current.session.id)
                    backToList()
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = backToList) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SessionRow(
    session: GameSession,
    selected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = session.displayName(), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = pluralStringResource(
                    R.plurals.session_played_count,
                    session.playedIds.size,
                    session.playedIds.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        IconButton(onClick = onRename) {
            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.session_rename))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
        }
    }
}

@Composable
private fun SessionNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.session_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
