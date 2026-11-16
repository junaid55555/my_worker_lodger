package com.fitnessmarket.myworkerlodger.domain

/** Whether a Room is designated for men or for women. */
enum class Sex { Male, Female }

@JvmInline
value class RoomId(val value: String)

/** A space within a Property that contains one or more Beds and is designated either male or female. */
data class Room(val id: RoomId, val propertyId: PropertyId, val name: String, val sex: Sex)
