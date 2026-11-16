package com.fitnessmarket.myworkerlodger.domain

/**
 * How full a Room, a Property or the whole accommodation is today. Out of Service Beds count in
 * neither number.
 */
data class Occupancy(val bedsWithCurrentStay: Int, val inServiceBeds: Int) {
    /** In-service Beds with no current Stay. */
    val availableBeds: Int get() = inServiceBeds - bedsWithCurrentStay

    /** The share of in-service Beds with a current Stay, or null when there are no in-service Beds. */
    val share: Double? get() = if (inServiceBeds == 0) null else bedsWithCurrentStay.toDouble() / inServiceBeds

    operator fun plus(other: Occupancy) =
        Occupancy(bedsWithCurrentStay + other.bedsWithCurrentStay, inServiceBeds + other.inServiceBeds)

    companion object {
        val NONE = Occupancy(0, 0)
    }
}
