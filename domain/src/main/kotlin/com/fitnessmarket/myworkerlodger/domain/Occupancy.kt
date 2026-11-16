package com.fitnessmarket.myworkerlodger.domain

/**
 * How full a Room, a Property or every Property together is today. Out of Service Beds count in
 * neither number.
 */
data class Occupancy(val bedsWithCurrentStay: Int, val inServiceBeds: Int) {
    /** In-service Beds with no current Stay. */
    val availableBeds: Int get() = inServiceBeds - bedsWithCurrentStay

    /** The share of in-service Beds with a current Stay, or null when there are no in-service Beds. */
    val share: Double? get() = if (inServiceBeds == 0) null else bedsWithCurrentStay.toDouble() / inServiceBeds

    /**
     * The share as a whole percentage, rounded down so it never claims 100% while a Bed is still
     * available, and at least 1 whenever any Bed has a current Stay. Null when there are no in-service Beds.
     */
    val percentFull: Int?
        get() {
            if (inServiceBeds == 0) return null
            val rounded = bedsWithCurrentStay * 100 / inServiceBeds
            return if (bedsWithCurrentStay > 0) maxOf(rounded, 1) else rounded
        }

    operator fun plus(other: Occupancy) =
        Occupancy(bedsWithCurrentStay + other.bedsWithCurrentStay, inServiceBeds + other.inServiceBeds)

    companion object {
        /** Nothing counted yet: the starting point when adding Occupancies together. */
        val ZERO = Occupancy(0, 0)
    }
}
