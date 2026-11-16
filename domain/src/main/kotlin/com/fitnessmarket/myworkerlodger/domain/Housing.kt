package com.fitnessmarket.myworkerlodger.domain

import java.time.LocalDate
import java.util.UUID

/** Supplies today's date, so date rules can be tested with a fixed day. */
fun interface Today {
    fun date(): LocalDate
}

/**
 * The single public entry point to the worker housing domain. Every operation an
 * Accommodation Manager can perform goes through here.
 */
class Housing(
    private val store: HousingStore,
    private val today: Today,
) {
    fun addProperty(name: String): Outcome<Property> {
        if (name.isBlank()) return Outcome.Rejected(Rejection.BlankName)
        val property = Property(PropertyId(UUID.randomUUID().toString()), name.trim())
        store.saveProperty(property)
        return Outcome.Accepted(property)
    }

    fun renameProperty(id: PropertyId, name: String): Outcome<Property> {
        val property = store.property(id) ?: return Outcome.Rejected(Rejection.PropertyNotFound)
        if (name.isBlank()) return Outcome.Rejected(Rejection.BlankName)
        val renamed = property.copy(name = name.trim())
        store.saveProperty(renamed)
        return Outcome.Accepted(renamed)
    }

    fun property(id: PropertyId): Property? = store.property(id)

    fun properties(): List<Property> =
        store.properties().sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    fun addRoom(propertyId: PropertyId, name: String, sex: Sex): Outcome<Room> {
        if (store.property(propertyId) == null) return Outcome.Rejected(Rejection.PropertyNotFound)
        if (name.isBlank()) return Outcome.Rejected(Rejection.BlankName)
        val room = Room(RoomId(UUID.randomUUID().toString()), propertyId, name.trim(), sex)
        store.saveRoom(room)
        return Outcome.Accepted(room)
    }

    fun renameRoom(id: RoomId, name: String): Outcome<Room> {
        val room = store.room(id) ?: return Outcome.Rejected(Rejection.RoomNotFound)
        if (name.isBlank()) return Outcome.Rejected(Rejection.BlankName)
        val renamed = room.copy(name = name.trim())
        store.saveRoom(renamed)
        return Outcome.Accepted(renamed)
    }

    fun addBeds(roomId: RoomId, labels: List<String>): Outcome<List<Bed>> {
        if (store.room(roomId) == null) return Outcome.Rejected(Rejection.RoomNotFound)
        if (labels.isEmpty() || labels.any { it.isBlank() }) return Outcome.Rejected(Rejection.BlankLabel)
        val beds = labels.map { Bed(BedId(UUID.randomUUID().toString()), roomId, it.trim()) }
        store.saveBeds(beds)
        return Outcome.Accepted(beds)
    }

    fun relabelBed(id: BedId, label: String): Outcome<Bed> {
        val bed = store.bed(id) ?: return Outcome.Rejected(Rejection.BedNotFound)
        if (label.isBlank()) return Outcome.Rejected(Rejection.BlankLabel)
        val relabelled = bed.copy(label = label.trim())
        store.saveBeds(listOf(relabelled))
        return Outcome.Accepted(relabelled)
    }

    /** Marks a Bed Out of Service, for example because it's broken, so no Worker can check in to it. */
    fun markOutOfService(id: BedId): Outcome<Bed> = setInService(id, inService = false)

    fun putBackInService(id: BedId): Outcome<Bed> = setInService(id, inService = true)

    private fun setInService(id: BedId, inService: Boolean): Outcome<Bed> {
        val bed = store.bed(id) ?: return Outcome.Rejected(Rejection.BedNotFound)
        if (!inService && store.stays().any { it.bedId == id && it.isCurrent }) return Outcome.Rejected(Rejection.BedOccupied)
        val changed = bed.copy(inService = inService)
        store.saveBeds(listOf(changed))
        return Outcome.Accepted(changed)
    }

    fun beds(roomId: RoomId): List<Bed> = store.beds(roomId).sortedWith(compareBy(NaturalOrder) { it.label })

    fun room(id: RoomId): Room? = store.room(id)

    fun rooms(propertyId: PropertyId): List<Room> =
        store.rooms(propertyId).sortedWith(compareBy(NaturalOrder) { it.name })

    fun addEmployer(name: String): Outcome<Employer> {
        if (name.isBlank()) return Outcome.Rejected(Rejection.BlankName)
        val employer = Employer(EmployerId(UUID.randomUUID().toString()), name.trim())
        store.saveEmployer(employer)
        return Outcome.Accepted(employer)
    }

    fun renameEmployer(id: EmployerId, name: String): Outcome<Employer> {
        val employer = store.employer(id) ?: return Outcome.Rejected(Rejection.EmployerNotFound)
        if (name.isBlank()) return Outcome.Rejected(Rejection.BlankName)
        val renamed = employer.copy(name = name.trim())
        store.saveEmployer(renamed)
        return Outcome.Accepted(renamed)
    }

    fun employer(id: EmployerId): Employer? = store.employer(id)

    fun addWorker(employerId: EmployerId, staffNumber: String, name: String, sex: Sex): Outcome<Worker> {
        if (store.employer(employerId) == null) return Outcome.Rejected(Rejection.EmployerNotFound)
        workerDetailsProblem(employerId, staffNumber, name, except = null)?.let { return Outcome.Rejected(it) }
        val worker = Worker(WorkerId(UUID.randomUUID().toString()), employerId, staffNumber.trim(), name.trim(), sex)
        store.saveWorker(worker)
        return Outcome.Accepted(worker)
    }

    fun editWorker(id: WorkerId, staffNumber: String, name: String): Outcome<Worker> {
        val worker = store.worker(id) ?: return Outcome.Rejected(Rejection.WorkerNotFound)
        workerDetailsProblem(worker.employerId, staffNumber, name, except = id)?.let { return Outcome.Rejected(it) }
        val edited = worker.copy(staffNumber = staffNumber.trim(), name = name.trim())
        store.saveWorker(edited)
        return Outcome.Accepted(edited)
    }

    fun worker(id: WorkerId): Worker? = store.worker(id)

    fun checkIn(workerId: WorkerId, bedId: BedId, date: LocalDate): Outcome<Stay> {
        checkInProblem(workerId, bedId, date, store.stays())?.let { return Outcome.Rejected(it) }
        val stay = newStay(workerId, bedId, date)
        store.saveStays(listOf(stay))
        return Outcome.Accepted(stay)
    }

    /**
     * Moves a housed Worker to another Bed on [date]: their current Stay ends with a Check-out and a
     * new Stay begins with a Check-in, both on that date. All or nothing: if the new Bed breaks any
     * Check-in rule, the current Stay is left as it was.
     */
    fun move(workerId: WorkerId, bedId: BedId, date: LocalDate): Outcome<Stay> {
        val ended = when (val checkOut = endCurrentStay(workerId, date)) {
            is Outcome.Rejected -> return checkOut
            is Outcome.Accepted -> checkOut.value
        }
        if (ended.bedId == bedId) return Outcome.Rejected(Rejection.AlreadyInThatBed)
        val staysAfterCheckOut = store.stays().map { if (it.id == ended.id) ended else it }
        checkInProblem(workerId, bedId, date, staysAfterCheckOut)?.let { return Outcome.Rejected(it) }
        val started = newStay(workerId, bedId, date)
        store.saveStays(listOf(ended, started))
        return Outcome.Accepted(started)
    }

    /** Which Check-in rule, if any, stops [workerId] checking in to [bedId] on [date], given [stays]. */
    private fun checkInProblem(workerId: WorkerId, bedId: BedId, date: LocalDate, stays: List<Stay>): Rejection? {
        val worker = store.worker(workerId) ?: return Rejection.WorkerNotFound
        val bed = store.bed(bedId) ?: return Rejection.BedNotFound
        val room = store.room(bed.roomId) ?: return Rejection.RoomNotFound
        return when {
            date.isAfter(today.date()) -> Rejection.FutureDate
            stays.any { it.workerId == workerId && it.isCurrent } -> Rejection.WorkerAlreadyHoused
            stays.any { it.bedId == bedId && it.isCurrent } -> Rejection.BedOccupied
            !bed.inService -> Rejection.BedOutOfService
            worker.sex != room.sex -> Rejection.SexMismatch
            stays.any { (it.workerId == workerId || it.bedId == bedId) && it.overlaps(date, until = null) } ->
                Rejection.OverlappingStay
            else -> null
        }
    }

    fun checkOut(workerId: WorkerId, date: LocalDate): Outcome<Stay> {
        val ended = when (val checkOut = endCurrentStay(workerId, date)) {
            is Outcome.Rejected -> return checkOut
            is Outcome.Accepted -> checkOut.value
        }
        store.saveStays(listOf(ended))
        return Outcome.Accepted(ended)
    }

    /** The Worker's current Stay as it would be with a Check-out on [date], checked but not yet saved. */
    private fun endCurrentStay(workerId: WorkerId, date: LocalDate): Outcome<Stay> {
        if (store.worker(workerId) == null) return Outcome.Rejected(Rejection.WorkerNotFound)
        val stay = currentStayOf(workerId) ?: return Outcome.Rejected(Rejection.WorkerNotHoused)
        if (date.isAfter(today.date())) return Outcome.Rejected(Rejection.FutureDate)
        if (date.isBefore(stay.checkIn)) return Outcome.Rejected(Rejection.CheckOutBeforeCheckIn)
        return Outcome.Accepted(stay.copy(checkOut = date))
    }

    private fun newStay(workerId: WorkerId, bedId: BedId, date: LocalDate) =
        Stay(StayId(UUID.randomUUID().toString()), workerId, bedId, date)

    /** Every Stay the Worker has had, most recent Check-in first. */
    fun staysOf(workerId: WorkerId): List<StayDetails> =
        store.stays().filter { it.workerId == workerId }.sortedByDescending { it.checkIn }.mapNotNull(::details)

    /** Where the Worker is living now, or null if they have no current Stay. */
    fun currentStay(workerId: WorkerId): StayDetails? = currentStayOf(workerId)?.let(::details)

    private fun currentStayOf(workerId: WorkerId): Stay? = store.stays().firstOrNull { it.workerId == workerId && it.isCurrent }

    /**
     * The Beds a Worker can be checked in to now: in-service Beds with no current Stay (Available
     * Beds) in Rooms of the Worker's sex,
     * ordered by Property, then Room, then Bed.
     */
    fun bedsAvailableFor(workerId: WorkerId): List<BedDetails> {
        val worker = store.worker(workerId) ?: return emptyList()
        val occupied = store.stays().filter { it.isCurrent }.map { it.bedId }.toSet()
        return properties().flatMap { property ->
            rooms(property.id).filter { it.sex == worker.sex }.flatMap { room ->
                beds(room.id).filter { it.inService && it.id !in occupied }.map { BedDetails(it, room, property) }
            }
        }
    }

    /** The Worker in each occupied Bed of the Room. */
    fun workersByBed(roomId: RoomId): Map<BedId, Worker> {
        val bedIds = store.beds(roomId).map { it.id }.toSet()
        return store.stays()
            .filter { it.isCurrent && it.bedId in bedIds }
            .mapNotNull { stay -> store.worker(stay.workerId)?.let { stay.bedId to it } }
            .toMap()
    }

    private fun details(stay: Stay): StayDetails? {
        val bed = store.bed(stay.bedId) ?: return null
        val room = store.room(bed.roomId) ?: return null
        val property = store.property(room.propertyId) ?: return null
        return StayDetails(stay, BedDetails(bed, room, property))
    }

    /** Staff Numbers are compared ignoring case and surrounding spaces, so "E-1042" and " e-1042" clash. */
    private fun staffNumberTaken(employerId: EmployerId, staffNumber: String, except: WorkerId?): Boolean =
        store.workers().any {
            it.id != except && it.employerId == employerId &&
                it.staffNumber.equals(staffNumber.trim(), ignoreCase = true)
        }

    /** What's wrong with these Worker details, if anything; [except] is the Worker being edited. */
    private fun workerDetailsProblem(employerId: EmployerId, staffNumber: String, name: String, except: WorkerId?): Rejection? =
        when {
            staffNumber.isBlank() -> Rejection.BlankStaffNumber
            name.isBlank() -> Rejection.BlankName
            staffNumberTaken(employerId, staffNumber, except) -> Rejection.DuplicateStaffNumber
            else -> null
        }

    /**
     * Workers in natural order of name, optionally only those of one Employer ([employerId]), and only those whose
     * name or Staff Number contains [search] (ignoring case). A blank [search] matches everyone.
     */
    fun workers(employerId: EmployerId? = null, search: String = ""): List<Worker> {
        val text = search.trim()
        return store.workers()
            .filter { employerId == null || it.employerId == employerId }
            .filter { it.name.contains(text, ignoreCase = true) || it.staffNumber.contains(text, ignoreCase = true) }
            .sortedWith(compareBy(NaturalOrder) { it.name })
    }

    fun employers(): List<Employer> = store.employers().sortedWith(compareBy(NaturalOrder) { it.name })
}
