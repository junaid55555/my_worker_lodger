package com.fitnessmarket.myworkerlodger.ui.worker

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.BedDetails
import com.fitnessmarket.myworkerlodger.domain.BedId
import com.fitnessmarket.myworkerlodger.ui.common.DateField
import java.time.LocalDate

/** "Property · Room · Bed 1A", the way a Bed is named outside its Room. */
@Composable
fun BedDetails.describe(): String = stringResource(R.string.bed_place, property.name, room.name, bed.label)

/** Choose an Available Bed and a date, for a Check-in or a Move. */
@Composable
fun ChooseBedDialog(
    @StringRes title: Int,
    beds: List<BedDetails>,
    selected: BedId?,
    date: LocalDate,
    today: LocalDate,
    @StringRes error: Int?,
    onSelectBed: (BedId) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column {
                DateField(R.string.date, date, latest = today, onChange = onDateChange)
                if (beds.isEmpty()) {
                    Text(stringResource(R.string.no_available_beds), modifier = Modifier.padding(top = 16.dp))
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp).padding(top = 8.dp).selectableGroup()) {
                        items(beds, key = { it.bed.id.value }) { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = option.bed.id == selected,
                                        onClick = { onSelectBed(option.bed.id) },
                                        role = Role.RadioButton,
                                    ),
                            ) {
                                RadioButton(selected = option.bed.id == selected, onClick = null)
                                Text(option.describe())
                            }
                        }
                    }
                }
                ErrorText(error)
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Choose the date of a Check-out. */
@Composable
fun CheckOutDialog(
    date: LocalDate,
    today: LocalDate,
    @StringRes error: Int?,
    onDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.check_out)) },
        text = {
            Column {
                DateField(R.string.date, date, latest = today, onChange = onDateChange)
                ErrorText(error)
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ErrorText(@StringRes error: Int?) {
    error?.let {
        Text(
            stringResource(it),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Correct the dates of a Stay. An ended Stay shows its Check-out too; a current Stay has none. */
@Composable
fun CorrectStayDialog(
    checkIn: LocalDate,
    checkOut: LocalDate?,
    today: LocalDate,
    @StringRes error: Int?,
    onCheckInChange: (LocalDate) -> Unit,
    onCheckOutChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.correct_dates)) },
        text = {
            Column {
                DateField(R.string.check_in_date, checkIn, latest = today, onChange = onCheckInChange)
                if (checkOut != null) {
                    DateField(R.string.check_out_date, checkOut, latest = today, onChange = onCheckOutChange)
                }
                ErrorText(error)
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
