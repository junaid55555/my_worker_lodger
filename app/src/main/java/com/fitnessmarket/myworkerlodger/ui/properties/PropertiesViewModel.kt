package com.fitnessmarket.myworkerlodger.ui.properties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessmarket.myworkerlodger.WorkerLodgerApplication
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.domain.Outcome
import com.fitnessmarket.myworkerlodger.domain.Property
import com.fitnessmarket.myworkerlodger.ui.common.TextEntry
import com.fitnessmarket.myworkerlodger.ui.common.onIo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PropertiesUiState(
    val properties: List<Property> = emptyList(),
    /** The "Add Property" dialog, when it's open. */
    val newProperty: TextEntry? = null,
)

/** Keeps the Properties screen thin: it only calls [Housing] and holds what to show. */
class PropertiesViewModel(private val housing: Housing) : ViewModel() {

    private val _state = MutableStateFlow(PropertiesUiState())
    val state: StateFlow<PropertiesUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val properties = onIo { housing.properties() }
            _state.update { it.copy(properties = properties) }
        }
    }

    fun startAdding() = _state.update { it.copy(newProperty = TextEntry()) }

    fun changeName(name: String) = _state.update { it.copy(newProperty = it.newProperty?.edited(name)) }

    fun closeDialog() = _state.update { it.copy(newProperty = null) }

    fun save() {
        val entry = _state.value.newProperty ?: return
        viewModelScope.launch {
            when (val outcome = onIo { housing.addProperty(entry.text) }) {
                is Outcome.Accepted -> {
                    _state.update { it.copy(newProperty = null) }
                    refresh()
                }
                is Outcome.Rejected -> _state.update { it.copy(newProperty = it.newProperty?.rejected(outcome.rejection)) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { PropertiesViewModel((this[APPLICATION_KEY] as WorkerLodgerApplication).housing) }
        }
    }
}
