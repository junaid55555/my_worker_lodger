package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CorrectStayTest {

    private val housing = newHousing()
    private val today: LocalDate = TODAY

    private val property = housing.addProperty("Al Quoz Block A").accepted()
    private val room = housing.addRoom(property.id, "B-204", Sex.Male).accepted()
    private val bed1A = housing.addBeds(room.id, listOf("1A")).accepted().single()
    private val bed1B = housing.addBeds(room.id, listOf("1B")).accepted().single()

    private val gulf = housing.addEmployer("Gulf Builders").accepted()
    private val ahmed = housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male).accepted()
    private val bilal = housing.addWorker(gulf.id, "1043", "Bilal Ahmed", Sex.Male).accepted()

    /** Ahmed: 1A from day -20 to day -10, then 1B from day -10 (current). */
    private val earlier = housing.checkIn(ahmed.id, bed1A.id, today.minusDays(20)).accepted()
    private val current = housing.move(ahmed.id, bed1B.id, today.minusDays(10)).accepted()

    @Test
    fun `the Check-in date of a current Stay can be corrected`() {
        housing.correctStay(current.id, checkIn = today.minusDays(8), checkOut = null).accepted()

        assertEquals(today.minusDays(8), housing.currentStay(ahmed.id)?.stay?.checkIn)
    }

    @Test
    fun `both dates of an ended Stay can be corrected`() {
        housing.correctStay(earlier.id, checkIn = today.minusDays(25), checkOut = today.minusDays(12)).accepted()

        assertEquals(
            today.minusDays(25) to today.minusDays(12),
            housing.staysOf(ahmed.id).last().stay.let { it.checkIn to it.checkOut },
        )
    }

    @Test
    fun `a correction to a future date is rejected`() {
        assertRejectedAndUnchanged(Rejection.FutureDate, housing.correctStay(current.id, today.plusDays(1), null))
        assertRejectedAndUnchanged(
            Rejection.FutureDate,
            housing.correctStay(earlier.id, today.minusDays(20), today.plusDays(1)),
        )
    }

    @Test
    fun `a correction that puts the Check-out before the Check-in is rejected`() {
        assertRejectedAndUnchanged(
            Rejection.CheckOutBeforeCheckIn,
            housing.correctStay(earlier.id, today.minusDays(20), today.minusDays(21)),
        )
    }

    @Test
    fun `a correction that overlaps another Stay of the same Worker is rejected`() {
        assertRejectedAndUnchanged(
            Rejection.OverlappingStay,
            housing.correctStay(earlier.id, today.minusDays(20), today.minusDays(9)),
        )
    }

    @Test
    fun `a Check-out can be corrected to the date the next Stay begins`() {
        housing.correctStay(current.id, checkIn = today.minusDays(8), checkOut = null).accepted()

        housing.correctStay(earlier.id, checkIn = today.minusDays(20), checkOut = today.minusDays(8)).accepted()
    }

    @Test
    fun `correcting a current Stay's Check-in back into the earlier Stay is rejected`() {
        assertRejectedAndUnchanged(Rejection.OverlappingStay, housing.correctStay(current.id, today.minusDays(12), null))
    }

    @Test
    fun `a correction that overlaps another Stay in the same Bed is rejected`() {
        housing.checkIn(bilal.id, bed1A.id, today.minusDays(5)).accepted()

        assertRejectedAndUnchanged(
            Rejection.OverlappingStay,
            housing.correctStay(earlier.id, today.minusDays(20), today.minusDays(4)),
        )
    }

    @Test
    fun `a correction can't end a current Stay or reopen an ended one`() {
        assertRejectedAndUnchanged(
            Rejection.StayIsCurrent,
            housing.correctStay(current.id, today.minusDays(10), today.minusDays(1)),
        )
        assertRejectedAndUnchanged(
            Rejection.CheckOutRequired,
            housing.correctStay(earlier.id, today.minusDays(20), null),
        )
    }

    @Test
    fun `correcting a Stay that does not exist is rejected`() {
        assertEquals(
            Outcome.Rejected(Rejection.StayNotFound),
            housing.correctStay(StayId("no-such-stay"), today.minusDays(1), null),
        )
    }

    private fun assertRejectedAndUnchanged(rejection: Rejection, outcome: Outcome<Stay>) {
        assertEquals(Outcome.Rejected(rejection), outcome)
        assertEquals(
            listOf(today.minusDays(10) to null, today.minusDays(20) to today.minusDays(10)),
            housing.staysOf(ahmed.id).map { it.stay.checkIn to it.stay.checkOut },
        )
    }
}
