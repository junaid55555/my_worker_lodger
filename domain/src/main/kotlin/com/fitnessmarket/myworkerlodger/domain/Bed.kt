package com.fitnessmarket.myworkerlodger.domain

@JvmInline
value class BedId(val value: String)

/** A single sleeping place within a Room, and the unit a Worker is assigned to. */
data class Bed(val id: BedId, val roomId: RoomId, val label: String)
