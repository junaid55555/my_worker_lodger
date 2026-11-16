package com.fitnessmarket.myworkerlodger.ui.worker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.domain.Worker
import com.fitnessmarket.myworkerlodger.domain.WorkerId
import com.fitnessmarket.myworkerlodger.ui.common.ContentOrMessage
import com.fitnessmarket.myworkerlodger.ui.common.RefreshOnResume
import com.fitnessmarket.myworkerlodger.ui.common.ScreenTopBar
import com.fitnessmarket.myworkerlodger.ui.common.onIo
import com.fitnessmarket.myworkerlodger.ui.labelRes
import com.fitnessmarket.myworkerlodger.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerDetailUiState(
    val loaded: Boolean = false,
    /** Null once loaded means the Worker no longer exists. */
    val worker: Worker? = null,
    val employer: Employer? = null,
    val form: WorkerForm? = null,
)

/** Shows one Worker's details and lets the Accommodation Manager edit their Staff Number and name. */
class WorkerDetailViewModel(private val housing: Housing, private val workerId: WorkerId) : ViewModel() {

    private val _state = MutableStateFlow(WorkerDetailUiState())
    val state = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val (worker, employer) = onIo {
                val worker = housing.worker(workerId)
                worker to worker?.let { housing.employer(it.employerId) }
            }
            _state.update { it.copy(loaded = true, worker = worker, employer = employer) }
        }
    }

    fun startEditing() {
        val worker = _state.value.worker ?: return
        _state.update { it.copy(form = WorkerForm.editing(worker)) }
    }

    fun changeForm(form: WorkerForm) = _state.update { it.copy(form = form) }

    fun save() {
        val form = _state.value.form ?: return
        saveWorkerForm(housing, form, onError = { error -> _state.update { it.copy(form = it.form?.copy(error = error)) } }) {
            closeDialog()
            refresh()
        }
    }

    fun closeDialog() = _state.update { it.copy(form = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val workerId = WorkerId(checkNotNull(createSavedStateHandle().get<String>(Routes.WORKER_ID)))
                WorkerDetailViewModel((this[APPLICATION_KEY] as WorkerLodgerApplication).housing, workerId)
            }
        }
    }
}

@Composable
fun WorkerDetailScreen(
    onBack: () -> Unit,
    viewModel: WorkerDetailViewModel = viewModel(factory = WorkerDetailViewModel.Factory),
) {
    RefreshOnResume(viewModel::refresh)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val worker = state.worker
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ScreenTopBar(title = worker?.name.orEmpty(), onBack = onBack) {
                if (worker != null) TextButton(onClick = viewModel::startEditing) { Text(stringResource(R.string.edit)) }
            }
        },
    ) { innerPadding ->
        ContentOrMessage(innerPadding, R.string.rejection_worker_not_found.takeIf { state.loaded && worker == null }) {
            if (worker != null) {
                Column(Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())) {
                    Detail(R.string.employer, state.employer?.name.orEmpty())
                    Detail(R.string.staff_number, worker.staffNumber)
                    Detail(R.string.sex, stringResource(worker.sex.labelRes()))
                }
            }
        }
    }
    state.form?.let { form ->
        WorkerFormDialog(form, viewModel::changeForm, viewModel::save, viewModel::closeDialog)
    }
}

@Composable
private fun Detail(label: Int, value: String) {
    ListItem(overlineContent = { Text(stringResource(label)) }, headlineContent = { Text(value) })
}
