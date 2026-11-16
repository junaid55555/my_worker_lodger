package com.fitnessmarket.myworkerlodger.domain

/** The result of a command: either it was accepted, or a housing rule rejected it. */
sealed interface Outcome<out T> {
    data class Accepted<out T>(val value: T) : Outcome<T>
    data class Rejected(val rejection: Rejection) : Outcome<Nothing>
}

/** The housing rule that stopped a command. The app turns each one into a message. */
sealed interface Rejection {
    /** A name was empty or only whitespace. */
    data object BlankName : Rejection

    /** A Bed label was empty or only whitespace. */
    data object BlankLabel : Rejection

    /** The Property the command refers to doesn't exist (for example, it was deleted). */
    data object PropertyNotFound : Rejection

    /** The Room the command refers to doesn't exist. */
    data object RoomNotFound : Rejection

    /** The Bed the command refers to doesn't exist. */
    data object BedNotFound : Rejection

    /** The Employer the command refers to doesn't exist. */
    data object EmployerNotFound : Rejection

    /** The Worker the command refers to doesn't exist. */
    data object WorkerNotFound : Rejection

    /** A Staff Number was empty or only whitespace. */
    data object BlankStaffNumber : Rejection

    /** Another Worker of the same Employer already has this Staff Number. */
    data object DuplicateStaffNumber : Rejection

    /** The date is after today; only things that have actually happened can be recorded. */
    data object FutureDate : Rejection

    /** Another Worker has a current Stay in this Bed. */
    data object BedOccupied : Rejection

    /** The Worker already has a current Stay, so they can't be checked in to a second Bed. */
    data object WorkerAlreadyHoused : Rejection

    /** The Worker's sex doesn't match the sex the Room is designated for. */
    data object SexMismatch : Rejection

    /** The Worker has no current Stay, so there is nothing to check out of. */
    data object WorkerNotHoused : Rejection

    /** A Check-out can't be dated before the Stay's Check-in. */
    data object CheckOutBeforeCheckIn : Rejection

    /** The dates would overlap another Stay of the same Worker or in the same Bed. */
    data object OverlappingStay : Rejection

    /** A Move must be to a different Bed from the one the Worker is staying in. */
    data object AlreadyInThatBed : Rejection
}
