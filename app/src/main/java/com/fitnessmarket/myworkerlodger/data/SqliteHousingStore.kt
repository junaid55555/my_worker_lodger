package com.fitnessmarket.myworkerlodger.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.fitnessmarket.myworkerlodger.domain.Bed
import com.fitnessmarket.myworkerlodger.domain.BedId
import com.fitnessmarket.myworkerlodger.domain.Employer
import com.fitnessmarket.myworkerlodger.domain.EmployerId
import com.fitnessmarket.myworkerlodger.domain.HousingStore
import com.fitnessmarket.myworkerlodger.domain.Property
import com.fitnessmarket.myworkerlodger.domain.PropertyId
import com.fitnessmarket.myworkerlodger.domain.Removal
import com.fitnessmarket.myworkerlodger.domain.Room
import com.fitnessmarket.myworkerlodger.domain.RoomId
import com.fitnessmarket.myworkerlodger.domain.Sex
import com.fitnessmarket.myworkerlodger.domain.Stay
import com.fitnessmarket.myworkerlodger.domain.StayId
import com.fitnessmarket.myworkerlodger.domain.Worker
import com.fitnessmarket.myworkerlodger.domain.WorkerId
import java.time.LocalDate

/** The on-device [HousingStore] (ADR-0001): one SQLite database file, included in Android backup. */
class SqliteHousingStore(context: Context) : HousingStore {

