package com.fitnessmarket.myworkerlodger.ui.properties

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.Property
import com.fitnessmarket.myworkerlodger.domain.PropertyId
import com.fitnessmarket.myworkerlodger.ui.common.ScreenTopBar
import com.fitnessmarket.myworkerlodger.ui.common.TextEntryDialog
import com.fitnessmarket.myworkerlodger.ui.theme.MyWorkerLodgerTheme

@Composable
fun PropertiesScreen(
    onOpenProperty: (PropertyId) -> Unit,
    viewModel: PropertiesViewModel = viewModel(factory = PropertiesViewModel.Factory),
) {
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    PropertiesContent(state, onOpenProperty, viewModel::startAdding)
    state.newProperty?.let { entry ->
        TextEntryDialog(
            title = R.string.add_property,
            fieldLabel = R.string.property_name,
            entry = entry,
            onTextChange = viewModel::changeName,
            onSave = viewModel::save,
            onDismiss = viewModel::closeDialog,
        )
    }
}

@Composable
private fun PropertiesContent(state: PropertiesUiState, onOpenProperty: (PropertyId) -> Unit, onAdd: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ScreenTopBar(title = stringResource(R.string.properties_title)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd) { Text(stringResource(R.string.add_property)) }
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
                        modifier = Modifier.clickable { onOpenProperty(property.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
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
            onOpenProperty = {},
            onAdd = {},
        )
    }
}
