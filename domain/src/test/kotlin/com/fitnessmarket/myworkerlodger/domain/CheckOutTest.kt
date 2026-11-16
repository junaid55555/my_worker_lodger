package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckOutTest {

    private val housing = newHousing()
    private val today: LocalDate = TODAY

    private val property = housing.addProperty("Al Quoz Block A").accepted()
    private val room = housing.addRoom(property.id, "B-204", Sex.Male).accepted()
    private val bed = housing.addBeds(room.id, listOf("1A")).accepted().single()

    private val gulf = housing.addEmployer("Gulf Builders").accepted()
    private val ahmed = housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male).accepted()

    @Test
    fun `a checked-out Worker has no current Stay, the Bed is free, and the Stay is kept`() {
        housing.checkIn(ahmed.id, bed.id, today.minusDays(10))

        housing.checkOut(ahmed.id, today)

        assertEquals(null, housing.currentPlace(ahmed.id))
        assertEquals(emptyMap<BedId, Worker>(), housing.occupants(room.id))
        assertEquals(
            listOf(today.minusDays(10) to today),
            housing.staysOf(ahmed.id).map { it.stay.checkIn to it.stay.checkOut },
        )
    }

    @Test
    fun `a Check-out can be recorded on a past date`() {
        housing.checkIn(ahmed.id, bed.id, today.minusDays(10))

        housing.checkOut(ahmed.id, today.minusDays(2))

        assertEquals(today.minusDays(2), housing.staysOf(ahmed.id).single().stay.checkOut)
    }

    @Test
    fun `a Check-out dated in the future is rejected and the Stay stays current`() {
        housing.checkIn(ahmed.id, bed.id, today)

        val outcome = housing.checkOut(ahmed.id, today.plusDays(1))

        assertEquals(Outcome.Rejected(Rejection.FutureDate), outcome)
        assertEquals(bed.id, housing.currentPlace(ahmed.id)?.bed?.id)
    }

    @Test
    fun `a Check-out before the Check-in is rejected, but the same date is accepted`() {
        housing.checkIn(ahmed.id, bed.id, today.minusDays(1))

        assertEquals(Outcome.Rejected(Rejection.CheckOutBeforeCheckIn), housing.checkOut(ahmed.id, today.minusDays(2)))
        assertEquals(today.minusDays(1), housing.checkOut(ahmed.id, today.minusDays(1)).accepted().checkOut)
    }

    @Test
    fun `checking out a Worker with no current Stay is rejected`() {
        assertEquals(Outcome.Rejected(Rejection.WorkerNotHoused), housing.checkOut(ahmed.id, today))
    }

    @Test
    fun `checking out a Worker that does not exist is rejected`() {
        assertEquals(Outcome.Rejected(Rejection.WorkerNotFound), housing.checkOut(WorkerId("no-such-worker"), today))
    }
}
