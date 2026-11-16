package com.fitnessmarket.myworkerlodger.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.Occupancy
import kotlin.math.roundToInt

/** "75% full · 1 Available Bed", or a note that there are no in-service Beds to fill. */
@Composable
fun Occupancy.summary(): String {
    val share = share ?: return stringResource(R.string.no_in_service_beds)
    val available = pluralStringResource(R.plurals.available_beds, availableBeds, availableBeds)
    return stringResource(R.string.occupancy_summary, (share * 100).roundToInt(), available)
}
