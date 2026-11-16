package com.fitnessmarket.myworkerlodger.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.WorkerLodgerApplication
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.domain.Occupancy
import com.fitnessmarket.myworkerlodger.domain.Property
import com.fitnessmarket.myworkerlodger.domain.PropertyId
import com.fitnessmarket.myworkerlodger.ui.common.ContentOrMessage
import com.fitnessmarket.myworkerlodger.ui.common.RefreshOnResume
import com.fitnessmarket.myworkerlodger.ui.common.ScreenTopBar
import com.fitnessmarket.myworkerlodger.ui.common.onIo
import com.fitnessmarket.myworkerlodger.ui.summary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OverviewUiState(
    val loaded: Boolean = false,
    val overall: Occupancy = Occupancy.ZERO,
    val properties: List<Pair<Property, Occupancy>> = emptyList(),
)

/** Today's Occupancy and Available Beds across every Property, and for each one. */
class OverviewViewModel(private val housing: Housing) : ViewModel() {

    private val _state = MutableStateFlow(OverviewUiState())
    val state = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val loaded = onIo {
                OverviewUiState(
                    loaded = true,
                    overall = housing.overallOccupancy(),
                    properties = housing.properties().map { it to housing.propertyOccupancy(it.id) },
                )
            }
            _state.update { loaded }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { OverviewViewModel((this[APPLICATION_KEY] as WorkerLodgerApplication).housing) }
        }
    }
}

@Composable
fun OverviewScreen(
    onOpenProperty: (PropertyId) -> Unit,
    viewModel: OverviewViewModel = viewModel(factory = OverviewViewModel.Factory),
) {
    RefreshOnResume(viewModel::refresh)
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ScreenTopBar(title = stringResource(R.string.overview_title)) },
    ) { innerPadding ->
        ContentOrMessage(innerPadding, R.string.no_properties_yet.takeIf { state.loaded && state.properties.isEmpty() }) {
            LazyColumn(Modifier.fillMaxSize().padding(innerPadding)) {
                if (state.loaded) item {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.today_overall), style = MaterialTheme.typography.labelMedium)
                        Text(state.overall.summary(), style = MaterialTheme.typography.headlineSmall)
                    }
                    HorizontalDivider()
                }
                items(state.properties, key = { it.first.id.value }) { (property, occupancy) ->
                    ListItem(
                        headlineContent = { Text(property.name) },
                        supportingContent = { Text(occupancy.summary()) },
                        modifier = Modifier.clickable { onOpenProperty(property.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
