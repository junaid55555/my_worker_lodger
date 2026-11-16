package com.fitnessmarket.myworkerlodger.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.Rejection
import com.fitnessmarket.myworkerlodger.ui.messageRes

/** What the Accommodation Manager has typed into a dialog, and why the last save was rejected, if it was. */
data class TextEntry(val text: String = "", val rejection: Rejection? = null) {
    fun edited(text: String) = TextEntry(text)
    fun rejected(rejection: Rejection) = copy(rejection = rejection)
}

/** A dialog with one text field, used to add or rename things. [extra] adds fields below it. */
@Composable
fun TextEntryDialog(
    @StringRes title: Int,
    @StringRes fieldLabel: Int,
    entry: TextEntry,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    @StringRes hint: Int? = null,
    extra: @Composable () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = entry.text,
                    onValueChange = onTextChange,
                    label = { Text(stringResource(fieldLabel)) },
                    singleLine = true,
                    isError = entry.rejection != null,
                    supportingText = (entry.rejection?.messageRes() ?: hint)?.let { { Text(stringResource(it)) } },
                )
                extra()
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
