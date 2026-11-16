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
        housing.markOutOfService(bed1A.id)
        assertEquals(listOf(false, true), housing.beds(room.id).map { it.inService })

        housing.putBackInService(bed1A.id)
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
        housing.markOutOfService(bed1A.id)

        assertEquals(Outcome.Rejected(Rejection.BedOutOfService), housing.checkIn(ahmed.id, bed1A.id, today))
    }

    @Test
    fun `moving to an Out of Service Bed is rejected`() {
        housing.checkIn(ahmed.id, bed1A.id, today).accepted()
        housing.markOutOfService(bed1B.id)

        assertEquals(Outcome.Rejected(Rejection.BedOutOfService), housing.move(ahmed.id, bed1B.id, today))
        assertEquals(bed1A.id, housing.currentStay(ahmed.id)?.bed?.id)
    }

    @Test
    fun `Out of Service Beds are not offered when choosing a Bed`() {
        housing.markOutOfService(bed1A.id)

        assertEquals(listOf("1B"), housing.bedsAvailableFor(ahmed.id).map { it.bed.label })
    }

    @Test
    fun `marking a Bed that does not exist is rejected`() {
        assertEquals(Outcome.Rejected(Rejection.BedNotFound), housing.markOutOfService(BedId("no-such-bed")))
    }
}
