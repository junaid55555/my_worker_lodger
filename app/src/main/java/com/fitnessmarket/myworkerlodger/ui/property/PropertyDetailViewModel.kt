package com.fitnessmarket.myworkerlodger.ui.property

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessmarket.myworkerlodger.WorkerLodgerApplication
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.domain.Outcome
import com.fitnessmarket.myworkerlodger.domain.Property
import com.fitnessmarket.myworkerlodger.domain.PropertyId
import com.fitnessmarket.myworkerlodger.domain.Room
import com.fitnessmarket.myworkerlodger.domain.Sex
import com.fitnessmarket.myworkerlodger.ui.common.TextEntry
import com.fitnessmarket.myworkerlodger.ui.common.onIo
import com.fitnessmarket.myworkerlodger.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PropertyDetailUiState(
    val loaded: Boolean = false,
    /** Null once loaded means the Property no longer exists. */
    val property: Property? = null,
    val rooms: List<Room> = emptyList(),
    val dialog: PropertyDialog? = null,
)

sealed interface PropertyDialog {
    val entry: TextEntry
    fun withEntry(entry: TextEntry): PropertyDialog

    data class Rename(override val entry: TextEntry) : PropertyDialog {
        override fun withEntry(entry: TextEntry) = copy(entry = entry)
    }

    data class AddRoom(override val entry: TextEntry, val sex: Sex = Sex.Male) : PropertyDialog {
        override fun withEntry(entry: TextEntry) = copy(entry = entry)
    }
}

/** Shows one Property and its Rooms, and lets the Accommodation Manager rename it and add Rooms. */
class PropertyDetailViewModel(private val housing: Housing, private val propertyId: PropertyId) : ViewModel() {

    private val _state = MutableStateFlow(PropertyDetailUiState())
    val state: StateFlow<PropertyDetailUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val (property, rooms) = onIo { housing.property(propertyId) to housing.rooms(propertyId) }
            _state.update { it.copy(loaded = true, property = property, rooms = rooms) }
        }
    }

    fun startRenaming() {
        val name = _state.value.property?.name ?: return
        _state.update { it.copy(dialog = PropertyDialog.Rename(TextEntry(name))) }
    }

    fun startAddingRoom() = _state.update { it.copy(dialog = PropertyDialog.AddRoom(TextEntry())) }

    fun changeText(text: String) =
        _state.update { state -> state.copy(dialog = state.dialog?.run { withEntry(entry.edited(text)) }) }

    fun chooseSex(sex: Sex) = _state.update { state ->
        state.copy(dialog = (state.dialog as? PropertyDialog.AddRoom)?.copy(sex = sex) ?: state.dialog)
    }

    fun closeDialog() = _state.update { it.copy(dialog = null) }

    fun save() {
        val dialog = _state.value.dialog ?: return
        viewModelScope.launch {
            val outcome = onIo {
                when (dialog) {
                    is PropertyDialog.Rename -> housing.renameProperty(propertyId, dialog.entry.text)
                    is PropertyDialog.AddRoom -> housing.addRoom(propertyId, dialog.entry.text, dialog.sex)
                }
            }
            when (outcome) {
                is Outcome.Accepted -> {
                    _state.update { it.copy(dialog = null) }
                    refresh()
                }
                is Outcome.Rejected -> _state.update { state ->
                    state.copy(dialog = state.dialog?.run { withEntry(entry.rejected(outcome.rejection)) })
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val propertyId = PropertyId(checkNotNull(createSavedStateHandle().get<String>(Routes.PROPERTY_ID)))
                PropertyDetailViewModel((this[APPLICATION_KEY] as WorkerLodgerApplication).housing, propertyId)
            }
        }
    }
}
