package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class OutOfServiceTest {

    private val housing = newHousing()
    private val today: LocalDate = TODAY

    private val property = housing.addProperty("Al Quoz Block A").accepted()
    private val room = housing.addRoom(property.id, "B-204", Sex.Male).accepted()
    private val bed1A = housing.addBeds(room.id, listOf("1A")).accepted().single()
    private val bed1B = housing.addBeds(room.id, listOf("1B")).accepted().single()

    private val gulf = housing.addEmployer("Gulf Builders").accepted()
    private val ahmed = housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male).accepted()

    @Test
    fun `a Bed can be marked Out of Service and put back in service`() {
        housing.markOutOfService(bed1A.id).accepted()
        assertEquals(listOf(false, true), housing.beds(room.id).map { it.inService })

        housing.putBackInService(bed1A.id).accepted()
        assertEquals(listOf(true, true), housing.beds(room.id).map { it.inService })
    }

    @Test
    fun `a Bed with a current Stay can't be marked Out of Service`() {
        housing.checkIn(ahmed.id, bed1A.id, today).accepted()

        assertEquals(Outcome.Rejected(Rejection.BedOccupied), housing.markOutOfService(bed1A.id))
        assertEquals(true, housing.beds(room.id).first().inService)
    }

    @Test
    fun `checking in to an Out of Service Bed is rejected`() {
        housing.markOutOfService(bed1A.id).accepted()

        assertEquals(Outcome.Rejected(Rejection.BedOutOfService), housing.checkIn(ahmed.id, bed1A.id, today))
        assertEquals(null, housing.currentStay(ahmed.id))
    }

    @Test
    fun `a Bed put back in service can be checked in to and is offered again`() {
        housing.markOutOfService(bed1A.id).accepted()
        housing.putBackInService(bed1A.id).accepted()

        assertEquals(listOf("1A", "1B"), housing.bedsAvailableFor(ahmed.id).map { it.bed.label })
        housing.checkIn(ahmed.id, bed1A.id, today).accepted()
    }

    @Test
    fun `moving to an Out of Service Bed is rejected`() {
        housing.checkIn(ahmed.id, bed1A.id, today).accepted()
        housing.markOutOfService(bed1B.id).accepted()

        assertEquals(Outcome.Rejected(Rejection.BedOutOfService), housing.move(ahmed.id, bed1B.id, today))
        assertEquals(bed1A.id, housing.currentStay(ahmed.id)?.bed?.id)
    }

    @Test
    fun `Out of Service Beds are not offered when choosing a Bed`() {
        housing.markOutOfService(bed1A.id).accepted()

        assertEquals(listOf("1B"), housing.bedsAvailableFor(ahmed.id).map { it.bed.label })
    }

    @Test
    fun `marking a Bed that does not exist Out of Service is rejected`() {
        assertEquals(Outcome.Rejected(Rejection.BedNotFound), housing.markOutOfService(BedId("no-such-bed")))
    }

    @Test
    fun `a Bed's label and service state can be changed together`() {
        housing.editBed(bed1A.id, " 1C ", inService = false).accepted()

        assertEquals(listOf("1B" to true, "1C" to false), housing.beds(room.id).map { it.label to it.inService })
    }

    @Test
    fun `editing a Bed is all or nothing`() {
        housing.checkIn(ahmed.id, bed1A.id, today).accepted()

        assertEquals(Outcome.Rejected(Rejection.BedOccupied), housing.editBed(bed1A.id, "1C", inService = false))
        assertEquals(Outcome.Rejected(Rejection.BlankLabel), housing.editBed(bed1B.id, " ", inService = false))
        assertEquals(listOf("1A" to true, "1B" to true), housing.beds(room.id).map { it.label to it.inService })
    }
}
