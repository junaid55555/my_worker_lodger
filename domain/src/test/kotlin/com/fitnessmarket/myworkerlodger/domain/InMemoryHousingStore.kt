package com.fitnessmarket.myworkerlodger.domain

/** A [HousingStore] that keeps everything in memory, for domain tests. */
class InMemoryHousingStore : HousingStore {
    private val properties = linkedMapOf<PropertyId, Property>()

    override fun property(id: PropertyId): Property? = properties[id]

    override fun properties(): List<Property> = properties.values.toList()

    override fun saveProperty(property: Property) {
        properties[property.id] = property
    }

    private val rooms = linkedMapOf<RoomId, Room>()

    override fun room(id: RoomId): Room? = rooms[id]

    override fun rooms(propertyId: PropertyId): List<Room> = rooms.values.filter { it.propertyId == propertyId }

    override fun saveRoom(room: Room) {
        rooms[room.id] = room
    }

    private val beds = linkedMapOf<BedId, Bed>()

    override fun bed(id: BedId): Bed? = beds[id]

    override fun beds(roomId: RoomId): List<Bed> = beds.values.filter { it.roomId == roomId }

    override fun saveBeds(beds: List<Bed>) {
        beds.forEach { this.beds[it.id] = it }
    }

    private val employers = linkedMapOf<EmployerId, Employer>()

    override fun employer(id: EmployerId): Employer? = employers[id]

    override fun employers(): List<Employer> = employers.values.toList()

    override fun saveEmployer(employer: Employer) {
        employers[employer.id] = employer
    }

    private val workers = linkedMapOf<WorkerId, Worker>()

    override fun worker(id: WorkerId): Worker? = workers[id]

    override fun workers(): List<Worker> = workers.values.toList()

    override fun saveWorker(worker: Worker) {
        workers[worker.id] = worker
    }

    private val stays = linkedMapOf<StayId, Stay>()

    override fun stays(): List<Stay> = stays.values.toList()

    override fun saveStays(stays: List<Stay>) {
        stays.forEach { this.stays[it.id] = it }
    }

    override fun remove(removal: Removal) {
        properties.keys.removeAll(removal.propertyIds)
        rooms.keys.removeAll(removal.roomIds)
        beds.keys.removeAll(removal.bedIds)
        employers.keys.removeAll(removal.employerIds)
        workers.keys.removeAll(removal.workerIds)
    }
}
