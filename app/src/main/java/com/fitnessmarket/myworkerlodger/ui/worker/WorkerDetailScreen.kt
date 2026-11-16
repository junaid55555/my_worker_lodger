package com.fitnessmarket.myworkerlodger.ui.worker

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.fitnessmarket.myworkerlodger.domain.BedDetails
import com.fitnessmarket.myworkerlodger.domain.BedId
import com.fitnessmarket.myworkerlodger.domain.Employer
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.domain.StayDetails
import com.fitnessmarket.myworkerlodger.domain.Today
import com.fitnessmarket.myworkerlodger.domain.Worker
import com.fitnessmarket.myworkerlodger.domain.WorkerId
import com.fitnessmarket.myworkerlodger.ui.common.ContentOrMessage
import com.fitnessmarket.myworkerlodger.ui.common.RefreshOnResume
import com.fitnessmarket.myworkerlodger.ui.common.ScreenTopBar
import com.fitnessmarket.myworkerlodger.ui.common.display
import com.fitnessmarket.myworkerlodger.ui.common.onIo
import com.fitnessmarket.myworkerlodger.ui.common.runCommand
import com.fitnessmarket.myworkerlodger.ui.labelRes
import com.fitnessmarket.myworkerlodger.ui.messageRes
import com.fitnessmarket.myworkerlodger.ui.navigation.Routes
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerDetailUiState(
    val loaded: Boolean = false,
    /** Null once loaded means the Worker no longer exists. */
    val worker: Worker? = null,
    val employer: Employer? = null,
    /** Where the Worker lives now, or null if they aren't housed. */
    val currentPlace: StayDetails? = null,
    val dialog: WorkerDialog? = null,
)

sealed interface WorkerDialog {
    data class Edit(val form: WorkerForm) : WorkerDialog

    data class CheckIn(
        val beds: List<BedDetails>,
        val bedId: BedId? = null,
        val date: LocalDate,
        @StringRes val error: Int? = null,
    ) : WorkerDialog

    data class CheckOut(val date: LocalDate, @StringRes val error: Int? = null) : WorkerDialog
}

/** Shows one Worker and where they live, and lets the Accommodation Manager edit, check in or check out. */
class WorkerDetailViewModel(
    private val housing: Housing,
    private val today: Today,
    private val workerId: WorkerId,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkerDetailUiState())
    val state = _state.asStateFlow()

    val todayDate: LocalDate get() = today.date()

    fun refresh() {
        viewModelScope.launch {
            val (worker, employer, place) = onIo {
                val worker = housing.worker(workerId)
                Triple(worker, worker?.let { housing.employer(it.employerId) }, housing.currentPlace(workerId))
            }
            _state.update { it.copy(loaded = true, worker = worker, employer = employer, currentPlace = place) }
        }
    }

    fun startEditing() {
        val worker = _state.value.worker ?: return
        open(WorkerDialog.Edit(WorkerForm.editing(worker)))
    }

    fun changeForm(form: WorkerForm) = open(WorkerDialog.Edit(form))

    fun startCheckIn() {
        viewModelScope.launch {
            val beds = onIo { housing.bedsAvailableFor(workerId) }
            open(WorkerDialog.CheckIn(beds = beds, date = todayDate))
        }
    }

    fun startCheckOut() = open(WorkerDialog.CheckOut(date = todayDate))

    fun selectBed(bedId: BedId) = updateDialog<WorkerDialog.CheckIn> { it.copy(bedId = bedId, error = null) }

    fun changeDate(date: LocalDate) {
        updateDialog<WorkerDialog.CheckIn> { it.copy(date = date, error = null) }
        updateDialog<WorkerDialog.CheckOut> { it.copy(date = date, error = null) }
    }

    fun save() {
        when (val dialog = _state.value.dialog) {
            is WorkerDialog.Edit -> saveWorkerForm(
                housing,
                dialog.form,
                onError = { error -> open(WorkerDialog.Edit(dialog.form.copy(error = error))) },
                onSaved = ::closeAndRefresh,
            )
            is WorkerDialog.CheckIn -> {
                val bedId = dialog.bedId ?: return open(dialog.copy(error = R.string.choose_a_bed))
                runCommand(
                    command = { housing.checkIn(workerId, bedId, dialog.date) },
                    onRejected = { open(dialog.copy(error = it.messageRes())) },
                ) { closeAndRefresh() }
            }
            is WorkerDialog.CheckOut -> runCommand(
                command = { housing.checkOut(workerId, dialog.date) },
                onRejected = { open(dialog.copy(error = it.messageRes())) },
            ) { closeAndRefresh() }
            null -> Unit
        }
    }

    fun closeDialog() = open(null)

    private fun closeAndRefresh() {
        closeDialog()
        refresh()
    }

    private fun open(dialog: WorkerDialog?) = _state.update { it.copy(dialog = dialog) }

    private inline fun <reified D : WorkerDialog> updateDialog(crossinline change: (D) -> WorkerDialog) =
        _state.update { state -> (state.dialog as? D)?.let { state.copy(dialog = change(it)) } ?: state }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkerLodgerApplication
                val workerId = WorkerId(checkNotNull(createSavedStateHandle().get<String>(Routes.WORKER_ID)))
                WorkerDetailViewModel(app.housing, app.today, workerId)
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
                    CurrentPlace(state.currentPlace, viewModel::startCheckIn, viewModel::startCheckOut)
                    HorizontalDivider()
                    Detail(R.string.employer, state.employer?.name.orEmpty())
                    Detail(R.string.staff_number, worker.staffNumber)
                    Detail(R.string.sex, stringResource(worker.sex.labelRes()))
                }
            }
        }
    }
    when (val dialog = state.dialog) {
        is WorkerDialog.Edit -> WorkerFormDialog(dialog.form, viewModel::changeForm, viewModel::save, viewModel::closeDialog)
        is WorkerDialog.CheckIn -> CheckInDialog(
            title = R.string.check_in,
            beds = dialog.beds,
            selected = dialog.bedId,
            date = dialog.date,
            today = viewModel.todayDate,
            error = dialog.error,
            onSelectBed = viewModel::selectBed,
            onDateChange = viewModel::changeDate,
            onSave = viewModel::save,
            onDismiss = viewModel::closeDialog,
        )
        is WorkerDialog.CheckOut -> CheckOutDialog(
            date = dialog.date,
            today = viewModel.todayDate,
            error = dialog.error,
            onDateChange = viewModel::changeDate,
            onSave = viewModel::save,
            onDismiss = viewModel::closeDialog,
        )
        null -> Unit
    }
}

@Composable
private fun CurrentPlace(place: StayDetails?, onCheckIn: () -> Unit, onCheckOut: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.living_in), style = MaterialTheme.typography.labelMedium)
        if (place == null) {
            Text(stringResource(R.string.not_housed), style = MaterialTheme.typography.titleMedium)
            Button(onClick = onCheckIn, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.check_in)) }
        } else {
            Text(
                BedDetails(place.bed, place.room, place.property).describe(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.since_date, place.stay.checkIn.display()))
            Row(Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = onCheckOut) { Text(stringResource(R.string.check_out)) }
            }
        }
    }
}

@Composable
private fun Detail(@StringRes label: Int, value: String) {
    ListItem(overlineContent = { Text(stringResource(label)) }, headlineContent = { Text(value) })
}
