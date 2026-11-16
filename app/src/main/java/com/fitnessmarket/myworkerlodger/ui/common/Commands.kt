package com.fitnessmarket.myworkerlodger.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnessmarket.myworkerlodger.domain.Outcome
import com.fitnessmarket.myworkerlodger.domain.Rejection
import kotlinx.coroutines.launch

/**
 * Runs a domain [command] off the main thread, then calls [onAccepted] with its result or
 * [onRejected] with the housing rule that stopped it.
 */
fun <T> ViewModel.runCommand(
    command: () -> Outcome<T>,
    onRejected: (Rejection) -> Unit,
    onAccepted: (T) -> Unit,
) {
    viewModelScope.launch {
        when (val outcome = onIo(command)) {
            is Outcome.Accepted -> onAccepted(outcome.value)
            is Outcome.Rejected -> onRejected(outcome.rejection)
        }
    }
}
