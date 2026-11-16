package com.fitnessmarket.myworkerlodger.ui.employer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.WorkerLodgerApplication
import com.fitnessmarket.myworkerlodger.domain.Employer
import com.fitnessmarket.myworkerlodger.domain.EmployerId
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.domain.Worker
import com.fitnessmarket.myworkerlodger.domain.WorkerId
import com.fitnessmarket.myworkerlodger.ui.common.ContentOrMessage
import com.fitnessmarket.myworkerlodger.ui.common.RefreshOnResume
import com.fitnessmarket.myworkerlodger.ui.common.ScreenTopBar
import com.fitnessmarket.myworkerlodger.ui.common.TextEntry
import com.fitnessmarket.myworkerlodger.ui.common.TextEntryDialog
import com.fitnessmarket.myworkerlodger.ui.common.onIo
import com.fitnessmarket.myworkerlodger.ui.common.runCommand
import com.fitnessmarket.myworkerlodger.ui.navigation.Routes
import com.fitnessmarket.myworkerlodger.ui.worker.WorkerForm
import com.fitnessmarket.myworkerlodger.ui.worker.WorkerFormDialog
import com.fitnessmarket.myworkerlodger.ui.worker.saveWorkerForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmployerDetailUiState(
    val loaded: Boolean = false,
    /** Null once loaded means the Employer no longer exists. */
    val employer: Employer? = null,
    val workers: List<Worker> = emptyList(),
    val rename: TextEntry? = null,
    val workerForm: WorkerForm? = null,
)

/** Shows one Employer and its Workers, and lets the Accommodation Manager rename it and add Workers. */
class EmployerDetailViewModel(private val housing: Housing, private val employerId: EmployerId) : ViewModel() {

    private val _state = MutableStateFlow(EmployerDetailUiState())
    val state = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val (employer, workers) = onIo { housing.employer(employerId) to housing.workers(employer = employerId) }
            _state.update { it.copy(loaded = true, employer = employer, workers = workers) }
        }
    }

    fun startRenaming() {
        val name = _state.value.employer?.name ?: return
        _state.update { it.copy(rename = TextEntry(name)) }
    }

    fun changeName(name: String) = _state.update { it.copy(rename = it.rename?.edited(name)) }

    fun saveName() {
        val entry = _state.value.rename ?: return
        runCommand(
            command = { housing.renameEmployer(employerId, entry.text) },
            onRejected = { rejection -> _state.update { it.copy(rename = it.rename?.rejected(rejection)) } },
        ) {
            closeDialogs()
            refresh()
        }
    }

    fun startAddingWorker() = _state.update { it.copy(workerForm = WorkerForm(employerId = employerId)) }

    fun changeWorkerForm(form: WorkerForm) = _state.update { it.copy(workerForm = form) }

    fun saveWorker() {
        val form = _state.value.workerForm ?: return
        saveWorkerForm(housing, form, onError = { error -> _state.update { it.copy(workerForm = it.workerForm?.copy(error = error)) } }) {
            closeDialogs()
            refresh()
        }
    }

    fun closeDialogs() = _state.update { it.copy(rename = null, workerForm = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val employerId = EmployerId(checkNotNull(createSavedStateHandle().get<String>(Routes.EMPLOYER_ID)))
                EmployerDetailViewModel((this[APPLICATION_KEY] as WorkerLodgerApplication).housing, employerId)
            }
        }
    }
}

@Composable
fun EmployerDetailScreen(
    onBack: () -> Unit,
    onOpenWorker: (WorkerId) -> Unit,
    viewModel: EmployerDetailViewModel = viewModel(factory = EmployerDetailViewModel.Factory),
) {
    RefreshOnResume(viewModel::refresh)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val employer = state.employer
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ScreenTopBar(title = employer?.name.orEmpty(), onBack = onBack) {
                if (employer != null) TextButton(onClick = viewModel::startRenaming) { Text(stringResource(R.string.rename)) }
            }
        },
        floatingActionButton = {
            if (employer != null) {
                ExtendedFloatingActionButton(onClick = viewModel::startAddingWorker) { Text(stringResource(R.string.add_worker)) }
            }
        },
    ) { innerPadding ->
        val message = when {
            !state.loaded -> null
            employer == null -> R.string.rejection_employer_not_found
            state.workers.isEmpty() -> R.string.no_workers_for_employer
            else -> null
        }
        ContentOrMessage(innerPadding, message) {
            LazyColumn(Modifier.fillMaxSize().padding(innerPadding)) {
                items(state.workers, key = { it.id.value }) { worker ->
                    ListItem(
                        headlineContent = { Text(worker.name) },
                        supportingContent = { Text(stringResource(R.string.staff_number_value, worker.staffNumber)) },
                        modifier = Modifier.clickable { onOpenWorker(worker.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    state.rename?.let { entry ->
        TextEntryDialog(
            title = R.string.rename_employer,
            fieldLabel = R.string.employer_name,
            entry = entry,
            onTextChange = viewModel::changeName,
            onSave = viewModel::saveName,
            onDismiss = viewModel::closeDialogs,
        )
    }
    state.workerForm?.let { form ->
        WorkerFormDialog(form, viewModel::changeWorkerForm, viewModel::saveWorker, viewModel::closeDialogs)
    }
}
