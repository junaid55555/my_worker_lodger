package com.fitnessmarket.myworkerlodger.ui.property

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.Property
import com.fitnessmarket.myworkerlodger.domain.PropertyId
import com.fitnessmarket.myworkerlodger.domain.Room
import com.fitnessmarket.myworkerlodger.domain.RoomId
import com.fitnessmarket.myworkerlodger.domain.Sex
import com.fitnessmarket.myworkerlodger.ui.common.ContentOrMessage
import com.fitnessmarket.myworkerlodger.ui.common.DeletionDialog
import com.fitnessmarket.myworkerlodger.ui.common.RefreshOnResume
import com.fitnessmarket.myworkerlodger.ui.common.SexChoice
import com.fitnessmarket.myworkerlodger.ui.common.ScreenTopBar
import com.fitnessmarket.myworkerlodger.ui.common.TextEntryDialog
import com.fitnessmarket.myworkerlodger.ui.labelRes
import com.fitnessmarket.myworkerlodger.ui.TodayOccupancy
import com.fitnessmarket.myworkerlodger.ui.summary
import com.fitnessmarket.myworkerlodger.ui.theme.MyWorkerLodgerTheme

@Composable
fun PropertyDetailScreen(
    onBack: () -> Unit,
    onOpenRoom: (RoomId) -> Unit,
    viewModel: PropertyDetailViewModel = viewModel(factory = PropertyDetailViewModel.Factory),
) {
    RefreshOnResume(viewModel::refresh)
    DeletionDialog(viewModel.deletion, R.string.delete_property, R.string.delete_property_message, onDeleted = onBack)
    val state by viewModel.state.collectAsStateWithLifecycle()
    PropertyDetailContent(
        state = state,
        onBack = onBack,
        onRename = viewModel::startRenaming,
        onDelete = viewModel.deletion::start,
        onAddRoom = viewModel::startAddingRoom,
        onOpenRoom = onOpenRoom,
    )
    when (val dialog = state.dialog) {
        is PropertyDialog.Rename -> TextEntryDialog(
            title = R.string.rename_property,
            fieldLabel = R.string.property_name,
            entry = dialog.entry,
            onTextChange = viewModel::changeText,
            onSave = viewModel::save,
            onDismiss = viewModel::closeDialog,
        )
        is PropertyDialog.AddRoom -> TextEntryDialog(
            title = R.string.add_room,
            fieldLabel = R.string.room_name,
            entry = dialog.entry,
            onTextChange = viewModel::changeText,
            onSave = viewModel::save,
            onDismiss = viewModel::closeDialog,
            hint = R.string.room_name_hint,
        ) {
            SexChoice(selected = dialog.sex, onSelect = viewModel::chooseSex)
        }
        null -> Unit
    }
}

@Composable
private fun PropertyDetailContent(
    state: PropertyDetailUiState,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddRoom: () -> Unit,
    onOpenRoom: (RoomId) -> Unit,
) {
    val property = state.property
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ScreenTopBar(title = property?.name.orEmpty(), onBack = onBack) {
                if (property != null) TextButton(onClick = onRename) { Text(stringResource(R.string.rename)) }
                if (property != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
            }
        },
        floatingActionButton = {
            if (property != null) {
                ExtendedFloatingActionButton(onClick = onAddRoom) { Text(stringResource(R.string.add_room)) }
            }
        },
    ) { innerPadding ->
        val message = when {
            !state.loaded -> null
            property == null -> R.string.rejection_property_not_found
            state.rooms.isEmpty() -> R.string.no_rooms_yet
            else -> null
        }
        ContentOrMessage(innerPadding, message) {
            LazyColumn(Modifier.fillMaxSize().padding(innerPadding)) {
                item { TodayOccupancy(state.occupancy, state.loaded) }
                items(state.rooms, key = { it.id.value }) { room ->
                    ListItem(
                        headlineContent = { Text(room.name) },
                        supportingContent = {
                            val occupancy = state.roomOccupancy[room.id]?.summary().orEmpty()
                            Text(stringResource(R.string.room_summary, stringResource(room.sex.labelRes()), occupancy))
                        },
                        modifier = Modifier.clickable { onOpenRoom(room.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PropertyDetailPreview() {
    val property = Property(PropertyId("1"), "Al Quoz Block A")
    MyWorkerLodgerTheme {
        PropertyDetailContent(
            state = PropertyDetailUiState(
                loaded = true,
                property = property,
                rooms = listOf(
                    Room(RoomId("1"), property.id, "B-204", Sex.Male),
                    Room(RoomId("2"), property.id, "B-205", Sex.Female),
                ),
            ),
            onBack = {}, onRename = {}, onDelete = {}, onAddRoom = {}, onOpenRoom = {},
        )
    }
}
