package com.fitnessmarket.myworkerlodger.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomsTest {

    private val housing = newHousing()
    private val property = housing.addProperty("Al Quoz Block A").accepted()

    @Test
    fun `an added Room appears in its Property's Rooms with its sex`() {
        housing.addRoom(property.id, "B-204", Sex.Male)

        assertEquals(
            listOf("B-204" to Sex.Male),
            housing.rooms(property.id).map { it.name to it.sex },
        )
    }

    @Test
    fun `Rooms are listed in natural order, so numbers sort by value and case is ignored`() {
        housing.addRoom(property.id, "B-10", Sex.Male)
        housing.addRoom(property.id, "B-2", Sex.Male)
        housing.addRoom(property.id, "a-1", Sex.Female)

        assertEquals(listOf("a-1", "B-2", "B-10"), housing.rooms(property.id).map { it.name })
    }

    @Test
    fun `spaces around a Room name are not kept`() {
        housing.addRoom(property.id, "  B-204 ", Sex.Male)

        assertEquals(listOf("B-204"), housing.rooms(property.id).map { it.name })
    }

    @Test
    fun `a Room with a blank name is rejected and not added`() {
        val outcome = housing.addRoom(property.id, "  ", Sex.Female)

        assertEquals(Outcome.Rejected(Rejection.BlankName), outcome)
        assertEquals(emptyList<Room>(), housing.rooms(property.id))
    }

    @Test
    fun `adding a Room to a Property that does not exist is rejected`() {
        val outcome = housing.addRoom(PropertyId("no-such-property"), "B-204", Sex.Male)

        assertEquals(Outcome.Rejected(Rejection.PropertyNotFound), outcome)
    }

    @Test
    fun `a renamed Room keeps its sex and is listed under its new name`() {
        val room = housing.addRoom(property.id, "B-204", Sex.Female).accepted()

        housing.renameRoom(room.id, " B-205 ")

        assertEquals(
            listOf("B-205" to Sex.Female),
            housing.rooms(property.id).map { it.name to it.sex },
        )
    }

    @Test
    fun `renaming a Room to a blank name is rejected and keeps the old name`() {
        val room = housing.addRoom(property.id, "B-204", Sex.Male).accepted()

        val outcome = housing.renameRoom(room.id, "")

        assertEquals(Outcome.Rejected(Rejection.BlankName), outcome)
        assertEquals(listOf("B-204"), housing.rooms(property.id).map { it.name })
    }

    @Test
    fun `a Room can be found by its id, and an unknown id finds nothing`() {
        val room = housing.addRoom(property.id, "B-204", Sex.Male).accepted()

        assertEquals(room, housing.room(room.id))
        assertEquals(null, housing.room(RoomId("no-such-room")))
    }

    @Test
    fun `renaming a Room that does not exist is rejected, even with a blank name`() {
        val outcome = housing.renameRoom(RoomId("no-such-room"), "")

        assertEquals(Outcome.Rejected(Rejection.RoomNotFound), outcome)
    }
}
