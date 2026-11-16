package com.fitnessmarket.myworkerlodger.ui.worker

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import com.fitnessmarket.myworkerlodger.domain.Outcome
import com.fitnessmarket.myworkerlodger.domain.Stay
import com.fitnessmarket.myworkerlodger.domain.StayDetails
import com.fitnessmarket.myworkerlodger.domain.StayId
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
import com.fitnessmarket.myworkerlodger.ui.stayDates
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
    /** The Worker's current Stay, or null if they aren't housed. */
    val currentStay: StayDetails? = null,
    /** Every Stay the Worker has had, most recent first. */
    val history: List<StayDetails> = emptyList(),
    val dialog: WorkerDialog? = null,
)

/** What a chosen Bed and date are for: each knows its title and the command it runs. */
enum class BedChoice(@StringRes val title: Int) {
    CheckIn(R.string.check_in) {
        override fun run(housing: Housing, workerId: WorkerId, bedId: BedId, date: LocalDate) =
            housing.checkIn(workerId, bedId, date)
    },
    Move(R.string.move) {
        override fun run(housing: Housing, workerId: WorkerId, bedId: BedId, date: LocalDate) =
            housing.move(workerId, bedId, date)
    },
    ;

    abstract fun run(housing: Housing, workerId: WorkerId, bedId: BedId, date: LocalDate): Outcome<Stay>
}

sealed interface WorkerDialog {
    data class Edit(val form: WorkerForm) : WorkerDialog

    /** Choosing a Bed and a date, for a Check-in or a Move. */
    data class ChooseBed(
        val purpose: BedChoice,
        val beds: List<BedDetails>,
        val bedId: BedId? = null,
        val date: LocalDate,
        @StringRes val error: Int? = null,
    ) : WorkerDialog

    data class CheckOut(val date: LocalDate, @StringRes val error: Int? = null) : WorkerDialog

    /** Correcting the dates of a Stay; [checkOut] is null for the current Stay, which keeps none. */
    data class CorrectStay(
        val stayId: StayId,
        val checkIn: LocalDate,
        val checkOut: LocalDate?,
        @StringRes val error: Int? = null,
    ) : WorkerDialog
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
            val loaded = onIo {
                val worker = housing.worker(workerId)
                WorkerDetailUiState(
                    loaded = true,
                    worker = worker,
                    employer = worker?.let { housing.employer(it.employerId) },
                    currentStay = housing.currentStay(workerId),
                    history = housing.staysOf(workerId),
                )
            }
            _state.update { loaded.copy(dialog = it.dialog) }
        }
    }

    fun startEditing() {
        val worker = _state.value.worker ?: return
        open(WorkerDialog.Edit(WorkerForm.editing(worker)))
    }

    fun changeForm(form: WorkerForm) = open(WorkerDialog.Edit(form))

    fun startCheckIn() = chooseBed(BedChoice.CheckIn)

    fun startMove() = chooseBed(BedChoice.Move)

    private fun chooseBed(purpose: BedChoice) {
        viewModelScope.launch {
            val beds = onIo { housing.bedsAvailableFor(workerId) }
            open(WorkerDialog.ChooseBed(purpose = purpose, beds = beds, date = todayDate))
        }
    }

    fun startCheckOut() = open(WorkerDialog.CheckOut(date = todayDate))

    fun startCorrecting(stay: Stay) = open(WorkerDialog.CorrectStay(stay.id, stay.checkIn, stay.checkOut))

    fun changeCheckIn(date: LocalDate) = updateDialog<WorkerDialog.CorrectStay> { it.copy(checkIn = date, error = null) }

    fun changeCheckOut(date: LocalDate) = updateDialog<WorkerDialog.CorrectStay> { it.copy(checkOut = date, error = null) }

    fun selectBed(bedId: BedId) = updateDialog<WorkerDialog.ChooseBed> { it.copy(bedId = bedId, error = null) }

    fun changeDate(date: LocalDate) {
        updateDialog<WorkerDialog.ChooseBed> { it.copy(date = date, error = null) }
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
            is WorkerDialog.ChooseBed -> {
                val bedId = dialog.bedId ?: return open(dialog.copy(error = R.string.choose_a_bed))
                runCommand(
                    command = {
                        dialog.purpose.run(housing, workerId, bedId, dialog.date)
                    },
                    onRejected = { open(dialog.copy(error = it.messageRes())) },
                ) { closeAndRefresh() }
            }
            is WorkerDialog.CheckOut -> runCommand(
                command = { housing.checkOut(workerId, dialog.date) },
                onRejected = { open(dialog.copy(error = it.messageRes())) },
            ) { closeAndRefresh() }
            is WorkerDialog.CorrectStay -> runCommand(
                command = { housing.correctStay(dialog.stayId, dialog.checkIn, dialog.checkOut) },
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
                    LivingIn(state.currentStay, viewModel::startCheckIn, viewModel::startMove, viewModel::startCheckOut)
                    HorizontalDivider()
                    Detail(R.string.employer, state.employer?.name.orEmpty())
                    Detail(R.string.staff_number, worker.staffNumber)
                    Detail(R.string.sex, stringResource(worker.sex.labelRes()))
                    History(state.history, viewModel::startCorrecting)
                }
            }
        }
    }
    when (val dialog = state.dialog) {
        is WorkerDialog.Edit -> WorkerFormDialog(dialog.form, viewModel::changeForm, viewModel::save, viewModel::closeDialog)
        is WorkerDialog.ChooseBed -> ChooseBedDialog(
            title = dialog.purpose.title,
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
        is WorkerDialog.CorrectStay -> CorrectStayDialog(
            checkIn = dialog.checkIn,
            checkOut = dialog.checkOut,
            today = viewModel.todayDate,
            error = dialog.error,
            onCheckInChange = viewModel::changeCheckIn,
            onCheckOutChange = viewModel::changeCheckOut,
            onSave = viewModel::save,
            onDismiss = viewModel::closeDialog,
        )
        null -> Unit
    }
}

/** The Worker's Stays, most recent first; tapping one corrects its dates. */
@Composable
private fun History(history: List<StayDetails>, onCorrect: (Stay) -> Unit) {
    HorizontalDivider()
    Text(
        stringResource(R.string.stay_history),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
    if (history.isEmpty()) {
        Text(stringResource(R.string.no_stays_for_worker), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
    history.forEach { details ->
        ListItem(
            headlineContent = { Text(details.place.describe()) },
            supportingContent = { Text(details.stay.stayDates()) },
            trailingContent = { Text(stringResource(R.string.correct)) },
            modifier = Modifier.clickable { onCorrect(details.stay) },
        )
    }
}

@Composable
private fun LivingIn(stay: StayDetails?, onCheckIn: () -> Unit, onMove: () -> Unit, onCheckOut: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.living_in), style = MaterialTheme.typography.labelMedium)
        if (stay == null) {
            Text(stringResource(R.string.not_housed), style = MaterialTheme.typography.titleMedium)
            Button(onClick = onCheckIn, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.check_in)) }
        } else {
            Text(
                stay.place.describe(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.since_date, stay.stay.checkIn.display()))
            Row(Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = onMove, modifier = Modifier.padding(end = 8.dp)) { Text(stringResource(R.string.move)) }
                OutlinedButton(onClick = onCheckOut) { Text(stringResource(R.string.check_out)) }
            }
        }
    }
}

@Composable
private fun Detail(@StringRes label: Int, value: String) {
    ListItem(overlineContent = { Text(stringResource(label)) }, headlineContent = { Text(value) })
}
