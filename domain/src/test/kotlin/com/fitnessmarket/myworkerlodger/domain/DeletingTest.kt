package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DeletingTest {

    private val housing = newHousing()
    private val today: LocalDate = TODAY

    private val property = housing.addProperty("Al Quoz Block A").accepted()
    private val room = housing.addRoom(property.id, "B-204", Sex.Male).accepted()
    private val bed1A = housing.addBeds(room.id, listOf("1A")).accepted().single()
    private val bed1B = housing.addBeds(room.id, listOf("1B")).accepted().single()

    private val gulf = housing.addEmployer("Gulf Builders").accepted()
    private val ahmed = housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male).accepted()

    @Test
    fun `a Bed that has never had a Stay can be deleted`() {
        housing.deleteBed(bed1B.id).accepted()

        assertEquals(listOf("1A"), housing.beds(room.id).map { it.label })
    }

    @Test
    fun `a Bed with a past or current Stay can't be deleted`() {
        housing.checkIn(ahmed.id, bed1A.id, today.minusDays(3)).accepted()
        assertEquals(Outcome.Rejected(Rejection.HasStays), housing.deleteBed(bed1A.id))

        housing.checkOut(ahmed.id, today).accepted()
        assertEquals(Outcome.Rejected(Rejection.HasStays), housing.deleteBed(bed1A.id))
        assertEquals(listOf("1A", "1B"), housing.beds(room.id).map { it.label })
    }

    @Test
    fun `deleting a Room deletes its Beds, unless any of them has ever had a Stay`() {
        housing.checkIn(ahmed.id, bed1A.id, today).accepted()
        assertEquals(Outcome.Rejected(Rejection.HasStays), housing.deleteRoom(room.id))
        assertEquals(listOf("B-204"), housing.rooms(property.id).map { it.name })

        val emptyRoom = housing.addRoom(property.id, "B-205", Sex.Male).accepted()
        val emptyBed = housing.addBeds(emptyRoom.id, listOf("1A")).accepted().single()
        housing.deleteRoom(emptyRoom.id).accepted()

        assertEquals(listOf("B-204"), housing.rooms(property.id).map { it.name })
        assertEquals(null, housing.bedDetails(emptyBed.id))
    }

    @Test
    fun `deleting a Property deletes its Rooms and Beds, unless any Bed has ever had a Stay`() {
        housing.checkIn(ahmed.id, bed1A.id, today.minusDays(3)).accepted()
        housing.checkOut(ahmed.id, today).accepted()
        assertEquals(Outcome.Rejected(Rejection.HasStays), housing.deleteProperty(property.id))

        val other = housing.addProperty("Jebel Ali Block 3").accepted()
        val otherRoom = housing.addRoom(other.id, "C-1", Sex.Male).accepted()
        housing.addBeds(otherRoom.id, listOf("1"))
        housing.deleteProperty(other.id).accepted()

        assertEquals(listOf("Al Quoz Block A"), housing.properties().map { it.name })
        assertEquals(null, housing.room(otherRoom.id))
    }

    @Test
    fun `a Worker who has never had a Stay can be deleted, but not one with a Stay`() {
        val bilal = housing.addWorker(gulf.id, "1043", "Bilal Ahmed", Sex.Male).accepted()
        housing.checkIn(ahmed.id, bed1A.id, today).accepted()

        housing.deleteWorker(bilal.id).accepted()
        assertEquals(Outcome.Rejected(Rejection.HasStays), housing.deleteWorker(ahmed.id))

        assertEquals(listOf("Ahmed Khan"), housing.workers().map { it.name })
    }

    @Test
    fun `an Employer can be deleted only when it has no Workers`() {
        val noor = housing.addEmployer("Al Noor Facilities").accepted()

        assertEquals(Outcome.Rejected(Rejection.EmployerHasWorkers), housing.deleteEmployer(gulf.id))
        housing.deleteEmployer(noor.id).accepted()

        assertEquals(listOf("Gulf Builders"), housing.employers().map { it.name })
    }

    @Test
    fun `deleting something that does not exist is rejected`() {
        assertEquals(Outcome.Rejected(Rejection.PropertyNotFound), housing.deleteProperty(PropertyId("none")))
        assertEquals(Outcome.Rejected(Rejection.RoomNotFound), housing.deleteRoom(RoomId("none")))
        assertEquals(Outcome.Rejected(Rejection.BedNotFound), housing.deleteBed(BedId("none")))
        assertEquals(Outcome.Rejected(Rejection.EmployerNotFound), housing.deleteEmployer(EmployerId("none")))
        assertEquals(Outcome.Rejected(Rejection.WorkerNotFound), housing.deleteWorker(WorkerId("none")))
    }
}
