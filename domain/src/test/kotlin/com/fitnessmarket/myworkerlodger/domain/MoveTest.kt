package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MoveTest {

    private val housing = newHousing()
    private val today: LocalDate = TODAY

    private val alQuoz = housing.addProperty("Al Quoz Block A").accepted()
    private val room204 = housing.addRoom(alQuoz.id, "B-204", Sex.Male).accepted()
    private val bed1A = housing.addBeds(room204.id, listOf("1A")).accepted().single()
    private val bed1B = housing.addBeds(room204.id, listOf("1B")).accepted().single()

    private val gulf = housing.addEmployer("Gulf Builders").accepted()
    private val ahmed = housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male).accepted()
    private val bilal = housing.addWorker(gulf.id, "1043", "Bilal Ahmed", Sex.Male).accepted()

    init {
        housing.checkIn(ahmed.id, bed1A.id, today.minusDays(10)).accepted()
    }

    @Test
    fun `a Move ends the current Stay and starts a new one in the other Bed on the same date`() {
        housing.move(ahmed.id, bed1B.id, today.minusDays(2))

        assertEquals(bed1B.id, housing.currentStay(ahmed.id)?.bed?.id)
        assertEquals(
            listOf(
                Triple(bed1B.label, today.minusDays(2), null),
                Triple(bed1A.label, today.minusDays(10), today.minusDays(2)),
            ),
            housing.staysOf(ahmed.id).map { Triple(it.bed.label, it.stay.checkIn, it.stay.checkOut) },
        )
        assertEquals(mapOf(bed1B.id to ahmed), housing.workersByBed(room204.id))
    }

    @Test
    fun `a Worker can be moved to a Bed in another Property`() {
        val jebelAli = housing.addProperty("Jebel Ali Block 3").accepted()
        val roomC1 = housing.addRoom(jebelAli.id, "C-1", Sex.Male).accepted()
        val bed2 = housing.addBeds(roomC1.id, listOf("2")).accepted().single()

        housing.move(ahmed.id, bed2.id, today).accepted()

        assertEquals(jebelAli.name, housing.currentStay(ahmed.id)?.property?.name)
    }

    @Test
    fun `a Move to a Bed with a current Stay is rejected and the current Stay is unchanged`() {
        housing.checkIn(bilal.id, bed1B.id, today).accepted()

        assertRejectedAndUnchanged(Rejection.BedOccupied, housing.move(ahmed.id, bed1B.id, today))
    }

    @Test
    fun `a Move to a Room of the other sex is rejected and the current Stay is unchanged`() {
        val femaleRoom = housing.addRoom(alQuoz.id, "B-205", Sex.Female).accepted()
        val femaleBed = housing.addBeds(femaleRoom.id, listOf("1A")).accepted().single()

        assertRejectedAndUnchanged(Rejection.SexMismatch, housing.move(ahmed.id, femaleBed.id, today))
    }

    @Test
    fun `a Move dated in the future or before the current Check-in is rejected`() {
        assertRejectedAndUnchanged(Rejection.FutureDate, housing.move(ahmed.id, bed1B.id, today.plusDays(1)))
        assertRejectedAndUnchanged(
            Rejection.CheckOutBeforeCheckIn,
            housing.move(ahmed.id, bed1B.id, today.minusDays(11)),
        )
    }

    @Test
    fun `a Move to the Bed the Worker is already in is rejected`() {
        assertRejectedAndUnchanged(Rejection.AlreadyInThatBed, housing.move(ahmed.id, bed1A.id, today))
    }

    @Test
    fun `a past-dated Move to a Bed whose earlier Stay overlaps that date is rejected`() {
        housing.checkIn(bilal.id, bed1B.id, today.minusDays(9)).accepted()
        housing.checkOut(bilal.id, today.minusDays(3)).accepted()

        assertRejectedAndUnchanged(Rejection.OverlappingStay, housing.move(ahmed.id, bed1B.id, today.minusDays(5)))
    }

    @Test
    fun `moving to a Bed or moving a Worker that does not exist is rejected`() {
        assertRejectedAndUnchanged(Rejection.BedNotFound, housing.move(ahmed.id, BedId("no-such-bed"), today))
        assertEquals(Outcome.Rejected(Rejection.WorkerNotFound), housing.move(WorkerId("no-such-worker"), bed1B.id, today))
    }

    @Test
    fun `moving a Worker with no current Stay is rejected`() {
        assertEquals(Outcome.Rejected(Rejection.WorkerNotHoused), housing.move(bilal.id, bed1B.id, today))
    }

    private fun assertRejectedAndUnchanged(rejection: Rejection, outcome: Outcome<Stay>) {
        assertEquals(Outcome.Rejected(rejection), outcome)
        assertEquals(
            listOf(Triple(bed1A.id, today.minusDays(10), null)),
            housing.staysOf(ahmed.id).map { Triple(it.bed.id, it.stay.checkIn, it.stay.checkOut) },
        )
    }
}
