package com.fitnessmarket.myworkerlodger.domain

/** A [HousingStore] that keeps everything in memory, for domain tests. */
class InMemoryHousingStore : HousingStore {
    private val properties = linkedMapOf<PropertyId, Property>()

    override fun properties(): List<Property> = properties.values.toList()

    override fun saveProperty(property: Property) {
        properties[property.id] = property
    }
}
