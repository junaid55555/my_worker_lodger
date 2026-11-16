package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class OccupancyTest {

    private val housing = newHousing()
    private val today: LocalDate = TODAY

    private val alQuoz = housing.addProperty("Al Quoz Block A").accepted()
    private val room204 = housing.addRoom(alQuoz.id, "B-204", Sex.Male).accepted()
    private val room205 = housing.addRoom(alQuoz.id, "B-205", Sex.Male).accepted()
    private val beds204 = housing.addBeds(room204.id, listOf("1A", "1B", "2A", "2B")).accepted()
    private val beds205 = housing.addBeds(room205.id, listOf("1A", "1B")).accepted()

    private val gulf = housing.addEmployer("Gulf Builders").accepted()
    private val workers = (1..3).map { housing.addWorker(gulf.id, "$it", "Worker $it", Sex.Male).accepted() }

    @Test
    fun `a Room's Occupancy counts Beds with a current Stay against its in-service Beds`() {
        housing.checkIn(workers[0].id, beds204[0].id, today).accepted()
        housing.checkIn(workers[1].id, beds204[1].id, today).accepted()
        housing.markOutOfService(beds204[3].id).accepted()

        val occupancy = housing.roomOccupancy(room204.id)

        assertEquals(Occupancy(bedsWithCurrentStay = 2, inServiceBeds = 3), occupancy)
        assertEquals(1, occupancy.availableBeds)
        assertEquals(2.0 / 3, occupancy.share!!, 0.0001)
    }

    @Test
    fun `a Stay that has ended doesn't count towards Occupancy`() {
        housing.checkIn(workers[0].id, beds204[0].id, today.minusDays(5)).accepted()
        housing.checkOut(workers[0].id, today.minusDays(1)).accepted()

        assertEquals(Occupancy(bedsWithCurrentStay = 0, inServiceBeds = 4), housing.roomOccupancy(room204.id))
    }

    @Test
    fun `a Property's Occupancy adds up its Rooms, and the overall Occupancy adds up every Property`() {
        val jebelAli = housing.addProperty("Jebel Ali Block 3").accepted()
        val roomC1 = housing.addRoom(jebelAli.id, "C-1", Sex.Male).accepted()
        val bedC1 = housing.addBeds(roomC1.id, listOf("1")).accepted().single()
        housing.checkIn(workers[0].id, beds204[0].id, today)
        housing.checkIn(workers[1].id, beds205[0].id, today)
        housing.checkIn(workers[2].id, bedC1.id, today)

        assertEquals(Occupancy(bedsWithCurrentStay = 2, inServiceBeds = 6), housing.propertyOccupancy(alQuoz.id))
        assertEquals(Occupancy(bedsWithCurrentStay = 3, inServiceBeds = 7), housing.overallOccupancy())
    }

    @Test
    fun `with no in-service Beds there is no Occupancy share and no Available Beds`() {
        beds205.forEach { housing.markOutOfService(it.id).accepted() }
        val emptyRoom = housing.addRoom(alQuoz.id, "B-206", Sex.Male).accepted()

        assertEquals(null, housing.roomOccupancy(room205.id).share)
        assertEquals(0, housing.roomOccupancy(room205.id).availableBeds)
        assertEquals(null, housing.roomOccupancy(emptyRoom.id).share)
    }

    @Test
    fun `a Property or the whole of every Property with no in-service Beds has no Occupancy share`() {
        (beds204 + beds205).forEach { housing.markOutOfService(it.id).accepted() }

        assertEquals(null, housing.propertyOccupancy(alQuoz.id).share)
        assertEquals(null, housing.overallOccupancy().share)
    }

    @Test
    fun `the percentage full rounds down, but shows at least 1 when any Bed has a current Stay`() {
        assertEquals(99, Occupancy(bedsWithCurrentStay = 199, inServiceBeds = 200).percentFull)
        assertEquals(1, Occupancy(bedsWithCurrentStay = 1, inServiceBeds = 201).percentFull)
        assertEquals(0, Occupancy(bedsWithCurrentStay = 0, inServiceBeds = 4).percentFull)
        assertEquals(100, Occupancy(bedsWithCurrentStay = 4, inServiceBeds = 4).percentFull)
        assertEquals(null, Occupancy(bedsWithCurrentStay = 0, inServiceBeds = 0).percentFull)
    }
}
