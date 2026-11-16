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
}
