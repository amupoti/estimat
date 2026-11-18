package com.marcel.estimat

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
    val onReset: (String) -> Unit,
    val onDelete: (String) -> Unit,
)

@Composable
fun GameSession.displayName(): String =
    name.ifBlank { stringResource(R.string.session_unnamed) }

private sealed interface SessionDialog {
    data object Create : SessionDialog
    data class Rename(val session: GameSession) : SessionDialog
    data class Reset(val session: GameSession) : SessionDialog
    data class Delete(val session: GameSession) : SessionDialog
}

/** Lists every session; the active one is the group playing now. */
@Composable
fun SessionsScreen(
    state: AppUiState,
    actions: SessionActions,
    onPlay: () -> Unit,
) {
    var dialog by remember { mutableStateOf<SessionDialog?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // Leaves room so the last card is never hidden behind the button.
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.sessions_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            items(state.sessions.asReversed(), key = { it.id }) { session ->
                SessionCard(
                    session = session,
                    active = session.id == state.activeSessionId,
                    onPlay = {
                        actions.onSelect(session.id)
                        onPlay()
                    },
                    onRename = { dialog = SessionDialog.Rename(session) },
                    onReset = { dialog = SessionDialog.Reset(session) },
                    onDelete = { dialog = SessionDialog.Delete(session) },
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = { dialog = SessionDialog.Create },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.session_new)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }

    val close = { dialog = null }
    when (val current = dialog) {
        null -> Unit
        SessionDialog.Create -> SessionNameDialog(
            title = stringResource(R.string.session_new),
            initialName = stringResource(R.string.session_default_name, state.sessions.size + 1),
            confirmLabel = stringResource(R.string.session_create),
            onConfirm = { name ->
                actions.onCreate(name)
                close()
            },
            onDismiss = close,
        )
        is SessionDialog.Rename -> SessionNameDialog(
            title = stringResource(R.string.session_rename),
            initialName = current.session.name,
            confirmLabel = stringResource(R.string.save),
            onConfirm = { name ->
                actions.onRename(current.session.id, name)
                close()
            },
            onDismiss = close,
        )
        is SessionDialog.Reset -> ConfirmDialog(
            title = stringResource(R.string.session_reset_title),
            text = stringResource(R.string.session_reset_description, current.session.displayName()),
            confirmLabel = stringResource(R.string.session_reset_confirm),
            onConfirm = {
                actions.onReset(current.session.id)
                close()
            },
            onDismiss = close,
        )
        is SessionDialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.session_delete_title),
            text = stringResource(R.string.session_delete_description, current.session.displayName()),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                actions.onDelete(current.session.id)
                close()
            },
            onDismiss = close,
        )
    }
}

@Composable
private fun SessionCard(
    session: GameSession,
    active: Boolean,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = if (active) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (active) 0.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (active) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                text = stringResource(R.string.session_active),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.session_played_count,
                        session.playedIds.size,
                        session.playedIds.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (session.lastPlayedAt > 0) {
                        stringResource(R.string.session_last_played, relativeDay(session.lastPlayedAt))
                    } else {
                        stringResource(R.string.session_never_played)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.session_options),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MenuItem(R.string.session_play, Icons.Rounded.PlayArrow) {
                        menuOpen = false
                        onPlay()
                    }
                    MenuItem(R.string.session_rename, Icons.Rounded.Edit) {
                        menuOpen = false
                        onRename()
                    }
                    MenuItem(R.string.session_reset, Icons.Rounded.Refresh, enabled = session.playedIds.isNotEmpty()) {
                        menuOpen = false
                        onReset()
                    }
                    MenuItem(R.string.delete, Icons.Rounded.Delete) {
                        menuOpen = false
                        onDelete()
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    labelRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(labelRes)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        enabled = enabled,
        onClick = onClick,
    )
}

/** "Today", "Yesterday", "3 days ago"… in the device language. */
private fun relativeDay(time: Long): String =
    DateUtils.getRelativeTimeSpanString(
        time,
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
    ).toString()

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
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
