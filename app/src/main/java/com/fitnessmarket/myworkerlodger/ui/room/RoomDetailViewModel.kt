package com.fitnessmarket.myworkerlodger.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessmarket.myworkerlodger.WorkerLodgerApplication
import com.fitnessmarket.myworkerlodger.domain.Bed
import com.fitnessmarket.myworkerlodger.domain.BedId
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.domain.Occupancy
import com.fitnessmarket.myworkerlodger.domain.Room
import com.fitnessmarket.myworkerlodger.domain.RoomId
import com.fitnessmarket.myworkerlodger.domain.Worker
import com.fitnessmarket.myworkerlodger.ui.common.TextEntry
import com.fitnessmarket.myworkerlodger.ui.common.onIo
import com.fitnessmarket.myworkerlodger.ui.common.runCommand
import com.fitnessmarket.myworkerlodger.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoomDetailUiState(
    val loaded: Boolean = false,
    /** Null once loaded means the Room no longer exists. */
    val room: Room? = null,
    val beds: List<Bed> = emptyList(),
    /** The Worker in each occupied Bed. */
    val workersByBed: Map<BedId, Worker> = emptyMap(),
    /** Today's Occupancy of the Room. */
    val occupancy: Occupancy = Occupancy.NONE,
    val dialog: RoomDialog? = null,
)

sealed interface RoomDialog {
    val entry: TextEntry
    fun withEntry(entry: TextEntry): RoomDialog

    data class Rename(override val entry: TextEntry) : RoomDialog {
        override fun withEntry(entry: TextEntry) = copy(entry = entry)
    }

    /** Several labels can be typed at once, separated by commas. */
    data class AddBeds(override val entry: TextEntry) : RoomDialog {
        override fun withEntry(entry: TextEntry) = copy(entry = entry)
    }
}

/** Shows one Room and its Beds, and lets the Accommodation Manager rename it and add Beds. */
class RoomDetailViewModel(private val housing: Housing, private val roomId: RoomId) : ViewModel() {

    private val _state = MutableStateFlow(RoomDetailUiState())
    val state: StateFlow<RoomDetailUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val loaded = onIo {
                RoomDetailUiState(
                    loaded = true,
                    room = housing.room(roomId),
                    beds = housing.beds(roomId),
                    workersByBed = housing.workersByBed(roomId),
                    occupancy = housing.roomOccupancy(roomId),
                )
            }
            _state.update { loaded.copy(dialog = it.dialog) }
        }
    }

    fun startRenaming() {
        val name = _state.value.room?.name ?: return
        open(RoomDialog.Rename(TextEntry(name)))
    }

    fun startAddingBeds() = open(RoomDialog.AddBeds(TextEntry()))

    fun changeText(text: String) =
        _state.update { state -> state.copy(dialog = state.dialog?.run { withEntry(entry.edited(text)) }) }

    fun closeDialog() = open(null)

    fun save() {
        val dialog = _state.value.dialog ?: return
        runCommand(
            command = {
                when (dialog) {
                    is RoomDialog.Rename -> housing.renameRoom(roomId, dialog.entry.text)
                    is RoomDialog.AddBeds -> housing.addBeds(roomId, dialog.entry.labels())
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

    private fun open(dialog: RoomDialog?) = _state.update { it.copy(dialog = dialog) }

    /**
     * The Bed labels typed into the "Add Beds" dialog, separated by commas. Empty pieces from a stray
     * or doubled comma are ignored; if nothing is left, the domain rejects the empty list.
     */
    private fun TextEntry.labels(): List<String> = text.split(',').filter { it.isNotBlank() }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val roomId = RoomId(checkNotNull(createSavedStateHandle().get<String>(Routes.ROOM_ID)))
                RoomDetailViewModel((this[APPLICATION_KEY] as WorkerLodgerApplication).housing, roomId)
            }
        }
    }
}
