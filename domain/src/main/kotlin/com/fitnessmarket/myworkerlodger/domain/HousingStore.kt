package com.fitnessmarket.myworkerlodger.domain

/**
 * Where [Housing] keeps its records. The domain defines this interface; the app supplies an
 * on-device implementation (ADR-0001) and tests use an in-memory one.
 */
interface HousingStore {
    fun property(id: PropertyId): Property?
    fun properties(): List<Property>
    fun saveProperty(property: Property)

    fun room(id: RoomId): Room?
    fun rooms(propertyId: PropertyId): List<Room>
    fun saveRoom(room: Room)

    fun bed(id: BedId): Bed?
    fun beds(roomId: RoomId): List<Bed>

    /** Saves all [beds] together: either every one is saved or none is. */
    fun saveBeds(beds: List<Bed>)
}
