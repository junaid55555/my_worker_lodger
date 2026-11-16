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
        val property = store.property(id) ?: return Outcome.Rejected(Rejection.PropertyNotFound)
        if (name.isBlank()) return Outcome.Rejected(Rejection.BlankName)
        val renamed = property.copy(name = name.trim())
        store.saveProperty(renamed)
        return Outcome.Accepted(renamed)
    }

    fun property(id: PropertyId): Property? = store.property(id)

    fun properties(): List<Property> =
        store.properties().sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    fun addRoom(propertyId: PropertyId, name: String, sex: Sex): Outcome<Room> {
        if (store.property(propertyId) == null) return Outcome.Rejected(Rejection.PropertyNotFound)
        if (name.isBlank()) return Outcome.Rejected(Rejection.BlankName)
        val room = Room(RoomId(UUID.randomUUID().toString()), propertyId, name.trim(), sex)
        store.saveRoom(room)
        return Outcome.Accepted(room)
    }

    fun renameRoom(id: RoomId, name: String): Outcome<Room> {
        val room = store.room(id) ?: return Outcome.Rejected(Rejection.RoomNotFound)
        if (name.isBlank()) return Outcome.Rejected(Rejection.BlankName)
        val renamed = room.copy(name = name.trim())
        store.saveRoom(renamed)
        return Outcome.Accepted(renamed)
    }

    fun addBeds(roomId: RoomId, labels: List<String>): Outcome<List<Bed>> {
        if (store.room(roomId) == null) return Outcome.Rejected(Rejection.RoomNotFound)
        if (labels.isEmpty() || labels.any { it.isBlank() }) return Outcome.Rejected(Rejection.BlankLabel)
        val beds = labels.map { Bed(BedId(UUID.randomUUID().toString()), roomId, it.trim()) }
        store.saveBeds(beds)
        return Outcome.Accepted(beds)
    }

    fun relabelBed(id: BedId, label: String): Outcome<Bed> {
        val bed = store.bed(id) ?: return Outcome.Rejected(Rejection.BedNotFound)
        if (label.isBlank()) return Outcome.Rejected(Rejection.BlankLabel)
        val relabelled = bed.copy(label = label.trim())
        store.saveBeds(listOf(relabelled))
        return Outcome.Accepted(relabelled)
    }

    fun beds(roomId: RoomId): List<Bed> = store.beds(roomId).sortedWith(compareBy(NaturalOrder) { it.label })

    fun room(id: RoomId): Room? = store.room(id)

    fun rooms(propertyId: PropertyId): List<Room> =
        store.rooms(propertyId).sortedWith(compareBy(NaturalOrder) { it.name })
}
