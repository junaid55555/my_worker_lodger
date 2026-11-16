package com.fitnessmarket.myworkerlodger.ui.worker

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.Employer
import com.fitnessmarket.myworkerlodger.domain.EmployerId
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.domain.Sex
import com.fitnessmarket.myworkerlodger.domain.Worker
import com.fitnessmarket.myworkerlodger.domain.WorkerId
import com.fitnessmarket.myworkerlodger.ui.common.SexChoice
import com.fitnessmarket.myworkerlodger.ui.common.runCommand
import com.fitnessmarket.myworkerlodger.ui.messageRes

/**
 * The add/edit Worker dialog. When [workerId] is null a new Worker is being added; otherwise only
 * that Worker's Staff Number, name and sex are being edited.
 */
data class WorkerForm(
    val workerId: WorkerId? = null,
    val employerId: EmployerId? = null,
    val staffNumber: String = "",
    val name: String = "",
    val sex: Sex = Sex.Male,
    @StringRes val error: Int? = null,
) {
    val adding: Boolean get() = workerId == null

    companion object {
        fun editing(worker: Worker) = WorkerForm(
            workerId = worker.id,
            employerId = worker.employerId,
            staffNumber = worker.staffNumber,
            name = worker.name,
            sex = worker.sex,
        )
    }
}

/** Adds or edits the Worker in [form], then calls [onSaved], or reports the message to show via [onError]. */
fun ViewModel.saveWorkerForm(
    housing: Housing,
    form: WorkerForm,
    onError: (Int) -> Unit,
    onSaved: () -> Unit,
) {
    val employerId = form.employerId
    if (form.workerId == null && employerId == null) return onError(R.string.choose_an_employer)
    runCommand(
        command = {
            if (form.workerId == null) {
                housing.addWorker(checkNotNull(employerId), form.staffNumber, form.name, form.sex)
            } else {
                housing.editWorker(form.workerId, form.staffNumber, form.name, form.sex)
            }
        },
        onRejected = { onError(it.messageRes()) },
        onAccepted = { onSaved() },
    )
}

/** Shows [form]. Pass [employers] to let the manager choose the Employer of a new Worker. */
@Composable
fun WorkerFormDialog(
    form: WorkerForm,
    onChange: (WorkerForm) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    employers: List<Employer>? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (form.adding) R.string.add_worker else R.string.edit_worker)) },
        text = {
            Column {
                if (form.adding && employers != null) {
                    EmployerPicker(employers, form.employerId) { onChange(form.copy(employerId = it, error = null)) }
                }
                OutlinedTextField(
                    value = form.staffNumber,
                    onValueChange = { onChange(form.copy(staffNumber = it, error = null)) },
                    label = { Text(stringResource(R.string.staff_number)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { onChange(form.copy(name = it, error = null)) },
                    label = { Text(stringResource(R.string.worker_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SexChoice(selected = form.sex, onSelect = { onChange(form.copy(sex = it, error = null)) })
                form.error?.let {
                    Text(
                        stringResource(it),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployerPicker(employers: List<Employer>, selected: EmployerId?, onSelect: (EmployerId) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = employers.firstOrNull { it.id == selected }?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.employer)) },
            placeholder = { if (employers.isEmpty()) Text(stringResource(R.string.no_employers_yet_short)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            employers.forEach { employer ->
                DropdownMenuItem(
                    text = { Text(employer.name) },
                    onClick = {
                        onSelect(employer.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