    private val helper = object : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_PROPERTY)
            db.execSQL(CREATE_ROOM)
            db.execSQL(CREATE_BED)
            db.execSQL(CREATE_EMPLOYER)
            db.execSQL(CREATE_WORKER)
            db.execSQL(CREATE_STAY)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                db.execSQL(CREATE_ROOM)
                db.execSQL(CREATE_BED)
            }
            if (oldVersion < 3) {
                db.execSQL(CREATE_EMPLOYER)
                db.execSQL(CREATE_WORKER)
            }
            if (oldVersion < 4) db.execSQL(CREATE_STAY)
            if (oldVersion < 5) db.execSQL("ALTER TABLE bed ADD COLUMN in_service INTEGER NOT NULL DEFAULT 1")
        }
    }

    override fun property(id: PropertyId): Property? =
        query("property", "id = ?", id.value) { it.toProperty() }.firstOrNull()

    override fun properties(): List<Property> = query("property") { it.toProperty() }

    override fun saveProperty(property: Property) = replace(
        "property",
        ContentValues().apply {
            put("id", property.id.value)
            put("name", property.name)
        },
    )

    override fun room(id: RoomId): Room? = query("room", "id = ?", id.value) { it.toRoom() }.firstOrNull()

    override fun rooms(propertyId: PropertyId): List<Room> =
        query("room", "property_id = ?", propertyId.value) { it.toRoom() }

    override fun saveRoom(room: Room) = replace(
        "room",
        ContentValues().apply {
            put("id", room.id.value)
            put("property_id", room.propertyId.value)
            put("name", room.name)
            put("sex", room.sex.name)
        },
    )

    override fun bed(id: BedId): Bed? = query("bed", "id = ?", id.value) { it.toBed() }.firstOrNull()

    override fun beds(roomId: RoomId): List<Bed> = query("bed", "room_id = ?", roomId.value) { it.toBed() }

    override fun saveBeds(beds: List<Bed>) = inTransaction {
        beds.forEach { bed ->
            replace(
                "bed",
                ContentValues().apply {
                    put("id", bed.id.value)
                    put("room_id", bed.roomId.value)
                    put("label", bed.label)
                    put("in_service", if (bed.inService) 1 else 0)
                },
            )
        }
    }

    override fun employer(id: EmployerId): Employer? =
        query("employer", "id = ?", id.value) { it.toEmployer() }.firstOrNull()

    override fun employers(): List<Employer> = query("employer") { it.toEmployer() }

    override fun saveEmployer(employer: Employer) = replace(
        "employer",
        ContentValues().apply {
            put("id", employer.id.value)
            put("name", employer.name)
        },
    )

    override fun worker(id: WorkerId): Worker? = query("worker", "id = ?", id.value) { it.toWorker() }.firstOrNull()

    override fun workers(): List<Worker> = query("worker") { it.toWorker() }

    override fun saveWorker(worker: Worker) = replace(
        "worker",
        ContentValues().apply {
            put("id", worker.id.value)
            put("employer_id", worker.employerId.value)
            put("staff_number", worker.staffNumber)
            put("name", worker.name)
            put("sex", worker.sex.name)
        },
    )

    override fun stays(): List<Stay> = query("stay") { it.toStay() }

    override fun saveStays(stays: List<Stay>) = inTransaction {
        stays.forEach { stay ->
            replace(
                "stay",
                ContentValues().apply {
                    put("id", stay.id.value)
                    put("worker_id", stay.workerId.value)
                    put("bed_id", stay.bedId.value)
                    put("check_in", stay.checkIn.toString())
                    put("check_out", stay.checkOut?.toString())
                },
            )
        }
    }

    private fun <T> query(table: String, where: String? = null, vararg args: String, read: (Cursor) -> T): List<T> =
        helper.readableDatabase.query(table, null, where, args.takeIf { it.isNotEmpty() }, null, null, null)
            .use { cursor -> buildList { while (cursor.moveToNext()) add(read(cursor)) } }

    override fun remove(removal: Removal) = inTransaction {
        val db = helper.writableDatabase
        fun delete(table: String, ids: Collection<String>) = ids.forEach { db.delete(table, "id = ?", arrayOf(it)) }
        delete("bed", removal.bedIds.map { it.value })
        delete("room", removal.roomIds.map { it.value })
        delete("property", removal.propertyIds.map { it.value })
        delete("worker", removal.workerIds.map { it.value })
        delete("employer", removal.employerIds.map { it.value })
    }

    /** Runs [block] so that all its writes are saved together or not at all. */
    private fun inTransaction(block: () -> Unit) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            block()
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun replace(table: String, values: ContentValues) {
        helper.writableDatabase.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun Cursor.text(column: String): String = getString(getColumnIndexOrThrow(column))

    private fun Cursor.toProperty() = Property(PropertyId(text("id")), text("name"))

    private fun Cursor.toRoom() =
        Room(RoomId(text("id")), PropertyId(text("property_id")), text("name"), Sex.valueOf(text("sex")))

    private fun Cursor.toBed() = Bed(
        BedId(text("id")),
        RoomId(text("room_id")),
        text("label"),
        inService = getInt(getColumnIndexOrThrow("in_service")) == 1,
    )

    private fun Cursor.toStay() = Stay(
        StayId(text("id")),
        WorkerId(text("worker_id")),
        BedId(text("bed_id")),
        LocalDate.parse(text("check_in")),
        getString(getColumnIndexOrThrow("check_out"))?.let(LocalDate::parse),
    )

    private fun Cursor.toEmployer() = Employer(EmployerId(text("id")), text("name"))

    private fun Cursor.toWorker() = Worker(
        WorkerId(text("id")),
        EmployerId(text("employer_id")),
        text("staff_number"),
        text("name"),
        Sex.valueOf(text("sex")),
    )

    companion object {
        const val DATABASE_NAME = "housing.db"
        private const val DATABASE_VERSION = 5

        private const val CREATE_PROPERTY =
            "CREATE TABLE property (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)"
        private const val CREATE_ROOM =
            "CREATE TABLE room (id TEXT PRIMARY KEY NOT NULL, " +
                "property_id TEXT NOT NULL REFERENCES property(id), name TEXT NOT NULL, sex TEXT NOT NULL)"
        private const val CREATE_BED =
            "CREATE TABLE bed (id TEXT PRIMARY KEY NOT NULL, " +
                "room_id TEXT NOT NULL REFERENCES room(id), label TEXT NOT NULL, in_service INTEGER NOT NULL DEFAULT 1)"
        private const val CREATE_EMPLOYER =
            "CREATE TABLE employer (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)"
        private const val CREATE_WORKER =
            "CREATE TABLE worker (id TEXT PRIMARY KEY NOT NULL, " +
                "employer_id TEXT NOT NULL REFERENCES employer(id), staff_number TEXT NOT NULL, " +
                "name TEXT NOT NULL, sex TEXT NOT NULL)"
        private const val CREATE_STAY =
            "CREATE TABLE stay (id TEXT PRIMARY KEY NOT NULL, " +
                "worker_id TEXT NOT NULL REFERENCES worker(id), bed_id TEXT NOT NULL REFERENCES bed(id), " +
                "check_in TEXT NOT NULL, check_out TEXT)"
    }
}
