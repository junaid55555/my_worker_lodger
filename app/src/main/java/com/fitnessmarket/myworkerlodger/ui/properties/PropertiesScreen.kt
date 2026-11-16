package com.fitnessmarket.myworkerlodger.ui.properties

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.Property
import com.fitnessmarket.myworkerlodger.domain.PropertyId
import com.fitnessmarket.myworkerlodger.ui.messageRes
import com.fitnessmarket.myworkerlodger.ui.theme.MyWorkerLodgerTheme

@Composable
fun PropertiesScreen(viewModel: PropertiesViewModel = viewModel(factory = PropertiesViewModel.Factory)) {
    val state by viewModel.state.collectAsState()
    PropertiesContent(
        state = state,
        onAdd = viewModel::startAdding,
        onPropertyClick = viewModel::startRenaming,
        onNameChange = viewModel::changeName,
        onSave = viewModel::save,
        onDismiss = viewModel::dismissEditor,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertiesContent(
    state: PropertiesUiState,
    onAdd: () -> Unit,
    onPropertyClick: (Property) -> Unit,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.properties_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Text(stringResource(R.string.add_property), modifier = Modifier.padding(horizontal = 16.dp))
            }
        },
    ) { innerPadding ->
        if (state.properties.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_properties_yet))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(innerPadding)) {
                items(state.properties, key = { it.id.value }) { property ->
                    ListItem(
                        headlineContent = { Text(property.name) },
                        modifier = Modifier.clickable { onPropertyClick(property) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    state.editor?.let { editor ->
        PropertyEditorDialog(editor, onNameChange, onSave, onDismiss)
    }
}

@Composable
private fun PropertyEditorDialog(
    editor: PropertyEditor,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = if (editor.propertyId == null) R.string.add_property else R.string.rename_property
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            OutlinedTextField(
                value = editor.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.property_name)) },
                singleLine = true,
                isError = editor.rejection != null,
                supportingText = editor.rejection?.let { rejection -> { Text(stringResource(rejection.messageRes())) } },
            )
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun PropertiesPreview() {
    MyWorkerLodgerTheme {
        PropertiesContent(
            state = PropertiesUiState(
                properties = listOf(
                    Property(PropertyId("1"), "Al Quoz Block A"),
                    Property(PropertyId("2"), "Jebel Ali Block 3"),
                ),
            ),
            onAdd = {}, onPropertyClick = {}, onNameChange = {}, onSave = {}, onDismiss = {},
        )
    }
}
