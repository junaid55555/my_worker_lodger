package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StayHistoryTest {

    private val housing = newHousing()
    private val today: LocalDate = TODAY

    private val property = housing.addProperty("Al Quoz Block A").accepted()
    private val room = housing.addRoom(property.id, "B-204", Sex.Male).accepted()
    private val bed1A = housing.addBeds(room.id, listOf("1A")).accepted().single()
    private val bed1B = housing.addBeds(room.id, listOf("1B")).accepted().single()

    private val gulf = housing.addEmployer("Gulf Builders").accepted()
    private val ahmed = housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male).accepted()
    private val bilal = housing.addWorker(gulf.id, "1043", "Bilal Ahmed", Sex.Male).accepted()

    @Test
    fun `a Worker's Stays are listed with their Bed, Room and Property, most recent first`() {
        housing.checkIn(ahmed.id, bed1A.id, today.minusDays(20))
        housing.move(ahmed.id, bed1B.id, today.minusDays(5))

        assertEquals(
            listOf("B-204 1B from ${today.minusDays(5)}", "B-204 1A from ${today.minusDays(20)}"),
            housing.staysOf(ahmed.id).map { "${it.room.name} ${it.bed.label} from ${it.stay.checkIn}" },
        )
    }

    @Test
    fun `a Bed's Stays are listed with their Worker, most recent first`() {
        housing.checkIn(bilal.id, bed1A.id, today.minusDays(20))
        housing.checkOut(bilal.id, today.minusDays(10))
        housing.checkIn(ahmed.id, bed1A.id, today.minusDays(4))

        assertEquals(
            listOf("Ahmed Khan" to today.minusDays(4), "Bilal Ahmed" to today.minusDays(20)),
            housing.staysInBed(bed1A.id).map { it.worker.name to it.stay.checkIn },
        )
    }

    @Test
    fun `a Bed can be found with its Room and Property, and an unknown id finds nothing`() {
        assertEquals(BedDetails(bed1A, room, property), housing.bedDetails(bed1A.id))
        assertEquals(null, housing.bedDetails(BedId("no-such-bed")))
    }

    @Test
    fun `the Worker list can be narrowed to Workers who are housed now`() {
        housing.checkIn(ahmed.id, bed1A.id, today.minusDays(3))

        assertEquals(listOf("Ahmed Khan"), housing.workers(housedOnly = true).map { it.name })
        assertEquals(listOf("Ahmed Khan", "Bilal Ahmed"), housing.workers().map { it.name })
    }
}
