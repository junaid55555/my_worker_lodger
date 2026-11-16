package com.fitnessmarket.myworkerlodger.ui.workers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.WorkerLodgerApplication
import com.fitnessmarket.myworkerlodger.domain.Employer
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.domain.Worker
import com.fitnessmarket.myworkerlodger.domain.WorkerId
import com.fitnessmarket.myworkerlodger.ui.common.ContentOrMessage
import com.fitnessmarket.myworkerlodger.ui.common.RefreshOnResume
import com.fitnessmarket.myworkerlodger.ui.common.ScreenTopBar
import com.fitnessmarket.myworkerlodger.ui.common.onIo
import com.fitnessmarket.myworkerlodger.ui.worker.WorkerForm
import com.fitnessmarket.myworkerlodger.ui.worker.WorkerFormDialog
import com.fitnessmarket.myworkerlodger.ui.worker.saveWorkerForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkersUiState(
    val loaded: Boolean = false,
    val search: String = "",
    val workers: List<Worker> = emptyList(),
    val employers: List<Employer> = emptyList(),
    val workerForm: WorkerForm? = null,
) {
    /** Lets each row show its Worker's Employer, so Workers with the same name can be told apart. */
    fun employerName(worker: Worker): String = employers.firstOrNull { it.id == worker.employerId }?.name.orEmpty()
}

class WorkersViewModel(private val housing: Housing) : ViewModel() {

    private val _state = MutableStateFlow(WorkersUiState())
    val state = _state.asStateFlow()

    fun refresh() {
        val search = _state.value.search
        viewModelScope.launch {
            val (workers, employers) = onIo { housing.workers(search = search) to housing.employers() }
            _state.update { it.copy(loaded = true, workers = workers, employers = employers) }
        }
    }

    fun search(text: String) {
        _state.update { it.copy(search = text) }
        refresh()
    }

    fun startAdding() = _state.update { it.copy(workerForm = WorkerForm()) }

    fun changeWorkerForm(form: WorkerForm) = _state.update { it.copy(workerForm = form) }

    fun saveWorker() {
        val form = _state.value.workerForm ?: return
        saveWorkerForm(housing, form, onError = { error -> _state.update { it.copy(workerForm = it.workerForm?.copy(error = error)) } }) {
            closeDialog()
            refresh()
        }
    }

    fun closeDialog() = _state.update { it.copy(workerForm = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer { WorkersViewModel((this[APPLICATION_KEY] as WorkerLodgerApplication).housing) }
        }
    }
}

@Composable
fun WorkersScreen(
    onOpenWorker: (WorkerId) -> Unit,
    viewModel: WorkersViewModel = viewModel(factory = WorkersViewModel.Factory),
) {
    RefreshOnResume(viewModel::refresh)
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ScreenTopBar(title = stringResource(R.string.workers_title)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = viewModel::startAdding) { Text(stringResource(R.string.add_worker)) }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = state.search,
                onValueChange = viewModel::search,
                label = { Text(stringResource(R.string.search_workers)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            val message = when {
                !state.loaded || state.workers.isNotEmpty() -> null
                state.search.isNotBlank() -> R.string.no_matching_workers
                else -> R.string.no_workers_yet
            }
            ContentOrMessage(PaddingValues(), message) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.workers, key = { it.id.value }) { worker ->
                        ListItem(
                            headlineContent = { Text(worker.name) },
                            supportingContent = {
                                Text(stringResource(R.string.worker_summary, state.employerName(worker), worker.staffNumber))
                            },
                            modifier = Modifier.clickable { onOpenWorker(worker.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    state.workerForm?.let { form ->
        WorkerFormDialog(
            form = form,
            onChange = viewModel::changeWorkerForm,
            onSave = viewModel::saveWorker,
            onDismiss = viewModel::closeDialog,
            employers = state.employers,
        )
    }
}
