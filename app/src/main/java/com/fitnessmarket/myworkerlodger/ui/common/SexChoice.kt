package com.fitnessmarket.myworkerlodger.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fitnessmarket.myworkerlodger.domain.Sex
import com.fitnessmarket.myworkerlodger.ui.labelRes

/** Male / female radio buttons, for a Room or a Worker. */
@Composable
fun SexChoice(selected: Sex, onSelect: (Sex) -> Unit) {
    Row(Modifier.selectableGroup().padding(top = 8.dp)) {
        Sex.entries.forEach { sex ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .selectable(selected = sex == selected, onClick = { onSelect(sex) }, role = Role.RadioButton)
                    .padding(end = 16.dp),
            ) {
                RadioButton(selected = sex == selected, onClick = null)
                Text(stringResource(sex.labelRes()))
            }
        }
    }
}
