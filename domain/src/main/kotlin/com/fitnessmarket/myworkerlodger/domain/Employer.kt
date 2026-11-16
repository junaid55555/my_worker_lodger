package com.fitnessmarket.myworkerlodger.domain

@JvmInline
value class EmployerId(val value: String)

/** The company a Worker works for, which sends its Workers to be housed. */
data class Employer(val id: EmployerId, val name: String)
