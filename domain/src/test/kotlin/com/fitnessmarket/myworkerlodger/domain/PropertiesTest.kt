package com.fitnessmarket.myworkerlodger.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PropertiesTest {

    private val housing = newHousing()

    @Test
    fun `an added Property appears in the list of Properties`() {
        housing.addProperty("Al Quoz Block A")

        assertEquals(listOf("Al Quoz Block A"), housing.properties().map { it.name })
    }

    @Test
    fun `Properties are listed alphabetically, ignoring case`() {
        housing.addProperty("Sonapur")
        housing.addProperty("al Quoz")
        housing.addProperty("Jebel Ali")

        assertEquals(listOf("al Quoz", "Jebel Ali", "Sonapur"), housing.properties().map { it.name })
    }

    @Test
    fun `spaces around a Property name are not kept`() {
        housing.addProperty("  Al Quoz Block A  ")

        assertEquals(listOf("Al Quoz Block A"), housing.properties().map { it.name })
    }

    @Test
    fun `a Property with a blank name is rejected and not added`() {
        val outcome = housing.addProperty("   ")

        assertEquals(Outcome.Rejected(Rejection.BlankName), outcome)
        assertEquals(emptyList<Property>(), housing.properties())
    }

    @Test
    fun `a renamed Property is listed under its new name`() {
        val property = housing.addProperty("Al Quoz Block A").accepted()

        housing.renameProperty(property.id, "Al Quoz Block B")

        assertEquals(listOf("Al Quoz Block B"), housing.properties().map { it.name })
    }

    @Test
    fun `spaces around a new Property name are not kept`() {
        val property = housing.addProperty("Al Quoz Block A").accepted()

        housing.renameProperty(property.id, " Al Quoz Block B ")

        assertEquals(listOf("Al Quoz Block B"), housing.properties().map { it.name })
    }

    @Test
    fun `renaming a Property to a blank name is rejected and keeps the old name`() {
        val property = housing.addProperty("Al Quoz Block A").accepted()

        val outcome = housing.renameProperty(property.id, " ")

        assertEquals(Outcome.Rejected(Rejection.BlankName), outcome)
        assertEquals(listOf("Al Quoz Block A"), housing.properties().map { it.name })
    }

    @Test
    fun `renaming a Property that does not exist is rejected, even with a blank name`() {
        val outcome = housing.renameProperty(PropertyId("no-such-property"), " ")

        assertEquals(Outcome.Rejected(Rejection.PropertyNotFound), outcome)
    }

    @Test
    fun `a Property can be found by its id, and an unknown id finds nothing`() {
        val property = housing.addProperty("Al Quoz Block A").accepted()

        assertEquals(property, housing.property(property.id))
        assertEquals(null, housing.property(PropertyId("no-such-property")))
    }
}
