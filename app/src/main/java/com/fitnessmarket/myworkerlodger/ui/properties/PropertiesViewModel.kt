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
import com.fitnessmarket.myworkerlodger.domain.PropertyId
import com.fitnessmarket.myworkerlodger.domain.Rejection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PropertiesUiState(
    val properties: List<Property> = emptyList(),
    val editor: PropertyEditor? = null,
)

/** The add/rename dialog. [propertyId] is null when adding a new Property. */
data class PropertyEditor(
    val propertyId: PropertyId?,
    val name: String,
    val rejection: Rejection? = null,
)

/** Keeps the Properties screen thin: it only calls [Housing] and holds what to show. */
class PropertiesViewModel(private val housing: Housing) : ViewModel() {

    private val _state = MutableStateFlow(PropertiesUiState())
    val state: StateFlow<PropertiesUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun startAdding() = _state.update { it.copy(editor = PropertyEditor(propertyId = null, name = "")) }

    fun startRenaming(property: Property) =
        _state.update { it.copy(editor = PropertyEditor(propertyId = property.id, name = property.name)) }

    fun changeName(name: String) =
        _state.update { it.copy(editor = it.editor?.copy(name = name, rejection = null)) }

    fun dismissEditor() = _state.update { it.copy(editor = null) }

    fun save() {
        val editor = _state.value.editor ?: return
        viewModelScope.launch {
            val (outcome, properties) = withContext(Dispatchers.IO) {
                val outcome = if (editor.propertyId == null) {
                    housing.addProperty(editor.name)
                } else {
                    housing.renameProperty(editor.propertyId, editor.name)
                }
                outcome to housing.properties()
            }
            _state.update {
                when (outcome) {
                    is Outcome.Accepted -> it.copy(properties = properties, editor = null)
                    is Outcome.Rejected -> it.copy(
                        properties = properties,
                        editor = it.editor?.copy(rejection = outcome.rejection),
                    )
                }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val properties = withContext(Dispatchers.IO) { housing.properties() }
            _state.update { it.copy(properties = properties) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                PropertiesViewModel((this[APPLICATION_KEY] as WorkerLodgerApplication).housing)
            }
        }
    }
}
