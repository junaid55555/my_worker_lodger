package com.fitnessmarket.myworkerlodger.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkersTest {

    private val housing = newHousing()
    private val gulf = housing.addEmployer("Gulf Builders").accepted()
    private val noor = housing.addEmployer("Al Noor Facilities").accepted()

    @Test
    fun `an added Worker appears in the list of Workers with their Employer, Staff Number, name and sex`() {
        housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male)

        assertEquals(
            listOf(listOf(gulf.id, "1042", "Ahmed Khan", Sex.Male)),
            housing.workers().map { listOf(it.employerId, it.staffNumber, it.name, it.sex) },
        )
    }

    @Test
    fun `adding a Worker for an Employer that does not exist is rejected`() {
        val outcome = housing.addWorker(EmployerId("no-such-employer"), "1042", "Ahmed Khan", Sex.Male)

        assertEquals(Outcome.Rejected(Rejection.EmployerNotFound), outcome)
        assertEquals(emptyList<Worker>(), housing.workers())
    }

    @Test
    fun `a Worker with a blank Staff Number is rejected`() {
        val outcome = housing.addWorker(gulf.id, "  ", "Ahmed Khan", Sex.Male)

        assertEquals(Outcome.Rejected(Rejection.BlankStaffNumber), outcome)
        assertEquals(emptyList<Worker>(), housing.workers())
    }

    @Test
    fun `a Worker with a blank name is rejected`() {
        val outcome = housing.addWorker(gulf.id, "1042", "", Sex.Male)

        assertEquals(Outcome.Rejected(Rejection.BlankName), outcome)
        assertEquals(emptyList<Worker>(), housing.workers())
    }

    @Test
    fun `spaces around a Staff Number and name are not kept`() {
        housing.addWorker(gulf.id, " 1042 ", "  Ahmed Khan ", Sex.Male)

        assertEquals(listOf("1042" to "Ahmed Khan"), housing.workers().map { it.staffNumber to it.name })
    }

    @Test
    fun `a Staff Number already used by another Worker of the same Employer is rejected`() {
        housing.addWorker(gulf.id, "E-1042", "Ahmed Khan", Sex.Male)

        val outcome = housing.addWorker(gulf.id, " e-1042", "Bilal Ahmed", Sex.Male)

        assertEquals(Outcome.Rejected(Rejection.DuplicateStaffNumber), outcome)
        assertEquals(listOf("Ahmed Khan"), housing.workers().map { it.name })
    }

    @Test
    fun `the same Staff Number is accepted under a different Employer`() {
        housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male)

        housing.addWorker(noor.id, "1042", "Priya Nair", Sex.Female)

        assertEquals(setOf("Ahmed Khan", "Priya Nair"), housing.workers().map { it.name }.toSet())
    }

    @Test
    fun `an edited Worker has their new Staff Number and name`() {
        val worker = housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male).accepted()

        housing.editWorker(worker.id, " 1043 ", " Ahmed Raza Khan ")

        assertEquals("1043" to "Ahmed Raza Khan", housing.worker(worker.id)?.let { it.staffNumber to it.name })
    }

    @Test
    fun `a Worker can keep their own Staff Number when their name is edited`() {
        val worker = housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male).accepted()

        val outcome = housing.editWorker(worker.id, "1042", "Ahmed Raza Khan")

        assertEquals("Ahmed Raza Khan", outcome.accepted().name)
    }

    @Test
    fun `editing a Worker to another Worker's Staff Number in the same Employer is rejected`() {
        housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male)
        val bilal = housing.addWorker(gulf.id, "1043", "Bilal Ahmed", Sex.Male).accepted()

        val outcome = housing.editWorker(bilal.id, "1042", "Bilal Ahmed")

        assertEquals(Outcome.Rejected(Rejection.DuplicateStaffNumber), outcome)
        assertEquals("1043", housing.worker(bilal.id)?.staffNumber)
    }

    @Test
    fun `editing a Worker to a blank Staff Number or name is rejected`() {
        val worker = housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male).accepted()

        assertEquals(Outcome.Rejected(Rejection.BlankStaffNumber), housing.editWorker(worker.id, " ", "Ahmed Khan"))
        assertEquals(Outcome.Rejected(Rejection.BlankName), housing.editWorker(worker.id, "1042", " "))
        assertEquals(worker, housing.worker(worker.id))
    }

    @Test
    fun `editing a Worker that does not exist is rejected, even with blank details`() {
        val outcome = housing.editWorker(WorkerId("no-such-worker"), "", "")

        assertEquals(Outcome.Rejected(Rejection.WorkerNotFound), outcome)
    }

    @Test
    fun `Workers are listed in natural order of name`() {
        housing.addWorker(gulf.id, "3", "worker 10", Sex.Male)
        housing.addWorker(gulf.id, "2", "Worker 2", Sex.Male)
        housing.addWorker(noor.id, "1", "Ahmed Khan", Sex.Male)

        assertEquals(listOf("Ahmed Khan", "Worker 2", "worker 10"), housing.workers().map { it.name })
    }

    @Test
    fun `the Workers of one Employer can be listed on their own`() {
        housing.addWorker(gulf.id, "1042", "Ahmed Khan", Sex.Male)
        housing.addWorker(noor.id, "77", "Priya Nair", Sex.Female)

        assertEquals(listOf("Priya Nair"), housing.workers(employerId = noor.id).map { it.name })
    }

    @Test
    fun `Workers can be searched by part of their name or Staff Number, ignoring case`() {
        housing.addWorker(gulf.id, "E-1042", "Ahmed Khan", Sex.Male)
        housing.addWorker(gulf.id, "E-2001", "Bilal Ahmed", Sex.Male)
        housing.addWorker(noor.id, "77", "Priya Nair", Sex.Female)

        assertEquals(listOf("Ahmed Khan", "Bilal Ahmed"), housing.workers(search = " ahmed ").map { it.name })
        assertEquals(listOf("Ahmed Khan"), housing.workers(search = "e-104").map { it.name })
        assertEquals(3, housing.workers(search = "  ").size)
    }

    @Test
    fun `an unknown Worker id finds nothing`() {
        assertEquals(null, housing.worker(WorkerId("no-such-worker")))
    }
}
