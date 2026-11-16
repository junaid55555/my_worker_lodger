package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate
import java.util.UUID

/** Supplies today's date, so date rules can be tested with a fixed day. */
fun interface Today {
    fun date(): LocalDate
}

/**
 * The single public entry point to the worker housing domain. Every operation an
 * Accommodation Manager can perform goes through here.
 */
class Housing(
    private val store: HousingStore,
    private val today: Today,
) {
    fun addProperty(name: String): Outcome<Property> {
        if (name.isBlank()) return Outcome.Rejected(Rejection.BlankName)
        val property = Property(PropertyId(UUID.randomUUID().toString()), name.trim())
        store.saveProperty(property)
        return Outcome.Accepted(property)
    }

    fun renameProperty(id: PropertyId, name: String): Outcome<Property> {
        if (name.isBlank()) return Outcome.Rejected(Rejection.BlankName)
        val property = store.properties().find { it.id == id }
            ?: return Outcome.Rejected(Rejection.PropertyNotFound)
        val renamed = property.copy(name = name.trim())
        store.saveProperty(renamed)
        return Outcome.Accepted(renamed)
    }

    fun properties(): List<Property> =
        store.properties().sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
}
