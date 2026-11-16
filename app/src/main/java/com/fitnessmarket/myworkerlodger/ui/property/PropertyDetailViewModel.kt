package com.fitnessmarket.myworkerlodger.ui.property

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessmarket.myworkerlodger.WorkerLodgerApplication
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.domain.Occupancy
import com.fitnessmarket.myworkerlodger.domain.Property
import com.fitnessmarket.myworkerlodger.domain.PropertyId
import com.fitnessmarket.myworkerlodger.domain.Room
import com.fitnessmarket.myworkerlodger.domain.RoomId
import com.fitnessmarket.myworkerlodger.domain.Sex
import com.fitnessmarket.myworkerlodger.ui.common.Deletion
import com.fitnessmarket.myworkerlodger.ui.common.TextEntry
import com.fitnessmarket.myworkerlodger.ui.common.onIo
import com.fitnessmarket.myworkerlodger.ui.common.runCommand
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
    /** Today's Occupancy of the whole Property, and of each of its Rooms. */
    val occupancy: Occupancy = Occupancy.ZERO,
    val roomOccupancy: Map<RoomId, Occupancy> = emptyMap(),
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

    /** Deleting this Property, which the domain refuses if it would rewrite history. */
    val deletion = Deletion(viewModelScope) { housing.deleteProperty(propertyId) }
    val state: StateFlow<PropertyDetailUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val loaded = onIo {
                val rooms = housing.rooms(propertyId)
                PropertyDetailUiState(
                    loaded = true,
                    property = housing.property(propertyId),
                    rooms = rooms,
                    occupancy = housing.propertyOccupancy(propertyId),
                    roomOccupancy = rooms.associate { it.id to housing.roomOccupancy(it.id) },
                )
            }
            _state.update { loaded.copy(dialog = it.dialog) }
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
        runCommand(
            command = {
                when (dialog) {
                    is PropertyDialog.Rename -> housing.renameProperty(propertyId, dialog.entry.text)
                    is PropertyDialog.AddRoom -> housing.addRoom(propertyId, dialog.entry.text, dialog.sex)
                }
            },
            onRejected = { rejection ->
                _state.update { state -> state.copy(dialog = state.dialog?.run { withEntry(entry.rejected(rejection)) }) }
            },
        ) {
            closeDialog()
            refresh()
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
