package com.fitnessmarket.myworkerlodger.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class EmployersTest {

    private val housing = newHousing()

    @Test
    fun `an added Employer appears in the list of Employers`() {
        housing.addEmployer("Gulf Builders LLC")

        assertEquals(listOf("Gulf Builders LLC"), housing.employers().map { it.name })
    }

    @Test
    fun `an Employer with a blank name is rejected and not added`() {
        val outcome = housing.addEmployer(" ")

        assertEquals(Outcome.Rejected(Rejection.BlankName), outcome)
        assertEquals(emptyList<Employer>(), housing.employers())
    }

    @Test
    fun `spaces around an Employer name are not kept`() {
        housing.addEmployer("  Gulf Builders LLC ")

        assertEquals(listOf("Gulf Builders LLC"), housing.employers().map { it.name })
    }

    @Test
    fun `a renamed Employer is listed under its new name`() {
        val employer = housing.addEmployer("Gulf Builders").accepted()

        housing.renameEmployer(employer.id, " Gulf Builders LLC ")

        assertEquals(listOf("Gulf Builders LLC"), housing.employers().map { it.name })
    }

    @Test
    fun `renaming an Employer to a blank name is rejected and keeps the old name`() {
        val employer = housing.addEmployer("Gulf Builders").accepted()

        val outcome = housing.renameEmployer(employer.id, "")

        assertEquals(Outcome.Rejected(Rejection.BlankName), outcome)
        assertEquals(listOf("Gulf Builders"), housing.employers().map { it.name })
    }

    @Test
    fun `renaming an Employer that does not exist is rejected, even with a blank name`() {
        val outcome = housing.renameEmployer(EmployerId("no-such-employer"), " ")

        assertEquals(Outcome.Rejected(Rejection.EmployerNotFound), outcome)
    }

    @Test
    fun `an Employer can be found by its id, and an unknown id finds nothing`() {
        val employer = housing.addEmployer("Gulf Builders").accepted()

        assertEquals(employer, housing.employer(employer.id))
        assertEquals(null, housing.employer(EmployerId("no-such-employer")))
    }

    @Test
    fun `Employers are listed in natural order`() {
        housing.addEmployer("Site Crew 10")
        housing.addEmployer("site Crew 2")
        housing.addEmployer("Al Noor Facilities")

        assertEquals(
            listOf("Al Noor Facilities", "site Crew 2", "Site Crew 10"),
            housing.employers().map { it.name },
        )
    }
}
