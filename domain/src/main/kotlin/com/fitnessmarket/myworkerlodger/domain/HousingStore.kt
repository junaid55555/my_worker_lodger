package com.fitnessmarket.myworkerlodger.domain

/**
 * Where [Housing] keeps its records. The domain defines this interface; the app supplies an
 * on-device implementation (ADR-0001) and tests use an in-memory one.
 */
interface HousingStore {
    fun properties(): List<Property>
    fun saveProperty(property: Property)
}
