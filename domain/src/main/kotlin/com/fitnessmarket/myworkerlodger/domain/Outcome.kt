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
}
