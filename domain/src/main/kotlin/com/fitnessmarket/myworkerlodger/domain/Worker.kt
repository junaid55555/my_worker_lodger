package com.fitnessmarket.myworkerlodger.domain

@JvmInline
value class WorkerId(val value: String)

/**
 * A person who can be housed in the accommodation. A Worker is identified by their Employer
 * together with the Staff Number that Employer gave them.
 */
data class Worker(
    val id: WorkerId,
    val employerId: EmployerId,
    val staffNumber: String,
    val name: String,
    val sex: Sex,
)
