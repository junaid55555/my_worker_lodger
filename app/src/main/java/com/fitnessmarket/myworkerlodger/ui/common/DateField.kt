package com.fitnessmarket.myworkerlodger.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fitnessmarket.myworkerlodger.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/** How dates are shown throughout the app, in the device's own style. */
fun LocalDate.display(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/**
 * A read-only date with a "Change" button that opens a calendar. Only [latest] and earlier can be
 * picked, because Check-ins and Check-outs can't be recorded ahead of time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(@StringRes label: Int, date: LocalDate, latest: LocalDate, onChange: (LocalDate) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = date.display(),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(label)) },
        trailingIcon = { TextButton(onClick = { picking = true }) { Text(stringResource(R.string.change)) } },
        modifier = Modifier.fillMaxWidth(),
    )
    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochDay() * MILLIS_PER_DAY,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / MILLIS_PER_DAY <= latest.toEpochDay()
                override fun isSelectableYear(year: Int) = year <= latest.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(LocalDate.ofEpochDay(it / MILLIS_PER_DAY)) }
                    picking = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DatePicker(state = state)
        }
    }
}
