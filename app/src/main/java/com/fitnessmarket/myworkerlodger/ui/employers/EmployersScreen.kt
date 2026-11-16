package com.fitnessmarket.myworkerlodger.ui.employers

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.WorkerLodgerApplication
import com.fitnessmarket.myworkerlodger.domain.Employer
import com.fitnessmarket.myworkerlodger.domain.EmployerId
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.ui.common.ContentOrMessage
import com.fitnessmarket.myworkerlodger.ui.common.RefreshOnResume
import com.fitnessmarket.myworkerlodger.ui.common.ScreenTopBar
import com.fitnessmarket.myworkerlodger.ui.common.TextEntry
import com.fitnessmarket.myworkerlodger.ui.common.TextEntryDialog
import com.fitnessmarket.myworkerlodger.ui.common.onIo
import com.fitnessmarket.myworkerlodger.ui.common.runCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmployersUiState(
    val loaded: Boolean = false,
    val employers: List<Employer> = emptyList(),
    /** The "Add Employer" dialog, when it's open. */
    val newEmployer: TextEntry? = null,
)

class EmployersViewModel(private val housing: Housing) : ViewModel() {

    private val _state = MutableStateFlow(EmployersUiState())
    val state = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val employers = onIo { housing.employers() }
            _state.update { it.copy(loaded = true, employers = employers) }
        }
    }

    fun startAdding() = _state.update { it.copy(newEmployer = TextEntry()) }

    fun changeName(name: String) = _state.update { it.copy(newEmployer = it.newEmployer?.edited(name)) }

    fun closeDialog() = _state.update { it.copy(newEmployer = null) }

    fun save() {
        val entry = _state.value.newEmployer ?: return
        runCommand(
            command = { housing.addEmployer(entry.text) },
            onRejected = { rejection -> _state.update { it.copy(newEmployer = it.newEmployer?.rejected(rejection)) } },
        ) {
            closeDialog()
            refresh()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { EmployersViewModel((this[APPLICATION_KEY] as WorkerLodgerApplication).housing) }
        }
    }
}

@Composable
fun EmployersScreen(
    onOpenEmployer: (EmployerId) -> Unit,
    viewModel: EmployersViewModel = viewModel(factory = EmployersViewModel.Factory),
) {
    RefreshOnResume(viewModel::refresh)
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ScreenTopBar(title = stringResource(R.string.employers_title)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = viewModel::startAdding) { Text(stringResource(R.string.add_employer)) }
        },
    ) { innerPadding ->
        ContentOrMessage(innerPadding, R.string.no_employers_yet.takeIf { state.loaded && state.employers.isEmpty() }) {
            LazyColumn(Modifier.fillMaxSize().padding(innerPadding)) {
                items(state.employers, key = { it.id.value }) { employer ->
                    ListItem(
                        headlineContent = { Text(employer.name) },
                        modifier = Modifier.clickable { onOpenEmployer(employer.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    state.newEmployer?.let { entry ->
        TextEntryDialog(
            title = R.string.add_employer,
            fieldLabel = R.string.employer_name,
            entry = entry,
            onTextChange = viewModel::changeName,
            onSave = viewModel::save,
            onDismiss = viewModel::closeDialog,
        )
    }
}
