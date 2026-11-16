package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate

@JvmInline
value class StayId(val value: String)

/**
 * One Worker living in one Bed, from [checkIn] until [checkOut]. A Stay with no Check-out yet is
 * the Worker's current Stay. A Stay covers the nights from Check-in up to, but not including,
 * Check-out, so a Bed left on a date can be checked in to on that same date.
 */
data class Stay(
    val id: StayId,
    val workerId: WorkerId,
    val bedId: BedId,
    val checkIn: LocalDate,
    val checkOut: LocalDate? = null,
) {
    val isCurrent: Boolean get() = checkOut == null

    /** Whether this Stay shares any night with the nights from [from] up to [until] (open-ended when null). */
    fun overlaps(from: LocalDate, until: LocalDate?): Boolean =
        (until == null || checkIn.isBefore(until)) && (checkOut == null || from.isBefore(checkOut))
}

/** A Bed together with where it is: its Room and that Room's Property. */
data class BedDetails(val bed: Bed, val room: Room, val property: Property)

/** A Stay together with where it is: its Bed, that Bed's Room, and that Room's Property. */
data class StayDetails(val stay: Stay, val place: BedDetails) {
    val bed: Bed get() = place.bed
    val room: Room get() = place.room
    val property: Property get() = place.property
}
