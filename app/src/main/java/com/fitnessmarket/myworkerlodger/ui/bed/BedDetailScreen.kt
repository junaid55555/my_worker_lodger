package com.fitnessmarket.myworkerlodger.ui.bed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.fitnessmarket.myworkerlodger.domain.BedStay
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.domain.WorkerId
import com.fitnessmarket.myworkerlodger.ui.common.ContentOrMessage
import com.fitnessmarket.myworkerlodger.ui.common.Deletion
import com.fitnessmarket.myworkerlodger.ui.common.DeletionDialog
import com.fitnessmarket.myworkerlodger.ui.common.RefreshOnResume
import com.fitnessmarket.myworkerlodger.ui.common.SectionHeading
import com.fitnessmarket.myworkerlodger.ui.common.ScreenTopBar
import com.fitnessmarket.myworkerlodger.ui.common.TextEntry
import com.fitnessmarket.myworkerlodger.ui.common.TextEntryDialog
import com.fitnessmarket.myworkerlodger.ui.common.onIo
import com.fitnessmarket.myworkerlodger.ui.common.runCommand
import com.fitnessmarket.myworkerlodger.ui.navigation.Routes
import com.fitnessmarket.myworkerlodger.ui.stayDates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BedDetailUiState(
    val loaded: Boolean = false,
    /** Null once loaded means the Bed no longer exists. */
    val bed: BedDetails? = null,
    val stays: List<BedStay> = emptyList(),
    val edit: BedEdit? = null,
)

/** The Edit Bed dialog: its label and whether it's in service, saved together. */
data class BedEdit(val label: TextEntry, val inService: Boolean)

/** Shows one Bed and everyone who has stayed in it, and lets the Accommodation Manager edit it. */
class BedDetailViewModel(private val housing: Housing, private val bedId: BedId) : ViewModel() {

    private val _state = MutableStateFlow(BedDetailUiState())

    /** Deleting this Bed, which the domain refuses if it would rewrite history. */
    val deletion = Deletion(viewModelScope) { housing.deleteBed(bedId) }
    val state = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val (bed, stays) = onIo { housing.bedDetails(bedId) to housing.staysInBed(bedId) }
            _state.update { it.copy(loaded = true, bed = bed, stays = stays) }
        }
    }

    fun startEditing() {
        val bed = _state.value.bed?.bed ?: return
        _state.update { it.copy(edit = BedEdit(TextEntry(bed.label), bed.inService)) }
    }

    fun changeLabel(label: String) = _state.update { it.copy(edit = it.edit?.copy(label = it.edit.label.edited(label))) }

    fun setInService(inService: Boolean) = _state.update { it.copy(edit = it.edit?.copy(inService = inService)) }

    fun save() {
        val edit = _state.value.edit ?: return
        runCommand(
            command = { housing.editBed(bedId, edit.label.text, edit.inService) },
            onRejected = { rejection -> _state.update { it.copy(edit = it.edit?.copy(label = it.edit.label.rejected(rejection))) } },
        ) {
            closeDialog()
            refresh()
        }
    }

    fun closeDialog() = _state.update { it.copy(edit = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val bedId = BedId(checkNotNull(createSavedStateHandle().get<String>(Routes.BED_ID)))
                BedDetailViewModel((this[APPLICATION_KEY] as WorkerLodgerApplication).housing, bedId)
            }
        }
    }
}

@Composable
fun BedDetailScreen(
    onBack: () -> Unit,
    onOpenWorker: (WorkerId) -> Unit,
    viewModel: BedDetailViewModel = viewModel(factory = BedDetailViewModel.Factory),
) {
    RefreshOnResume(viewModel::refresh)
    DeletionDialog(viewModel.deletion, R.string.delete_bed, R.string.delete_bed_message, onDeleted = onBack)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val details = state.bed
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ScreenTopBar(
                title = details?.let { stringResource(R.string.bed_title, it.bed.label) }.orEmpty(),
                subtitle = details?.let { "${it.property.name} · ${it.room.name}" },
                onBack = onBack,
            ) {
                if (details != null) TextButton(onClick = viewModel::startEditing) { Text(stringResource(R.string.edit)) }
                if (details != null) TextButton(onClick = viewModel.deletion::start) { Text(stringResource(R.string.delete)) }
            }
        },
    ) { innerPadding ->
        val message = when {
            !state.loaded -> null
            details == null -> R.string.rejection_bed_not_found
            else -> null
        }
        ContentOrMessage(innerPadding, message) {
            if (details != null) {
                LazyColumn(Modifier.fillMaxSize().padding(innerPadding)) {
                    item {
                        val current = state.stays.firstOrNull { it.stay.isCurrent }
                        ListItem(
                            overlineContent = { Text(stringResource(R.string.now)) },
                            headlineContent = {
                                Text(
                                    when {
                                        !details.bed.inService -> stringResource(R.string.out_of_service)
                                        current != null -> current.worker.name
                                        else -> stringResource(R.string.available)
                                    },
                                )
                            },
                        )
                        HorizontalDivider()
                        SectionHeading(R.string.stay_history)
                        if (state.stays.isEmpty()) {
                            Text(stringResource(R.string.no_stays_in_bed), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    items(state.stays, key = { it.stay.id.value }) { bedStay ->
                        ListItem(
                            headlineContent = { Text(bedStay.worker.name) },
                            supportingContent = { Text(bedStay.stay.stayDates()) },
                            modifier = Modifier.clickable { onOpenWorker(bedStay.worker.id) },
                        )
                    }
                }
            }
        }
    }
    state.edit?.let { edit ->
        TextEntryDialog(
            title = R.string.edit_bed,
            fieldLabel = R.string.bed_label,
            entry = edit.label,
            onTextChange = viewModel::changeLabel,
            onSave = viewModel::save,
            onDismiss = viewModel::closeDialog,
        ) {
            InServiceSwitch(edit.inService, viewModel::setInService)
        }
    }
}

@Composable
private fun InServiceSwitch(inService: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .toggleable(inService, role = Role.Switch, onValueChange = onChange),
    ) {
        Text(stringResource(R.string.in_service), modifier = Modifier.weight(1f))
        Switch(checked = inService, onCheckedChange = null)
    }
}
