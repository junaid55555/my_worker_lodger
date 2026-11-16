package com.fitnessmarket.myworkerlodger.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.Outcome
import com.fitnessmarket.myworkerlodger.ui.messageRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where a deletion is: being confirmed (with the rule that refused it, if any), or done. */
data class DeletionState(val confirming: Boolean = false, @StringRes val error: Int? = null, val deleted: Boolean = false)

/**
 * The "Delete …?" flow every detail screen shares: ask for confirmation, run [command], and either
 * report the housing rule that refused it or mark the deletion done so the screen can close.
 */
class Deletion(private val scope: CoroutineScope, private val command: () -> Outcome<Unit>) {
    private val _state = MutableStateFlow(DeletionState())
    val state = _state.asStateFlow()

    fun start() = _state.update { DeletionState(confirming = true) }

    fun cancel() = _state.update { DeletionState() }

    /** The deletion under way, so a second tap on Delete doesn't run it twice. */
    private var running: Job? = null

    fun confirm() {
        if (running?.isActive == true) return
        running = scope.launch {
            when (val outcome = onIo(command)) {
                is Outcome.Accepted -> _state.update { DeletionState(deleted = true) }
                is Outcome.Rejected -> _state.update { it.copy(error = outcome.rejection.messageRes()) }
            }
        }
    }
}

/** Shows the confirmation for [deletion], and calls [onDeleted] once it has gone through. */
@Composable
fun DeletionDialog(deletion: Deletion, @StringRes title: Int, @StringRes message: Int, onDeleted: () -> Unit) {
    val state by deletion.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    if (!state.confirming) return
    AlertDialog(
        onDismissRequest = deletion::cancel,
        title = { Text(stringResource(title)) },
        text = {
            Column {
                Text(stringResource(message))
                state.error?.let {
                    Text(
                        stringResource(it),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (state.error == null) TextButton(onClick = deletion::confirm) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = deletion::cancel) {
                Text(stringResource(if (state.error == null) R.string.cancel else R.string.ok))
            }
        },
    )
}
