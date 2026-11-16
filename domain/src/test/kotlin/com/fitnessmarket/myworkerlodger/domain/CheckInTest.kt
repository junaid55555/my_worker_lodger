package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckInTest {

    private val housing = newHousing()
    private val today: LocalDate = TODAY

    private val property = housing.addProperty("Al Quoz Block A").accepted()
    private val maleRoom = housing.addRoom(property.id, "B-204", Sex.Male).accepted()
    private val bed1A = housing.addBeds(maleRoom.id, listOf("1A")).accepted().single()

    private val gulf = housing.addEmployer("Gulf Builders").accepted()
    private val ahmed = housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male).accepted()

    @Test
    fun `a checked-in Worker is in the Bed, Room and Property they checked in to`() {
        housing.checkIn(ahmed.id, bed1A.id, today)

        val place = housing.currentPlace(ahmed.id)
        assertEquals(
            listOf(bed1A.label, maleRoom.name, property.name, today),
            listOf(place?.bed?.label, place?.room?.name, place?.property?.name, place?.stay?.checkIn),
        )
        assertEquals(mapOf(bed1A.id to ahmed), housing.occupants(maleRoom.id))
    }

    private val bed1B = housing.addBeds(maleRoom.id, listOf("1B")).accepted().single()
    private val bilal = housing.addWorker(gulf.id, "1043", "Bilal Ahmed", Sex.Male).accepted()

    @Test
    fun `a Check-in can be recorded on a past date`() {
        housing.checkIn(ahmed.id, bed1A.id, today.minusDays(3))

        assertEquals(today.minusDays(3), housing.currentPlace(ahmed.id)?.stay?.checkIn)
    }

    @Test
    fun `a Check-in dated in the future is rejected`() {
        val outcome = housing.checkIn(ahmed.id, bed1A.id, today.plusDays(1))

        assertEquals(Outcome.Rejected(Rejection.FutureDate), outcome)
        assertEquals(null, housing.currentPlace(ahmed.id))
    }

    @Test
    fun `checking in to a Bed with a current Stay is rejected`() {
        housing.checkIn(ahmed.id, bed1A.id, today)

        val outcome = housing.checkIn(bilal.id, bed1A.id, today)

        assertEquals(Outcome.Rejected(Rejection.BedOccupied), outcome)
        assertEquals(mapOf(bed1A.id to ahmed), housing.occupants(maleRoom.id))
    }

    @Test
    fun `checking in a Worker who already has a current Stay is rejected`() {
        housing.checkIn(ahmed.id, bed1A.id, today)

        val outcome = housing.checkIn(ahmed.id, bed1B.id, today)

        assertEquals(Outcome.Rejected(Rejection.WorkerAlreadyHoused), outcome)
        assertEquals(bed1A.id, housing.currentPlace(ahmed.id)?.bed?.id)
    }

    @Test
    fun `checking a Worker in to a Room of the other sex is rejected`() {
        val priya = housing.addWorker(gulf.id, "2001", "Priya Nair", Sex.Female).accepted()

        val outcome = housing.checkIn(priya.id, bed1A.id, today)

        assertEquals(Outcome.Rejected(Rejection.SexMismatch), outcome)
        assertEquals(null, housing.currentPlace(priya.id))
    }

    @Test
    fun `Workers of different Employers can share a Room`() {
        val noor = housing.addEmployer("Al Noor Facilities").accepted()
        val ravi = housing.addWorker(noor.id, "77", "Ravi Kumar", Sex.Male).accepted()

        housing.checkIn(ahmed.id, bed1A.id, today)
        housing.checkIn(ravi.id, bed1B.id, today)

        assertEquals(mapOf(bed1A.id to ahmed, bed1B.id to ravi), housing.occupants(maleRoom.id))
    }

    @Test
    fun `a past-dated Check-in that overlaps the Worker's earlier Stay is rejected`() {
        housing.checkIn(ahmed.id, bed1A.id, today.minusDays(10))
        housing.checkOut(ahmed.id, today.minusDays(5))

        val outcome = housing.checkIn(ahmed.id, bed1B.id, today.minusDays(7))

        assertEquals(Outcome.Rejected(Rejection.OverlappingStay), outcome)
    }

    @Test
    fun `a past-dated Check-in that overlaps an earlier Stay in the same Bed is rejected`() {
        housing.checkIn(bilal.id, bed1A.id, today.minusDays(10))
        housing.checkOut(bilal.id, today.minusDays(5))

        val outcome = housing.checkIn(ahmed.id, bed1A.id, today.minusDays(7))

        assertEquals(Outcome.Rejected(Rejection.OverlappingStay), outcome)
    }

    @Test
    fun `a Bed can be checked in to on the date its last Stay ended`() {
        housing.checkIn(bilal.id, bed1A.id, today.minusDays(10))
        housing.checkOut(bilal.id, today.minusDays(5))

        housing.checkIn(ahmed.id, bed1A.id, today.minusDays(5)).accepted()

        assertEquals(mapOf(bed1A.id to ahmed), housing.occupants(maleRoom.id))
    }

    @Test
    fun `the Beds offered for a Worker are the Available Beds in Rooms of their sex, in order`() {
        val femaleRoom = housing.addRoom(property.id, "B-205", Sex.Female).accepted()
        housing.addBeds(femaleRoom.id, listOf("1A"))
        val otherProperty = housing.addProperty("Jebel Ali Block 3").accepted()
        val otherRoom = housing.addRoom(otherProperty.id, "C-1", Sex.Male).accepted()
        housing.addBeds(otherRoom.id, listOf("2", "10"))
        housing.checkIn(bilal.id, bed1A.id, today)

        assertEquals(
            listOf("Al Quoz Block A B-204 1B", "Jebel Ali Block 3 C-1 2", "Jebel Ali Block 3 C-1 10"),
            housing.bedsAvailableFor(ahmed.id).map { "${it.property.name} ${it.room.name} ${it.bed.label}" },
        )
    }

    @Test
    fun `checking in a Worker or to a Bed that does not exist is rejected`() {
        assertEquals(
            Outcome.Rejected(Rejection.WorkerNotFound),
            housing.checkIn(WorkerId("no-such-worker"), bed1A.id, today),
        )
        assertEquals(
            Outcome.Rejected(Rejection.BedNotFound),
            housing.checkIn(ahmed.id, BedId("no-such-bed"), today),
        )
    }
}
