package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate

/** The fixed "today" every test runs on. */
val TODAY: LocalDate = LocalDate.of(2026, 11, 16)

/** A [Housing] backed by an empty in-memory store, on [TODAY]. */
fun newHousing(): Housing = Housing(store = InMemoryHousingStore(), today = { TODAY })

/** The value of an accepted [Outcome]; fails the test if the command was rejected. */
fun <T> Outcome<T>.accepted(): T = when (this) {
    is Outcome.Accepted -> value
    is Outcome.Rejected -> throw AssertionError("Expected the command to be accepted, but it was rejected: $rejection")
}
