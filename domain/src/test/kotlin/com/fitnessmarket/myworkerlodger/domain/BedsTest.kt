package com.fitnessmarket.myworkerlodger.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BedsTest {

    private val housing = newHousing()
    private val property = housing.addProperty("Al Quoz Block A").accepted()
    private val room = housing.addRoom(property.id, "B-204", Sex.Male).accepted()

    @Test
    fun `an added Bed appears in its Room's Beds`() {
        housing.addBeds(room.id, listOf("3B"))

        assertEquals(listOf("3B"), housing.beds(room.id).map { it.label })
    }

    @Test
    fun `several Beds can be added at once and are listed in natural order`() {
        housing.addBeds(room.id, listOf("10", "2B", "2a", "1"))

        assertEquals(listOf("1", "2a", "2B", "10"), housing.beds(room.id).map { it.label })
    }

    @Test
    fun `if any label is blank, no Beds are added`() {
        val outcome = housing.addBeds(room.id, listOf("1A", " ", "1B"))

        assertEquals(Outcome.Rejected(Rejection.BlankLabel), outcome)
        assertEquals(emptyList<Bed>(), housing.beds(room.id))
    }

    @Test
    fun `adding no labels at all is rejected`() {
        val outcome = housing.addBeds(room.id, emptyList())

        assertEquals(Outcome.Rejected(Rejection.BlankLabel), outcome)
    }

    @Test
    fun `spaces around Bed labels are not kept`() {
        housing.addBeds(room.id, listOf(" 1A", "1B "))

        assertEquals(listOf("1A", "1B"), housing.beds(room.id).map { it.label })
    }

    @Test
    fun `adding Beds to a Room that does not exist is rejected`() {
        val outcome = housing.addBeds(RoomId("no-such-room"), listOf("1A"))

        assertEquals(Outcome.Rejected(Rejection.RoomNotFound), outcome)
    }

    @Test
    fun `a relabelled Bed is listed under its new label`() {
        val bed = housing.addBeds(room.id, listOf("3B")).accepted().single()

        housing.relabelBed(bed.id, " 3C ")

        assertEquals(listOf("3C"), housing.beds(room.id).map { it.label })
    }

    @Test
    fun `relabelling a Bed with a blank label is rejected and keeps the old label`() {
        val bed = housing.addBeds(room.id, listOf("3B")).accepted().single()

        val outcome = housing.relabelBed(bed.id, "   ")

        assertEquals(Outcome.Rejected(Rejection.BlankLabel), outcome)
        assertEquals(listOf("3B"), housing.beds(room.id).map { it.label })
    }

    @Test
    fun `relabelling a Bed that does not exist is rejected, even with a blank label`() {
        val outcome = housing.relabelBed(BedId("no-such-bed"), " ")

        assertEquals(Outcome.Rejected(Rejection.BedNotFound), outcome)
    }
}
