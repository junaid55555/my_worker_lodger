package com.fitnessmarket.myworkerlodger.domain

@JvmInline
value class PropertyId(val value: String)

/** A building or site used to house Workers. */
data class Property(val id: PropertyId, val name: String)
