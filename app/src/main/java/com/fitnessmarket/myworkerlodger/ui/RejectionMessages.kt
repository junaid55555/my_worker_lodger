package com.fitnessmarket.myworkerlodger.ui

import androidx.annotation.StringRes
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.domain.Rejection
import com.fitnessmarket.myworkerlodger.domain.Sex

/** The message shown to the Accommodation Manager for each housing rule. */
@StringRes
fun Rejection.messageRes(): Int = when (this) {
    Rejection.BlankName -> R.string.rejection_blank_name
    Rejection.BlankLabel -> R.string.rejection_blank_label
    Rejection.BlankStaffNumber -> R.string.rejection_blank_staff_number
    Rejection.DuplicateStaffNumber -> R.string.rejection_duplicate_staff_number
    Rejection.PropertyNotFound -> R.string.rejection_property_not_found
    Rejection.RoomNotFound -> R.string.rejection_room_not_found
    Rejection.BedNotFound -> R.string.rejection_bed_not_found
    Rejection.EmployerNotFound -> R.string.rejection_employer_not_found
    Rejection.WorkerNotFound -> R.string.rejection_worker_not_found
}

/** How a [Sex] is shown. */
@StringRes
fun Sex.labelRes(): Int = when (this) {
    Sex.Male -> R.string.sex_male
    Sex.Female -> R.string.sex_female
}
