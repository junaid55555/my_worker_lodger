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

    fun employer(id: EmployerId): Employer?
    fun employers(): List<Employer>
    fun saveEmployer(employer: Employer)

    fun worker(id: WorkerId): Worker?
    fun workers(): List<Worker>
    fun saveWorker(worker: Worker)

    fun stays(): List<Stay>
    /** Saves all [stays] together: either every one is saved or none is. */
    fun saveStays(stays: List<Stay>)

    /** Removes everything in [removal] together: either all of it is removed or none of it is. */
    fun remove(removal: Removal)
}

/** Records to remove from a [HousingStore] in one go, such as a Room together with its Beds. */
data class Removal(
    val propertyIds: Set<PropertyId> = emptySet(),
    val roomIds: Set<RoomId> = emptySet(),
    val bedIds: Set<BedId> = emptySet(),
    val employerIds: Set<EmployerId> = emptySet(),
    val workerIds: Set<WorkerId> = emptySet(),
)
