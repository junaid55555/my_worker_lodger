package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SexChangeTest {

    private val housing = newHousing()
    private val today: LocalDate = TODAY

    private val property = housing.addProperty("Al Quoz Block A").accepted()
    private val room = housing.addRoom(property.id, "B-204", Sex.Male).accepted()
    private val bed = housing.addBeds(room.id, listOf("1A")).accepted().single()

    private val gulf = housing.addEmployer("Gulf Builders").accepted()
    private val ahmed = housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male).accepted()

    @Test
    fun `a Room with no Worker of the other sex staying in it can change sex, and be renamed at the same time`() {
        housing.editRoom(room.id, " B-204F ", Sex.Female).accepted()

        assertEquals("B-204F" to Sex.Female, housing.room(room.id)?.let { it.name to it.sex })
    }

    @Test
    fun `a Room can't change sex while a Worker of the other sex is staying in it`() {
        housing.checkIn(ahmed.id, bed.id, today).accepted()

        assertEquals(Outcome.Rejected(Rejection.WouldMixRoom), housing.editRoom(room.id, "B-204F", Sex.Female))
        assertEquals("B-204" to Sex.Male, housing.room(room.id)?.let { it.name to it.sex })
    }

    @Test
    fun `past Stays don't stop a Room changing sex`() {
        housing.checkIn(ahmed.id, bed.id, today.minusDays(5)).accepted()
        housing.checkOut(ahmed.id, today).accepted()

        housing.editRoom(room.id, "B-204", Sex.Female).accepted()
    }

    @Test
    fun `a Worker with no current Stay can change sex`() {
        housing.editWorker(ahmed.id, "1042", "Ahmed Khan", Sex.Female).accepted()

        assertEquals(Sex.Female, housing.worker(ahmed.id)?.sex)
    }

    @Test
    fun `a housed Worker can't change to the sex the Room isn't for, and nothing else changes`() {
        housing.checkIn(ahmed.id, bed.id, today).accepted()

        assertEquals(Outcome.Rejected(Rejection.WouldMixRoom), housing.editWorker(ahmed.id, "1043", "Ahmed K", Sex.Female))
        assertEquals(ahmed, housing.worker(ahmed.id))
    }

    @Test
    fun `a housed Worker keeping the Room's sex can still have their details edited`() {
        housing.checkIn(ahmed.id, bed.id, today).accepted()

        housing.editWorker(ahmed.id, "1043", "Ahmed K", Sex.Male).accepted()
    }

    @Test
    fun `editing a Room that does not exist is rejected`() {
        assertEquals(Outcome.Rejected(Rejection.RoomNotFound), housing.editRoom(RoomId("none"), "X", Sex.Male))
    }
}
