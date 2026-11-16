package com.fitnessmarket.myworkerlodger.ui.room

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.Bed
import com.fitnessmarket.myworkerlodger.domain.BedId
import com.fitnessmarket.myworkerlodger.domain.PropertyId
import com.fitnessmarket.myworkerlodger.domain.Room
import com.fitnessmarket.myworkerlodger.domain.RoomId
import com.fitnessmarket.myworkerlodger.domain.Sex
import com.fitnessmarket.myworkerlodger.ui.common.ContentOrMessage
import com.fitnessmarket.myworkerlodger.ui.common.RefreshOnResume
import com.fitnessmarket.myworkerlodger.ui.common.ScreenTopBar
import com.fitnessmarket.myworkerlodger.ui.common.TextEntryDialog
import com.fitnessmarket.myworkerlodger.ui.labelRes
import com.fitnessmarket.myworkerlodger.ui.theme.MyWorkerLodgerTheme

@Composable
fun RoomDetailScreen(
    onBack: () -> Unit,
    viewModel: RoomDetailViewModel = viewModel(factory = RoomDetailViewModel.Factory),
) {
    RefreshOnResume(viewModel::refresh)
    val state by viewModel.state.collectAsStateWithLifecycle()
    RoomDetailContent(
        state = state,
        onBack = onBack,
        onRename = viewModel::startRenaming,
        onAddBeds = viewModel::startAddingBeds,
        onBedClick = viewModel::startRelabelling,
    )
    state.dialog?.let { dialog ->
        TextEntryDialog(
            title = when (dialog) {
                is RoomDialog.Rename -> R.string.rename_room
                is RoomDialog.AddBeds -> R.string.add_beds
                is RoomDialog.RelabelBed -> R.string.relabel_bed
            },
            fieldLabel = when (dialog) {
                is RoomDialog.Rename -> R.string.room_name
                is RoomDialog.AddBeds -> R.string.bed_labels
                is RoomDialog.RelabelBed -> R.string.bed_label
            },
            entry = dialog.entry,
            onTextChange = viewModel::changeText,
            onSave = viewModel::save,
            onDismiss = viewModel::closeDialog,
            hint = R.string.bed_labels_hint.takeIf { dialog is RoomDialog.AddBeds },
        )
    }
}

@Composable
private fun RoomDetailContent(
    state: RoomDetailUiState,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onAddBeds: () -> Unit,
    onBedClick: (Bed) -> Unit,
) {
    val room = state.room
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ScreenTopBar(
                title = room?.name.orEmpty(),
                subtitle = room?.let { stringResource(it.sex.labelRes()) },
                onBack = onBack,
            ) {
                if (room != null) TextButton(onClick = onRename) { Text(stringResource(R.string.rename)) }
            }
        },
        floatingActionButton = {
            if (room != null) {
                ExtendedFloatingActionButton(onClick = onAddBeds) { Text(stringResource(R.string.add_beds)) }
            }
        },
    ) { innerPadding ->
        val message = when {
            !state.loaded -> null
            room == null -> R.string.rejection_room_not_found
            state.beds.isEmpty() -> R.string.no_beds_yet
            else -> null
        }
        ContentOrMessage(innerPadding, message) {
            LazyColumn(Modifier.fillMaxSize().padding(innerPadding)) {
                items(state.beds, key = { it.id.value }) { bed ->
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.bed_title, bed.label)) },
                        supportingContent = {
                            Text(state.workersByBed[bed.id]?.name ?: stringResource(R.string.available))
                        },
                        modifier = Modifier.clickable { onBedClick(bed) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RoomDetailPreview() {
    val room = Room(RoomId("1"), PropertyId("1"), "B-204", Sex.Male)
    MyWorkerLodgerTheme {
        RoomDetailContent(
            state = RoomDetailUiState(
                loaded = true,
                room = room,
                beds = listOf("1A", "1B", "2A", "2B").map { Bed(BedId(it), room.id, it) },
            ),
            onBack = {}, onRename = {}, onAddBeds = {}, onBedClick = {},
        )
    }
}
