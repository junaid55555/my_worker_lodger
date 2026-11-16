package com.fitnessmarket.myworkerlodger.ui

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.Occupancy

/** "75% full · 1 Available Bed", or a note that there are no in-service Beds to fill. */
@Composable
fun Occupancy.summary(): String {
    val percent = percentFull ?: return stringResource(R.string.no_in_service_beds)
    val available = pluralStringResource(R.plurals.available_beds, availableBeds, availableBeds)
    return stringResource(R.string.occupancy_summary, percent, available)
}

/** The "Today" line at the top of a Room or Property, shown once its figures have [loaded]. */
@Composable
fun TodayOccupancy(occupancy: Occupancy, loaded: Boolean) {
    if (!loaded) return
    ListItem(
        overlineContent = { Text(stringResource(R.string.today)) },
        headlineContent = { Text(occupancy.summary()) },
    )
    HorizontalDivider()
}
