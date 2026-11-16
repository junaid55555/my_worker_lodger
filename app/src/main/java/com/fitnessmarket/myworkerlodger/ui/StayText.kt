package com.fitnessmarket.myworkerlodger.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.Stay
import com.fitnessmarket.myworkerlodger.ui.common.display

/** "3 Nov 2026 – 10 Nov 2026", or "3 Nov 2026 – now" for a current Stay. */
@Composable
fun Stay.stayDates(): String {
    val end = checkOut?.display() ?: stringResource(R.string.now_lowercase)
    return stringResource(R.string.stay_dates, checkIn.display(), end)
}
