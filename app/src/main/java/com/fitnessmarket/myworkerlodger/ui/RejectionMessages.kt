package com.fitnessmarket.myworkerlodger.ui

import androidx.annotation.StringRes
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.Rejection

/** The message shown to the Accommodation Manager for each housing rule. */
@StringRes
fun Rejection.messageRes(): Int = when (this) {
    Rejection.BlankName -> R.string.rejection_blank_name
    Rejection.PropertyNotFound -> R.string.rejection_property_not_found
}
